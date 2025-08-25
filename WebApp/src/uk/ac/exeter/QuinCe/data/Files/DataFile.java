package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.math.NumberUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.MissingDateTimeException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.utils.TimeRange;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class representing a data file
 */
public abstract class DataFile implements TimeRange {

  public static final String TIME_OFFSET_PROP = "timeOffset";

  /**
   * The instance of the {@link DataFile} class that has its contents loaded
   * into memory.
   */
  private static DataFile fileWithContents = null;

  /**
   * The database ID of this file
   */
  private long databaseId = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The file format definition
   */
  private FileDefinition fileDefinition;

  /**
   * The file name
   */
  private String filename;

  /**
   * The date/time of the first record in the file
   */
  private LocalDateTime startDate = null;

  /**
   * The date/time of the last record in the file
   */
  private LocalDateTime endDate = null;

  /**
   * The number of records in the file
   */
  private int recordCount = -1;

  /**
   * The file contents
   */
  protected List<String> contents;

  /**
   * Messages generated regarding the file
   */
  private TreeSet<DataFileMessage> messages;

  /**
   * Misc properties
   */
  private Properties properties;

  /**
   * Max number of messages. Additional messages will be discarded.
   */
  private static final int MAX_MESSAGE_COUNT = 25;

  /**
   * Row in the file where the error happened not set
   */
  private static final int ROW_NOT_SET = -1;

  /**
   * The date in the file header
   */
  private LocalDateTime headerDate = null;

  /**
   * Run types in this file not defined in the file definition
   */
  private Set<RunTypeAssignment> missingRunTypes = new HashSet<RunTypeAssignment>();

  /**
   * The {@link Instrument} that this file belongs to.
   */
  private final Instrument instrument;

  /**
   * The previously defined {@link Instrument}s that have the same platform name
   * and code as the {@link Instrument} that this file belongs to.
   *
   * <p>
   * Loaded on demand by {@link #getPreviousInstruments}.
   * </p>
   */
  List<Instrument> previousInstruments = null;

  /**
   * Create a DataFile with the specified definition and contents
   *
   * @param fileStore
   *          The location of the file store
   * @param fileDefinition
   *          The file format definition
   * @param filename
   *          The file name
   * @param contents
   *          The file contents
   * @throws MissingParamException
   *           If any fields are null
   */
  public DataFile(Instrument instrument, FileDefinition fileDefinition,
    String filename) throws MissingParamException, DataFileException {
    MissingParam.checkMissing(fileDefinition, "fileDefinition");
    MissingParam.checkMissing(filename, "fileName");

    this.instrument = instrument;
    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.properties = defaultProperties();

    messages = new TreeSet<DataFileMessage>();
  }

  /**
   * Constructor for the basic details of a file. The contents will be loaded on
   * demand if required.
   *
   * @param fileStore
   *          The file store location
   * @param id
   *          The file's database ID
   * @param fileDefinition
   *          The file definition for the file
   * @param filename
   *          The filename
   * @param startDate
   *          The date/time of the first record in the file
   * @param endDate
   *          The date/time of the last record in the file
   * @param recordCount
   *          The number of records in the file
   */
  public DataFile(long id, Instrument instrument, FileDefinition fileDefinition,
    String filename, LocalDateTime startDate, LocalDateTime endDate,
    int recordCount, Properties properties) {

    this.databaseId = id;
    this.instrument = instrument;
    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.startDate = startDate;
    this.endDate = endDate;
    this.recordCount = recordCount;
    this.properties = properties;
  }

  private Properties defaultProperties() {
    Properties result = new Properties();
    result.setProperty(TIME_OFFSET_PROP, "0");
    return result;
  }

  /**
   * Get the file format description
   *
   * @return The file format description
   */
  public String getFileDescription() {
    return fileDefinition.getFileDescription();
  }

  /**
   * Get the file name
   *
   * @return The file name
   */
  public String getFilename() {
    return filename;
  }

  public List<RunTypeAssignment> getMissingRunTypes() {
    List<RunTypeAssignment> list = new ArrayList<>(missingRunTypes);
    Collections.sort(list);
    return list;
  }

