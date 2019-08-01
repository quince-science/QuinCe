package uk.ac.exeter.QuinCe.data.Dataset;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducerFactory;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableList;
import uk.ac.exeter.QuinCe.web.datasets.data.DatasetMeasurementData;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class for handling database queries related to the
 * {@code dataset_data} table
 * @author Steve Jones
 *
 */
public class DataSetDataDB {

  /**
   * Field Set ID for sensor values
   */
  public static final long SENSORS_FIELDSET = -1;

  /**
   * Field set name for sensor values
   */
  public static final String SENSORS_FIELDSET_NAME = "Sensors";

  /**
   * Field Set ID for diagnostic values
   */
  public static final long DIAGNOSTICS_FIELDSET = -2;

  /**
   * Field set name for diagnostic values
   */
  public static final String DIAGNOSTICS_FIELDSET_NAME = "Diagnostics";

  /**
   * Query to get all measurements IDs for a data set
   */
  private static final String GET_ALL_MEASUREMENT_IDS_QUERY = "SELECT id FROM dataset_data WHERE dataset_id = ? ORDER BY date ASC";

  /**
   * Query to get the geographical bounds of a data set
   */
  private static final String GET_BOUNDS_QUERY = "SELECT"
      + " MIN(longitude), MIN(latitude), MAX(longitude), MAX(latitude)"
      + " FROM dataset_data WHERE dataset_id = ?";

  /**
   * Statement to store a sensor value
   */
  private static final String STORE_NEW_SENSOR_VALUE_STATEMENT = "INSERT INTO "
   + "sensor_values (dataset_id, file_column, date, value, "
   + "auto_qc, user_qc_flag, user_qc_message) "
   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_SENSOR_VALUE_STATEMENT = "UPDATE sensor_values "
    + "SET auto_qc=?, user_qc_flag=?, user_qc_message=? WHERE id = ?";

  /**
   * Statement to remove all sensor values for a data set
   */
  private static final String DELETE_SENSOR_VALUES_STATEMENT = "DELETE FROM "
    + "sensor_values WHERE dataset_id = ?";

  private static final String GET_SENSOR_VALUES_BY_COLUMN_QUERY = "SELECT "
    + "id, file_column, date, value, auto_qc, " // 5
    + "user_qc_flag, user_qc_message " // 7
    + "FROM sensor_values WHERE dataset_id = ? "
    + "ORDER BY file_column, date";

  /**
   * Query to get all the sensor values for a dataset
   */
  private static final String GET_SENSOR_VALUES_BY_DATE_AND_COLUMN_QUERY = "SELECT "
    + "id, file_column, date, value, auto_qc, " // 5
    + "user_qc_flag, user_qc_message " // 7
    + "FROM sensor_values WHERE dataset_id = ? "
    + "ORDER BY date, file_column";

  /**
   * Statement to store a measurement record
   */
  private static final String STORE_MEASUREMENT_STATEMENT = "INSERT INTO "
    + "measurements (dataset_id, variable_id, date, longitude, latitude, run_type) "
    + "VALUES (?, ?, ?, ?, ?, ?)";

  /**
   * Query to get all measurement records for a dataset
   */
  private static final String GET_MEASUREMENTS_QUERY = "SELECT "
    + "id, variable_id, date, longitude, latitude, run_type "
    + "FROM measurements WHERE dataset_id = ? "
    + "ORDER BY variable_id ASC, date ASC";

  /**
   * Statement to store a Measurement Value
   */
  private static final String STORE_MEASUREMENT_VALUE_STATEMENT = "INSERT INTO "
    + "measurement_values (measurement_id, variable_id, sensor_value_id) "
    + "VALUES (?, ?, ?)";

  /**
   * Statement to store a data reduction result
   */
  private static final String STORE_DATA_REDUCTION_STATEMENT = "INSERT INTO "
    + "data_reduction (measurement_id, variable_id, calculation_values, "
    + "qc_flag, qc_message) VALUES (?, ?, ?, ?, ?)";

