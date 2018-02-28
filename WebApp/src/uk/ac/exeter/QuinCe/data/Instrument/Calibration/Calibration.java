package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Base class for a calibration.
 *
 * <p>
 *   {@code Calibration} classes can be used for external standards,
 *   sensor calibrations or external calibrations.
 * </p>
 *
 * <p>
 *   All calibrations will be held in the same table in the database,
 *   distinguished by a {@code type} field
 * </p>
 *
 * <p>
 *   Comparison operations on this class compare the instrument ID,
 *   type and target in that order.
 * </p>
 *
 * @author Steve Jones
 *
 */
public abstract class Calibration implements Comparable<Calibration> {

  /**
   * The instrument to which this calibration belongs
   */
  private long instrumentId;

  /**
   * The calibration type
   */
  protected String type = null;

  /**
   * The date and time of the deployment. Some calibrations do not have a time,
   * in which case the time portion will be set to midnight.
   * @see #hasTime
   */
  private LocalDateTime deploymentDate = null;

  /**
   * The part of the instrument to which this calibration applies.
   * Examples are the name of an external standard, sensor etc.
   */
  private String target = null;

  /**
   * The values for the calibration. The list must contain
   * the same number of entries as the list of value names
   * returned by {@link #getCoefficientNames()}.
   * @see #getCoefficientNames()
   */
  protected List<CalibrationCoefficient> coefficients = null;

  /**
   * Create an empty calibration for an instrument
   * @param instrumentId The instrument's database ID
   */
  protected Calibration(long instrumentId, String type) {
    this.instrumentId = instrumentId;
    this.type = type;
  }

  /**
   * Create an empty calibration for a specified target
   * @param instrumentId The instrument ID
   * @param type The calibration type
   * @param target The target
   */
  protected Calibration(long instrumentId, String type, String target) {
    this.instrumentId = instrumentId;
    this.type = type;
    this.target = target;
  }

  /**
   * Get the human-readable names of the values to be stored for the calibration
   * @return The value names
   */
  public abstract List<String> getCoefficientNames();

  /**
   * Get the type of the calibration. This is provided
   * by each of the concrete implementations of the class
   * @return The calibration type
   */
  public String getType() {
    return type;
  }

  /**
   * Indicates whether or not the {@code deploymentDate} should
   * have a time component. Most calibrations do not have a time,
   * so the default method returns {@code false}.
   * @see #deploymentDate
   */
  protected boolean hasTime() {
    return false;
  }

  /**
   * Get the calibration values as a human-readable string.
   *
   * <p>
   *   If either the deployment date or coefficients are
   *   {@code null}, the method assumes that the coefficients
   *   are not set and returns a default {@code "Not set"} value.
   * </p>
   * @return The calibration values string
   */
  public String getHumanReadableCoefficients() {
    String result;

    if (null == deploymentDate) {
      result = "Not set";
    } else {
      if (null == coefficients) {
        result = "Not set";
      } else {
        result = buildHumanReadableCoefficients();
      }
    }

    return result;
  }

  /**
   * Build the human-readable coefficients string for
   * {@link #getHumanReadableCoefficients()}.
   * @return The human-readable coefficients
   */
  protected abstract String buildHumanReadableCoefficients();

  /**
   * Get the calibration target
   * @return The target
   */
  public String getTarget() {
    return target;
  }

  /**
   * Set the calibration target
   * @param target The target
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * Get the deployment date as a {@link LocalDateTime} object
   * @return The deployment date
   */
  public LocalDateTime getDeploymentDate() {
    return deploymentDate;
  }

  /**
   * Set the deployment date
   * @param deploymentDate The deployment date
   */
  public void setDeploymentDate(LocalDateTime deploymentDate) {
    this.deploymentDate = deploymentDate;

    if (null == coefficients) {
      initialiseCoefficients();
    }
  }

  /**
   * Get the database ID of the instrument to which this calibration applies
   * @return The instrument ID
   */
  public long getInstrumentId() {
    return instrumentId;
  }

  /**
   * Get the calibration values as a semicolon-delimited list
   * @return The calibration values
   */
  public String getCoefficientsAsDelimitedList() {
    if (null == coefficients) {
      initialiseCoefficients();
    }


    List<Double> values = new ArrayList<Double>(coefficients.size());

    for (CalibrationCoefficient coefficient : coefficients) {
      values.add(coefficient.getValue());
    }

    return StringUtils.listToDelimited(values, ";");
  }

  /**
   * Initialise the coefficients for this calibration with zero values
   */
  protected void initialiseCoefficients() {
    coefficients = new ArrayList<CalibrationCoefficient>(getCoefficientNames().size());

    for (String name : getCoefficientNames()) {
      coefficients.add(new CalibrationCoefficient(name));
    }
  }

  /**
   * Get the coefficients for this calibration
   * @return The coefficients
   */
  public List<CalibrationCoefficient> getCoefficients() {
    if (null == coefficients) {
      initialiseCoefficients();
    }
    return coefficients;
  }

  /**
   * Set the coefficients for this calibration
   * @param coefficients The coefficients
   * @throws CalibrationException If an incorrect number of coefficients is supplied
   */
  public void setCoefficients(List<Double> coefficients) throws CalibrationException {

    if (coefficients.size() != getCoefficientNames().size()) {
      throw new CalibrationException("Incorrect number of coefficients: expected " + getCoefficientNames().size() + ", got " + coefficients.size());
    }

    this.coefficients = new ArrayList<CalibrationCoefficient>(getCoefficientNames().size());

    int count = -1;
    for (String name : getCoefficientNames()) {
      count++;
      this.coefficients.add(new CalibrationCoefficient(name, coefficients.get(count)));
    }
  }

  /**
   * Check to ensure that this calibration is valid.
   *
   * <p>
   *   To pass validation, both a {@link #deploymentDate} and {@link #coefficients} must be
   *   present, and the coefficients must be valid.
   * </p>
   *
   * @return {@code true} if the calibration is valid; {@code false} if it is not
   * @see #coefficientsValid()
   */
  public boolean validate() {
    boolean valid = true;

    if (null == deploymentDate || null == coefficients) {
      valid = false;
    } else {
      valid = coefficientsValid();
    }

    return valid;
  }

  /**
   * Determine whether the calibration coefficients are valid
   * @return {@code true} if the coefficients are valid; {@code false} if they are not
   */
  public abstract boolean coefficientsValid();

  @Override
  public int compareTo(Calibration o) {
    int result = (int) (this.instrumentId - o.instrumentId);

    if (result == 0) {
      result = this.type.compareTo(o.type);
    }

    if (result == 0) {
      result = this.target.compareTo(o.target);
    }

    return result;
  }

  /**
   * Get the value of a named coefficient
   * @param name The coefficient name
   * @return The coefficient value
   */
  public Double getCoefficient(String name) {
    Double result = null;

    for (CalibrationCoefficient coefficient : getCoefficients()) {
      if (coefficient.getName().equals(name)) {
        result = coefficient.getValue();
        break;
      }
    }

    return result;
  }
}
