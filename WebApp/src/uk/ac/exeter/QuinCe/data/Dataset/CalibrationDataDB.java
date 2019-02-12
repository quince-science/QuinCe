package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationException;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableList;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class for handling calibration data from within a data set.
 *
 * <p>
 *   This is likely to be replaced
 *   when the new calibration data handling system is written
 *   (see Github issue #556).
 * </p>
 *
 * @author Steve Jones
 *
 */
public class CalibrationDataDB {

  /**
   * The name of the ID column
   */
  private static final String ID_COL = "id";

  /**
   * The name of the date column
   */
  private static final String DATE_COL = "date";

  /**
   * The name of the run type column
   */
  private static final String RUN_TYPE_COL = "run_type";

  /**
   * The name of the dataset ID column
   */
  private static final String DATASET_COL = "dataset_id";

  /**
   * Statement to delete all records for a given dataset
   */
  private static final String DELETE_CALIBRATION_DATA_QUERY = "DELETE FROM calibration_data "
      + "WHERE dataset_id = ?";

  /**
   * Statement to retrieve the count of all calibration data for a dataset
   */
  private static final String ALL_CALIBRATION_DATA_ROWIDS_QUERY = "SELECT id FROM calibration_data "
      + "WHERE dataset_id = ?";

  /**
   * Statement to retrieve the count of calibration data for a specific run type for a dataset
   */
  private static final String SELECTED_CALIBRATION_DATA_ROWIDS_QUERY = "SELECT id FROM calibration_data "
      + "WHERE dataset_id = ? AND run_type = ? ORDER BY date ASC";


  /**
   * Statement to set the Use flags for a set of records
   */
  private static final String SET_USE_FLAGS_STATEMENT = "UPDATE calibration_data SET "
      + "use_record = ?, use_message = ? "
      + "WHERE id IN " + DatabaseUtils.IN_PARAMS_TOKEN;

  /**
   * Query to get all records for a given dataset
   */
  private static final String GET_ALL_CALIBRATIONS_QUERY = "SELECT * FROM calibration_data WHERE dataset_id = ? AND use_record = 1 ORDER BY date ASC";

  /**
   * Store a data set record in the database.
   *
   * Measurement and calibration records are automatically detected
   * and stored in the appropriate table.
   *
   * @param conn A database connection
   * @param record The record to be stored
   * @param statement A previously generated statement for inserting a record. Can be null.
   * @return A {@link PreparedStatement} that can be used for storing subsequent records
   * @throws MissingParamException If any required parameters are missing
   * @throws DataSetException If a non-measurement record is supplied
   * @throws DatabaseException If a database error occurs
   * @throws NoSuchCategoryException If the record's Run Type is not recognised
   */
  public static PreparedStatement storeCalibrationRecord(Connection conn,
    DataSetRawDataRecord record)
      throws MissingParamException, DataSetException, DatabaseException, NoSuchCategoryException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");

    if (!record.isCalibration()) {
      throw new DataSetException("Record is not a calibration record");
    }

    PreparedStatement statement = null;

    try {
      statement = DatabaseUtils.createInsertStatement(conn, "calibration_data",
        createAllFieldsList(conn, record.getDataSet().getInstrumentId()));

      statement.setLong(1, record.getDatasetId());
      statement.setLong(2, DateTimeUtils.dateToLong(record.getDate()));
      statement.setString(3, record.getRunType());
      statement.setBoolean(4, true);
      statement.setNull(5, Types.VARCHAR);

      int currentField = 5;
      SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
      for (SensorType sensorType : sensorConfig.getSensorTypes()) {
        if (sensorConfig.isRequired(conn, record.getDataSet().getInstrumentId(), sensorType)) {
          currentField++;
          Double sensorValue = record.getSensorValue(sensorType.getName());
          if (null == sensorValue) {
            statement.setNull(currentField, Types.DOUBLE);
          } else {
            statement.setDouble(currentField, sensorValue);
          }
        }
      }

      statement.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing dataset record", e);
    } finally {
      DatabaseUtils.closeStatements(statement);
    }

    return statement;
  }

  /**
   * Generate a list of all the fields in the {@code calibration_data} table
   * @return The field list
   * @throws DatabaseException If a database error occurs
   */
  private static List<String> createAllFieldsList(Connection conn, long instrumentId) throws DatabaseException {
    List<String> fieldNames = new ArrayList<String>();

    fieldNames.add("dataset_id");
    fieldNames.add("date");
    fieldNames.add("run_type");
    fieldNames.add("use_record");
    fieldNames.add("use_message");

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    for (SensorType sensorType : sensorConfig.getSensorTypes()) {
      if (sensorConfig.isRequired(conn, instrumentId, sensorType)) {
        fieldNames.add(sensorType.getDatabaseFieldName());
      }
    }

    return fieldNames;
  }

  /**
   * Delete all records for a given data set
   * @param conn A database connection
   * @param dataSet The data set
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static void deleteDatasetData(Connection conn, DataSet dataSet) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataSet, "dataSet");

    PreparedStatement stmt = null;

    try {
      stmt = conn.prepareStatement(DELETE_CALIBRATION_DATA_QUERY);
      stmt.setLong(1, dataSet.getId());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting dataset data", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Get the external standard data for a given data set and standard in JSON format for the table view
   * If {@code standardName} is {@code null}, all external standards will be included in the results
   * @param dataSource A data source
   * @param datasetId The database ID of the data set
   * @param standardName The name of the standard ({@code null} for all standards)
   * @param start The first record to return
   * @param length The number of records to return
   * @return The standards data
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the data set doesn't exist in the database
   */
  public static String getJsonTableData(DataSource dataSource, long datasetId, String standardName, int start, int length) throws MissingParamException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    List<String> calibrationFields = getCalibrationFields();

    List<String> queryFields = new ArrayList<String>();
    queryFields.add("id");
    queryFields.add("date");
    queryFields.add("run_type");
    queryFields.addAll(calibrationFields);
    queryFields.add("use_record");
    queryFields.add("use_message");

    List<String> andFields = new ArrayList<String>();
    andFields.add("dataset_id");
    if (null != standardName) {
      andFields.add("run_type");
    }

    JSONArray json = new JSONArray();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();

      CalibrationSet externalStandards = null;
      DataSet dataSet = DataSetDB.getDataSet(conn, datasetId);

      stmt = DatabaseUtils.createSelectStatement(conn, "calibration_data", queryFields, andFields, start, length);
      stmt.setLong(1, datasetId);
      if (null != standardName) {
        stmt.setString(2, standardName);
      }

      records = stmt.executeQuery();
      int rowId = start - 1;
      while (records.next()) {

        // Get the external standards if we don't already have them
        if (null == externalStandards) {
          // The date is always the second field
          LocalDateTime firstDate = DateTimeUtils.longToDate(records.getLong(2));
          externalStandards = ExternalStandardDB.getInstance().getStandardsSet(conn, dataSet.getInstrumentId(), firstDate);

          if (!externalStandards.isComplete()) {
            throw new CalibrationException("No complete set of external standards available");
          }
        }

        rowId++;
        int columnIndex = 0;
        int dbColumn = 0;

        JSONObject jsonRecord = new JSONObject();
        jsonRecord.put("DT_RowId", "row" + rowId);

        columnIndex++;
        dbColumn++;
        jsonRecord.put(String.valueOf(columnIndex - 1), records.getLong(dbColumn)); // id

        columnIndex++;
        dbColumn++;
        jsonRecord.put(String.valueOf(columnIndex - 1), records.getLong(dbColumn)); // date

        columnIndex++;
        dbColumn++;
        String runType = records.getString(dbColumn);
        jsonRecord.put(String.valueOf(columnIndex - 1), runType); //Run Type

        for (int i = 0; i < calibrationFields.size(); i++) {
          columnIndex++;
          dbColumn++;
          jsonRecord.put(String.valueOf(columnIndex - 1), records.getDouble(dbColumn));
        }

        double calibrationValue = externalStandards.getCalibrationValue(runType, "CO2");
        columnIndex++;
        jsonRecord.put(String.valueOf(columnIndex - 1), calibrationValue);
        columnIndex++;
        jsonRecord.put(String.valueOf(columnIndex - 1), records.getDouble(dbColumn) - calibrationValue);

        columnIndex++;
        dbColumn++;
        jsonRecord.put(String.valueOf(columnIndex - 1), records.getBoolean(dbColumn)); // Use?

        // Use message
        columnIndex++;
        dbColumn++;
        String message = records.getString(dbColumn);
        if (null == message) {
          jsonRecord.put(String.valueOf(columnIndex - 1), "");
        } else {
          jsonRecord.put(String.valueOf(columnIndex - 1), message);
        }

        json.put(jsonRecord);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error retrieving calibration data", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return json.toString();
  }

  /**
   * Get the external standard data for a given data set and standard in JSON format for the table view
   * If {@code standardName} is {@code null}, all external standards will be included in the results
   * @param dataSource A data source
   * @param dataset The data set
   * @param standardNames The names of the standards ({@code null} for all standards)
   * @return The standards data
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the dataset doesn't exist in the database
   */
  public static String getJsonPlotData(DataSource dataSource, DataSet dataset, List<String> standardNames) throws MissingParamException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataset, "dataset");

    List<String> calibrationFields = getCalibrationFields();

    StringBuilder sql = new StringBuilder();

    sql.append("SELECT run_type, date, id, use_record, ");
    for (int i = 0; i < calibrationFields.size(); i++) {
      sql.append(calibrationFields.get(i));

      if (i < calibrationFields.size() - 1) {
        sql.append(',');
      }
    }

    sql.append(" FROM calibration_data ");

    sql.append(" WHERE dataset_id = ?");

    if (null != standardNames && standardNames.size() > 0) {
      sql.append(" AND run_type IN (");

      for (int i = 0; i < standardNames.size(); i++) {
        sql.append('"');
        sql.append(standardNames.get(i));
        sql.append('"');

        if (i < standardNames.size() - 1) {
          sql.append(',');
        }
      }

      sql.append(')');
    }

    JSONArray json = new JSONArray();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();

      CalibrationSet externalStandards = null;

      stmt = conn.prepareStatement(sql.toString());
      stmt.setLong(1, dataset.getId());

      records = stmt.executeQuery();
      while (records.next()) {
        // Get the external standards if we don't already have them
        if (null == externalStandards) {
          // The date is always the second field
          LocalDateTime firstDate = DateTimeUtils.longToDate(records.getLong(2));
          externalStandards = ExternalStandardDB.getInstance().getStandardsSet(conn, dataset.getInstrumentId(), firstDate);

          if (!externalStandards.isComplete()) {
            throw new CalibrationException("No complete set of external standards available");
          }
        }

        JSONArray jsonRecord = new JSONArray();

        int columnIndex = 0;
        columnIndex++;
        String runType = records.getString(columnIndex);

        columnIndex++;
        jsonRecord.put(records.getLong(columnIndex)); // date;

        columnIndex++;
        jsonRecord.put(records.getLong(columnIndex)); // id

        // The Use Record flag is converted to BAD or GOOD for the plot highlighting functions
        columnIndex++;
        if (records.getBoolean(columnIndex)) {
          jsonRecord.put(Flag.GOOD.getFlagValue());
        } else {
          jsonRecord.put(Flag.BAD.getFlagValue());
        }

        for (int i = 0; i < calibrationFields.size(); i++) {
          columnIndex++;

          double value = records.getDouble(columnIndex);

          for (int j = 0; j < standardNames.size(); j++) {
            if (runType.equals(standardNames.get(j))) {
              double calibrationValue = externalStandards.getCalibrationValue(runType, "CO2");
              jsonRecord.put(value - calibrationValue);
            } else {
              jsonRecord.put(JSONObject.NULL);
            }
          }
        }

        json.put(jsonRecord);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error retrieving calibration data", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return json.toString();
  }

  /**
   * Get the calibration fields from the system
   * @return The calibration fields
   */
  private static List<String> getCalibrationFields() {
    List<String> calibrationFields = new ArrayList<String>();
    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    for (SensorType sensorType : sensorConfig.getSensorTypes()) {
      if (sensorType.hasInternalCalibration()) {
        calibrationFields.add(sensorType.getDatabaseFieldName());
      }
    }
    return calibrationFields;
  }

  /**
   * Get the calibration fields from the system
   * @return The calibration fields
   */
  private static List<String> getCalibrationFieldNames() {
    List<String> calibrationFields = new ArrayList<String>();
    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    for (SensorType sensorType : sensorConfig.getSensorTypes()) {
      if (sensorType.hasInternalCalibration()) {
        calibrationFields.add(sensorType.getName());
      }
    }
    return calibrationFields;
  }

  /**
   * Get the external standard data for a given data set and standard in JSON format
   * If {@code standardName} is {@code null}, all external standards will be included in the results
   * @param dataSource A data source
   * @param datasetId The database ID of the data set
   * @param standardName The name of the standard ({@code null} for all standards)
   * @return The standards data
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static List<Long> getCalibrationRowIds(DataSource dataSource, long datasetId, String standardName) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    List<Long> result = new ArrayList<Long>();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      if (null == standardName) {
        stmt = conn.prepareStatement(ALL_CALIBRATION_DATA_ROWIDS_QUERY);
        stmt.setLong(1, datasetId);
      } else {
        stmt = conn.prepareStatement(SELECTED_CALIBRATION_DATA_ROWIDS_QUERY);
        stmt.setLong(1, datasetId);
        stmt.setString(2, standardName);
      }

      records = stmt.executeQuery();
      while (records.next()) {
        result.add(records.getLong(1));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error retrieving calibration data", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Set the use flags on a set of rows in a data set
   * @param dataSource A data set
   * @param ids The row IDs
   * @param use The Use flag
   * @param useMessage The message for the flag (only used if the flag is {@code false}
   * @throws MissingParamException If any required flags are missing
   * @throws DatabaseException If a database error occurs
   */
  public static void setCalibrationUse(DataSource dataSource, List<Long> ids, boolean use, String useMessage) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(ids, "ids");
    if (!use) {
      MissingParam.checkMissing(useMessage, "useMessage");
    }

    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(DatabaseUtils.makeInStatementSql(SET_USE_FLAGS_STATEMENT, ids.size()));
      stmt.setBoolean(1, use);

      if (use) {
        stmt.setNull(2, Types.VARCHAR);
      } else {
        stmt.setString(2, useMessage);
      }

      int valueIndex = 2;
      for (long id : ids) {
        valueIndex++;
        stmt.setLong(valueIndex, id);
      }

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing calibration use flags", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Get all the calibration records for a dataset
   * @param conn A database connection
   * @param dataSet The data set
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static CalibrationDataSet getCalibrationRecords(Connection conn, DataSet dataSet) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataSet, "dataSet");

    PreparedStatement stmt = null;
    ResultSet records = null;

    CalibrationDataSet result = new CalibrationDataSet();

    int idCol = -1;
    int dateCol = -1;
    int runTypeCol = -1;

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    Map<Integer, String> calibrationColumns = new HashMap<Integer, String>();

    try {
      stmt = conn.prepareStatement(GET_ALL_CALIBRATIONS_QUERY);
      stmt.setLong(1, dataSet.getId());

      records = stmt.executeQuery();
      ResultSetMetaData rsmd = records.getMetaData();

      while (records.next()) {

        // Get the column indices if we haven't already got them
        if (idCol == -1) {
          for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String columnName = rsmd.getColumnName(i);
            switch (columnName) {
            case ID_COL: {
              idCol = i;
              break;
            }
            case DATE_COL: {
              dateCol = i;
              break;
            }
            case RUN_TYPE_COL: {
              runTypeCol = i;
              break;
            }
            case DATASET_COL: {
              // Do nothing
              break;
            }
            default: {
              // This is a sensor field. Get the sensor name from the sensors configuration
              for (SensorType sensorType : sensorConfig.getSensorTypes()) {
                if (sensorConfig.isRequired(conn, dataSet.getInstrumentId(), sensorType)) {
                  if (columnName.equals(sensorType.getDatabaseFieldName())) {
                    calibrationColumns.put(i, sensorType.getName());
                  }
                }
              }
            }
            }
          }
        }

        long id = records.getLong(idCol);
        LocalDateTime date = DateTimeUtils.longToDate(records.getLong(dateCol));
        double longitude = DataSetRawDataRecord.NO_POSITION;
        double latitude = DataSetRawDataRecord.NO_POSITION;
        String runType = records.getString(runTypeCol);
        RunTypeCategory runTypeCategory = ResourceManager.getInstance().getRunTypeCategoryConfiguration().getCategory(runType);

        DataSetRawDataRecord measurement = new DataSetRawDataRecord(dataSet, id, date, longitude, latitude, runType, runTypeCategory);

        for (Map.Entry<Integer, String> entry : calibrationColumns.entrySet()) {
          measurement.setSensorValue(entry.getValue(), records.getDouble(entry.getKey()));
        }

        result.add(measurement);
      }

      return result;

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurements", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Populate a variables list with the calibration fields
   * @param dataSource A data source
   * @param dataset A data set
   * @param variables The variables list
   * @throws Exception If an error occurs
   */
  public static void populateVariableList(DataSource dataSource, DataSet dataset, VariableList variables) throws Exception {
    for (String fieldName : getCalibrationFieldNames()) {
      for (String runType : InstrumentDB.getRunTypes(dataSource, dataset.getInstrumentId(), "EXT")) {
        variables.addVariable(fieldName, new Variable(Variable.TYPE_SENSOR, runType, runType, false, true, true));
      }
    }
  }
}
