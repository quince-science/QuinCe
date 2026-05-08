package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.lang3.math.NumberUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class representing a data file.
 */
public abstract class DataFile implements Comparable<DataFile> {

  /**
   * The database ID of this file
   */
  private long databaseId = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The file format definition
   */
  protected FileDefinition fileDefinition;

  /**
   * The file name
   */
  private String filename;

  /**
   * The number of records in the file
   */
  private int recordCount = -1;

  /**
   * The file contents
   */
  private FileContents contents;

  /**
   * Messages generated regarding the file
   */
  private TreeSet<DataFileMessage> messages;

  /**
   * Misc properties
   */
  protected Properties properties;

  /**
   * Max number of messages. Additional messages will be discarded.
   */
  private static final int MAX_MESSAGE_COUNT = 25;

  /**
   * Row in the file where the error happened not set
   */
  private static final int ROW_NOT_SET = -1;

  /**
   * The {@link Instrument} that this file belongs to.
   */
  protected final Instrument instrument;

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
   * @throws MissingParamException
   *           If any fields are null
   */
  public DataFile(Instrument instrument, FileDefinition fileDefinition,
    String filename, FileContents contents)
    throws MissingParamException, DataFileException {

    MissingParam.checkMissing(fileDefinition, "fileDefinition");
    MissingParam.checkMissing(filename, "fileName");

    this.instrument = instrument;
    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.contents = contents;
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
    String filename, int recordCount, Properties properties) {

    this.databaseId = id;
    this.instrument = instrument;
    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.contents = null;
    this.recordCount = recordCount;
    this.properties = properties;
  }

  protected Properties defaultProperties() {
    return new Properties();
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
    return fileDefinition.getHeaderLength(getContents().get())
      + fileDefinition.getColumnHeaderRows();
  }

  /**
   * Get the number of rows in the file
   *
   * @return The row count
   * @throws DataFileException
   */
  public int getContentLineCount() throws DataFileException {
    return getContents().size();
  }

  /**
   * Get the number of records in the file.
   *
   * @return The record count
   */
  public int getRecordCount() throws DataFileException {
    if (-1 == recordCount) {
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
      result = StringUtils.trimList(Arrays
        .asList(getContents().get(row).split(fileDefinition.getSeparator())));
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
    boolean continueValidation = true;

    // Check that there is actually data in the file
    try {
      getFirstDataLine();
    } catch (DataFileException e) {
      addMessage("File does not contain any data");
      continueValidation = false;
    }

    if (continueValidation) {
      validateWorker();
    }
  }

  /**
   * Extra validation steps.
   *
   * <p>
   * Can be overridden by concrete implementations.
   * </p>
   *
   * @throws DataFileException
   *           If any validation fails.
   */
  protected void validateWorker() throws DataFileException {
    // The default implementation does nothing.
  }

  /**
   * Shortcut method for adding a message to the message list. When the list
   * size reaches MAX_MESSAGE_COUNT messages, a final message saying "Too many
   * messages..." is added, then no more messages are allowed.
   *
   * @param lineNumber
   *          The line number. Line number < 0 means no line number.
   * @param message
   *          The message text
   */
  protected void addMessage(int lineNumber, String message) {
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
  protected void addMessage(String message) {
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

  protected FileContents getContents() {
    if (null == contents) {
      contents = DataFileDB.getFileContents(getFileDefinition().getDatabaseId(),
        databaseId);
    }

    return contents;
  }

  /**
   * Get the contents of the file as a single string
   *
   * @return The file contents
   * @throws DataFileException
   *           If the file contents cannot be retrieved
   */
  public String getContentsAsString() throws DataFileException {
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < getContentLineCount(); i++) {
      result.append(getContents().get(i));

      if (i < getContentLineCount() - 1) {
        result.append('\n');
      }
    }

    return result.toString();
  }

  /**
   * Get the raw bytes for a file
   *
   * @return The file
   * @throws IOException
   *           If the file cannot be read
   */
  public byte[] getBytes() throws IOException {
    return getContents().getBytes();
  }

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
    String fieldValue = fileDefinition.extractFields(getContents().get(line))
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
   * Get a line from the file as a list of field values
   *
   * @param line
   *          The line number
   * @return The line fields
   * @throws DataFileException
   */
  public List<String> getLine(int line) throws DataFileException {
    return fileDefinition.extractFields(getContents().get(line));
  }

  public Properties getProperties() {
    return properties;
  }

  @Override
  public String toString() {
    return filename;
  }

  protected List<Instrument> getPreviousInstruments()
    throws MissingParamException, VariableNotFoundException, DatabaseException,
    RecordNotFoundException, InstrumentException, SensorGroupsException,
    ClassNotFoundException {

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

  /**
   * Run actions after the file contents have been loaded.
   *
   * @throws DataFileException
   *           If the hook fails.
   */
  protected void postLoadHook() throws DataFileException {
    // Default action does nothing.
  }

  /**
   * Get the start point of the file.
   *
   * <p>
   * This can be an encoded String specific to the implementation, and may not
   * be suitable for display to end users.
   * </p>
   *
   * @see #getStartDisplayString()
   *
   * @return The start point.
   */
  public abstract String getStartString() throws DataFileException;

  /**
   * Get the start point of the file for display.
   *
   * <p>
   * The default implementation returns the output of {@link #getStartString()}.
   * </p>
   *
   * @return The start point.
   */
  public String getStartDisplayString() throws DataFileException {
    return getStartString();
  }

  /**
   * Get the end point of the file.
   *
   * <p>
   * This can be an encoded String specific to the implementation, and may not
   * be suitable for display to end users.
   * </p>
   *
   * @see #getEndDisplayString()
   *
   * @return The end point.
   */
  public abstract String getEndString() throws DataFileException;

  /**
   * Get the end point of the file for display.
   *
   * <p>
   * The default implementation returns the output of {@link #getEndString()}.
   * </p>
   *
   * @return The end point.
   */
  public String getEndDisplayString() throws DataFileException {
    return getEndString();
  }

  /**
   * Get a list of existing files that overlap with this file.
   *
   * @return The overlapping files.
   */
  public abstract TreeSet<DataFile> getOverlappingFiles(
    TreeSet<DataFile> allFiles);

  /**
   * Get any extra properties that should be included in the export manifest.
   *
   * @return
   */
  public Properties getExportProperties() {
    return new Properties();
  }

  protected Instrument getInstrument() {
    return instrument;
  }

  /**
   * Determine whether or not this file has a fundamental issue which means it
   * cannot be processed.
   *
   * @return {@code true} if the file cannot be processed; {@code false} if it
   *         is OK.
   */
  public abstract boolean hasFundametalProcessingIssue();

  /**
   * The data file item that causes {@link #hasFundametalProcessingIssue()} to
   * return {@code true}.
   *
   * <p>
   * The value returned by this method will be included in messages of the form
   * {@code The file has &lt;item&gt; issues}.
   *
   * @return
   */
  public abstract String getFundamentalProcessingIssueItem();
}
