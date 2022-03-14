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
  public MeasurementValue calculate(Instrument instrument,
    Measurement measurement, SensorType sensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();
      SensorType pressAtInstrumentSensorType = sensorConfig
        .getSensorType("Pressure at instrument");

      MeasurementValue diffEqPress = new DefaultMeasurementValueCalculator()
        .calculate(instrument, measurement, sensorType, allMeasurements,
          allSensorValues, conn);

      MeasurementValue pressAtInstrument = new DefaultMeasurementValueCalculator()
        .calculate(instrument, measurement, pressAtInstrumentSensorType,
          allMeasurements, allSensorValues, conn);

      Double finalPressure = pressAtInstrument.getCalculatedValue()
        + diffEqPress.getCalculatedValue();

      return new MeasurementValue(sensorType,
        getSensorValues(allSensorValues, diffEqPress),
        getSensorValues(allSensorValues, pressAtInstrument), finalPressure,
        diffEqPress.getMemberCount());

    } catch (SensorTypeNotFoundException e) {
      throw new MeasurementValueCalculatorException(
        "Invalid sensors configuration", e);
    }
  }
}
