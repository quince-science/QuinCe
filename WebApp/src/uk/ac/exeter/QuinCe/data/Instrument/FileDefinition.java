package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.HighlightedStringException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Holds a description of a sample data file uploaded during the creation of a
 * new instrument.
 */
public class FileDefinition implements Comparable<FileDefinition> {

  /**
   * Fixed column ID for the time
   */
  public static final long TIME_COLUMN_ID = -1100L;

  /**
   * Fixed column name for the time
   */
  public static final String TIME_COLUMN_NAME = "Time";

  /**
   * The application-wide standard time column heading
   */
  public static final ColumnHeading TIME_COLUMN_HEADING;

  /**
   * Special column ID for longitude, because it's not defined in the
   * file_column dataset
   *
   * @see uk.ac.exeter.QuinCe.web.datasets.data.FieldSets#POSITION_FIELD_ID
   */
  public static final long LONGITUDE_COLUMN_ID = -1000L;

  /**
   * Fixed name for the Longitude column
   */
  public static final String LONGITUDE_COLUMN_NAME = "Longitude";

  /**
   * The application-wide standard longitude column heading
   */
  public static final ColumnHeading LONGITUDE_COLUMN_HEADING;

  /**
   * Special column ID for latitude
   *
   * @see uk.ac.exeter.QuinCe.web.datasets.data.FieldSets#POSITION_FIELD_ID
   */
  public static final long LATITUDE_COLUMN_ID = -1001L;

  /**
   * Fixed name for the Latitude column
   */
  public static final String LATITUDE_COLUMN_NAME = "Latitude";

  /**
   * The application-wide standard longitude column heading
   */
  public static final ColumnHeading LATITUDE_COLUMN_HEADING;

  /**
   * The name of the Run Type column
   */
  public static final String RUN_TYPE_COL_NAME = "Run Type";

  /**
   * Inidicates that the file header is defined by a number of lines
   */
  public static final int HEADER_TYPE_LINE_COUNT = 0;

  /**
   * Indicates that the file header is defined by a specific string
   */
  public static final int HEADER_TYPE_STRING = 1;

  /**
   * The mapping of separator names to the separator characters
   */
  private static Map<String, String> SEPARATOR_LOOKUP = null;

  /**
   * The available separator characters
   */
  protected static final String[] VALID_SEPARATORS = { "\t", ",", ";", " " };

  /**
   * Menu index for the tab separator
   */
  public static final int SEPARATOR_INDEX_TAB = 0;

  /**
   * Menu index for the comma separator
   */
  public static final int SEPARATOR_INDEX_COMMA = 1;

  /**
   * Menu index for the semicolon separator
   */
  public static final int SEPARATOR_INDEX_SEMICOLON = 2;

  /**
   * Menu index for the space separator
   */
  public static final int SEPARATOR_INDEX_SPACE = 3;

  /**
   * The database ID of this file definition
   */
  private long databaseId;

  /**
   * The name used to identify files of this type
   */
  private String fileDescription;

  /**
   * The method by which the header is defined. One of
   * {@link #HEADER_TYPE_LINE_COUNT} or {@link #HEADER_TYPE_STRING}
   *
   * <p>
   * For an empty header, use {@link #HEADER_TYPE_LINE_COUNT} and set
   * {@link #headerLines} to zero.
   * </p>
   */
  private int headerType = HEADER_TYPE_LINE_COUNT;

  /**
   * The number of lines in the header (if the header is defined by a number of
   * lines)
   */
  private int headerLines = 0;

  /**
   * The string that indicates the end of the header
   */
  private String headerEndString = null;

  /**
   * The number of rows containing column headers
   */
  private int columnHeaderRows = 0;

  /**
   * The column separator
   */
  private String separator = ",";

  /**
   * The number of columns in the file
   */
  private int columnCount = 0;

  /**
   * The longitude specification
   */
  private LongitudeSpecification longitudeSpecification;

  /**
   * The latitude specification
   */
  private LatitudeSpecification latitudeSpecification;

  /**
   * The Date/Time specification
   */
  private DateTimeSpecification dateTimeSpecification;

  /**
   * The list of run type values for the instrument
   */
  protected RunTypeAssignments runTypes = null;

  /**
   * The file set of which this definition is a member
   */
  protected InstrumentFileSet fileSet;

