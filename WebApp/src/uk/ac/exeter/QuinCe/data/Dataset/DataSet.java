package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonArray;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.Message;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Object to represent a data set
 */
public class DataSet implements Comparable<DataSet> {

  public static final long TIMEPOS_FIELDSET_ID = 0L;

  public static final String TIMEPOS_FIELDSET_NAME = "Time/Position";

  /**
   * Special key for the dataset properties that holds the overarching
   * properties for the instrument.
   *
   * @see #getProperty(String, String)
   */
  public static final String INSTRUMENT_PROPERTIES_KEY = "_INSTRUMENT";

  /**
   * Special key for the dataset properties that holds the general properties
   * for the dataset.
   *
   * @see #getProperty(String, String)
   */
  public static final String DATASET_PROPERTIES_KEY = "_DATASET";

  private static final String PROCESSING_VERSION_PROPERTY = "ProcessingVersion";

  public static final int STATUS_REPROCESS = -4;

  public static final String STATUS_REPROCESS_NAME = "Marked for reprocessing";

  /**
   * The numeric value for the delete status.
   */
  public static final int STATUS_DELETING = -3;

  /**
   * The string for the delete status
   */
  public static final String STATUS_DELETING_NAME = "Deleting";

  /**
   * The numeric value for the delete status.
   */
  public static final int STATUS_DELETE = -2;

  /**
   * The string for the delete status
   */
  public static final String STATUS_DELETE_NAME = "Marked for delete";

  /**
   * The numeric value for the error status. The data set will be given this
   * status whenever a processing job fails.
   */
  public static final int STATUS_ERROR = -1;

  /**
   * The string for the error status
   */
  public static final String STATUS_ERROR_NAME = "ERROR";

  /**
   * The numeric value for the data extraction status
   */
  public static final int STATUS_WAITING = 0;

  /**
   * The string for the data extraction status
   */
  public static final String STATUS_WAITING_NAME = "Waiting";

  /**
   * The numeric value for the data extraction status
   */
  public static final int STATUS_DATA_EXTRACTION = 10;

  /**
   * The string for the data extraction status
   */
  public static final String STATUS_DATA_EXTRACTION_NAME = "Data Extraction";

  /**
   * The numeric value for the automatic QC status
   */
  public static final int STATUS_SENSOR_QC = 20;

  /**
   * The string for the automatic QC status
   */
  public static final String STATUS_SENSOR_QC_NAME = "Sensor QC";

  /**
   * The numeric value for the data reduction status
   */
  public static final int STATUS_DATA_REDUCTION = 30;

  /**
   * The string for the data extraction status
   */
  public static final String STATUS_DATA_REDUCTION_NAME = "Data Reduction";

  /**
   * The numeric value for the data reduction QC status
   */
  public static final int STATUS_DATA_REDUCTION_QC = 40;

  /**
   * The string for the export complete status
   */
  public static final String STATUS_DATA_REDUCTION_QC_NAME = "Data Reduction QC";

  /**
   * The numeric value for the user QC status
   */
  public static final int STATUS_USER_QC = 50;

  /**
   * The string for the user QC status
   */
  public static final String STATUS_USER_QC_NAME = "Ready for Manual QC";

  /**
   * The numeric value for the ready for submission status
   */
  public static final int STATUS_READY_FOR_SUBMISSION = 100;

  /**
   * The string for the ready for submission status
   */
  public static final String STATUS_READY_FOR_SUBMISSION_NAME = "Ready for Submission";

  /**
   * The numeric value for the waiting for approval status
   */
  public static final int STATUS_WAITING_FOR_APPROVAL = 110;

  /**
   * The string for the waiting for approval status
   */
  public static final String STATUS_WAITING_FOR_APPROVAL_NAME = "Waiting for Approval";

  /**
   * The numeric value for the ready for export status
   */
  public static final int STATUS_READY_FOR_EXPORT = 120;

