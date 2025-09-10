package uk.ac.exeter.QuinCe.data.Instrument;

import java.io.IOException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.api.nrt.NrtInstrument;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.FileStoreException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.files.MissingRunType;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Database methods dealing with instruments
 */
public class InstrumentDB {
  /**
   * Statement for inserting an instrument record
   */
  private static final String CREATE_INSTRUMENT_STATEMENT = "INSERT INTO instrument ("
    + "owner, name, platform_name, platform_code, nrt, properties" // 5
    + ") VALUES (?, ?, ?, ?, ?, ?)";

  /**
   * Statement for inserting an instrument variable record
   */
  private static final String CREATE_INSTRUMENT_VARIABLE_STATEMENT = "INSERT INTO "
    + "instrument_variables (instrument_id, variable_id, properties) " // 3
    + "VALUES (?, ?, ?)";

  /**
   * Statement for inserting a file definition record
   */
  private static final String CREATE_FILE_DEFINITION_STATEMENT = "INSERT INTO file_definition ("
    + "instrument_id, description, column_separator, " // 3
    + "header_type, header_lines, header_end_string, " // 6
    + "column_header_rows, column_count, " // 8
    + "lon_spec, lat_spec, datetime_spec" // 11
    + ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /**
   * Statement for inserting a file column definition record
   */
  private static final String CREATE_FILE_COLUMN_STATEMENT = "INSERT INTO file_column ("
    + "file_definition_id, file_column, primary_sensor, sensor_type, " // 4
    + "sensor_name, depends_question_answer, missing_value" // 7
    + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

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
   * SQL query to get an instrument's base record
   */
  private static final String GET_INSTRUMENT_QUERY = "SELECT name, owner, " // 2
    + "platform_name, platform_code, nrt, last_nrt_export, properties, created " // 5
    + "FROM instrument WHERE id = ?";

  /**
   * Query to get the variables measured by an instrument
   */
  private static final String GET_INSTRUMENT_VARIABLES_QUERY = "SELECT "
    + "variable_id, properties FROM instrument_variables WHERE instrument_id = ?";

  /**
   * SQL query to get the file definitions for an instrument
   */
  private static final String GET_FILE_DEFINITIONS_QUERY = "SELECT "
    + "id, description, column_separator, " // 3
    + "header_type, header_lines, header_end_string, column_header_rows, " // 7
    + "column_count, lon_spec, lat_spec, datetime_spec " // 11
    + "FROM file_definition WHERE instrument_id = ? ORDER BY description";

  /**
   * SQL query to get the file column assignments for a file
   */
  private static final String GET_FILE_COLUMNS_QUERY = "SELECT "
    + "id, file_column, primary_sensor, sensor_type, sensor_name, " // 5
    + "depends_question_answer, missing_value " // 7
    + "FROM file_column WHERE file_definition_id = ?";

  /**
   * Query to get the list of sensors that require calibration for a given
   * instrument
   */
  private static final String GET_CALIBRATABLE_SENSORS_QUERY = "SELECT "
    + "c.id AS id, c.sensor_type as sensor_type, f.description AS file, c.sensor_name AS sensor "
    + "FROM file_definition AS f INNER JOIN file_column AS c ON c.file_definition_id = f.id "
    + "WHERE f.instrument_id = ? ORDER BY file, sensor";

  /**
   * Query for retrieving the list of all instruments that provide NRT data
   */
  private static final String GET_NRT_INSTRUMENTS_QUERY = "SELECT "
    + "i.id, CONCAT(i.platform_name, ': ', i.name) as instrument, "
    + "CONCAT(u.surname, ', ', u.firstname) AS name " + "FROM instrument AS i "
    + "INNER JOIN user AS u ON i.owner = u.id " + "WHERE i.nrt = 1 "
    + "ORDER BY name ASC, instrument ASC";

  /**
   * Query used to determine if an instrument with a specified ID exists in the
   * database
   */
  private static final String INSTRUMENT_ID_EXISTS_QUERY = "SELECT "
    + " id FROM instrument WHERE id = ?";

  /**
   * Query used to determine if an instrument with a specified ID allows NRT
   * datasets
   */
  private static final String NRT_INSTRUMENT_QUERY = "SELECT "
    + "nrt FROM instrument WHERE id = ?";

  /**
   * Query to get the owner ID for an instrument
   */
  private static final String GET_INSTRUMENT_OWNER_QUERY = "SELECT "
    + "owner FROM instrument WHERE id = ?";

  private static final String GET_ALL_VARIABLES_QUERY = "SELECT "
    + "id FROM variables WHERE visible = 1";

  /**
   * Get the sensor or diagnostic column details for a given instrument
   */
  private static final String GET_COLUMNS_QUERY = "SELECT "
    + "fc.id, fc.sensor_name, st.id, st.name FROM sensor_types st "
    + "LEFT JOIN file_column fc ON (fc.sensor_type = st.id) "
    + "INNER JOIN file_definition fd ON (fc.file_definition_id = fd.id)"
    + "WHERE fd.instrument_id = ? "
    + "ORDER BY st.display_order, st.name, fc.sensor_name";

  /**
   * Query to see if an instrument exists with the given owner and name.
   */
  private static final String INSTRUMENT_EXISTS_QUERY = "SELECT "
    + "id FROM instrument WHERE owner = ? AND platform_name = ? AND name = ?";

  private static final String INSTRUMENT_LIST_QUERY = "SELECT "
    + "i.id, i.name, i.owner, i.platform_name, i.platform_code, i.nrt, "
    + "i.last_nrt_export, i.properties, i.created, "
    + "iv.variable_id, iv.properties, "
    + "CONCAT(u.surname, ', ', u.firstname) AS owner_name "
    + "FROM instrument i LEFT JOIN instrument_variables iv ON i.id = iv.instrument_id "
    + "INNER JOIN user u on i.owner = u.id " + "WHERE i.id IN ("
    + "SELECT id FROM instrument WHERE OWNER = ? " + "UNION "
    + "SELECT instrument_id FROM shared_instruments WHERE shared_with = ?"
    + ") ORDER BY owner_name, i.owner, i.platform_name, i.name";

