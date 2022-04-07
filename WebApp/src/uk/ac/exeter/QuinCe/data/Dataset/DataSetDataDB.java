
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Class for handling database queries related to dataset data
 *
 * @author Steve Jones
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

  private static final String GET_SENSOR_VALUES_FOR_DATASET_QUERY = "SELECT "
    + "id, file_column, date, value, auto_qc, " // 5
    + "user_qc_flag, user_qc_message " // 7
    + "FROM sensor_values WHERE dataset_id = ?";

  private static final String GET_SENSOR_VALUES_FOR_DATASET_NO_FLUSHING_QUERY = "SELECT "
    + "id, file_column, date, value, auto_qc, " // 5
    + "user_qc_flag, user_qc_message " // 7
    + "FROM sensor_values WHERE dataset_id = ? AND user_qc_flag != "
    + Flag.VALUE_FLUSHING;

  /**
   * Statement to store a measurement record
   */
  private static final String STORE_MEASUREMENT_STATEMENT = "INSERT INTO "
    + "measurements (dataset_id, date) " + "VALUES (?, ?)";

  private static final String STORE_RUN_TYPE_STATEMENT = "INSERT INTO "
    + "measurement_run_types (measurement_id, variable_id, run_type) "
    + "VALUES (?, ?, ?)";

  /**
   * Query to get all measurement records for a dataset
   */
  private static final String GET_MEASUREMENTS_QUERY = "SELECT "
    + "m.id, m.date, m.measurement_values, r.variable_id, r.run_type "
    + "FROM measurements m "
    + "LEFT JOIN measurement_run_types r ON r.measurement_id = m.id "
    + "WHERE dataset_id = ? " + "ORDER BY m.date ASC";

  private static final String GET_MEASUREMENT_TIMES_QUERY = "SELECT "
    + "id, date FROM measurements WHERE dataset_id = ? " + "AND run_type IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " ORDER BY date ASC";

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
    + "(SELECT id FROM measurements WHERE dataset_id = ?)";

  private static final String DELETE_MEASUREMENT_RUN_TYPES_STATEMENT = "DELETE FROM "
    + "measurement_run_types WHERE measurement_id IN "
    + "(SELECT id FROM measurements WHERE dataset_id = ?)";

  private static final String DELETE_MEASUREMENTS_STATEMENT = "DELETE FROM "
    + "measurements WHERE dataset_id = ?";

  private static final String GET_SENSOR_VALUES_FOR_COLUMNS_QUERY = "SELECT "
    + "id, file_column, date, value, auto_qc, " // 5
    + "user_qc_flag, user_qc_message " // 8
    + "FROM sensor_values WHERE dataset_id = ? AND file_column IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + "ORDER BY date";

  private static final String GET_SENSOR_VALUE_DATES_QUERY = "SELECT DISTINCT "
    + "date FROM sensor_values WHERE dataset_id = ? ORDER BY date ASC";

  private static final String GET_REQUIRED_FLAGS_QUERY = "SELECT "
    + "COUNT(*) FROM sensor_values WHERE dataset_id = ? "
    + "AND user_qc_flag = " + Flag.VALUE_NEEDED;

  private static final String GET_DATA_REDUCTION_QUERY = "SELECT "
    + "dr.measurement_id, dr.variable_id, dr.calculation_values, "
    + "dr.qc_flag, dr.qc_message FROM data_reduction dr INNER JOIN "
    + "measurements m ON dr.measurement_id = m.id WHERE m.dataset_id = ? "
    + "ORDER BY dr.measurement_id ASC";

  private static final String GET_RECORD_COUNT_QUERY = "SELECT "
    + "COUNT(DISTINCT(date)) FROM sensor_values WHERE dataset_id = ?";

  private static final String GET_RUN_TYPES_QUERY = "SELECT "
    + "date, value FROM sensor_values "
    + " WHERE dataset_id = ? AND file_column IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " ORDER BY date ASC";

  private static final String STORE_MEASUREMENT_VALUES_STATEMENT = "UPDATE measurements "
    + "SET measurement_values = ? WHERE id = ?";

  private static final String GET_INTERNAL_CALIBRATION_SENSOR_VALUES_QUERY = "SELECT "
    + "sv.id, sv.file_column, sv.date, sv.value, sv.auto_qc, "
    + "sv.user_qc_flag, sv.user_qc_message, mrt.run_type "
    + "FROM sensor_values sv "
    + "INNER JOIN measurements m ON m.date = sv.date "
    + "INNER JOIN measurement_run_types mrt ON m.id = mrt.measurement_id "
    + "WHERE m.dataset_id = ? AND mrt.run_type IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " AND sv.file_column IN "
    + DatabaseUtils.IN_PARAMS_TOKEN;

  /**
   * Store a set of sensor values in the database. Values will only be stored if
   * their {@code dirty} flag is set. If a sensor value has a database ID, it
   * will be updated. Otherwise it will be stored as a new record. Note that the
   * new records will not be given an ID; they must be re-read from the database
   * afterwards.
   *
   * @param conn
   *          A database connection.
   * @param sensorValues
   *          The sensor values.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws MissingParamException
   *           If any required parameters are missing.
   * @throws InstrumentException
   *           If the instrument details cannot be retrieved.
   * @throws InvalidSensorValueException
   *           If any sensor value contains invalid dataset/column IDs.
   */
  public static void storeSensorValues(Connection conn,
    Collection<SensorValue> sensorValues)
    throws MissingParamException, DatabaseException,
    InvalidSensorValueException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(sensorValues, "sensorValues", true);

    try (
      PreparedStatement addStmt = conn
        .prepareStatement(STORE_NEW_SENSOR_VALUE_STATEMENT);
      PreparedStatement updateStmt = conn
        .prepareStatement(UPDATE_SENSOR_VALUE_STATEMENT);) {

      // Set of dataset IDs/columns we know to be valid
      HashSet<DatasetColumn> verifiedColumns = new HashSet<DatasetColumn>();

      for (SensorValue value : sensorValues) {

        DatasetColumn dsCol = new DatasetColumn(value);

        if (!verifiedColumns.contains(dsCol)) {

          // Make sure the dataset exists
          DataSet dataSet;
          try {
            dataSet = DataSetDB.getDataSet(conn, value.getDatasetId());
          } catch (RecordNotFoundException e) {
            throw new InvalidSensorValueException(
              "Dataset specified in SensorValue does not exist", value);
          }

          // Make sure the column ID is valid for the dataset's instrument
          Instrument instrument = InstrumentDB.getInstrument(conn,
            dataSet.getInstrumentId());

          if (!instrument.columnValid(value.getColumnId())) {
            throw new InvalidSensorValueException(
              "Column specified in SensorValue is not valid for the instrument",
              value);
          }

          // If we got to here, we are good to go
          verifiedColumns.add(dsCol);
        }

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

            // Truncate user QC message
            String userQCMessage = value.getUserQCMessage();
            if (userQCMessage.length() > 255) {
              userQCMessage = userQCMessage.substring(0, 255);
            }

            updateStmt.setString(3, userQCMessage);
            updateStmt.setLong(4, value.getId());

            updateStmt.addBatch();
          }
        }
      }

      addStmt.executeBatch();
      updateStmt.executeBatch();

      // Clear the dirty flag on all the sensor values
      SensorValue.clearDirtyFlag(sensorValues);
    } catch (SQLException e) {
      throw new DatabaseException("Error storing sensor values", e);
    }
  }

  /**
   * Remove all sensor values for a dataset
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
    Instrument instrument, long datasetId, boolean ignoreFlushing,
    boolean ignoreInternalCalibrations) throws RecordNotFoundException,
    DatabaseException, MissingParamException, InvalidFlagException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    DatasetSensorValues values = new DatasetSensorValues(instrument);

    String query = ignoreFlushing
      ? GET_SENSOR_VALUES_FOR_DATASET_NO_FLUSHING_QUERY
      : GET_SENSOR_VALUES_FOR_DATASET_QUERY;

    try (PreparedStatement stmt = conn.prepareStatement(query)) {

      stmt.setLong(1, datasetId);

      try (ResultSet records = stmt.executeQuery()) {

        while (records.next()) {
          values.add(sensorValueFromResultSet(records, datasetId));
        }
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving sensor values", e);
    }

    if (instrument.hasInternalCalibrations() && ignoreInternalCalibrations) {
      values.removeAll(
        getInternalCalibrationSensorValues(conn, instrument, datasetId));
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
   */
  private static SensorValue sensorValueFromResultSet(ResultSet record,
    long datasetId) throws SQLException, InvalidFlagException {

    long valueId = record.getLong(1);
    long fileColumnId = record.getLong(2);
    LocalDateTime time = DateTimeUtils.longToDate(record.getLong(3));
    String value = record.getString(4);
    AutoQCResult autoQC = AutoQCResult.buildFromJson(record.getString(5));
    Flag userQCFlag = new Flag(record.getInt(6));
    String userQCMessage = record.getString(7);

    return new SensorValue(valueId, datasetId, fileColumnId, time, value,
      autoQC, userQCFlag, userQCMessage);
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
        measurementStmt.setLong(1, measurement.getDatasetId());
        measurementStmt.setLong(2,
          DateTimeUtils.dateToLong(measurement.getTime()));
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
   * Get the number of measurements in a dataset
   *
   * @param conn
   *          A database connection
   * @param datasetId
   *          The dataset's database ID
   * @return The number of records
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static int getRecordCount(Connection conn, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    int result = -1;

    PreparedStatement stmt = null;
    ResultSet count = null;

    try {
      stmt = conn.prepareStatement(GET_RECORD_COUNT_QUERY);
      stmt.setLong(1, datasetId);

      count = stmt.executeQuery();
      if (count.next()) {
        result = count.getInt(1);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement count", e);
    } finally {
      DatabaseUtils.closeResultSets(count);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
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
    Instrument instrument, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    DatasetMeasurements measurements = new DatasetMeasurements();

    try {

      stmt = conn.prepareStatement(GET_MEASUREMENTS_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();
      records.next();
      while (!records.isAfterLast()) {
        Measurement measurement = measurementFromResultSet(datasetId, records);
        measurements.addMeasurement(measurement);
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
    long datasetId) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    List<Measurement> measurements = new ArrayList<Measurement>();

    try {

      stmt = conn.prepareStatement(GET_MEASUREMENTS_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();
      records.next();
      while (!records.isAfterLast()) {
        measurements.add(measurementFromResultSet(datasetId, records));
      }

    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurements", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return measurements;
  }

  private static Measurement measurementFromResultSet(long datasetId,
    ResultSet record) throws SQLException {

    // Get the main measurement details
    long id = record.getLong(1);
    LocalDateTime time = DateTimeUtils.longToDate(record.getLong(2));
    String measurementValuesJson = record.getString(3);

    HashMap<Long, MeasurementValue> measurementValues = null == measurementValuesJson
      ? null
      : Measurement.gson.fromJson(measurementValuesJson,
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

    return new Measurement(id, datasetId, time, runTypes, measurementValues);
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
        dataReductionStmt.setInt(4, dataReduction.getQCFlag().getFlagValue());
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
          dataReductionStmt.setInt(1, dataReduction.getQCFlag().getFlagValue());
          dataReductionStmt.setString(2, StringUtils
            .collectionToDelimited(dataReduction.getQCMessages(), ";"));
          dataReductionStmt.setLong(3, dataReduction.getMeasurementId());
          dataReductionStmt.setLong(4, dataReduction.getVariableId());
        }

        dataReductionStmt.addBatch();
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
    MissingParam.checkZeroPositive(datasetId, "datasetId");

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
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    PreparedStatement delDataReductionStmt = null;
    PreparedStatement delMeasurementValuesStmt = null;
    PreparedStatement delRunTypesStmt = null;
    PreparedStatement delMeasurementsStmt = null;

    try {
      boolean initialAutoCommitState = conn.getAutoCommit();
      if (initialAutoCommitState) {
        conn.setAutoCommit(false);
      }

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

      conn.commit();

      if (initialAutoCommitState) {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting measurements", e);
    } finally {
      DatabaseUtils.closeStatements(delMeasurementsStmt, delRunTypesStmt,
        delMeasurementValuesStmt, delDataReductionStmt);
    }
  }

  /**
   * Get all the sensor values for the given columns that occur between the
   * specified dates (inclusive).
   *
   * @param conn
   *          A database connection
   * @param datasetId
   *          The dataset's database ID
   * @param columnIds
   *          The column IDs
   * @return The matching {@link SensorValue}s.
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static List<SensorValue> getSensorValuesForColumns(Connection conn,
    long datasetId, List<Long> columnIds)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(datasetId, "datasetId");
    MissingParam.checkMissing(columnIds, "columnIds", false);

    List<SensorValue> result = new ArrayList<SensorValue>();

    String sql = DatabaseUtils.makeInStatementSql(
      GET_SENSOR_VALUES_FOR_COLUMNS_QUERY, columnIds.size());
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, datasetId);
      for (int i = 0; i < columnIds.size(); i++) {
        stmt.setLong(i + 2, columnIds.get(i));
      }

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {
          result.add(sensorValueFromResultSet(records, datasetId));
        }
      }

    } catch (SQLException | InvalidFlagException e) {
      throw new DatabaseException("Error getting sensor values", e);
    }

    return result;
  }

  /**
   * Get the unique list of dates for which sensor values have been recorded for
   * a given dataset. This ignores any values recorded during flushing times.
   *
   * @param dataSource
   *          A data source
   * @param datasetId
   *          The dataset's database ID
   * @return The sensor value dates
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static List<LocalDateTime> getSensorValueTimes(DataSource dataSource,
    long datasetId) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "dataSetId");

    List<LocalDateTime> times = new ArrayList<LocalDateTime>();

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(GET_SENSOR_VALUE_DATES_QUERY);) {

      stmt.setLong(1, datasetId);

      try (ResultSet records = stmt.executeQuery();) {
        while (records.next()) {
          times.add(DateTimeUtils.longToDate(records.getLong(1)));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting sensor value dates", e);
    }

    return times;
  }

  public static int getFlagsRequired(DataSource dataSource, long datasetId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(datasetId, "dataSetId");

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

          Flag qcFlag = new Flag(records.getInt(4));
          String qcMessage = records.getString(5);

          ReadOnlyDataReductionRecord record = ReadOnlyDataReductionRecord
            .makeRecord(measurementId, variableId, calculationValues, qcFlag,
              qcMessage);

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

  public static Map<LocalDateTime, Long> getMeasurementTimes(Connection conn,
    long datasetId, List<String> runTypes)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(datasetId, "datasetId");
    MissingParam.checkMissing(runTypes, "runTypes", false);

    TreeMap<LocalDateTime, Long> result = new TreeMap<LocalDateTime, Long>();

    String sql = DatabaseUtils.makeInStatementSql(GET_MEASUREMENT_TIMES_QUERY,
      runTypes.size());

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, datasetId);

      for (int i = 0; i < runTypes.size(); i++) {
        stmt.setString(i + 2, runTypes.get(i));
      }

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {
          long id = records.getLong(1);
          LocalDateTime date = DateTimeUtils.longToDate(records.getLong(2));

          result.put(date, id);
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error getting measurement times", e);
    }

    return result;
  }

  public static RunTypePeriods getRunTypePeriods(Connection conn,
    Instrument instrument, long datasetId)
    throws MissingParamException, DatabaseException, DataSetException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    RunTypePeriods result = new RunTypePeriods();

    List<Long> runTypeColumnIds = instrument.getSensorAssignments()
      .getRunTypeColumnIDs();

    String sensorValuesSQL = DatabaseUtils
      .makeInStatementSql(GET_RUN_TYPES_QUERY, runTypeColumnIds.size());

    try (PreparedStatement stmt = conn.prepareStatement(sensorValuesSQL)) {

      stmt.setLong(1, datasetId);

      int currentParam = 2;
      for (long column : runTypeColumnIds) {
        stmt.setLong(currentParam, column);
        currentParam++;
      }

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {
          result.add(records.getString(2),
            DateTimeUtils.longToDate(records.getLong(1)));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting run type periods", e);
    }

    return result;
  }

  public static void storeMeasurementValues(Connection conn,
    Measurement measurement) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurement, "measurement");

    measurement.postProcessMeasurementValues();

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
        measurement.postProcessMeasurementValues();
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
    MissingParam.checkPositive(datasetId, "datasetId");

    try (PreparedStatement drStmt = conn
      .prepareStatement(DELETE_DATA_REDUCTION_STATEMENT);) {

      drStmt.setLong(1, datasetId);
      drStmt.execute();

    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting measurement values", e);
    }
  }

  public static List<RunTypeSensorValue> getInternalCalibrationSensorValues(
    Connection conn, Instrument instrument, long datasetId)
    throws MissingParamException, DatabaseException, InvalidFlagException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(instrument, "Instrument");
    MissingParam.checkPositive(datasetId, "datasetId");

    List<RunTypeSensorValue> result = new ArrayList<RunTypeSensorValue>();

    List<String> calibrationRunTypes = instrument
      .getInternalCalibrationRunTypes();
    List<Long> calibratedColumns = instrument.getSensorAssignments()
      .getInternalCalibrationSensors();

    String sql = DatabaseUtils.makeInStatementSql(
      GET_INTERNAL_CALIBRATION_SENSOR_VALUES_QUERY, calibrationRunTypes.size(),
      calibratedColumns.size());

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, datasetId);

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
            datasetId);
          result.add(new RunTypeSensorValue(sensorValue, records.getString(8)));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException(
        "Error while getting calibration sensor values", e);
    }

    return result;
  }

  /**
   * Get the times for which {@link SensorValue}s of a given {@link SensorType}
   * contain the specified value.
   * <p>
   * The times are returned as a {@link HashSet} for fast interrogation using
   * {@link Set#contains}. They are not ordered.
   * </p>
   *
   * @param conn
   *          A database connection
   * @param sensorType
   *          The required {@link SensorType}
   * @param value
   *          The value being searched for
   * @return The times of the {@link SensorValue}s that match the passed in
   *         value.
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static HashSet<LocalDateTime> getFilteredSensorValueTimes(
    Connection conn, Instrument instrument, DataSet dataset,
    SensorType sensorType, String value)
    throws MissingParamException, DatabaseException {

    List<SensorValue> allValues = getSensorValuesForColumns(conn,
      dataset.getId(),
      instrument.getSensorAssignments().getColumnIds(sensorType));

    return allValues.stream().filter(v -> v.getValue().equals(value))
      .map(v -> v.getTime()).collect(Collectors.toCollection(HashSet::new));
  }

  /**
   * Get the times for which {@link SensorValue}s of a given {@link SensorType}
   * contain the specified value.
   * <p>
   * The times are returned as a {@link HashSet} for fast interrogation using
   * {@link Set#contains}. They are not ordered.
   * </p>
   *
   * @param conn
   *          A database connection
   * @param sensorType
   *          The required {@link SensorType}
   * @param value
   *          The value being searched for
   * @return The times of the {@link SensorValue}s that match the passed in
   *         value.
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static HashSet<LocalDateTime> getFilteredSensorValueTimes(
    Connection conn, Instrument instrument, DataSet dataset,
    SensorType sensorType, double value)
    throws MissingParamException, DatabaseException {

    List<SensorValue> allValues = getSensorValuesForColumns(conn,
      dataset.getId(),
      instrument.getSensorAssignments().getColumnIds(sensorType));

    return allValues.stream().filter(v -> v.getDoubleValue().equals(value))
      .map(v -> v.getTime()).collect(Collectors.toCollection(HashSet::new));
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
