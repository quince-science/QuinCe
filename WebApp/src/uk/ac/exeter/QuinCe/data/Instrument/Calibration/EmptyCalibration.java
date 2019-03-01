package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.List;

/**
 * An empty calibration object, used as a placeholder when a calibration is missing
 * @author Steve Jones
 *
 */
public class EmptyCalibration extends Calibration {

  /**
   * Constructor for the basic calibration details.
   * This allows us to set the details that show the user exactly
   * which calibration is missing
   * @param instrumentId The instrument ID
   * @param type The calibration type
   * @param target The calibration target
   */
  protected EmptyCalibration(long instrumentId, String type, String target) {
    super(instrumentId, type, target);
  }

  @Override
  public List<String> getCoefficientNames() {
    return null;
  }

  @Override
  protected String buildHumanReadableCoefficients() {
    return "Not set";
  }

  @Override
  public boolean coefficientsValid() {
    return false;
  }

  @Override
  public Double calibrateValue(Double rawValue) {
    return rawValue;
  }

  @Override
  public boolean isValid() {
    boolean result = false;

    // Sensor calibrations can be empty. Others cannot.
    if (type.equals(SensorCalibrationDB.SENSOR_CALIBRATION_TYPE)) {
      result = true;
    }

    return result;
  }
}
