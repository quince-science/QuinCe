package uk.ac.exeter.QuinCe.data.Files;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Methods for handling raw data files.
 * <p>
 * The database stores details of the files, while the files themselves are kept
 * on the file system (see {@link FileStore}).
 * </p>
 * <p>
 * This class provides the API for all raw data handling activities. All methods
 * automatically make the appropriate calls to store and retrieve files on the
 * file system, and methods in the {@link FileStore} class should not be called
 * directly (they are {@code protected} within this package).
 * </p>
 *
 * @author Steve Jones
 * @see FileStore
 */
public class DataFileDB {

  /**
   * Statement to add a data file to the database
   *
   * @see #storeNewFile(DataSource, Properties, DataFile)
   */
  private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file "
    + "(file_definition_id, filename, start_date, end_date, record_count, properties) "
    + "VALUES (?, ?, ?, ?, ?, ?)";

  /**
   * Statement to add a data file to the database
   *
   * @see #replaceFile(DataSource, Properties, DataFile, long)
   */
  private static final String REPLACE_FILE_STATEMENT = "UPDATE data_file "
    + "SET filename = ?, start_date = ?, end_date = ?, record_count = ?, properties = ? "
    + "WHERE id = ?";

  /**
   * Query to get a set of data files by their ID
   */
  private static final String GET_FILENAME_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, "
    + "f.end_date, f.record_count, f.properties, i.id FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id " + "WHERE f.id IN "
    + DatabaseUtils.IN_PARAMS_TOKEN + " ORDER BY f.start_date ASC";

  /**
   * Query to find all the data files owned by a given user
   *
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, "
    + "f.record_count, f.properties, i.id FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
    + "ORDER BY f.start_date ASC";

  /**
   * Query to find all the data files owned by a given user
   *
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_BY_INSTRUMENT_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, "
    + "f.record_count, f.properties FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
    + "WHERE d.instrument_id = ? ORDER BY f.start_date ASC";

  /**
   * Query to find all the data files owned by a given user
   *
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_BY_DEFINITION_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, "
    + "f.record_count, f.properties, d.id FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "WHERE d.id = ? ORDER BY f.start_date ASC";

  /**
   * Query to find all the data files owned by a given user
   *
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_AFTER_DATE_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, "
    + "f.record_count, f.properties, i.id FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
    + "WHERE f.file_definition_id = ? AND f.end_date > ?";

  /**
   * Statement to delete the details of a data file
   *
   * @see #deleteFile(DataSource, Properties, FileInfo)
   */
  private static final String DELETE_FILE_STATEMENT = "DELETE FROM data_file WHERE id = ?";

  /**
   * Query to find a file using its database ID
   *
   * @see #getFileDetails(Connection, long)
   * @see #makeFileInfo(ResultSet, Connection)
   * @see #fileExists(Connection, long)
   */
  private static final String FIND_FILE_BY_ID_QUERY = "SELECT "
    + "id FROM data_file WHERE id = ?";

  /**
   * Query to get the last date covered by any file for an instrument
   */
  private static final String GET_LAST_FILE_DATE_QUERY = "SELECT "
    + "end_date, properties FROM data_file WHERE file_definition_id IN "
    + "(SELECT id FROM file_definition WHERE instrument_id = ?) "
    + "ORDER BY end_date DESC LIMIT 1";

  private static final String FIND_FILE_BY_NAME_QUERY = "SELECT "
    + "id FROM data_file WHERE filename = ? AND file_definition_id IN "
    + "(SELECT id FROM file_definition WHERE instrument_id = ?)";

  private static final String GET_FILE_COUNT_QUERY = "SELECT "
    + "COUNT(*) FROM data_file WHERE " + "file_definition_id IN"
    + "(SELECT id FROM file_definition WHERE instrument_id = ?)";

  /**
   * Query to get the last file modification date for an instrument.
   *
   * @see #getLastFileModification(Connection, long)
   */
  private static final String GET_LAST_MODIFIED_QUERY = "SELECT "
    + "modified FROM data_file WHERE file_definition_id IN "
    + "(SELECT id FROM file_definition WHERE instrument_id = ?) "
    + "ORDER BY modified DESC LIMIT 1";