  /**
   * The string for the ready for export status
   */
  public static final String STATUS_READY_FOR_EXPORT_NAME = "Waiting for Automatic Export";

  /**
   * The numeric value for the exporting status
   */
  public static final int STATUS_EXPORTING = 130;

  /**
   * The string for the exporting status
   */
  public static final String STATUS_EXPORTING_NAME = "Automatic Export In Progress";

  /**
   * The numeric value for the export complete status
   */
  public static final int STATUS_EXPORT_COMPLETE = 140;

  /**
   * The string for the export complete status
   */
  public static final String STATUS_EXPORT_COMPLETE_NAME = "Automatic Export Complete";

  private static Map<Integer, String> validStatuses;

  /**
   * The database ID
   */
  private long id = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The database ID of the instrument to which this data set belongs
   */
  private Instrument instrument;

  /**
   * The data set name
   */
  private String name;

  /**
   * The start date of the data set
   */
  private LocalDateTime start;

  /**
   * The end date of the data set
   */
  private LocalDateTime end;

  /**
   * Properties for each of the measured variables
   */
  private Map<String, Properties> properties;

  /**
   * The time that this Dataset was created in the database
   */
  private LocalDateTime createdDate;

  /**
   * The time when the data file was last touched
   */
  private LocalDateTime lastTouched;

  /**
   * The data set's status
   */
  private int status = STATUS_WAITING;

  /**
   * The date that the status was set
   */
  private LocalDateTime statusDate = null;

  /**
   * Indicates whether or not this is a NRT dataset
   */
  private boolean nrt = false;

  /**
   * Errors from jobs handling this data set
   */
  private ArrayList<Message> errorMessages = new ArrayList<Message>();

  /**
   * Messages generated during processing and data reduction.
   */
  private DatasetProcessingMessages processingMessages;

  /**
   * Messages entered by the user.
   */
  private DatasetUserMessages userMessages;

  /**
   * The available field sets for this dataset
   */
  private LinkedHashMap<String, Long> fieldSetsByName = null;

  /**
   * The minimum longitude
   */
  private double minLon = 0.0;

  /**
   * The maximum longitude
   */
  private double maxLon = 0.0;

  /**
   * The minimum latitude
   */
  private double minLat = 0.0;

  /**
   * The maximum latitiude
   */
  private double maxLat = 0.0;

  /**
   * Indicates whether or not a dataset has ever been exported.
   */
  private boolean exported;

  /**
   * The sensor offsets defined for the dataset.
   */
  private SensorOffsets sensorOffsets;

  static {
    validStatuses = new HashMap<Integer, String>();
    validStatuses.put(STATUS_REPROCESS, STATUS_REPROCESS_NAME);
    validStatuses.put(STATUS_DELETE, STATUS_DELETE_NAME);
    validStatuses.put(STATUS_ERROR, STATUS_ERROR_NAME);
    validStatuses.put(STATUS_WAITING, STATUS_WAITING_NAME);
    validStatuses.put(STATUS_DATA_EXTRACTION, STATUS_DATA_EXTRACTION_NAME);
    validStatuses.put(STATUS_SENSOR_QC, STATUS_SENSOR_QC_NAME);
    validStatuses.put(STATUS_DATA_REDUCTION, STATUS_DATA_REDUCTION_NAME);
    validStatuses.put(STATUS_DATA_REDUCTION_QC, STATUS_DATA_REDUCTION_QC_NAME);
    validStatuses.put(STATUS_USER_QC, STATUS_USER_QC_NAME);
    validStatuses.put(STATUS_READY_FOR_SUBMISSION,
      STATUS_READY_FOR_SUBMISSION_NAME);
    validStatuses.put(STATUS_WAITING_FOR_APPROVAL,
      STATUS_WAITING_FOR_APPROVAL_NAME);
    validStatuses.put(STATUS_READY_FOR_EXPORT, STATUS_READY_FOR_EXPORT_NAME);
    validStatuses.put(STATUS_EXPORTING, STATUS_EXPORTING_NAME);
    validStatuses.put(STATUS_EXPORT_COMPLETE, STATUS_EXPORT_COMPLETE_NAME);
    validStatuses.put(STATUS_DELETING, STATUS_DELETING_NAME);
  }

