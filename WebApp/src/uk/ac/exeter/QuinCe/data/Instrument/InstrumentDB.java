package uk.ac.exeter.QuinCe.data.Instrument;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.api.nrt.NrtInstrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecificationException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Database methods dealing with instruments
 * @author Steve Jones
 *
 */
public class InstrumentDB {

  ////////// *** CONSTANTS *** ///////////////

  /**
   * Statement for inserting an instrument record
   */
  private static final String CREATE_INSTRUMENT_STATEMENT = "INSERT INTO instrument ("
      + "owner, name," // 2
      + "pre_flushing_time, post_flushing_time, minimum_water_flow, averaging_mode, " // 6
      + "platform_code, nrt" // 8
      + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

  /**
   * Statement for inserting an instrument variable record
   */
  private static final String CREATE_INSTRUMENT_VARIABLE_STATEMENT = "INSERT INTO "
    + "instrument_variables (instrument_id, variable_id) " // 2
    + "VALUES (?, ?)";

  /**
   * Statement for inserting a file definition record
   */
  private static final String CREATE_FILE_DEFINITION_STATEMENT = "INSERT INTO file_definition ("
      + "instrument_id, description, column_separator, " // 3
      + "header_type, header_lines, header_end_string, " // 6
      + "column_header_rows, column_count, " // 8
      + "lon_format, lon_value_col, lon_hemisphere_col, " // 11
      + "lat_format, lat_value_col, lat_hemisphere_col, " // 14
      + "date_time_col, date_time_props, date_col, date_props, " // 18
      + "hours_from_start_col, hours_from_start_props, " // 20
      + "jday_time_col, jday_col, year_col, month_col, day_col, " // 25
      + "time_col, time_props, hour_col, minute_col, second_col" // 30
      + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /**
   * Statement for inserting a file column definition record
   */
  private static final String CREATE_FILE_COLUMN_STATEMENT = "INSERT INTO file_column ("
      + "file_definition_id, file_column, primary_sensor, sensor_type, " // 4
      + "sensor_name, value_column, depends_question_answer, " // 7
      + "missing_value" // 9
      + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

  /**
   * Query to get all the run types of a given run type category
   */
  private static final String GET_RUN_TYPES_QUERY = "SELECT r.run_name AS run_type "
      + "FROM file_definition AS f INNER JOIN run_type AS r ON f.id = r.file_definition_id "
      + "WHERE f.instrument_id = ? AND category_code = ? ORDER BY run_type";

  /**
   * Query to get all the run types used in a given file definition
   */
  private static final String GET_FILE_RUN_TYPES_QUERY = "SELECT "
      + "run_name, category_code, alias_to "
      + "FROM run_type WHERE file_definition_id = ?";

  /**
   * Statement for inserting run types
   */
  private static final String CREATE_RUN_TYPE_STATEMENT = "INSERT INTO run_type ("
      + "file_definition_id, run_name, category_code, alias_to" // 4
      + ") VALUES (?, ?, ?, ?)";

  /**
   * Query for retrieving the list of instruments owned by a particular user
   */
  private static final String GET_SINGLE_USER_INSTRUMENT_LIST_QUERY = "SELECT "
      + "id, name FROM instrument "
      + "WHERE owner = ? ORDER BY name ASC";

  /**
   * Query for retrieving the list of all instruments in the system, grouped by owner
   */
  private static final String GET_ALL_USERS_INSTRUMENT_LIST_QUERY = "SELECT "
      + "i.id, CONCAT(u.surname, \", \", u.firstname, \" - \", i.name) AS name "
      + "FROM instrument AS i "
      + "INNER JOIN user AS u ON i.owner = u.id "
      + "GROUP BY i.id ORDER BY name ASC";

  /**
   * Query for retrieving the stub for a specific instrument
   */
  private static final String GET_INSTRUMENT_STUB_QUERY = "SELECT name "
      + "FROM instrument WHERE i.id = ? ";

  /**
   * SQL query to get an instrument's base record
   */
  private static final String GET_INSTRUMENT_QUERY = "SELECT "
      + "name, owner, " // 2
      + "pre_flushing_time, post_flushing_time, minimum_water_flow, averaging_mode, " // 6
      + "platform_code, nrt " // 7
      + "FROM instrument WHERE id = ?";

  /**
   * Query to get the variables measured by an instrument
   */
  private static final String GET_INSTRUMENT_VARIABLES_QUERY = "SELECT "
    + "variable_id FROM instrument_variables WHERE instrument_id = ?";

  /**
   * SQL query to get the file definitions for an instrument
   */
  private static final String GET_FILE_DEFINITIONS_QUERY = "SELECT "
        + "id, description, column_separator, " // 3
      + "header_type, header_lines, header_end_string, column_header_rows, " // 7
      + "column_count, lon_format, lon_value_col, lon_hemisphere_col, " // 11
      + "lat_format, lat_value_col, lat_hemisphere_col, " // 14
      + "date_time_col, date_time_props, date_col, date_props, hours_from_start_col, hours_from_start_props, " // 20
      + "jday_time_col, jday_col, year_col, month_col, day_col, " // 25
      + "time_col, time_props, hour_col, minute_col, second_col " // 30
      + "FROM file_definition WHERE instrument_id = ? ORDER BY description";