  static {
    SEPARATOR_LOOKUP = new HashMap<String, String>(4);
    SEPARATOR_LOOKUP.put("TAB", "\t");
    SEPARATOR_LOOKUP.put("COMMA", ",");
    SEPARATOR_LOOKUP.put("SEMICOLON", ";");
    SEPARATOR_LOOKUP.put("SPACE", " ");

    TIME_COLUMN_HEADING = new ColumnHeading(TIME_COLUMN_ID, TIME_COLUMN_NAME,
      TIME_COLUMN_NAME, "STRT8601", null, false, false);
    LONGITUDE_COLUMN_HEADING = new ColumnHeading(LONGITUDE_COLUMN_ID,
      LONGITUDE_COLUMN_NAME, LONGITUDE_COLUMN_NAME, "ALONGP01", "degrees_east",
      true, true);
    LATITUDE_COLUMN_HEADING = new ColumnHeading(LATITUDE_COLUMN_ID,
      LATITUDE_COLUMN_NAME, LATITUDE_COLUMN_NAME, "ALATGP01", "degrees_north",
      true, true);
  }

  /**
   * Create a new file with the given description.
   *
   * @param fileDescription
   *          The file description
   * @param fileSet
   *          The file set of which this definition is a member
   */
  public FileDefinition(String fileDescription, InstrumentFileSet fileSet) {
    this.fileDescription = fileDescription;
    this.longitudeSpecification = new LongitudeSpecification();
    this.latitudeSpecification = new LatitudeSpecification();
    this.dateTimeSpecification = new DateTimeSpecification(false);
    this.fileSet = fileSet;
  }

  /**
   * Construct a complete file definition
   *
   * @param databaseId
   *          The definition's database ID
   * @param description
   *          The file description
   * @param separator
   *          The column separator
   * @param headerType
   *          The header type
   * @param headerLines
   *          The number of header lines
   * @param headerEndString
   *          The string used to identify the end of the header
   * @param columnHeaderRows
   *          The number of column header rows
   * @param columnCount
   *          The column count
   * @param lonSpec
   *          The longitude specification
   * @param latSpec
   *          The latitude specification
   * @param dateTimeSpec
   *          The date/time specification
   * @param fileSet
   *          The parent file set
   */
  public FileDefinition(long databaseId, String description, String separator,
    int headerType, int headerLines, String headerEndString,
    int columnHeaderRows, int columnCount, LongitudeSpecification lonSpec,
    LatitudeSpecification latSpec, DateTimeSpecification dateTimeSpec,
    InstrumentFileSet fileSet) {

    // TODO checks

    this.databaseId = databaseId;
    this.fileDescription = description;
    this.separator = separator;
    this.headerType = headerType;
    this.headerLines = headerLines;
    this.headerEndString = headerEndString;
    this.columnHeaderRows = columnHeaderRows;
    this.columnCount = columnCount;
    this.longitudeSpecification = lonSpec;
    this.latitudeSpecification = latSpec;
    this.dateTimeSpecification = dateTimeSpec;

    this.fileSet = fileSet;
  }

  /**
   * Get the database ID of this file definition
   *
   * @return The database ID
   */
  public long getDatabaseId() {
    return databaseId;
  }

  /**
   * Get the description for this file
   *
   * @return The file description
   */
  public String getFileDescription() {
    return fileDescription;
  }

  /**
   * Set the description for this file
   *
   * @param fileDescription
   *          The file description
   */
  public void setFileDescription(String fileDescription) {
    this.fileDescription = fileDescription;
  }

  /**
   * Comparison is based on the file description. The comparison is case
   * insensitive.
   */
  @Override
  public int compareTo(FileDefinition o) {
    return fileDescription.toLowerCase()
      .compareTo(o.fileDescription.toLowerCase());
  }

  /**
   * Get the number of lines that make up the header. This is only valid if
   * {@link #headerType} is set to {@link #HEADER_TYPE_LINE_COUNT}.
   *
   * @return The number of lines in the header
   */
  public int getHeaderLines() {
    return headerLines;
  }

  /**
   * Set the number of lines that make up the header
   *
   * @param headerLines
   *          The number of lines in the header
   */
  public void setHeaderLines(int headerLines) {
    this.headerLines = headerLines;
    dateTimeSpecification.setFileHasHeader(headerLines > 0);
  }

  /**
   * Get the string that defines the last line of the header. This is only valid
   * if {@link #headerType} is set to {@link #HEADER_TYPE_STRING}.
   *
   * @return The string that defines the last line of the header.
   */
  public String getHeaderEndString() {
    return headerEndString;
  }