  private static final String DELETE_DATA_REDUCTION_STATEMENT = "DELETE FROM "
    + "data_reduction WHERE measurement_id IN "
    + "(SELECT id FROM measurements WHERE dataset_id = ?)";

  private static final String DELETE_MEASUREMENT_VALUES_STATEMENT = "DELETE FROM "
    + "measurement_values WHERE measurement_id IN "
    + "(SELECT id FROM measurements WHERE dataset_id = ?)";

  private static final String DELETE_MEASUREMENTS_STATEMENT = "DELETE FROM "
    + "measurements WHERE dataset_id = ?";

  private static final String GET_SENSOR_VALUES_QUERY = "SELECT "
    + "id, file_column, date, value, "
    + "auto_qc, user_qc_flag, user_qc_message "
    + "FROM sensor_values "
    + "WHERE dataset_id = ? "
    + "ORDER BY date ASC";

  private static final String GET_USED_SENSOR_VALUES_QUERY = "SELECT DISTINCT "
    + "sensor_value_id FROM measurement_values "
    + "WHERE measurement_id IN "
    + "(SELECT id FROM measurements WHERE dataset_id = ?) "
    + "ORDER BY sensor_value_id";

  private static final String GET_DATA_REDUCTION_QUERY = "SELECT "
  + "m.date, dr.variable_id, dr.calculation_values, dr.qc_flag, dr.qc_message "
  + "FROM measurements m INNER JOIN data_reduction dr "
  + "ON (m.id = dr.measurement_id) "
  + "WHERE m.dataset_id = ? ORDER BY m.date ASC";

  private static final String SET_QC_STATEMENT = "UPDATE sensor_values SET "
    + "user_qc_flag = ?, user_qc_message = ? "
    + "WHERE id = ?";