  /**
   * SQL query to get the file column assignments for a file
   */
  private static final String GET_FILE_COLUMNS_QUERY = "SELECT "
      + "id, file_column, primary_sensor, sensor_type, sensor_name, value_column, " // 6
      + "depends_question_answer, missing_value " // 8
        + "FROM file_column WHERE file_definition_id = ?";

  /**
   * Query to get the list of sensors that require calibration for a given instrument
   */
  private static final String GET_CALIBRATABLE_SENSORS_QUERY = "SELECT CONCAT(f.description, ': ', c.sensor_name) AS sensor "
      + "FROM file_definition AS f INNER JOIN file_column AS c ON c.file_definition_id = f.id "
      + "WHERE f.instrument_id = ? AND c.post_calibrated = true ORDER BY sensor";

  /**
   * Query for retrieving the list of all instruments that provide NRT data
   */
  private static final String GET_NRT_INSTRUMENTS_QUERY = "SELECT "
      + "i.id, i.name, CONCAT(u.surname, \", \", u.firstname) AS name "
      + "FROM instrument AS i "
      + "INNER JOIN user AS u ON i.owner = u.id "
      + "WHERE i.nrt = 1 "
      + "ORDER BY name ASC, i.name ASC";

  /**
   * Query used to determine if an instrument with a specified ID
   * exists in the database
   */
  private static final String INSTRUMENT_ID_EXISTS_QUERY = "SELECT "
      + " id FROM instrument WHERE id = ?";

  /**
   * Query used to determine if an instrument with a specified ID
   * allows NRT datasets
   */
  private static final String NRT_INSTRUMENT_QUERY = "SELECT "
      + "nrt FROM instrument WHERE id = ?";

  /**
   * Query to get the owner ID for an instrument
   */
  private static final String GET_INSTRUMENT_OWNER_QUERY = "SELECT "
      + "owner FROM instrument WHERE id = ?";

