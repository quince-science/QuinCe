package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.data.Instrument.InvalidInstrumentBasisException;
import uk.ac.exeter.QuinCe.data.Instrument.InvalidSeparatorException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.HighlightedStringException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.html.HtmlUtils;

/**
 * This is a specialised instance of the InstrumentFile class that provides
 * special functions used only when an instrument is being defined.
 */
public class FileDefinitionBuilder extends FileDefinition {

  /**
   * The maximum number of lines from an uploaded file to be stored
   */
  protected static final int MAX_DISPLAY_LINES = 500;

  /**
   * The number of lines to search in order to determine the file's column
   * separator
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
   * The list of file columns, used during assignment.
   */
  private List<FileColumn> fileColumns = null;

  /**
   * Create a new file definition with the default description
   *
   * @param fileSet
   *          The file set that will contain this file definition
   * @throws InvalidInstrumentBasisException
   */
  public FileDefinitionBuilder(InstrumentFileSet fileSet, int instrumentBasis)
    throws InvalidInstrumentBasisException {
    super(DEFAULT_DESCRIPTION, fileSet, instrumentBasis);

    int counter = 1;
    while (fileSet.containsFileDescription(getFileDescription())) {
      counter++;
      setFileDescription(DEFAULT_DESCRIPTION + " " + counter);
    }
  }

  /**
   * Create a new file definition with a specified description
   *
   * @param fileDescription
   *          The file description
   * @param fileSet
   *          The file set that will contain this file definition
   * @throws InvalidInstrumentBasisException
   */
  public FileDefinitionBuilder(String fileDescription,
    InstrumentFileSet fileSet, int instrumentBasis)
    throws InvalidInstrumentBasisException {
    super(fileDescription, fileSet, instrumentBasis);
  }

  public FileDefinitionBuilder(String fileDescription,
    InstrumentFileSet fileSet, Class<? extends DataFile> fileClass)
    throws InvalidInstrumentBasisException {

    super(fileDescription, fileSet, fileClass);
  }

  /**
   * Determines whether or not file data has been uploaded for this instrument
   * file
   *
   * @return {@code true} if file data has been uploaded; {@code false} if it
   *         has not.
   */
  public boolean getHasFileData() {
    return (null != fileContents);
  }

