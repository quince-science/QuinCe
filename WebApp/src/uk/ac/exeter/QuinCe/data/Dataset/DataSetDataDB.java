package uk.ac.exeter.QuinCe.data.Dataset;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
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
 * Class for handling database queries related to the
 * {@code dataset_data} table
 * @author Steve Jones
 *
 */
public class DataSetDataDB {

  /**
   * Query to get all measurements for a data set
   */
  private static final String GET_ALL_MEASUREMENTS_QUERY = "SELECT * FROM dataset_data WHERE dataset_id = ? ORDER BY date ASC";

  /**
   * Query to get all measurements IDs for a data set
   */
  private static final String GET_ALL_MEASUREMENT_IDS_QUERY = "SELECT id FROM dataset_data WHERE dataset_id = ? ORDER BY date ASC";

  /**
   * Query to get a single measurement
   */
  private static final String GET_MEASUREMENT_QUERY = "SELECT * FROM dataset_data WHERE id = ?";

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
    + "measurement_values (measurement_id, sensor_value_id) "
    + "VALUES (?, ?)";

  /**
   * Query to get all sensor values related to all measurements
   * in a data set
   */
  private static final String GET_MEASUREMENT_SENSOR_VALUES_QUERY = "SELECT "
    + "m.id, sv.id, sv.file_column, sv.date, sv.value, " // 5
    + "sv.auto_qc, sv.user_qc_flag, sv.user_qc_message " // 8
    + "FROM measurements m "
    + "INNER JOIN measurement_values mv ON m.id = mv.measurement_id "
    + "INNER JOIN sensor_values sv ON mv.sensor_value_id = sv.id "
    + "WHERE m.dataset_id = ? ORDER BY sv.date ASC";

  /**
   * The name of the ID column
   */
  private static final String ID_COL = "id";

  /**
   * The name of the date column
   */
  private static final String DATE_COL = "date";

  /**
   * The name of the longitude column
   */
  private static final String LON_COL = "longitude";

  /**
   * The name of the latitude column
   */
  private static final String LAT_COL = "latitude";

  /**
   * The name of the run type column
   */
  private static final String RUN_TYPE_COL = "run_type";

  /**
   * The name of the dataset ID column
   */
  private static final String DATASET_COL = "dataset_id";

  /**
   * Store a data set record in the database.
   *
   * Measurement and calibration records are automatically detected
   * and stored in the appropriate table.
   *
   * @param conn A database connection
   * @param record The record to be stored
   * @param datasetDataStatement A previously generated statement for inserting a record. Can be null.
   * @return A {@link PreparedStatement} that can be used for storing subsequent records
   * @throws MissingParamException If any required parameters are missing
   * @throws DataSetException If a non-measurement record is supplied
   * @throws DatabaseException If a database error occurs
   * @throws NoSuchCategoryException If the record's Run Type is not recognised
   * @throws VariableNotFoundException
   * @throws SensorConfigurationException
   */
  public static void storeRecord(Connection conn, DataSetRawDataRecord record)
    throws MissingParamException, DataSetException, DatabaseException,
      NoSuchCategoryException, SensorConfigurationException, VariableNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");

    if (!record.isMeasurement()) {
      throw new DataSetException("Record is not a measurement");
    }

    ResultSet createdKeys = null;
    PreparedStatement datasetDataStatement = null;

