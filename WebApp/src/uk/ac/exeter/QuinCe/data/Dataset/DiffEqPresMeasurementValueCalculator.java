package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class DiffEqPresMeasurementValueCalculator
  extends MeasurementValueCalculator {

  @Override
  public MeasurementValue calculate(Instrument instrument, DataSet dataSet,
    Measurement measurement, SensorType coreSensorType,
    SensorType requiredSensorType, DatasetMeasurements allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn)
    throws MeasurementValueCalculatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();
      SensorType pressAtInstrumentSensorType = sensorConfig
        .getSensorType("Pressure at instrument");

      MeasurementValue diffEqPress = new DefaultMeasurementValueCalculator()
        .calculate(instrument, dataSet, measurement, coreSensorType,
          requiredSensorType, allMeasurements, allSensorValues, conn);

      MeasurementValue pressAtInstrument = new DefaultMeasurementValueCalculator()
        .calculate(instrument, dataSet, measurement, coreSensorType,
          pressAtInstrumentSensorType, allMeasurements, allSensorValues, conn);

      Double finalPressure = pressAtInstrument.getCalculatedValue()
        + diffEqPress.getCalculatedValue();

      return new MeasurementValue(requiredSensorType,
        getSensorValues(allSensorValues, diffEqPress),
        getSensorValues(allSensorValues, pressAtInstrument), finalPressure,
        diffEqPress.getMemberCount(), diffEqPress.getType());

    } catch (SensorTypeNotFoundException e) {
      throw new MeasurementValueCalculatorException(
        "Invalid sensors configuration", e);
    }
  }
}