  /**
   * Store a file in the database and in the file store
   *
   * @param dataSource
   *          A data source
   * @param appConfig
   *          The application configuration
   * @param dataFile
   *          The data file
   * @throws MissingParamException
   *           If any of the parameters are missing
   * @throws FileExistsException
   *           If the file already exists in the system
   * @throws DatabaseException
   *           If an error occurs while storing the file
   * @throws RecordNotFoundException
   * @see #ADD_FILE_STATEMENT
   * @see FileStore#storeFile(String, DataFile)
   */
  public static void storeFile(DataSource dataSource, Properties appConfig,
    DataFile dataFile, long replacementId) throws MissingParamException,
    FileExistsException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(appConfig, "appConfig");
    MissingParam.checkMissing(dataFile, "dataFile");

    Connection conn = null;

    try {
      conn = dataSource.getConnection();

      if (replacementId > -1) {
        if (!fileExists(conn, replacementId)) {
          throw new RecordNotFoundException(
            "Tried to replace a file that doesn't exist (id " + replacementId
              + ")");
        }

        replaceFile(conn, appConfig, dataFile, replacementId);
      } else {
        storeNewFile(conn, appConfig, dataFile);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing file", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Store a file in the database and in the file store
   *
   * @param dataSource
   *          A data source
   * @param appConfig
   *          The application configuration
   * @param dataFile
   *          The data file
   * @throws FileExistsException
   *           If the file already exists in the system
   * @throws DatabaseException
   *           If an error occurs while storing the file
   * @see #ADD_FILE_STATEMENT
   * @see FileStore#storeFile(String, DataFile)
   */
  private static void storeNewFile(Connection conn, Properties appConfig,
    DataFile dataFile) throws DatabaseException, FileExistsException {

    PreparedStatement stmt = null;
    ResultSet generatedKeys = null;

    try {
      if (getFilesWithinDates(conn, dataFile.getFileDefinition(),
        dataFile.getRawStartTime(), dataFile.getRawEndTime(), false)
          .size() > 0) {
        throw new FileExistsException(dataFile.getFileDescription(),
          dataFile.getRawStartTime(), dataFile.getRawEndTime());
      }

      boolean initialAutoCommit = conn.getAutoCommit();

      if (initialAutoCommit) {
        conn.setAutoCommit(false);
      }

      stmt = conn.prepareStatement(ADD_FILE_STATEMENT,
        Statement.RETURN_GENERATED_KEYS);
      stmt.setLong(1, dataFile.getFileDefinition().getDatabaseId());
      stmt.setString(2, dataFile.getFilename());
      stmt.setLong(3, DateTimeUtils.dateToLong(dataFile.getRawStartTime()));
      stmt.setLong(4, DateTimeUtils.dateToLong(dataFile.getRawEndTime()));
      stmt.setInt(5, dataFile.getRecordCount());
      stmt.setString(6, new Gson().toJson(dataFile.getProperties()));

      stmt.execute();

      generatedKeys = stmt.getGeneratedKeys();
      if (generatedKeys.next()) {

        dataFile.setDatabaseId(generatedKeys.getLong(1));

        // Store the file
        FileStore.storeFile(appConfig.getProperty("filestore"), dataFile);

        conn.commit();
      }

      if (initialAutoCommit) {
        conn.setAutoCommit(true);
      }
    } catch (FileExistsException e) {
      throw e;
    } catch (Exception e) {
      try {
        DatabaseUtils.rollBack(conn);
      } catch (Exception e2) {
        // Do nothing
      }

      throw new DatabaseException("An error occurred while storing the file",
        e);
    } finally {
      DatabaseUtils.closeResultSets(generatedKeys);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Store a file in the database and in the file store
   *
   * @param dataSource
   *          A data source
   * @param appConfig
   *          The application configuration
   * @param dataFile
   *          The data file
   * @throws FileExistsException
   *           If the file already exists in the system
   * @throws DatabaseException
   *           If an error occurs while storing the file
   * @see #ADD_FILE_STATEMENT
   * @see FileStore#storeFile(String, DataFile)
   */
  private static void replaceFile(Connection conn, Properties appConfig,
    DataFile dataFile, long replacementId)
    throws DatabaseException, FileExistsException {

    PreparedStatement stmt = null;

    try {
      boolean storeFile = true;

      if (replacementId > -1) {
        // Get the existing file. If it's identical to the current file,
        // we don't need to do anything
        List<Long> idList = new ArrayList<Long>(1);
        idList.add(replacementId);
        DataFile fileToReplace = getDataFiles(conn, appConfig, idList).get(0);

        byte[] existingFile = fileToReplace.getBytes();
        byte[] newFile = dataFile.getContents().getBytes();
        storeFile = !Arrays.equals(existingFile, newFile);
      }

      if (storeFile) {
        boolean initialAutoCommit = conn.getAutoCommit();

        if (initialAutoCommit) {
          conn.setAutoCommit(false);
        }
        stmt = conn.prepareStatement(REPLACE_FILE_STATEMENT);
        stmt.setString(1, dataFile.getFilename());
        stmt.setLong(2, DateTimeUtils.dateToLong(dataFile.getRawStartTime()));
        stmt.setLong(3, DateTimeUtils.dateToLong(dataFile.getRawEndTime()));
        stmt.setInt(4, dataFile.getRecordCount());
        stmt.setString(5, new Gson().toJson(dataFile.getProperties()));
        stmt.setLong(6, replacementId);

        stmt.execute();

        // Set the database ID on the file now the replacement has succeeded
        dataFile.setDatabaseId(replacementId);

        // Store the file - automatically replaces the old one
        FileStore.storeFile(appConfig.getProperty("filestore"), dataFile);

        conn.commit();

        if (initialAutoCommit) {
          conn.setAutoCommit(true);
        }
      }
    } catch (Exception e) {
      try {
        DatabaseUtils.rollBack(conn);
      } catch (Exception e2) {
        // Do nothing
      }

      throw new DatabaseException("An error occurred while storing the file",
        e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Determines whether a file with the specified ID exists in the database
   *
   * @param conn
   *          A database connection
   * @param fileId
   *          The file ID
   * @return {@code true} if the file exists; {@code false} if it does not
   * @throws MissingParamException
   *           If any parameters are missing
   * @throws DatabaseException
   *           If an error occurs
   * @throws RecordNotFoundException
   *           If the file disappears between locating it and reading its
   *           details
   */
  public static boolean fileExists(Connection conn, long fileId)
    throws MissingParamException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(fileId, "fileId");

    boolean result = false;

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(FIND_FILE_BY_ID_QUERY);
      stmt.setLong(1, fileId);
      records = stmt.executeQuery();
      if (records.next()) {
        result = true;
      }
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while checking file existence", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Returns a list of all the files owned by a specific user. The list can
   * optionally be restricted by an instrument ID.
   *
   * @param conn
   *          A database connection
   * @param appConfig
   *          The application configuration
   * @param user
   *          The user
   * @param instrumentId
   *          The instrument ID used to filter the list (optional)
   * @return The list of files
   * @throws DatabaseException
   *           If an error occurs during the search
   * @see #GET_USER_FILES_QUERY
   * @see #GET_USER_FILES_BY_INSTRUMENT_QUERY
   * @see #makeDataFile(ResultSet, String, Connection)
   */
  public static List<DataFile> getFiles(DataSource dataSource,
    Properties appConfig, Long instrumentId) throws DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      return getFiles(conn, appConfig, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("An error occurred while searching for files",
        e);
    }
  }

  /**
   * Returns a list of all the files owned by a specific user. The list can
   * optionally be restricted by an instrument ID.
   *
   * @param dataSource
   *          A data source
   * @param appConfig
   *          The application configuration
   * @param user
   *          The user
   * @param instrumentId
   *          The instrument ID used to filter the list (optional)
   * @return The list of files
   * @throws DatabaseException
   *           If an error occurs during the search
   * @see #GET_USER_FILES_QUERY
   * @see #GET_USER_FILES_BY_INSTRUMENT_QUERY
   * @see #makeDataFile(ResultSet, String, Connection)
   */
  public static List<DataFile> getFiles(Connection conn, Properties appConfig,
    Long instrumentId) throws DatabaseException {

    PreparedStatement stmt = null;
    ResultSet records = null;
    List<DataFile> fileInfo = new ArrayList<DataFile>();

    try {
      InstrumentFileSet fileDefinitions = InstrumentDB.getFileDefinitions(conn,
        instrumentId);

      if (null != instrumentId) {
        stmt = conn.prepareStatement(GET_FILES_BY_INSTRUMENT_QUERY);
      } else {
        stmt = conn.prepareStatement(GET_FILES_QUERY);
      }

      if (null != instrumentId) {
        stmt.setLong(1, instrumentId);
      }

      records = stmt.executeQuery();
      while (records.next()) {
        fileInfo.add(makeDataFile(records, appConfig.getProperty("filestore"),
          fileDefinitions));
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new DatabaseException("An error occurred while searching for files",
        e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return fileInfo;
  }

  /**
   * Returns a list of all the files for a given file definition.
   *
   * @param conn
   *          A database connection
   * @param appConfig
   *          The application configuration
   * @param fileDefinition
   *          The file definition
   * @return The list of files
   * @throws DatabaseException
   *           If an error occurs during the search
   * @see #GET_USER_FILES_QUERY
   * @see #GET_USER_FILES_BY_INSTRUMENT_QUERY
   * @see #makeDataFile(ResultSet, String, Connection)
   */
  public static List<DataFile> getFiles(DataSource dataSource,
    Properties appConfig, FileDefinition fileDefinition)
    throws DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      return getFiles(conn, appConfig, fileDefinition);
    } catch (SQLException e) {
      throw new DatabaseException("An error occurred while searching for files",
        e);
    }
  }

  /**
   * Returns a list of all the files for a give file definition
   *
   * @param dataSource
   *          A data source
   * @param appConfig
   *          The application configuration
   * @param fileDefinition
   *          The file definition
   * @return The list of files
   * @throws DatabaseException
   *           If an error occurs during the search
   * @see #makeDataFile(ResultSet, String, Connection)
   */
  public static List<DataFile> getFiles(Connection conn, Properties appConfig,
    FileDefinition fileDefinition) throws DatabaseException {

    PreparedStatement stmt = null;
    ResultSet records = null;
    List<DataFile> fileInfo = new ArrayList<DataFile>();

    try {
      stmt = conn.prepareStatement(GET_FILES_BY_DEFINITION_QUERY);
      stmt.setLong(1, fileDefinition.getDatabaseId());

      records = stmt.executeQuery();
      while (records.next()) {
        fileInfo.add(makeDataFile(records, appConfig.getProperty("filestore"),
          fileDefinition));
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new DatabaseException("An error occurred while searching for files",
        e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return fileInfo;
  }

  /**
   * Get the {@link DataFile} objects for a set of files
   *
   * @param conn
   *          A datbase connection
   * @param appConfig
   *          The application configuration
   * @param ids
   *          The file ids
   * @return The DataFile objects
   * @throws MissingParamException
   *           If any of the parameters are missing
   * @throws DatabaseException
   *           If an error occurs during the search
   * @throws RecordNotFoundException
   *           If any files are not in the database
   */
  public static List<DataFile> getDataFiles(Connection conn,
    Properties appConfig, List<Long> ids)
    throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(appConfig, "appConfig");
    MissingParam.checkMissing(ids, "ids");

    List<DataFile> files = new ArrayList<DataFile>(ids.size());
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      stmt = conn.prepareStatement(
        DatabaseUtils.makeInStatementSql(GET_FILENAME_QUERY, ids.size()));
      for (int i = 0; i < ids.size(); i++) {
        stmt.setLong(i + 1, ids.get(i));
      }

      records = stmt.executeQuery();
      while (records.next()) {
        files
          .add(makeDataFile(records, appConfig.getProperty("filestore"), conn));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting files", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    if (files.size() != ids.size()) {
      throw new RecordNotFoundException("Could not locate all files");
    }

    return files;
  }

  /**
   * Build a {@link DataFile} object from a database record
   *
   * @param record
   *          The database record
   * @param fileStore
   *          The file store location
   * @param conn
   *          A database connection
   * @return The DataFile object
   * @throws SQLException
   *           If the record cannot be read
   * @throws DatabaseException
   *           If any sub-queries fail
   */
  private static DataFile makeDataFile(ResultSet record, String fileStore,
    Connection conn) throws SQLException, DatabaseException {
    DataFile result = null;

    try {
      long instrumentId = record.getLong(8);
      InstrumentFileSet files = InstrumentDB.getFileDefinitions(conn,
        instrumentId);
      result = makeDataFile(record, fileStore, files);
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new DatabaseException("Error retrieving file definition details",
        e);
    }

    return result;
  }

  /**
   * Build a {@link DataFile} object from a database record
   *
   * @param record
   *          The database record
   * @param fileStore
   *          The file store location
   * @param conn
   *          A database connection
   * @return The DataFile object
   * @throws SQLException
   *           If the record cannot be read
   * @throws DatabaseException
   *           If any sub-queries fail
   */
  private static DataFile makeDataFile(ResultSet record, String fileStore,
    InstrumentFileSet fileDefinitions) throws SQLException, DatabaseException {
    DataFile result = null;

    try {
      long fileDefinitionId = record.getLong(2);
      result = makeDataFile(record, fileStore,
        fileDefinitions.get(fileDefinitionId));
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new DatabaseException("Error retrieving file definition details",
        e);
    }

    return result;
  }

  /**
   * Build a {@link DataFile} object from a database record
   *
   * @param record
   *          The record
   * @param fileStore
   *          The file store location
   * @param fileDefinition
   *          The file definition for the file
   * @return The DataFile object
   * @throws SQLException
   *           If the data cannot be extracted from the record
   */
  private static DataFile makeDataFile(ResultSet record, String fileStore,
    FileDefinition fileDefinition) throws SQLException {
    DataFile result = null;

    try {
      long id = record.getLong(1);
      String filename = record.getString(3);
      LocalDateTime startDate = DateTimeUtils.longToDate(record.getLong(4));
      LocalDateTime endDate = DateTimeUtils.longToDate(record.getLong(5));
      int recordCount = record.getInt(6);
      Properties properties = new Gson().fromJson(record.getString(7),
        Properties.class);

      result = new DataFile(fileStore, id, fileDefinition, filename, startDate,
        endDate, recordCount, properties);
    } catch (SQLException e) {
      throw e;
    }

    return result;
  }

  /**
   * Removes a file from the database and the underlying file store.
   *
   * @param dataSource
   *          A data source
   * @param appConfig
   *          The application configuration
   * @param dataFile
   *          The data file
   * @throws MissingParamException
   *           If any parameters are missing
   * @throws DatabaseException
   *           If an error occurs during deletion
   * @see #DELETE_FILE_STATEMENT
   * @see FileStore#deleteFile(String, DataFile)
   */
  public static void deleteFile(DataSource dataSource, Properties appConfig,
    DataFile dataFile) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(appConfig, "appConfig");
    MissingParam.checkMissing(dataFile, "dataFile");

    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      // Send out sub-record delete requests
      stmt = conn.prepareStatement(DELETE_FILE_STATEMENT);
      stmt.setLong(1, dataFile.getDatabaseId());
      stmt.execute();

      // Delete the file from the file store
      FileStore.deleteFile(appConfig.getProperty("filestore"), dataFile);

      conn.commit();

    } catch (SQLException e) {
      DatabaseUtils.rollBack(conn);
      throw new DatabaseException(
        "An error occurred while deleting the data file", e);
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (SQLException e) {
        throw new DatabaseException("Unable to reset connection autocommit", e);
      }
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Get the list of data files of a given file definition that encompass two
   * dates. Time offsets are not applied.
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The file definition
   * @param start
   *          The start date
   * @param end
   *          The end date
   * @return
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static List<DataFile> getFilesWithinDates(DataSource dataSource,
    FileDefinition fileDefinition, LocalDateTime start, LocalDateTime end,
    boolean applyOffset) throws MissingParamException, DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      return getFilesWithinDates(conn, fileDefinition, start, end, applyOffset);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting files", e);
    }
  }

  /**
   * Get the list of data files of a given file definition that encompass two
   * dates. Time offsets are not applied.
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The file definition
   * @param start
   *          The start date
   * @param end
   *          The end date
   * @return
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static List<DataFile> getFilesWithinDates(Connection conn,
    FileDefinition fileDefinition, LocalDateTime start, LocalDateTime end,
    boolean applyOffset) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(fileDefinition, "fileDefinition");
    MissingParam.checkMissing(start, "start");
    MissingParam.checkMissing(end, "end");

    List<DataFile> allInstrumentFiles = getFiles(conn,
      ResourceManager.getInstance().getConfig(), fileDefinition);

    return filterFilesByDates(allInstrumentFiles, start, end, applyOffset);
  }

  /**
   * Filter a list of files so that only those overlapping a given time period
   * are returned.
   * <p>
   * This function assumes that the passed in list is already sorted.
   * </p>
   *
   * @param files
   *          The files to filter.
   * @param start
   *          The start date.
   * @param end
   *          The end date.
   * @param applyOffset
   *          Indicates whether or not the files' time offsets should be
   *          applied.
   * @return The filtered file list.
   */
  private static List<DataFile> filterFilesByDates(List<DataFile> files,
    LocalDateTime start, LocalDateTime end, boolean applyOffset) {

    if (end.isBefore(start)) {
      throw new IllegalArgumentException("End must be >= start date");
    }

    return files.stream()
      .filter(f -> f.getEndTime(applyOffset).isAfter(start)
        && f.getStartTime(applyOffset).isBefore(end))
      .collect(Collectors.toList());
  }

  /**
   * Get the list of data files for a given instrument that encompass two dates
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument ID
   * @param start
   *          The start date
   * @param end
   *          The end date
   * @return
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static List<Long> getFilesWithinDates(Connection conn,
    long instrumentId, LocalDateTime start, LocalDateTime end,
    boolean applyOffset) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(start, "start");
    MissingParam.checkMissing(end, "end");

    List<DataFile> allInstrumentFiles = getFiles(conn,
      ResourceManager.getInstance().getConfig(), instrumentId);

    List<DataFile> dateFiles = filterFilesByDates(allInstrumentFiles, start,
      end, applyOffset);

    return dateFiles.stream().map(f -> f.getDatabaseId())
      .collect(Collectors.toList());
  }

  /**
   * Determine whether or not there is a complete set of files available after a
   * given time, from which a dataset can be made.
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The ID of the instrument for which files must be found
   * @param time
   *          The time boundary
   * @return {@code true} if a complete set of files is available; {@code false}
   *           if not
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   * @throws InstrumentException
   */
  public static boolean completeFilesAfter(Connection conn,
    Properties appConfig, long instrumentId, LocalDateTime time)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {

    boolean result = true;

    // If no time is specified, we can use all files
    if (null == time) {
      time = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    }

    List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
    List<ResultSet> resultSets = new ArrayList<ResultSet>();
    try {
      InstrumentFileSet fileDefinitions = InstrumentDB.getFileDefinitions(conn,
        instrumentId);
      Map<FileDefinition, List<DataFile>> filesAfterDate = new HashMap<FileDefinition, List<DataFile>>();

      // Get all the files after the specified date, grouped by file definition
      for (FileDefinition fileDefinition : fileDefinitions) {
        List<DataFile> foundFiles = new ArrayList<DataFile>();

        PreparedStatement stmt = conn
          .prepareStatement(GET_FILES_AFTER_DATE_QUERY);
        stmt.setLong(1, fileDefinition.getDatabaseId());
        stmt.setLong(2, DateTimeUtils.dateToLong(time));

        ResultSet records = stmt.executeQuery();
        while (records.next()) {
          foundFiles.add(makeDataFile(records,
            appConfig.getProperty("filestore"), fileDefinitions));
        }

        statements.add(stmt);
        resultSets.add(records);

        // If no matching files are found, abort.
        if (foundFiles.size() == 0) {
          result = false;
          break;
        }

        filesAfterDate.put(fileDefinition, foundFiles);
      }

      // If any file defs had no files, the result will be false
      // and we don't go any further. Also if there's only one file
      // definition we can skip this check
      if (result && fileDefinitions.size() > 1) {

        result = false;

        List<DataFile> rootFiles = filesAfterDate.get(fileDefinitions.get(0));

        for (DataFile rootFile : rootFiles) {
          for (int i = 1; i < fileDefinitions.size() && !result; i++) {
            List<DataFile> compareFiles = filesAfterDate
              .get(fileDefinitions.get(i));

            for (int j = 0; j < compareFiles.size() && !result; j++) {
              DataFile compareFile = compareFiles.get(j);
              if (compareFile.getRawStartTime()
                .compareTo(rootFile.getRawEndTime()) < 0
                && compareFile.getRawEndTime()
                  .compareTo(rootFile.getRawStartTime()) > 0) {
                result = true;
              }
            }
          }
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving file info", e);
    } finally {
      DatabaseUtils.closeResultSets(resultSets);
      DatabaseUtils.closeStatements(statements);
    }

    return result;
  }

  /**
   * Get the last date covered by any file for a given instrument
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @param applyOffset
   *          Indicates whether or not file's time offset should be applied
   * @return The last date, or {@code null} if there are no files
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public static LocalDateTime getLastFileDate(Connection conn,
    long instrumentId, boolean applyOffset)
    throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(instrumentId, "instrumentId");

    LocalDateTime result = null;

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_LAST_FILE_DATE_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();
      if (records.next()) {
        result = DateTimeUtils.longToDate(records.getLong(1));

        if (applyOffset) {
          Properties properties = new Gson().fromJson(records.getString(2),
            Properties.class);
          result.plusSeconds(Integer
            .parseInt(properties.getProperty(DataFile.TIME_OFFSET_PROP)));
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting file dates", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Determine whether or not an instrument has a file with the specified name
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument's database ID
   * @param filename
   *          The filename
   * @return {@code true} if a file exists; {@code false} if it does not
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   */
  public static boolean hasFileWithName(DataSource dataSource,
    long instrumentId, String filename)
    throws DatabaseException, MissingParamException {
    boolean result = false;

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(filename, "filename");

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(FIND_FILE_BY_NAME_QUERY);
      stmt.setString(1, filename);
      stmt.setLong(2, instrumentId);

      records = stmt.executeQuery();
      if (records.next()) {
        result = true;
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while searching for file", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  public static int getFileCount(DataSource dataSource, long instrumentId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    int fileCount = 0;

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn.prepareStatement(GET_FILE_COUNT_QUERY);) {

      stmt.setLong(1, instrumentId);

      try (ResultSet records = stmt.executeQuery()) {
        records.first();
        fileCount = records.getInt(1);
      } catch (SQLException e) {
        throw e;
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting instrument files", e);
    }

    return fileCount;
  }

  /**
   * Get the last date that any data file was modified for a given instrument.
   * <p>
   * Returns {@code null} if there are no data files for the instrument, or the
   * instrument does not exist.
   * </p>
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @return The last modification date
   * @throws MissingParamException
   * @throws DatabaseException
   */
  public static LocalDateTime getLastFileModification(Connection conn,
    long instrumentId) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    LocalDateTime result = null;

    try (
      PreparedStatement stmt = conn.prepareStatement(GET_LAST_MODIFIED_QUERY)) {

      stmt.setLong(1, instrumentId);

      try (ResultSet records = stmt.executeQuery()) {
        if (records.next()) {
          result = DateTimeUtils.longToDate(records.getTimestamp(1).getTime());
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting file modification dates",
        e);
    }

    return result;
  }
}
