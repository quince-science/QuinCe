package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.data.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunType;
import uk.ac.exeter.QuinCe.data.QC.QCDB;
import uk.ac.exeter.QuinCe.jobs.Job;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.FileJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

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
	 * @see #storeFile(DataSource, Properties, User, long, RawDataFile)
	 */
	private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file (instrument_id, filename, start_date, record_count, current_job, last_touched) "
		+ "VALUES (?, ?, ?, ?, ?, ?)";

	/**
	 * Query to find all the data files owned by a given user
	 * @see #getUserFiles(DataSource, User)
	 */
	private static final String GET_USER_FILES_QUERY = "SELECT f.id, i.id, i.name, f.filename, f.start_date, f.record_count, f.current_job, f.last_touched, f.delete_flag FROM instrument AS i INNER JOIN data_file AS f ON i.id = f.instrument_id"
			+ " WHERE f.delete_flag = 0 ORDER BY f.last_touched DESC";
	
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
	private static final String FIND_FILE_BY_ID_QUERY = "SELECT f.id, i.id, i.name, f.filename, f.start_date, f.record_count, f.current_job, f.last_touched, f.delete_flag FROM instrument AS i INNER JOIN data_file AS f ON i.id = f.instrument_id"
			+ " WHERE f.id = ?";
	
	/**
	 * Query to get the database ID of the instrument associated with a given data file
	 * @see #getInstrumentId(DataSource, long)
	 */
	private static final String GET_INSTRUMENT_ID_QUERY = "SELECT instrument_id FROM data_file WHERE id = ?";
	
	/**
	 * Statement to set the ID of the job currently being run on a given data file
	 * @see #setCurrentJob(Connection, long, int)
	 */
	private static final String SET_JOB_STATEMENT = "UPDATE data_file SET current_job = ? WHERE id = ?";
	
	/**
	 * Query to retrieve the number of atmospheric CO<sub>2</sub> measurements in a given data file
	 * @see #makeFileInfo(ResultSet, Connection)
	 */
	private static final String ATMOSPHERIC_MEAS_COUNT_QUERY = "SELECT COUNT(*) FROM raw_data WHERE data_file_id = ? AND co2_type = " + RunType.RUN_TYPE_ATMOSPHERIC;
	
	/**
	 * Query to retrieve the number of ocean CO<sub>2</sub> measurements in a given data file
	 * @see #makeFileInfo(ResultSet, Connection)
	 */
	private static final String OCEAN_MEAS_COUNT_QUERY = "SELECT COUNT(*) FROM raw_data WHERE data_file_id = ? AND co2_type = " + RunType.RUN_TYPE_WATER;
	
	/**
	 * Query to retrieve the number of gas standard measurements in a given data file
	 * @see #makeFileInfo(ResultSet, Connection)
	 */
	private static final String STANDARDS_COUNT_QUERY = "SELECT COUNT(*) FROM gas_standards_data WHERE data_file_id = ?";
	
	/**
	 * Query to retrieve the number of records with each possible QC flag value for a given data file
     * Only marine measurements are included.
     * @see #updateFlagCounts(Connection, FileInfo)
	 */
	private static final String GET_QC_FLAGS_QUERY = "SELECT DISTINCT qc_flag, COUNT(qc_flag) FROM qc AS q INNER JOIN raw_data AS w on w.data_file_id = q.data_file_id AND w.row = q.row WHERE q.data_file_id = ? AND w.co2_type = 0 GROUP BY qc_flag";

	/**
	 * Query to retrieve the number of records with each possible WOCE flag value for a given data file
     * Only marine measurements are included.
     * @see #updateFlagCounts(Connection, FileInfo)
	 */
	private static final String GET_WOCE_FLAGS_QUERY = "SELECT DISTINCT woce_flag, COUNT(woce_flag) FROM qc AS q INNER JOIN raw_data AS w on w.data_file_id = q.data_file_id AND w.row = q.row WHERE q.data_file_id = ? AND w.co2_type = 0 GROUP BY woce_flag";

	/**
	 * Statement to set the given data file's {@code last_touched} value to the current time
	 * @see #touchFile(DataSource, long)
	 */
	private static final String TOUCH_FILE_STATEMENT = "UPDATE data_file SET last_touched = NOW() WHERE id = ?";
	
	/**
	 * Query to flag a data file for deletion
	 * @see #setDeleteFlag(DataSource, long, boolean)
	 */
	private static final String SET_DELETE_FLAG_STATEMENT = "UPDATE data_file SET delete_flag = ? WHERE id = ?";

	/**
	 * Query to get the delete flag for a given data file
	 * @see #getDeleteFlag(Connection, long)
	 */
	private static final String GET_DELETE_FLAG_STATEMENT = "SELECT delete_flag FROM data_file WHERE id = ?";
	
	/**
	 * Query to get the set of data files whose delete flags have been set
	 * @see #getFilesWithDeleteFlag(DataSource)
	 */
	private static final String GET_DELETE_FLAG_FILES_QUERY = "SELECT id FROM data_file WHERE delete_flag = 1";

	/**
	 * Store a file in the database and in the file store
	 * @param dataSource A data source
	 * @param appConfig The application configuration
	 * @param owner The user that uploaded the file. This user is designated as the file's owner for access permissions.
	 * @param instrumentID The instrument ID
	 * @param dataFile The data file
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws FileExistsException If the file already exists in the system
	 * @throws DatabaseException If an error occurs while storing the file
	 * @see #ADD_FILE_STATEMENT
	 * @see FileStore#storeFile(Properties, long, RawDataFile)
	 */
	public static void storeFile(DataSource dataSource, Properties appConfig, User owner, long instrumentID, RawDataFile dataFile) throws MissingParamException, FileExistsException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(appConfig, "appConfig");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(dataFile, "file");
		
		if (fileExists(dataSource, instrumentID, dataFile.getFileName())) {
			throw new FileExistsException(dataSource, instrumentID, dataFile.getFileName());
		}
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet generatedKeys = null;
		
		
		try {
			
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.prepareStatement(ADD_FILE_STATEMENT, Statement.RETURN_GENERATED_KEYS);
			stmt.setLong(1, instrumentID);
			stmt.setString(2, dataFile.getFileName());
			stmt.setDate(3, new java.sql.Date(dataFile.getStartDate().getTimeInMillis()));
			stmt.setInt(4, dataFile.getRecordCount());
			stmt.setInt(5, FileInfo.JOB_CODE_EXTRACT);
			stmt.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
			
			stmt.execute();
			
			generatedKeys = stmt.getGeneratedKeys();
			if (generatedKeys.next()) {
			
				Map<String, String> jobParameters = new HashMap<String, String>(1);
				jobParameters.put(FileJob.FILE_ID_KEY, String.valueOf(generatedKeys.getLong(1)));
				JobManager.addJob(conn, owner, FileInfo.getJobClass(FileInfo.JOB_CODE_EXTRACT), jobParameters);
				
				// Store the file
				FileStore.storeFile(appConfig, instrumentID, dataFile);

				conn.commit();
			}
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
	 * Determines whether a file with the specified ID exists in the database
	 * @param dataSource A data source
	 * @param fileId The file ID
	 * @return {@code true} if the file exists; {@code false} if it does not
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an error occurs
	 * @throws RecordNotFoundException If the database record disappears during checks. A very unlikely occurrence.
	 * @see #getFileDetails(Connection, long)
	 */
	public static boolean fileExists(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		return (null != getFileDetails(dataSource, fileId));
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
			throw new DatabaseException("An error occurred while search for the data file", e);
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
	 * Returns a list of all the files owned by a specific user
	 * @param dataSource A data source 
	 * @param user The user
	 * @return The list of files
	 * @throws DatabaseException If an error occurs during the search
	 * @see #GET_USER_FILES_QUERY
	 * @see #makeFileInfo(ResultSet, Connection)
	 */
	public static List<FileInfo> getUserFiles(DataSource dataSource, User user) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<FileInfo> fileInfo = new ArrayList<FileInfo>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_USER_FILES_QUERY);
			
			records = stmt.executeQuery();
			while (records.next()) {
				fileInfo.add(makeFileInfo(records, conn));
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
	 * Retrieve all the details of a specified data file. These are supplied as
	 * a {@link FileInfo} object.
	 * 
	 * @param dataSource A data source
	 * @param fileId The data file's database ID
	 * @return An object containing the file's details
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the specified data file does not exist
	 * @see #getFileDetails(Connection, long)
	 */
	public static FileInfo getFileDetails(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			return getFileDetails(conn, fileId);
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while searching for the file", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Returns the details of a specified data file
	 * @param conn A database connection
	 * @param fileId The database ID of the data file
	 * @return The file details
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any of the file's details cannot be found
	 * @see #FIND_FILE_BY_ID_QUERY
	 * @see #makeFileInfo(ResultSet, Connection)
	 */
	public static FileInfo getFileDetails(Connection conn, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
			
		FileInfo result = null;
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
				
		PreparedStatement stmt = null;
		ResultSet record = null;
		
		try {
			stmt = conn.prepareStatement(FIND_FILE_BY_ID_QUERY);
			stmt.setLong(1, fileId);
			record = stmt.executeQuery();
			if (record.next()) {
				result = makeFileInfo(record, conn);
			}
						
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while searching for the file", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return result;
	}
	
	/**
	 * Removes a file from the database and the underlying file store.
	 * @param dataSource A data source
	 * @param appConfig The application configuration
	 * @param fileId The database ID of the data file
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an error occurs during deletion
	 * @throws RecordNotFoundException If any database lookups fail
	 * @see #deleteFile(DataSource, Properties, FileInfo)
	 */
	public static void deleteFile(DataSource dataSource, Properties appConfig, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(appConfig, "appConfig");
		MissingParam.checkPositive(fileId, "fileId");
		deleteFile(dataSource, appConfig, getFileDetails(dataSource, fileId));
	}
	
	/**
	 * Removes a file from the database and the underlying file store.
	 * @param dataSource A data source
	 * @param appConfig The application configuration
	 * @param fileDetails The details of the file to be deleted
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an error occurs during deletion
	 * @see #DELETE_FILE_STATEMENT
	 * @see FileStore#deleteFile(Properties, FileInfo)
	 */
	public static void deleteFile(DataSource dataSource, Properties appConfig, FileInfo fileDetails) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(appConfig, "appConfig");
		MissingParam.checkMissing(fileDetails, "fileDetails");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			// Send out sub-record delete requests
			QCDB.clearQCData(conn, fileDetails.getFileId());
			DataReductionDB.clearDataReductionData(conn, fileDetails.getFileId());
			RawDataDB.clearRawData(conn, fileDetails.getFileId());
			
			stmt = conn.prepareStatement(DELETE_FILE_STATEMENT);
			stmt.setLong(1, fileDetails.getFileId());
			stmt.execute();
			
			// Delete the file from the file store
			FileStore.deleteFile(appConfig, fileDetails);
			
			conn.commit();
			
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while deleting the data file", e);
		} finally {
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
	public static long getInstrumentId(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(fileId, "fileId");
		
		long result = -1;
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet record = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_INSTRUMENT_ID_QUERY);
			stmt.setLong(1, fileId);
			
			record = stmt.executeQuery();
			if (!record.next()) {
				throw new RecordNotFoundException("Could not find file with id " + fileId);
			} else {
				result = record.getLong(1);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("An error occured while retrieving the file's instrument ID", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}
	
	/**
	 * Retrieve the contents of a data file from the file store
	 * @param dataSource A data source
	 * @param appConfig The application configuration
	 * @param fileId The file's database ID
	 * @return The data file
	 * @throws DatabaseException If an error occurs while retrieving the file details or contents
	 * @throws MissingParamException If any parameters are missing
	 * @throws RawDataFileException If the data file cannot be parsed
	 * @see FileStore#getFile(DataSource, Properties, FileInfo)
	 */
	public static RawDataFile getRawDataFile(DataSource dataSource, Properties appConfig, long fileId) throws MissingParamException, DatabaseException, RawDataFileException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(appConfig, "appConfig");
		MissingParam.checkPositive(fileId, "fileId");
		
		try {
			FileInfo fileInfo = getFileDetails(dataSource, fileId);
			return FileStore.getFile(dataSource, appConfig, fileInfo);
		} catch (RecordNotFoundException|IOException e) {
			throw new DatabaseException("An error occurred while retrieving the file from the file store", e);
		}
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
	public static void setCurrentJob(Connection conn, long fileId, int jobCode) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(SET_JOB_STATEMENT);
			stmt.setInt(1, jobCode);
			stmt.setLong(2, fileId);
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while setting the current job", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	/**
	 * <p>Create a {@link FileInfo} object from the current record in a ResultSet.</p>
	 * 
	 * <p>
	 *   This method expects the {@link ResultSet} to have come from the {@link #FIND_FILE_BY_ID_QUERY}.
	 *   If it has not, the method's behaviour is undefined. It is most likely that a {@link DatabaseException}
	 *   or {@link RecordNotFoundException} will be thrown.
	 * </p>  
	 * 
	 * @param record The {@link ResultSet} whose current record is to be read
	 * @return A {@link FileInfo} object for the record
	 * @throws SQLException If the record is not of the right format
	 * @throws RecordNotFoundException If the database record disappears while its details are being retrieved  
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @see #ATMOSPHERIC_MEAS_COUNT_QUERY
	 * @see #OCEAN_MEAS_COUNT_QUERY
	 * @see #STANDARDS_COUNT_QUERY
	 * @see #updateFlagCounts(Connection, FileInfo)
	 */
	private static FileInfo makeFileInfo(ResultSet record, Connection conn) throws SQLException, DatabaseException, MissingParamException, RecordNotFoundException {
		long fileID = record.getLong(1);
		long instrumentId = record.getLong(2);
		String instrumentName = record.getString(3);
		String fileName = record.getString(4);
		Calendar startDate = DateTimeUtils.getUTCCalendarInstance();
		startDate.setTime(record.getDate(5));
		int recordCount = record.getInt(6);
		int currentJob = record.getInt(7);
		Calendar lastTouched = DateTimeUtils.getUTCCalendarInstance();
		lastTouched.setTime(record.getDate(8));
		boolean deleteFlag = record.getBoolean(9);
	
		PreparedStatement atmosphericMeasurementsStmt = null;
		PreparedStatement oceanMeasurementsStmt = null;
		PreparedStatement standardsStmt = null;
		ResultSet atmosphericMeasurementsCount = null;
		ResultSet oceanMeasurementsCount = null;
		ResultSet standardsCount = null;
		int atmosphericMeasurements = 0;
		int oceanMeasurements = 0;
		int standards = 0;

		try {
			atmosphericMeasurementsStmt = conn.prepareStatement(ATMOSPHERIC_MEAS_COUNT_QUERY);
			atmosphericMeasurementsStmt.setLong(1, fileID);
			
			atmosphericMeasurementsCount = atmosphericMeasurementsStmt.executeQuery();
			if (atmosphericMeasurementsCount.next()) {
				atmosphericMeasurements = atmosphericMeasurementsCount.getInt(1);
			}
			
			oceanMeasurementsStmt = conn.prepareStatement(OCEAN_MEAS_COUNT_QUERY);
			oceanMeasurementsStmt.setLong(1, fileID);
			
			oceanMeasurementsCount = oceanMeasurementsStmt.executeQuery();
			if (oceanMeasurementsCount.next()) {
				oceanMeasurements = oceanMeasurementsCount.getInt(1);
			}
			
			standardsStmt = conn.prepareStatement(STANDARDS_COUNT_QUERY);
			standardsStmt.setLong(1, fileID);
			
			standardsCount = standardsStmt.executeQuery();
			if (standardsCount.next()) {
				standards = standardsCount.getInt(1);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			DatabaseUtils.closeResultSets(atmosphericMeasurementsCount, oceanMeasurementsCount, standardsCount);
			DatabaseUtils.closeStatements(atmosphericMeasurementsStmt, oceanMeasurementsStmt, standardsStmt);
		}

		
		FileInfo result = new FileInfo(fileID, instrumentId, instrumentName, fileName, startDate, recordCount, deleteFlag, currentJob, lastTouched, atmosphericMeasurements, oceanMeasurements, standards);
		updateFlagCounts(conn, result);
		return result;
	}
	
	/**
	 * <p>Update the flag counts for a specified data file.</p>
	 * 
	 * <p>
	 *   The file details to be updated are provided as a pre-existing {@link FileInfo}
	 *   object. The file's database ID is extracted and used to query the database. The
	 *   {@link FileInfo} object is then updated with the retrieved flag counts.
	 * </p>
	 * @param conn A database connection
	 * @param fileInfo The {@link FileInfo} for the file whose flag counts are to be updated
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the specified data file does not exist
	 * @throws MissingParamException If any required parameters in internal method calls are missing
	 * @see Flag
	 * @see #GET_QC_FLAGS_QUERY
	 * @see #GET_WOCE_FLAGS_QUERY
	 */
	public static void updateFlagCounts(Connection conn, FileInfo fileInfo) throws DatabaseException, RecordNotFoundException, MissingParamException {
		
		fileInfo.clearAllCounts();
		
		PreparedStatement qcFlagsStatement = null;
		PreparedStatement woceFlagsStatement = null;
		ResultSet qcFlags = null;
		ResultSet woceFlags = null;
		
		try {
			if (!fileExists(conn, fileInfo.getFileId())) {
				throw new RecordNotFoundException("Data file ID " + fileInfo.getFileId() + " does not exist");
			}
			qcFlagsStatement = conn.prepareStatement(GET_QC_FLAGS_QUERY);
			qcFlagsStatement.setLong(1, fileInfo.getFileId());
			
			qcFlags = qcFlagsStatement.executeQuery();
			
			while (qcFlags.next()) {
				switch (qcFlags.getInt(1)) {
				case Flag.VALUE_GOOD: {
					fileInfo.setQcGoodCount(qcFlags.getInt(2));
					break;
				}
				case Flag.VALUE_QUESTIONABLE: {
					fileInfo.setQcQuestionableCount(qcFlags.getInt(2));
					break;
				}
				case Flag.VALUE_BAD: {
					fileInfo.setQcBadCount(qcFlags.getInt(2));
					break;
				}
				case Flag.VALUE_FATAL: {
					fileInfo.setQcFatalCount(qcFlags.getInt(2));
					break;
				}
				case Flag.VALUE_NOT_SET: {
					fileInfo.setQcNotSetCount(qcFlags.getInt(2));
					break;
				}
				case Flag.VALUE_IGNORED: {
					fileInfo.setIgnoredCount(qcFlags.getInt(2));
					break;
				}
				default: {
					throw new DatabaseException("Invalid QC Flag value " + qcFlags.getInt(1));
				}
				}
			}
			
			woceFlagsStatement = conn.prepareStatement(GET_WOCE_FLAGS_QUERY);
			woceFlagsStatement.setLong(1, fileInfo.getFileId());
			
			woceFlags = woceFlagsStatement.executeQuery();
			
			while (woceFlags.next()) {
				switch (woceFlags.getInt(1)) {
				case Flag.VALUE_GOOD: {
					fileInfo.setWoceGoodCount(woceFlags.getInt(2));
					break;
				}
				case Flag.VALUE_ASSUMED_GOOD: {
					fileInfo.setWoceAssumedGoodCount(woceFlags.getInt(2));
					break;
				}
				case Flag.VALUE_QUESTIONABLE: {
					fileInfo.setWoceQuestionableCount(woceFlags.getInt(2));
					break;
				}
				case Flag.VALUE_BAD: {
					fileInfo.setWoceBadCount(woceFlags.getInt(2));
					break;
				}
				case Flag.VALUE_FATAL: {
					fileInfo.setWoceFatalCount(woceFlags.getInt(2));
					break;
				}
				case Flag.VALUE_NOT_SET: {
					fileInfo.setWoceNotSetCount(woceFlags.getInt(2));
					break;
				}
				case Flag.VALUE_NEEDED: {
					fileInfo.setWoceNeededCount(woceFlags.getInt(2));;
					break;
				}
				case Flag.VALUE_IGNORED: {
					fileInfo.setIgnoredCount(woceFlags.getInt(2));
					break;
				}
				default: {
					throw new DatabaseException("Invalid WOCE Flag value " + qcFlags.getInt(1));
				}
				}
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving flag data", e);
		} finally {
			DatabaseUtils.closeResultSets(qcFlags, woceFlags);
			DatabaseUtils.closeStatements(qcFlagsStatement, woceFlagsStatement);
		}
	}
	
	/**
	 * Set the 'Last Touched' property of the specified file to the current date and time.
	 * 
	 * @param dataSource A data source
	 * @param fileId The database ID of the data file
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @see #TOUCH_FILE_STATEMENT
	 */
	public static void touchFile(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(TOUCH_FILE_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Error while touching file", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
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
	public static void setDeleteFlag(DataSource dataSource, long fileId, boolean deleteFlag) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		
		Connection conn = null;
		PreparedStatement stmt= null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(SET_DELETE_FLAG_STATEMENT);
			stmt.setBoolean(1, deleteFlag);
			stmt.setLong(2, fileId);
			
			stmt.execute();
			if (stmt.getUpdateCount() == 0) {
				throw new RecordNotFoundException("Data file missing while setting delete flag", "data_file", fileId);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while setting delete flag", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
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
	public static boolean getDeleteFlag(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			return getDeleteFlag(conn, fileId);
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while searching for the file", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
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
	public static boolean getDeleteFlag(Connection conn, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
	
		boolean result = false;
		
		PreparedStatement stmt= null;
		ResultSet records = null;
		
		try {
			stmt = conn.prepareStatement(GET_DELETE_FLAG_STATEMENT);
			stmt.setLong(1, fileId);
			
			records = stmt.executeQuery();
			if (!records.next()) {
				throw new RecordNotFoundException("Data file missing while getting delete flag", "data_file", fileId);
			} else {
				result = records.getBoolean(1);
			}
			
			return result;
		} catch (SQLException e) {
			throw new DatabaseException("Error while setting delete flag", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	/**
	 * Retrieves a list of all data files that have their delete flag set
	 * @param dataSource A data source
	 * @return The list of data files that have their delete flag set
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @see #GET_DELETE_FLAG_FILES_QUERY
	 */
	public static List<Long> getFilesWithDeleteFlag(DataSource dataSource) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			List<Long> deleteFiles = new ArrayList<Long>();

			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_DELETE_FLAG_FILES_QUERY);
			records = stmt.executeQuery();
			while (records.next()) {
				deleteFiles.add(records.getLong(1));
			}
			
			return deleteFiles;
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for delete flag files", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}
}
