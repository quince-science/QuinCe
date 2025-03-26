package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class DiffEqPresMeasurementValueCalculator
  extends MeasurementValueCalculator {

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    SensorValuesListValue timeReference, Variable variable,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();
      SensorType pressAtInstrumentSensorType = sensorConfig
        .getSensorType("Pressure at instrument");

      MeasurementValue diffEqPress = new DefaultMeasurementValueCalculator()
        .calculate(instrument, dataSet, timeReference, variable,
          requiredSensorType, allMeasurements, allSensorValues, conn);

      MeasurementValue pressAtInstrument = new DefaultMeasurementValueCalculator()
        .calculate(instrument, dataSet, timeReference, variable,
          pressAtInstrumentSensorType, allMeasurements, allSensorValues, conn);

      Double finalPressure = pressAtInstrument.getCalculatedValue()
        + diffEqPress.getCalculatedValue();

      List<SensorValue> usedSensorValues = getSensorValues(allSensorValues,
        diffEqPress);
      usedSensorValues
        .addAll(getSensorValues(allSensorValues, pressAtInstrument));

      return new MeasurementValue(requiredSensorType, usedSensorValues, null,
        MeasurementValue.interpolatesAroundFlag(diffEqPress, pressAtInstrument),
        allSensorValues, finalPressure, diffEqPress.getMemberCount(),
        diffEqPress.getType());

    } catch (RoutineException e) {
      throw new MeasurementValueCalculatorException(
        "Unable to extract QC flag information", e);
    } catch (SensorTypeNotFoundException e) {
      throw new MeasurementValueCalculatorException(
        "Invalid sensors configuration", e);
    }
  }
}
