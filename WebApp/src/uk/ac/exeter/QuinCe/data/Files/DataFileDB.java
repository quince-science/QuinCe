package uk.ac.exeter.QuinCe.data.Files;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Methods for handling raw data files.
 *
 * <p>
 *   The database stores details of the files, while the files themselves
 *   are kept on the file system (see {@link FileStore}).
 * </p>
 *
 * <p>
 *   This class provides the API for all raw data handling activities.
 *   All methods automatically make the appropriate calls to store and
 *   retrieve files on the file system, and methods in the {@link FileStore}
 *   class should not be called directly (they are {@code protected} within this package).
 * </p>
 *
 * @author Steve Jones
 * @see FileStore
 */
public class DataFileDB {

  /**
   * Query to find a data file in the database by name and instrument ID
   * @see #fileExists(DataSource, long, String)
   */
  private static final String FIND_FILE_QUERY = "SELECT id FROM data_file WHERE instrument_id = ? AND filename = ?";

  /**
   * Statement to add a data file to the database
   * @see #storeNewFile(DataSource, Properties, DataFile)
   */
  private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file "
      + "(file_definition_id, filename, start_date, end_date, record_count) "
    + "VALUES (?, ?, ?, ?, ?)";

  /**
   * Statement to add a data file to the database
   * @see #replaceFile(DataSource, Properties, DataFile, long)
   */
  private static final String REPLACE_FILE_STATEMENT = "UPDATE data_file "
      + "SET filename = ?, start_date = ?, end_date = ?, record_count = ? "
      + "WHERE id = ?";

  /**
   * Query to get a set of data files by their ID
   */
  private static final String GET_FILENAME_QUERY = "SELECT "
      + "f.id, f.file_definition_id, f.filename, f.start_date, "
      + "f.end_date, f.record_count, i.id FROM data_file AS f "
      + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
      + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
      + "WHERE f.id IN " + DatabaseUtils.IN_PARAMS_TOKEN;

  /**
   * Query to find all the data files owned by a given user
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_USER_FILES_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, f.record_count, i.id "
    + "FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
    + "WHERE i.owner = ? ORDER BY f.start_date ASC";

  /**
   * Query to find all the data files owned by a given user
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_USER_FILES_BY_INSTRUMENT_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, f.record_count, i.id "
    + "FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
    + "WHERE i.owner = ? AND d.instrument_id = ? ORDER BY f.start_date ASC";

  /**
   * Query to find all the data files owned by a given user
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_AFTER_DATE_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, f.record_count, i.id "
    + "FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
    + "WHERE f.file_definition_id = ? AND f.end_date > ?";

  /**
   * Statement to delete the details of a data file
   * @see #deleteFile(DataSource, Properties, FileInfo)
   */
  private static final String DELETE_FILE_STATEMENT = "DELETE FROM data_file WHERE id = ?";

  /**
   * Query to find a file using its database ID
   * @see #getFileDetails(Connection, long)
   * @see #makeFileInfo(ResultSet, Connection)
   * @see #fileExists(Connection, long)
   */
  private static final String FIND_FILE_BY_ID_QUERY = "SELECT "
      + "id FROM data_file WHERE id = ?";

  /**
   * Query to determine if a file exists covering two dates
   */
  private static final String FILE_EXISTS_WITH_DATES_QUERY = "SELECT COUNT(*) FROM data_file "
      + "WHERE file_definition_id = ? AND "
      + "(start_date BETWEEN ? AND ? OR "
      + "end_date BETWEEN ? AND ?)";

  /**
   * Query to find files for a given definition that covers part of a given date range
   * @see #getFiles(DataSource, FileDefinition, LocalDateTime, LocalDateTime)
   */
  private static final String GET_FILES_BY_TYPE_DATE_QUERY = "SELECT "
      + "id, file_definition_id, filename, start_date, end_date, record_count "
      + "FROM data_file WHERE "
      + "file_definition_id = ? AND "
      + "(start_date <= ? AND end_date > ? OR "
      + "start_date < ? AND end_date >= ?) "
      + "ORDER BY start_date ASC";

