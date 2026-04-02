
package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.CalibrationFlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class for handling database queries related to dataset data
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
   * Statement to store a sensor value
   */
  private static final String STORE_NEW_SENSOR_VALUE_STATEMENT = "INSERT INTO "
    + "sensor_values (coordinate_id, file_column, value, "
    + "auto_qc, user_qc_flag, user_qc_message) VALUES (?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_SENSOR_VALUE_STATEMENT = "UPDATE sensor_values "
    + "SET auto_qc=?, user_qc_flag=?, user_qc_message=? WHERE id = ?";

  /**
   * Statement to remove all sensor values for a data set
   */
  private static final String DELETE_SENSOR_VALUES_STATEMENT = "DELETE FROM "
    + "sensor_values WHERE coordinate_id IN (SELECT id FROM coordinates WHERE dataset_id = ?)";

  /**
   * Statement to remove all coordinates for a data set
   */
  private static final String DELETE_COORDINATES_STATEMENT = "DELETE FROM "
    + "coordinates WHERE dataset_id = ?";

  private static final String GET_SENSOR_VALUES_FOR_DATASET_QUERY = "SELECT "
    + "sv.id, sv.coordinate_id, sv.file_column, sv.value, sv.auto_qc, "
    + "sv.user_qc_flag, sv.user_qc_message "
    + "FROM sensor_values sv INNER JOIN coordinates c ON sv.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? ORDER BY sv.id";

  private static final String GET_SENSOR_VALUES_FOR_DATASET_NO_FLUSHING_QUERY = "SELECT "
    + "sv.id, sv.coordinate_id, sv.file_column, sv.value, sv.auto_qc, "
    + "sv.user_qc_flag, sv.user_qc_message "
    + "FROM sensor_values sv INNER JOIN coordinates c ON sv.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? AND user_qc_flag != "
    + FlagScheme.FLUSHING_FLAG.getValue();

  private static final String GET_POSITION_SENSOR_VALUES_QUERY = "SELECT "
    + "sv.id, sv.coordinate_id, sv.file_column, sv.value, sv.auto_qc, sv.user_qc_flag, sv.user_qc_message "
    + "FROM sensor_values sv INNER JOIN coordinates c ON sv.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? AND sv.file_column IN (" + SensorType.LONGITUDE_ID
    + ", " + SensorType.LATITUDE_ID + ") ORDER BY sv.id";

  /**
   * Statement to store a measurement record
   */
  private static final String STORE_MEASUREMENT_STATEMENT = "INSERT INTO "
    + "measurements (coordinate_id) " + "VALUES (?)";

  private static final String STORE_RUN_TYPE_STATEMENT = "INSERT INTO "
    + "measurement_run_types (measurement_id, variable_id, run_type) "
    + "VALUES (?, ?, ?)";

  /**
   * Query to get all measurement records for a dataset
   */
  private static final String GET_MEASUREMENTS_QUERY = "SELECT "
    + "m.id, m.coordinate_id, m.measurement_values, r.variable_id, r.run_type "
    + "FROM measurements m LEFT JOIN measurement_run_types r ON r.measurement_id = m.id "
    + "INNER JOIN coordinates c ON m.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? " + "ORDER BY c.date ASC";

  /**
   * Statement to store a data reduction result
   */
  private static final String STORE_DATA_REDUCTION_STATEMENT = "INSERT INTO "
    + "data_reduction (measurement_id, variable_id, calculation_values, "
    + "qc_flag, qc_message) VALUES (?, ?, ?, ?, ?)";

  /**
   * Statement to update the QC info for a data reduction record
   */
  private static final String STORE_DATA_REDUCTION_QC_STATEMENT = "UPDATE "
    + "data_reduction SET qc_flag = ?, qc_message = ? WHERE "
    + "measurement_id = ? AND variable_id = ?";

  private static final String DELETE_DATA_REDUCTION_STATEMENT = "DELETE FROM "
    + "data_reduction WHERE measurement_id IN "
    + "(SELECT id FROM measurements WHERE coordinate_id IN "
    + "(SELECT id FROM coordinates WHERE dataset_id = ?))";

  private static final String DELETE_MEASUREMENT_RUN_TYPES_STATEMENT = "DELETE FROM "
    + "measurement_run_types WHERE measurement_id IN "
    + "(SELECT id FROM measurements WHERE coordinate_id IN "
    + "(SELECT id FROM coordinates WHERE dataset_id = ?))";

  private static final String DELETE_MEASUREMENTS_STATEMENT = "DELETE FROM "
    + "measurements WHERE coordinate_id IN "
    + "(SELECT id FROM coordinates WHERE dataset_id = ?)";

  private static final String GET_REQUIRED_FLAGS_QUERY = "SELECT "
    + "COUNT(*) FROM sensor_values sv INNER JOIN coordinates c ON sv.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? AND sv.user_qc_flag = '"
    + FlagScheme.NEEDED_FLAG.getValue() + "'";

  private static final String GET_DATA_REDUCTION_QUERY = "SELECT "
    + "dr.measurement_id, dr.variable_id, dr.calculation_values, "
    + "dr.qc_flag, dr.qc_message FROM data_reduction dr "
    + "INNER JOIN measurements m ON dr.measurement_id = m.id "
    + "INNER JOIN coordinates c ON m.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? ORDER BY dr.measurement_id ASC";

  private static final String GET_RUN_TYPES_QUERY = "SELECT "
    + "sv.coordinate_id, sv.value "
    + "FROM sensor_values sv INNER JOIN coordinates c ON sv.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? AND sv.file_column IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " ORDER BY date ASC";

  private static final String STORE_MEASUREMENT_VALUES_STATEMENT = "UPDATE measurements "
    + "SET measurement_values = ? WHERE id = ?";

  private static final String GET_INTERNAL_CALIBRATION_SENSOR_VALUES_QUERY = "SELECT "
    + "sv.id, sv.coordinate_id, sv.file_column, sv.value, sv.auto_qc, "
    + "sv.user_qc_flag, sv.user_qc_message, mrt.run_type "
    + "FROM sensor_values sv "
    + "INNER JOIN measurements m ON m.coordinate_id = sv.coordinate_id "
    + "INNER JOIN measurement_run_types mrt ON m.id = mrt.measurement_id "
    + "INNER JOIN coordinates c ON sv.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? AND mrt.run_type IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " AND sv.file_column IN "
    + DatabaseUtils.IN_PARAMS_TOKEN;

  private static final String GET_INTERNAL_CALIBRATION_SENSOR_VALUE_IDS_QUERY = "SELECT "
    + "sv.id FROM sensor_values sv "
    + "INNER JOIN measurements m ON m.coordinate_id = sv.coordinate_id "
    + "INNER JOIN measurement_run_types mrt ON m.id = mrt.measurement_id "
    + "INNER JOIN coordinates c ON sv.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? AND mrt.run_type IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " AND sv.file_column IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " ORDER BY sv.id";

  private static final String HAS_CALIBRATION_NEEDED_FLAGS_QUERY = "SELECT "
    + "COUNT(*) FROM data_reduction dr "
    + "INNER JOIN measurements m ON dr.measurement_id = m.id "
    + "INNER JOIN coordinates c ON m.coordinate_id = c.id "
    + "WHERE c.dataset_id = ? AND dr.qc_flag = ?";

  private static final String UPDATE_MEASUREMENT_COORDINATE_STATEMENT = "UPDATE "
    + "measurements SET coordinate_id = ? WHERE id = ?";

  /**
   * Take a list of fields, and return those which come from the dataset data.
   * Any others will come from calculation data and will be left alone.
   *
   * @param conn
   *          A database connection
   * @param dataSet
   *          The data set to which the fields belong
   * @param originalFields
   *          The list of fields
   * @return The fields that come from dataset data
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the dataset or its instrument do not exist
   * @throws InstrumentException
   *           If the instrument details cannot be retrieved
   * @throws SensorGroupsException
   * @throws ClassNotFoundException
   */
  public static List<String> extractDatasetFields(Connection conn,
    DataSet dataSet, List<String> originalFields)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException, SensorGroupsException, ClassNotFoundException {

    List<String> datasetFields = new ArrayList<String>();

    ResourceManager resourceManager = ResourceManager.getInstance();
    SensorsConfiguration sensorConfig = resourceManager
      .getSensorsConfiguration();

    Instrument instrument = InstrumentDB.getInstrument(conn,
      dataSet.getInstrumentId());

    SensorAssignments sensorAssignments = instrument.getSensorAssignments();

    for (String originalField : originalFields) {

      switch (originalField) {
      case "id":
      case "date": {
        datasetFields.add(originalField);
        break;
      }
      default: {
        // Sensor value columns
        for (SensorType sensorType : sensorConfig.getSensorTypes()) {
          if (sensorAssignments.getAssignmentCount(sensorType) > 0) {
            if (originalField.equals(sensorType.getDatabaseFieldName())) {
              // TODO Eventually this will use the sensor name as the label, and
              // the sensor type as the group
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
   *
   * @param conn
   *          A database connection
   * @param dataset
   *          The dataset to which the field belongs
   * @param field
   *          The field name
   * @return {@code true} if the field is a dataset field; {@code false} if it
   *         is not
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the dataset or its instrument do not exist
   * @throws InstrumentException
   *           If the instrument details cannot be retrieved
   * @throws SensorGroupsException
   * @throws ClassNotFoundException
   */
  public static boolean isDatasetField(Connection conn, DataSet dataset,
    String field)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException, SensorGroupsException, ClassNotFoundException {

    List<String> fieldList = new ArrayList<String>(1);
    fieldList.add(field);

    List<String> detectedDatasetField = extractDatasetFields(conn, dataset,
      fieldList);

    return (detectedDatasetField.size() > 0);
  }

  /**
   * Store a {@link Collection} of {@link SensorValue}s in the database.
   *
   * <p>
   * Values will only be stored if their {@code dirty} flag is set.
   * </p>
   *
   * <p>
   * The dirty values' {@link Coordinate}s will be extracted first. Any
   * {@link Coordinate} without a database ID will be stored, and its ID updated
   * with the generated key. After the coordinates have been stored, the
   * {@link SensorValue}s will be processed. If a {@link SensorValue} has a
   * database ID, it will be updated. Otherwise it will be stored as a new
   * record, and its ID set to the generated key.
   * </p>
   *
   * <p>
   * The passed in {@link Connection} must have {@code autoCommit == false}. The
   * calling method is responsible for managing the transaction.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param sensorValues
   *          The sensor values.
   * @throws Exception
   * @throws MissingParamException
   *           If any required parameters are missing.
   */
  public static void storeNewSensorValues(Connection conn,
    NewSensorValues sensorValues) throws Exception {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(sensorValues, "sensorValues");

    PreparedStatement addStmt = null;
    ResultSet generatedKeys;

    try {
      if (conn.getAutoCommit()) {
        throw new DatabaseException("Connection must be autoCommit=false");
      }

      // Extract the Coordinate objects and ensure they are stored
      // in the database.
      CoordinateDB.saveCoordinates(conn, sensorValues.getCoordinates());

      addStmt = conn.prepareStatement(STORE_NEW_SENSOR_VALUE_STATEMENT,
        Statement.RETURN_GENERATED_KEYS);

      DataSet dataSet = null;
      Instrument instrument = null;

      // Set of dataset IDs/columns we know to be valid
      HashSet<DatasetColumn> verifiedColumns = new HashSet<DatasetColumn>();

      for (SensorValue value : sensorValues.getSensorValues()) {
        if (!value.canBeSaved()) {
          throw new InvalidSensorValueException(
            "Attempt to store SensorValue but canBeSaved = false", value);
        }

        DatasetColumn dsCol = new DatasetColumn(value);

        if (!verifiedColumns.contains(dsCol)) {

          // Make sure the dataset exists
          if (null == dataSet || dataSet.getId() != value.getDatasetId()) {
            try {
              dataSet = DataSetDB.getDataSet(conn, value.getDatasetId());
            } catch (RecordNotFoundException e) {
              throw new InvalidSensorValueException(
                "Dataset specified in SensorValue does not exist", value);
            }
          }

          // Make sure the column ID is valid for the dataset's instrument
          if (null == instrument
            || instrument.getId() != dataSet.getInstrumentId()) {
            instrument = InstrumentDB.getInstrument(conn,
              dataSet.getInstrumentId());
          }

          if (!instrument.columnValid(value.getColumnId())) {
            throw new InvalidSensorValueException(
              "Column specified in SensorValue is not valid for the instrument",
              value);
          }

          // If we got to here, we are good to go
          verifiedColumns.add(dsCol);
        }

        if (value.isDirty()) {
          addStmt.setLong(1, value.getCoordinate().getId());
          addStmt.setLong(2, value.getColumnId());
          if (null == value.getValue()) {
            addStmt.setNull(3, Types.VARCHAR);
          } else {
            addStmt.setString(3, value.getValue());
          }

          addStmt.setString(4, value.getAutoQcResult().toJson());
          addStmt.setInt(5, value.getUserQCFlag().getValue());
          addStmt.setString(6, value.getUserQCMessage());

          addStmt.execute();

          generatedKeys = addStmt.getGeneratedKeys();
          if (generatedKeys.next()) {
            value.setId(generatedKeys.getLong(1));
          }
        }
      }

      DatabaseUtils.closeStatements(addStmt);
    } catch (SQLException e) {
      throw new DatabaseException("Error storing sensor values", e);
    } catch (Exception e) {
      DatabaseUtils.closeStatements(addStmt);
      throw e;
    }

    sensorValues.clearDirtyFlags();
  }

  /**
   * Store updated {@link SensorValue}s in the database.
   *
   * <p>
   * All the supplied {@link SensorValue}s must already be in the database (i.e.
   * have a database ID). If any of the values do not, an exception will be
   * thrown and none of the values will be stored.
   * </p>
   *
   * @param conn
   * @param sensorValues
   * @throws DatabaseException
   * @throws InvalidSensorValueException
   * @throws SensorGroupsException
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws ClassNotFoundException
   * @throws MissingParamException
   */
  public static void updateSensorValues(Connection conn,
    Collection<SensorValue> sensorValues) throws DatabaseException,
    InvalidSensorValueException, RecordNotFoundException, InstrumentException,
    SensorGroupsException, MissingParamException, ClassNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(sensorValues, "sensorValues", true);

    if (sensorValues.stream().anyMatch(sv -> !sv.isInDatabase())) {
      throw new DatabaseException("All sensor values must have database ID");
    }

    PreparedStatement updateStmt = null;

    try {
      if (conn.getAutoCommit()) {
        throw new DatabaseException("Connection must be autoCommit=false");
      }

      updateStmt = conn.prepareStatement(UPDATE_SENSOR_VALUE_STATEMENT);

      DataSet dataSet = null;
      Instrument instrument = null;

      // Set of dataset IDs/columns we know to be valid
      HashSet<DatasetColumn> verifiedColumns = new HashSet<DatasetColumn>();

      for (SensorValue value : sensorValues) {

        if (!value.canBeSaved()) {
          throw new InvalidSensorValueException(
            "Attempt to store SensorValue but canBeSaved = false", value);
        }

        DatasetColumn dsCol = new DatasetColumn(value);

        if (!verifiedColumns.contains(dsCol)) {

          // Make sure the dataset exists
          if (null == dataSet || dataSet.getId() != value.getDatasetId()) {
            try {
              dataSet = DataSetDB.getDataSet(conn, value.getDatasetId());
            } catch (RecordNotFoundException e) {
              throw new InvalidSensorValueException(
                "Dataset specified in SensorValue does not exist", value);
            }
          }

          // Make sure the column ID is valid for the dataset's instrument
          if (null == instrument
            || instrument.getId() != dataSet.getInstrumentId()) {
            instrument = InstrumentDB.getInstrument(conn,
              dataSet.getInstrumentId());
          }

          if (!instrument.columnValid(value.getColumnId())) {
            throw new InvalidSensorValueException(
              "Column specified in SensorValue is not valid for the instrument",
              value);
          }

          // If we got to here, we are good to go
          verifiedColumns.add(dsCol);
        }

        if (value.isDirty()) {
          updateStmt.setString(1, value.getAutoQcResult().toJson());
          updateStmt.setInt(2, value.getUserQCFlag().getValue());

          // Truncate user QC message (except for LOOKUP flags)
          String userQCMessage = value.getUserQCMessage();
          if (!value.getUserQCFlag().equals(FlagScheme.LOOKUP_FLAG)) {
            if (userQCMessage.length() > 255) {
              userQCMessage = userQCMessage.substring(0, 255);
            }
          }

          if (value.getUserQCFlag().isCommentRequired()
            && StringUtils.isBlank(value.getUserQCMessage())) {
            throw new InvalidSensorValueException(
              "User QC message cannot be empty with flag "
                + value.getUserQCFlag().getValue(),
              value);
          }

          updateStmt.setString(3, userQCMessage);
          updateStmt.setLong(4, value.getId());

          updateStmt.execute();
        }
      }

      // Clear the dirty flag on all the sensor values
      SensorValue.clearDirtyFlag(sensorValues);

    } catch (SQLException e) {
      throw new DatabaseException("Error updating sensor values", e);
    } finally {
      DatabaseUtils.closeStatements(updateStmt);
    }
  }

  /**
   * Remove all {@link SensorValue}s and {@link Coordinate}s for a
   * {@link DataSet}.
   *
   * @param conn
   *          A database connection
   * @param datasetId
   *          The dataset's database ID
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static void deleteSensorValues(Connection conn, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkDatabaseId(datasetId, "datasetId", false);

    PreparedStatement sensorValuesStmt = null;
    PreparedStatement coordinatesStmt = null;

    try {
      sensorValuesStmt = conn.prepareStatement(DELETE_SENSOR_VALUES_STATEMENT);
      sensorValuesStmt.setLong(1, datasetId);
      sensorValuesStmt.execute();

      coordinatesStmt = conn.prepareStatement(DELETE_COORDINATES_STATEMENT);
      coordinatesStmt.setLong(1, datasetId);
      coordinatesStmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing sensor values", e);
    } finally {
      DatabaseUtils.closeStatements(sensorValuesStmt, coordinatesStmt);
    }
  }

  /**
   * Get all the sensor values for a {@link DataSet} grouped by their column in
   * the source data file(s)
   *
   * @param conn
   *          A database connection
   * @param instrument
   *          The instrument to which the dataset belongs.
   * @param datasetId
   *          The database ID of the dataset whose values are to be retrieved
   * @param ignoreFlushing
   *          Indicates whether or not values in the instrument's flushing
   *          period should be left out of the result.
   * @return The values
   * @throws RecordNotFoundException
   *           If the instrument configuration does not match the values
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws InvalidFlagException
   */
  public static DatasetSensorValues getSensorValues(Connection conn,
    DataSet dataset, boolean ignoreFlushing, boolean ignoreInternalCalibrations)
    throws RecordNotFoundException, DatabaseException, MissingParamException,
    InvalidFlagException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    TreeSet<Long> ignoredSensorValues = new TreeSet<Long>();

    if (dataset.getInstrument().hasInternalCalibrations()
      && ignoreInternalCalibrations) {
      ignoredSensorValues = getInternalCalibrationSensorValueIDs(conn, dataset);
    }

    DatasetSensorValues values = new DatasetSensorValues(dataset);

    String query = ignoreFlushing
      ? GET_SENSOR_VALUES_FOR_DATASET_NO_FLUSHING_QUERY
      : GET_SENSOR_VALUES_FOR_DATASET_QUERY;

    try {
      Map<Long, Coordinate> coordinates = CoordinateDB
        .getSensorValueCoordinates(conn, dataset);

      try (PreparedStatement stmt = conn.prepareStatement(query)) {

        stmt.setLong(1, dataset.getId());

        try (ResultSet records = stmt.executeQuery()) {

          while (records.next()) {
            SensorValue value = sensorValueFromResultSet(records,
              dataset.getId(), dataset.getFlagScheme(), coordinates);
            if (!ignoredSensorValues.contains(value.getId())) {
              values.add(value);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving sensor values", e);
    }

    return values;
  }

  /**
   * Get all the sensor values for a {@link DataSet} as a simple collection.
   *
   * @param conn
   *          A database connection.
   * @param datasetId
   *          The {@link DataSet} id.
   * @return The sensor values.
   * @throws DatabaseException
   * @throws InvalidFlagException
   * @throws RecordNotFoundException
   */
  public static TreeSet<SensorValue> getRawSensorValues(Connection conn,
    DataSet dataset) throws DatabaseException, InvalidFlagException,
    CoordinateException, RecordNotFoundException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    TreeSet<SensorValue> sensorValues = new TreeSet<SensorValue>();

    try {
      Map<Long, Coordinate> coordinates = CoordinateDB
        .getSensorValueCoordinates(conn, dataset);

      try (PreparedStatement stmt = conn
        .prepareStatement(GET_SENSOR_VALUES_FOR_DATASET_QUERY)) {

        stmt.setLong(1, dataset.getId());

        try (ResultSet records = stmt.executeQuery()) {
          while (records.next()) {
            sensorValues.add(sensorValueFromResultSet(records, dataset.getId(),
              dataset.getFlagScheme(), coordinates));
          }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving sensor values", e);
    }

    return sensorValues;
  }

  public static DatasetSensorValues getPositionSensorValues(Connection conn,
    DataSet dataset) throws DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    DatasetSensorValues values = new DatasetSensorValues(dataset);

    try {
      Map<Long, Coordinate> coordinates = CoordinateDB
        .getSensorValueCoordinates(conn, dataset);

      try (PreparedStatement stmt = conn
        .prepareStatement(GET_POSITION_SENSOR_VALUES_QUERY)) {

        stmt.setLong(1, dataset.getId());
        try (ResultSet records = stmt.executeQuery()) {
          while (records.next()) {
            values.add(sensorValueFromResultSet(records, dataset.getId(),
              dataset.getFlagScheme(), coordinates));
          }
        }
      }
    } catch (

    Exception e) {
      throw new DatabaseException("Error while retrieving sensor values", e);
    }

    return values;
  }

  /**
   * Build a SensorValue object from a ResultSet
   *
   * @param record
   *          The ResultSet
   * @param datasetId
   *          The ID of the value's parent dataset
   * @return The SensorValue
   * @throws SQLException
   *           If any values cannot be read
   * @throws InvalidFlagException
   *           If the stored Flag value is invalid
   * @throws RecordNotFoundException
   */
  private static SensorValue sensorValueFromResultSet(ResultSet record,
    long datasetId, FlagScheme flagScheme, Map<Long, Coordinate> coordinates)
    throws SQLException, InvalidFlagException, RecordNotFoundException {

    long valueId = record.getLong(1);
    long coordinateId = record.getLong(2);
    long fileColumnId = record.getLong(3);
    String value = record.getString(4);
    AutoQCResult autoQC = AutoQCResult.buildFromJson(record.getString(5),
      flagScheme);
    Flag userQCFlag = flagScheme.getFlag(record.getInt(6));
    String userQCMessage = record.getString(7);

    if (null == coordinates.get(coordinateId)) {
      throw new RecordNotFoundException("Coordinate not loaded");
    }

    return new SensorValue(valueId, datasetId, flagScheme, fileColumnId,
      coordinates.get(coordinateId), value, autoQC, userQCFlag, userQCMessage);
  }

  /**
   * Store a set of measurements in the database. The resulting database IDs are
   * added to the Measurement objects
   *
   * @param conn
   *          A database connection
   * @param measurements
   *          The measurements to be stored
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static void storeMeasurements(Connection conn,
    Collection<Measurement> measurements)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurements, "measurements");

    try (
      PreparedStatement measurementStmt = conn.prepareStatement(
        STORE_MEASUREMENT_STATEMENT, Statement.RETURN_GENERATED_KEYS);

      PreparedStatement runTypeStmt = conn
        .prepareStatement(STORE_RUN_TYPE_STATEMENT);) {

      // Batch up all the measurements
      for (Measurement measurement : measurements) {
        measurementStmt.setLong(1, measurement.getCoordinate().getId());
        measurementStmt.execute();

        try (ResultSet createdKey = measurementStmt.getGeneratedKeys()) {
          if (!createdKey.next()) {
            throw new DatabaseException(
              "Did not get key from created measurement record");
          }

          measurement.setDatabaseId(createdKey.getLong(1));

          for (Map.Entry<Long, String> runTypeEntry : measurement.getRunTypes()
            .entrySet()) {
            runTypeStmt.setLong(1, createdKey.getLong(1));
            runTypeStmt.setLong(2, runTypeEntry.getKey());
            runTypeStmt.setString(3, runTypeEntry.getValue());

            runTypeStmt.execute();
          }
        }
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while storing measurements", e);
    }
  }

  /**
   * Get the set of measurements for a dataset, grouped by run type and ordered
   * by date
   *
   * @param conn
   *          A database connection
   * @param instrument
   *          The instrument to which the dataset belongs
   * @param datasetId
   *          The database ID of the dataset
   * @return The measurements
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static DatasetMeasurements getMeasurementsByRunType(Connection conn,
    DataSet dataset) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    PreparedStatement stmt = null;
    ResultSet records = null;

    DatasetMeasurements measurements = new DatasetMeasurements();

    try {
      Map<Long, Coordinate> coordinates = CoordinateDB
        .getMeasurementCoordinates(conn, dataset);

      stmt = conn.prepareStatement(GET_MEASUREMENTS_QUERY);
      stmt.setLong(1, dataset.getId());

      records = stmt.executeQuery();

      // There are potentially multiple records per measurement.
      // See measurementFromResultSet.
      records.next();
      while (!records.isAfterLast()) {
        measurements.addMeasurement(measurementFromResultSet(records,
          dataset.getId(), coordinates, dataset.getFlagScheme()));
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
   * Get the set of measurements for a dataset ordered by date
   *
   * @param conn
   *          A database connection
   * @param datasetId
   *          The database ID of the dataset
   * @return The measurements
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static List<Measurement> getMeasurements(Connection conn,
    DataSet dataset) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    PreparedStatement stmt = null;
    ResultSet records = null;

    List<Measurement> measurements = new ArrayList<Measurement>();

    try {
      Map<Long, Coordinate> coordinates = CoordinateDB
        .getMeasurementCoordinates(conn, dataset);

      stmt = conn.prepareStatement(GET_MEASUREMENTS_QUERY);
      stmt.setLong(1, dataset.getId());

      records = stmt.executeQuery();

      // There are potentially multiple records per measurement.
      // See measurementFromResultSet.
      records.next();
      while (!records.isAfterLast()) {
        measurements.add(measurementFromResultSet(records, dataset.getId(),
          coordinates, dataset.getFlagScheme()));
      }

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurements", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return measurements;
  }

  private static Measurement measurementFromResultSet(ResultSet record,
    long datasetId, Map<Long, Coordinate> coordinates, FlagScheme flagScheme)
    throws SQLException, RecordNotFoundException {

    // Get the main measurement details
    long id = record.getLong(1);
    Coordinate coordinate = coordinates.get(record.getLong(2));
    String measurementValuesJson = record.getString(3);

    Gson gson = new GsonBuilder()
      .registerTypeAdapter(new HashMap<Long, MeasurementValue>().getClass(),
        new MeasurementValuesSerializer(flagScheme))
      .create();

    HashMap<Long, MeasurementValue> measurementValues = null == measurementValuesJson
      ? null
      : gson.fromJson(measurementValuesJson,
        Measurement.MEASUREMENT_VALUES_TYPE);

    // Now extract run types
    Map<Long, String> runTypes = new HashMap<Long, String>();

    boolean extractRunType = true;

    while (extractRunType) {

      // Get the run type details. If the variable Id is null, there is no run
      // type
      long variableId = record.getLong(4);
      if (!record.wasNull()) {
        runTypes.put(variableId, record.getString(5));
      }

      record.next();
      if (record.isAfterLast() || record.getLong(1) != id) {
        // We've gone off the end of the result set, or we've found
        // a new measurement. Either way stop reading run types.
        extractRunType = false;
      }
    }

    if (null == coordinate) {
      throw new RecordNotFoundException("Coordinate not loaded");
    }

    return new Measurement(id, datasetId, coordinate, runTypes,
      measurementValues, flagScheme);
  }

  /**
   * Store the results of data reduction in the database
   *
   * @param conn
   *          A database connection
   * @param dataReductionRecords
   *          The data reduction calculations
   * @throws DatabaseException
   *           If the data cannot be stored
   */
  public static void storeDataReduction(Connection conn,
    Collection<DataReductionRecord> dataReductionRecords)
    throws DatabaseException {

    try (PreparedStatement dataReductionStmt = conn
      .prepareStatement(STORE_DATA_REDUCTION_STATEMENT)) {
      for (DataReductionRecord dataReduction : dataReductionRecords) {

        if (dataReduction instanceof ReadOnlyDataReductionRecord) {
          throw new DatabaseException(
            "Attempt to store a read-only DataReductionRecord");
        }

        dataReductionStmt.setLong(1, dataReduction.getMeasurementId());
        dataReductionStmt.setLong(2, dataReduction.getVariableId());
        dataReductionStmt.setString(3, dataReduction.getCalculationJson());
        dataReductionStmt.setInt(4, dataReduction.getQCFlag().getValue());
        dataReductionStmt.setString(5, StringUtils
          .collectionToDelimited(dataReduction.getQCMessages(), ";"));

        dataReductionStmt.addBatch();
      }

      dataReductionStmt.executeBatch();

    } catch (SQLException e) {
      throw new DatabaseException("Error while storing data reduction", e);
    }
  }

  /**
   * Store the results of data reduction in the database
   *
   * @param conn
   *          A database connection
   * @param values
   *          The calculation values for the data reduction, as extracted from
   *          the sensor values
   * @param dataReductionRecords
   *          The data reduction calculations
   * @throws DatabaseException
   *           If the data cannot be stored
   */
  public static void storeDataReductionQC(Connection conn,
    Collection<ReadOnlyDataReductionRecord> dataReductionRecords)
    throws DatabaseException {

    try (PreparedStatement dataReductionStmt = conn
      .prepareStatement(STORE_DATA_REDUCTION_QC_STATEMENT)) {
      for (ReadOnlyDataReductionRecord dataReduction : dataReductionRecords) {

        if (dataReduction.isDirty()) {
          dataReductionStmt.setInt(1, dataReduction.getQCFlag().getValue());
          dataReductionStmt.setString(2, StringUtils
            .collectionToDelimited(dataReduction.getQCMessages(), ";"));
          dataReductionStmt.setLong(3, dataReduction.getMeasurementId());
          dataReductionStmt.setLong(4, dataReduction.getVariableId());

          dataReductionStmt.addBatch();
        }
      }

      dataReductionStmt.executeBatch();

    } catch (SQLException e) {
      throw new DatabaseException("Error while storing data reduction", e);
    }
  }

  /**
   * Remove all measurement details from a data set, ready for them to be
   * recalculated
   *
   * @param dataSource
   *          A data source
   * @param datasetId
   *          The database ID of the data set
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static void deleteMeasurements(DataSource dataSource, long datasetId)
    throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkDatabaseId(datasetId, "datasetId", false);

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      deleteMeasurements(conn, datasetId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting measurements", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Remove all measurement details from a data set, ready for them to be
   * recalculated
   *
   * @param conn
   *          A database connection
   * @param datasetId
   *          The database ID of the data set
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static void deleteMeasurements(Connection conn, long datasetId)
    throws DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkDatabaseId(datasetId, "datasetId", false);

    PreparedStatement delDataReductionStmt = null;
    PreparedStatement delMeasurementValuesStmt = null;
    PreparedStatement delRunTypesStmt = null;
    PreparedStatement delMeasurementsStmt = null;

    try {
      delDataReductionStmt = conn
        .prepareStatement(DELETE_DATA_REDUCTION_STATEMENT);
      delDataReductionStmt.setLong(1, datasetId);
      delDataReductionStmt.execute();

      delRunTypesStmt = conn
        .prepareStatement(DELETE_MEASUREMENT_RUN_TYPES_STATEMENT);
      delRunTypesStmt.setLong(1, datasetId);
      delRunTypesStmt.execute();

      delMeasurementsStmt = conn
        .prepareStatement(DELETE_MEASUREMENTS_STATEMENT);
      delMeasurementsStmt.setLong(1, datasetId);
      delMeasurementsStmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting measurements", e);
    } finally {
      DatabaseUtils.closeStatements(delMeasurementsStmt, delRunTypesStmt,
        delMeasurementValuesStmt, delDataReductionStmt);
    }
  }

  public static int getFlagsRequired(DataSource dataSource, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkDatabaseId(datasetId, "dataSetId", false);

    int result = 0;

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(GET_REQUIRED_FLAGS_QUERY);) {

      stmt.setLong(1, datasetId);

      try (ResultSet records = stmt.executeQuery();) {
        records.next();
        result = records.getInt(1);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting flag info", e);
    }

    return result;
  }

  /**
   * Determine whether or not calibration is required for a dataset prior to
   * setting its status.
   *
   * Searches for any data reduction records with a QC flag of
   * {@link Flag#NOT_CALIBRATED}.
   *
   * @param dataSource
   *          A data source.
   * @param datasetId
   *          The dataset ID.
   * @return {@code true} if any NOT_CALIBRATED flags are set; {@code false}
   *         otherwise.
   * @throws DatabaseException
   *           If the check fails.
   */
  public static boolean hasCalibrationRequiredFlags(DataSource dataSource,
    CalibrationFlagScheme flagScheme, long datasetId) throws DatabaseException {

    boolean result = false;

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkDatabaseId(datasetId, "datasetId", false);

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(HAS_CALIBRATION_NEEDED_FLAGS_QUERY);) {

      stmt.setLong(1, datasetId);
      stmt.setInt(2, flagScheme.getNotCalibratedFlag().getValue());

      try (ResultSet records = stmt.executeQuery()) {
        records.next();
        result = records.getInt(1) > 0;
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error getting calibration flag info", e);
    }

    return result;
  }

  /**
   * Get the data reduction data for a dataset.
   * <p>
   * Returns a Map structure of Measurement ID -&gt; Variable -&gt;
   * DataReductionRecord.
   * <p>
   *
   * @param conn
   *          A database connection
   * @param instrument
   *          The instrument
   * @param dataSet
   *          The dataset
   * @return The data reduction data
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static Map<Long, Map<Variable, ReadOnlyDataReductionRecord>> getDataReductionData(
    Connection conn, Instrument instrument, DataSet dataSet)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkMissing(dataSet, "dataSet");

    Map<Long, Map<Variable, ReadOnlyDataReductionRecord>> result = new HashMap<Long, Map<Variable, ReadOnlyDataReductionRecord>>();

    try (PreparedStatement stmt = conn
      .prepareStatement(GET_DATA_REDUCTION_QUERY)) {

      stmt.setLong(1, dataSet.getId());

      long currentMeasurement = -1L;
      try (ResultSet records = stmt.executeQuery()) {

        while (records.next()) {

          long measurementId = records.getLong(1);
          long variableId = records.getLong(2);

          String calculationValuesJson = records.getString(3);
          Type mapType = new TypeToken<HashMap<String, Double>>() {
          }.getType();
          Map<String, Double> calculationValues = new Gson()
            .fromJson(calculationValuesJson, mapType);

          Flag qcFlag = instrument.getFlagScheme().getFlag(records.getInt(4));
          String qcMessage = records.getString(5);

          ReadOnlyDataReductionRecord record = ReadOnlyDataReductionRecord
            .makeRecord(measurementId, variableId, instrument.getFlagScheme(),
              calculationValues, qcFlag, qcMessage);

          if (measurementId != currentMeasurement) {
            result.put(measurementId,
              new HashMap<Variable, ReadOnlyDataReductionRecord>());
            currentMeasurement = measurementId;
          }

          result.get(currentMeasurement).put(instrument.getVariable(variableId),
            record);
        }
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving data reduction data",
        e);
    }

    return result;
  }

  public static RunTypePeriods getRunTypePeriods(Connection conn,
    DataSet dataset) throws MissingParamException, DatabaseException,
    DataSetException, CoordinateException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    RunTypePeriods result = new RunTypePeriods();

    List<Long> runTypeColumnIds = dataset.getInstrument().getSensorAssignments()
      .getRunTypeColumnIDs();

    if (runTypeColumnIds.size() > 0) {
      String sensorValuesSQL = DatabaseUtils
        .makeInStatementSql(GET_RUN_TYPES_QUERY, runTypeColumnIds.size());

      try {
        Map<Long, Coordinate> coordinates = CoordinateDB
          .getSensorValueCoordinates(conn, dataset);

        try (PreparedStatement stmt = conn.prepareStatement(sensorValuesSQL)) {

          stmt.setLong(1, dataset.getId());

          int currentParam = 2;
          for (long column : runTypeColumnIds) {
            stmt.setLong(currentParam, column);
            currentParam++;
          }

          try (ResultSet records = stmt.executeQuery()) {
            while (records.next()) {
              result.add(records.getString(2),
                coordinates.get(records.getLong(1)).getTime());
            }
          }
        }
      } catch (SQLException e) {
        throw new DatabaseException("Error while getting run type periods", e);
      }

    }

    return result;
  }

  public static void storeMeasurementValues(Connection conn,
    Measurement measurement) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurement, "measurement");

    try (PreparedStatement stmt = conn
      .prepareStatement(STORE_MEASUREMENT_VALUES_STATEMENT)) {
      stmt.setString(1, measurement.getMeasurementValuesJson());
      stmt.setLong(2, measurement.getId());
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing measurement values", e);
    }
  }

  public static void storeMeasurementValues(Connection conn,
    Collection<Measurement> measurements)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurements, "measurements", true);

    try (PreparedStatement stmt = conn
      .prepareStatement(STORE_MEASUREMENT_VALUES_STATEMENT)) {

      for (Measurement measurement : measurements) {
        stmt.setString(1, measurement.getMeasurementValuesJson());
        stmt.setLong(2, measurement.getId());
        stmt.addBatch();
      }

      stmt.executeBatch();
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing measurement values", e);
    }
  }

  public static void deleteDataReduction(Connection conn, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkDatabaseId(datasetId, "datasetId", false);

    try (PreparedStatement drStmt = conn
      .prepareStatement(DELETE_DATA_REDUCTION_STATEMENT);) {

      drStmt.setLong(1, datasetId);
      drStmt.execute();

    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting measurement values", e);
    }
  }

  public static TreeSet<Long> getInternalCalibrationSensorValueIDs(
    Connection conn, DataSet dataset) throws DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    TreeSet<Long> result = new TreeSet<Long>();

    List<String> calibrationRunTypes = dataset.getInstrument()
      .getInternalCalibrationRunTypes();
    List<Long> calibratedColumns = dataset.getInstrument()
      .getSensorAssignments().getInternalCalibrationSensors();

    String sql = DatabaseUtils.makeInStatementSql(
      GET_INTERNAL_CALIBRATION_SENSOR_VALUE_IDS_QUERY,
      calibrationRunTypes.size(), calibratedColumns.size());

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, dataset.getId());
      int currentParam = 1;

      for (String runType : calibrationRunTypes) {
        currentParam++;
        stmt.setString(currentParam, runType);
      }

      for (Long column : calibratedColumns) {
        currentParam++;
        stmt.setLong(currentParam, column);
      }

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {
          result.add(records.getLong(1));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException(
        "Error while getting calibration sensor values", e);
    }

    return result;
  }

  public static List<RunTypeSensorValue> getInternalCalibrationSensorValues(
    Connection conn, DataSet dataset) throws DatabaseException,
    InvalidFlagException, CoordinateException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataset, "dataset");

    List<RunTypeSensorValue> result = new ArrayList<RunTypeSensorValue>();

    List<String> calibrationRunTypes = dataset.getInstrument()
      .getInternalCalibrationRunTypes();
    List<Long> calibratedColumns = dataset.getInstrument()
      .getSensorAssignments().getInternalCalibrationSensors();

    String sql = DatabaseUtils.makeInStatementSql(
      GET_INTERNAL_CALIBRATION_SENSOR_VALUES_QUERY, calibrationRunTypes.size(),
      calibratedColumns.size());

    try {
      Map<Long, Coordinate> coordinates = CoordinateDB
        .getSensorValueCoordinates(conn, dataset);

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setLong(1, dataset.getId());
        int currentParam = 1;

        for (String runType : calibrationRunTypes) {
          currentParam++;
          stmt.setString(currentParam, runType);
        }

        for (Long column : calibratedColumns) {
          currentParam++;
          stmt.setLong(currentParam, column);
        }

        try (ResultSet records = stmt.executeQuery()) {
          while (records.next()) {
            SensorValue sensorValue = sensorValueFromResultSet(records,
              dataset.getId(), dataset.getFlagScheme(), coordinates);
            result
              .add(new RunTypeSensorValue(sensorValue, records.getString(8)));
          }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException(
        "Error while getting calibration sensor values", e);
    }

    return result;
  }

  /**
   * Update the {@link Coordinate} for a {@link Measurement} in the database.
   *
   * <p>
   * If the {@link Coordinate} is not yet in the database, it will be added and
   * have its ID updated.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param measurement
   *          The measurement whose coordinate is to be updated.
   * @throws DatabaseException
   * @throws CoordinateException
   * @throws RecordNotFoundException
   */
  public static void updateMeasurementCoordinate(Connection conn,
    Measurement measurement)
    throws CoordinateException, DatabaseException, RecordNotFoundException {

    try {
      if (!measurement.getCoordinate().isInDatabase()) {
        CoordinateDB.saveCoordinate(conn, measurement.getCoordinate());
      }

      try (PreparedStatement stmt = conn
        .prepareStatement(UPDATE_MEASUREMENT_COORDINATE_STATEMENT)) {
        stmt.setLong(1, measurement.getCoordinate().getId());
        stmt.setLong(2, measurement.getId());

        stmt.execute();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error updating measurement coordinate", e);
    }
  }
}

class DatasetColumn {

  private long datasetId;

  private long columnId;

  protected DatasetColumn(SensorValue sensorValue) {
    this.datasetId = sensorValue.getDatasetId();
    this.columnId = sensorValue.getColumnId();
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnId, datasetId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DatasetColumn other = (DatasetColumn) obj;
    return columnId == other.columnId && datasetId == other.datasetId;
  }
}