  /**
   * Set a new file definition for the file. The new definition must match the
   * layout of the previous definition, since the file's format must conform to
   * both the new and old definitions.
   *
   * @param newDefinition
   *          The new file definition
   * @throws FileDefinitionException
   *           The the definition does not match the file layout
   */
  public void setFileDefinition(FileDefinition newDefinition)
    throws FileDefinitionException {
    if (!fileDefinition.matchesLayout(newDefinition)) {
      throw new FileDefinitionException(
        "File Definition does not match file contents");
    } else {
      this.fileDefinition = newDefinition;
    }
  }

  /**
   * Get the zero-based row number of the first data row in a file
   *
   * @return The first data row number
   * @throws DataFileException
   *           If the end of the file is reached without finding the end of the
   *           header
   */
  public int getFirstDataLine() throws DataFileException {
    if (null == contents) {
      loadContents();
    }
    return fileDefinition.getHeaderLength(contents)
      + fileDefinition.getColumnHeaderRows();
  }

  /**
   * Get the number of rows in the file
   *
   * @return The row count
   */
  public int getContentLineCount() {
    return contents.size();
  }

  /**
   * Get the number of records in the file.
   *
   * @return The record count
   */
  public int getRecordCount() throws DataFileException {
    if (-1 == recordCount) {
      loadContents();
      recordCount = getContentLineCount() - getFirstDataLine();
    }

    return recordCount;
  }

  /**
   * Get the data from a specified row in the file as a list of string fields.
   * This is the row position in the whole file, including headers.
   *
   * @param row
   *          The row to be retrieved
   * @return The row fields
   * @throws DataFileException
   *           If the requested row is outside the bounds of the file
   */
  public List<String> getRowFields(int row) throws DataFileException {
    List<String> result;

    if (row < getFirstDataLine()) {
      throw new DataFileException(databaseId, row,
        "Requested row is in the file header");
    } else if (row > (getContentLineCount() - 1)) {
      throw new DataFileException(databaseId, row,
        "Requested row is in the file header");
    } else {
      result = StringUtils.trimList(
        Arrays.asList(contents.get(row).split(fileDefinition.getSeparator())));
    }

    return result;
  }

  /**
   * Validate the file contents. Creates a set of {@code DataFileMessage}
   * objects, which can be retrieved using {@code getMessages()}.
   *
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  public void validate() throws DataFileException {

    loadContents();

    // We will load this if we need it below
    List<Instrument> previousInstruments = null;

    // Check that there is actually data in the file
    int firstDataLine = -1;
    try {
      firstDataLine = getFirstDataLine();
    } catch (DataFileException e) {
      addMessage("File does not contain any data");
    }

    if (firstDataLine > -1) {

      // For each line in the file, check that:
      // (a) The date/time is monotonic in the file (bad date/times are ignored)
      // (b) Has the correct number of columns (for Run Types that aren't
      // IGNORED)
      // (c) The Run Type is recognised

      LocalDateTime lastDateTime = null;
      for (int lineNumber = firstDataLine; lineNumber < getContentLineCount(); lineNumber++) {
        String line = contents.get(lineNumber);

        try {
          LocalDateTime dateTime = fileDefinition.getDateTimeSpecification()
            .getDateTime(headerDate, fileDefinition.extractFields(line));
          if (null != lastDateTime) {
            if (dateTime.compareTo(lastDateTime) <= 0) {
              addMessage(lineNumber, "Date/Time is not monotonic");
            }
          }
        } catch (MissingDateTimeException | DateTimeSpecificationException e) {
          // We don't mind bad dates in the file -
          // we'll just ignore those lines
        }

        // Check the run type
        if (fileDefinition.hasRunTypes()) {
          String runType = fileDefinition.getRunTypeValue(line);

          if (!fileDefinition.runTypeAssigned(runType)) {

            boolean alreadyProcessed = missingRunTypes.stream()
              .filter(mrt -> mrt.getRunName().equals(runType.toLowerCase()))
              .findAny().isPresent();

            if (!alreadyProcessed) {

              RunTypeAssignment guessedAssignment;

              if (null == previousInstruments) {
                try {
                  previousInstruments = getPreviousInstruments();
                } catch (Exception e) {
                  // Log the error, but continue.
                  // It's not important enough to break the workflow.
                  ExceptionUtils.printStackTrace(e);
                  previousInstruments = new ArrayList<Instrument>();
                }
              }

              RunTypeAssignment previousAssignment = RunTypeAssignments
                .getPreviousRunTypeAssignment(runType, previousInstruments);

              if (null != previousAssignment) {
                guessedAssignment = previousAssignment;
              } else {
                // Guess from presets
                RunTypeAssignment presetAssignment = RunTypeAssignments
                  .getPresetAssignment(instrument.getVariables(), runType,
                    fileDefinition.getRunTypes());
                if (null != presetAssignment) {
                  guessedAssignment = presetAssignment;
                } else {
                  guessedAssignment = new RunTypeAssignment(runType,
                    RunTypeCategory.IGNORED);
                }
              }

              missingRunTypes.add(guessedAssignment);
            }
          }
        }
      }
    }
  }

  /*
   * Shortcut method for adding a message to the message list. When the list
   * size reaches MAX_MESSAGE_COUNT messages, a final message saying "Too many
   * messages..." is added, then no more messages are allowed.
   *
   * @param lineNumber The line number. Line number < 0 means no line number.
   *
   * @param message The message text
   */
  private void addMessage(int lineNumber, String message) {
    if (messages.size() == MAX_MESSAGE_COUNT - 1) {
      messages.add(new DataFileMessage("Too many messages..."));
    } else if (messages.size() < MAX_MESSAGE_COUNT - 1) {
      if (lineNumber < 0) {
        messages.add(new DataFileMessage(message));
      } else {
        messages.add(new DataFileMessage(lineNumber, message));
      }
    }
  }

