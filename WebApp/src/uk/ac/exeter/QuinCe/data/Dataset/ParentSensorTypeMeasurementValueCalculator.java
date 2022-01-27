package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.WeightedMeanCalculator;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ParentSensorTypeMeasurementValueCalculator
  extends MeasurementValueCalculator {

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    Measurement measurement, SensorType coreSensorType,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    List<MeasurementValue> childMeasurementValues = new ArrayList<MeasurementValue>();

    for (SensorType childType : sensorConfig.getChildren(requiredSensorType)) {
      if (instrument.getSensorAssignments().isAssigned(childType)) {
        childMeasurementValues.add(MeasurementValueCalculatorFactory
          .calculateMeasurementValue(instrument, dataSet, measurement,
            coreSensorType, childType, allMeasurements, allSensorValues, conn));
      }
    }

    WeightedMeanCalculator mean = new WeightedMeanCalculator();
    childMeasurementValues.forEach(x -> mean.add(x.getCalculatedValue(),
      Double.valueOf(x.getMemberCount())));

    return new MeasurementValue(requiredSensorType,
      getSensorValues(childMeasurementValues, allSensorValues), null,
      mean.getWeightedMean(), (int) Math.floor(mean.getSumOfWeights()));
  }
}
