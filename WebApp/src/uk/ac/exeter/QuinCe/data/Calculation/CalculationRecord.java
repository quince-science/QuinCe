package uk.ac.exeter.QuinCe.data.Calculation;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import uk.ac.exeter.QCRoutines.config.ColumnConfig;
import uk.ac.exeter.QCRoutines.config.InvalidDataTypeException;
import uk.ac.exeter.QCRoutines.data.DataColumn;
import uk.ac.exeter.QCRoutines.data.DataRecord;
import uk.ac.exeter.QCRoutines.data.DataRecordException;
import uk.ac.exeter.QCRoutines.data.InvalidDataException;
import uk.ac.exeter.QCRoutines.data.NoSuchColumnException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Instance of the QCRoutines {@link DataRecord} for
 * calculated measurements.
 *
 * @author Steve Jones
 *
 */
public abstract class CalculationRecord extends DataRecord {

  /**
   * Dummy set of date/time columns for the QCRoutines code.
   * Set up in the {@code static} block
   */
  private static TreeSet<Integer> dateTimeColumns;

  /**
   * Dummy latitude column for the QCRoutines code
   */
  private static final int LATITUDE_COLUMN = -1;

  /**
   * Dummy longitude column for the QCRoutines code
   */
  private static final int LONGITUDE_COLUMN = -1;

  // Set up the dummy date/time columns
  static {
    dateTimeColumns = new TreeSet<Integer>();
    dateTimeColumns.add(-1);
  }

  /**
   * The dataset ID
   */
  private long datasetId;

  /**
   * The data set object
   */
  private DataSet dataSet = null;

  /**
   * The record date
   */
  private LocalDateTime date = null;

  /**
   * The longitude
   */
  private Double longitude = null;

  /**
   * The latitude
   */
  private Double latitude = null;

  /**
   * The calculation DB instance
   */
  protected CalculationDB calculationDB = null;

  /**
   * The flag set by the automatic QC
   */
  private Flag autoFlag = Flag.NOT_SET;

  /**
   * The flag set by the user
   */
  private Flag userFlag = Flag.NOT_SET;

  /**
   * The user QC message
   */
  private String userMessage = null;

  /**
   * The aliases of human-readable column headings to database column names
   */
  protected Map<String, String> columnAliases;

  /**
   * Create an empty calculation record for a given measurement in a given data set
   * @param datasetId The dataset ID
   * @param measurementId The measurement ID
   * @param columnConfig The column configuration for the QC routines
   */
  public CalculationRecord(long datasetId, long measurementId, ColumnConfig columnConfig) {
    super(measurementId, columnConfig);
    this.datasetId = datasetId;
    calculationDB = getCalculationDB();
    buildColumnAliases();
  }

  /**
   * Get the dataset ID
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  @Override
  public DateTime getTime() throws DataRecordException {
    return new org.joda.time.DateTime(DateTimeUtils.dateToLong(date), DateTimeZone.UTC);
  }

  @Override
  public TreeSet<Integer> getDateTimeColumns() {
    return dateTimeColumns;
  }

  @Override
  public double getLongitude() throws DataRecordException {
    return longitude;
  }

  @Override
  public int getLongitudeColumn() {
    return LONGITUDE_COLUMN;
  }

  @Override
  public double getLatitude() throws DataRecordException {
    return latitude;
  }

  @Override
  public int getLatitudeColumn() {
    return LATITUDE_COLUMN;
  }

  /**
   * Set the record date
   * @param date The date
   */
  public void setDate(LocalDateTime date) {
    this.date = date;
  }

  /**
   * Get the record date.
   *
   * Note that this returns the date as a
   * {@code java.time.LocalDateTime} object;
   * the {@link #getTime()} method returns
   * a {@code joda.time.DateTime} object for use with
   * the QC_Routines library.
   *
   * @return The record date
   */
  public LocalDateTime getDate() {
    return date;
  }

  /**
   * Set the longitude
   * @param longitude The longitude
   */
  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  /**
   * Set the latitude
   * @param latitude The latitude
   */
  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  /**
   * Load the record data from the database.
   *
   * Loads the position and date information itself, and then
   * calls the {@link #loadCalculationData(Connection)} method to get the calculation data.
   *
   * @param conn A database connection
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the record is not in the database
   * @throws InvalidDataException If a field cannot be added to the record
   * @throws MessageException If the automatic QC messages cannot be parsed
   * @throws NoSuchColumnException If the automatic QC messages cannot be parsed
   */
  public void loadData(Connection conn) throws MissingParamException, DatabaseException, RecordNotFoundException, InvalidDataException, NoSuchColumnException, MessageException {
    loadSensorData(conn);
    loadCalculationData(conn);
  }