  /**
   * Set the string that defines the last line of the header.
   *
   * @param headerEndString
   *          The string that defines the last line of the header.
   */
  public void setHeaderEndString(String headerEndString) {
    this.headerEndString = headerEndString;
  }

  /**
   * Get the number of column header rows
   *
   * @return The number of column header rows
   */
  public int getColumnHeaderRows() {
    return columnHeaderRows;
  }

  /**
   * Set the number of column header rows
   *
   * @param columnHeaderRows
   *          The number of column header rows
   */
  public void setColumnHeaderRows(int columnHeaderRows) {
    this.columnHeaderRows = columnHeaderRows;
  }

  /**
   * Get the file's column separator
   *
   * @return The separator
   */
  public String getSeparator() {
    return separator;
  }

  /**
   * Get the name for the file's column separator
   *
   * @return The separator name
   */
  public String getSeparatorName() {

    String result = null;

    for (Map.Entry<String, String> entry : SEPARATOR_LOOKUP.entrySet()) {
      if (entry.getValue().equals(separator)) {
        result = entry.getKey();
        break;
      }
    }

    return result;
  }

  /**
   * Set the column separator. This can use either a separator name or the
   * separator character itself.
   *
   * @param separator
   *          The separator
   * @throws InvalidSeparatorException
   *           If the supplied separator is not supported
   * @see #SEPARATOR_LOOKUP
   * @see #VALID_SEPARATORS
   */
  public void setSeparator(String separator) throws InvalidSeparatorException {
    if (!validateSeparator(separator)) {
      throw new InvalidSeparatorException(separator);
    }
    this.separator = separator;
  }

  /**
   * Set the file's column separator using the separator name
   *
   * @param separatorName
   *          The separator name
   * @throws InvalidSeparatorException
   *           If the separator name is not recognised
   */
  public void setSeparatorName(String separatorName)
    throws InvalidSeparatorException {
    if (!SEPARATOR_LOOKUP.containsKey(separatorName)) {
      throw new InvalidSeparatorException(separatorName);
    } else {
      this.separator = SEPARATOR_LOOKUP.get(separatorName);
    }
  }

  /**
   * Ensure that a separator is one of the supported options
   *
   * @param separator
   *          The separator to be checked
   * @return {@code true} if the separator is supported; {@code false} if it is
   *         not
   */
  public static boolean validateSeparator(String separator) {

    boolean separatorValid = true;

    if (!SEPARATOR_LOOKUP.containsKey(separator)
      && !SEPARATOR_LOOKUP.containsValue(separator)) {
      separatorValid = false;
    }

    return separatorValid;
  }

  /**
   * Get the header type of the file. Will be either
   * {@link #HEADER_TYPE_LINE_COUNT} or {@link #HEADER_TYPE_STRING}.
   *
   * @return The header type.
   */
  public int getHeaderType() {
    return headerType;
  }

  /**
   * Set the header type of the file. Must be either
   * {@link #HEADER_TYPE_LINE_COUNT} or {@link #HEADER_TYPE_STRING}.
   *
   * @param headerType
   *          The header type
   * @throws InvalidHeaderTypeException
   *           If an invalid header type is specified
   */
  public void setHeaderType(int headerType) throws InvalidHeaderTypeException {
    if (headerType != HEADER_TYPE_LINE_COUNT
      && headerType != HEADER_TYPE_STRING) {
      throw new InvalidHeaderTypeException();
    }
    this.headerType = headerType;
  }

  /**
   * Set the header to be defined by a number of lines
   *
   * @param headerLines
   *          The number of lines in the header
   */
  public void setLineCountHeaderType(int headerLines) {
    this.headerType = HEADER_TYPE_LINE_COUNT;
    this.headerLines = headerLines;
  }

  /**
   * Set the header to be defined by a string that marks the end of the header
   *
   * @param headerString
   *          The string denoting the end of the header
   */
  public void setStringHeaderType(String headerString) {
    this.headerType = HEADER_TYPE_STRING;
    this.headerEndString = headerString;
  }

  /**
   * Set the column count
   *
   * @param columnCount
   *          The column count
   */
  public void setColumnCount(int columnCount) {
    this.columnCount = columnCount;
  }

  /**
   * Get the column count
   *
   * @return The column count
   */
  public int getColumnCount() {
    return columnCount;
  }