  private static final String ALL_INSTRUMENT_LIST_QUERY = "SELECT "
    + "i.id, i.name, i.owner, i.platform_name, i.platform_code, i.nrt, "
    + "i.last_nrt_export, i.properties, i.created, "
    + "iv.variable_id, iv.properties, "
    + "CONCAT(u.surname, ', ', u.firstname) AS owner_name "
    + "FROM instrument i LEFT JOIN instrument_variables iv ON i.id = iv.instrument_id "
    + "INNER JOIN user u on i.owner = u.id "
    + "ORDER BY owner_name, i.owner, i.platform_name, i.name";

  private static final String PLATFORMS_QUERY = "SELECT "
    + "platform_name, platform_code FROM instrument "
    + "WHERE owner = ? ORDER BY created ASC";

  private static final String ALL_PLATFORMS_QUERY = "SELECT "
    + "platform_name, platform_code FROM instrument ORDER BY created ASC";

  private static final String SAVE_PROPERTIES_STATEMENT = "UPDATE instrument "
    + "SET properties = ? WHERE id = ?";

  private static final String DELETE_CALIBRATIONS_STATEMENT = "DELETE FROM calibration "
    + "WHERE instrument_id = ?";

  private static final String DELETE_RUN_TYPES_STATEMENT = "DELETE FROM run_type "
    + "WHERE file_definition_id IN "
    + "(SELECT id FROM file_definition WHERE instrument_id = ?)";

  private static final String DELETE_FILE_COLUMNS_STATEMENT = "DELETE FROM file_column "
    + "WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = ?)";

  private static final String DELETE_FILE_DEFINITIONS_STATEMENT = "DELETE FROM "
    + "file_definition WHERE instrument_id = ?";

  private static final String DELETE_SHARED_INSTRUMENTS_STATEMENT = "DELETE FROM "
    + "shared_instruments WHERE instrument_id = ?";

  private static final String DELETE_INSTRUMENT_VARIABLES_STATEMENT = "DELETE FROM "
    + "instrument_variables WHERE instrument_id = ?";

  private static final String DELETE_INSTRUMENT_STATEMENT = "DELETE FROM "
    + "instrument WHERE id = ?";

  private static final String SHARED_USERS_QUERY = "SELECT"
    + " shared_with FROM shared_instruments si"
    + " INNER JOIN user u ON si.shared_with = u.id"
    + " WHERE si.instrument_id = ? ORDER BY u.surname, u.firstname";

  private static final String SHARED_INSTRUMENTS_QUERY = "SELECT "
    + " instrument_id, shared_with FROM shared_instruments si"
    + " INNER JOIN user u ON si.shared_with = u.id"
    + " ORDER BY u.surname, u.firstname";

  private static final String ADD_SHARE_STATEMENT = "INSERT INTO"
    + " shared_instruments (instrument_id, shared_with)" + " VALUES (?, ?)";

  private static final String REMOVE_SHARE_STATEMENT = "DELETE FROM"
    + " shared_instruments WHERE instrument_id = ? AND shared_with = ?";

  private static final String SET_OWNER_STATEMENT = "UPDATE instrument"
    + " set owner = ? WHERE id = ?";