    try {
      if (null == datasetDataStatement) {
        datasetDataStatement = createInsertRecordStatement(conn, record);
      }

      datasetDataStatement.setLong(1, record.getDatasetId());
      datasetDataStatement.setLong(2, DateTimeUtils.dateToLong(record.getDate()));
      datasetDataStatement.setDouble(3, record.getLongitude());
      datasetDataStatement.setDouble(4, record.getLatitude());
      datasetDataStatement.setString(5, record.getRunType());

      int currentField = 5;
      SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
      long instrumentId = record.getDataSet().getInstrumentId();

      for (SensorType sensorType : sensorConfig.getSensorTypes()) {

        // TODO The requiredForVariables check will be removed later in the migration
        if (sensorConfig.requiredForVariables(sensorType, InstrumentVariable.getIDsList(InstrumentDB.getVariables(conn, instrumentId)))) {

          if (!sensorConfig.isParent(sensorType)) {
            currentField++;
            Double sensorValue = record.getSensorValue(sensorType.getName());
            if (null == sensorValue) {
              datasetDataStatement.setNull(currentField, Types.DOUBLE);
            } else {
              datasetDataStatement.setDouble(currentField, sensorValue);
            }
          }
        }
      }

      datasetDataStatement.execute();

      CalculationDB calculationDB = CalculationDBFactory.getCalculationDB();

      createdKeys = datasetDataStatement.getGeneratedKeys();
      while (createdKeys.next()) {
        calculationDB.createCalculationRecord(conn, createdKeys.getLong(1));

        // TODO Diagnostic values are removed from this stage of the migration. Will be returned later
        //DiagnosticDataDB.storeDiagnosticValues(conn, createdKeys.getLong(1), record.getDiagnosticValues());
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error storing dataset record", e);
    } finally {
      DatabaseUtils.closeStatements(datasetDataStatement);
      DatabaseUtils.closeResultSets(createdKeys);
    }
  }

  /**
   * Get a single measurement from the database
   * @param conn A database connection
   * @param dataSet The data set to which the measurement belongs
   * @param measurementId The measurement's database ID
   * @return The record
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the measurement does not exist
   */
  public static DataSetRawDataRecord getMeasurement(Connection conn, DataSet dataSet, long measurementId) throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    DataSetRawDataRecord result = null;
    Map<String, Integer> baseColumns = new HashMap<String, Integer>();
    Map<Integer, String> sensorColumns = new HashMap<Integer, String>();