  /**
   * Shortcut method for adding a message to the message list
   *
   * @param message
   *          The message text
   */
  private void addMessage(String message) {
    addMessage(ROW_NOT_SET, message);
  }

  /**
   * Get the messages generated for this file
   *
   * @return The messages
   */
  public TreeSet<DataFileMessage> getMessages() {
    return messages;
  }

  /**
   * Get the number of messages that have been generated for this file
   *
   * @return The message count
   */
  public int getMessageCount() {
    return messages.size();
  }

  /**
   * Get the start date from the file header. This is only applicable if the
   * date format is {@link DateTimeSpecification#HOURS_FROM_START}.
   *
   * @return {@code true} if the header date is successfully extracted;
   *         {@code false} if the date cannot be extracted
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  private boolean extractHeaderDate() throws DataFileException {

    loadContents();

    boolean result = true;

    DateTimeSpecification dateTimeSpec = fileDefinition
      .getDateTimeSpecification();

    DateTimeColumnAssignment assignment = null;

    if (dateTimeSpec.isAssigned(DateTimeSpecification.HOURS_FROM_START)) {
      assignment = dateTimeSpec
        .getAssignment(DateTimeSpecification.HOURS_FROM_START);
    } else if (dateTimeSpec
      .isAssigned(DateTimeSpecification.SECONDS_FROM_START)) {
      assignment = dateTimeSpec
        .getAssignment(DateTimeSpecification.SECONDS_FROM_START);
    }

    if (null != assignment) {
      try {
        HighlightedString matchedLine = fileDefinition.getHeaderLine(contents,
          assignment.getPrefix(), assignment.getSuffix());
        headerDate = LocalDateTime.parse(matchedLine.getHighlightedPortion(),
          assignment.getFormatter());
      } catch (Exception e) {
        ExceptionUtils.printStackTrace(e);
        addMessage(
          "Could not extract file start date from header: " + e.getMessage());
        result = false;
      }
    }

    return result;
  }

  /**
   * Get the time of the first record in the file. Time offset will not be
   * applied. Lines with invalid/missing dates are ignored.
   *
   * @return The date, or null if the date cannot be retrieved
   */
  public LocalDateTime getRawStartTime() {
    if (null == startDate) {
      try {
        loadContents();
        LocalDateTime foundDate = null;
        int searchLine = getFirstDataLine() - 1;
        int lastLine = getContentLineCount() - 1;

        while (null == foundDate && searchLine <= lastLine) {
          searchLine++;

          try {
            foundDate = getRawTime(searchLine);
          } catch (Exception e) {
            // Ignore errors and try the next line
          }
        }

        if (null != foundDate) {
          startDate = foundDate;
        } else {
          addMessage("No valid dates in file");
        }

      } catch (DataFileException e) {
        addMessage("Unable to extract data from file");
      }
    }

    return startDate;
  }

