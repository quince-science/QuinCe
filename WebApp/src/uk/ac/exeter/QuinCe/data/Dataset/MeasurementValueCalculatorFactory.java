package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class MeasurementValueCalculatorFactory {

  public static MeasurementValue calculateMeasurementValue(
    Instrument instrument, DataSet dataSet, Measurement measurement,
    SensorType coreSensorType, SensorType requiredSensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException {

    return getCalculator(requiredSensorType).calculate(instrument, dataSet,
      measurement, coreSensorType, requiredSensorType, allMeasurements,
      allSensorValues, conn);
  }

  private static MeasurementValueCalculator getCalculator(SensorType sensorType)
    throws MeasurementValueCalculatorException {

    try {
      MeasurementValueCalculator result;

      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      if (sensorType.getShortName()
        .equals("Equilibrator Pressure (differential)")) {
        result = new DiffEqPresMeasurementValueCalculator();
      } else if (sensorType.getShortName().equals("xCO₂ (with standards)")) {
        result = new XCO2MeasurementValueCalculator();
      } else if (sensorConfig.isParent(sensorType)) {
        result = new ParentSensorTypeMeasurementValueCalculator();
      } else {
        result = new DefaultMeasurementValueCalculator();
      }

      return result;
    } catch (Exception e) {
      throw new MeasurementValueCalculatorException(
        "Error getting MeasurementValueCalculator for "
          + sensorType.getShortName(),
        e);
    }
  }
}
