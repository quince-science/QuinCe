package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
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
 *
 * @author Steve Jones
 *
 */
public class DataSetDB {

  /**
   * Statement to add a new data set into the database
   *
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String ADD_DATASET_STATEMENT = "INSERT INTO dataset "
    + "(instrument_id, name, start, end, status, status_date, "
    + "nrt, properties, last_touched, messages_json,"
    + "min_longitude, max_longitude, min_latitude, max_latitude) "
    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // 14

  /**
   * Statement to update a data set in the database
   *
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String UPDATE_DATASET_STATEMENT = "Update dataset set "
    + "instrument_id = ?, name = ?, start = ?, end = ?, status = ?, " // 5
    + "status_date = ?, nrt = ?, properties = ?, last_touched = ?, " // 9
    + "messages_json = ?, min_longitude = ?, max_longitude = ?, " // 12
    + "min_latitude = ?, max_latitude = ? WHERE id = ?"; // 15

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
    + "d.id, d.instrument_id, d.name, d.start, d.end, d.status, " // 6
    + "d.status_date, d.nrt, d.properties, d.created, d.last_touched, " // 11
    + "COALESCE(d.messages_json, '[]'), " // 12
    + "d.min_longitude, d.max_longitude, d.min_latitude, d.max_latitude " // 16
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
  public static List<DataSet> getDataSets(DataSource dataSource,
    long instrumentId, boolean includeNrt)
    throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    List<DataSet> result = null;
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
  public static List<DataSet> getDataSets(Connection conn, long instrumentId,
    boolean includeNrt) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    List<DataSet> result = new ArrayList<DataSet>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(makeGetDatasetsQuery("instrument_id"));
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {
        DataSet dataSet = dataSetFromRecord(records);
        if (!dataSet.isNrt() || includeNrt) {
          result.add(dataSet);
        }
      }

    } catch (SQLException e) {
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
   */
  private static DataSet dataSetFromRecord(ResultSet record)
    throws SQLException {

    long id = record.getLong(1);
    long instrumentId = record.getLong(2);
    String name = record.getString(3);
    LocalDateTime start = DateTimeUtils.longToDate(record.getLong(4));
    LocalDateTime end = DateTimeUtils.longToDate(record.getLong(5));
    int status = record.getInt(6);
    LocalDateTime statusDate = DateTimeUtils.longToDate(record.getLong(7));
    boolean nrt = record.getBoolean(8);

    Type propertiesType = new TypeToken<Map<String, Properties>>() {
    }.getType();
    Map<String, Properties> properties = new Gson()
      .fromJson(record.getString(9), propertiesType);

    LocalDateTime createdDate = DateTimeUtils
      .longToDate(record.getTimestamp(10).getTime());

    LocalDateTime lastTouched = DateTimeUtils.longToDate(record.getLong(11));
    String json = record.getString(12);
    JSONArray array = new JSONArray(json);
    ArrayList<Message> messages = new ArrayList<>();
    for (Object o : array) {
      if (o instanceof JSONObject) {
        JSONObject jo = (JSONObject) o;
        Message m = new Message(jo.getString("message"),
          jo.getString("details"));
        messages.add(m);
      }
    }

    double minLon = record.getDouble(13);
    double maxLon = record.getDouble(14);
    double minLat = record.getDouble(15);
    double maxLat = record.getDouble(16);

    return new DataSet(id, instrumentId, name, start, end, status, statusDate,
      nrt, properties, createdDate, lastTouched, messages, minLon, minLat,
      maxLon, maxLat);
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

      stmt = conn.prepareStatement(sql,
        PreparedStatement.RETURN_GENERATED_KEYS);

      stmt.setLong(1, dataSet.getInstrumentId());
      stmt.setString(2, dataSet.getName());
      stmt.setLong(3, DateTimeUtils.dateToLong(dataSet.getStart()));
      stmt.setLong(4, DateTimeUtils.dateToLong(dataSet.getEnd()));
      stmt.setInt(5, dataSet.getStatus());
      stmt.setLong(6, DateTimeUtils.dateToLong(dataSet.getStatusDate()));
      stmt.setBoolean(7, dataSet.isNrt());
      stmt.setString(8, new Gson().toJson(dataSet.getAllProperties()));
      stmt.setLong(9, DateTimeUtils.dateToLong(LocalDateTime.now()));

      if (dataSet.getMessageCount() > 0) {
        String jsonString = dataSet.getMessagesAsJSONString();
        stmt.setString(10, jsonString);
      } else {
        stmt.setNull(10, Types.VARCHAR);
      }

      stmt.setDouble(11, dataSet.getMinLon());
      stmt.setDouble(12, dataSet.getMaxLon());
      stmt.setDouble(13, dataSet.getMinLat());
      stmt.setDouble(14, dataSet.getMaxLat());

      if (DatabaseUtils.NO_DATABASE_RECORD != dataSet.getId()) {
        stmt.setLong(15, dataSet.getId());
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
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getDataSet(conn, id);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
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

    PreparedStatement stmt = null;
    ResultSet record = null;

    try {
      stmt = conn.prepareStatement(makeGetDatasetsQuery("id"));
      stmt.setLong(1, id);

      record = stmt.executeQuery();

      if (!record.next()) {
        throw new RecordNotFoundException("Data set does not exist", "dataset",
          id);
      } else {
        result = dataSetFromRecord(record);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
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

    List<DataSet> datasets = getDataSets(conn, instrumentId, includeNrt);
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
          DataSet dataset = dataSetFromRecord(records);
          result = dataset;
          break;
        }
      }

    } catch (SQLException e) {
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
  public static JSONObject getMetadataJson(DataSource dataSource,
    DataSet dataset) throws DatabaseException, MissingParamException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {

    JSONObject result = null;
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
  public static JSONObject getMetadataJson(Connection conn, DataSet dataset)
    throws DatabaseException, MissingParamException, RecordNotFoundException,
    InstrumentException, SensorGroupsException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    Instrument instrument = InstrumentDB.getInstrument(conn,
      dataset.getInstrumentId());

    JSONObject result = new JSONObject();
    result.put("name", dataset.getName());
    result.put("startdate", DateTimeUtils.toIsoDate(dataset.getStart()));
    result.put("enddate", DateTimeUtils.toIsoDate(dataset.getEnd()));
    result.put("platformCode", instrument.getPlatformCode());
    result.put("nrt", dataset.isNrt());
    result.put("last_touched",
      DateTimeUtils.toIsoDate(dataset.getLastTouched()));

    int recordCount = DataSetDataDB.getRecordCount(conn, dataset.getId());
    result.put("records", recordCount);

    JSONObject boundsObject = new JSONObject();
    boundsObject.put("south", dataset.getMinLat());
    boundsObject.put("west", dataset.getMinLon());
    boundsObject.put("east", dataset.getMaxLon());
    boundsObject.put("north", dataset.getMaxLat());
    result.put("bounds", boundsObject);

    return result;
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
  public static List<DataSet> getDatasetsWithStatus(Connection conn, int status)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");

    List<DataSet> dataSets = new ArrayList<DataSet>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(makeGetDatasetsQuery("status"));
      stmt.setInt(1, status);
      records = stmt.executeQuery();
      while (records.next()) {
        dataSets.add(dataSetFromRecord(records));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting export datasets", e);
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
          result.add(dataSetFromRecord(records));
        }

      } catch (SQLException e) {
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
}
