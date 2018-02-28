package uk.ac.exeter.QuinCe.data.Files;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunType;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Class representing a data file
 * @author Steve Jones
 *
 */
public class DataFile {

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
  private List<String> contents;

  /**
   * Messages generated regarding the file
   */
  private TreeSet<DataFileMessage> messages;

  /**
   * The date in the file header
   */
  private LocalDateTime headerDate = null;

  /**
   * The location of the file store
   */
  private String fileStore;

  /**
   * Run types in this file not defined in the file definition
   */
  private Set<RunType> missingRunTypes = new HashSet<>();


  public List<RunType> getMissingRunTypes() {
    List<RunType> list = new ArrayList<>(missingRunTypes);
    Collections.sort(list);
    return list;
  }

  /**
   * Create a DataFile with the specified definition and contents
   * @param fileStore The location of the file store
   * @param fileDefinition The file format definition
   * @param filename The file name
   * @param contents The file contents
   * @throws MissingParamException If any fields are null
   */
  public DataFile(String fileStore, FileDefinition fileDefinition, String filename, List<String> contents) throws MissingParamException {
    MissingParam.checkMissing(fileDefinition, "fileDefinition");
    MissingParam.checkMissing(filename, "fileName");
    MissingParam.checkMissing(contents, "contents");

    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.contents = contents;

    messages = new TreeSet<DataFileMessage>();
    boolean fileOK = false;

    try {
      fileOK = extractHeaderDate();
    } catch (Exception e) {
      // Since we were provided with the file contents,
      // we won't be loading them so we can't get an exception here.
    }

    if (fileOK) {
      try {
        validate();
      } catch (Exception e) {
        // Since we were provided with the file contents,
        // we won't be loading them so we can't get an exception here.
      }
    }
  }

  /**
   * Constructor for the basic details of a file. The contents will
   * be loaded on demand if required.
   * @param fileStore The file store location
   * @param id The file's database ID
   * @param fileDefinition The file definition for the file
   * @param filename The filename
   * @param startDate The date/time of the first record in the file
   * @param endDate The date/time of the last record in the file
   * @param recordCount The number of records in the file
   */
  public DataFile(String fileStore, long id, FileDefinition fileDefinition, String filename, LocalDateTime startDate, LocalDateTime endDate, int recordCount) {
    this.fileStore = fileStore;
    this.databaseId = id;
    this.fileDefinition = fileDefinition;
    this.filename = filename;
    this.startDate = startDate;
    this.endDate = endDate;
    this.recordCount = recordCount;
  }

  /**
   * Get the file format description
   * @return The file format description
   */
  public String getFileDescription() {
    return fileDefinition.getFileDescription();
  }

  /**
   * Get the file name
   * @return The file name
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Set a new file definition for the file. The new definition must match
   * the layout of the previous definition, since the file's format must
   * conform to both the new and old definitions.
   *
   * @param newDefinition The new file definition
   * @throws FileDefinitionException The the definition does not match the file layout
   */
  public void setFileDefinition(FileDefinition newDefinition) throws FileDefinitionException {
    if (!fileDefinition.matchesLayout(newDefinition)) {
      throw new FileDefinitionException("File Definition does not match file contents");
    } else {
      this.fileDefinition = newDefinition;
    }
  }

  /**
   * Get the zero-based row number of the first data row
   * in a file
   * @return The first data row number
   * @throws DataFileException If the end of the file is reached without finding the end of the header
   */
  public int getFirstDataLine() throws DataFileException {
    loadContents();
    return fileDefinition.getHeaderLength(contents) + fileDefinition.getColumnHeaderRows();
  }

  /**
   * Get the number of rows in the file
   * @return The row count
   */
  public int getLineCount() {
    return contents.size();
  }

  /**
   * Get the number of records in the file
   * @return The record count
   * @throws DataFileException If the record count cannot be calculated
   */
  public int getRecordCount() throws DataFileException {
    if (-1 == recordCount) {
      loadContents();
      recordCount = contents.size() - getFirstDataLine();
    }

    return recordCount;
  }

  /**
   * Get the data from a specified row in the file as a list of string fields.
   * This is the row position in the whole file, including headers.
   * @param row The row to be retrieved
   * @return The row fields
   * @throws DataFileException If the requested row is outside the bounds of the file
   */
  public List<String> getRowFields(int row) throws DataFileException {
    List<String> result;

    if (row < getFirstDataLine()) {
      throw new DataFileException("Requested row " + row + " is in the file header");
    } else if (row > (contents.size() - 1)) {
      throw new DataFileException("Requested row " + row + " is in the file header");
    } else {
      result = StringUtils.trimList(Arrays.asList(contents.get(row).split(fileDefinition.getSeparator())));
    }

    return result;
  }