  /**
   * Constructor for all fields
   *
   * @param id
   *          Data set's database ID
   * @param instrumentId
   *          Database ID of the instrument to which the data set belongs
   * @param name
   *          Dataset name
   * @param start
   *          Start date
   * @param end
   *          End date
   * @param status
   *          The current status
   * @param statusDate
   *          The date that the status was set
   * @param nrt
   *          Indicates whether or not this is a NRT dataset
   * @param properties
   *          Additional properties
   * @param createdDate
   *          Date that the dataset was created
   * @param lastTouched
   *          Date that the dataset was last accessed
   * @param errorMessages
   *          List of messages concerning the dataset (errors etc)
   * @param minLon
   *          The minimum longitude of the dataset's geographical bounds
   * @param minLat
   *          The minimum latitude of the dataset's geographical bounds
   * @param maxLon
   *          The maximum longitude of the dataset's geographical bounds
   * @param maxLat
   *          The maximum latitude of the dataset's geographical bounds
   *
   */
  protected DataSet(long id, Instrument instrument, String name,
    LocalDateTime start, LocalDateTime end, int status,
    LocalDateTime statusDate, boolean nrt, Map<String, Properties> properties,
    SensorOffsets sensorOffsets, LocalDateTime createdDate,
    LocalDateTime lastTouched, List<Message> errorMessages,
    DatasetProcessingMessages processingMessages,
    DatasetUserMessages userMessages, double minLon, double minLat,
    double maxLon, double maxLat, boolean exported) {

    this.id = id;
    this.instrument = instrument;
    this.name = name;
    this.start = start;
    this.end = end;
    this.status = status;
    this.statusDate = statusDate;
    this.nrt = nrt;
    this.properties = properties;
    this.sensorOffsets = sensorOffsets;
    this.createdDate = createdDate;
    this.lastTouched = lastTouched;
    this.errorMessages = new ArrayList<Message>(errorMessages);
    this.processingMessages = processingMessages;
    this.userMessages = userMessages;
    this.minLon = minLon;
    this.minLat = minLat;
    this.maxLon = maxLon;
    this.maxLat = maxLat;
    this.exported = exported;
  }

  /**
   * Constructor for a new, empty data set
   *
   * @param instrument
   *          The instrument to which the data set belongs
   */
  public DataSet(Instrument instrument) {
    this.instrument = instrument;
    this.statusDate = DateTimeUtils.longToDate(System.currentTimeMillis());
    loadProperties(instrument);
    this.sensorOffsets = new SensorOffsets(instrument.getSensorGroups());
    this.processingMessages = new DatasetProcessingMessages();
    this.userMessages = new DatasetUserMessages();
    initBounds();
    this.exported = false;
  }

  /**
   * Basic constructor for new dataset with only start and end dates.
   *
   * @param instrument
   *          The instrument to which the data set belongs
   * @param name
   *          Dataset name
   * @param start
   *          Start date
   * @param end
   *          End date
   * @param nrt
   *          Indicates whether or not this is a NRT dataset
   */
  public DataSet(Instrument instrument, String name, LocalDateTime start,
    LocalDateTime end, boolean nrt) {
    this.instrument = instrument;
    this.name = name;
    this.start = start;
    this.end = end;
    this.nrt = nrt;
    this.statusDate = DateTimeUtils.longToDate(System.currentTimeMillis());
    loadProperties(instrument);
    this.sensorOffsets = new SensorOffsets(instrument.getSensorGroups());
    this.processingMessages = new DatasetProcessingMessages();
    this.userMessages = new DatasetUserMessages();
    initBounds();
    this.exported = false;
  }