  public LocalDateTime getStart() {
    return getRawStartTime();
  }

  /**
   * Get the time of the last record in the file. Time offset will not be
   * applied.
   *
   * @return The date, or null if the date cannot be retrieved
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  public LocalDateTime getRawEndTime() {
    if (null == endDate) {
      try {
        loadContents();
        LocalDateTime foundDate = null;
        int firstLine = getFirstDataLine();
        int searchLine = getContentLineCount();

        while (null == foundDate && searchLine >= firstLine) {
          searchLine--;

          try {
            foundDate = getRawTime(searchLine);
          } catch (Exception e) {
            // Ignore errors and try the next line
          }
        }

        if (null != foundDate) {
          endDate = foundDate;
        } else {
          addMessage("No valid dates in file");
        }

      } catch (DataFileException e) {
        addMessage("Unable to extract data from file");
      }
    }

    return endDate;
  }

  public LocalDateTime getEnd() {
    return getRawEndTime();
  }

  public LocalDateTime getOffsetStartTime() {
    return applyTimeOffset(getRawStartTime());
  }

  public LocalDateTime getOffsetEndTime() {
    return applyTimeOffset(getRawEndTime());
  }

  public LocalDateTime getStartTime(boolean applyOffset) {
    return applyOffset ? getOffsetStartTime() : getRawStartTime();
  }

  public LocalDateTime getEndTime(boolean applyOffset) {
    return applyOffset ? getOffsetEndTime() : getRawEndTime();
  }

  /**
   * Get the time of a line in the file, without the define offset applied
   *
   * @param line
   *          The line
   * @return The time
   * @throws DataFileException
   *           If any date/time fields are empty
   * @throws MissingDateTimeException
   */
  public LocalDateTime getRawTime(int line) throws DataFileException,
    DateTimeSpecificationException, MissingDateTimeException {
    loadContents();
    return getRawTime(fileDefinition.extractFields(contents.get(line)));
  }

  public LocalDateTime getOffsetTime(List<String> line)
    throws DataFileException, DateTimeSpecificationException,
    MissingDateTimeException {

    return applyTimeOffset(getRawTime(line));
  }

