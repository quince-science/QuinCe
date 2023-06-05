package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class MeasurementValueCalculatorFactory {

  public static MeasurementValue calculateMeasurementValue(
    Instrument instrument, DataSet dataSet, Measurement measurement,
    Variable variable, SensorType requiredSensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException {

    return getCalculator(requiredSensorType).calculate(instrument, dataSet,
      measurement, variable, requiredSensorType, allMeasurements,
      allSensorValues, conn);
  }

  private static MeasurementValueCalculator getCalculator(SensorType sensorType)
    throws MeasurementValueCalculatorException {

    try {
      MeasurementValueCalculator result;

      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      switch (sensorType.getShortName()) {
      case "Equilibrator Pressure (differential)": {
        result = new DiffEqPresMeasurementValueCalculator();
        break;
      }
      case "xCO₂ (with standards)": {
        result = new XCO2MeasurementValueCalculator();
        break;
      }
      case "x¹²CO₂ (with standards)":
      case "x¹³CO₂ (with standards)":
      case "x¹²CO₂ + x¹³CO₂ (with standards)": {
        result = new D12D13CMeasurementValueCalculator();
        break;
      }
      default: {
        if (sensorConfig.isParent(sensorType)) {
          result = new ParentSensorTypeMeasurementValueCalculator();
        } else {
          result = new DefaultMeasurementValueCalculator();
        }
      }
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