  /**
   * Load the base sensor data for the measurement
   * @param conn A database connection
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the record is not in the database
   * @throws InvalidDataException If a value cannot be added to the record
   */
  private void loadSensorData(Connection conn) throws MissingParamException, DatabaseException, RecordNotFoundException, InvalidDataException {
    dataSet = DataSetDB.getDataSet(conn, datasetId);
    DataSetRawDataRecord sensorData = DataSetDataDB.getMeasurement(conn, dataSet, lineNumber);

    date = sensorData.getDate();
    longitude = sensorData.getLongitude();
    latitude = sensorData.getLatitude();

    // TODO Load diagnostic values (Issue #614)

    for (int i = 1; i < data.size(); i++) {
      DataColumn column = data.get(i);
      Double value = sensorData.getSensorValue(column.getName());
      if (null != value) {
        column.setValue(String.valueOf(value));
      }
    }
  }


  /**
   * Load the calculation data for the measurement
   * @param conn A database connection
   * @throws InvalidDataException If a value cannot be added to the record
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the record is not in the database
   * @throws MessageException If the automatic QC messages cannot be parsed
   * @throws NoSuchColumnException If the automatic QC messages cannot be parsed
   */
  private void loadCalculationData(Connection conn) throws InvalidDataException, MissingParamException, DatabaseException, RecordNotFoundException, NoSuchColumnException, MessageException {
    calculationDB.getCalculationValues(conn, this);
  }

  /**
   * Retrieve the CalculationDB instance to be used with this record
   * @return The CalculationDB instance
   */
  protected abstract CalculationDB getCalculationDB();

  /**
   * Get the record data objects
   * @return The record data objects
   */
  public List<DataColumn> getData() {
    return data;
  }

  /**
   * Get the automatic QC flag
   * @return the autoFlag
   */
  public Flag getAutoFlag() {
    return autoFlag;
  }

  /**
   * Set the automatic QC flag
   * @param autoFlag the autoFlag to set
   */
  public void setAutoFlag(Flag autoFlag) {
    this.autoFlag = autoFlag;
  }

  /**
   * Get the automatic QC message
   * @return the autoMessage
   */
  public List<Message> getAutoQCMessages() {
    return messages;
  }

  /**
   * Get message strings for all messages related to this record
   * @return The message strings
   */
  public String getAutoQCMessagesString() {
    StringBuilder result = new StringBuilder();

    for (int i = 0; i < messages.size(); i++) {
      result.append(messages.get(i).getShortMessage());
      if (i < messages.size() - 1) {
        result.append(',');
      }
    }

    return result.toString();
  }

  /**
   * Get the user QC flag
   * @return the userFlag
   */
  public Flag getUserFlag() {
    return userFlag;
  }

  /**
   * Set the user QC flag
   * @param userFlag the userFlag to set
   */
  public void setUserFlag(Flag userFlag) {
    this.userFlag = userFlag;
  }

  /**
   * Get the user QC message
   * @return the userMessage
   */
  public String getUserMessage() {
    return userMessage;
  }

  /**
   * Set the user QC message
   * @param userMessage the userMessage to set
   */
  public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
  }

  /**
   * Clear the automatic QC data
   */
  public void clearAutoQCData() {
    messages = new ArrayList<Message>();
    setAutoFlag(Flag.NOT_SET);
  }

  @Override
  public void addMessage(Message message) throws NoSuchColumnException {
    super.addMessage(message);
    if (message.getFlag().moreSignificantThan(autoFlag)) {
      autoFlag = message.getFlag();

      if (message.getFlag().equals(Flag.FATAL)) {
        userFlag = autoFlag;
        userMessage = message.getShortMessage();
      }
    }
  }

  @Override
  public Double getNumericValue(String columnName) throws NoSuchColumnException, InvalidDataTypeException {
    String retrievalColumn = columnName;
    if (columnAliases.containsKey(columnName)) {
      retrievalColumn = columnAliases.get(columnName);
    }
    return super.getNumericValue(retrievalColumn);
  }

  /**
   * Get the list of columns that contain calculation values
   * @return The column names
   */
  public abstract List<String> getCalculationColumns();

  /**
   * Build the set of human readable/database column aliases
   */
  protected abstract void buildColumnAliases();

  /**
   * Set all calculated values to NULL
   */
  public abstract Map<String, Double> generateNullCalculationRecords();
}
