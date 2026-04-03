package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import javax.sql.DataSource;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
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
 * @see FileStore
 */
public class DataFileDB {

  /**
   * Statement to add a data file to the database
   *
   * @see #storeNewFile(DataSource, Properties, DataFile)
   */
  private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file "
    + "(file_definition_id, filename, start, end, record_count, "
    + "properties) VALUES (?, ?, ?, ?, ?, ?)";

  /**
   * Statement to add a data file to the database
   *
   * @see #replaceFile(DataSource, Properties, DataFile, long)
   */
  private static final String REPLACE_FILE_STATEMENT = "UPDATE data_file "
    + "SET filename = ?, start = ?, end = ?, record_count = ?, properties = ? "
    + "WHERE id = ?";

  /**
   * Query to get a set of data files by their ID
   */
  private static final String GET_FILENAME_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start, "
    + "f.end, f.record_count, f.properties, i.id FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id " + "WHERE f.id IN "
    + DatabaseUtils.IN_PARAMS_TOKEN;

  /**
   * Query to find all the data files owned by a given user
   *
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start, f.end, "
    + "f.record_count, f.properties, i.id FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id";

  /**
   * Query to find all the data files owned by a given user
   *
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_BY_INSTRUMENT_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start, f.end, "
    + "f.record_count, f.properties FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "INNER JOIN instrument AS i ON d.instrument_id = i.id "
    + "WHERE d.instrument_id = ?";

  /**
   * Query to find all the data files owned by a given user
   *
   * @see #getUserFiles(DataSource, User)
   */
  private static final String GET_FILES_BY_DEFINITION_QUERY = "SELECT "
    + "f.id, f.file_definition_id, f.filename, f.start, f.end, "
    + "f.record_count, f.properties, d.id FROM data_file AS f "
    + "INNER JOIN file_definition AS d ON f.file_definition_id = d.id "
    + "WHERE d.id = ?";

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