  /**
   * Guess the layout of the file from its contents
   *
   * @see #calculateColumnCount()
   */
  public void guessFileLayout() {
    try {
      // Look at the last few lines. Find the most common separator character,
      // and the maximum number of columns in each line
      setSeparator(getMostCommonSeparator());

      // Work out the likely number of columns in the file from the last few
      // lines
      super.setColumnCount(calculateColumnCount());

      // Now start at the beginning of the file, looking for rows containing the
      // maximum column count. Start rows that don't contain this are considered
      // to be the header.
      int currentRow = 0;
      boolean columnsRowFound = false;
      while (!columnsRowFound && currentRow < fileContents.size()) {
        if (countSeparatorInstances(getSeparator(),
          fileContents.get(currentRow)) + 1 == getColumnCount()) {
          columnsRowFound = true;
        } else {
          currentRow++;
        }
      }

      setHeaderLines(currentRow);
      if (currentRow > 0) {
        setHeaderEndString(fileContents.get(currentRow - 1));
      }

      // Finally, find the line row that's mostly numeric. This is the first
      // proper data line.
      // Any lines between the header and this are column header rows
      boolean dataFound = false;
      while (!dataFound && currentRow < fileContents.size()) {

        // Filter out multiple consecutive spaces, because they can throw off
        // the data:non-data ratio
        String filteredRow = fileContents.get(currentRow)
          .replaceAll(getSeparator(), "");

        // Also remove the run type strings if we know them, because they are
        // data but not necessarily numbers
        List<String> runTypes = new ArrayList<String>();
        if (null != fileSet) {
          for (FileDefinition fileDef : fileSet) {
            RunTypeAssignments runTypeAssignments = fileDef.getRunTypes();
            if (null != runTypeAssignments) {
              runTypes.addAll(runTypeAssignments.keySet());
            }
          }
        }

        // Process the run types by longest first to ensure short substrings
        // don't get removed and leave longer strings behind
        StringUtils.sortByLength(runTypes, true);

        // All run types are stored in lower case
        filteredRow = filteredRow.toLowerCase();
        for (String runType : runTypes) {
          filteredRow = filteredRow.replaceAll(runType, "");
        }

        // Finally remove NaN, since that's also data
        filteredRow = filteredRow.replaceAll("nan", "");

        // Now get all the numbers from the original row
        String numbers = fileContents.get(currentRow).replaceAll("[^0-9]", "");

        // The count of numbers is over half of the length of the filtered row,
        // then we think this is data.
        if (numbers.length() > (filteredRow.length() / 2)) {
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
   * Get the {@link #MAX_DISPLAY_LINES} of the file data as a JSON string, to be
   * used in previewing the file contents
   *
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
   *
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
   * Count the number of instances of a given separator in a string. Trailing
   * separators are ignored.
   *
   * @param separator
   *          The separator to search for
   * @param searchString
   *          The string to be searched
   * @return The number of separators found in the string
   */
  private int countSeparatorInstances(String separator, String searchString) {

    Pattern searchPattern;

    if (separator.equals(" ")) {
      // Space separators come in groups, so count consecutive spaces as one
      // instance
      searchString = searchString.trim();
      searchPattern = Pattern.compile("  *");
    } else {
      searchPattern = Pattern.compile(separator);
    }

    Matcher matcher = searchPattern.matcher(searchString);

    int matchCount = 0;
    while (matcher.find()) {
      matchCount++;
    }

    /*
     * This was implemented for early attempts at SubCTech support. The latest
     * SubCTech data I have doesn't seem to have a problem that requires this
     * fix, and it interferes with other data formats, so I'm taking it out.
     */

    // If the string ends with a separator, ignore it
    /*
     * if (searchString.endsWith(separator)) { matchCount--; }
     */
    return matchCount;
  }

  /**
   * Store the file data as an array of Strings
   *
   * @param fileContents
   *          The file data
   */
  public void setFileContents(List<String> fileContents) {
    this.fileContents = fileContents;
  }

  /**
   * Create a deep copy of a {@code FileDefinitionBuilder} object.
   *
   * @param source
   *          The source object
   * @return The copied object
   * @throws InvalidInstrumentBasisException
   */
  public static FileDefinitionBuilder copy(FileDefinitionBuilder source)
    throws InvalidInstrumentBasisException {

    FileDefinitionBuilder dest = new FileDefinitionBuilder(
      source.getFileDescription(), source.getFileSet(), source.getFileClass());

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
   * This states the hypothetical number of columns found in the file if the
   * specified separator is used.
   *
   * This counts is based on the number of separators found per line in the last
   * few lines of the file. The largest number of columns is used, because some
   * instruments report diagnostic information on shorter lines.
   *
   * @param separator
   *          The separator
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
      int separatorCount = countSeparatorInstances(separator,
        fileContents.get(i));
      if (separatorCount > maxColumnCount) {
        maxColumnCount = separatorCount + 1;
      }
    }

    return maxColumnCount;
  }

  /**
   * Dummy set column method for bean requirements. We don't allow external
   * agencies to set this, but it's needed for bean compatibility
   *
   * @param columnCount
   *          The column count
   */
  @Override
  public void setColumnCount(int columnCount) {
    // Do nothing
  }

  /**
   * Build the list of {@link FileColumn} objects used during sensor assignment.
   */
  private void makeFileColumns() {

    List<String> columns;

    if (getColumnHeaderRows() == 0) {
      columns = new ArrayList<String>(getColumnCount());
      for (int i = 0; i < getColumnCount(); i++) {
        columns.add("Column " + (i + 1));
      }
    } else {
      String columnHeaders = fileContents.get(getFirstColumnHeaderRow());
      columns = extractFields(columnHeaders);
    }

    fileColumns = FileColumn.makeFileColumns(columns, getSampleColumnValues());
  }

  /**
   * Get the list of {@link FileColumn} objects to be used during sensor
   * assignment.
   *
   * @return The file columns.
   */
  public List<FileColumn> getFileColumns() {
    if (null == fileColumns) {
      makeFileColumns();
    }

    return fileColumns;
  }

  /**
   * Get a sample value for each column in the data file.
   *
   * <p>
   * The sample value is the first value in the file that isn't a common missing
   * value. If no such value is found for a column, an empty string will be
   * used.
   * </p>
   *
   * @return The sample values for each column.
   * @see FileColumn#isMissingValue(String)
   */
  private List<String> getSampleColumnValues() {

    List<String> values = new ArrayList<String>(getColumnCount());
    List<Boolean> complete = new ArrayList<Boolean>(getColumnCount());

    for (int i = 0; i < getColumnCount(); i++) {
      values.add("");
      complete.add(false);
    }

    // Loop through the fileContents lines, finding non-missing values
    // for each column. Keep going until we have values for all columns
    // or we fall off the end of the file.
    int currentLine = getFirstDataRow();
    while (anyFalse(complete) && currentLine < fileContents.size()) {
      List<String> row = extractFields(fileContents.get(currentLine));

      for (int i = 0; i < getColumnCount(); i++) {
        if (!complete.get(i)) {
          // Handle short rows
          if (row.size() > i) {
            if (!FileColumn.isMissingValue(row.get(i))) {
              values.set(i, row.get(i));
              complete.set(i, true);
            }
          }
        }
      }

      currentLine++;
    }

    return values;
  }

  /**
   * Determine whether any value in a list of booleans is {@code false}.
   *
   * @param list
   *          The list
   * @return {@code true} if any value is {@code false}; {@code false} if all
   *         values are {@code true}.
   */
  private boolean anyFalse(List<Boolean> list) {
    boolean result = false;

    for (Boolean bool : list) {
      if (!bool) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Return the row number of the first column header row in the file
   *
   * @return The first column header row, or -1 if the file does not have column
   *         headers
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
   *
   * @return The file data
   */
  public String getJsonData() {

    StringBuilder result = new StringBuilder();

    result.append('[');

    int firstDataRow = getFirstDataRow();
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
        // fill in empty strings for unused columns. Also replace tabs
        // with \\t so it's valid JSON after loading in Javascript.
        if (j < columnValues.size()) {
          result.append(
            StringUtils.tabToSpace(columnValues.get(j).replaceAll("'", "\\'")));
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
  public void setSeparatorName(String separatorName)
    throws InvalidSeparatorException {
    super.setSeparatorName(separatorName);
    super.setColumnCount(calculateColumnCount());
  }

  protected TreeSet<String> getUniqueRunTypes() {

    TreeSet<String> values = new TreeSet<String>();

    for (int i = getHeaderLength() + getColumnHeaderRows(); i < fileContents
      .size(); i++) {
      List<String> columns = extractFields(fileContents.get(i));
      values.add(runTypes.getRunType(columns));
    }

    return values;
  }

  public void buildRunTypeCategories() {
    runTypes.clear();
    for (String runType : getUniqueRunTypes()) {
      setRunTypeCategory(runType, RunTypeCategory.IGNORED);
    }
  }

  @Override
  public void removeRunTypeColumn(int runTypeColumn) {
    super.removeRunTypeColumn(runTypeColumn);

    if (null != runTypes) {
      runTypes.clear();
      for (String runType : getUniqueRunTypes()) {
        setRunTypeCategory(runType, RunTypeCategory.IGNORED);
      }
    }
  }

  /**
   * Shortcut method to get the length of the header
   *
   * @return The header length
   */
  private int getHeaderLength() {
    return getHeaderLength(fileContents);
  }

  /**
   * Get the header line from a file that contains the given prefix and suffix.
   * A line will match if it contains the prefix, followed by a number of
   * characters, followed by the suffix.
   * <p>
   * The matching line will be returned as a {@link HighlightedString}, with the
   * portion between the prefix and suffix highlighted.
   * </p>
   * <p>
   * If multiple lines match the prefix and suffix, the first line will be
   * returned.
   * </p>
   *
   * @param prefix
   *          The prefix
   * @param suffix
   *          The suffix
   * @return The matching line
   * @throws HighlightedStringException
   *           If an error occurs while building the highlighted string
   */
  public HighlightedString getHeaderLine(String prefix, String suffix)
    throws HighlightedStringException {
    return super.getHeaderLine(fileContents, prefix, suffix);
  }

  /**
   * Get the first row that contains data.
   *
   * @return The first data row.
   */
  private int getFirstDataRow() {
    return getHeaderLength() + getColumnHeaderRows();
  }

  public String getColumnName(int columnIndex) {
    String result = "*** COLUMN NOT FOUND ***";

    Optional<FileColumn> column = fileColumns.stream()
      .filter(c -> c.getIndex() == columnIndex).findAny();

    if (column.isPresent()) {
      result = column.get().getName();
    }

    return result;
  }

  public int getColumnIndex(String columnName) {
    int result = -1;

    Optional<FileColumn> column = fileColumns.stream()
      .filter(c -> c.getName().equals(columnName)).findAny();

    if (column.isPresent()) {
      result = column.get().getIndex();
    }

    return result;
  }
}
