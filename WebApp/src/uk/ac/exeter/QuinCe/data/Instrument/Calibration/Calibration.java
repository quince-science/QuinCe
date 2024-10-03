package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Base class for a calibration.
 *
 * <p>
 * {@code Calibration} classes can be used for external standards, sensor
 * calibrations or external calibrations.
 * </p>
 *
 * <p>
 * All calibrations will be held in the same table in the database,
 * distinguished by a {@code type} field
 * </p>
 *
 * <p>
 * Comparison operations on this class compare the instrument ID, type and
 * target in that order.
 * </p>
 */
public abstract class Calibration implements Comparable<Calibration> {

  /**
   * The database ID of this calibration
   */
  private long id;

  /**
   * The instrument to which this calibration belongs
   */
  protected Instrument instrument;

  /**
   * The calibration type
   */
  protected String type = null;

  /**
   * The date and time of the deployment. Some calibrations do not have a time,
   * in which case the time portion will be set to midnight.
   */
  private LocalDateTime deploymentDate = LocalDateTime.now(ZoneOffset.UTC)
    .truncatedTo(ChronoUnit.DAYS);

  /**
   * The part of the instrument to which this calibration applies. Examples are
   * the name of an external standard, sensor etc.
   */
  private String target = null;

  /**
   * The values for the calibration. The list must contain the same number of
   * entries as the list of value names returned by
   * {@link #getCoefficientNames()}.
   *
   * <p>
   * <b>Note:</b> You should not use this variable directly unless you have a
   * good reason to. You should instead use {@link #getCoefficients()}, which
   * automatically takes care of initialise them if they haven't been already.
   * </p>
   */
  protected List<CalibrationCoefficient> coefficients = null;

  /**
   * Create an empty calibration for an instrument.
   *
   * @param instrumentId
   *          The instrument's database ID.
   * @param type
   *          The calibration type.
   */
  protected Calibration(Instrument instrument, String type, long id,
    LocalDateTime deploymentDate) {

    this.id = id;
    this.instrument = instrument;
    this.type = type;
    this.deploymentDate = deploymentDate;
  }

  /**
   * Create an empty calibration for a specified target
   *
   * @param instrumentId
   *          The instrument ID
   * @param type
   *          The calibration type
   * @param target
   *          The target
   */
  protected Calibration(Instrument instrument, String type, String target) {
    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.instrument = instrument;
    this.type = type;
    this.target = target;
  }

  protected Calibration(long id, Instrument instrument, String type,
    String target) {
    this.id = id;
    this.instrument = instrument;
    this.type = type;
    this.target = target;
  }

  /**
   * Get the human-readable names of the values to be stored for the calibration
   *
   * @return The value names
   */
  public abstract List<String> getCoefficientNames(boolean includeHidden);

  /**
   * Get the type of the calibration. This is provided by each of the concrete
   * implementations of the class
   *
   * @return The calibration type
   */
  public String getType() {
    return type;
  }

  /**
   * Get the calibration values as a human-readable string.
   *
   * <p>
   * If either the deployment date or coefficients are {@code null}, the method
   * assumes that the coefficients are not set and returns a default
   * {@code "Not set"} value.
   * </p>
   *
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
   *
   * @return The human-readable coefficients
   */
  protected String buildHumanReadableCoefficients() {

    String result = "Not set";

    List<CalibrationCoefficient> editableCoefficients = getEditableCoefficients();

    if (editableCoefficients.size() == 1) {
      result = editableCoefficients.get(0).getValue();
    } else if (size() > 1) {
      result = editableCoefficients.stream().map(c -> c.toString())
        .collect(Collectors.joining("; "));
    }

    return result;

  }

  /**
   * Get the calibration target
   *
   * @return The target
   */
  public String getTarget() {
    return target;
  }

  /**
   * Set the calibration target
   *
   * @param target
   *          The target
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * Get the deployment date as a {@link LocalDateTime} object
   *
   * @return The deployment date
   */
  public LocalDateTime getDeploymentDate() {
    return deploymentDate;
  }

