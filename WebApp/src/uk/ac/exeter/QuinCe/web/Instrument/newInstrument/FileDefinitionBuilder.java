package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.data.Instrument.InvalidSeparatorException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.HighlightedStringException;
import uk.ac.exeter.QuinCe.web.html.HtmlUtils;

/**
 * This is a specialised instance of the InstrumentFile class
 * that provides special functions used only when an instrument
 * is being defined.
 *
 * @author Steve Jones
 *
 */
public class FileDefinitionBuilder extends FileDefinition {

  /**
   * The maximum number of lines from an uploaded file to be stored
   */
  protected static final int MAX_DISPLAY_LINES = 250;

  /**
   * The number of lines to search in order to determine
   * the file's column separator
   */
  private static final int SEPARATOR_SEARCH_LINES = 10;

  /**
   * The default description for new files
   */
  private static final String DEFAULT_DESCRIPTION = "Data File";

  /**
   * The contents of the uploaded sample file, as an array of Strings
   */
  private List<String> fileContents = null;

  /**
   * Create a new file definition with the default description
   * @param fileSet The file set that will contain this file definition
   */
  public FileDefinitionBuilder(InstrumentFileSet fileSet) {
    super(DEFAULT_DESCRIPTION, fileSet);

    int counter = 1;
    while (fileSet.containsFileDescription(getFileDescription())) {
      counter++;
      setFileDescription(DEFAULT_DESCRIPTION + " " + counter);
    }
  }

  /**
   * Create a new file definition with a specified description
   * @param fileDescription The file description
   * @param fileSet The file set that will contain this file definition
   */
  public FileDefinitionBuilder(String fileDescription, InstrumentFileSet fileSet) {
    super(fileDescription, fileSet);
  }

  /**
   * Determines whether or not file data has been uploaded for this instrument file
   * @return {@code true} if file data has been uploaded; {@code false} if it has not.
   */
  public boolean getHasFileData() {
    return (null != fileContents);
  }

  /**
   * Guess the layout of the file from its contents
   * @see #calculateColumnCount()
   */
  public void guessFileLayout() {
    try {
      // Look at the last few lines. Find the most common separator character,
      // and the maximum number of columns in each line
      setSeparator(getMostCommonSeparator());

      // Work out the likely number of columns in the file from the last few lines
      super.setColumnCount(calculateColumnCount());

      // Now start at the beginning of the file, looking for rows containing the
      // maximum column count. Start rows that don't contain this are considered
      // to be the header.
      int currentRow = 0;
      boolean columnsRowFound = false;
      while (!columnsRowFound && currentRow < fileContents.size()) {
        if (countSeparatorInstances(getSeparator(), fileContents.get(currentRow)) + 1 == getColumnCount()) {
          columnsRowFound = true;
        } else {
          currentRow++;
        }
      }

      setHeaderLines(currentRow);
      if (currentRow > 0) {
        setHeaderEndString(fileContents.get(currentRow - 1));
      }

      // Finally, find the line row that's mostly numeric. This is the first proper data line.
      // Any lines between the header and this are column header rows
      boolean dataFound = false;
      while (!dataFound && currentRow < fileContents.size()) {
        String numbers = fileContents.get(currentRow).replaceAll("[^0-9]", "");
        if (numbers.length() > (fileContents.get(currentRow).length() / 2)) {
          dataFound = true;
        } else {
          currentRow++;
        }
      }

      setColumnHeaderRows(currentRow - getHeaderLines());

    } catch (Exception e) {
      // Any exceptions mean that the guessing failed. We don't need to take
      // any action because it will be left up to the user to specify the format
      // manually.
    }
  }

  /**
   * Get the {@link #MAX_DISPLAY_LINES} of the file data as a JSON string,
   * to be used in previewing the file contents
   * @return The file preview data
   */
  public String getFilePreview() {
    String result = null;

    if (null != fileContents) {
      int lines = MAX_DISPLAY_LINES - 1;
      if (lines > fileContents.size()) {
        lines = fileContents.size();
      }
      result = HtmlUtils.makeJSONArray(fileContents.subList(0, lines));
    }

    return result;
  }