  /**
   * Convert a string from a data file into a list of column values
   *
   * @param dataLine
   *          The data line
   * @return The column values
   */
  public List<String> extractFields(String dataLine) {
    List<String> values = new ArrayList<String>();

    if (separator.equals(" ")) {
      dataLine = dataLine.trim().replaceAll("  *", " ");
    }

    values = StringUtils.trimListAndQuotes(
      Arrays.asList(dataLine.split(separator, dataLine.length())));

    // If the column count is one less than the extracted number of columns,
    // and the last column is empty, assume we have a rogue trailing separator.
    if (values.size() == getColumnCount() + 1
      && values.get(values.size() - 1).length() == 0) {

      values.remove(values.size() - 1);
    }

    return values;
  }

  /**
   * Get the longitude specification
   *
   * @return The longitude specification
   */
  public LongitudeSpecification getLongitudeSpecification() {
    return longitudeSpecification;
  }

  /**
   * Get the latitude specification
   *
   * @return The latitude specification
   */
  public LatitudeSpecification getLatitudeSpecification() {
    return latitudeSpecification;
  }

  /**
   * Get the date/time specification
   *
   * @return The date/time specification
   */
  public DateTimeSpecification getDateTimeSpecification() {
    return dateTimeSpecification;
  }

  /**
   * Determine whether or not the file has a header.
   *
   * The header has either with a specified number of header lines or a header
   * end string.
   *
   * @return {@code true} if the file has a header; {@code false} if it does
   *         not.
   */
  public boolean hasHeader() {
    return (headerLines > 0 || null != headerEndString);
  }

  /**
   * Get the file set of which this definition is a member
   *
   * @return The parent file set
   */
  public InstrumentFileSet getFileSet() {
    return fileSet;
  }

  /**
   * Remove a column assignment from the date/time or position specification. If
   * there is no assignment for the column, no action is taken.
   *
   * @param column
   *          The column to be unassigned
   * @return {@code true} if an assignment was found and removed; {@code false}
   *         if not.
   */
  public boolean removeAssignment(int column) {

    boolean unassigned = false;

    if (latitudeSpecification.getValueColumn() == column) {
      latitudeSpecification.clearValueColumn();
      unassigned = true;
    } else if (latitudeSpecification.getHemisphereColumn() == column) {
      latitudeSpecification.clearHemisphereColumn();
      unassigned = true;
    }

    if (!unassigned) {
      if (longitudeSpecification.getValueColumn() == column) {
        longitudeSpecification.clearValueColumn();
        unassigned = true;
      } else if (longitudeSpecification.getHemisphereColumn() == column) {
        longitudeSpecification.clearHemisphereColumn();
        unassigned = true;
      }
    }

    if (!unassigned) {
      DateTimeSpecification dateTime = getDateTimeSpecification();
      unassigned = dateTime.removeAssignment(column);
    }

    return unassigned;
  }

  /**
   * Get the assigned run types for this file. If the {@link #runTypeColumn} is
   * {@code -1}, this will return {@code null}.
   *
   * @return The run types
   */
  public RunTypeAssignments getRunTypes() {
    return runTypes;
  }

  /**
   * Get the list of run type values in this file
   *
   * @return The run type values
   */
  public List<String> getRunTypeValues() {
    List<String> values;

    if (null == runTypes) {
      values = new ArrayList<String>(0);
    } else {
      values = new ArrayList<String>(runTypes.size());
      values.addAll(runTypes.keySet());
    }

    return values;
  }

  /**
   * Get the list of run type values with the specified value excluded
   *
   * @param exclusion
   *          The value to exclude from the list
   * @return The list of run types without the excluded value
   */
  public List<String> getRunTypeValuesWithExclusion(String exclusion) {
    List<String> runTypeValues = getRunTypeValues();
    runTypeValues.remove(exclusion);
    return runTypeValues;
  }

  /**
   * Get the run type column for this file. Returns -1 if the column is not
   * assigned
   *
   * @return The run type column
   */
  public TreeSet<Integer> getRunTypeColumns() {
    TreeSet<Integer> result = null;

    if (null != runTypes) {
      result = runTypes.getColumns();
    }

    return result;
  }

  /**
   * Set the Run Type column, and initialise the assignments
   *
   * @throws SensorAssignmentException
   */
  public void addRunTypeColumn(int runTypeColumn) {
    if (null == runTypes) {
      runTypes = new RunTypeAssignments(runTypeColumn);
    } else {
      runTypes.addColumn(runTypeColumn);
    }
  }