    try {
      stmt = conn.prepareStatement(GET_MEASUREMENT_QUERY);
      stmt.setLong(1, measurementId);
      records = stmt.executeQuery();

      if (!records.next()) {
        throw new RecordNotFoundException("Measurement data not found", "dataset_data", measurementId);
      } else {
        result = getRecordFromResultSet(conn, dataSet, records, baseColumns, sensorColumns);
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurement data", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }


  /**
   * Get measurement records for a dataset
   * @param dataSource A data source
   * @param dataSet The data set
   * @param start The first record to retrieve
   * @param length The number of records to retrieve
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(DataSource dataSource, DataSet dataSet, int start, int length) throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");

    List<DataSetRawDataRecord> result = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getMeasurements(conn, dataSet, start, length);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting measurement IDs", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }


  /**
   * Get all measurement records for a dataset
   * @param conn A database connection
   * @param dataSet The data set
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(Connection conn, DataSet dataSet) throws DatabaseException, MissingParamException {
    return getMeasurements(conn, dataSet, -1, -1);
  }

  /**
   * Get all measurement records for a dataset
   * @param conn A database connection
   * @param dataSet The data set
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(DataSource dataSource, DataSet dataSet) throws DatabaseException, MissingParamException {
    return getMeasurements(dataSource, dataSet, -1, -1);
  }

  /**
   * Get measurement records for a dataset
   * @param conn A database connection
   * @param dataSet The data set
   * @param start The first record to retrieve
   * @param length The number of records to retrieve
   * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   */
  public static List<DataSetRawDataRecord> getMeasurements(Connection conn, DataSet dataSet, int start, int length) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(dataSet, "dataSet");

    PreparedStatement stmt = null;
    ResultSet records = null;

    List<DataSetRawDataRecord> result = new ArrayList<DataSetRawDataRecord>();

    Map<String, Integer> baseColumns = new HashMap<String, Integer>();
    Map<Integer, String> sensorColumns = new HashMap<Integer, String>();

    try {
      StringBuilder query = new StringBuilder(GET_ALL_MEASUREMENTS_QUERY);
      if (length > 0) {
        query.append(" LIMIT ");
        query.append(start);
        query.append(',');
        query.append(length);
      }

      stmt = conn.prepareStatement(query.toString());
      stmt.setLong(1, dataSet.getId());

      records = stmt.executeQuery();

      while (records.next()) {
        result.add(getRecordFromResultSet(conn, dataSet, records, baseColumns, sensorColumns));
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
   * Read a record from a ResultSet
   * @param dataSet The data set to which the record belongs
   * @param records The result set
   * @param baseColumns The column indices for the base columns
   * @param sensorColumns The column indices for the sensor columns
   * @return The record
     * @throws MissingParamException If any required parameters are missing
     * @throws SQLException If the record details cannot be extracted
   * @throws DataSetException If the diagnostic values cannot be read
   * @throws InstrumentException If the instrument details cannot be retrieved
   * @throws RecordNotFoundException If the instrument does not exist
   * @throws DatabaseException If a database error occurs
     *
   */
  private static DataSetRawDataRecord getRecordFromResultSet(Connection conn, DataSet dataSet, ResultSet records, Map<String, Integer> baseColumns, Map<Integer, String> sensorColumns) throws MissingParamException, SQLException, DataSetException, DatabaseException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(records, "records");
    MissingParam.checkMissing(baseColumns, "baseColumns", true);
    MissingParam.checkMissing(sensorColumns, "sensorColumns", true);

    DataSetRawDataRecord result = null;

    // Get the column indices if we haven't already got them
    if (baseColumns.size() == 0) {
      ResultSetMetaData rsmd = records.getMetaData();
      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
      SensorAssignments sensorAssignments = instrument.getSensorAssignments();
      calculateColumnIndices(conn, dataSet.getInstrumentId(), rsmd, sensorAssignments, baseColumns, sensorColumns);
    }

    long id = records.getLong(baseColumns.get(ID_COL));
    LocalDateTime date = DateTimeUtils.longToDate(records.getLong(baseColumns.get(DATE_COL)));
    double longitude = records.getDouble(baseColumns.get(LON_COL));
    double latitude = records.getDouble(baseColumns.get(LAT_COL));
    long runType = records.getLong(baseColumns.get(RUN_TYPE_COL));
    RunTypeCategory runTypeCategory = ResourceManager.getInstance().getRunTypeCategoryConfiguration().getCategory(runType);

    // TODO v2 This code will be removed when the new data reduction routine is written.
    //         Commented out so things compile during this intermediate stage.
    //result = new DataSetRawDataRecord(dataSet, id, date, longitude, latitude, runType, runTypeCategory);

    // TODO Add diagnostics here

    for (Map.Entry<Integer, String> entry : sensorColumns.entrySet()) {
      Double value = records.getDouble(entry.getKey());
      if (records.wasNull()) {
        value = null;
      }

      result.setSensorValue(entry.getValue(), value);
    }

    return result;
  }

  /**
   * Calculate all the required column indices for extracting a data set record
   * @param rsmd The metadata of the query that's been run
   * @param baseColumns The mapping of base columns
   * @param sensorColumns The mapping of sensor columns
   * @throws SQLException If the column details cannot be read
   * @throws DatabaseException If a database error occurs
   * @throws VariableNotFoundException
   * @throws MissingParamException
   * @throws SensorConfigurationException
   */
  private static void calculateColumnIndices(Connection conn, long instrumentId,
      ResultSetMetaData rsmd, SensorAssignments sensorAssignments,
      Map<String, Integer> baseColumns, Map<Integer, String> sensorColumns)
        throws SQLException, DatabaseException, SensorConfigurationException, MissingParamException, VariableNotFoundException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();

    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
      String columnName = rsmd.getColumnName(i);
      switch (columnName) {
      case ID_COL: {
        baseColumns.put(ID_COL, i);
        break;
      }
      case DATE_COL: {
        baseColumns.put(DATE_COL, i);
        break;
      }
      case LON_COL: {
        baseColumns.put(LON_COL, i);
        break;
      }
      case LAT_COL: {
        baseColumns.put(LAT_COL, i);
        break;
      }
      case RUN_TYPE_COL: {
        baseColumns.put(RUN_TYPE_COL, i);
        break;
      }
      case DATASET_COL: {
        // Do nothing
        break;
      }
      default: {
        // This is a sensor field. Get the sensor name from the sensors configuration
        for (SensorType sensorType : sensorConfig.getSensorTypes()) {
//          if (sensorConfig.requiredForVariables(sensorType,
//            InstrumentVariable.getIDsList(InstrumentDB.getVariables(conn, instrumentId))) &&
//            sensorAssignments.getAssignmentCount(sensorType) > 0) {

          if (sensorAssignments.getAssignmentCount(sensorType) > 0) {

            if (columnName.equals(sensorType.getDatabaseFieldName())) {
              sensorColumns.put(i, sensorType.getName());
              break;
            }
          }
        }
      }
      }
    }
  }

  /**
   * Create a statement to insert a new dataset record in the database
   * @param conn A database connection
   * @param record A dataset record
   * @return The statement
   * @throws MissingParamException If any required parameters are missing
   * @throws SQLException If the statement cannot be created
   * @throws DatabaseException If a database error occurs
   * @throws VariableNotFoundException
   * @throws SensorConfigurationException
   */
  private static PreparedStatement createInsertRecordStatement(Connection conn,
    DataSetRawDataRecord record)
      throws MissingParamException, SQLException, DatabaseException, SensorConfigurationException, VariableNotFoundException {

    List<String> fieldNames = new ArrayList<String>();

    fieldNames.add("dataset_id");
    fieldNames.add("date");
    fieldNames.add("longitude");
    fieldNames.add("latitude");
    fieldNames.add("run_type");

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    for (SensorType sensorType : sensorConfig.getSensorTypes()) {

      // TODO requiredForVariables will not be called later in the migration.
      if (sensorConfig.requiredForVariables(sensorType,
        InstrumentVariable.getIDsList(InstrumentDB.getVariables(conn, record.getDataSet().getInstrumentId())))) {

        if (!sensorConfig.isParent(sensorType)) {
          fieldNames.add(sensorType.getDatabaseFieldName());
        }
      }
    }

    return DatabaseUtils.createInsertStatement(conn, "dataset_data", fieldNames, Statement.RETURN_GENERATED_KEYS);
  }

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
  public static List<String> getDatasetDataColumnNames(DataSource dataSource, DataSet dataSet) throws MissingParamException, DatabaseException, InstrumentException, RecordNotFoundException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");

    List<String> result = new ArrayList<String>();

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    Connection conn = null;
    ResultSet columns = null;

    try {
      conn = dataSource.getConnection();

      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
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

  public static void populateVariableList(DataSource dataSource, DataSet dataSet, VariableList variables) throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");
    MissingParam.checkMissing(variables, "variables", true);

    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    Connection conn = null;
    ResultSet columns = null;

    try {
      conn = dataSource.getConnection();

      ResourceManager resourceManager = ResourceManager.getInstance();
      Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
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
          variables.addVariable("Date/Time", new Variable(Variable.TYPE_BASE, "Date/Time", "date"));
          break;
        }
        case "longitude": {
          variables.addVariable("Longitude", new Variable(Variable.TYPE_BASE, "Longitude", "longitude"));
          break;
        }
        case "latitude": {
          variables.addVariable("Latitude", new Variable(Variable.TYPE_BASE, "Latitude", "latitude"));
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
                variables.addVariable(sensorType.getGroup(), new Variable(Variable.TYPE_SENSOR, sensorType.getName(), columnName));
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
  public static List<String> extractDatasetFields(Connection conn, DataSet dataSet, List<String> originalFields) throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException {
    List<String> datasetFields = new ArrayList<String>();

    ResourceManager resourceManager = ResourceManager.getInstance();
    SensorsConfiguration sensorConfig = resourceManager.getSensorsConfiguration();

    Instrument instrument = InstrumentDB.getInstrument(conn, dataSet.getInstrumentId(), resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());
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
  public static boolean isDatasetField(Connection conn, DataSet dataset, String field) throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException {
    List<String> fieldList = new ArrayList<String>(1);
    fieldList.add(field);

    List<String> detectedDatasetField = extractDatasetFields(conn, dataset, fieldList);

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
  public static List<Double> getDataBounds(DataSource dataSource, DataSet dataset) throws MissingParamException, DatabaseException {

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
  public static List<Double> getDataBounds(Connection conn, DataSet dataset) throws MissingParamException, DatabaseException {

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
      result.add((records.getDouble(3) - records.getDouble(1)) / 2 + records.getDouble(1));
      result.add((records.getDouble(4) - records.getDouble(2)) / 2 + records.getDouble(2));

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
  public static Map<Long, List<SensorValue>> getSensorValuesByColumn(
    Connection conn, long datasetId)
      throws RecordNotFoundException, DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");

    Map<Long, List<SensorValue>> values = new HashMap<Long, List<SensorValue>>();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      stmt = conn.prepareStatement(GET_SENSOR_VALUES_BY_COLUMN_QUERY);
      stmt.setLong(1, datasetId);

      records = stmt.executeQuery();

      long currentColumnId = -1;
      List<SensorValue> currentSensorValues = new ArrayList<SensorValue>();

      while (records.next()) {
        SensorValue sensorValue = sensorValueFromResultSet(records, datasetId);

        if (sensorValue.getColumnId() != currentColumnId) {
          if (currentColumnId != -1) {
            values.put(currentColumnId, currentSensorValues);
          }

          currentColumnId = sensorValue.getColumnId();
          currentSensorValues = new ArrayList<SensorValue>();
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

      stmt = conn.prepareStatement(STORE_MEASUREMENT_STATEMENT, Statement.RETURN_GENERATED_KEYS);

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
  public static List<Measurement> getMeasurements(Connection conn, Instrument instrument, long datasetId) throws MissingParamException, DatabaseException {

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

  public static MeasurementsWithSensorValues
  getMeasurementData(Connection conn, Instrument instrument, long datasetId)
    throws MissingParamException, DatabaseException, InvalidFlagException {

    MeasurementsWithSensorValues result = null;

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkPositive(datasetId, "datasetId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      List<Measurement> measurements = getMeasurements(conn, instrument, datasetId);
      result = new MeasurementsWithSensorValues(instrument, measurements);

      stmt = conn.prepareStatement(GET_MEASUREMENT_SENSOR_VALUES_QUERY);
      stmt.setLong(1,  datasetId);

      records = stmt.executeQuery();

      while (records.next()) {
        long measurementId = records.getLong(1);
        long sensorValueId = records.getLong(2);
        long fileColumnId = records.getLong(3);
        LocalDateTime date = DateTimeUtils.longToDate(records.getLong(4));
        String value = records.getString(5);
        AutoQCResult autoQC = AutoQCResult.buildFromJson(records.getString(6));
        Flag userQCFlag = new Flag(records.getInt(7));
        String userQCMessage = records.getString(8);

        SensorValue sensorValue = new SensorValue(sensorValueId, datasetId,
          fileColumnId, date, value, autoQC, userQCFlag, userQCMessage);

        result.add(measurementId, sensorValue);
      }
    } catch (Exception e) {
      throw new DatabaseException("Error while retrieving measurements and sensor values", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }
}