  /**
   * Search a string to find the most commonly occurring valid separator value
   * @return The most common separator in the string
   */
  private String getMostCommonSeparator() {
    String mostCommonSeparator = null;
    int mostCommonSeparatorCount = 0;

    for (String separator : VALID_SEPARATORS) {
      int matchCount = calculateColumnCount(separator);

      if (matchCount > mostCommonSeparatorCount) {
        mostCommonSeparator = separator;
        mostCommonSeparatorCount = matchCount;
      }
    }

    return mostCommonSeparator;
  }

  /**
   * Count the number of instances of a given separator in a string
   * @param separator The separator to search for
   * @param searchString The string to be searched
   * @return The number of separators found in the string
   */
  private int countSeparatorInstances(String separator, String searchString) {

    Pattern searchPattern;

    if (separator.equals(" ")) {
      // Space separators come in groups, so count consecutive spaces as one instance
      searchPattern = Pattern.compile(" +");
    } else {
      searchPattern = Pattern.compile(separator);
    }

    Matcher matcher = searchPattern.matcher(searchString.trim());

    int matchCount = 0;
    while (matcher.find()) {
      matchCount++;
    }

    return matchCount;
  }

  /**
   * Store the file data as an array of Strings
   * @param fileContents The file data
   */
  public void setFileContents(List<String> fileContents) {
    this.fileContents = fileContents;
  }

  /**
   * Create a deep copy of a {@code FileDefinitionBuilder} object.
   * @param source The source object
   * @return The copied object
   */
  public static FileDefinitionBuilder copy(FileDefinitionBuilder source) {

    FileDefinitionBuilder dest = new FileDefinitionBuilder(source.getFileDescription(), (NewInstrumentFileSet) source.getFileSet());

    try {
      dest.setHeaderType(source.getHeaderType());
      dest.setHeaderLines(source.getHeaderLines());
      dest.setHeaderEndString(source.getHeaderEndString());
      dest.setColumnHeaderRows(source.getColumnHeaderRows());
      dest.setSeparator(source.getSeparator());

      dest.fileContents = new ArrayList<String>(source.fileContents.size());
      for (String sourceLine : source.fileContents) {
        dest.fileContents.add(sourceLine);
      }

    } catch (Exception e) {
      // Since we're copying an existing object that must already be valid,
      // we can safely swallow exceptions
    }

    return dest;
  }

  /**
   * Get the number of columns in the file.
   *
   * @return The number of columns in the file
   * @see #calculateColumnCount(String)
   */
  public int calculateColumnCount() {

    int result;

    if (null == getSeparator() || null == fileContents)
      result = 0;
    else {
      result = calculateColumnCount(getSeparator());
    }

    return result;
  }

  /**
   * Get the number of columns in a file using the specified separator.
   *
   * This states the hypothetical number of columns found in the file
   * if the specified separator is used.
   *
   * This counts is based on the number of separators found per line
   * in the last few lines of the file. The largest number of columns
   * is used, because some instruments report diagnostic information on shorter lines.
   *
   * @param separator The separator
   * @return The number of columns
   * @see #SEPARATOR_SEARCH_LINES
   * @see #getMostCommonSeparator()
   */
   public int calculateColumnCount(String separator) {
    int maxColumnCount = 0;

    int firstSearchLine = fileContents.size() - SEPARATOR_SEARCH_LINES;
    if (firstSearchLine < 0) {
      firstSearchLine = 0;
    }
    for (int i = firstSearchLine; i < fileContents.size(); i++) {
      int separatorCount = countSeparatorInstances(separator, fileContents.get(i));
      if (separatorCount > maxColumnCount) {
        maxColumnCount = separatorCount + 1;
      }
    }

    return maxColumnCount;
  }

  /**
   * Dummy set column method for bean requirements.
   * We don't allow external agencies to set this,
   * but it's needed for bean compatibility
   * @param columnCount The column count
   */
  @Override
  public void setColumnCount(int columnCount) {
    // Do nothing
  }