  /**
   * Query to find files that fall wholly or partially within
   * a date range for a given instrument.
   * NOTE: start_date <= range_end and end_date >= range_start
   */
  private static final String GET_FILES_WITHIN_DATES_QUERY = "SELECT "
      + "id FROM data_file WHERE file_definition_id IN "
      + "(SELECT id FROM file_definition WHERE instrument_id = ?) AND "
      + "start_date <= ? AND end_date >= ?";

  /**
   * Query to get data files for a file definition that encompass given dates
   */
  private static final String GET_FILEDEF_FILES_WITHIN_DATES_QUERY = "SELECT "
      + "f.id, f.file_definition_id, f.filename, f.start_date, f.end_date, f.record_count, i.id "
      + "FROM data_file AS f "
      + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
      + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
      + "WHERE d.id = ? AND start_date <= ? AND end_date >= ?";

  /**
   * Query to get the last date covered by any file for an instrument
   */
  private static final String GET_LAST_FILE_DATE_QUERY = "SELECT "
      + "end_date FROM data_file WHERE file_definition_id IN "
      + "(SELECT id FROM file_definition WHERE instrument_id = ?) "
      + "ORDER BY end_date DESC LIMIT 1";

  private static final String FIND_FILE_BY_NAME_QUERY = "SELECT "
      + "id FROM data_file WHERE filename = ? AND file_definition_id IN "
      + "(SELECT id FROM file_definition WHERE instrument_id = ?)";

  /**
   * Store a file in the database and in the file store
   * @param dataSource A data source
   * @param appConfig The application configuration
   * @param dataFile The data file
   * @throws MissingParamException If any of the parameters are missing
   * @throws FileExistsException If the file already exists in the system
   * @throws DatabaseException If an error occurs while storing the file
   * @throws RecordNotFoundException
   * @see #ADD_FILE_STATEMENT
   * @see FileStore#storeFile(String, DataFile)
   */
  public static void storeFile(DataSource dataSource, Properties appConfig,
      DataFile dataFile,long replacementId)
      throws MissingParamException, FileExistsException, DatabaseException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(appConfig, "appConfig");
    MissingParam.checkMissing(dataFile, "dataFile");

    Connection conn = null;

    try {
      conn = dataSource.getConnection();

      if (replacementId > -1) {
        if (!fileExists(conn, replacementId)) {
          throw new RecordNotFoundException("Tried to replace a file that doesn't exist (id "
            + replacementId + ")");
        }

        replaceFile(conn, appConfig, dataFile, replacementId);
      } else {
        storeNewFile(conn, appConfig, dataFile);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing file", e);
    }
  }

