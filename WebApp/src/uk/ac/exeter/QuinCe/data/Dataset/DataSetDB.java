package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.SensorCalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.Message;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for manipulating data sets in the database
 */
public class DataSetDB {

  /**
   * Statement to add a new data set into the database
   *
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String ADD_DATASET_STATEMENT = "INSERT INTO dataset "
    + "(instrument_id, name, start, end, status, status_date, "
    + "nrt, properties, last_touched, error_messages, processing_messages, user_messages, "
    + "min_longitude, max_longitude, min_latitude, max_latitude, exported) "
    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /**
   * Statement to update a data set in the database
   *
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String UPDATE_DATASET_STATEMENT = "Update dataset set "
    + "instrument_id = ?, name = ?, start = ?, end = ?, status = ?, "
    + "status_date = ?, nrt = ?, properties = ?, last_touched = ?, "
    + "error_messages = ?, processing_messages = ?, user_messages = ?, "
    + "min_longitude = ?, max_longitude = ?, "
    + "min_latitude = ?, max_latitude = ?, exported = ? WHERE id = ?";

  /**
   * Statement to delete a dataset record
   */
  private static final String DELETE_DATASET_QUERY = "DELETE FROM dataset "
    + "WHERE id = ?";

  /**
   * Base for all dataset queries to ensure that all queries get the same fields
   * in the same order.
   */
  private static final String DATASET_QUERY_BASE = "SELECT "
    + "d.id, d.instrument_id, d.name, d.start, d.end, d.status, "
    + "d.status_date, d.nrt, d.properties, d.created, d.last_touched, "
    + "COALESCE(d.error_messages, '[]'), processing_messages, user_messages, "
    + "d.min_longitude, d.max_longitude, d.min_latitude, d.max_latitude, d.exported "
    + "FROM dataset d WHERE ";

  private static final String GET_DATASETS_BETWEEN_DATES_QUERY = DATASET_QUERY_BASE
    + "d.instrument_id = ? AND d.start <= ? AND d.end >= ?";

  private static final String NRT_COUNT_QUERY = "SELECT COUNT(*) FROM dataset "
    + "WHERE nrt = 1 AND instrument_id = ?";

  private static final String NRT_STATUS_QUERY = "SELECT "
    + "ds.instrument_id, ds.created, ds.end, ds.status, ds.status_date "
    + "FROM dataset ds INNER JOIN instrument i ON ds.instrument_id = i.id "
    + "WHERE ds.nrt = 1 ORDER BY i.platform_code ASC";

  /**
   * Statement to update the user messages for a dataset
   */
  private static final String UPDATE_USER_MESSAGES_STATEMENT = "UPDATE dataset SET "
    + "user_messages = ? WHERE id = ?";

  private static final String GET_DATASET_COUNTS_QUERY = "SELECT i.id, COUNT(ds.id) "
    + "FROM instrument i LEFT JOIN dataset ds ON ds.instrument_id = i.id "
    + "WHERE i.id IN " + DatabaseUtils.IN_PARAMS_TOKEN + " GROUP BY i.id";

  private static final String DATASET_EXPORTED_STATEMENT = "UPDATE dataset "
    + "SET exported = 1 WHERE id = ?";

  private static final String SENSOR_OFFSETS_PROPERTY = "__SENSOR_OFFSETS";

  /**
   * Make an SQL query for retrieving complete datasets using a specified WHERE
   * clause
   *
   * @param whereFields
   *          The fields to use in the WHERE clause
   * @return The query SQL
   * @see #DATASET_QUERY_BASE
   */
  private static String makeGetDatasetsQuery(String... whereFields) {
    StringBuilder sql = new StringBuilder(DATASET_QUERY_BASE);

    sql.append(Stream.of(whereFields).map(field -> "d." + field + " = ? ")
      .collect(Collectors.joining("AND ")));

    sql.append("GROUP BY d.id ORDER BY d.start ASC");

    return sql.toString();
  }

