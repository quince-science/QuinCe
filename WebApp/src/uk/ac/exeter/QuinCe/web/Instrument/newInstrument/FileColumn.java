package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

/**
 * Simple class to hold a column's name and an example value
 *
 * @author Steve Jones
 *
 */
public class FileColumn {

  /**
   * Common missing values to ignore when looking for example values
   */
  protected static final String[] MISSING_VALUES = { "", "NaN", "-999",
    "-9999" };

  /**
   * The column index
   */
  private final int index;

  /**
   * The column name
   */
  private final String name;

  /**
   * An example value extracted from the file
   */
  private final String exampleValue;

  /**
   * Simple constructor to set all fields.
   *
   * @param index
   *          The column index.
   * @param name
   *          The column name.
   * @param exampleValue
   *          The example value.
   */
  protected FileColumn(int index, String name, String exampleValue) {
    this.index = index;
    this.name = name;
    this.exampleValue = exampleValue;
  }

  /**
   * Get the column index.
   *
   * @return The column index.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Get the column name.
   *
   * @return The column name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the example value from the column.
   *
   * @return The example value.
   */
  public String getExampleValue() {
    return exampleValue;
  }
}
