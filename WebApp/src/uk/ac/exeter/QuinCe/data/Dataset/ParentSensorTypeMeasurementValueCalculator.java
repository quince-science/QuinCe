package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.WeightedMeanCalculator;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ParentSensorTypeMeasurementValueCalculator
  extends MeasurementValueCalculator {

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    List<MeasurementValue> childMeasurementValues = new ArrayList<MeasurementValue>();

    for (SensorType childType : sensorConfig.getChildren(requiredSensorType)) {
      if (instrument.getSensorAssignments().isAssigned(childType)) {
        childMeasurementValues.add(MeasurementValueCalculatorFactory
          .calculateMeasurementValue(instrument, dataSet, timeReference,
            variable, childType, allMeasurements, allSensorValues, conn));
      }
    }

    WeightedMeanCalculator mean = new WeightedMeanCalculator();
    childMeasurementValues.forEach(x -> mean.add(x.getCalculatedValue(),
      Double.valueOf(x.getMemberCount())));

    char valueType = PlotPageTableValue.MEASURED_TYPE;

    if (childMeasurementValues.stream()
      .filter(c -> c.getType() == PlotPageTableValue.INTERPOLATED_TYPE)
      .findAny().isPresent()) {

      valueType = PlotPageTableValue.INTERPOLATED_TYPE;
    }

    boolean interpolatesOverFlags = childMeasurementValues.stream()
      .anyMatch(cmv -> cmv.interpolatesAroundFlag());

    try {
      return new MeasurementValue(requiredSensorType,
        getSensorValues(childMeasurementValues, allSensorValues), null,
        interpolatesOverFlags, allSensorValues, mean.getWeightedMean(),
        (int) Math.floor(mean.getSumOfWeights()), valueType);
    } catch (RoutineException e) {
      throw new MeasurementValueCalculatorException(
        "Error extracting QC information", e);
    }
  }
}