  /**
   * Store a new instrument in the database
   *
   * @param dataSource
   *          A data source
   * @param instrument
   *          The instrument
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws InstrumentException
   *           If the Instrument object is invalid
   * @throws DatabaseException
   *           If a database error occurs
   * @throws IOException
   *           If any of the data cannot be converted for storage in the
   *           database
   */
  public static void storeInstrument(DataSource dataSource,
    Instrument instrument) throws MissingParamException, InstrumentException,
    DatabaseException, IOException {

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
        throw new DatabaseException(
          "Instrument record was not created in the database");
      } else {
        long instrumentId = instrumentKey.getLong(1);
        instrument.setDatabaseId(instrumentId);

        // Store the instrument's variables
        for (Variable variable : instrument.getVariables()) {
          PreparedStatement variableStmt = conn
            .prepareStatement(CREATE_INSTRUMENT_VARIABLE_STATEMENT);
          variableStmt.setLong(1, instrumentId);
          variableStmt.setLong(2, variable.getId());
          variableStmt.setString(3,
            new Gson().toJson(instrument.getVariableProperties(variable)));
          variableStmt.execute();
          subStatements.add(variableStmt);
        }

        // Store the database IDs for all the file definitions
        Map<String, Long> fileDefinitionIds = new HashMap<String, Long>(
          instrument.getFileDefinitions().size());

        // Now store the file definitions
        for (FileDefinition file : instrument.getFileDefinitions()) {
          PreparedStatement fileStatement = makeCreateFileDefinitionStatement(
            conn, file, instrumentId);
          subStatements.add(fileStatement);

          fileStatement.execute();
          ResultSet fileKey = fileStatement.getGeneratedKeys();
          keyResultSets.add(fileKey);

          if (!fileKey.next()) {
            throw new DatabaseException(
              "File Definition record was not created in the database");
          } else {
            long fileId = fileKey.getLong(1);
            fileDefinitionIds.put(file.getFileDescription(), fileId);

            // Run Types
            if (null != file.getRunTypes()) {
              for (RunTypeAssignment assignment : file.getRunTypes().values()) {
                PreparedStatement runTypeStatement = storeFileRunType(conn,
                  fileId, assignment);
                subStatements.add(runTypeStatement);
              }
            }
          }
        }

        for (Map.Entry<SensorType, TreeSet<SensorAssignment>> sensorAssignmentsEntry : instrument
          .getSensorAssignments().entrySet()) {

          SensorType sensorType = sensorAssignmentsEntry.getKey();

          for (SensorAssignment assignment : sensorAssignmentsEntry
            .getValue()) {

            PreparedStatement fileColumnStatement = conn.prepareStatement(
              CREATE_FILE_COLUMN_STATEMENT, Statement.RETURN_GENERATED_KEYS);
            fileColumnStatement.setLong(1,
              fileDefinitionIds.get(assignment.getDataFile()));
            fileColumnStatement.setInt(2, assignment.getColumn());
            fileColumnStatement.setBoolean(3, assignment.isPrimary());
            fileColumnStatement.setLong(4, sensorType.getId());
            fileColumnStatement.setString(5, assignment.getSensorName());
            fileColumnStatement.setBoolean(6,
              assignment.getDependsQuestionAnswer());
            fileColumnStatement.setString(7, assignment.getMissingValue());

            fileColumnStatement.execute();
            ResultSet fileColumnKey = fileColumnStatement.getGeneratedKeys();
            if (!fileColumnKey.next()) {
              throw new DatabaseException(
                "File Column record was not created in the database");
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

      throw new DatabaseException("Exception while storing instrument", e,
        rollbackOK);
    } finally {
      if (null != conn) {
        try {
          conn.setAutoCommit(true);
        } catch (SQLException e) {
          throw new DatabaseException("Unable to reset connection autocommit",
            e);
        }
      }
      DatabaseUtils.closeResultSets(keyResultSets);
      DatabaseUtils.closeResultSets(instrumentKey);
      DatabaseUtils.closeStatements(subStatements);
      DatabaseUtils.closeStatements(instrumentStatement);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Make the statement used to create an instrument record in the database
   *
   * @param conn
   *          A database connection
   * @param instrument
   *          The instrument
   * @return The database statement
   * @throws SQLException
   *           If an error occurs while building the statement
   */
  private static PreparedStatement makeCreateInstrumentStatement(
    Connection conn, Instrument instrument) throws SQLException {
    PreparedStatement stmt = conn.prepareStatement(CREATE_INSTRUMENT_STATEMENT,
      Statement.RETURN_GENERATED_KEYS);
    stmt.setLong(1, instrument.getOwner().getDatabaseID()); // owner
    stmt.setString(2, instrument.getName()); // name
    stmt.setString(3, instrument.getPlatformName()); // platform_name
    stmt.setString(4, instrument.getPlatformCode()); // platform_code
    stmt.setBoolean(5, instrument.getNrt()); // nrt
    stmt.setString(6, instrument.getPropertiesJson()); // attributes

    return stmt;
  }

  /**
   * Create a statement for adding a file definition to the database
   *
   * @param conn
   *          A database connection
   * @param file
   *          The file definition
   * @param instrumentId
   *          The database ID of the instrument to which the file belongs
   * @return The statement
   * @throws SQLException
   *           If the statement cannot be built
   * @throws IOException
   *           If any Properties objects cannot be serialized into Strings for
   *           storage
   */
  private static PreparedStatement makeCreateFileDefinitionStatement(
    Connection conn, FileDefinition file, long instrumentId)
    throws SQLException, IOException {

    Gson gson = new Gson();

    PreparedStatement stmt = conn.prepareStatement(
      CREATE_FILE_DEFINITION_STATEMENT, Statement.RETURN_GENERATED_KEYS);

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
    stmt.setString(9, gson.toJson(file.getLongitudeSpecification()));
    stmt.setString(10, gson.toJson(file.getLatitudeSpecification()));
    stmt.setString(11, gson.toJson(file.getDateTimeSpecification()));

    return stmt;
  }

  /**
   * Returns a list of instruments owned by a given user. The list contains
   * {@link InstrumentStub} objects, which just contain the details required for
   * lists of instruments in the UI.
   *
   * If the specified owner is an administrator, the returned list will contain
   * all instruments in the system
   *
   * The list is ordered by the name of the instrument.
   *
   * @param dataSource
   *          A data source
   * @param owner
   *          The owner whose instruments are to be listed
   * @return The list of instruments
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurred
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws VariableNotFoundException
   * @throws SensorGroupsException
   */
  public static List<Instrument> getInstrumentList(DataSource dataSource,
    User owner)
    throws MissingParamException, DatabaseException, VariableNotFoundException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(owner, "owner");

    Connection conn = null;
    List<Instrument> result = null;

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
   * @param conn
   *          A database connection
   * @param ownerId
   *          The user's database ID
   * @return The list of instruments
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurred
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws VariableNotFoundException
   * @throws SensorGroupsException
   */
  public static List<Instrument> getInstrumentList(Connection conn,
    long ownerId)
    throws MissingParamException, DatabaseException, VariableNotFoundException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {

    MissingParam.checkMissing(conn, "conn");

    Map<Long, List<Long>> sharedInstruments = getSharedInstruments(conn);

    List<Instrument> result = new ArrayList<Instrument>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      // -1 means get all instruments
      if (ownerId == -1) {
        stmt = conn.prepareStatement(ALL_INSTRUMENT_LIST_QUERY);
      } else {
        stmt = conn.prepareStatement(INSTRUMENT_LIST_QUERY);
        stmt.setLong(1, ownerId);
        stmt.setLong(2, ownerId);
      }

      // The queries above select one row for each variable in each instrument,
      // meaning we get multiple rows with the details for each of its
      // variables.
      records = stmt.executeQuery();

      long currentInstrument = -1;
      String name = null;
      long owner = -1;
      String platformName = null;
      String platformCode = null;
      boolean nrt = false;
      LocalDateTime lastNrtExport = null;
      String propertiesJson = null;
      List<Long> variables = null;
      Map<Long, String> variableProperties = null;
      LocalDateTime created = null;

      while (records.next()) {

        long instrumentId = records.getLong(1);
        if (instrumentId != currentInstrument) {

          if (currentInstrument != -1) {
            result.add(createInstrument(conn, owner, currentInstrument, name,
              sharedInstruments.get(currentInstrument), variables,
              variableProperties, platformName, platformCode, nrt,
              lastNrtExport, propertiesJson, created));
          }

          currentInstrument = instrumentId;
          name = records.getString(2);
          owner = records.getLong(3);
          platformName = records.getString(4);
          platformCode = records.getString(5);
          nrt = records.getBoolean(6);
          lastNrtExport = DateTimeUtils.longToDate(records.getLong(7));
          propertiesJson = records.getString(8);
          variables = new ArrayList<Long>();
          variableProperties = new HashMap<Long, String>();
          created = records.getTimestamp(9).toLocalDateTime();
        }

        variables.add(records.getLong(10));
        variableProperties.put(records.getLong(10), records.getString(11));
      }

      // Create the last instrument, if there is one
      if (currentInstrument != -1) {
        result.add(createInstrument(conn, owner, currentInstrument, name,
          sharedInstruments.get(currentInstrument), variables,
          variableProperties, platformName, platformCode, nrt, lastNrtExport,
          propertiesJson, created));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving instrument list", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Get the information of {@link Instrument}s between users.
   *
   * <p>
   * Returns a {@link Map} of {@code Instrument ID -> User IDs}, where the user
   * IDs are the users with which the owner has shared the {@link Instrument}.
   * The owner is not included in the {@link Map}.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @return The sharing info.
   * @throws DatabaseException
   */
  private static Map<Long, List<Long>> getSharedInstruments(Connection conn)
    throws DatabaseException {
    Map<Long, List<Long>> sharedInstruments = new HashMap<Long, List<Long>>();

    try (
      PreparedStatement stmt = conn.prepareStatement(SHARED_INSTRUMENTS_QUERY);
      ResultSet records = stmt.executeQuery()) {

      while (records.next()) {
        Long instrumentID = records.getLong(1);
        if (!sharedInstruments.containsKey(instrumentID)) {
          sharedInstruments.put(instrumentID, new ArrayList<Long>());
        }

        sharedInstruments.get(instrumentID).add(records.getLong(2));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error getting instrument sharing info", e);
    }

    return sharedInstruments;
  }

  private static Instrument createInstrument(Connection conn, long ownerId,
    long id, String name, List<Long> sharedWith, List<Long> variableIds,
    Map<Long, String> variableProperties, String platformName,
    String platformCode, boolean nrt, LocalDateTime lastNrtExport,
    String propertiesJson, LocalDateTime created)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException, VariableNotFoundException, SensorGroupsException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();
    RunTypeCategoryConfiguration runTypeConfig = ResourceManager.getInstance()
      .getRunTypeCategoryConfiguration();

    // Get the file definitions and sensor assignments
    InstrumentFileSet files = getFileDefinitions(conn, id);
    SensorAssignments sensorAssignments = getSensorAssignments(conn, id, files,
      sensorConfig, runTypeConfig);

    List<Variable> variables = new ArrayList<Variable>(variableIds.size());
    for (long varId : variableIds) {
      variables.add(sensorConfig.getInstrumentVariable(varId));
    }

    Map<Variable, Properties> processedVariableProperties = new HashMap<Variable, Properties>();
    for (Map.Entry<Long, String> entry : variableProperties.entrySet()) {
      Variable var = sensorConfig.getInstrumentVariable(entry.getKey());
      Properties props = new Gson().fromJson(entry.getValue(),
        Properties.class);

      processedVariableProperties.put(var, props);
    }

    return new Instrument(UserDB.getUser(conn, ownerId), id, name, sharedWith,
      files, variables, processedVariableProperties, sensorAssignments,
      platformName, platformCode, nrt, lastNrtExport, propertiesJson, created);
  }

  /**
   * Get the list of all instruments in the system
   *
   * See {@link #getInstrumentList(DataSource, User)}
   *
   * @param conn
   *          A database connection
   * @param ownerId
   *          The user's database ID
   * @return The list of instruments
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurred
   * @throws InstrumentException
   * @throws RecordNotFoundException
   * @throws VariableNotFoundException
   * @throws SensorGroupsException
   */
  private static List<Instrument> getAllUsersInstrumentList(Connection conn)
    throws MissingParamException, DatabaseException, VariableNotFoundException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {

    return getInstrumentList(conn, -1);
  }

  /**
   * Determine whether an instrument with a given name and owner exists
   *
   * @param dataSource
   *          A data source
   * @param owner
   *          The owner
   * @param name
   *          The instrument name
   * @return {@code true} if the instrument exists; {@code false} if it does not
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static boolean instrumentExists(DataSource dataSource, User owner,
    String platformName, String name)
    throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(owner, "owner");

    boolean exists = false;

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(INSTRUMENT_EXISTS_QUERY);) {

      stmt.setLong(1, owner.getDatabaseID());
      stmt.setString(2, platformName);
      stmt.setString(3, name);

      try (ResultSet records = stmt.executeQuery()) {
        if (records.next()) {
          exists = true;
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting instrument", e);
    }

    return exists;
  }

  public static Instrument getInstrument(DataSource dataSource,
    long instrumentId) throws DatabaseException, MissingParamException,
    RecordNotFoundException, InstrumentException, SensorGroupsException {
    try (Connection conn = dataSource.getConnection()) {
      return getInstrument(conn, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting instrument", e);
    }
  }

  /**
   * Returns a complete instrument object for the specified instrument ID
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument ID
   * @param sensorConfiguration
   *          The sensors configuration
   * @param runTypeConfiguration
   *          The run type category configuration
   * @return The complete Instrument object
   * @throws DatabaseException
   *           If an error occurs while retrieving the instrument details
   * @throws RecordNotFoundException
   *           If the specified instrument cannot be found
   * @throws InstrumentException
   *           If any instrument values are invalid
   * @throws SensorGroupsException
   */
  public static Instrument getInstrument(Connection conn, long instrumentId)
    throws DatabaseException, RecordNotFoundException, InstrumentException,
    SensorGroupsException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();
    RunTypeCategoryConfiguration runTypeConfig = ResourceManager.getInstance()
      .getRunTypeCategoryConfiguration();

    Instrument instrument = null;

    List<PreparedStatement> stmts = new ArrayList<PreparedStatement>();
    List<ResultSet> resultSets = new ArrayList<ResultSet>();

    try {
      // Get the shared users
      List<Long> sharedUsers = new ArrayList<Long>();

      PreparedStatement sharedStmt = conn.prepareStatement(SHARED_USERS_QUERY);
      sharedStmt.setLong(1, instrumentId);
      stmts.add(sharedStmt);

      // Get the raw instrument data
      PreparedStatement instrStmt = conn.prepareStatement(GET_INSTRUMENT_QUERY);
      instrStmt.setLong(1, instrumentId);
      stmts.add(instrStmt);

      ResultSet shareRecords = sharedStmt.executeQuery();
      while (shareRecords.next()) {
        sharedUsers.add(shareRecords.getLong(1));
      }

      ResultSet instrumentRecord = instrStmt.executeQuery();
      resultSets.add(instrumentRecord);

      if (!instrumentRecord.next()) {
        throw new RecordNotFoundException("Instrument record not found",
          "instrument", instrumentId);
      } else {
        // Read in the instrument details
        String name = instrumentRecord.getString(1);
        long owner = instrumentRecord.getLong(2);
        String platformName = instrumentRecord.getString(3);
        String platformCode = instrumentRecord.getString(4);
        boolean nrt = instrumentRecord.getBoolean(5);

        LocalDateTime lastNrtExport = DateTimeUtils
          .longToDate(instrumentRecord.getLong(6));

        String propertiesJson = instrumentRecord.getString(7);

        LocalDateTime created = instrumentRecord.getTimestamp(8)
          .toLocalDateTime();

        // Now get the file definitions
        InstrumentFileSet files = getFileDefinitions(conn, instrumentId);

        // The variables
        List<Variable> variables = new ArrayList<Variable>();
        Map<Variable, Properties> variableProperties = new HashMap<Variable, Properties>();
        loadInstrumentVariables(conn, instrumentId, variables,
          variableProperties);

        // Now the sensor assignments
        SensorAssignments sensorAssignments = getSensorAssignments(conn,
          instrumentId, files, sensorConfig, runTypeConfig);

        instrument = new Instrument(UserDB.getUser(conn, owner), instrumentId,
          name, sharedUsers, files, variables, variableProperties,
          sensorAssignments, platformName, platformCode, nrt, lastNrtExport,
          propertiesJson, created);
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
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's ID
   * @return The file definitions
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If no file definitions are stored for the instrument
   * @throws InstrumentException
   */
  public static InstrumentFileSet getFileDefinitions(Connection conn,
    long instrumentId) throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    InstrumentFileSet fileSet = new InstrumentFileSet();

    Gson gson = new Gson();
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

        LongitudeSpecification lonSpec = gson.fromJson(records.getString(9),
          LongitudeSpecification.class);
        LatitudeSpecification latSpec = gson.fromJson(records.getString(10),
          LatitudeSpecification.class);
        DateTimeSpecification dateTimeSpec = gson
          .fromJson(records.getString(11), DateTimeSpecification.class);

        FileDefinition fileDefinition = new FileDefinition(id, description,
          separator, headerType, headerLines, headerEndString, columnHeaderRows,
          columnCount, lonSpec, latSpec, dateTimeSpec, fileSet);

        fileSet.add(fileDefinition);

        // Load in the sensors configuration. As part of this, the file
        // definitions
        // will be updated with column information.
        getSensorAssignments(conn, instrumentId, fileSet,
          ResourceManager.getInstance().getSensorsConfiguration(),
          ResourceManager.getInstance().getRunTypeCategoryConfiguration());
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error reading file definitions", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    if (fileSet.size() == 0) {
      throw new RecordNotFoundException(
        "No file definitions found for instrument " + instrumentId);
    }

    return fileSet;
  }

  /**
   * Get the variables measured by an instrument
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @return The variables
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws VariableNotFoundException
   *           If an invalid variable is configured for the instrument
   */
  public static List<Variable> getVariables(Connection conn, long instrumentId)
    throws MissingParamException, VariableNotFoundException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    List<Variable> variables = new ArrayList<Variable>();
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();
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
      throw new DatabaseException("Error while getting instrument variables",
        e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return variables;
  }

  private static void loadInstrumentVariables(Connection conn,
    long instrumentId, List<Variable> variables,
    Map<Variable, Properties> variableProperties)
    throws DatabaseException, VariableNotFoundException {

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    try (PreparedStatement stmt = conn
      .prepareStatement(GET_INSTRUMENT_VARIABLES_QUERY)) {

      stmt.setLong(1, instrumentId);

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {
          Variable variable = sensorConfig
            .getInstrumentVariable(records.getLong(1));
          variables.add(variable);
          variableProperties.put(variable,
            new Gson().fromJson(records.getString(2), Properties.class));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error getting instrument variable details",
        e);
    }

  }

  /**
   * Get the sensor and file column configuration for an instrument
   *
   * @param conn
   *          A database connection
   * @param files
   *          The instrument's files
   * @param sensorConfiguration
   *          The sensor configuration
   * @param runTypeConfiguration
   *          The run type configuration
   * @return The assignments for the instrument
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If any required records are not found
   * @throws InstrumentException
   *           If any instrument values are invalid
   * @throws MissingParamException
   *           If any internal calls are missing required parameters
   */
  private static SensorAssignments getSensorAssignments(Connection conn,
    long instrumentId, InstrumentFileSet files,
    SensorsConfiguration sensorConfiguration,
    RunTypeCategoryConfiguration runTypeConfiguration) throws DatabaseException,
    RecordNotFoundException, InstrumentException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(files, "files");

    SensorAssignments assignments = SensorAssignments
      .makeSensorAssignmentsFromVariables(conn,
        getVariables(conn, instrumentId));

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
          SensorType sensorType = ResourceManager.getInstance()
            .getSensorsConfiguration().getSensorType(columns.getLong(4));
          String sensorName = columns.getString(5);
          boolean dependsQuestionAnswer = columns.getBoolean(6);
          String missingValue = columns.getString(7);

          assignments.addAssignment(new SensorAssignment(assignmentId,
            file.getFileDescription(), fileColumn, sensorType, sensorName,
            primarySensor, dependsQuestionAnswer, missingValue), true);

          // Add the run type assignments to the file definition
          if (sensorType.getId() == SensorType.RUN_TYPE_ID) {
            addFileRunTypes(conn, file, fileColumn);
          }
        }

        // If there's no columns for this file, something is wrong.
        // Although it might just have position data.
        if (columnsRead == 0 && !file.hasPosition()) {
          throw new RecordNotFoundException("No file columns found",
            "file_column", file.getDatabaseId());
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
   * Get the names of all run types of a given run type category in a given
   * instrument
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument's database ID
   * @param categoryCode
   *          The run type category code
   * @return The list of run types
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static List<String> getRunTypes(DataSource dataSource,
    long instrumentId, long categoryType)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    List<String> runTypes = null;

    Connection conn = null;
    try {

      conn = dataSource.getConnection();
      runTypes = getRunTypes(conn, instrumentId, categoryType);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting run types", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return runTypes;
  }

  /**
   * Get the names of all run types of a given run type category in a given
   * instrument
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @param categoryCode
   *          The run type category code
   * @return The list of run types
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static List<String> getRunTypes(Connection conn, long instrumentId,
    long categoryType) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    List<String> runTypes = new ArrayList<String>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_RUN_TYPES_QUERY);
      stmt.setLong(1, instrumentId);
      stmt.setLong(2, categoryType);

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
   * Get a list of all the sensors on a particular instrument that require
   * calibration.
   *
   * <p>
   * Each sensor will be listed in the form of {@code <file>: <sensorName>}
   * </p>
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument ID
   * @return The list of calibratable sensors
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   * @throws InstrumentException
   */
  public static Map<String, String> getCalibratableSensors(Connection conn,
    long instrumentId) throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    Map<String, String> result = new LinkedHashMap<String, String>();

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      InstrumentFileSet files = getFileDefinitions(conn, instrumentId);

      stmt = conn.prepareStatement(GET_CALIBRATABLE_SENSORS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();
      while (records.next()) {
        if (records.getLong(2) != SensorType.RUN_TYPE_ID) {
          if (files.size() > 1) {
            result.put(records.getString(1),
              records.getString(3) + ": " + records.getString(4));
          } else {
            result.put(records.getString(1), records.getString(4));
          }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting run types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Load the run types for a file definition from the database
   *
   * @param conn
   *          A database connection
   * @param file
   *          The file whose run types are to be retrieved
   * @param runTypeConfig
   *          The run types configuration
   * @throws DatabaseException
   *           If a database error occurs
   * @throws InstrumentException
   *           If a stored run type category is not configured
   */
  private static void addFileRunTypes(Connection conn, FileDefinition file,
    int column) throws DatabaseException, InstrumentException {
    PreparedStatement stmt = null;
    ResultSet records = null;

    RunTypeCategoryConfiguration runTypeConfig = ResourceManager.getInstance()
      .getRunTypeCategoryConfiguration();

    try {
      stmt = conn.prepareStatement(GET_FILE_RUN_TYPES_QUERY);
      stmt.setLong(1, file.getDatabaseId());

      records = stmt.executeQuery();
      RunTypeAssignments runTypes = null;

      while (records.next()) {

        if (null == runTypes) {
          runTypes = new RunTypeAssignments(column);
        }

        String runName = records.getString(1);
        long categoryCode = records.getLong(2);
        String aliasTo = records.getString(3);

        RunTypeAssignment assignment = null;

        if (categoryCode == RunTypeCategory.ALIAS.getType()) {
          assignment = new RunTypeAssignment(runName, aliasTo);
        } else {
          assignment = new RunTypeAssignment(runName,
            runTypeConfig.getCategory(categoryCode));
        }

        runTypes.put(runName, assignment);
      }

      file.setRunTypes(runTypes);

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving run types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

  }

  /**
   * Store a run type assignment for a file
   *
   * @param conn
   *          A database connection
   * @param fileId
   *          The database ID of the file definition to which the run type is
   *          assigned
   * @param runType
   *          The run type assignment
   * @return The statement used to store the assignment, so it can be closed as
   *         part of a larger transaction
   * @throws SQLException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static PreparedStatement storeFileRunType(Connection conn, long fileId,
    RunTypeAssignment runType) throws SQLException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(fileId, "fileId");
    MissingParam.checkMissing(runType, "runType");

    PreparedStatement runTypeStatement = conn
      .prepareStatement(CREATE_RUN_TYPE_STATEMENT);
    runTypeStatement.setLong(1, fileId);
    runTypeStatement.setString(2, runType.getRunName());

    if (runType.isAlias()) {
      runTypeStatement.setLong(3, RunTypeCategory.ALIAS.getType());
      runTypeStatement.setString(4, runType.getAliasTo());
    } else {
      runTypeStatement.setLong(3, runType.getCategory().getType());
      runTypeStatement.setNull(4, Types.VARCHAR);
    }

    runTypeStatement.execute();
    return runTypeStatement;
  }

  /**
   * Store a set of run type assignments for a file
   *
   * @param dataSource
   *          A data source
   * @param fileId
   *          The file's database ID
   * @param assignments
   *          The assignments to store
   * @throws SQLException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static void storeFileRunTypes(DataSource dataSource,
    Collection<MissingRunType> runTypes)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(runTypes, "runTypes", true);

    List<PreparedStatement> stmts = new ArrayList<PreparedStatement>(
      runTypes.size());
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      for (MissingRunType runType : runTypes) {
        stmts.add(storeFileRunType(conn,
          runType.getFileDefinition().getDatabaseId(), runType.getRunType()));
      }

      conn.commit();
    } catch (SQLException e) {
      DatabaseUtils.rollBack(conn);
      throw new DatabaseException("Error while storing run type assignments",
        e);
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException e) {
        throw new DatabaseException("Unable to reset connection autocommit", e);
      }
      DatabaseUtils.closeStatements(stmts);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Get the list of all instruments that provide NRT data
   *
   * @param dataSource
   *          A data source
   * @return The NRT instruments
   * @throws SQLException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public static List<NrtInstrument> getNrtInstruments(DataSource dataSource)
    throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");

    List<NrtInstrument> result = new ArrayList<NrtInstrument>();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_NRT_INSTRUMENTS_QUERY);
      records = stmt.executeQuery();

      while (records.next()) {
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
   *
   * @param conn
   *          A database connection
   * @param id
   *          The instrument's database ID
   * @return {@code true} if the instrument exists; {@code false} if it does not
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static boolean instrumentExists(Connection conn, long id)
    throws MissingParamException, DatabaseException {
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
   *
   * @param conn
   *          A database connection
   * @param id
   *          The instrument's database ID
   * @return {@code true} if the instrument exists; {@code false} if it does not
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If the instrument does not exist
   */
  public static boolean isNrtInstrument(Connection conn, long id)
    throws MissingParamException, DatabaseException, RecordNotFoundException {
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
   *
   * @param conn
   *          A database connection
   * @param id
   *          The instrument's database ID
   * @return The User object
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If the instrument does not exist
   */
  public static User getInstrumentOwner(Connection conn, long id)
    throws MissingParamException, DatabaseException, RecordNotFoundException {
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
      throw new DatabaseException("Error while looking up instrument ownder",
        e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Get all the variables registered in the application
   *
   * @param dataSource
   *          A data source
   * @return The variables
   * @throws DatabaseException
   *           If the variables cannot be retrieved
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws VariableNotFoundException
   */
  public static List<Variable> getAllVariables(DataSource dataSource)
    throws DatabaseException, MissingParamException, VariableNotFoundException {
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      return getAllVariables(conn);
    } catch (SQLException e) {
      throw new DatabaseException("Error while reading variables", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  public static List<Variable> getAllVariables(Connection conn)
    throws MissingParamException, DatabaseException, VariableNotFoundException {

    return getAllVariables(conn,
      ResourceManager.getInstance().getSensorsConfiguration());
  }

  /**
   * Get all the variables registered in the application
   *
   * @param conn
   *          A database connection
   * @return The variables
   * @throws DatabaseException
   *           If the variables cannot be retrieved
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws VariableNotFoundException
   */
  public static List<Variable> getAllVariables(Connection conn,
    SensorsConfiguration sensorConfig)
    throws MissingParamException, DatabaseException, VariableNotFoundException {

    MissingParam.checkMissing(conn, "conn");

    List<Variable> variables = new ArrayList<Variable>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_ALL_VARIABLES_QUERY);
      records = stmt.executeQuery();
      while (records.next()) {
        variables.add(sensorConfig.getInstrumentVariable(records.getLong(1)));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while reading variables", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return variables;
  }

  /**
   * Get the column details for a given instrument, either for sensor values or
   * diagnostic values
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument's database ID
   * @param diagnostic
   *          If {@code true}, returns diagnostic columns; otherwise returns
   *          sensor value columns
   * @return The column details
   * @throws DatabaseException
   *           If the variables cannot be retrieved
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws SensorTypeNotFoundException
   */
  public static List<FileColumn> getSensorColumns(DataSource dataSource,
    long instrumentId) throws MissingParamException, DatabaseException,
    SensorTypeNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;
    List<FileColumn> result = new ArrayList<FileColumn>();

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_COLUMNS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {
        long columnId = records.getLong(1);
        String columnName = records.getString(2);
        long sensorTypeId = records.getLong(3);

        result.add(new FileColumn(columnId, columnName,
          sensorConfig.getSensorType(sensorTypeId)));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Exception while getting columns", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  public static List<FileColumn> getCalibratedSensorColumns(
    DataSource dataSource, long instrumentId) throws MissingParamException,
    SensorTypeNotFoundException, DatabaseException {

    return getSensorColumns(dataSource, instrumentId).stream()
      .filter(x -> x.getSensorType().hasInternalCalibration())
      .collect(Collectors.toList());
  }

  public static TreeMap<String, String> getPlatforms(DataSource dataSource,
    User user) throws DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(user, "user");

    TreeMap<String, String> result = new TreeMap<String, String>();

    try (Connection conn = dataSource.getConnection()) {

      PreparedStatement stmt;

      if (user.isAdminUser()) {
        stmt = conn.prepareStatement(ALL_PLATFORMS_QUERY);
      } else {
        stmt = conn.prepareStatement(PLATFORMS_QUERY);
        stmt.setLong(1, user.getDatabaseID());
      }

      try (ResultSet records = stmt.executeQuery()) {
        while (records.next()) {
          result.put(records.getString(1), records.getString(2));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error getting platforms list", e);
    }

    return result;
  }

  public static void saveInstrumentProperties(DataSource dataSource,
    Instrument instrument) throws DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "instrument");

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(SAVE_PROPERTIES_STATEMENT)) {

      if (instrument.getId() == DatabaseUtils.NO_DATABASE_RECORD) {
        throw new DatabaseException("Instrument not saved to database");
      }

      stmt.setString(1, instrument.getPropertiesJson());
      stmt.setLong(2, instrument.getId());
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error saving instrument properties", e);
    }
  }

  public static void deleteInstrument(DataSource dataSource, long instrumentId)
    throws DatabaseException, RecordNotFoundException, MissingParamException,
    InstrumentException, SensorGroupsException, FileStoreException,
    IOException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    try (Connection conn = dataSource.getConnection()) {
      Instrument instrument = getInstrument(conn, instrumentId);

      // Make sure there are no datasets attached to the instrument
      LinkedHashMap<Long, DataSet> datasets = DataSetDB.getDataSets(conn,
        instrumentId, true);
      if (datasets.size() > 0) {
        throw new InstrumentException(
          "Cannot delete instrument - datasets present");
      }

      // Delete all data files
      DataFileDB.deleteAllFiles(conn, ResourceManager.getInstance().getConfig(),
        instrument, true);

      runDeleteStatement(conn, DELETE_CALIBRATIONS_STATEMENT, instrumentId);
      runDeleteStatement(conn, DELETE_RUN_TYPES_STATEMENT, instrumentId);
      runDeleteStatement(conn, DELETE_FILE_COLUMNS_STATEMENT, instrumentId);
      runDeleteStatement(conn, DELETE_FILE_DEFINITIONS_STATEMENT, instrumentId);
      runDeleteStatement(conn, DELETE_SHARED_INSTRUMENTS_STATEMENT,
        instrumentId);
      runDeleteStatement(conn, DELETE_INSTRUMENT_VARIABLES_STATEMENT,
        instrumentId);
      runDeleteStatement(conn, DELETE_INSTRUMENT_STATEMENT, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error deleting instrument", e);
    }
  }

  private static void runDeleteStatement(Connection conn, String query,
    long instrumentId) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement(query)) {
      stmt.setLong(1, instrumentId);
      stmt.execute();
    }
  }

  /**
   * Share the specified {@link Instrument} with the specified {@link User}.
   *
   * <p>
   * The new share is stored in the database and the {@link Instrument} object
   * is updated.
   * </p>
   *
   * <p>
   * The method does not throw any errors unless the parameters are
   * {@code null}. The following situations are ignored silently:
   * </p>
   * <ul>
   * <li>The {@link User} is the {@link Instrument} owner.</li>
   * <li>The {@link Instrument} is already shared with the {@link User}.</li>
   * </ul>
   *
   * @param dataSource
   *          A data source.
   * @param instrument
   *          The {@link Instrument} to be shared.
   * @param user
   *          The {@link User} that the {@link Instrument} is to be shared with.
   * @throws RecordNotFoundException
   *           If the {@link User} does not exist in the database.
   * @throws DatabaseException
   *           If a database error occurs.
   */
  public static void addUserShare(DataSource dataSource, Instrument instrument,
    User user) throws RecordNotFoundException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "Instrument");
    MissingParam.checkMissing(user, "user");

    if (!UserDB.userExists(dataSource, user.getDatabaseID())) {
      throw new RecordNotFoundException("User does not exist");
    }

    if (!instrument.getOwner().equals(user) && !instrument.isSharedWith(user)) {

      try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(ADD_SHARE_STATEMENT);) {

        stmt.setLong(1, instrument.getId());
        stmt.setLong(2, user.getDatabaseID());

        stmt.execute();

        instrument.addShare(user);
      } catch (SQLException e) {
        throw new DatabaseException("Error adding share", e);
      }
    }
  }

  /**
   * Remove share access to an {@link Instrument} for a specified {@link User}.
   *
   * <p>
   * The share is removed from the database and the {@link Instrument} object is
   * updated.
   * </p>
   *
   * <p>
   * The method does not throw any errors unless the parameters are
   * {@code null}. If the {@link Instrument} is not shared with the
   * {@link User}, the method has no effect.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param instrument
   *          The {@link Instrument} to be shared.
   * @param user
   *          The {@link User} that the {@link Instrument} is to be shared with.
   * @throws DatabaseException
   *           If a database error occurs.
   */
  public static void removeUserShare(DataSource dataSource,
    Instrument instrument, User user) throws DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "Instrument");
    MissingParam.checkMissing(user, "user");

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn.prepareStatement(REMOVE_SHARE_STATEMENT);) {

      stmt.setLong(1, instrument.getId());
      stmt.setLong(2, user.getDatabaseID());

      stmt.execute();

      instrument.removeShare(user);

    } catch (SQLException e) {
      throw new DatabaseException("Error removing share", e);
    }

  }

  /**
   * Give the specified {@link Instrument} a new owner.
   *
   * <p>
   * The original owner will retain access to the {@link Instrument} through the
   * sharing mechanism.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param instrument
   *          The {@link Instrument} whose ownership is to be changed.
   * @param newOwner
   *          The {@link Instrument}'s new owner.
   * @throws DatabaseException
   * @throws RecordNotFoundException
   */
  public static void setOwner(DataSource dataSource, Instrument instrument,
    User newOwner) throws DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkMissing(newOwner, "newOwner");

    User existingOwner = instrument.getOwner();
    if (!existingOwner.equals(newOwner)) {
      try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(false);

        if (!UserDB.userExists(conn, newOwner.getDatabaseID())) {
          throw new RecordNotFoundException("New Owner does not exist");
        } else {
          try (
            PreparedStatement ownerStmt = conn
              .prepareStatement(SET_OWNER_STATEMENT);
            PreparedStatement removeShareStmt = conn
              .prepareStatement(REMOVE_SHARE_STATEMENT);
            PreparedStatement addShareStmt = conn
              .prepareStatement(ADD_SHARE_STATEMENT);) {

            removeShareStmt.setLong(1, instrument.getId());
            removeShareStmt.setLong(2, newOwner.getDatabaseID());

            ownerStmt.setLong(1, newOwner.getDatabaseID());
            ownerStmt.setLong(2, instrument.getId());

            addShareStmt.setLong(1, instrument.getId());
            addShareStmt.setLong(2, existingOwner.getDatabaseID());

            ownerStmt.execute();
            removeShareStmt.execute();
            addShareStmt.execute();

            conn.commit();

            instrument.removeShare(newOwner);
            instrument.setOwner(newOwner);
            instrument.addShare(existingOwner);
          }
        }

      } catch (SQLException e) {
        throw new DatabaseException("Error while setting instrument ownership",
          e);
      }
    }

  }
}