  /**
   * Get the list of data sets defined for a given instrument
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument's database ID
   * @param includeNrt
   *          Indicates whether or not NRT datasets should be included in the
   *          result
   * @return The list of data sets
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static LinkedHashMap<Long, DataSet> getDataSets(DataSource dataSource,
    long instrumentId, boolean includeNrt)
    throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    LinkedHashMap<Long, DataSet> result = null;
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      result = getDataSets(conn, instrumentId, includeNrt);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the list of data sets defined for a given instrument
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @param includeNrt
   *          Indicates whether or not NRT datasets should be included in the
   *          result
   * @return The list of data sets
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static LinkedHashMap<Long, DataSet> getDataSets(Connection conn,
    long instrumentId, boolean includeNrt)
    throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    LinkedHashMap<Long, DataSet> result = new LinkedHashMap<Long, DataSet>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(makeGetDatasetsQuery("instrument_id"));
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {
        DataSet dataSet = dataSetFromRecord(conn, records);
        if (!dataSet.isNrt() || includeNrt) {
          result.put(dataSet.getId(), dataSet);
        }
      }

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Create a DataSet object from a search result
   *
   * @param record
   *          The search result
   * @return The Data Set object
   * @throws SQLException
   *           If the data cannot be extracted from the result
   * @throws SensorGroupsException
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  private static DataSet dataSetFromRecord(Connection conn, ResultSet record)
    throws SQLException, MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {

    long id = record.getLong(1);
    Instrument instrument = InstrumentDB.getInstrument(conn, record.getLong(2));
    String name = record.getString(3);
    LocalDateTime start = DateTimeUtils.longToDate(record.getLong(4));
    LocalDateTime end = DateTimeUtils.longToDate(record.getLong(5));
    int status = record.getInt(6);
    LocalDateTime statusDate = DateTimeUtils.longToDate(record.getLong(7));
    boolean nrt = record.getBoolean(8);

    // The properties field combines several things. Extract them.
    Gson gson = new GsonBuilder().registerTypeAdapter(SensorOffsets.class,
      new SensorOffsetsDeserializer(instrument)).create();

    String propertiesString = record.getString(9);
    if (null == propertiesString) {
      propertiesString = "{}";
    }

    JsonObject parsedJson = JsonParser.parseString(propertiesString)
      .getAsJsonObject();

    SensorOffsets sensorOffsets;
    if (parsedJson.has(SENSOR_OFFSETS_PROPERTY)) {
      sensorOffsets = gson.fromJson(parsedJson.get(SENSOR_OFFSETS_PROPERTY),
        SensorOffsets.class);
      parsedJson.remove(SENSOR_OFFSETS_PROPERTY);
    } else {
      // Create a new SensorOffsets object
      sensorOffsets = new SensorOffsets(instrument.getSensorGroups());
    }

    Type propertiesType = new TypeToken<Map<String, Properties>>() {
    }.getType();
    Map<String, Properties> properties = gson.fromJson(parsedJson,
      propertiesType);

    LocalDateTime createdDate = DateTimeUtils
      .longToDate(record.getTimestamp(10).getTime());

    LocalDateTime lastTouched = DateTimeUtils.longToDate(record.getLong(11));
    String errorMessagesJson = record.getString(12);

    JsonArray array = (JsonArray) JsonParser.parseString(errorMessagesJson);
    ArrayList<Message> errorMessage = new ArrayList<Message>();
    for (Object o : array) {
      if (o instanceof JsonObject) {
        JsonObject jo = (JsonObject) o;
        Message m = new Message(jo.get("message").getAsString(),
          jo.get("details").getAsString());
        errorMessage.add(m);
      }
    }

    DatasetProcessingMessages processingMessages = DatasetProcessingMessages
      .fromJson(record.getString(13));
    DatasetUserMessages userMessages = DatasetUserMessages
      .fromString(record.getString(14));

    double minLon = record.getDouble(15);
    double maxLon = record.getDouble(16);
    double minLat = record.getDouble(17);
    double maxLat = record.getDouble(18);
    boolean exported = record.getBoolean(19);

    return new DataSet(id, instrument, name, start, end, status, statusDate,
      nrt, properties, sensorOffsets, createdDate, lastTouched, errorMessage,
      processingMessages, userMessages, minLon, minLat, maxLon, maxLat,
      exported);
  }

  /**
   * Store a new data set in the database.
   *
   * The created data set's ID is stored in the provided {@link DataSet} object
   *
   * @param dataSource
   *          A data source
   * @param dataSet
   *          The data set to be stored
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static void addDataSet(DataSource dataSource, DataSet dataSet)
    throws DatabaseException, MissingParamException {
    // Make sure this inserts a new record
    dataSet.setId(DatabaseUtils.NO_DATABASE_RECORD);
    saveDataSet(dataSource, dataSet);
  }

  /**
   * Store a new data set in the database.
   *
   * The created data set's ID is stored in the provided {@link DataSet} object
   *
   * @param conn
   *          A database connection
   * @param dataSet
   *          The data set to be stored
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static void addDataSet(Connection conn, DataSet dataSet)
    throws DatabaseException, MissingParamException {
    // Make sure this inserts a new record
    dataSet.setId(DatabaseUtils.NO_DATABASE_RECORD);
    saveDataSet(conn, dataSet);
  }

  private static void saveDataSet(DataSource dataSource, DataSet dataSet)
    throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(dataSource, "dataSource");
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);
      saveDataSet(conn, dataSet);
      conn.commit();
      conn.setAutoCommit(true);
    } catch (SQLException e) {
      throw new DatabaseException("Error opening database connection", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  private static void saveDataSet(Connection conn, DataSet dataSet)
    throws DatabaseException, MissingParamException {

    // TODO Validate the data set
    // TODO Make sure it's not a duplicate of an existing data set

    MissingParam.checkMissing(dataSet, "dataSet");

    PreparedStatement stmt = null;
    ResultSet addedKeys = null;

    try {
      String sql = UPDATE_DATASET_STATEMENT;
      if (DatabaseUtils.NO_DATABASE_RECORD == dataSet.getId()) {
        sql = ADD_DATASET_STATEMENT;
      }

      stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

      stmt.setLong(1, dataSet.getInstrumentId());
      stmt.setString(2, dataSet.getName());
      stmt.setLong(3, DateTimeUtils.dateToLong(dataSet.getStart()));
      stmt.setLong(4, DateTimeUtils.dateToLong(dataSet.getEnd()));
      stmt.setInt(5, dataSet.getStatus());
      stmt.setLong(6, DateTimeUtils.dateToLong(dataSet.getStatusDate()));
      stmt.setBoolean(7, dataSet.isNrt());
      stmt.setString(8, getDatasetPropertiesJson(dataSet));
      stmt.setLong(9, DateTimeUtils.dateToLong(LocalDateTime.now()));

      if (dataSet.getMessageCount() > 0) {
        String jsonString = dataSet.getErrorMessagesAsJSONString();
        stmt.setString(10, jsonString);
      } else {
        stmt.setNull(10, Types.VARCHAR);
      }

      stmt.setString(11, dataSet.getProcessingMessages().toJson());
      stmt.setString(12, dataSet.getUserMessages().getStorageString());

      stmt.setDouble(13, dataSet.getMinLon());
      stmt.setDouble(14, dataSet.getMaxLon());
      stmt.setDouble(15, dataSet.getMinLat());
      stmt.setDouble(16, dataSet.getMaxLat());
      stmt.setBoolean(17, dataSet.hasBeenExported());

      if (DatabaseUtils.NO_DATABASE_RECORD != dataSet.getId()) {
        stmt.setLong(18, dataSet.getId());
      }

      stmt.execute();

      // Add the database ID to the database object
      if (DatabaseUtils.NO_DATABASE_RECORD == dataSet.getId()) {
        addedKeys = stmt.getGeneratedKeys();
        addedKeys.next();
        dataSet.setId(addedKeys.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while adding data set", e);
    } finally {
      DatabaseUtils.closeResultSets(addedKeys);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  private static String getDatasetPropertiesJson(DataSet dataSet) {

    Gson gson = new GsonBuilder()
      .registerTypeAdapter(SensorOffsets.class, new SensorOffsetsSerializer())
      .create();

    JsonObject propertiesJson = gson.toJsonTree(dataSet.getAllProperties())
      .getAsJsonObject();

    JsonElement sensorOffsetsJson = gson.toJsonTree(dataSet.getSensorOffsets());

    propertiesJson.add(SENSOR_OFFSETS_PROPERTY, sensorOffsetsJson);

    return propertiesJson.toString();
  }

  /**
   * Get a data set using its database ID
   *
   * @param dataSource
   *          A data source
   * @param id
   *          The data set's id
   * @return The data set
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the data set does not exist
   */
  public static DataSet getDataSet(DataSource dataSource, long id)
    throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(id, "id");