  public void removeRunTypeColumn(int column) {
    if (null != runTypes) {
      runTypes.removeColumn(column);
      if (runTypes.getColumnCount() == 0) {
        runTypes = null;
      }
    }
  }

  /**
   * Set the run type category for a given run type
   *
   * @param runType
   *          The run type
   * @param category
   *          The run type category
   */
  public void setRunTypeCategory(String runType, RunTypeCategory category) {
    runTypes.put(runType, new RunTypeAssignment(runType, category));
  }

  /**
   * Set the run type category for a given run type
   *
   * @param runType
   *          The run type
   * @param category
   *          The run type category
   */
  public void setRunTypeCategory(String runType, String alias) {
    // TODO We should check to make sure that the aliased run type actually
    // exists
    // and that it's not a circular alias
    // I'm not sure how hard this is at the minute so I'm ignoring it -
    // the UI should prevent it from happening anyway
    runTypes.put(runType, new RunTypeAssignment(runType, alias));
  }

  /**
   * Insert the complete set of run types associated with this file definition.
   * Replaces any existing run types.
   *
   * @param runTypes
   *          The run types
   */
  public void setRunTypes(RunTypeAssignments runTypes) {
    this.runTypes = runTypes;
  }

  /**
   * Compare the layout of this file definition to a supplied definition to see
   * if they are identical.
   *
   * @param compare
   *          The definition to be compared
   * @return {@code true} if the layouts match; {@code false} otherwise.
   */
  public boolean matchesLayout(FileDefinition compare) {
    boolean matches = true;

    switch (headerType) {
    case HEADER_TYPE_LINE_COUNT: {
      if (headerLines != compare.headerLines) {
        matches = false;
      }
      break;
    }
    case HEADER_TYPE_STRING: {
      if (null == headerEndString) {
        if (null != compare.headerEndString) {
          matches = false;
        }
      } else if (null == compare.headerEndString
        || !headerEndString.equals(compare.headerEndString)) {
        matches = false;
      }
      break;
    }
    }

    if (matches) {
      if (columnHeaderRows != compare.columnHeaderRows) {
        matches = false;
      } else if (!separator.equals(compare.separator)) {
        matches = false;
      } else if (columnCount != compare.columnCount) {
        matches = false;
      }
    }

    return matches;
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
   * @param fileContents
   *          The file contents
   * @param prefix
   *          The prefix
   * @param suffix
   *          The suffix
   * @return The matching line
   * @throws HighlightedStringException
   *           If an error occurs while building the highlighted string
   */
  public HighlightedString getHeaderLine(List<String> fileContents,
    String prefix, String suffix) throws HighlightedStringException {

    HighlightedString matchedLine = null;

    for (String line : getFileHeader(fileContents)) {
      int prefixPos;
      if (prefix.length() == 0) {
        prefixPos = 0;
      } else {
        prefixPos = line.indexOf(prefix);
      }

      if (prefixPos > -1) {
        int suffixPos = line.length();
        if (suffix.length() > 0) {
          suffixPos = line.indexOf(suffix, (prefixPos + prefix.length()));
          if (suffixPos == -1) {
            suffixPos = line.length();
          }
        }

        matchedLine = new HighlightedString(line, prefixPos + prefix.length(),
          suffixPos);
        break;
      }
    }

    return matchedLine;
  }

  /**
   * Get the number of rows in a file header
   *
   * @param fileContents
   *          The file contents
   * @return The number of rows in the file header
   */
  public int getHeaderLength(List<String> fileContents) {
    int result = 0;

    switch (getHeaderType()) {
    case HEADER_TYPE_LINE_COUNT: {
      result = getHeaderLines();
      break;
    }
    case HEADER_TYPE_STRING: {

      int row = 0;
      boolean foundHeaderEnd = false;
      while (!foundHeaderEnd && row < fileContents.size()) {
        if (fileContents.get(row).equalsIgnoreCase(getHeaderEndString())) {
          foundHeaderEnd = true;
        }

        row++;
      }

      result = row;
      break;
    }
    }

    return result;
  }

  /**
   * Get the file header from a file. If there is no header, returns an empty
   * list.
   *
   * @param fileContents
   *          The file contents
   * @return The lines of the file header
   */
  public List<String> getFileHeader(List<String> fileContents) {
    List<String> result;

    int headerLines = getHeaderLength(fileContents);
    if (headerLines == 0) {
      result = new ArrayList<String>();
    } else {
      result = fileContents.subList(0, headerLines);
    }

    return result;
  }

  /**
   * Determine whether or not this file contains Run Types
   *
   * @return {@code true} if the file contains Run Types; {@code false} if not
   */
  public boolean hasRunTypes() {
    return null != runTypes;
  }

  /**
   * Get the run type from a data line
   *
   * @param line
   *          The line
   * @return The run type
   * @throws FileDefinitionException
   *           If this file does not contain run types, the run type is not
   *           present, or the run type is not recognised
   */
  public RunTypeAssignment getRunType(String line, boolean followAlias)
    throws FileDefinitionException {
    return getRunType(extractFields(line), followAlias);
  }

  /**
   * Get the run type from a data line
   *
   * @param line
   *          The line
   * @return The run type
   * @throws FileDefinitionException
   *           If this file does not contain run types, the run type is not
   *           present, or the run type is not recognised
   */
  public RunTypeAssignment getRunType(List<String> line, boolean followAlias)
    throws FileDefinitionException {
    RunTypeAssignment result = null;

    if (!hasRunTypes()) {
      throw new FileDefinitionException("File does not contain run types");
    } else {
      String runTypeValue = StringUtils.listToDelimited(line,
        runTypes.getColumns(), "|");
      if (null != runTypeValue && runTypeValue.length() > 0) {
        result = runTypes.get(runTypeValue, followAlias);
      }
    }

    return result;
  }

  /**
   * Get the Run Type Category of the Run Type on the given line
   *
   * @param line
   *          The line
   * @return The Run Type Category
   * @throws FileDefinitionException
   *           If the Run Type is not recognised
   */
  public RunTypeCategory getRunTypeCategory(String line)
    throws FileDefinitionException {
    return getRunType(line, true).getCategory();
  }

  /**
   * Examine the contents of a file to see if they match the format of this file
   * definition.
   *
   * @param lines
   *          The file lines
   * @return {@code true} if the file contents are compatible with this format;
   *         {@code false} if not.
   */
  public boolean fileMatches(List<String> lines) {

    boolean matches = true;

    int currentLine = 0;

    // Check the header
    if (headerType == HEADER_TYPE_LINE_COUNT) {
      currentLine += headerLines;
    } else if (headerType == HEADER_TYPE_STRING) {
      boolean headerEndFound = false;

      while (!headerEndFound && currentLine < lines.size()) {
        if (lines.get(currentLine).equals(headerEndString)) {
          headerEndFound = true;
        }

        currentLine++;
      }

      if (!headerEndFound) {
        matches = false;
      }
    }

    if (matches) {

      // Check that the first column header row contains the correct number
      // of columns. If it does, skip the total number of header rows.
      if (columnHeaderRows > 0) {
        int firstRowColumnCount = extractFields(lines.get(currentLine)).size();

        if (firstRowColumnCount != columnCount) {
          matches = false;
        } else {
          currentLine += columnHeaderRows;
        }
      }
    }

    if (matches) {

      // Check the remaining rows to make sure that most of them contain the
      // correct number of columns.
      // The percentage threshold is entirely arbitrary and may need adjustment.
      int dataRows = lines.size() - currentLine;
      int correctColumnCountRows = 0;

      if (dataRows > 0) {
        while (currentLine < lines.size()) {
          if (extractFields(lines.get(currentLine)).size() == columnCount) {
            correctColumnCountRows++;
          }
          currentLine++;
        }

        if ((float) correctColumnCountRows / (float) dataRows < 0.75) {
          matches = false;
        }
      }
    }

    return matches;
  }

  @Override
  public String toString() {
    return fileDescription;
  }

  /**
   * Determines whether or not this file definition contains position columns.
   *
   * @return {@code true} if a valid position specification is present;
   *         {@code false} otherwise.
   */
  public boolean hasPosition() {
    boolean result = true;

    if (null == getLongitudeSpecification()) {
      result = false;
    } else if (getLongitudeSpecification().getFormat() < 0) {
      result = false;
    }

    return result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(databaseId, fileDescription);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FileDefinition other = (FileDefinition) obj;
    return databaseId == other.databaseId
      && Objects.equals(fileDescription, other.fileDescription);
  }
}
