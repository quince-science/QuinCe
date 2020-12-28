package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;

public class CalculationParameter extends ColumnHeading {

  /**
   * Indicates whether or not this parameter should be treated as a result of
   * the calculation, as opposed to an intermediate value.
   */
  private final boolean result;

  /**
   * Indicates whether or not this is an interpolated sensor value.
   */
  private final boolean interpolated;

  public CalculationParameter(long id, String name, String columnName,
    String columnCode, String units, boolean result, boolean interpolated) {

    // A result field will have a QC value
    super(id, name, columnName, columnCode, units, result);
    this.result = result;
    this.interpolated = interpolated;
  }

  /**
   * Determine whether or not this parameter is a result of the calculation, as
   * opposed to an intermediate value.
   *
   * @return {@code true} if it is a result; {@code false} otherwise.
   */
  public boolean isResult() {
    return result;
  }

  /**
   * Determine whether or not this parameter is an interpolated sensor value.
   *
   * @return {@code true} if it is a interpolated; {@code false} otherwise.
   */
  public boolean isInterpolated() {
    return interpolated;
  }
}
