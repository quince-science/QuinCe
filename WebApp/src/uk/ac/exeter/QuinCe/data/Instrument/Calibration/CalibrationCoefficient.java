package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.Objects;

/**
 * Simple object for a single calibration coefficient.
 */
public class CalibrationCoefficient implements Cloneable {

  /**
   * The coefficient's name.
   */
  private String name;

  /**
   * The coefficient value.
   */
  private String value = "0";

  /**
   * Creates an empty (zero) coefficient.
   *
   * @param name
   *          The coefficient name.
   */
  protected CalibrationCoefficient(String name) {
    this.name = name;
  }

  /**
   * Create a coefficient with the specified value.
   *
   * @param name
   *          The coefficient name.
   * @param value
   *          The value.
   */
  protected CalibrationCoefficient(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Get the coefficient's name.
   *
   * @return The coefficient name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the coefficient value as a {@link String}.
   *
   * @return The value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Get the coefficient value as a {@link Double} object.
   *
   * @return The value.
   */
  public Double getDoubleValue() {
    return Double.parseDouble(value);
  }

  /**
   * Set the coefficient value.
   *
   * @param value
   *          The value.
   */
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return name + ": " + value;
  }

  @Override
  public Object clone() {
    return new CalibrationCoefficient(name, value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CalibrationCoefficient other = (CalibrationCoefficient) obj;
    return Objects.equals(name, other.name)
      && Objects.equals(value, other.value);
  }
}
