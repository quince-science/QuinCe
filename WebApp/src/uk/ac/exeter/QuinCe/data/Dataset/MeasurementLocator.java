package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Locates measurements in a {@link DataSet}.
 *
 * <p>
 * Measurement locators will scan a {@link DataSet} to find places where
 * calculations can be made for each of its parent {@link Instrument}'s
 * {@link Variable}s.
 * </p>
 *
 * <p>
 * For the most simple {@link Variable}s, this will be any point where a value
 * exists for the core {@link SensorType} (see
 * {@link Variable#getCoreSensorType()}). However, for more complex
 * {@link Variable}s there will be other factors to consider, such as whether
 * the instrument is performing calibrations or settling after a long period
 * between measurements. The logic for these considerations is encompassed in
 * concrete implementation of this class.
 * </p>
 */
public abstract class MeasurementLocator {

  /**
   * Get the {@link Measurement}s from the {@link DataSet} that apply to the
   * {@link Variable}(s) that this locator handles.
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The parent {@link Instrument} of the {@link DataSet} being
   *          processed.
   * @param dataset
   *          The {@link DataSet} being processed.
   * @param allSensorValues
   *          The complete set of {@link SensorValue}s in the {@link DataSet}.
   * @return The located measurements.
   * @throws MeasurementLocatorException
   *           If an error occurs during the location process.
   */
  public abstract List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset, DatasetSensorValues allSensorValues)
    throws MeasurementLocatorException;

  /**
   * Get the {@link MeasurementLocator} instance for the specified variable.
   *
   * @param variable
   *          The {@link Variable}.
   * @return The measurement locator.
   */
  public static MeasurementLocator getMeasurementLocator(Variable variable) {

    MeasurementLocator result = null;

    // If the variable has internal calibrations, it has run types
    if (variable.hasInternalCalibrations()) {
      result = new RunTypeMeasurementLocator();
    } else {
      switch (variable.getName()) {
      case "CONTROS pCO₂": {
        result = new ControsPco2MeasurementLocator();
        break;
      }
      case "CONTROS pCO₂ via FerryBox": {
        result = new ControsPco2XFerryBoxMeasurementLocator();
        break;
      }
      case "Pro Oceanus CO₂ Water": {
        result = new ProOceanusCO2MeasurementLocator();
        break;
      }
      case "ASVCO₂ Water": {
        result = new ASVCO2MeasurementLocator();
        break;
      }
      case "Water Vapour Mixing Ratio": {
        result = new WaterVapourMixingRatioMeasurementLocator();
        break;
      }
      case "Pro Oceanus CO₂ Atmosphere":
      case "ASVCO₂ Atmosphere": {
        // The atmospheric measurements are automatically created by the water
        // measurement locator.
        result = new DummyMeasurementLocator();
        break;
      }
      default: {
        result = new SimpleMeasurementLocator(variable);
      }
      }
    }

    return result;
  }
}
