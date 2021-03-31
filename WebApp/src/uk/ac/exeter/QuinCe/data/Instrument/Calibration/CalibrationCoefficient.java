package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

/**
 * Simple object for a single calibration coefficient
 *
 * @author Steve Jones
 *
 */
public class CalibrationCoefficient {

  /**
   * The coefficient's name
   */
  private String name;

  /**
   * The coefficient value
   */
  private String value = "0";

  /**
   * Creates an empty (zero) coefficient
   *
   * @param name
   *          The coefficient name
   */
  protected CalibrationCoefficient(String name) {
    this.name = name;
  }

  /**
   * Create a coefficient with a value
   *
   * @param name
   *          The coefficient name
   * @param value
   *          The value
   */
  protected CalibrationCoefficient(String name, String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Get the coefficient name
   *
   * @return The coefficient's name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the coefficient value
   *
   * @return The value
   */
  public String getValue() {
    return value;
  }

  public Double getDoubleValue() {
    return Double.parseDouble(value);
  }

  /**
   * Set the coefficient value
   *
   * @param value
   *          The value
   */
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return name + ": " + value;
  }
}
