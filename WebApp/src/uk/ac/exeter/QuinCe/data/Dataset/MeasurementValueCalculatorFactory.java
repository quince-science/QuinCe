package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Factory class for getting {@link MeasurementValueCalculator} objects for a
 * given {@link SensorType}.
 *
 * <p>
 * Note that certain {@link MeasurementValueCalculator}s are usable for multiple
 * {@link SensorType}s, so that still needs to be passed to the calculator on
 * use.
 * </p>
 */
public class MeasurementValueCalculatorFactory {

  /**
   * Calculate a {@link MeasurementValue} for a given {@link Measurement} and
   * {@link SensorType}.
   *
   * <p>
   * This acquires the correct {@link MeasurementValueCalculator} for the
   * {@link SensorType} and uses it to calculate the required
   * {@link MeasurementValue}.
   * </p>
   *
   * @param instrument
   *          The {@link Instrument} for which measurements are being
   *          calculated.
   * @param dataSet
   *          The {@link DataSet} for which measurements are being calculated.
   * @param timeReference
   *          A {link SensorValuesListValue} containing details of the time
   *          period to be considered when building the {@link MeasuremntValue}.
   * @param variable
   *          The {@link Variable} for the current {@link Measurement}.
   * @param requiredSensorType
   *          The {@link SensorType} for which a value is required.
   * @param allMeasurements
   *          The complete set of {@link Measurements} for the current
   *          {@link DataSet}.
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s for the current
   *          {@link DataSet}.
   * @param conn
   *          A database connection.
   * @return The calculated {@link MeasurementValue}.
   * @throws MeasurementValueCalculatorException
   *           If the value cannot be constructed.
   */
  public static MeasurementValue calculateMeasurementValue(
    Instrument instrument, DataSet dataSet, SensorValuesListValue timeReference,
    Variable variable, SensorType requiredSensorType,
    DatasetMeasurements allMeasurements, DatasetSensorValues allSensorValues,
    Connection conn) throws MeasurementValueCalculatorException {

    return getCalculator(requiredSensorType).calculate(instrument, dataSet,
      timeReference, variable, requiredSensorType, allMeasurements,
      allSensorValues, conn);
  }

  /**
   * Obtain the {@link MeasurementValueCalculator} for the specified
   * {@link SensorType}.
   *
   * @param sensorType
   *          The {@link SensorType}.
   * @return The {@link MeasurementValueCalculator}.
   * @throws MeasurementValueCalculatorException
   *           If the calculator cannot be retrieved.
   */
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
      case "SubCTech xCO₂": {
        result = new SubCTechXCO2MeasurementValueCalculator();
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