  /**
   * Get the IDs of all the measurements for a given data set
   * @param dataSource A data source
   * @param datasetId The dataset ID
   * @return The measurement IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If no measurements are found
   */
  public static List<Long> getMeasurementIds(DataSource dataSource, long datasetId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    List<Long> result = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getMeasurementIds(conn, datasetId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement IDs", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the IDs of all the measurements for a given data set
   * @param conn A database connection
   * @param datasetId The dataset ID
   * @return The measurement IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If no measurements are found
   */
  public static List<Long> getMeasurementIds(Connection conn, long datasetId) throws MissingParamException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    List<Long> ids = new ArrayList<Long>();

    try {
      stmt = conn.prepareStatement(GET_ALL_MEASUREMENT_IDS_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();

      while(records.next()) {
        ids.add(records.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement IDs", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    if (ids.size() == 0) {
      throw new RecordNotFoundException("No records found for dataset " + datasetId);
    }

    return ids;
  }

  /**
   * Get the list of columns names for a raw dataset record
   * @param dataSource A data source
   * @param dataSet The data set
   * @return The column names
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
     * @throws InstrumentException If the instrument details cannot be retrieved
     * @throws RecordNotFoundException If the instrument for the data set does not exist
   */
  public static List<String> getDatasetDataColumnNames(DataSource dataSource,
    DataSet dataSet) throws MissingParamException, DatabaseException,
    InstrumentException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");

    List<String> result = new ArrayList<String>();

    SensorsConfiguration sensorConfig =
      ResourceManager.getInstance().getSensorsConfiguration();
    Connection conn = null;
    ResultSet columns = null;

    try {
      conn = dataSource.getConnection();

      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn,
        dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(),
        resourceManager.getRunTypeCategoryConfiguration());

      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      DatabaseMetaData metadata = conn.getMetaData();
      columns = metadata.getColumns(null, null, "dataset_data", null);

      while (columns.next()) {
        String columnName = columns.getString(4);

        switch (columnName) {
        case "id": {
          result.add("ID");
          break;
        }
        case "date": {
          result.add("Date");
          break;
        }
        case "longitude": {
          result.add("Longitude");
          break;
        }
        case "latitude": {
          result.add("Latitude");
          break;
        }
        case "dataset_id":
        case "run_type": {
          // Ignored
          break;
        }
        default: {
          // Sensor value columns
          for (SensorType sensorType : sensorConfig.getSensorTypes()) {
            if (sensorAssignments.getAssignmentCount(sensorType) > 0) {
              if (columnName.equals(sensorType.getDatabaseFieldName())) {
                result.add(sensorType.getName());
                break;
              }
            }
          }

          break;
        }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting column names", e);
    } finally {
      DatabaseUtils.closeResultSets(columns);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  public static void populateVariableList(DataSource dataSource, DataSet dataSet,
    VariableList variables) throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");
    MissingParam.checkMissing(variables, "variables", true);

    SensorsConfiguration sensorConfig =
      ResourceManager.getInstance().getSensorsConfiguration();
    Connection conn = null;
    ResultSet columns = null;

    try {
      conn = dataSource.getConnection();

      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn,
        dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(),
        resourceManager.getRunTypeCategoryConfiguration());

      SensorAssignments sensorAssignments = instrument.getSensorAssignments();

      DatabaseMetaData metadata = conn.getMetaData();
      columns = metadata.getColumns(null, null, "dataset_data", null);

      while (columns.next()) {
        String columnName = columns.getString(4);

        switch (columnName) {
        case "id": {
          // Ignored
          break;
        }
        case "date": {
          variables.addVariable("Date/Time",
            new Variable(Variable.TYPE_BASE, "Date/Time", "date"));
          break;
        }
        case "longitude": {
          variables.addVariable("Longitude",
            new Variable(Variable.TYPE_BASE, "Longitude", "longitude"));
          break;
        }
        case "latitude": {
          variables.addVariable("Latitude",
            new Variable(Variable.TYPE_BASE, "Latitude", "latitude"));
          break;
        }
        case "dataset_id":
        case "run_type": {
          // Ignored
          break;
        }
        default: {
          // Sensor value columns
          for (SensorType sensorType : sensorConfig.getSensorTypes()) {
            if (sensorAssignments.getAssignmentCount(sensorType) > 0) {
              if (columnName.equals(sensorType.getDatabaseFieldName())) {
                // TODO Eventually this will use the sensor name as the label, and the sensor type as the group
                variables.addVariable(sensorType.getGroup(),
                  new Variable(Variable.TYPE_SENSOR, sensorType.getName(), columnName));
                break;
              }
            }
          }

          break;
        }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting column names", e);
    } finally {
      DatabaseUtils.closeResultSets(columns);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Take a list of fields, and return those which come from the dataset data.
   * Any others will come from calculation data and will be left alone.
   * @param conn A database connection
   * @param dataSet The data set to which the fields belong
   * @param originalFields The list of fields
   * @return The fields that come from dataset data
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the dataset or its instrument do not exist
   * @throws InstrumentException If the instrument details cannot be retrieved
   */
  public static List<String> extractDatasetFields(Connection conn, DataSet dataSet,
    List<String> originalFields) throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    List<String> datasetFields = new ArrayList<String>();

    ResourceManager resourceManager = ResourceManager.getInstance();
    SensorsConfiguration sensorConfig = resourceManager.getSensorsConfiguration();

    Instrument instrument = InstrumentDB.getInstrument(conn,
      dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(),
      resourceManager.getRunTypeCategoryConfiguration());

    SensorAssignments sensorAssignments = instrument.getSensorAssignments();

    for (String originalField : originalFields) {

      switch (originalField) {
      case "id":
      case "date":
      case "longitude":
      case "latitude": {
        datasetFields.add(originalField);
        break;
      }
      default: {
        // Sensor value columns
        for (SensorType sensorType : sensorConfig.getSensorTypes()) {
          if (sensorAssignments.getAssignmentCount(sensorType) > 0) {
            if (originalField.equals(sensorType.getDatabaseFieldName())) {
              // TODO Eventually this will use the sensor name as the label, and the sensor type as the group
              datasetFields.add(originalField);
              break;
            }
          }
        }

        break;
      }
      }
    }

    return datasetFields;
  }

  /**
   * Determine whether or not a given field is a dataset-level field
   * @param conn A database connection
   * @param dataset The dataset to which the field belongs
   * @param field The field name
   * @return {@code true} if the field is a dataset field; {@code false} if it is not
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the dataset or its instrument do not exist
   * @throws InstrumentException If the instrument details cannot be retrieved
   */
  public static boolean isDatasetField(Connection conn, DataSet dataset,
    String field) throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    List<String> fieldList = new ArrayList<String>(1);
    fieldList.add(field);

    List<String> detectedDatasetField =
      extractDatasetFields(conn, dataset, fieldList);

    return (detectedDatasetField.size() > 0);
  }

  /**
   * Get the geographical bounds of a data set
   * @param dataSource A data source
   * @param dataset The dataset
   * @return The bounds
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<Double> getDataBounds(DataSource dataSource, DataSet dataset)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataset, "dataset");

    List<Double> result = null;

    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      result = getDataBounds(conn, dataset);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting dataset bounds", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the geographical bounds of a data set
   * @param dataSource A data source
   * @param dataset The dataset
   * @return The bounds
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static List<Double> getDataBounds(Connection conn, DataSet dataset)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    List<Double> result = new ArrayList<Double>(6);

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_BOUNDS_QUERY);
      stmt.setLong(1, dataset.getId());

      records = stmt.executeQuery();

      records.next();
      result.add(records.getDouble(1)); // West
      result.add(records.getDouble(2)); // South
      result.add(records.getDouble(3)); // East
      result.add(records.getDouble(4)); // North

      // Mid point
      result.add((records.getDouble(3) - records.getDouble(1)) / 2 +
        records.getDouble(1));
      result.add((records.getDouble(4) - records.getDouble(2)) / 2 +
        records.getDouble(2));

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting dataset bounds", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Store a set of sensor values in the database.
   *
   * Values will only be stored if their {@code dirty} flag is set.
   *
   * If a sensor value has a database ID, it will be updated. Otherwise
   * it will be stored as a new record. Note that the new records will not
   * be given an ID; they must be re-read from the database afterwards.
   *
   * @param conn A database connection
   * @param sensorValues The sensor values
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void storeSensorValues(Connection conn,
    Collection<SensorValue> sensorValues)
      throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(sensorValues, "sensorValues");

    PreparedStatement addStmt = null;
    PreparedStatement updateStmt = null;

    try {
      addStmt = conn.prepareStatement(STORE_NEW_SENSOR_VALUE_STATEMENT);
      updateStmt = conn.prepareStatement(UPDATE_SENSOR_VALUE_STATEMENT);

      for (SensorValue value : sensorValues) {
        if (value.isDirty()) {

          if (!value.isInDatabase()) {
            addStmt.setLong(1, value.getDatasetId());
            addStmt.setLong(2, value.getColumnId());
            addStmt.setLong(3, DateTimeUtils.dateToLong(value.getTime()));
            if (null == value.getValue()) {
              addStmt.setNull(4, Types.VARCHAR);
            } else {
              addStmt.setString(4, value.getValue());
            }

            addStmt.setString(5, value.getAutoQcResult().toJson());
            addStmt.setInt(6, value.getUserQCFlag().getFlagValue());
            addStmt.setString(7, value.getUserQCMessage());

            addStmt.addBatch();
          } else {
            updateStmt.setString(1, value.getAutoQcResult().toJson());
            updateStmt.setInt(2, value.getUserQCFlag().getFlagValue());
            updateStmt.setString(3, value.getUserQCMessage());
            updateStmt.setLong(4, value.getId());

            updateStmt.addBatch();
          }
        }
      }

      addStmt.executeBatch();
      updateStmt.executeBatch();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing sensor values", e);
    } finally {
      DatabaseUtils.closeStatements(addStmt, updateStmt);
    }

    // Clear the dirty flag on all the sensor values
    SensorValue.clearDirtyFlag(sensorValues);
  }

  /**
   * Remove all sensor values for a dataset
   * @param conn A database connection
   * @param datasetId The dataset's database ID
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void deleteSensorValues(Connection conn, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    PreparedStatement stmt = null;

    try {
      stmt = conn.prepareStatement(DELETE_SENSOR_VALUES_STATEMENT);

      stmt.setLong(1, datasetId);
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing sensor values", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Get all the sensor values for a dataset grouped by their column in the
   * source data file(s)
   * @param conn A database connection
   * @param datasetId The database ID of the dataset whose values are to be
   *                  retrieved
   * @return The values
   * @throws RecordNotFoundException If the instrument configuration does not
   *                                 match the values
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static Map<Long, NavigableSensorValuesList> getSensorValuesByColumn(
    Connection conn, long datasetId)
      throws RecordNotFoundException, DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    Map<Long, NavigableSensorValuesList> values = new HashMap<Long, NavigableSensorValuesList>();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      stmt = conn.prepareStatement(GET_SENSOR_VALUES_BY_COLUMN_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();

      long currentColumnId = -1;
      NavigableSensorValuesList currentSensorValues = new NavigableSensorValuesList();

      while (records.next()) {
        SensorValue sensorValue = sensorValueFromResultSet(records, datasetId);

        if (sensorValue.getColumnId() != currentColumnId) {
          if (currentColumnId != -1) {
            values.put(currentColumnId, currentSensorValues);
          }

          currentColumnId = sensorValue.getColumnId();
          currentSensorValues = new NavigableSensorValuesList();
        }

        currentSensorValues.add(sensorValue);
      }

      if (currentColumnId == -1) {
        throw new RecordNotFoundException(
          "No sensor values found for dataset " + datasetId);
      } else {
        values.put(currentColumnId, currentSensorValues);
      }

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving sensor values", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return values;
  }

  /**
   * Get all the sensor values for a data set
   * The returned list is ordered by timestamp and then grouped by
   * the values' source {@code file_column} database records
   * @param conn A database connection
   * @param datasetId The dataset whose values are to be retrieved
   * @return The values
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static DateColumnGroupedSensorValues getSensorValuesByDateAndColumn(
    Connection conn, Instrument instrument, long datasetId)
      throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    DateColumnGroupedSensorValues result =
      new DateColumnGroupedSensorValues(instrument);

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_SENSOR_VALUES_BY_DATE_AND_COLUMN_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();
      while (records.next()) {
        result.add(sensorValueFromResultSet(records, datasetId));
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving sensor values", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Build a SensorValue object from a ResultSet
   * @param record The ResultSet
   * @param datasetId The ID of the value's parent dataset
   * @return The SensorValue
   * @throws SQLException If any values cannot be read
   * @throws InvalidFlagException If the stored Flag value is invalid
   */
  private static SensorValue sensorValueFromResultSet(
    ResultSet record, long datasetId) throws SQLException, InvalidFlagException {

    long valueId = record.getLong(1);
    long fileColumnId = record.getLong(2);
    LocalDateTime time = DateTimeUtils.longToDate(record.getLong(3));
    String value = record.getString(4);
    AutoQCResult autoQC = AutoQCResult.buildFromJson(record.getString(5));
    Flag userQCFlag = new Flag(record.getInt(6));
    String userQCMessage = record.getString(7);

    return new SensorValue(valueId, datasetId, fileColumnId,
      time, value, autoQC, userQCFlag, userQCMessage);
  }

  /**
   * Store a set of measurements in the database. The resulting database
   * IDs are added to the Measurement objects
   *
   * @param conn A database connection
   * @param measurements The measurements to be stored
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void storeMeasurements(Connection conn,
    List<Measurement> measurements)
      throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurements, "measurements");

    PreparedStatement stmt = null;
    ResultSet createdKeys = null;

    try {

      stmt = conn.prepareStatement(STORE_MEASUREMENT_STATEMENT,
        Statement.RETURN_GENERATED_KEYS);

      // Batch up all the measurements
      for (Measurement measurement : measurements) {
        stmt.setLong(1, measurement.getDatasetId());
        stmt.setLong(2, measurement.getVariable().getId());
        stmt.setLong(3, DateTimeUtils.dateToLong(measurement.getTime()));
        stmt.setDouble(4, measurement.getLongitude());
        stmt.setDouble(5, measurement.getLatitude());
        stmt.setString(6, measurement.getRunType());
        stmt.addBatch();
      }

      // Store them, and get the keys back
      stmt.executeBatch();
      createdKeys = stmt.getGeneratedKeys();
      int currentMeasurement = -1;
      while (createdKeys.next()) {
        currentMeasurement++;
        measurements.get(currentMeasurement).setDatabaseId(createdKeys.getLong(1));
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while storing measurements", e);
    } finally {
      DatabaseUtils.closeResultSets(createdKeys);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Get the set of measurements for a dataset, ordered by date and variable
   * @param conn A database connection
   * @param instrument The instrument to which the dataset belongs
   * @param datasetId The database ID of the dataset
   * @return The measurements
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static List<Measurement> getMeasurements(Connection conn,
    Instrument instrument, long datasetId)
      throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    List<Measurement> measurements = new ArrayList<Measurement>();

    try {

      stmt = conn.prepareStatement(GET_MEASUREMENTS_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();
      while (records.next()) {
        long id = records.getLong(1);
        // We already have the dataset id
        long variableId = records.getLong(2);
        LocalDateTime time = DateTimeUtils.longToDate(records.getLong(3));
        double longitude = records.getDouble(4);
        double latitude = records.getDouble(5);
        String runType = records.getString(6);

        measurements.add(new Measurement(id, datasetId,
          instrument.getVariable(variableId), time, longitude, latitude, runType));
      }

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurements", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return measurements;
  }

  /**
   * Store a set of measurement values
   * @param conn A database connection
   * @param measurementValues The measurement values
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void storeMeasurementValues(Connection conn,
    List<MeasurementValue> measurementValues)
      throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurementValues, "measurementValues");

    PreparedStatement stmt = null;

    try {

      stmt = conn.prepareStatement(STORE_MEASUREMENT_VALUE_STATEMENT);

      // Batch up all the measurements
      for (MeasurementValue measurementValue : measurementValues) {
        stmt.setLong(1, measurementValue.getMeasurementId());
        stmt.setLong(2, measurementValue.getSensorValueId());
        stmt.addBatch();
      }

      // Store them, and get the keys back
      stmt.executeBatch();
    } catch (Exception e) {
      throw new DatabaseException("Error while storing measurements", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Store the results of data reduction in the database
   * @param conn A database connection
   * @param values The calculation values for the data reduction, as extracted
   *               from the sensor values
   * @param dataReductionRecords The data reduction calculations
   * @throws DatabaseException If the data cannot be stored
   */
  public static void storeDataReduction(Connection conn,
      Collection<CalculationValue> values,
      List<DataReductionRecord> dataReductionRecords) throws DatabaseException {

    PreparedStatement valueStmt = null;
    PreparedStatement dataReductionStmt = null;

    try {
      valueStmt = conn.prepareStatement(STORE_MEASUREMENT_VALUE_STATEMENT);

      // First the used sensor values
      for (CalculationValue value : values) {
        for (long id : value.getUsedSensorValueIds()) {
          valueStmt.setLong(1, value.getMeasurementId());
          valueStmt.setLong(2, value.getVariableId());
          valueStmt.setLong(3, id);

          valueStmt.addBatch();
        }
      }

      valueStmt.executeBatch();

      // And now the data reduction record
      dataReductionStmt = conn.prepareStatement(STORE_DATA_REDUCTION_STATEMENT);
      for (DataReductionRecord dataReduction : dataReductionRecords) {
        dataReductionStmt.setLong(1, dataReduction.getMeasurementId());
        dataReductionStmt.setLong(2, dataReduction.getVariableId());
        dataReductionStmt.setString(3, dataReduction.getCalculationJson());
        dataReductionStmt.setInt(4, dataReduction.getQCFlag().getFlagValue());
        dataReductionStmt.setString(5,
          StringUtils.collectionToDelimited(dataReduction.getQCMessages(), ";"));

        dataReductionStmt.addBatch();
      }

      dataReductionStmt.executeBatch();

    } catch (SQLException e) {
      throw new DatabaseException("Error while storing data reduction", e);
    } finally {
      DatabaseUtils.closeStatements(valueStmt);
    }
  }

  /**
   * Remove all measurement details from a data set, ready for them to be
   * recalculated
   * @param dataSource A data source
   * @param datasetId The database ID of the data set
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void deleteMeasurements(DataSource dataSource, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    Connection conn = null;
    PreparedStatement delDataReductionStmt = null;
    PreparedStatement delMeasurementValuesStmt = null;
    PreparedStatement delMeasurementsStmt = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      delDataReductionStmt = conn.prepareStatement(
        DELETE_DATA_REDUCTION_STATEMENT);
      delDataReductionStmt.setLong(1, datasetId);
      delDataReductionStmt.execute();

      delMeasurementValuesStmt = conn.prepareStatement(
        DELETE_MEASUREMENT_VALUES_STATEMENT);
      delMeasurementValuesStmt.setLong(1,  datasetId);
      delMeasurementValuesStmt.execute();

      delMeasurementsStmt = conn.prepareStatement(
        DELETE_MEASUREMENTS_STATEMENT);
      delMeasurementsStmt.setLong(1,  datasetId);
      delMeasurementsStmt.execute();

      conn.commit();
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting measurements", e);
    } finally {
      DatabaseUtils.closeStatements(delMeasurementsStmt,
        delMeasurementValuesStmt, delDataReductionStmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Load the data to be displayed in the QC table
   * @param dataSource A data source
   * @param datasetId The data set's database ID
   * @param sensorIDs The database IDs of the sensors to be retrieved
   * @return The table data
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws InvalidFlagException If a QC flag is invalid
   * @throws RoutineException If an automatic QC result cannot be processed
   * @throws SensorTypeNotFoundException
   * @throws RecordNotFoundException
   */
  public static void getQCSensorData(
    DataSource dataSource, DatasetMeasurementData tableData, long datasetId, Instrument instrument, List<Long> sensorIDs)
      throws MissingParamException, DatabaseException, InvalidFlagException,
        RoutineException, SensorTypeNotFoundException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(tableData, "tableData", true);
    MissingParam.checkPositive(datasetId, "datasetId");
    MissingParam.checkMissing(sensorIDs, "sensorIDs", false);

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();

      // Get the Run Type column IDs
      List<Long> runTypeColumns = instrument.getSensorAssignments().getRunTypeColumnIDs();

      // Get the list of sensor values used in the dataset
      long[] usedSensorValueIDs = getUsedSensorValueIDs(conn, datasetId);


      // Get all the sensor values for the data set
      stmt = conn.prepareStatement(GET_SENSOR_VALUES_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();

      // We collect together all the sensor values for a given date.
      // Then we check them all together and add them to the output
      String currentRunType = null;
      LocalDateTime currentTime = LocalDateTime.MIN;
      Map<Long, FieldValue> currentDateValues = new HashMap<Long, FieldValue>();

      while (records.next()) {
        LocalDateTime time = DateTimeUtils.longToDate(records.getLong(3));

        // If the time has changed, process the current set of collected values
        if (!time.isEqual(currentTime)) {
          if (!currentTime.isEqual(LocalDateTime.MIN)) {
            tableData.filterAndAddValues(currentRunType, currentTime, currentDateValues);

          }

          currentTime = time;
          currentDateValues = new HashMap<Long, FieldValue>();
        }


        // Process the current record

        // See if this is a Run Type
        long fileColumn = records.getLong(2);

        if (runTypeColumns.contains(fileColumn)) {
          currentRunType = records.getString(4);
        } else if (sensorIDs.contains(fileColumn)) {

          //This is a sensor value
          long valueId = records.getLong(1);
          Double sensorValue = StringUtils.doubleFromString(records.getString(4));
          AutoQCResult autoQC = AutoQCResult.buildFromJson(records.getString(5));
          Flag userQCFlag = new Flag(records.getInt(6));
          String qcComment = records.getString(7);

          SensorType sensorType = instrument.getSensorAssignments().getSensorTypeForDBColumn(fileColumn);

          // See if this value has been used in the data set
          // Position and diagnostics are always marked as used
          boolean used = false;
          if (fileColumn < 0 || sensorType.isDiagnostic()) {
            used = true;
          } else if (Arrays.binarySearch(usedSensorValueIDs, valueId) >= 0) {
            used = true;
          }

          FieldValue value = new FieldValue(valueId, sensorValue, autoQC,
            userQCFlag, qcComment, used);
          currentDateValues.put(fileColumn, value);
        }
      }

      // Store the last set of values
      tableData.filterAndAddValues(currentRunType, currentTime, currentDateValues);

    } catch (Exception e) {
      throw new DatabaseException("Error getting sensor data", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  public static void getDataReductionData(
    DataSource dataSource, DatasetMeasurementData tableData, DataSet dataSet)
      throws MissingParamException, DatabaseException, InvalidFlagException,
        RoutineException, VariableNotFoundException, DataReductionException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(tableData, "tableData", true);
    MissingParam.checkMissing(dataSet, "dataSet");

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;
    SensorsConfiguration sensorConfig =
      ResourceManager.getInstance().getSensorsConfiguration();

    try {
      conn = dataSource.getConnection();

      stmt = conn.prepareStatement(GET_DATA_REDUCTION_QUERY);
      stmt.setLong(1, dataSet.getId());
      records = stmt.executeQuery();

      while (records.next()) {

        LocalDateTime time = DateTimeUtils.longToDate(records.getLong(1));
        long variableId = records.getLong(2);
        String valuesJson = records.getString(3);
        Flag qcFlag = new Flag(records.getInt(4));
        String qcComment = records.getString(5);

        Type mapType = new TypeToken<HashMap<String, Double>>() {}.getType();
        Map<String, Double> values = new Gson().fromJson(valuesJson, mapType);

        LinkedHashMap<String, Long> reductionParameters =
          DataReducerFactory.getCalculationParameters(
            sensorConfig.getInstrumentVariable(variableId));

        for (Map.Entry<String, Long> entry : reductionParameters.entrySet()) {

          FieldValue columnValue = new FieldValue(entry.getValue(),
            values.get(entry.getKey()),
            new AutoQCResult(), qcFlag, qcComment, true);

          tableData.addValue(time,reductionParameters.get(entry.getKey()),
            columnValue);
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error getting sensor data", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  private static long[] getUsedSensorValueIDs(Connection conn, long datasetId) throws DatabaseException {

    ArrayList<Long> ids = new ArrayList<Long>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      stmt = conn.prepareStatement(GET_USED_SENSOR_VALUES_QUERY);
      stmt.setLong(1,  datasetId);

      records = stmt.executeQuery();
      while (records.next()) {
        ids.add(records.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting used sensor value IDS", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    long[] result = new long[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      result[i] = ids.get(i);
    }

    return result;
  }

  public static void setQC(DataSource dataSource, List<FieldValue> updateValues) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(updateValues, "updateValues");

    Connection conn = null;
    PreparedStatement stmt = null;

    try {

      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(SET_QC_STATEMENT);

      for (FieldValue value : updateValues) {
        stmt.setInt(1, value.getQcFlag().getFlagValue());
        stmt.setString(2, value.getQcComment());
        stmt.setLong(3, value.getValueId());

        stmt.addBatch();
      }

      stmt.executeBatch();

    } catch (SQLException e) {
      throw new DatabaseException("Error updating QC values", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

  }
}
