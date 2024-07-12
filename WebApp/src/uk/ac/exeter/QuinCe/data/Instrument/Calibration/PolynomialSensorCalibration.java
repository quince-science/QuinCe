package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * A calibration that applies up to a fifth-order polynomial adjustment
 */
public class PolynomialSensorCalibration extends SensorCalibration {

  /**
   * The name of the Intercept coefficient
   */
  private static final String INTERCEPT_NAME = "Intercept";

  /**
   * Contains the labels for the polynomial curve parameters (constructed in the
   * {@code static} block)
   */
  private static List<String> valueNames;

  static {
    valueNames = new ArrayList<String>(5);
    valueNames.add("x⁵");
    valueNames.add("x⁴");
    valueNames.add("x³");
    valueNames.add("x²");
    valueNames.add("x");
    valueNames.add(INTERCEPT_NAME);
  }

  /**
   * Create a calibration object
   *
   * @param instrumentId
   *          The instrument to which the calibration target belongs
   * @param target
   *          The calibration target (most likely a sensor)
   */
  public PolynomialSensorCalibration(Instrument instrument, String target) {
    super(instrument, target);
  }

  /**
   * Create a calibration object with no target set
   *
   * @param instrumentId
   *          The instrument to which the calibration target belongs
   */
  public PolynomialSensorCalibration(Instrument instrument, long id,
    LocalDateTime date) {
    super(instrument, id, date);
  }

  /**
   * Construct a complete sensor calibration object.
   *
   * @param id
   *          The calibration's database ID
   * @param instrumentId
   *          The instrument ID
   * @param target
   *          The target sensor
   * @param deploymentDate
   *          The deployment date
   * @param coefficients
   *          The calibration coefficients
   * @throws CalibrationException
   *           If the calibration details are invalid
   */
  public PolynomialSensorCalibration(long id, Instrument instrument,
    String target, LocalDateTime deploymentDate,
    Map<String, String> coefficients) throws CalibrationException {
    super(id, instrument, target, deploymentDate, coefficients);
  }

  /**
   * Copy constructor.
   *
   * @param source
   *          The copy source.
   * @throws CalibrationException
   */
  protected PolynomialSensorCalibration(PolynomialSensorCalibration source)
    throws CalibrationException {
    super(source.getId(), source.getInstrument(), source.getTarget(),
      source.getDeploymentDate(), duplicateCoefficients(source));
  }

  @Override
  public List<String> getCoefficientNames(boolean includeHidden) {
    return valueNames;
  }

  @Override
  protected String buildHumanReadableCoefficients() {
    StringBuilder result = new StringBuilder();
    getCoefficients().forEach(c -> appendCoefficient(result, c));
    return result.toString();
  }

  /**
   * Add a coefficient to the human readable coefficients string
   *
   * @param string
   *          The string being constructed
   * @param coefficient
   *          The coefficient
   */
  private void appendCoefficient(StringBuilder string,
    CalibrationCoefficient coefficient) {

    String name = coefficient.getName();
    double value = Double.parseDouble(coefficient.getValue());

    if (string.length() == 0 && value != 0) {
      string.append(value);

      if (!name.equals(INTERCEPT_NAME)) {
        string.append(name);
      }
    } else if (value != 0) {
      if (value > 0) {
        string.append(" + ");
      } else if (value < 0) {
        string.append(" - ");
      }

      string.append(Math.abs(value));
      if (!name.equals(INTERCEPT_NAME)) {
        string.append(name);
      }
    }
  }

  @Override
  public boolean coefficientsValid() {
    return coefficients.size() == 6;
  }

  @Override
  protected void initialiseCoefficients() {
    super.initialiseCoefficients();

    // Default to a linear 1:1 relationship, i.e. no change.
    setCoefficient("x", "1.0");
  }

  @Override
  public Double calibrateValue(Double rawValue) {

    Double calibratedValue = 0d;

    // The first coefficient is the 5th power. We go down to the intercept
    // (zeroth power)
    int power = 5;

    for (CalibrationCoefficient coefficient : coefficients) {
      calibratedValue += coefficient.getDoubleValue()
        * Math.pow(rawValue, power);
      power--;
    }

    return calibratedValue;
  }

  @Override
  public Calibration makeCopy() {
    try {
      return new PolynomialSensorCalibration(this);
    } catch (CalibrationException e) {
      // This shouldn't happen, because it implies that we successfully created
      // in invalid object
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getCoefficientsLabel() {
    return "Formula";
  }
}