  /**
   * Validate the file contents.
   * Creates a set of {@code DataFileMessage}
   * objects, which can be retrieved using {@code getMessages()}.
   * @throws DataFileException If the file contents could not be loaded
   */
  public void validate() throws DataFileException {

    loadContents();

    // Check that there is actually data in the file
    int firstDataLine = -1;
    try {
      firstDataLine = getFirstDataLine();
    } catch (DataFileException e) {
      addMessage("File does not contain any data");
    }

    if (firstDataLine > -1) {

      // For each line in the file, check that:
      // (a) The date/time are present and monotonic in the file
      // (b) Has the correct number of columns (for Run Types that aren't IGNORED)
      // (c) The Run Type is recognised

      LocalDateTime lastDateTime = null;
      for (int lineNumber = firstDataLine; lineNumber < contents.size(); lineNumber++) {
        String line = contents.get(lineNumber);

        try {
          LocalDateTime dateTime = fileDefinition.getDateTimeSpecification().getDateTime(headerDate, fileDefinition.extractFields(line));
          if (null != lastDateTime) {
            if (dateTime.compareTo(lastDateTime) <= 0) {
              addMessage(lineNumber, "Date/Time is not monotonic");
            }
          }
        } catch (DataFileException e) {
          addMessage(lineNumber, e.getMessage());
        }

        boolean checkColumnCount = true;

        if (fileDefinition.hasRunTypes()) {
          try {
            RunTypeCategory runType = fileDefinition.getRunTypeCategory(line);
            if (runType.equals(RunTypeCategory.IGNORED_CATEGORY)) {
              checkColumnCount = false;
            }
          } catch (FileDefinitionException e) {
            addMessage(lineNumber, e.getMessage());
            if (e instanceof MissingRunTypeException) {
              missingRunTypes.add(((MissingRunTypeException)e).getRunType());
            }
          }
        }

        if (checkColumnCount && fileDefinition.extractFields(line).size() != fileDefinition.getColumnCount()) {
          addMessage(lineNumber, "Incorrect number of columns");
        }
      }
    }
  }

  /**
   * Shortcut method for adding a message to the message list
   * @param lineNumber The line number
   * @param message The message text
   */
  private void addMessage(int lineNumber, String message) {
    messages.add(new DataFileMessage(lineNumber, message));
  }

  /**
   * Shortcut method for adding a message to the message list
   * @param message The message text
   */
  private void addMessage(String message) {
    messages.add(new DataFileMessage(message));
  }

  /**
   * Get the messages generated for this file
   * @return The messages
   */
  public TreeSet<DataFileMessage> getMessages() {
    return messages;
  }

  /**
   * Get the number of messages that have been generated
   * for this file
   * @return The message count
   */
  public int getMessageCount() {
    return messages.size();
  }

  /**
   * Get the start date from the file header. This is only applicable
   * if the date format is {@link DateTimeSpecification#HOURS_FROM_START}.
   * @return {@code true} if the header date is successfully extracted; {@code false} if the date cannot be extracted
   * @throws DataFileException If the file contents could not be loaded
   */
  private boolean extractHeaderDate() throws DataFileException {

    loadContents();

    boolean result = true;

    DateTimeSpecification dateTimeSpec = fileDefinition.getDateTimeSpecification();
    if (dateTimeSpec.isAssigned(DateTimeSpecification.HOURS_FROM_START)) {
      try {
        DateTimeColumnAssignment assignment = dateTimeSpec.getAssignment(DateTimeSpecification.HOURS_FROM_START);
        HighlightedString matchedLine = fileDefinition.getHeaderLine(contents, assignment.getPrefix(), assignment.getSuffix());
        headerDate = LocalDateTime.parse(matchedLine.getHighlightedPortion(), assignment.getFormatter());
      } catch (Exception e) {
        e.printStackTrace();
        addMessage("Could not extract file start date from header: " + e.getMessage());
        result = false;
      }
    }

    return result;
  }