  public LocalDateTime getRawTime(List<String> line)
    throws DateTimeSpecificationException, MissingDateTimeException {

    return fileDefinition.getDateTimeSpecification().getDateTime(headerDate,
      line);
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does
   * not contain run types
   *
   * @param line
   *          The line
   * @return The run type for the line
   * @throws DataFileException
   *           If the data cannot be extracted
   * @throws FileDefinitionException
   *           If the run types are invalid
   */
  public String getRunType(int line)
    throws DataFileException, FileDefinitionException {
    String runType = null;

    if (fileDefinition.hasRunTypes()) {
      loadContents();
      runType = fileDefinition.getRunType(contents.get(line), true)
        .getRunName();
    }

    return runType;
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does
   * not contain run types
   *
   * @param line
   *          The line
   * @return The run type for the line
   * @throws DataFileException
   *           If the data cannot be extracted
   * @throws FileDefinitionException
   *           If the run types are invalid
   */
  public RunTypeCategory getRunTypeCategory(int line)
    throws DataFileException, FileDefinitionException {
    RunTypeCategory runType = null;

    if (fileDefinition.hasRunTypes()) {
      loadContents();
      runType = fileDefinition.getRunTypeCategory(contents.get(line));
    }

    return runType;
  }

  public String getLongitude(List<String> line) throws PositionException {
    return fileDefinition.getLongitudeSpecification().getValue(line);
  }

  public String getLatitude(List<String> line) throws PositionException {
    return fileDefinition.getLatitudeSpecification().getValue(line);
  }

  /**
   * Get a {@link Double} value from a field.
   * <p>
   * Returns {@code null} if the field string is empty, or the field equals the
   * supplied {@code missingValue} (if it is supplied).
   * </p>
   *
   * @param field
   *          The field
   * @param missingValue
   *          The 'missing' value for the field
   * @return The numeric field value
   * @throws ValueNotNumericException
   *           If the field value is not numeric
   */
  public static Double extractDoubleFieldValue(String field,
    String missingValue) throws ValueNotNumericException {
    Double result = null;

    if (null != field && field.trim().length() > 0) {
      if (null == missingValue || !field.equals(missingValue)) {
        try {
          result = Double.parseDouble(field.replace(",", ""));
        } catch (NumberFormatException e) {
          throw new ValueNotNumericException();
        }
      }
    }

    return result;
  }

  /**
   * Get a {@link Integer} value from a field.
   * <p>
   * Returns {@code null} if the field string is empty, or the field equals the
   * supplied {@code missingValue} (if it is supplied).
   * </p>
   *
   * @param field
   *          The field
   * @param missingValue
   *          The 'missing' value for the field
   * @return The numeric field value
   * @throws ValueNotNumericException
   *           If the field value is not numeric
   */
  public static Integer extractIntFieldValue(String field, String missingValue)
    throws ValueNotNumericException {
    Integer result = null;

    if (null != field && field.trim().length() > 0) {
      if (null == missingValue || !field.equals(missingValue)) {
        try {
          result = Integer.parseInt(field);
        } catch (NumberFormatException e) {
          throw new ValueNotNumericException();
        }
      }
    }

    return result;
  }

  /**
   * Get a {@link String} value from a field.
   * <p>
   * If the field is empty, or equals the supplied {@code missingValue},
   * {@code null} is returned.
   * </p>
   *
   * @param field
   *          The field
   * @param missingValue
   *          The 'missing' value for the field
   * @return The field value
   */
  public static String extractStringFieldValue(String field,
    String missingValue) {
    String result = field;

    if (null != field) {
      result = field.trim();

      if (null != missingValue && !field.equals(missingValue)) {
        result = null;
      }
    }

    return result;
  }

  /**
   * Get the file's database ID
   *
   * @return The database ID
   */
  public long getDatabaseId() {
    return databaseId;
  }

  /**
   * Set the file's database ID
   *
   * @param databaseId
   *          The ID
   */
  protected void setDatabaseId(long databaseId) {
    this.databaseId = databaseId;
  }

  /**
   * Get the file definition object
   *
   * @return The file definition
   */
  public FileDefinition getFileDefinition() {
    return fileDefinition;
  }

  /**
   * Get the contents of the file as a single string
   *
   * @return The file contents
   * @throws DataFileException
   *           If the file contents cannot be retrieved
   */
  public String getContents() throws DataFileException {
    loadContents();

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < getContentLineCount(); i++) {
      result.append(contents.get(i));

      if (i < getContentLineCount() - 1) {
        result.append('\n');
      }
    }

    return result.toString();
  }

  /**
   * Set the contents of the data file
   *
   * @param contents
   *          The contents
   */
  protected void setContents(String contents) {
    this.contents = new ArrayList<String>(
      Arrays.asList(contents.split("[\\r\\n]+")));

    StringUtils.removeBlankTailLines(this.contents);
  }

  /**
   * Get the raw bytes for a file
   *
   * @return The file
   * @throws IOException
   *           If the file cannot be read
   */
  public abstract byte[] getBytes() throws IOException;

  /**
   * Load the contents of the data file from disk, if they are not already
   * loaded
   *
   * @throws DataFileException
   *           If the file contents could not be loaded
   */
  private void loadContents() throws DataFileException {

    boolean doLoad = false;

    if (null == fileWithContents) {
      doLoad = true;
    } else if (fileWithContents != this) {
      fileWithContents.contents = null;
      fileWithContents = null;
      doLoad = true;
    }

    if (doLoad) {
      try {
        loadAction();
        fileWithContents = this;

        if (!extractHeaderDate()) {
          throw new Exception("Could not extract file header date");
        }

        recordCount = getContentLineCount() - getFirstDataLine();
      } catch (Exception e) {
        throw new DataFileException(databaseId,
          DataFileException.NO_LINE_NUMBER, "Error while loading file contents",
          e);
      }
    }
  }

  protected abstract void loadAction() throws DataFileException;

  /**
   * Get a value from a field as a Double. If the extracted value equals the
   * {@code missingValue}, the method returns {@code null}.
   *
   * @param line
   *          The line
   * @param field
   *          The field index
   * @param missingValue
   *          The string indicating a missing value
   * @return The value
   * @throws DataFileException
   *           If the data cannot be extracted
   */
  public Double getDoubleValue(int line, int field, String missingValue)
    throws DataFileException {
    loadContents();
    String fieldValue = fileDefinition.extractFields(contents.get(line))
      .get(field);

    Double result = null;

    try {
      result = extractDoubleFieldValue(fieldValue, missingValue);
    } catch (ValueNotNumericException e) {
      throw new DataFileException(databaseId, line, e);
    }

    return result;
  }

  /**
   * Get a field value from a line. If the line does not have enough fields, or
   * the field is the defined missing value, returns {@code null}
   *
   * Any commas in a data value are assumed to be thousands separators and
   * removed from the string. QuinCe does not support commas as decimal points.
   *
   * @param line
   *          The line containing the value
   * @param field
   *          The field to retrieve
   * @param missingValue
   *          The defined missing value
   * @return The extracted value
   */
  public String getStringValue(String jobName, DataSet dataSet, int lineNumber,
    List<String> line, int field, String missingValue) {
    String result = null;

    if (field < line.size()) {
      result = StringUtils.removeFromString(line.get(field).trim(), ',');
      if (result.length() == 0 || result.equals(missingValue)
        || result.equalsIgnoreCase("NaN") || result.equalsIgnoreCase("NA")) {
        result = null;
      } else {

        // Strip leading zeros from integers - otherwise we get octal number
        // nonsense. (Unless it's a zero to begin with.)
        if (!result.equals("0") && !StringUtils.contains(result, '.')) {

          result = StringUtils.stripStart(result, '0');
        }

        if (!NumberUtils.isCreatable(result)) {
          dataSet.addProcessingMessage(jobName, this, lineNumber,
            "Invalid value '" + result + "'");
          result = null;
        }
      }
    }

    return result;
  }

  /**
   * Get the list of run type values with the specified value excluded. This
   * list will include all the run types from the stored file definition plus
   * any missing run types (except that specified as the exclusion).
   *
   * @param exclusion
   *          The value to exclude from the list
   * @return The list of run types without the excluded value
   */
  public List<String> getRunTypeValuesWithExclusion(String exclusion) {
    List<String> runTypeValues = fileDefinition.getRunTypeValues();
    for (RunTypeAssignment runTypeAssignment : missingRunTypes) {
      if (!runTypeAssignment.getRunName().equals(exclusion)) {
        runTypeValues.add(runTypeAssignment.getRunName());
      }
    }

    return runTypeValues;
  }

  /**
   * Get a line from the file as a list of field values
   *
   * @param line
   *          The line number
   * @return The line fields
   * @throws DataFileException
   */
  public List<String> getLine(int line) throws DataFileException {
    loadContents();
    return fileDefinition.extractFields(contents.get(line));
  }

  public Properties getProperties() {
    return properties;
  }

  public void setTimeOffset(int seconds) {
    properties.setProperty(TIME_OFFSET_PROP, String.valueOf(seconds));
  }

  public int getTimeOffset() {
    return Integer.parseInt(properties.getProperty(TIME_OFFSET_PROP));
  }

  public boolean hasTimeOffset() {
    return getTimeOffset() != 0;
  }

  private LocalDateTime applyTimeOffset(LocalDateTime rawTime) {
    return rawTime.plusSeconds(getTimeOffset());
  }

  /**
   * Calculate the mean length (in seconds) of collection of DataFiles.
   *
   * <p>
   * The length of each file is calculated as {@code endDate - startDate}, so
   * files with a single line will be zero seconds long.
   * </p>
   *
   * @param files
   *          The files.
   * @return The mean file length.
   */
  public static double getMeanFileLength(Collection<DataFile> files) {
    MeanCalculator mean = new MeanCalculator();
    files.forEach(
      f -> mean.add(DateTimeUtils.secondsBetween(f.startDate, f.endDate)));
    return mean.mean();
  }

  /**
   * Determines whether or not this file overlaps the specified time period.
   *
   * <p>
   * The file's time offset is applied before the check.
   * </p>
   *
   * @param start
   *          The start of the period to check.
   * @param end
   *          The end of the period to check.
   * @return {@code true} if this file overlaps the time period; {@code false}
   *         if it does not.
   */
  public boolean overlaps(LocalDateTime start, LocalDateTime end) {
    return DateTimeUtils.overlap(getOffsetStartTime(), getOffsetEndTime(),
      start, end);
  }

  /**
   * Determines whether or not this file overlaps another file.
   *
   * <p>
   * The file's time offset is applied before the check.
   * </p>
   *
   * @param other
   *          The file to check against this file.
   * @return {@code true} if both files overlap; {@code false} if they do not.
   */
  public boolean overlaps(DataFile other) {
    return DateTimeUtils.overlap(getOffsetStartTime(), getOffsetEndTime(),
      other.getOffsetStartTime(), other.getOffsetEndTime());
  }

  @Override
  public String toString() {
    return filename;
  }

  /**
   * Determine whether the supplied {@link Collection} of {@link DataFile}s
   * contains at least one period where there is an overlapping file for all the
   * {@link Instrument}'s {@link FileDefinitions} within the specified time
   * period.
   *
   * <p>
   * This method can tell us whether the set of {@link DataFile}s can be used to
   * create a {@link DataSet} covering the specified period.
   * </p>
   *
   * @param instrument
   *          The {@link Instrument} whose files are being examined.
   * @param files
   *          The files.
   * @param start
   *          The start time of the required period.
   * @param end
   *          The end time of the required period.
   * @return
   */
  public static boolean hasConcurrentFiles(Instrument instrument,
    Collection<DataFile> files, LocalDateTime start, LocalDateTime end) {

    /*
     * TODO This could be a little more efficient if we ensure that all the
     * incoming files are in time order. Then we could break out as soon as we
     * run off the end time before establishing the final result.
     *
     * In practice the files will almost certainly be in time order anyway, so
     * unless I explicitly see this being a performance problem I won't bother.
     */

    boolean result = false;

    // If there's only one FileDefinition, we can just find any file with data
    // between the start and end.
    if (instrument.getFileDefinitions().size() == 1) {
      result = files.stream().anyMatch(f -> f.overlaps(start, end));
    } else {
      // Build a Map of the files that encompass the required period
      // grouped by FileDefinition
      HashMap<FileDefinition, List<DataFile>> map = new HashMap<FileDefinition, List<DataFile>>();

      List<FileDefinition> fileDefinitions = instrument.getFileDefinitions();

      fileDefinitions.forEach(fd -> map.put(fd, new ArrayList<DataFile>()));

      files.stream().filter(f -> f.overlaps(start, end))
        .forEach(f -> map.get(f.getFileDefinition()).add(f));

      for (DataFile checkFile : map.get(fileDefinitions.get(0))) {

        boolean setComplete = true;

        for (int i = 1; i < instrument.getFileDefinitions().size(); i++) {
          boolean overlapFound = false;

          for (DataFile defFile : map.get(fileDefinitions.get(i))) {
            if (checkFile.overlaps(defFile)) {
              overlapFound = true;
              break;
            }
          }

          if (!overlapFound) {
            setComplete = false;
            break;
          }
        }

        if (setComplete) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  private List<Instrument> getPreviousInstruments()
    throws MissingParamException, VariableNotFoundException, DatabaseException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {

    List<Instrument> result = null;

    if (null == previousInstruments) {
      if (null == instrument) {
        result = new ArrayList<Instrument>();
      } else {
        List<Instrument> instruments = InstrumentDB.getInstrumentList(
          ResourceManager.getInstance().getDBDataSource(),
          instrument.getOwner());

        result = Instrument.filterByPlatform(instruments,
          instrument.getPlatformName(), instrument.getPlatformCode(),
          instrument.getId());
      }
    } else {
      result = previousInstruments;
    }

    return result;
  }
}
