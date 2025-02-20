package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple class to hold a column's name and an example value
 */
public class FileColumn {

  /**
   * Common missing values to ignore when looking for example values
   */
  private static ArrayList<String> MISSING_VALUES;

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

  // Set up the list of missing values
  static {
    MISSING_VALUES = new ArrayList<String>();
    MISSING_VALUES.add("");
    MISSING_VALUES.add("nan");
    MISSING_VALUES.add("-999");
    MISSING_VALUES.add("-9999");
  }

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

  protected static List<FileColumn> makeFileColumns(List<String> names,
    List<String> exampleValues) {

    if (names.size() != exampleValues.size()) {
      throw new IndexOutOfBoundsException("Lists are not the same size");
    }

    List<FileColumn> result = new ArrayList<FileColumn>(names.size());

    for (int i = 0; i < names.size(); i++) {
      result.add(new FileColumn(i, names.get(i), exampleValues.get(i)));
    }

    return result;
  }

  protected static boolean isMissingValue(String value) {
    return MISSING_VALUES.contains(value.toLowerCase());
  }
}