  private void loadProperties(Instrument instrument) {

    // Copy in the properties from the instrument definition
    this.properties = new HashMap<String, Properties>();
    properties.put(INSTRUMENT_PROPERTIES_KEY, instrument.getProperties());
    for (Map.Entry<Variable, Properties> entry : instrument
      .getAllVariableProperties().entrySet()) {

      properties.put(entry.getKey().getName(), entry.getValue());
    }
  }

  /**
   * Get the data set's status
   *
   * @return The status
   */
  public int getStatus() {
    return status;
  }

  /**
   * Get the human-readable status of the data set
   *
   * @return The status
   */
  public String getStatusName() {
    return getStatusName(status);
  }

  /**
   * Get the human-readable name of a given data set status
   *
   * @param statusValue
   *          The status value
   * @return The status name
   */
  public static String getStatusName(int statusValue) {
    String result = validStatuses.get(statusValue);
    if (null == result) {
      result = "UNKNOWN";
    }
    return result;
  }

  /**
   * Get the data set's database ID
   *
   * @return The database ID
   */
  public long getId() {
    return id;
  }

  /**
   * Set the data set's database ID
   *
   * @param id
   *          The database ID
   */
  protected void setId(long id) {
    this.id = id;
  }

  /**
   * Get the {@link Instrument} to which this data set belongs.
   *
   * @return The instrument.
   */
  public Instrument getInstrument() {
    return instrument;
  }

  /**
   * Get the database ID of the instrument to which this data set belongs
   *
   * @return The instrument's database ID
   */
  public long getInstrumentId() {
    return instrument.getId();
  }

  /**
   * Get the name of the data set
   *
   * @return The data set name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the data set
   *
   * @param name
   *          The data set name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the start date of the data set
   *
   * @return The start date
   */
  public LocalDateTime getStart() {
    return start;
  }

  /**
   * Set the start date of the data set
   *
   * @param start
   *          The start date
   */
  public void setStart(LocalDateTime start) {
    this.start = start;
  }

  /**
   * Get the end date of the data set
   *
   * @return The end date
   */
  public LocalDateTime getEnd() {
    return end;
  }

  /**
   * Set the end date of the data set
   *
   * @param end
   *          The end date
   */
  public void setEnd(LocalDateTime end) {
    this.end = end;
  }

  /**
   * Get the date that the data set was last accessed
   *
   * @return The last access date
   */
  public LocalDateTime getLastTouched() {
    return lastTouched;
  }

  /**
   * Set the last access date of the data set to the current time
   */
  public void touch() {
    this.lastTouched = DateTimeUtils.longToDate(System.currentTimeMillis());
  }

  /**
   * Set the data set's status
   *
   * @param status
   *          The status
   * @throws InvalidDataSetStatusException
   *           If the status is invalid
   */
  public void setStatus(int status) throws InvalidDataSetStatusException {
    if (!validateStatus(status)) {
      throw new InvalidDataSetStatusException(status);
    }

    this.status = status;
    statusDate = DateTimeUtils.longToDate(System.currentTimeMillis());
  }

  /**
   * Get the date that the dataset's status was set
   *
   * @return The status date
   */
  public LocalDateTime getStatusDate() {
    return statusDate;
  }

  /**
   * Get a property variables's property from the data set
   *
   * @param variable
   *          The variables whose properties are to be searched
   * @param key
   *          The key
   * @return The value
   * @see Properties#getProperty(String)
   * @see #INSTRUMENT_PROPERTIES_KEY
   */
  public String getProperty(String variable, String key) {
    String result = null;

    if (null != properties) {
      Properties varProps = properties.get(variable);
      if (null != varProps) {
        result = varProps.getProperty(key, null);
      }
    }

    return result;
  }

  public void setProperty(Variable variable, String key, String value) {
    setProperty(variable.getName(), key, value);
  }

