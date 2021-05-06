package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public interface MeasurementLocator {

  /**
   * Get the Measurements from the dataset that apply to the variable(s) that
   * this locator handles.
   *
   * @return The measurements
   */
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException;

  /**
   * Get the {@link MeasurementLocator} for the specified variable.
   *
   * @param variable
   *          The variable
   * @return The measurement locator
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
      default: {
        result = new SimpleMeasurementLocator(variable);
      }
      }
    }

    return result;
  }
}