  /**
   * Store a new instrument in the database
   * @param dataSource A data source
   * @param instrument The instrument
   * @throws MissingParamException If any required parameters are missing
   * @throws InstrumentException If the Instrument object is invalid
   * @throws DatabaseException If a database error occurs
   * @throws IOException If any of the data cannot be converted for storage in the database
   */
  public static void storeInstrument(DataSource dataSource, Instrument instrument) throws MissingParamException, InstrumentException, DatabaseException, IOException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "instrument");

    // Validate the instrument. Will throw an exception
    instrument.validate(false);

    Connection conn = null;
    PreparedStatement instrumentStatement = null;
    ResultSet instrumentKey = null;
    List<PreparedStatement> subStatements = new ArrayList<PreparedStatement>();
    List<ResultSet> keyResultSets = new ArrayList<ResultSet>();

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Create the instrument record
      instrumentStatement = makeCreateInstrumentStatement(conn, instrument);
      instrumentStatement.execute();
      instrumentKey = instrumentStatement.getGeneratedKeys();
      if (!instrumentKey.next()) {
        throw new DatabaseException("Instrument record was not created in the database");
      } else {
        long instrumentId = instrumentKey.getLong(1);
        instrument.setDatabaseId(instrumentId);

        // Store the instrument's variables
        for (InstrumentVariable variable : instrument.getVariables()) {
          PreparedStatement variableStmt = conn.prepareStatement(CREATE_INSTRUMENT_VARIABLE_STATEMENT);
          variableStmt.setLong(1, instrumentId);
          variableStmt.setLong(2, variable.getId());
          variableStmt.execute();
          subStatements.add(variableStmt);
        }

        // Store the database IDs for all the file definitions
        Map<String, Long> fileDefinitionIds = new HashMap<String, Long>(instrument.getFileDefinitions().size());

        // Now store the file definitions
        for (FileDefinition file : instrument.getFileDefinitions()) {
          PreparedStatement fileStatement = makeCreateFileDefinitionStatement(conn, file, instrumentId);
          subStatements.add(fileStatement);

          fileStatement.execute();
          ResultSet fileKey = fileStatement.getGeneratedKeys();
          keyResultSets.add(fileKey);

          if (!fileKey.next()) {
            throw new DatabaseException("File Definition record was not created in the database");
          } else {
            long fileId = fileKey.getLong(1);
            fileDefinitionIds.put(file.getFileDescription(), fileId);

            // Run Types
            if (null != file.getRunTypes()) {
              for (RunTypeAssignment assignment : file.getRunTypes().values()) {
                PreparedStatement runTypeStatement =
                    storeFileRunType(conn, fileId, assignment);
                subStatements.add(runTypeStatement);
              }
            }
          }
        }

        // Sensor assignments
        int databaseColumn = -1;

        for (Map.Entry<SensorType, Set<SensorAssignment>> sensorAssignmentsEntry : instrument.getSensorAssignments().entrySet()) {

          SensorType sensorType = sensorAssignmentsEntry.getKey();

          for (SensorAssignment assignment : sensorAssignmentsEntry.getValue()) {

            PreparedStatement fileColumnStatement = conn.prepareStatement(CREATE_FILE_COLUMN_STATEMENT, Statement.RETURN_GENERATED_KEYS);
            fileColumnStatement.setLong(1, fileDefinitionIds.get(assignment.getDataFile()));
            fileColumnStatement.setInt(2, assignment.getColumn());
            fileColumnStatement.setBoolean(3, assignment.isPrimary());
            fileColumnStatement.setLong(4, sensorType.getId());
            fileColumnStatement.setString(5, assignment.getSensorName());

            if (sensorType.isDiagnostic()) {
              fileColumnStatement.setNull(6, Types.INTEGER);
            } else {
              databaseColumn++;
              assignment.setDatabaseColumn(databaseColumn);
              fileColumnStatement.setInt(6, databaseColumn);
            }

            fileColumnStatement.setBoolean(7, assignment.getDependsQuestionAnswer());
            fileColumnStatement.setString(8, assignment.getMissingValue());

            fileColumnStatement.execute();
            ResultSet fileColumnKey = fileColumnStatement.getGeneratedKeys();
            if (!fileColumnKey.next()) {
              throw new DatabaseException("File Column record was not created in the database");
            } else {
              assignment.setDatabaseId(fileColumnKey.getLong(1));
            }

            subStatements.add(fileColumnStatement);
            keyResultSets.add(fileColumnKey);
          }
        }
      }

      conn.commit();
    } catch (SQLException e) {
      boolean rollbackOK = true;

      try {
        conn.rollback();
      } catch (SQLException e2) {
        rollbackOK = false;
      }

      throw new DatabaseException("Exception while storing instrument", e, rollbackOK);
    } finally {
      DatabaseUtils.closeResultSets(keyResultSets);
      DatabaseUtils.closeResultSets(instrumentKey);
      DatabaseUtils.closeStatements(subStatements);
      DatabaseUtils.closeStatements(instrumentStatement);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Make the statement used to create an instrument record in the database
   * @param conn A database connection
   * @param instrument The instrument
   * @return The database statement
   * @throws SQLException If an error occurs while building the statement
   */
  private static PreparedStatement makeCreateInstrumentStatement(Connection conn, Instrument instrument) throws SQLException {
    PreparedStatement stmt = conn.prepareStatement(CREATE_INSTRUMENT_STATEMENT, Statement.RETURN_GENERATED_KEYS);
    stmt.setLong(1, instrument.getOwnerId()); // owner
    stmt.setString(2, instrument.getName()); // name
    stmt.setInt(3, instrument.getPreFlushingTime()); // pre_flushing_time
    stmt.setInt(4, instrument.getPostFlushingTime()); // post_flushing_time
    stmt.setInt(5, instrument.getMinimumWaterFlow()); // minimum_water_flow
    stmt.setInt(6, instrument.getAveragingMode()); // averaging_mode
    stmt.setString(7, instrument.getPlatformCode()); // platform_code
    stmt.setBoolean(8, instrument.getNrt()); // nrt

    return stmt;
  }

  /**
   * Create a statement for adding a file definition to the database
   * @param conn A database connection
   * @param file The file definition
   * @param instrumentId The database ID of the instrument to which the file belongs
   * @return The statement
   * @throws SQLException If the statement cannot be built
   * @throws IOException If any Properties objects cannot be serialized into Strings for storage
   */
  private static PreparedStatement makeCreateFileDefinitionStatement(Connection conn, FileDefinition file, long instrumentId) throws SQLException, IOException {

    PreparedStatement stmt = conn.prepareStatement(CREATE_FILE_DEFINITION_STATEMENT, Statement.RETURN_GENERATED_KEYS);

    stmt.setLong(1, instrumentId); // instrument_id
    stmt.setString(2, file.getFileDescription()); // description
    stmt.setString(3, file.getSeparator()); // separator
    stmt.setInt(4, file.getHeaderType()); // header_type

    if (file.getHeaderType() == FileDefinition.HEADER_TYPE_LINE_COUNT) {
      stmt.setInt(5, file.getHeaderLines()); // header_lines
      stmt.setNull(6, Types.VARCHAR); // header_end_string
    } else {
      stmt.setNull(5, Types.INTEGER); // header_lines
      stmt.setString(6, file.getHeaderEndString()); // header_end_string
    }

    stmt.setInt(7, file.getColumnHeaderRows()); // column_header_rows
    stmt.setInt(8, file.getColumnCount()); // column_count

    addPositionAssignment(stmt, file.getLongitudeSpecification(), 9, 10, 11); // longitude
    addPositionAssignment(stmt, file.getLatitudeSpecification(), 12, 13, 14); // latitude

    DateTimeSpecification dateTimeSpec = file.getDateTimeSpecification();
    addDateTimeAssignment(stmt, 15, 16, DateTimeSpecification.DATE_TIME, dateTimeSpec); // date_time_col
    addDateTimeAssignment(stmt, 17, 18, DateTimeSpecification.DATE, dateTimeSpec); // date
    addDateTimeAssignment(stmt, 19, 20, DateTimeSpecification.HOURS_FROM_START, dateTimeSpec); // hours_from_start
    addDateTimeAssignment(stmt, 21, -1, DateTimeSpecification.JDAY_TIME, dateTimeSpec); // jday_time_col
    addDateTimeAssignment(stmt, 22, -1, DateTimeSpecification.JDAY, dateTimeSpec); // jday_col
    addDateTimeAssignment(stmt, 23, -1, DateTimeSpecification.YEAR, dateTimeSpec); // year_col
    addDateTimeAssignment(stmt, 24, -1, DateTimeSpecification.MONTH, dateTimeSpec); // jmonth_col
    addDateTimeAssignment(stmt, 25, -1, DateTimeSpecification.DAY, dateTimeSpec); // jday_col
    addDateTimeAssignment(stmt, 26, 27, DateTimeSpecification.TIME, dateTimeSpec); // time_col
    addDateTimeAssignment(stmt, 28, -1, DateTimeSpecification.HOUR, dateTimeSpec); // hour_col
    addDateTimeAssignment(stmt, 29, -1, DateTimeSpecification.MINUTE, dateTimeSpec); // minute_col
    addDateTimeAssignment(stmt, 30, -1, DateTimeSpecification.SECOND, dateTimeSpec); // second_col

    return stmt;
  }

  /**
   * Add a position assignment fields to a statement for inserting a file definition
   * @param stmt The file definition statement
   * @param posSpec The position specification
   * @param formatIndex The index in the statement of the format field
   * @param valueIndex The index in the statement of the value field
   * @param hemisphereIndex The index in the statement of the hemisphere field
   * @throws SQLException If adding the assignment fails
   */
  private static void addPositionAssignment(PreparedStatement stmt, PositionSpecification posSpec, int formatIndex, int valueIndex, int hemisphereIndex) throws SQLException {
    stmt.setInt(formatIndex, posSpec.getFormat()); // pos_format
    stmt.setInt(valueIndex, posSpec.getValueColumn()); // pos_value_col

    if (posSpec.hemisphereRequired()) {
      stmt.setInt(hemisphereIndex, posSpec.getHemisphereColumn()); // pos_hemisphere_col
    } else {
      stmt.setInt(hemisphereIndex, -1); // pos_hemisphere_col
    }
  }

  /**
   * Add a date/time column assignment to a statement for inserting a file definition
   * @param stmt The file definition statement
   * @param stmtColumnIndex The index in the statement to be set
   * @param stmtPropsIndex The index in the statement to be set. If no properties are to be stored, set to -1
   * @param assignmentId The required date/time assignment
   * @param dateTimeSpec The file's date/time specification
   * @throws SQLException If adding the assignment fails
   * @throws IOException If the Properties object cannot be serialized into a String
   */
  private static void addDateTimeAssignment(PreparedStatement stmt, int stmtColumnIndex, int stmtPropsIndex, int assignmentId, DateTimeSpecification dateTimeSpec) throws SQLException, IOException {

    int column = -1;
    Properties properties = null;

    DateTimeColumnAssignment assignment = dateTimeSpec.getAssignment(assignmentId);
    if (null != assignment) {
      if (assignment.isAssigned()) {
        column = assignment.getColumn();
        properties = assignment.getProperties();
      }
    }

    if (column == -1) {
      stmt.setInt(stmtColumnIndex, -1);
      if (stmtPropsIndex != -1) {
        stmt.setNull(stmtPropsIndex, Types.VARCHAR);
      }
    } else {
      stmt.setInt(stmtColumnIndex, column);

      if (stmtPropsIndex != -1) {
        if (null == properties || properties.size() == 0) {
          stmt.setNull(stmtPropsIndex, Types.VARCHAR);
        } else {
          StringWriter writer = new StringWriter();
          properties.store(writer, null);
          stmt.setString(stmtPropsIndex, writer.toString());
        }
      }
    }
  }

  /**
   * Returns a list of instruments owned by a given user.
   * The list contains {@link InstrumentStub} objects, which just contain
   * the details required for lists of instruments in the UI.
   *
   * If the specified owner is an administrator, the returned list
   * will contain all instruments in the system
   *
   * The list is ordered by the name of the instrument.
   *
   * @param dataSource A data source
   * @param owner The owner whose instruments are to be listed
   * @return The list of instruments
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurred
   */
  public static List<InstrumentStub> getInstrumentList(DataSource dataSource, User owner) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(owner, "owner");

    Connection conn = null;
    List<InstrumentStub> result = null;

    try {
      conn = dataSource.getConnection();

      if (owner.isAdminUser() || owner.isApprovalUser()) {
        result = getAllUsersInstrumentList(conn);
      } else {
        result = getInstrumentList(conn, owner.getDatabaseID());
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error retrieving instrument list", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the list of instruments for a single user.
   *
   * See {@link #getInstrumentList(DataSource, User)}
   *
   * @param conn A database connection
   * @param ownerId The user's database ID
   * @return The list of instruments
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurred
   */
  private static List<InstrumentStub> getInstrumentList(Connection conn, long ownerId) throws MissingParamException, DatabaseException {

    PreparedStatement stmt = null;
    List<InstrumentStub> instrumentList = null;

    try {
      stmt = conn.prepareStatement(GET_SINGLE_USER_INSTRUMENT_LIST_QUERY);
      stmt.setLong(1, ownerId);
      instrumentList = runInstrumentListQuery(conn, stmt);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving instrument list", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }

    return instrumentList;
  }

  /**
   * Get the list of all instruments in the system
   *
   * See {@link #getInstrumentList(DataSource, User)}
   *
   * @param conn A database connection
   * @param ownerId The user's database ID
   * @return The list of instruments
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurred
   */
  private static List<InstrumentStub> getAllUsersInstrumentList(Connection conn) throws MissingParamException, DatabaseException {

    PreparedStatement stmt = null;
    List<InstrumentStub> instrumentList = null;

    try {
      stmt = conn.prepareStatement(GET_ALL_USERS_INSTRUMENT_LIST_QUERY);
      instrumentList = runInstrumentListQuery(conn, stmt);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving instrument list", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }

    return instrumentList;
  }

  /**
   * Run a query to get a list of instruments. The query must produce three columns:
   *
   * <ol>
   *   <li>Instrument ID</li>
   *   <li>Instrument Name</li>
   *   <li>Number of calibratable sensors</li>
   * </ol>
   *
   * @param conn A database connection
   * @param stmt The query to be executed
   * @return The list of instruments
   * @throws DatabaseException If the query fails
   */
  private static List<InstrumentStub> runInstrumentListQuery(Connection conn, PreparedStatement stmt) throws DatabaseException {

    ResultSet instruments = null;
    List<InstrumentStub> instrumentList = new ArrayList<InstrumentStub>();
    try {
      instruments = stmt.executeQuery();
      while (instruments.next()) {
        InstrumentStub record = new InstrumentStub(instruments.getLong(1), instruments.getString(2));
        instrumentList.add(record);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving instrument list", e);
    } finally {
      DatabaseUtils.closeResultSets(instruments);
    }

    return instrumentList;
  }


  /**
   * Get an stub object for a specified instrument
   * @param dataSource A data source
   * @param instrumentId The instrument's database ID
   * @return The instrument stub
   * @throws RecordNotFoundException If the instrument does not exist
   * @throws DatabaseException If a database error occurs
   */
  public static InstrumentStub getInstrumentStub(DataSource dataSource, long instrumentId) throws RecordNotFoundException, DatabaseException {
    InstrumentStub result = null;

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet record = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_INSTRUMENT_STUB_QUERY);
      stmt.setLong(1, instrumentId);

      record = stmt.executeQuery();
      if (!record.next()) {
        throw new RecordNotFoundException("Instrument not found", "instrument", instrumentId);
      } else {
        result = new InstrumentStub(instrumentId, record.getString(1));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving instrument list", e);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Determine whether an instrument with a given name and owner exists
   * @param dataSource A data source
   * @param owner The owner
   * @param name The instrument name
   * @return {@code true} if the instrument exists; {@code false} if it does not
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static boolean instrumentExists(DataSource dataSource, User owner, String name) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(owner, "owner");
    MissingParam.checkMissing(name, "name");

    boolean exists = false;

    for (InstrumentStub instrument : getInstrumentList(dataSource, owner)) {
      if (instrument.getName().equalsIgnoreCase(name)) {
        exists = true;
        break;
      }
    }

    return exists;
  }

  /**
   * Retrieve a complete {@link Instrument} from the database
   * @param dataSource A data source
   * @param instrumentID The instrument's database ID
   * @param sensorConfiguration The sensors configuration
   * @param runTypeConfiguration The run type category configuration
   * @return The instrument object
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If the instrument ID does not exist
   * @throws InstrumentException If any instrument values are invalid
   */
  public static Instrument getInstrument(DataSource dataSource, long instrumentID, SensorsConfiguration sensorConfiguration, RunTypeCategoryConfiguration runTypeConfiguration) throws DatabaseException, MissingParamException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(instrumentID, "instrumentID");

    Instrument result = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getInstrument(conn, instrumentID, sensorConfiguration, runTypeConfiguration);
    } catch (SQLException e) {
      throw new DatabaseException("Error while updating record counts", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }


  /**
   * Returns a complete instrument object for the specified instrument ID
   * @param conn A database connection
   * @param instrumentId The instrument ID
   * @param sensorConfiguration The sensors configuration
   * @param runTypeConfiguration The run type category configuration
   * @return The complete Instrument object
   * @throws MissingParamException If the data source is not supplied
   * @throws DatabaseException If an error occurs while retrieving the instrument details
   * @throws RecordNotFoundException If the specified instrument cannot be found
   * @throws InstrumentException If any instrument values are invalid
   */
  public static Instrument getInstrument(Connection conn, long instrumentId, SensorsConfiguration sensorConfiguration, RunTypeCategoryConfiguration runTypeConfiguration) throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    Instrument instrument = null;

    List<PreparedStatement> stmts = new ArrayList<PreparedStatement>();
    List<ResultSet> resultSets = new ArrayList<ResultSet>();

    try {
      // Data fields
      String name;
      long owner;
      int preFlushingTime;
      int postFlushingTime;
      int minimumWaterFlow;
      int averagingMode;
      String platformCode;
      boolean nrt;
      InstrumentFileSet files;
      List<InstrumentVariable> variables;
      SensorAssignments sensorAssignments;


      // Get the raw instrument data
      PreparedStatement instrStmt = conn.prepareStatement(GET_INSTRUMENT_QUERY);
      instrStmt.setLong(1, instrumentId);
      stmts.add(instrStmt);

      ResultSet instrumentRecord = instrStmt.executeQuery();
      resultSets.add(instrumentRecord);

      if (!instrumentRecord.next()) {
        throw new RecordNotFoundException("Instrument record not found", "instrument", instrumentId);
      } else {
        // Read in the instrument details
        name = instrumentRecord.getString(1);
        owner = instrumentRecord.getLong(2);
        preFlushingTime = instrumentRecord.getInt(3);
        postFlushingTime = instrumentRecord.getInt(4);
        minimumWaterFlow = instrumentRecord.getInt(5);
        averagingMode = instrumentRecord.getInt(6);
        platformCode = instrumentRecord.getString(7);
        nrt = instrumentRecord.getBoolean(8);

        // Now get the file definitions
        files = getFileDefinitions(conn, instrumentId);

        // The variables
        variables = getVariables(conn, instrumentId);

        // Now the sensor assignments
        sensorAssignments = getSensorAssignments(conn, instrumentId, files, sensorConfiguration, runTypeConfiguration);

        instrument = new Instrument(instrumentId, owner, name, files, variables,
          sensorAssignments, preFlushingTime, postFlushingTime, minimumWaterFlow,
          averagingMode, platformCode, nrt);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error retrieving instrument", e);
    } finally {
      DatabaseUtils.closeResultSets(resultSets);
      DatabaseUtils.closeStatements(stmts);
    }

    return instrument;
  }

  /**
   * Get the file definitions for an instrument
   * @param conn A database connection
   * @param instrumentId The instrument's ID
   * @return The file definitions
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If no file definitions are stored for the instrument
   */
  public static InstrumentFileSet getFileDefinitions(Connection conn, long instrumentId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    InstrumentFileSet fileSet = new InstrumentFileSet();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      stmt = conn.prepareStatement(GET_FILE_DEFINITIONS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {

        long id = records.getLong(1);
        String description = records.getString(2);
        String separator = records.getString(3);
        int headerType = records.getInt(4);
        int headerLines = records.getInt(5);
        String headerEndString = records.getString(6);
        int columnHeaderRows = records.getInt(7);
        int columnCount = records.getInt(8);

        LongitudeSpecification lonSpec = buildLongitudeSpecification(records);
        LatitudeSpecification latSpec = buildLatitudeSpecification(records);
        DateTimeSpecification dateTimeSpec = buildDateTimeSpecification(records);

        fileSet.add(new FileDefinition(id, description, separator, headerType, headerLines, headerEndString, columnHeaderRows, columnCount, lonSpec, latSpec, dateTimeSpec, fileSet));
      }

    } catch (SQLException|IOException|PositionException|DateTimeSpecificationException e) {
      throw new DatabaseException("Error reading file definitions", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    if (fileSet.size() == 0) {
      throw new RecordNotFoundException("No file definitions found for instrument " + instrumentId);
    }
    return fileSet;
  }

  /**
   * Construct a {@link LongitudeSpecification} object from a database record
   * @param record The database record
   * @return The specification
   * @throws SQLException If a database error occurs
   * @throws PositionException If the specification is invalid
   * @see #GET_FILE_DEFINITIONS_QUERY
   */
  private static LongitudeSpecification buildLongitudeSpecification(ResultSet record) throws SQLException, PositionException {
    LongitudeSpecification spec = null;

    int format = record.getInt(9);
    int valueColumn = record.getInt(10);
    int hemisphereColumn = record.getInt(11);

    if (format != -1) {
      spec = new LongitudeSpecification(format, valueColumn, hemisphereColumn);
    }
    return spec;
  }

  /**
   * Construct a {@link LatitudeSpecification} object from a database record
   * @param record The database record
   * @return The specification
   * @throws SQLException If a database error occurs
   * @throws PositionException If the specification is invalid
   * @see #GET_FILE_DEFINITIONS_QUERY
   */
  private static LatitudeSpecification buildLatitudeSpecification(ResultSet record) throws SQLException, PositionException {
    LatitudeSpecification spec = null;

    int format = record.getInt(12);
    int valueColumn = record.getInt(13);
    int hemisphereColumn = record.getInt(14);

    if (format != -1) {
      spec = new LatitudeSpecification(format, valueColumn, hemisphereColumn);
    }

    return spec;
  }

  /**
   * Construct a {@link DateTimeSpecification} object from a database record
   * @param record The database record
   * @return The specification
   * @throws SQLException If a database error occurs
   * @throws IOException If a Properties string cannot be parsed
   * @throws DateTimeSpecificationException If the specification is invalid
   * @see #GET_FILE_DEFINITIONS_QUERY
   */
  private static DateTimeSpecification buildDateTimeSpecification(ResultSet record) throws SQLException, IOException, DateTimeSpecificationException {
    int headerLines = record.getInt(5);

    int dateTimeCol = record.getInt(15);
    Properties dateTimeProps = StringUtils.propertiesFromString(record.getString(16));
    int dateCol = record.getInt(17);
    Properties dateProps = StringUtils.propertiesFromString(record.getString(18));
    int hoursFromStartCol = record.getInt(19);
    Properties hoursFromStartProps = StringUtils.propertiesFromString(record.getString(20));
    int jdayTimeCol = record.getInt(21);
    int jdayCol = record.getInt(22);
    int yearCol = record.getInt(23);
    int monthCol = record.getInt(24);
    int dayCol = record.getInt(25);
    int timeCol = record.getInt(26);
    Properties timeProps = StringUtils.propertiesFromString(record.getString(27));
    int hourCol = record.getInt(28);
    int minuteCol = record.getInt(29);
    int secondCol = record.getInt(30);

    return new DateTimeSpecification(headerLines > 0, dateTimeCol, dateTimeProps, dateCol, dateProps, hoursFromStartCol, hoursFromStartProps,
        jdayTimeCol, jdayCol, yearCol, monthCol, dayCol, timeCol, timeProps, hourCol, minuteCol, secondCol);
  }

  /**
   * Get the variables measured by an instrument
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The variables
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws VariableNotFoundException If an invalid variable is configured for the instrument
   */
  public static List<InstrumentVariable> getVariables(Connection conn, long instrumentId)
    throws MissingParamException, VariableNotFoundException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    List<InstrumentVariable> variables = new ArrayList<InstrumentVariable>();
    SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_INSTRUMENT_VARIABLES_QUERY);
      stmt.setLong(1, instrumentId);
      records = stmt.executeQuery();
      while (records.next()) {
        variables.add(sensorConfig.getInstrumentVariable(records.getLong(1)));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting instrument variables", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return variables;
  }

  /**
   * Get the sensor and file column configuration for an instrument
   * @param conn A database connection
   * @param files The instrument's files
   * @param sensorConfiguration The sensor configuration
   * @param runTypeConfiguration The run type configuration
   * @return The assignments for the instrument
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If any required records are not found
   * @throws InstrumentException If any instrument values are invalid
   * @throws MissingParamException If any internal calls are missing required parameters
   */
  private static SensorAssignments getSensorAssignments(Connection conn,
    long instrumentId, InstrumentFileSet files, SensorsConfiguration sensorConfiguration,
    RunTypeCategoryConfiguration runTypeConfiguration) throws DatabaseException,
      RecordNotFoundException, InstrumentException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(files, "files");

    SensorAssignments assignments =
      SensorAssignments.makeSensorAssignmentsFromVariables(
        conn, getVariables(conn, instrumentId));

    List<PreparedStatement> stmts = new ArrayList<PreparedStatement>();
    List<ResultSet> records = new ArrayList<ResultSet>();

    try {
      for (FileDefinition file : files) {

        PreparedStatement stmt = conn.prepareStatement(GET_FILE_COLUMNS_QUERY);
        stmts.add(stmt);
        stmt.setLong(1, file.getDatabaseId());

        ResultSet columns = stmt.executeQuery();
        records.add(columns);
        int columnsRead = 0;
        while (columns.next()) {
          columnsRead++;

          long assignmentId = columns.getLong(1);
          int fileColumn = columns.getInt(2);
          boolean primarySensor = columns.getBoolean(3);
          long sensorType = columns.getLong(4);
          String sensorName = columns.getString(5);
          int valueColumn = columns.getInt(6);
          boolean dependsQuestionAnswer = columns.getBoolean(7);
          String missingValue = columns.getString(8);

          if (sensorType == SensorType.RUN_TYPE_ID) {
            file.setRunTypeColumn(fileColumn);
            getFileRunTypes(conn, file, runTypeConfiguration);
          } else {
            assignments.addAssignment(sensorType, new SensorAssignment(assignmentId, file.getFileDescription(), fileColumn, valueColumn, sensorName, primarySensor, dependsQuestionAnswer, missingValue));
          }
        }

        if (columnsRead == 0) {
          throw new RecordNotFoundException("No file columns found", "file_column", file.getDatabaseId());
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving file columns", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmts);
    }

    return assignments;
  }

  /**
   * Get the names of all run types of a given run type category in a given instrument
   * @param dataSource A data source
   * @param instrumentId The instrument's database ID
   * @param categoryCode The run type category code
   * @return The list of run types
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<String> getRunTypes(DataSource dataSource, long instrumentId, String categoryCode) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    List<String> runTypes = null;

    Connection conn = null;
    try {

      conn = dataSource.getConnection();
      runTypes = getRunTypes(conn, instrumentId, categoryCode);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting run types", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return runTypes;
  }

  /**
   * Get the names of all run types of a given run type category in a given instrument
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @param categoryCode The run type category code
   * @return The list of run types
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<String> getRunTypes(Connection conn, long instrumentId, String categoryCode) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(categoryCode, "categoryCode");

    List<String> runTypes = new ArrayList<String>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_RUN_TYPES_QUERY);
      stmt.setLong(1, instrumentId);
      stmt.setString(2, categoryCode);

      records = stmt.executeQuery();
      while (records.next()) {
        runTypes.add(records.getString(1));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting run types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return runTypes;
  }

  /**
   * Get a list of all the sensors on a particular instrument that require calibration.
   *
   * <p>
   *   Each sensor will be listed in the form of
   *   {@code <file>: <sensorName>}
   * </p>
   *
   * @param conn A database connection
   * @param instrumentId The instrument ID
   * @return The list of calibratable sensors
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<String> getCalibratableSensors(Connection conn, long instrumentId) throws MissingParamException, DatabaseException {

    List<String> result = new ArrayList<String>();

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_CALIBRATABLE_SENSORS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();
      while (records.next()) {
        result.add(records.getString(1));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting run types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }


    return Collections.unmodifiableList(result);
  }

  /**
   * Load the run types for a file from the database
   * @param conn A database connection
   * @param file The file whose run types are to be retrieved
   * @param runTypeConfig The run types configuration
   * @throws DatabaseException If a database error occurs
   * @throws InstrumentException If a stored run type category is not configured
   */
  private static void getFileRunTypes(Connection conn, FileDefinition file, RunTypeCategoryConfiguration runTypeConfig) throws DatabaseException, InstrumentException {
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_FILE_RUN_TYPES_QUERY);
      stmt.setLong(1, file.getDatabaseId());

      records = stmt.executeQuery();
      while (records.next()) {
        String runType = records.getString(1);
        String categoryCode = records.getString(2);
        String aliasTo = records.getString(3);

        if (categoryCode.equals(RunTypeCategory.ALIAS_CATEGORY.getCode())) {
          file.setRunTypeCategory(runType, aliasTo);
        } else {
          file.setRunTypeCategory(runType, runTypeConfig.getCategory(categoryCode));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving run types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }


  }

  /**
   * Store a run type assignment for a file
   * @param conn A database connection
   * @param fileId The database ID of the file definition to which the run type is assigned
   * @param runType The run type assignment
   * @return The statement used to store the assignment, so it can be closed as part of a larger transaction
   * @throws SQLException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static PreparedStatement storeFileRunType(
      Connection conn,
      long fileId,
      RunTypeAssignment runType) throws SQLException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(fileId, "fileId");
    MissingParam.checkMissing(runType, "runType");

    PreparedStatement runTypeStatement = conn.prepareStatement(CREATE_RUN_TYPE_STATEMENT);
    runTypeStatement.setLong(1, fileId);
    runTypeStatement.setString(2, runType.getRunType());

    if (runType.isAlias()) {
      runTypeStatement.setString(3, RunTypeCategory.ALIAS_CATEGORY.getCode());
      runTypeStatement.setString(4, runType.getAliasTo());
    } else {
      runTypeStatement.setString(3, runType.getCategory().getCode());
      runTypeStatement.setNull(4, Types.VARCHAR);
    }

    runTypeStatement.execute();
    return runTypeStatement;
  }

  /**
   * Store a set of run type assignments for a file
   * @param dataSource A data source
   * @param fileId The file's database ID
   * @param assignments The assignments to store
   * @throws SQLException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static void storeFileRunTypes(
      DataSource dataSource,
      long fileId,
      List<RunTypeAssignment> assignments) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(fileId, "fileDefinitionId");
    MissingParam.checkMissing(assignments, "runTypes", true);

    List<PreparedStatement> stmts = new ArrayList<PreparedStatement>(assignments.size());
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      for (RunTypeAssignment assignment : assignments) {
        stmts.add(storeFileRunType(conn, fileId, assignment));
      }

      conn.commit();
    } catch (SQLException e) {
      DatabaseUtils.rollBack(conn);
      throw new DatabaseException("Error while storing run type assignments", e);
    } finally {
      DatabaseUtils.closeStatements(stmts);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Get the list of all instruments that provide NRT data
   * @param dataSource A data source
   * @return The NRT instruments
   * @throws SQLException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public static List<NrtInstrument> getNrtInstruments(DataSource dataSource) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");

    List<NrtInstrument> result = new ArrayList<NrtInstrument>();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_NRT_INSTRUMENTS_QUERY);
      records = stmt.executeQuery();

      while(records.next()) {
        long id = records.getLong(1);
        String instrument = records.getString(2);
        String owner = records.getString(3);

        result.add(new NrtInstrument(id, instrument, owner));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving NRT instruments", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }


    return result;

  }

  /**
   * Determine whether an instrument with the specified ID exists
   * @param conn A database connection
   * @param id The instrument's database ID
   * @return {@code true} if the instrument exists; {@code false} if it does not
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static boolean instrumentExists(Connection conn, long id) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(id, "id");

    boolean exists = false;

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(INSTRUMENT_ID_EXISTS_QUERY);
      stmt.setLong(1, id);

      records = stmt.executeQuery();
      exists = records.next();
    } catch (SQLException e) {
      throw new DatabaseException("Error while checking instrument exists", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return exists;
  }

  /**
   * Determine whether an instrument with the specified ID exists
   * @param conn A database connection
   * @param id The instrument's database ID
   * @return {@code true} if the instrument exists; {@code false} if it does not
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the instrument does not exist
   */
  public static boolean isNrtInstrument(Connection conn, long id) throws MissingParamException, DatabaseException, RecordNotFoundException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(id, "id");

    boolean result = false;

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(NRT_INSTRUMENT_QUERY);
      stmt.setLong(1, id);

      records = stmt.executeQuery();
      if (records.next()) {
        result = records.getBoolean(1);
      } else {
        throw new RecordNotFoundException("Instrument " + id + " not found");
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while checking instrument exists", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Get the user who owns a given instrument
   * @param conn A database connection
   * @param id The instrument's database ID
   * @return The User object
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the instrument does not exist
   */
  public static User getInstrumentOwner(Connection conn, long id) throws MissingParamException, DatabaseException, RecordNotFoundException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(id, "id");

    User result = null;

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_INSTRUMENT_OWNER_QUERY);
      stmt.setLong(1, id);

      records = stmt.executeQuery();
      if (!records.next()) {
        throw new RecordNotFoundException("Instrument " + id + " not found");
      } else {
        result = UserDB.getUser(conn, records.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while looking up instrument ownder", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }
}