  private void setProperty(String variableName, String key, String value) {
    if (null == properties) {
      properties = new HashMap<String, Properties>();
    }

    Properties varProps;

    if (null == properties.get(variableName)) {
      properties.put(variableName, new Properties());
    }

    varProps = properties.get(variableName);
    varProps.setProperty(key, value);
  }

  /**
   * Determine whether or not a given status value is valid
   *
   * @param status
   *          The status to be checked
   * @return {@code true} if the status is valid; {@code false} if it is not
   */
  public static boolean validateStatus(int status) {
    return validStatuses.containsKey(status);
  }

  /**
   * Determine whether or not this dataset can be exported.
   *
   * <p>
   * The data set can be exported if it meets the following criteria:
   * </p>
   * <ul>
   * <li>The dataset ready for user QC (not in any other state)</li>
   * <li>No rows need flagging</li>
   * </ul>
   *
   * @return {@code true} if the dataset can be exported; {@code false} if it
   *         cannot
   */
  public boolean getCanBeExported() {
    // TODO Reinstate check of whether QC is complete
    return (getStatus() >= STATUS_USER_QC); // || isNrt());
  }

  public void addMessage(String message, String details) {
    errorMessages.add(new Message(message, details));
  }

  public List<Message> getMessages() {
    return errorMessages;
  }

  public String getErrorMessagesAsJSONString() {
    JsonArray json = new JsonArray();
    for (Message message : getMessages()) {
      json.add(message.getAsJSON());
    }
    return json.toString();
  }

  public int getMessageCount() {
    return errorMessages.size();
  }

  /**
   * Remove all messages from this dataset.
   */
  public void clearMessages() {
    errorMessages.clear();
  }

  /**
   * Get a list of the raw data files used to construct this DataSet
   *
   * @param conn
   *          A database connection
   * @return The IDs of the files
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public List<Long> getSourceFiles(Connection conn)
    throws MissingParamException, DatabaseException {
    return DataFileDB.getFilesWithinDates(conn, instrument.getId(), start, end,
      true);
  }

  /**
   * Determine whether or not this is a NRT dataset
   *
   * @return {@code true} if this is an NRT dataset; {@code false} if it is not
   */
  public boolean isNrt() {
    return nrt;
  }

  /**
   * Get the available field sets for this dataset keyed by name. Builds the
   * list once, then caches it
   *
   * @param includeTimePos
   *          Indicates whether or not the root field set for Time and Position
   *          is included
   * @return The field sets
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws VariableNotFoundException
   *           If an invalid variable is configured for the instrument
   */
  @SuppressWarnings("unchecked")
  public LinkedHashMap<String, Long> getFieldSets(boolean includeTimePos)
    throws MissingParamException, VariableNotFoundException, DatabaseException {

    if (null == fieldSetsByName) {
      fieldSetsByName = new LinkedHashMap<String, Long>();
      fieldSetsByName.put(TIMEPOS_FIELDSET_NAME, TIMEPOS_FIELDSET_ID);

      fieldSetsByName.put(DataSetDataDB.SENSORS_FIELDSET_NAME,
        DataSetDataDB.SENSORS_FIELDSET);

      fieldSetsByName.put(DataSetDataDB.DIAGNOSTICS_FIELDSET_NAME,
        DataSetDataDB.DIAGNOSTICS_FIELDSET);

      for (Variable variable : instrument.getVariables()) {
        fieldSetsByName.put(variable.getName(), variable.getId());
      }
    }

    LinkedHashMap<String, Long> result = fieldSetsByName;

    if (!includeTimePos) {
      result = (LinkedHashMap<String, Long>) fieldSetsByName.clone();
      result.remove(DataSet.TIMEPOS_FIELDSET_NAME);
    }

    return result;
  }

  /**
   * Set the dataset's geographical bounds
   *
   * @param minLon
   *          The minimum longitude
   * @param maxLon
   *          The maximum longitude
   * @param minLat
   *          The minimum latitude
   * @param maxLat
   *          The maximum latitude
   */
  public void setBounds(double minLon, double minLat, double maxLon,
    double maxLat) {

    if (!instrument.fixedPosition()) {
      this.minLon = minLon;
      this.maxLon = maxLon;
      this.minLat = minLat;
      this.maxLat = maxLat;
    }
  }