  /**
   * Get the date of the first record in the file
   * @return The date, or null if the date cannot be retrieved
   */
  public LocalDateTime getStartDate() {
    if (null == startDate) {
      try {
        startDate = getDate(getFirstDataLine());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return startDate;
  }

  /**
   * Get the date of the last record in the file
   * @return The date, or null if the date cannot be retrieved
   * @throws DataFileException If the file contents could not be loaded
   */
  public LocalDateTime getEndDate() throws DataFileException {
    if (null == endDate) {
      loadContents();

      try {
        endDate = getDate(contents.size() - 1);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return endDate;
  }

  /**
   * Get the date of a line in the file
   * @param line The line
   * @return The date
   * @throws DataFileException If any date/time fields are empty
   */
  public LocalDateTime getDate(int line) throws DataFileException {
    loadContents();
    return fileDefinition.getDateTimeSpecification().getDateTime(headerDate, fileDefinition.extractFields(contents.get(line)));
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does not contain run types
   * @param line The line
   * @return The run type for the line
   * @throws DataFileException If the data cannot be extracted
   * @throws FileDefinitionException If the run types are invalid
   */
  public String getRunType(int line) throws DataFileException, FileDefinitionException {
    String runType = null;
    int runTypeColumn = fileDefinition.getRunTypeColumn();

    if (runTypeColumn > -1) {
      loadContents();
      runType = fileDefinition.getRunType(contents.get(line));
    }

    return runType;
  }

  /**
   * Get the run type for a given line. Returns {@code null} if this file does not contain run types
   * @param line The line
   * @return The run type for the line
   * @throws DataFileException If the data cannot be extracted
   * @throws FileDefinitionException If the run types are invalid
   */
  public RunTypeCategory getRunTypeCategory(int line) throws DataFileException, FileDefinitionException {
    RunTypeCategory runType = null;
    int runTypeColumn = fileDefinition.getRunTypeColumn();

    if (runTypeColumn > -1) {
      loadContents();
      runType = fileDefinition.getRunTypeCategory(contents.get(line));
    }

    return runType;
  }

  /**
   * Get the longitude for a given line
   * @param line The line
   * @return The longitude
   * @throws DataFileException If the file contents cannot be extracted
   * @throws PositionException If the longitude is invalid
   */
  public double getLongitude(int line) throws DataFileException, PositionException {
    loadContents();
    return fileDefinition.getLongitudeSpecification().getValue(fileDefinition.extractFields(contents.get(line)));
  }

  /**
   * Get the latitude for a given line
   * @param line The line
   * @return The longitude
   * @throws DataFileException If the file contents cannot be extracted
   * @throws PositionException If the latitude is invalid
   */
  public double getLatitude(int line) throws DataFileException, PositionException {
    loadContents();
    return fileDefinition.getLatitudeSpecification().getValue(fileDefinition.extractFields(contents.get(line)));
  }

  /**
   * Get a {@link Double} value from a field.
   * <p>
   *   Returns {@code null} if the field string is empty, or the field
   *   equals the supplied {@code missingValue} (if it is supplied).
   * </p>
   *
   * @param field The field
   * @param missingValue The 'missing' value for the field
   * @return The numeric field value
   * @throws ValueNotNumericException If the field value is not numeric
   */
  public static Double extractDoubleFieldValue(String field, String missingValue) throws ValueNotNumericException {
    Double result = null;

    if (null != field && field.trim().length() > 0) {
      if (null == missingValue || !field.equals(missingValue)) {
        try {
          result = Double.parseDouble(field);
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
   *   Returns {@code null} if the field string is empty, or the field
   *   equals the supplied {@code missingValue} (if it is supplied).
   * </p>
   *
   * @param field The field
   * @param missingValue The 'missing' value for the field
   * @return The numeric field value
   * @throws ValueNotNumericException If the field value is not numeric
   */
  public static Integer extractIntFieldValue(String field, String missingValue) throws ValueNotNumericException {
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
   *   If the field is empty, or equals the supplied {@code missingValue},
   *   {@code null} is returned.
   * </p>
   *
   * @param field The field
   * @param missingValue The 'missing' value for the field
   * @return The field value
   */
  public static String extractStringFieldValue(String field, String missingValue) {
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
   * @return The database ID
   */
  public long getDatabaseId() {
    return databaseId;
  }

  /**
   * Set the file's database ID
   * @param databaseId The ID
   */
  protected void setDatabaseId(long databaseId) {
    this.databaseId = databaseId;
  }

  /**
   * Get the file definition object
   * @return The file definition
   */
  public FileDefinition getFileDefinition() {
    return fileDefinition;
  }

  /**
   * Get the contents of the file as a single string
   * @return The file contents
   * @throws DataFileException If the file contents cannot be retrieved
   */
  public String getContents() throws DataFileException {
    loadContents();

    StringBuilder result = new StringBuilder();

    for (int i = 0; i < contents.size(); i++) {
      result.append(contents.get(i));

      if (i < contents.size() - 1) {
        result.append('\n');
      }
    }

    return result.toString();
  }

  /**
   * Set the contents of the data file
   * @param contents The contents
   */
  protected void setContents(String contents) {
    this.contents = Arrays.asList(contents.split("\n"));
  }

  /**
   * Load the contents of the data file from disk, if they are not already loaded
   * @throws DataFileException If the file contents could not be loaded
   */
  private void loadContents() throws DataFileException {
    if (null == contents) {
      try {
        FileStore.loadFileContents(fileStore, this);

        if (!extractHeaderDate()) {
          throw new Exception("Could not extract file header date");
        }
      } catch (Exception e) {
        throw new DataFileException("Error while loading file contents", e);
      }
    }
  }

  /**
   * Get a value from a field as a Double. If the extracted
   * value equals the {@code missingValue}, the method returns {@code null}.
   * @param line The line
   * @param field The field index
   * @param missingValue The string indicating a missing value
   * @return The value
   * @throws DataFileException If the data cannot be extracted
   */
  public Double getDoubleValue(int line, int field, String missingValue) throws DataFileException {
    loadContents();
    String fieldValue = fileDefinition.extractFields(contents.get(line)).get(field);
    return extractDoubleFieldValue(fieldValue, missingValue);
  }
}