  /**
   * Get the set of column definitions for this file definition as a JSON array
   * <p>
   *   If the file has column header rows, the column names will
   *   be taken from the first of those rows. Otherwise, they will be
   *   Column 1, Column 2 etc.
   * </p>
   *
   * @return The file columns
   */
  public String getFileColumns() {

    // TODO Only regenerate the columns when the file spec is changed. Don't do it
    //      on demand like this.
    List<String> columns = new ArrayList<String>();

    if (getColumnHeaderRows() == 0) {
      for (int i = 0; i < getColumnCount(); i++) {
        columns.add("Column " + (i + 1));
      }
    } else {
      String columnHeaders = fileContents.get(getFirstColumnHeaderRow());
      columns = extractFields(columnHeaders);
    }

    StringBuilder result = new StringBuilder();

    result.append('[');

    for (int i = 0; i < columns.size(); i++) {
      result.append('"');
      result.append(columns.get(i).replaceAll("'", "\\'"));
      result.append('"');

      if (i < columns.size() - 1) {
        result.append(',');
      }
    }

    result.append(']');

    return result.toString();
  }

  /**
   * Return the row number of the first column header row in the file
   * @return The first column header row, or -1 if the file does not have column headers
   */
  public int getFirstColumnHeaderRow() {
    int result = -1;

    if (getColumnHeaderRows() > 0) {
      // The column headers are the first row
      // after the file header. If we get the file header length,
      // then the row index is the same because it's zero based.
      // It's useful sometimes...
      result = getHeaderLength();
    }

    return result;
  }

  /**
   * Get the data from the sample file as a JSON string
   * @return The file data
   */
  public String getJsonData() {

    StringBuilder result = new StringBuilder();

    result.append('[');

    int firstDataRow = getHeaderLength() + getColumnHeaderRows();
    int lastRow = firstDataRow + MAX_DISPLAY_LINES;
    if (lastRow > fileContents.size()) {
      lastRow = fileContents.size();
    }

    for (int i = firstDataRow; i < lastRow; i++) {
      List<String> columnValues = extractFields(fileContents.get(i));

      result.append('[');

      for (int j = 0; j < getColumnCount(); j++) {

        result.append('"');

        // We can't guarantee that every column has data, so
        // fill in empty strings for unused columns
        if (j < columnValues.size()) {
          result.append(columnValues.get(j).replaceAll("'", "\\'"));
        }

        result.append('"');
        if (j < getColumnCount() - 1) {
          result.append(',');
        }
      }

      result.append(']');
      if (i < lastRow - 1) {
        result.append(',');
      }
    }


    result.append(']');

    return result.toString();
  }

  @Override
  public void setSeparatorName(String separatorName) throws InvalidSeparatorException {
    super.setSeparatorName(separatorName);
    super.setColumnCount(calculateColumnCount());
  }

  /**
   * Get the unique values from a column
   * @param column The column index
   * @return The unique values
   */
  protected TreeSet<String> getUniqueColumnValues(int column) {

    TreeSet<String> values = new TreeSet<String>();

    for (int i = getColumnHeaderRows(); i < fileContents.size(); i++) {
      List<String> columns = extractFields(fileContents.get(i));
      values.add(columns.get(column));
    }

    return values;
  }

  @Override
  public void setRunTypeColumn(int runTypeColumn) {
    super.setRunTypeColumn(runTypeColumn);

    if (runTypeColumn != -1) {
      for (String runType : getUniqueColumnValues(runTypeColumn)) {
        setRunTypeCategory(runType, RunTypeCategory.IGNORED_CATEGORY);
      }
    }
  }

  /**
   * Shortcut method to get the length of the header
   * @return The header length
   */
  private int getHeaderLength() {
    return getHeaderLength(fileContents);
  }

  /**
   * Get the header line from a file that contains the given prefix and suffix.
   * A line will match if it contains the prefix, followed by a
   * number of characters, followed by the suffix.
   * <p>
   *   The matching line will be returned as a {@link HighlightedString},
   *   with the portion between the prefix and suffix highlighted.
   * </p>
   * <p>
   *   If multiple lines match the prefix and suffix, the first line
   *   will be returned.
   * </p>
   * @param prefix The prefix
   * @param suffix The suffix
   * @return The matching line
   * @throws HighlightedStringException If an error occurs while building the highlighted string
   */
  public HighlightedString getHeaderLine(String prefix, String suffix) throws HighlightedStringException {
    return super.getHeaderLine(fileContents, prefix, suffix);
  }
}