  /**
   * Set the deployment date
   *
   * @param deploymentDate
   *          The deployment date
   */
  public void setDeploymentDate(LocalDateTime deploymentDate) {
    this.deploymentDate = deploymentDate;

    if (null == coefficients) {
      initialiseCoefficients();
    }
  }

  /**
   * Get the database ID of the instrument to which this calibration applies
   *
   * @return The instrument ID
   */
  public Instrument getInstrument() {
    return instrument;
  }

  /**
   * Get the calibration values as a semicolon-delimited list
   *
   * @return The calibration values
   */
  public String getCoefficientsAsDelimitedList() {
    if (null == coefficients) {
      initialiseCoefficients();
    }

    List<String> values = new ArrayList<String>(coefficients.size());

    for (CalibrationCoefficient coefficient : coefficients) {
      values.add(coefficient.getValue());
    }

    return StringUtils.collectionToDelimited(values, ";");
  }

  /**
   * Initialise the coefficients for this calibration with zero values
   */
  protected void initialiseCoefficients() {
    coefficients = getCoefficientNames(true).stream()
      .map(n -> new CalibrationCoefficient(n)).collect(Collectors.toList());
  }

  /**
   * Get the coefficients for this calibration
   *
   * @return The coefficients
   */
  public List<CalibrationCoefficient> getCoefficients() {
    if (null == coefficients) {
      initialiseCoefficients();
    }
    return coefficients;
  }

  /**
   * Get the list of coefficients that are user-editable. For most calibrations
   * this will be the complete set
   *
   * @return The user-editable calibration coefficients
   */
  public List<CalibrationCoefficient> getEditableCoefficients() {
    return getCoefficients();
  }

  /**
   * Set the coefficients for this calibration
   *
   * @param coefficients
   *          The coefficients
   * @throws CalibrationException
   *           If an incorrect number of coefficients is supplied
   */
  public void setCoefficients(Map<String, String> newCoefficients)
    throws CalibrationException {

    if (newCoefficients.size() != getCoefficientNames(true).size()) {
      throw new CalibrationException(
        "Incorrect number of coefficients: expected "
          + getCoefficientNames(true).size() + ", got "
          + newCoefficients.size());
    }

    initialiseCoefficients();

    newCoefficients.entrySet()
      .forEach(e -> setCoefficient(e.getKey(), e.getValue()));
  }

  protected void setCoefficients(List<CalibrationCoefficient> newCoefficients)
    throws CalibrationException {

    List<String> coefficientNames = getCoefficientNames(true);

    if (newCoefficients.size() != coefficientNames.size()) {
      throw new CalibrationException("Incorrect number of coefficients");
    }

    List<String> newNames = newCoefficients.stream().map(c -> c.getName())
      .toList();
    if (!newNames.containsAll(coefficientNames)) {
      throw new CalibrationException(
        "Invalid coefficient names for this Calibration");
    }

    this.coefficients = newCoefficients;
  }

  /**
   * Check to ensure that this calibration is valid.
   *
   * <p>
   * To pass validation, both a {@link #deploymentDate} and
   * {@link #coefficients} must be present, and the coefficients must be valid.
   * </p>
   *
   * @return {@code true} if the calibration is valid; {@code false} if it is
   *         not
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
   *
   * @return {@code true} if the coefficients are valid; {@code false} if they
   *         are not
   */
  public abstract boolean coefficientsValid();

  @Override
  public int compareTo(Calibration o) {
    int result = (int) (this.instrument.getId() - o.instrument.getId());

    if (result == 0) {
      result = this.type.compareTo(o.type);
    }

    if (result == 0) {
      result = this.target.compareTo(o.target);
    }

    if (result == 0) {
      result = this.deploymentDate.compareTo(o.deploymentDate);
    }

    return result;
  }