  private void initBounds() {
    if (instrument.fixedPosition()) {
      this.minLon = Double.parseDouble(instrument.getProperty("longitude"));
      this.maxLon = Double.parseDouble(instrument.getProperty("longitude"));
      this.minLat = Double.parseDouble(instrument.getProperty("latitude"));
      this.maxLat = Double.parseDouble(instrument.getProperty("latitude"));
    } else {
      this.minLon = 0D;
      this.maxLon = 0D;
      this.minLat = 0D;
      this.maxLat = 0D;
    }
  }

  /**
   * Get the geographical bounds of the dataset This is a list of six values:
   *
   * <ol>
   * <li>West</li>
   * <li>South</li>
   * <li>East</li>
   * <li>North</li>
   * <li>Middle longitude</li>
   * <li>Middle latitude</li>
   * </ol>
   *
   * @return The dataset bounds
   */
  public GeoBounds getBounds() {
    return new GeoBounds(minLon, maxLon, minLat, maxLat);
  }

  public double getMinLon() {
    return minLon;
  }

  public double getMaxLon() {
    return maxLon;
  }

  public double getMinLat() {
    return minLat;
  }

  public double getMaxLat() {
    return maxLat;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  @Override
  public String toString() {
    return id + ";" + name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof DataSet))
      return false;
    DataSet other = (DataSet) obj;
    if (id != other.id)
      return false;
    return true;
  }

  public Map<String, Properties> getAllProperties() {
    return properties;
  }

  /**
   * Convenience method to determine whether or not this dataset has a fixed
   * position (i.e. does not have lat/lon data values).
   *
   * @return {@code true} if the dataset has a fixed position; {@code false}
   *         otherwise.
   */
  public boolean fixedPosition() {
    return null != getProperty(INSTRUMENT_PROPERTIES_KEY, "longitude");
  }

  public SensorOffsets getSensorOffsets() {
    return sensorOffsets;
  }

  @Override
  public int compareTo(DataSet o) {
    return start.compareTo(o.start);
  }

  public void addProcessingMessage(String module, String message) {
    processingMessages.addMessage(module, message);
  }

  public void addProcessingMessage(String module, DataFile file, int line,
    Throwable e) {
    processingMessages.addMessage(module, file, line, e);
  }

  public void addProcessingMessage(String module, DataFile file, int line,
    String message) {
    processingMessages.addMessage(module, file, line, message);
  }

  public DatasetProcessingMessages getProcessingMessages() {
    return processingMessages;
  }

  public DatasetUserMessages getUserMessages() {
    return userMessages;
  }

  public void setUserMessages(String messages) {
    userMessages = DatasetUserMessages.fromString(messages);
  }

  public boolean hasProcessingMessages() {
    return processingMessages.getMessageCount() > 0;
  }

  public void setProcessingVersion() {
    setProperty(DATASET_PROPERTIES_KEY, PROCESSING_VERSION_PROPERTY,
      ResourceManager.getInstance().getConfig().getProperty("version"));
  }

  public String getProcessingVersion() {

    String result = getProperty(DATASET_PROPERTIES_KEY,
      PROCESSING_VERSION_PROPERTY);

    // If the version wasn't set for some reason, assume it was done with the
    // current version
    if (null == result) {
      result = ResourceManager.getInstance().getConfig().getProperty("version");
    }

    return result;
  }

  /**
   * Determine whether or not this dataset has ever been exported.
   *
   * @return {@code true} if the dataset has been exported; {@code false}
   *         otherwise.
   */
  public boolean hasBeenExported() {
    return exported;
  }

  /**
   * Mark the dataset as having been exported
   */
  public void markExported() {
    exported = true;
  }
}
