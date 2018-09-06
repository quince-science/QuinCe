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
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.Message;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for manipulating data sets in the database
 * @author Steve Jones
 *
 */
public class DataSetDB {

  /**
   * Query to get the defined data sets for a given instrument
   * @see #getDataSets(DataSource, long)
   */
  private static final String GET_DATASETS_QUERY = "SELECT "
      + "d.id, d.instrument_id, d.name, d.start, d.end, d.status, "
      + "d.properties, d.last_touched, COUNT(c.user_flag), "
      + "COALESCE(d.messages_json, '[]') "
      + "FROM dataset d "
      + "LEFT JOIN dataset_data dd ON d.id = dd.dataset_id "
      + "LEFT JOIN equilibrator_pco2 c ON c.measurement_id = dd.id AND c.user_flag = " + Flag.VALUE_NEEDED + " "
      + "WHERE d.instrument_id = ? "
      + "GROUP BY d.id "
      + "ORDER BY d.start ASC";

  /**
   * Statement to add a new data set into the database
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String ADD_DATASET_STATEMENT = "INSERT INTO dataset "
      + "(instrument_id, name, start, end, status, properties, last_touched, "
      + "messages_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"; // 9

  /**
   * Statement to update a data set in the database
   *
   * @see #addDataSet(DataSource, DataSet)
   */
  private static final String UPDATE_DATASET_STATEMENT = "Update dataset set "
      + " instrument_id = ?, name = ?, start = ?, end = ?, status = ?, " // 5
      + " properties = ?, last_touched = ?, messages_json = ? WHERE id = ?"; // 9

  /**
   * Query to get a single data set by its ID
   *
   * @see #getDataSet(DataSource, long)
   */
  private static final String GET_DATASET_QUERY = "SELECT "
      + "d.id, d.instrument_id, d.name, d.start, d.end, d.status, "
      + "d.properties, d.last_touched, COUNT(c.user_flag), "
      + "COALESCE(d.messages_json, '[]') "
      + "FROM dataset d "
      + "LEFT JOIN dataset_data dd ON d.id = dd.dataset_id "
      + "LEFT JOIN equilibrator_pco2 c ON c.measurement_id = dd.id AND c.user_flag = " + Flag.VALUE_NEEDED + " "
      + "WHERE d.id = ? "
      + "GROUP BY d.id";

  /**
   * Statement to delete all records for a given dataset
   */
  private static final String DELETE_DATASET_QUERY = "DELETE FROM dataset_data "
      + "WHERE dataset_id = ?";

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

    List<DataSet> result = new ArrayList<DataSet>();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_DATASETS_QUERY);
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
      DatabaseUtils.closeConnection(conn);
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
    Properties properties = null;
    LocalDateTime lastTouched = DateTimeUtils.longToDate(record.getLong(8));
    int needsFlagCount = record.getInt(9);
    String json = record.getString(10);
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

    return new DataSet(id, instrumentId, name, start, end, status, properties,
        lastTouched, needsFlagCount, messages);
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
      stmt.setLong(5, dataSet.getStatus());
      stmt.setNull(6, Types.VARCHAR);
      stmt.setLong(7, DateTimeUtils.dateToLong(LocalDateTime.now()));

      if (dataSet.getMessageCount() > 0) {
        String jsonString = dataSet.getMessagesAsJSONString();
        stmt.setString(8, jsonString);
      } else {
        stmt.setNull(8, Types.VARCHAR);
      }

      if (DatabaseUtils.NO_DATABASE_RECORD != dataSet.getId()) {
        stmt.setLong(9, dataSet.getId());
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
      stmt = conn.prepareStatement(GET_DATASET_QUERY);
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
    if (dataSet.getStatus() != status) {
      dataSet.setStatus(status);
      updateDataSet(conn, dataSet);
    }
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
      stmt = conn.prepareStatement(DELETE_DATASET_QUERY);
      stmt.setLong(1, dataSet.getId());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting dataset data", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  public static void updateDataSet(Connection conn, DataSet dataSet)
      throws MissingParamException, DatabaseException, RecordNotFoundException {
    saveDataSet(conn, dataSet);
  }
}
