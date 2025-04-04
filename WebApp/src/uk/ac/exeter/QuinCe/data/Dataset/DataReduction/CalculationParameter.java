package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;

/**
 * Represents a data column that contains a value calculated during data
 * reduction.
 *
 * <p>
 * These columns do not contain 'normal' database IDs. Instead they use
 * constructed IDs that tie them to the
 * {@link uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable} for
 * which the data reduction is being performed. See
 * {@link DataReducer#makeParameterId(int)}.
 * </p>
 */
public class CalculationParameter extends ColumnHeading {

  /**
   * Indicates whether or not this parameter should be treated as a result of
   * the data reduction as opposed to an intermediate value.
   */
  private final boolean result;

  /**
   * Complete constructor.
   *
   * @param id
   *          The column ID.
   * @param name
   *          The short parameter name.
   * @param columnName
   *          The long parameter name.
   * @param columnCode
   *          The parameter code.
   * @param units
   *          The units of the values in the column.
   * @param result
   *          Whether this represents the final result of data reduction or an
   *          intermediate value.
   */
  public CalculationParameter(long id, String name, String columnName,
    String columnCode, String units, boolean result) {

    // A result field will have a QC value
    super(id, name, columnName, columnCode, units, result, false);
    this.result = result;
  }

  /**
   * Determine whether or not this parameter is a result of the data reduction
   * as opposed to an intermediate value.
   *
   * @return {@code true} if it is a result; {@code false} otherwise.
   */
  public boolean isResult() {
    return result;
  }
}