    DataSet result = null;

    try (Connection conn = dataSource.getConnection()) {
      result = getDataSet(conn, id);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    }

    return result;
  }

  /**
   * Get a data set using its database ID
   *
   * @param conn
   *          A database connection
   * @param id
   *          The data set's id
   * @return The data set
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the data set does not exist
   */
  public static DataSet getDataSet(Connection conn, long id)
    throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(id, "id");

    DataSet result = null;

    try (PreparedStatement stmt = conn
      .prepareStatement(makeGetDatasetsQuery("id"))) {
      stmt.setLong(1, id);

      try (ResultSet record = stmt.executeQuery()) {
        if (!record.next()) {
          throw new RecordNotFoundException("Data set does not exist",
            "dataset", id);
        } else {
          result = dataSetFromRecord(conn, record);
        }
      }
    } catch (RecordNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    }

    return result;
  }

  public static void setNrtDatasetStatus(DataSource dataSource,
    Instrument instrument, int status)
    throws DatabaseException, MissingParamException,
    InvalidDataSetStatusException, RecordNotFoundException {

    try (Connection conn = dataSource.getConnection();) {

      DataSet nrtDataset = getNrtDataSet(conn, instrument.getId());
      if (null != nrtDataset) {
        setDatasetStatus(conn, nrtDataset.getId(), status);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while setting dataset status", e);
    }

  }

  /**
   * Set the status of a {@link DataSet}.
   *
   * @param dataSource
   *          A data source
   * @param dataSet
   *          The data set
   * @param status
   *          The new status
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws InvalidDataSetStatusException
   *           If the status is invalid
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static void setDatasetStatus(DataSource dataSource, DataSet dataSet,
    int status) throws MissingParamException, InvalidDataSetStatusException,
    DatabaseException {
    dataSet.setStatus(status);
    saveDataSet(dataSource, dataSet);
  }

  /**
   * Set the status of a {@link DataSet}.
   *
   * @param dataSource
   *          A data source
   * @param datasetId
   *          The data set's database ID
   * @param status
   *          The new status
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws InvalidDataSetStatusException
   *           If the status is invalid
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If the dataset cannot be found in the database
   */
  public static void setDatasetStatus(DataSource dataSource, long datasetId,
    int status) throws MissingParamException, InvalidDataSetStatusException,
    DatabaseException, RecordNotFoundException {
    MissingParam.checkZeroPositive(datasetId, "datasetId");
    setDatasetStatus(dataSource, getDataSet(dataSource, datasetId), status);
  }

  /**
   * Set the status of a {@link DataSet}.
   *
   * @param conn
   *          A database connection
   * @param datasetId
   *          The data set's ID
   * @param status
   *          The new status
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws InvalidDataSetStatusException
   *           If the status is invalid
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws InvalidDataSetStatusException
   *           If the specified status is invalid
   * @throws RecordNotFoundException
   *           If the dataset does not exist
   */
  public static void setDatasetStatus(Connection conn, long datasetId,
    int status) throws MissingParamException, InvalidDataSetStatusException,
    DatabaseException, RecordNotFoundException {
    DataSet dataSet = getDataSet(conn, datasetId);
    dataSet.setStatus(status);
    updateDataSet(conn, dataSet);
  }

  /**
   * Update a dataset in the database
   *
   * @param conn
   *          A database connection
   * @param dataSet
   *          The updated dataset
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If the dataset is not already in the database
   */
  public static void updateDataSet(DataSource dataSource, DataSet dataSet)
    throws MissingParamException, DatabaseException, RecordNotFoundException {

    try (Connection conn = dataSource.getConnection()) {
      saveDataSet(conn, dataSet);
    } catch (SQLException e) {
      throw new DatabaseException("Error saving dataset", e);
    }
  }

  /**
   * Update a dataset in the database
   *
   * @param conn
   *          A database connection
   * @param dataSet
   *          The updated dataset
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If the dataset is not already in the database
   */
  public static void updateDataSet(Connection conn, DataSet dataSet)
    throws MissingParamException, DatabaseException, RecordNotFoundException {
    saveDataSet(conn, dataSet);
  }

  /**
   * Retrieve the most recent data set for an instrument
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @param includeNrt
   *          Indicates whether or not NRT datasets should be included in the
   *          search
   * @return The most recent dataset, or {@code null} if there are no datasets
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static DataSet getLastDataSet(Connection conn, long instrumentId,
    boolean includeNrt) throws MissingParamException, DatabaseException {
    DataSet result = null;

    List<DataSet> datasets = new ArrayList<DataSet>(
      getDataSets(conn, instrumentId, includeNrt).values());

    if (datasets.size() > 0) {
      result = datasets.get(datasets.size() - 1);
    }

    return result;
  }

  public static DataSet getNrtDataSet(DataSource dataSource, long instrumentId)
    throws MissingParamException, DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      return getNrtDataSet(conn, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error getting NRT dataset", e);
    }
  }

  /**
   * Retrieve the NRT data set for an instrument
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @return The NRT dataset, or {@code null} if there there isn't one
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static DataSet getNrtDataSet(Connection conn, long instrumentId)
    throws MissingParamException, DatabaseException {
    DataSet result = null;

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    try (PreparedStatement stmt = conn
      .prepareStatement(makeGetDatasetsQuery("instrument_id", "nrt"))) {

      stmt.setLong(1, instrumentId);
      stmt.setBoolean(2, true);

      try (ResultSet records = stmt.executeQuery()) {
        while (null == result && records.next()) {
          DataSet dataset = dataSetFromRecord(conn, records);
          result = dataset;
          break;
        }
      }

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    }

    return result;
  }

  /**
   * Delete all NRT datasets defined for a given instrument. In theory there
   * should be only one, but this deletes all that it can find, just in case.
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument's database ID
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   * @throws InvalidDataSetStatusException
   */
  public static void deleteNrtDataSet(DataSource dataSource, long instrumentId)
    throws MissingParamException, DatabaseException,
    InvalidDataSetStatusException, RecordNotFoundException {

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      deleteNrtDataSet(conn, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting NRT dataset", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Delete the NRT datasets defined for a given instrument.
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   * @throws InvalidDataSetStatusException
   */
  public static void deleteNrtDataSet(Connection conn, long instrumentId)
    throws MissingParamException, DatabaseException,
    InvalidDataSetStatusException, RecordNotFoundException {

    DataSet nrtDataset = getNrtDataSet(conn, instrumentId);
    if (null != nrtDataset) {
      deleteDataSet(conn, nrtDataset);
    }
  }

  /**
   * Delete a dataset and all related records
   *
   * @param conn
   *          A database connection
   * @param dataSet
   *          The dataset to be deleted
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   * @throws InvalidDataSetStatusException
   */
  public static void deleteDataSet(Connection conn, DataSet dataSet)
    throws MissingParamException, DatabaseException,
    InvalidDataSetStatusException, RecordNotFoundException {

    boolean currentAutoCommitStatus = false;
    PreparedStatement datasetStatement = null;

    try {
      currentAutoCommitStatus = conn.getAutoCommit();
      setDatasetStatus(conn, dataSet.getId(), DataSet.STATUS_DELETING);
      if (!currentAutoCommitStatus) {
        conn.commit();
      }

      if (currentAutoCommitStatus) {
        conn.setAutoCommit(false);
      }

      DataSetDataDB.deleteMeasurements(conn, dataSet.getId());
      DataSetDataDB.deleteSensorValues(conn, dataSet.getId());

      datasetStatement = conn.prepareStatement(DELETE_DATASET_QUERY);
      datasetStatement.setLong(1, dataSet.getId());
      datasetStatement.execute();

      if (currentAutoCommitStatus) {
        // Return the connection to its non-transaction state
        conn.commit();
        conn.setAutoCommit(true);
      }
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      if (currentAutoCommitStatus) {
        try {
          conn.rollback();
          conn.setAutoCommit(true);
        } catch (SQLException e2) {
          ExceptionUtils.printStackTrace(e2);
        }
      }
    } finally {
      DatabaseUtils.closeStatements(datasetStatement);
    }
  }

  /**
   * Generate the metadata portion of the manifest
   *
   * @param dataSource
   *          A data source
   * @param dataset
   *          The dataset
   * @return The metadata
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the dataset doesn't exist
   * @throws InstrumentException
   *           If the instrument details cannot be retrieved
   * @throws SensorGroupsException
   */
  public static JsonObject getMetadataJson(DataSource dataSource,
    DataSet dataset) throws DatabaseException, MissingParamException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {

    JsonObject result = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getMetadataJson(conn, dataset);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving metadata", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Generate the metadata portion of the manifest
   *
   * @param conn
   *          A database connection
   * @param dataset
   *          The dataset
   * @return The metadata
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the dataset doesn't exist
   * @throws InstrumentException
   *           If the instrument details cannot be retrieved
   * @throws SensorGroupsException
   */
  public static JsonObject getMetadataJson(Connection conn, DataSet dataset)
    throws DatabaseException, MissingParamException, RecordNotFoundException,
    InstrumentException, SensorGroupsException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    Instrument instrument = InstrumentDB.getInstrument(conn,
      dataset.getInstrumentId());

    JsonObject result = new JsonObject();
    result.addProperty("name", dataset.getName());
    result.addProperty("startdate",
      DateTimeUtils.toIsoDate(dataset.getStart()));
    result.addProperty("enddate", DateTimeUtils.toIsoDate(dataset.getEnd()));
    result.addProperty("platformCode", instrument.getPlatformCode());
    result.addProperty("platformName", instrument.getPlatformName());
    result.addProperty("instrumentName", instrument.getName());
    result.addProperty("nrt", dataset.isNrt());
    result.addProperty("last_touched",
      DateTimeUtils.toIsoDate(dataset.getLastTouched()));
    result.addProperty("comments",
      dataset.getUserMessages().getDisplayString());

    JsonObject boundsObject = new JsonObject();
    boundsObject.addProperty("south", dataset.getMinLat());
    boundsObject.addProperty("west", dataset.getMinLon());
    boundsObject.addProperty("east", dataset.getMaxLon());
    boundsObject.addProperty("north", dataset.getMaxLat());
    result.add("bounds", boundsObject);

    // Calibrations
    JsonObject calibrationObject = new JsonObject();

    if (instrument.hasInternalCalibrations()) {

      // External standards
      JsonArray externalStandards = new JsonArray();

      CalibrationSet standards = ExternalStandardDB.getInstance()
        .getStandardsSet(conn, instrument, dataset.getStart());

      for (Calibration calibration : standards) {
        JsonObject calibObject = new JsonObject();
        calibObject.addProperty("name", calibration.getTarget());
        calibObject.addProperty("concentration",
          Double.parseDouble(calibration.getHumanReadableCoefficients()));
        calibObject.addProperty("date",
          DateTimeUtils.toIsoDate(calibration.getDeploymentDate()));

        externalStandards.add(calibObject);
      }

      calibrationObject.add("gasStandards", externalStandards);
    }

    // Sensors
    CalibrationSet sensorCalibrations = SensorCalibrationDB.getInstance()
      .getMostRecentCalibrations(conn, instrument, dataset.getStart());

    if (sensorCalibrations.size() > 0) {
      JsonArray calibsArray = new JsonArray();

      for (Calibration calibration : sensorCalibrations) {
        if (calibration.isSet()) {
          JsonObject calibObject = new JsonObject();

          long columnId = Long.parseLong(calibration.getTarget());
          String sensorName = instrument.getSensorAssignments()
            .getById(columnId).getSensorName();

          calibObject.addProperty("name", sensorName);
          calibObject.addProperty("formula",
            calibration.getHumanReadableCoefficients());
          calibObject.addProperty("date",
            DateTimeUtils.toIsoDate(calibration.getDeploymentDate()));

          calibsArray.add(calibObject);
        }

      }

      if (calibsArray.size() > 0) {
        calibrationObject.add("sensorCalibrations", calibsArray);
      }
    }

    if (calibrationObject.size() > 0) {
      result.add("calibration", calibrationObject);
    }

    return result;
  }

  public static LinkedHashMap<Long, DataSet> getDatasetsWithStatus(
    DataSource dataSource, int status)
    throws MissingParamException, DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      return getDatasetsWithStatus(conn, status);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting datasets", e);
    }

  }

  /**
   * Get the all the datasets that are ready for export, but not already
   * being/been exported
   *
   * @param conn
   *          A database connection
   * @param status
   *          The status value
   * @return The exportable datasets
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static LinkedHashMap<Long, DataSet> getDatasetsWithStatus(
    Connection conn, int status)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");

    LinkedHashMap<Long, DataSet> dataSets = new LinkedHashMap<Long, DataSet>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(makeGetDatasetsQuery("status"));
      stmt.setInt(1, status);
      records = stmt.executeQuery();
      while (records.next()) {
        DataSet dataset = dataSetFromRecord(conn, records);
        dataSets.put(dataset.getId(), dataset);
      }

    } catch (Exception e) {
      throw new DatabaseException("Error while getting datasets", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return dataSets;
  }

  /**
   * Get the {@link DataSet}s between two dates for a given instrument.
   *
   * <p>
   * Any dataset that is partially covered by the selected date range will be
   * included in the results.
   * </p>
   *
   * <p>
   * If either the {@code start} or {@code end} dates are {@code null}, the
   * method assumes that they are infinitely far away in time and will therefore
   * encompass all datasets.
   * </p>
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument's database ID
   * @param start
   *          The start date
   * @param end
   *          The end date
   * @return The matching {@link DataSet}s
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static List<DataSet> getDatasetsBetweenDates(DataSource dataSource,
    long instrumentId, LocalDateTime start, LocalDateTime end)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    List<DataSet> result = new ArrayList<DataSet>();

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(GET_DATASETS_BETWEEN_DATES_QUERY)) {

      long startDateMillis = null != start ? DateTimeUtils.dateToLong(start)
        : Long.MIN_VALUE;
      long endDateMillis = null != end ? DateTimeUtils.dateToLong(end)
        : Long.MAX_VALUE;

      stmt.setLong(1, instrumentId);
      stmt.setLong(2, endDateMillis);
      stmt.setLong(3, startDateMillis);

      try (ResultSet records = stmt.executeQuery()) {

        while (records.next()) {
          result.add(dataSetFromRecord(conn, records));
        }

      } catch (Exception e) {
        throw new DatabaseException("Error while retrieving datasets", e);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving datasets", e);
    }

    return result;
  }

  /**
   * Get the number of NRT datasets defined for the specified instrument.
   *
   * @param conn
   *          A database connection.
   * @param instrumentId
   *          The instrument's database ID.
   * @return The number of NRT datasets.
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static int getNrtCount(Connection conn, long instrumentId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    int result = 0;

    try (PreparedStatement stmt = conn.prepareStatement(NRT_COUNT_QUERY)) {

      stmt.setLong(1, instrumentId);

      try (ResultSet records = stmt.executeQuery()) {
        records.next();
        result = records.getInt(1);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting NRT count", e);
    }

    return result;
  }

  public static List<NrtStatus> getNrtStatus(DataSource dataSource)
    throws DatabaseException, MissingParamException, RecordNotFoundException,
    InstrumentException, SensorGroupsException {

    List<NrtStatus> result = new ArrayList<NrtStatus>();

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn.prepareStatement(NRT_STATUS_QUERY)) {

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {

          long instrumentId = records.getLong(1);
          LocalDateTime createdDate = DateTimeUtils
            .longToDate(records.getTimestamp(2).getTime());
          LocalDateTime lastRecord = DateTimeUtils
            .longToDate(records.getLong(3));
          int status = records.getInt(4);
          LocalDateTime statusDate = DateTimeUtils
            .longToDate(records.getLong(5));

          Instrument instrument = InstrumentDB.getInstrument(conn,
            instrumentId);

          result.add(new NrtStatus(instrument, createdDate, lastRecord, status,
            statusDate));

        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error getting NRT status", e);
    }

    return result;

  }

  public static void storeUserMessages(DataSource dataSource, DataSet dataset)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataset, "dataset");

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(UPDATE_USER_MESSAGES_STATEMENT)) {

      stmt.setString(1, dataset.getUserMessages().getStorageString());
      stmt.setLong(2, dataset.getId());
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing user messages", e);
    }
  }

  public static Map<Long, Integer> getDataSetCounts(DataSource dataSource,
    List<Long> instruments) throws DatabaseException {

    Map<Long, Integer> result = new HashMap<Long, Integer>();

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instruments, "instruments", true);

    if (instruments.size() > 0) {
      try (Connection conn = dataSource.getConnection()) {

        String sql = DatabaseUtils.makeInStatementSql(GET_DATASET_COUNTS_QUERY,
          instruments.size());

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

          for (int i = 0; i < instruments.size(); i++) {
            stmt.setLong(i + 1, instruments.get(i));
          }

          try (ResultSet records = stmt.executeQuery()) {
            while (records.next()) {
              result.put(records.getLong(1), records.getInt(2));
            }
          }
        }
      } catch (SQLException e) {
        throw new DatabaseException("Error getting dataset counts", e);
      }
    }

    return result;
  }

  public static void setDatasetExported(Connection conn, long datasetId)
    throws DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(datasetId, "datasetId");

    try (PreparedStatement stmt = conn
      .prepareStatement(DATASET_EXPORTED_STATEMENT)) {
      stmt.setLong(1, datasetId);
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error setting dataset export status", e);
    }
  }
}