    /**
     * Store a file in the database and in the file store
     * @param dataSource A data source
     * @param appConfig The application configuration
     * @param dataFile The data file
     * @throws FileExistsException If the file already exists in the system
     * @throws DatabaseException If an error occurs while storing the file
     * @see #ADD_FILE_STATEMENT
     * @see FileStore#storeFile(String, DataFile)
     */
  private static void storeNewFile(Connection conn, Properties appConfig, DataFile dataFile)
      throws DatabaseException, FileExistsException {

    PreparedStatement stmt = null;
    ResultSet generatedKeys = null;

    try {
      if (fileExistsWithDates(conn, dataFile.getFileDefinition().getDatabaseId(),
          dataFile.getStartDate(), dataFile.getEndDate())) {
        throw new FileExistsException(dataFile.getFileDescription(),
            dataFile.getStartDate(), dataFile.getEndDate());
      }

      conn.setAutoCommit(false);
      stmt = conn.prepareStatement(ADD_FILE_STATEMENT, Statement.RETURN_GENERATED_KEYS);
      stmt.setLong(1, dataFile.getFileDefinition().getDatabaseId());
      stmt.setString(2, dataFile.getFilename());
      stmt.setLong(3, DateTimeUtils.dateToLong(dataFile.getStartDate()));
      stmt.setLong(4, DateTimeUtils.dateToLong(dataFile.getEndDate()));
      stmt.setInt(5, dataFile.getRecordCount());

      stmt.execute();

      generatedKeys = stmt.getGeneratedKeys();
      if (generatedKeys.next()) {

        dataFile.setDatabaseId(generatedKeys.getLong(1));

        // Store the file
        FileStore.storeFile(appConfig.getProperty("filestore"), dataFile);

        conn.commit();
      }
    } catch (FileExistsException e) {
      throw e;
    } catch (Exception e) {
      try {
        DatabaseUtils.rollBack(conn);
      } catch (Exception e2) {
        //Do nothing
      }

      throw new DatabaseException("An error occurred while storing the file", e);
    } finally {
      DatabaseUtils.closeResultSets(generatedKeys);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Store a file in the database and in the file store
   * @param dataSource A data source
   * @param appConfig The application configuration
   * @param dataFile The data file
   * @throws FileExistsException If the file already exists in the system
   * @throws DatabaseException If an error occurs while storing the file
   * @see #ADD_FILE_STATEMENT
   * @see FileStore#storeFile(String, DataFile)
   */
  private static void replaceFile(Connection conn, Properties appConfig, DataFile dataFile, long replacementId)
    throws DatabaseException, FileExistsException {

    PreparedStatement stmt = null;

    try {
      conn.setAutoCommit(false);
      stmt = conn.prepareStatement(REPLACE_FILE_STATEMENT);
      stmt.setString(1, dataFile.getFilename());
      stmt.setLong(2, DateTimeUtils.dateToLong(dataFile.getStartDate()));
      stmt.setLong(3, DateTimeUtils.dateToLong(dataFile.getEndDate()));
      stmt.setInt(4, dataFile.getRecordCount());
      stmt.setLong(5, replacementId);

      stmt.execute();

      // Set the database ID on the file now the replacement has succeeded
      dataFile.setDatabaseId(replacementId);

      // Store the file - automatically replaces the old one
      FileStore.storeFile(appConfig.getProperty("filestore"), dataFile);


      conn.commit();
    } catch (Exception e) {
      try {
        DatabaseUtils.rollBack(conn);
      } catch (Exception e2) {
        //Do nothing
      }

      throw new DatabaseException("An error occurred while storing the file", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

/**
   * Determine whether a file of a given type already exists covering at
   * least part of the specified date range
   * @param dataSource A data source
   * @param fileDefinitionId The file definition database ID
   * @param startDate The start of the range
   * @param endDate The end of the range
   * @return {@code true} if a file overlapping the date range exists; {@code false} otherwise
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static boolean fileExistsWithDates(DataSource dataSource, long fileDefinitionId, LocalDateTime startDate, LocalDateTime endDate) throws MissingParamException, DatabaseException {

    boolean result;

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = fileExistsWithDates(conn, fileDefinitionId, startDate, endDate);
    } catch (SQLException e) {
      throw new DatabaseException("Error while finding existing files", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Determine whether a file of a given type already exists covering at
   * least part of the specified date range
   * @param conn A database connection
   * @param fileDefinitionId The file definition database ID
   * @param startDate The start of the range
   * @param endDate The end of the range
   * @return {@code true} if a file overlapping the date range exists; {@code false} otherwise
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static boolean fileExistsWithDates(Connection conn, long fileDefinitionId, LocalDateTime startDate, LocalDateTime endDate) throws MissingParamException, DatabaseException {

    boolean exists;

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(fileDefinitionId, "fileDefinitionId");
    MissingParam.checkMissing(startDate, "startDate");
    MissingParam.checkMissing(endDate, "endDate");

    PreparedStatement stmt = null;
    ResultSet count = null;

    try {
      stmt = conn.prepareStatement(FILE_EXISTS_WITH_DATES_QUERY);
      stmt.setLong(1, fileDefinitionId);
      stmt.setLong(2, DateTimeUtils.dateToLong(startDate));
      stmt.setLong(3, DateTimeUtils.dateToLong(endDate));
      stmt.setLong(4, DateTimeUtils.dateToLong(startDate));
      stmt.setLong(5, DateTimeUtils.dateToLong(endDate));

      count = stmt.executeQuery();
      if (!count.next()) {
        throw new DatabaseException("No count record from query");
      } else {
        exists = (count.getInt(1) > 0);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while finding existing files", e);
    } finally {
      DatabaseUtils.closeResultSets(count);
      DatabaseUtils.closeStatements(stmt);
    }

    return exists;
  }

  /**
   * Determines whether a file with the specified ID exists in the database
   * @param conn A database connection
   * @param fileId The file ID
   * @return {@code true} if the file exists; {@code false} if it does not
   * @throws MissingParamException If any parameters are missing
   * @throws DatabaseException If an error occurs
   * @throws RecordNotFoundException If the file disappears between locating it and reading its details
   */
  public static boolean fileExists(Connection conn, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {

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
      throw new DatabaseException("An error occurred while checking file existence", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Checks to see if a file with a given name and for a given instrument
   * already exists in the system
   * @param dataSource A data source
   * @param instrumentID The instrument ID
   * @param fileName The name of the file
   * @return {@code true} if the file exists; {@code false} if it does not
   * @throws MissingParamException If any of the parameters are missing
   * @throws DatabaseException If an error occurs during the search
   * @see #FIND_FILE_QUERY
   */
  @Deprecated
  public static boolean fileExists(DataSource dataSource, long instrumentID, String fileName) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(instrumentID, "instrumentID");
    MissingParam.checkMissing(fileName, "fileName");

    boolean result = false;

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(FIND_FILE_QUERY);
      stmt.setLong(1, instrumentID);
      stmt.setString(2, fileName);
      records = stmt.executeQuery();
      result = records.next();

    } catch (SQLException e) {
      throw new DatabaseException("An error occurred while searching for an existing file", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Returns a list of all the files owned by a specific user. The list
   * can optionally be restricted by an instrument ID.
   * @param dataSource A data source
   * @param appConfig The application configuration
   * @param user The user
   * @param instrumentId The instrument ID used to filter the list (optional)
   * @return The list of files
   * @throws DatabaseException If an error occurs during the search
   * @see #GET_USER_FILES_QUERY
   * @see #GET_USER_FILES_BY_INSTRUMENT_QUERY
   * @see #makeDataFile(ResultSet, String, Connection)
   */
  public static List<DataFile> getUserFiles(DataSource dataSource, Properties appConfig, User user, Long instrumentId) throws DatabaseException {

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;
    List<DataFile> fileInfo = new ArrayList<DataFile>();

    try {
      conn = dataSource.getConnection();

      if (null != instrumentId) {
        stmt = conn.prepareStatement(GET_USER_FILES_BY_INSTRUMENT_QUERY);
      } else {
        stmt = conn.prepareStatement(GET_USER_FILES_QUERY);
      }

      stmt.setLong(1, user.getDatabaseID());

      if (null != instrumentId) {
        stmt.setLong(2, instrumentId);
      }

      records = stmt.executeQuery();
      while (records.next()) {
        fileInfo.add(makeDataFile(records, appConfig.getProperty("filestore"), conn));
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new DatabaseException("An error occurred while searching for files", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return fileInfo;
  }

  /**
   * Get the {@link DataFile} objects for a set of files
   * @param conn A datbase connection
   * @param appConfig The application configuration
   * @param ids The file ids
   * @return The DataFile objects
   * @throws MissingParamException If any of the parameters are missing
   * @throws DatabaseException If an error occurs during the search
   * @throws RecordNotFoundException If any files are not in the database
   */
  public static List<DataFile> getDataFiles(Connection conn, Properties appConfig, List<Long> ids) throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(appConfig, "appConfig");
    MissingParam.checkMissing(ids, "ids");

    List<DataFile> files = new ArrayList<DataFile>(ids.size());
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      stmt = conn.prepareStatement(DatabaseUtils.makeInStatementSql(GET_FILENAME_QUERY, ids.size()));
      for (int i = 0; i < ids.size(); i++) {
        stmt.setLong(i + 1, ids.get(i));
      }

      records = stmt.executeQuery();
      while (records.next()) {
        files.add(makeDataFile(records, appConfig.getProperty("filestore"), conn));
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
   * @param record The database record
   * @param fileStore The file store location
   * @param conn A database connection
   * @return The DataFile object
   * @throws SQLException If the record cannot be read
   * @throws DatabaseException If any sub-queries fail
   */
  private static DataFile makeDataFile(ResultSet record, String fileStore, Connection conn) throws SQLException, DatabaseException {
    DataFile result = null;

    try {
      long fileDefinitionId = record.getLong(2);
      long instrumentId = record.getLong(7);

      InstrumentFileSet files = InstrumentDB.getFileDefinitions(conn, instrumentId);

      result = makeDataFile(record, fileStore, files.get(fileDefinitionId));
    } catch (SQLException e) {
      throw e;
    } catch (Exception e) {
      throw new DatabaseException("Error retrieving file definition details", e);
    }

    return result;
  }

  /**
   * Build a {@link DataFile} object from a database record
   * @param record The record
   * @param fileStore The file store location
   * @param fileDefinition The file definition for the file
   * @return The DataFile object
   * @throws SQLException If the data cannot be extracted from the record
   */
  private static DataFile makeDataFile(ResultSet record, String fileStore, FileDefinition fileDefinition) throws SQLException {
    DataFile result = null;

    try {
      long id = record.getLong(1);
      String filename = record.getString(3);
      LocalDateTime startDate = DateTimeUtils.longToDate(record.getLong(4));
      LocalDateTime endDate = DateTimeUtils.longToDate(record.getLong(5));
      int recordCount = record.getInt(6);

      result = new DataFile(fileStore, id, fileDefinition, filename, startDate, endDate, recordCount);
    } catch (SQLException e) {
      throw e;
    }

    return result;
  }

  /**
   * Removes a file from the database and the underlying file store.
   * @param dataSource A data source
   * @param appConfig The application configuration
   * @param dataFile The data file
   * @throws MissingParamException If any parameters are missing
   * @throws DatabaseException If an error occurs during deletion
   * @see #DELETE_FILE_STATEMENT
   * @see FileStore#deleteFile(String, DataFile)
   */
  public static void deleteFile(DataSource dataSource, Properties appConfig, DataFile dataFile) throws MissingParamException, DatabaseException {

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
      throw new DatabaseException("An error occurred while deleting the data file", e);
    } finally {
      try {
        conn.rollback();
      } catch (SQLException e2) {
        // Not much we can do
      }

      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Retrieves the database ID of the instrument with which the specified data file is associated.
   * @param dataSource A data source
   * @param fileId The database ID of the data file
   * @return The database ID of the instrument
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the specified data file does not exist
   * @see #GET_INSTRUMENT_ID_QUERY
   */
  @Deprecated
  public static long getInstrumentId(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    // TODO Remove in the long run
    return -1;
  }

  /**
   * Specify the type of job that is currently being run (or queued to be run) on the specified file.
   *
   * The job is specified as a Job Code, so that it is not the specific {@link Job} object that
   * is referenced, but the type of Job. These Job Codes are defined in the {@link FileInfo} class.
   *
   * @param conn A database connection
   * @param fileId The database ID of the data file
   * @param jobCode The Job Code of the job that is running (or will be run)
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @see FileInfo
   * @see #SET_JOB_STATEMENT
   */
  @Deprecated
  public static void setCurrentJob(Connection conn, long fileId, int jobCode) throws MissingParamException, DatabaseException {
    // TODO remove
  }

  /**
   * Set the delete flag on a data file. If set to {@code true}, this will
   * indicate that the file should be deleted.
   *
   * <p>
   *   Files that have their delete flag set will not appear in the system
   *   as far as users are concerned. A background task will perform the actual deletion.
   * </p>
   * @param dataSource A data source
   * @param fileId The database ID of the data file
   * @param deleteFlag The delete flag
   * @throws MissingParamException If any of the parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the specified data file does not exist in the database
   * @see #SET_DELETE_FLAG_STATEMENT
   */
  @Deprecated
  public static void setDeleteFlag(DataSource dataSource, long fileId, boolean deleteFlag) throws MissingParamException, DatabaseException, RecordNotFoundException {
  }

  /**
   * Get the delete flag for a data file.
   * @param dataSource A data source
   * @param fileId The database ID of the data file
   * @return {@code true} if the file is marked for deletion; {@code false} if it is not
   * @throws MissingParamException If any parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the specified data file does not exist in the database
   */
  @Deprecated
  public static boolean getDeleteFlag(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    // TODO remove
    return false;
  }


  /**
   * Get the delete flag for a data file.
   * @param conn A database connection
   * @param fileId The database ID of the data file
   * @return {@code true} if the file is marked for deletion; {@code false} if it is not
   * @throws MissingParamException If any parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If the specified data file does not exist in the database
   * @see #GET_DELETE_FLAG_STATEMENT
   */
  @Deprecated
  public static boolean getDeleteFlag(Connection conn, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    return false;
  }

  /**
   * Retrieves a list of all data files that have their delete flag set
   * @param dataSource A data source
   * @return The list of data files that have their delete flag set
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @see #GET_DELETE_FLAG_FILES_QUERY
   */
  @Deprecated
  public static List<Long> getFilesWithDeleteFlag(DataSource dataSource) throws MissingParamException, DatabaseException {
    // TODO Remove
    return null;
  }

  /**
   * Get the data files for a given file definition that are covered by the supplied date range
   * @param dataSource A data source
   * @param fileDefinition The file definition
   * @param start The start date
   * @param end The end date
   * @return The matched files
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   * @throws RecordNotFoundException If no files are found
   */
  public static List<DataFile> getFiles(DataSource dataSource, FileDefinition fileDefinition, LocalDateTime start, LocalDateTime end) throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(fileDefinition, "fileDefinition");
    MissingParam.checkMissing(start, "start");
    MissingParam.checkMissing(end, "end");

    List<DataFile> files = new ArrayList<DataFile>();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_FILES_BY_TYPE_DATE_QUERY);
      stmt.setLong(1, fileDefinition.getDatabaseId());
      stmt.setLong(2, DateTimeUtils.dateToLong(end));
      stmt.setLong(3, DateTimeUtils.dateToLong(start));
      stmt.setLong(4, DateTimeUtils.dateToLong(end));
      stmt.setLong(5, DateTimeUtils.dateToLong(start));

      records = stmt.executeQuery();

      while (records.next()) {
        files.add(makeDataFile(records, ResourceManager.getInstance().getConfig().getProperty("filestore"), fileDefinition));
      }

      if (files.size() == 0) {
        throw new RecordNotFoundException("No files found");
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting data files", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return files;
  }

  /**
   * Get the list of data files of a given file definition
   * that encompass two dates
   * @param dataSource A data source
   * @param instrumentId The file definition
   * @param start The start date
   * @param end The end date
   * @return
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<DataFile> getFilesWithinDates(DataSource dataSource, FileDefinition fileDefinition,
      LocalDateTime start, LocalDateTime end) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(fileDefinition, "fileDefinition");
    MissingParam.checkMissing(start, "start");
    MissingParam.checkMissing(end, "end");

    if (end.equals(start) || end.isBefore(start) ) {
      throw new IllegalArgumentException("End date must be after start date");
    }

    List<DataFile> files = new ArrayList<DataFile>();
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_FILEDEF_FILES_WITHIN_DATES_QUERY);
      stmt.setLong(1, fileDefinition.getDatabaseId());
      stmt.setLong(2, DateTimeUtils.dateToLong(end));
      stmt.setLong(3,  DateTimeUtils.dateToLong(start));

      records = stmt.executeQuery();
      while (records.next()) {
        files.add(makeDataFile(records, ResourceManager.getInstance().getConfig().getProperty("filestore"),
            fileDefinition));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting files in date range", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return files;
  }


  /**
   * Get the list of data files for a given instrument
   * that encompass two dates
   * @param dataSource A data source
   * @param instrumentId The instrument ID
   * @param start The start date
   * @param end The end date
   * @return
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<Long> getFilesWithinDates(Connection conn, long instrumentId,
      LocalDateTime start, LocalDateTime end) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(start, "start");
    MissingParam.checkMissing(end, "end");

    if (end.equals(start) || end.isBefore(start) ) {
      throw new IllegalArgumentException("End date must be after start date");
    }

    List<Long> ids = new ArrayList<Long>();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_FILES_WITHIN_DATES_QUERY);
      stmt.setLong(1, instrumentId);
      stmt.setLong(2, DateTimeUtils.dateToLong(end));
      stmt.setLong(3,  DateTimeUtils.dateToLong(start));

      records = stmt.executeQuery();
      while (records.next()) {
        ids.add(records.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting files in date range", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return ids;
  }

  /**
   * Determine whether or not there is a complete set of files
   * available after a given time, from which a dataset can be made.
   * @param conn A database connection
   * @param instrumentId The ID of the instrument for which files must be found
   * @param time The time boundary
   * @return {@code true} if a complete set of files is available; {@code false} if not
   * @throws RecordNotFoundException
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public static boolean completeFilesAfter(Connection conn, Properties appConfig, long instrumentId, LocalDateTime time) throws MissingParamException, DatabaseException, RecordNotFoundException {
    boolean result = true;

    // If no time is specified, we can use all files
    if (null == time) {
      time = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
    }

    List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
    List<ResultSet> resultSets = new ArrayList<ResultSet>();
    try {
      List<FileDefinition> fileDefinitions = InstrumentDB.getFileDefinitions(conn, instrumentId);
      Map<FileDefinition, List<DataFile>> filesAfterDate = new HashMap<FileDefinition, List<DataFile>>();

      // Get all the files after the specified date, grouped by file definition
      for (FileDefinition fileDefinition : fileDefinitions) {
        List<DataFile> foundFiles = new ArrayList<DataFile>();

        PreparedStatement stmt = conn.prepareStatement(GET_FILES_AFTER_DATE_QUERY);
        stmt.setLong(1, fileDefinition.getDatabaseId());
        stmt.setLong(2, DateTimeUtils.dateToLong(time));

        ResultSet records = stmt.executeQuery();
        while (records.next()) {
          foundFiles.add(makeDataFile(records, appConfig.getProperty("filestore"), conn));
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
            List<DataFile> compareFiles = filesAfterDate.get(fileDefinitions.get(i));

            for (int j = 0; j < compareFiles.size() && !result; j++) {
              DataFile compareFile = compareFiles.get(j);
              if (compareFile.getStartDate().compareTo(rootFile.getEndDate()) < 0 && compareFile.getEndDate().compareTo(rootFile.getStartDate()) > 0) {
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
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The last date, or {@code null} if there are no files
   * @throws DatabaseException
   * @throws MissingParamException
   */
  public static LocalDateTime getLastFileDate(Connection conn, long instrumentId) throws MissingParamException, DatabaseException {
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
   * @param dataSource A data source
   * @param instrumentId The instrument's database ID
   * @param filename The filename
   * @return {@code true} if a file exists; {@code false} if it does not
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static boolean hasFileWithName(DataSource dataSource, long instrumentId, String filename) throws DatabaseException, MissingParamException {
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
}
