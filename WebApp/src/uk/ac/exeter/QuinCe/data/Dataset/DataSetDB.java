package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.Message;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Methods for manipulating data sets in the database
 * @author Steve Jones
 *
 */
public class DataSetDB {

  /**
   * Statement to add a new data set into the database
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String ADD_DATASET_STATEMENT = "INSERT INTO dataset "
      + "(instrument_id, name, start, end, status, status_date, "
      + "nrt, properties, last_touched, messages_json) "
      + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // 10

  /**
   * Statement to update a data set in the database
   *
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String UPDATE_DATASET_STATEMENT = "Update dataset set "
      + "instrument_id = ?, name = ?, start = ?, end = ?, status = ?, " // 5
      + "status_date = ?, nrt = ?, properties = ?, last_touched = ?, " // 9
      + "messages_json = ? WHERE id = ?"; // 11

  /**
   * Statement to delete all records for a given dataset
   */
  private static final String DELETE_DATASET_DATA_QUERY = "DELETE FROM dataset_data "
      + "WHERE dataset_id = ?";

   /**
   * Statement to delete a dataset record
   */
  private static final String DELETE_DATASET_QUERY = "DELETE FROM dataset "
      + "WHERE id = ?";

 /**
   * Make an SQL query for retrieving complete datasets using
   * a specified WHERE clause
   * @param whereField The field to use in the WHERE clause
   * @return The query SQL
   */
  private static String makeGetDatasetsQuery(String whereField) {
    StringBuilder sql = new StringBuilder("SELECT "
        + "d.id, d.instrument_id, d.name, d.start, d.end, d.status, " // 6
        + "d.status_date, d.nrt, d.properties, d.last_touched, " // 9
        + "COALESCE(d.messages_json, '[]'), " // 10
        + "COUNT(sv.user_qc_flag) "
        + "FROM dataset d "
        + "LEFT JOIN sensor_values sv "
        + "ON (d.id = sv.dataset_id AND "
        + "sv.user_qc_flag = " + Flag.VALUE_NEEDED + ") "
        + "WHERE d.");

    sql.append(whereField);
    sql.append(" = ? GROUP BY d.id ORDER BY d.start ASC");

    System.out.println(sql.toString());

    return sql.toString();
  }

  /**
   * Get the list of data sets defined for a given instrument
   * @param dataSource A data source
   * @param instrumentId The instrument's database ID
   * @return The list of data sets
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSet> getDataSets(DataSource dataSource, long instrumentId) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    List<DataSet> result = null;
    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      result = getDataSets(conn, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving data sets", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the list of data sets defined for a given instrument
   * @param dataSource A data source
   * @param instrumentId The instrument's database ID
   * @return The list of data sets
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSet> getDataSets(Connection conn, long instrumentId) throws DatabaseException, MissingParamException {

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
        result.add(dataSetFromRecord(records));
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
   * @param record The search result
   * @return The Data Set object
   * @throws SQLException If the data cannot be extracted from the result
   */
  private static DataSet dataSetFromRecord(ResultSet record) throws SQLException {

    long id = record.getLong(1);
    long instrumentId = record.getLong(2);
    String name = record.getString(3);
    LocalDateTime start = DateTimeUtils.longToDate(record.getLong(4));
    LocalDateTime end = DateTimeUtils.longToDate(record.getLong(5));
    int status = record.getInt(6);
    LocalDateTime statusDate = DateTimeUtils.longToDate(record.getLong(7));
    boolean nrt = record.getBoolean(8);
    Properties properties = null; // 9
    LocalDateTime lastTouched = DateTimeUtils.longToDate(record.getLong(10));
    String json = record.getString(11);
    JSONArray array = new JSONArray(json);
    ArrayList<Message> messages = new ArrayList<>();
    for (Object o: array) {
      if (o instanceof JSONObject) {
        JSONObject jo = (JSONObject) o;
        Message m = new Message(
            jo.getString("message"), jo.getString("details")
        );
        messages.add(m);
      }
    }

    int needsFlagCount = record.getInt(12);

    return new DataSet(id, instrumentId, name, start, end, status, statusDate,
        nrt, properties, lastTouched, needsFlagCount, messages);
  }