  private static final String GET_USED_FILES_QUERY = "SELECT "
    + "datafile_id FROM dataset_files WHERE dataset_id = ?";

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
   * @see #REPLACE_FILE_STATEMENT
   * @see FileStore#storeFile(String, DataFile)
   */
  public static void storeFile(DataSource dataSource, Properties appConfig,
    Instrument instrument, DataFile dataFile, long replacementId)
    throws MissingParamException, FileExistsException, DatabaseException,
    RecordNotFoundException {

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
        storeNewFile(conn, appConfig, instrument, dataFile);
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
    Instrument instrument, DataFile dataFile)
    throws DatabaseException, FileExistsException {

    PreparedStatement stmt = null;
    ResultSet generatedKeys = null;

    try {
      TreeSet<DataFile> existingFiles = getFiles(conn, instrument,
        dataFile.getFileDefinition());

      if (dataFile.getOverlappingFiles(existingFiles).size() > 0) {
        throw new FileExistsException(dataFile.getFileDescription());
      }

      stmt = conn.prepareStatement(ADD_FILE_STATEMENT,
        Statement.RETURN_GENERATED_KEYS);
      stmt.setLong(1, dataFile.getFileDefinition().getDatabaseId());
      stmt.setString(2, dataFile.getFilename());
      stmt.setString(3, dataFile.getStartString());
      stmt.setString(4, dataFile.getEndString());
      stmt.setInt(5, dataFile.getRecordCount());
      stmt.setString(6, new Gson().toJson(dataFile.getProperties()));

      stmt.execute();

      generatedKeys = stmt.getGeneratedKeys();
      if (generatedKeys.next()) {
        dataFile.setDatabaseId(generatedKeys.getLong(1));

        // Store the file
        FileStore.storeFile(appConfig.getProperty("filestore"), dataFile);
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
   * @see #REPLACE_FILE_STATEMENT
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
        DataFile fileToReplace = getDataFiles(conn, idList).first();

        byte[] existingFile = fileToReplace.getBytes();
        byte[] newFile = dataFile.getBytes();
        storeFile = !Arrays.equals(existingFile, newFile);
      }

      if (storeFile) {
        boolean initialAutoCommit = conn.getAutoCommit();

        if (initialAutoCommit) {
          conn.setAutoCommit(false);
        }
        stmt = conn.prepareStatement(REPLACE_FILE_STATEMENT);
        stmt.setString(1, dataFile.getFilename());
        stmt.setString(2, dataFile.getStartString());
        stmt.setString(3, dataFile.getEndString());
        stmt.setInt(4, dataFile.getRecordCount());
        stmt.setString(5, new Gson().toJson(dataFile.getProperties()));
        stmt.setLong(6, replacementId);

        stmt.execute();

        // Set the database ID on the file now the replacement has succeeded
        dataFile.setDatabaseId(replacementId);

        // Store the file - automatically replaces the old one
        FileStore.storeFile(appConfig.getProperty("filestore"), dataFile);
      }
    } catch (Exception e) {
      DatabaseUtils.rollBack(conn);

      throw new DatabaseException("An error occurred while storing the file",
        e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
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
    MissingParam.checkDatabaseId(fileId, "fileId", false);

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
  public static TreeSet<DataFile> getFiles(DataSource dataSource,
    Instrument instrument) throws DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      return getFiles(conn, instrument);
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
  public static TreeSet<DataFile> getFiles(Connection conn,
    Instrument instrument) throws DatabaseException {

    PreparedStatement stmt = null;
    ResultSet records = null;
    TreeSet<DataFile> fileInfo = new TreeSet<DataFile>();

    try {
      if (null != instrument) {
        stmt = conn.prepareStatement(GET_FILES_BY_INSTRUMENT_QUERY);
      } else {
        stmt = conn.prepareStatement(GET_FILES_QUERY);
      }

      if (null != instrument) {
        stmt.setLong(1, instrument.getId());
      }

      records = stmt.executeQuery();
      while (records.next()) {
        fileInfo.add(makeDataFile(records, instrument));
      }

    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
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
  public static TreeSet<DataFile> getFiles(DataSource dataSource,
    Instrument instrument, FileDefinition fileDefinition)
    throws DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      return getFiles(conn, instrument, fileDefinition);
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
  public static TreeSet<DataFile> getFiles(Connection conn,
    Instrument instrument, FileDefinition fileDefinition)
    throws DatabaseException {

    PreparedStatement stmt = null;
    ResultSet records = null;
    TreeSet<DataFile> fileInfo = new TreeSet<DataFile>();

    try {
      stmt = conn.prepareStatement(GET_FILES_BY_DEFINITION_QUERY);
      stmt.setLong(1, fileDefinition.getDatabaseId());

      records = stmt.executeQuery();
      while (records.next()) {
        fileInfo.add(makeDataFile(records, instrument, fileDefinition));
      }

    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
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
  public static TreeSet<DataFile> getDataFiles(Connection conn, List<Long> ids)
    throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(ids, "ids");

    TreeSet<DataFile> files = new TreeSet<DataFile>();
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
        files.add(makeDataFile(records, conn));
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
  private static DataFile makeDataFile(ResultSet record, Connection conn)
    throws SQLException, DatabaseException {
    DataFile result = null;

    try {
      long instrumentId = record.getLong(8);
      Instrument instrument = InstrumentDB.getInstrument(conn, instrumentId);
      result = makeDataFile(record, instrument);
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
  private static DataFile makeDataFile(ResultSet record, Instrument instrument)
    throws SQLException, DatabaseException {
    DataFile result = null;

    try {
      long fileDefinitionId = record.getLong(2);
      result = makeDataFile(record, instrument,
        instrument.getFileDefinitions().get(fileDefinitionId));
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
   * @throws ClassNotFoundException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  private static DataFile makeDataFile(ResultSet record, Instrument instrument,
    FileDefinition fileDefinition) throws SQLException, DataFileException {

    DataFile result = null;

    try {
      long id = record.getLong(1);
      String filename = record.getString(3);
      String start = record.getString(4);
      String end = record.getString(5);
      int recordCount = record.getInt(6);
      Properties properties = new Gson().fromJson(record.getString(7),
        Properties.class);

      try {
        Constructor<? extends DataFile> constructor = fileDefinition
          .getFileClass().getConstructor(long.class, Instrument.class,
            FileDefinition.class, String.class, String.class, String.class,
            int.class, Properties.class);
        result = constructor.newInstance(id, instrument, fileDefinition,
          filename, start, end, recordCount, properties);
      } catch (Exception e) {
        throw new DataFileException("Failed to construct DataFile object", e);
      }
    } catch (SQLException e) {
      throw e;
    }

    return result;
  }

  public static void deleteFile(DataSource dataSource, Properties appConfig,
    DataFile dataFile) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");

    try (Connection conn = dataSource.getConnection()) {
      deleteFile(conn, appConfig, dataFile);
    } catch (SQLException e) {
      throw new DatabaseException("Error deleting files", e);
    }
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
  public static void deleteFile(Connection conn, Properties appConfig,
    DataFile dataFile) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(appConfig, "appConfig");
    MissingParam.checkMissing(dataFile, "dataFile");

    PreparedStatement stmt = null;

    try {
      conn.setAutoCommit(false);

      // Send out sub-record delete requests
      stmt = conn.prepareStatement(DELETE_FILE_STATEMENT);
      stmt.setLong(1, dataFile.getDatabaseId());
      stmt.execute();

      // Delete the file from the file store
      FileStore.deleteFile(appConfig.getProperty("filestore"), dataFile);
    } catch (SQLException e) {
      DatabaseUtils.rollBack(conn);
      throw new DatabaseException(
        "An error occurred while deleting the data file", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
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
    MissingParam.checkDatabaseId(instrumentId, "instrumentId", false);
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
    MissingParam.checkDatabaseId(instrumentId, "instrumentId", false);

    int fileCount = 0;

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn.prepareStatement(GET_FILE_COUNT_QUERY);) {

      stmt.setLong(1, instrumentId);

      try (ResultSet records = stmt.executeQuery()) {
        records.next();
        fileCount = records.getInt(1);
      } catch (SQLException e) {
        throw e;
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while getting instrument files", e);
    }

    return fileCount;
  }

  public static void deleteAllFiles(Connection conn, Properties appConfig,
    Instrument instrument, boolean deleteFolders)
    throws DatabaseException, FileStoreException, IOException {

    TreeSet<DataFile> files = getFiles(conn, instrument);
    for (DataFile file : files) {
      deleteFile(conn, appConfig, file);
    }

    if (deleteFolders) {
      for (FileDefinition fileDefinition : instrument.getFileDefinitions()) {
        FileStore.deleteFolder(appConfig.getProperty("filestore"),
          fileDefinition.getDatabaseId());
      }
    }
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
    long instrumentId) throws DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkDatabaseId(instrumentId, "instrumentId", false);

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

  /**
   * Get the files used by a {@link DataSet}.
   *
   * @param conn
   *          A database connection.
   * @param config
   *          The application configuration.
   * @param dataset
   *          The {@link DataSet}.
   * @return The files used by the {@link DataSet}.
   * @throws DatabaseException
   * @throws RecordNotFoundException
   */
  public static TreeSet<DataFile> getDatasetFiles(Connection conn,
    DataSet dataset) throws DatabaseException, RecordNotFoundException {

    PreparedStatement usedFilesStmt = null;
    ResultSet records = null;

    try {
      usedFilesStmt = conn.prepareStatement(GET_USED_FILES_QUERY);
      usedFilesStmt.setLong(1, dataset.getId());

      List<Long> fileIds = new ArrayList<Long>();

      records = usedFilesStmt.executeQuery();
      while (records.next()) {
        fileIds.add(records.getLong(1));
      }

      return getDataFiles(conn, fileIds);
    } catch (SQLException e) {
      throw new DatabaseException("Error getting dataset files", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(usedFilesStmt);
    }
  }

  public static FileContents getFileContents(long fileDefinitionId,
    long fileId) {

    String fileStore = ResourceManager.getInstance().getConfig()
      .getProperty("filestore");

    return new FileStoreFileContents(fileStore, fileDefinitionId, fileId);
  }
}