  /**
   * Get the value of a named coefficient
   *
   * @param name
   *          The coefficient name
   * @return The coefficient value
   */
  public Double getDoubleCoefficient(String name) {
    Double result = null;

    for (CalibrationCoefficient coefficient : getCoefficients()) {
      if (coefficient.getName().equals(name)) {
        result = Double.parseDouble(coefficient.getValue());
        break;
      }
    }

    return result;
  }

  public BigDecimal getBigDecimalCoefficient(String name) {
    BigDecimal result = null;

    for (CalibrationCoefficient coefficient : getCoefficients()) {
      if (coefficient.getName().equals(name)) {
        result = new BigDecimal(coefficient.getValue());
        break;
      }
    }

    return result;
  }

  /**
   * Calibrate a single value using this calibration
   *
   * @param rawValue
   *          The value to be calibrated
   * @return The calibrated value
   */
  public abstract Double calibrateValue(Double rawValue);

  /**
   * Check that this calibration is valid.
   *
   * <p>
   * Most calibrations are valid all the time, so the default implementation
   * always returns {@code true}. Override the method to provide more
   * sophisticated logic.
   * </p>
   *
   * @return {@code true} if the calibration is valid; {@code false} if it is
   *         not.
   */
  public boolean isValid() {
    return true;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();

    result.append(target);
    result.append(':');

    if (null == coefficients) {
      result.append("null");
    } else {
      coefficients.stream().forEach(c -> {
        result.append(c.getName());
        result.append('=');
        result.append(c.getValue());
        result.append(';');
      });
    }

    return result.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(coefficients, deploymentDate, id, target);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Calibration other = (Calibration) obj;
    return CollectionUtils.isEqualCollection(coefficients, other.coefficients)
      && Objects.equals(deploymentDate, other.deploymentDate) && id == other.id
      && Objects.equals(target, other.target);
  }

  protected void setCoefficient(String name, String value) {
    getCoefficients().get(getCoefficientIndex(name)).setValue(value);
  }

  protected int size() {
    return getCoefficients().size();
  }

  private int getCoefficientIndex(String name) {
    int result = -1;

    int searchIndex = 0;
    while (result == -1 && searchIndex < coefficients.size()) {
      if (coefficients.get(searchIndex).getName().equals(name)) {
        result = searchIndex;
      }
      searchIndex++;
    }

    return result;
  }

  public String getCoefficientsJson() {
    JsonObject json = new JsonObject();
    coefficients.forEach(c -> json.addProperty(c.getName(), c.getValue()));
    return json.toString();
  }

  public boolean isSet() {
    return null != coefficients;
  }

  /**
   * Get the label to use for the set of coefficients.
   *
   * @return The coefficients label.
   */
  public abstract String getCoefficientsLabel();

  public String getJsonCoefficientsLabel() {
    return getCoefficientsLabel().toLowerCase();
  }

  /**
   * Create a copy of this {@code Calibration} object.
   *
   * <p>
   * This performs much the same operation as {@link Object#clone} but works
   * around the {@code abstract} nature of this class.
   * </p>
   *
   * @return A copy of this Calibration.
   */
  public abstract Calibration makeCopy();

  /**
   * Create a deep copy of the {@code coefficients} from the specified
   * {@link Calibration}.
   *
   * @param calibration
   *          The {@link Calibration} whose coefficients are to be duplicated.
   * @return
   */
  protected static List<CalibrationCoefficient> duplicateCoefficients(
    Calibration calibration) {

    return calibration.coefficients.stream()
      .map(c -> new CalibrationCoefficient(c.getName(), c.getValue())).toList();
  }

  public boolean hasSameEffect(Calibration other) {

    if (timeAffectsCalibration()
      && !this.deploymentDate.equals(other.deploymentDate)) {
      return false;
    } else if (!this.target.equals(other.target)) {
      return false;
    } else if (!CollectionUtils.isEqualCollection(this.coefficients,
      other.coefficients)) {
      return false;
    } else {
      return true;
    }

  }

  protected abstract boolean timeAffectsCalibration();
}
