package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * An empty calibration object, used as a placeholder when a calibration is
 * missing
 */
public class EmptyCalibration extends Calibration {

  /**
   * Constructor for the basic calibration details. This allows us to set the
   * details that show the user exactly which calibration is missing
   *
   * @param instrumentId
   *          The instrument ID
   * @param type
   *          The calibration type
   * @param target
   *          The calibration target
   */
  protected EmptyCalibration(Instrument instrument, String type,
    String target) {
    super(instrument, type, target);
  }

  /**
   * Copy constructor.
   *
   * @param source
   *          The source of the copy.
   */
  protected EmptyCalibration(EmptyCalibration source) {
    super(source.getInstrument(), source.getType(), source.getTarget());
  }

  @Override
  public List<String> getCoefficientNames(boolean includeHidden) {
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

  @Override
  public Calibration makeCopy() {
    return new EmptyCalibration(this);
  }
}