  /**
   * Store a new data set in the database.
   *
   * The created data set's ID is stored in the provided {@link DataSet} object
   * @param dataSource A data source
   * @param dataSet The data set to be stored
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void addDataSet(DataSource dataSource, DataSet dataSet) throws DatabaseException, MissingParamException {
    // Make sure this inserts a new record
    dataSet.setId(DatabaseUtils.NO_DATABASE_RECORD);
    saveDataSet(dataSource, dataSet);
  }

  /**
   * Store a new data set in the database.
   *
   * The created data set's ID is stored in the provided {@link DataSet} object
   * @param dataSource A data source
   * @param dataSet The data set to be stored
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void addDataSet(Connection conn, DataSet dataSet) throws DatabaseException, MissingParamException {
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
      stmt.setNull(8, Types.VARCHAR);
      stmt.setLong(9, DateTimeUtils.dateToLong(LocalDateTime.now()));

      if (dataSet.getMessageCount() > 0) {
        String jsonString = dataSet.getMessagesAsJSONString();
        stmt.setString(10, jsonString);
      } else {
        stmt.setNull(10, Types.VARCHAR);
      }

      if (DatabaseUtils.NO_DATABASE_RECORD != dataSet.getId()) {
        stmt.setLong(11, dataSet.getId());
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
   * @param dataSource A data source
   * @param id The data set's id
   * @return The data set
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the data set does not exist
   */
  public static DataSet getDataSet(DataSource dataSource, long id) throws DatabaseException, MissingParamException, RecordNotFoundException {

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
   * @param conn A database connection
   * @param id The data set's id
   * @return The data set
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the data set does not exist
   */
  public static DataSet getDataSet(Connection conn, long id) throws DatabaseException, MissingParamException, RecordNotFoundException {

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
        throw new RecordNotFoundException("Data set does not exist", "dataset", id);
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
   * @param dataSetId
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
   */
  public static void setDatasetStatus(Connection conn, long datasetId,
      int status) throws MissingParamException, InvalidDataSetStatusException,
      DatabaseException, RecordNotFoundException {
    DataSet dataSet = getDataSet(conn, datasetId);
    dataSet.setStatus(status);
    updateDataSet(conn, dataSet);
  }

  /**
   * Delete all records for a given data set
   *
   * @param conn
   *          A database connection
   * @param dataSet
   *          The data set
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static void deleteDatasetData(Connection conn, DataSet dataSet) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataSet, "dataSet");

    PreparedStatement stmt = null;

    try {
      stmt = conn.prepareStatement(DELETE_DATASET_DATA_QUERY);
      stmt.setLong(1, dataSet.getId());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting dataset data", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Update a dataset in the database
   * @param conn A database connection
   * @param dataSet The updated dataset
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
   * @param conn A database connection
   * @param instrument The instrument's database ID
   * @return The most recent dataset, or {@code null} if there are no datasets
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static DataSet getLastDataSet(Connection conn, long instrumentId) throws MissingParamException, DatabaseException {
    DataSet result = null;

    List<DataSet> datasets = getDataSets(conn, instrumentId);
    if (datasets.size() > 0) {
      result = datasets.get(datasets.size() - 1);
    }

    return result;
  }

  /**
   * Retrieve the NRT data set for an instrument
   * @param conn A database connection
   * @param instrument The instrument's database ID
   * @return The NRT dataset, or {@code null} if there there isn't one
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static DataSet getNrtDataSet(Connection conn, long instrumentId) throws MissingParamException, DatabaseException {
    DataSet result = null;

    for (DataSet dataSet : getDataSets(conn, instrumentId)) {
      if (dataSet.isNrt()) {
        result = dataSet;
        break;
      }
    }

    return result;
  }

  /**
   * Delete all NRT datasets defined for a given instrument.
   * In theory there should be only one, but this deletes
   * all that it can find, just in case.
   * @param dataSource A data source
   * @param instrumentId The instrument's database ID
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static void deleteNrtDataSet(DataSource dataSource, long instrumentId)
      throws MissingParamException, DatabaseException {

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
   * Delete all NRT datasets defined for a given instrument.
   * In theory there should be only one, but this deletes
   * all that it can find, just in case.
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static void deleteNrtDataSet(Connection conn, long instrumentId)
      throws MissingParamException, DatabaseException {
    for (DataSet dataSet : getDataSets(conn, instrumentId)) {
      if (dataSet.isNrt()) {
        deleteDataSet(conn, dataSet);
      }
    }
  }

  /**
   * Delete a dataset and all related records
   * @param conn A database connection
   * @param dataSet The dataset to be deleted
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public static void deleteDataSet(Connection conn, DataSet dataSet) throws MissingParamException, DatabaseException {

    // Delete all related data
    CalculationDBFactory.getCalculationDB().deleteDatasetCalculationData(conn, dataSet);
    CalibrationDataDB.deleteDatasetData(conn, dataSet);
    DataSetDB.deleteDatasetData(conn, dataSet);

    boolean currentAutoCommitStatus = false;
    PreparedStatement stmt = null;

    try {
      currentAutoCommitStatus = conn.getAutoCommit();
      if (currentAutoCommitStatus) {
        conn.setAutoCommit(false);
      }

      stmt = conn.prepareStatement(DELETE_DATASET_QUERY);
      stmt.setLong(1, dataSet.getId());
      stmt.execute();

      if (currentAutoCommitStatus) {
        // Return the connection to its non-transaction state
        conn.commit();
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      if (currentAutoCommitStatus) {
        try {
          conn.rollback();
          conn.setAutoCommit(true);
        } catch (SQLException e2) {
          e2.printStackTrace();
        }
      }
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }


  /**
   * Generate the metadata portion of the manifest
   * @return The metadata
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the dataset doesn't exist
   * @throws InstrumentException If the instrument details cannot be retrieved
   */
  public static JSONObject getMetadataJson(DataSource dataSource, DataSet dataset) throws DatabaseException,
    MissingParamException, RecordNotFoundException, InstrumentException {

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
   * @return The metadata
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the dataset doesn't exist
   * @throws InstrumentException If the instrument details cannot be retrieved
   */
  public static JSONObject getMetadataJson(Connection conn, DataSet dataset) throws DatabaseException,
    MissingParamException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    ResourceManager resourceManager = ResourceManager.getInstance();
    Instrument instrument = InstrumentDB.getInstrument(conn, dataset.getInstrumentId(),
        resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());

    JSONObject result = new JSONObject();
    result.put("name", dataset.getName());
    result.put("startdate", DateTimeUtils.toIsoDate(dataset.getStart()));
    result.put("enddate", DateTimeUtils.toIsoDate(dataset.getEnd()));
    result.put("platformCode", instrument.getPlatformCode());
    result.put("nrt", dataset.isNrt());
    result.put("last_touched", DateTimeUtils.toIsoDate(dataset.getLastTouched()));

    int recordCount = DataSetDataDB.getMeasurementIds(conn, dataset.getId()).size();
    result.put("records", recordCount);

    List<Double> bounds = DataSetDataDB.getDataBounds(conn, dataset);
    JSONObject boundsObject = new JSONObject();
    boundsObject.put("west", bounds.get(0));
    boundsObject.put("south", bounds.get(1));
    boundsObject.put("east", bounds.get(2));
    boundsObject.put("north", bounds.get(3));
    result.put("bounds", boundsObject);

    return result;
  }

  /**
   * Get the all the datasets that are ready for export,
   * but not already being/been exported
   * @param conn A database connection
   * @return The exportable datasets
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
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
}
