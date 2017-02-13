package uk.ac.exeter.QuinCe.database.files;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.data.RawDataFileException;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Calculation.DataReductionDB;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Methods for storing raw data files in the
 * database and on the file system
 * 
 * @author Steve Jones
 *
 */
public class DataFileDB {

	/**
	 * Query to find a data file by name and instrument ID
	 */
	private static final String FIND_FILE_QUERY = "SELECT id FROM data_file WHERE instrument_id = ? AND filename = ?";
	
	/**
	 * Statement to add a data file to the database
	 */
	private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file (instrument_id, filename, start_date, record_count, current_job, last_touched) "
		+ "VALUES (?, ?, ?, ?, ?, ?)";

	/**
	 * Query to find all the data files for a given user
	 */
	private static final String GET_USER_FILES_QUERY = "SELECT f.id, i.id, i.name, f.filename, f.start_date, f.record_count, f.current_job, f.last_touched FROM instrument AS i INNER JOIN data_file AS f ON i.id = f.instrument_id"
			+ " WHERE f.delete_flag = 0 AND i.owner = ? ORDER BY f.last_touched DESC";
	
	/**
	 * Statement to delete a file
	 */
	private static final String DELETE_FILE_STATEMENT = "DELETE FROM data_file WHERE id = ?";
	
	/**
	 * Query to find a file using its ID
	 */
	private static final String FIND_FILE_BY_ID_QUERY = "SELECT f.id, i.id, i.name, f.filename, f.start_date, f.record_count, f.current_job, f.last_touched FROM instrument AS i INNER JOIN data_file AS f ON i.id = f.instrument_id"
			+ " WHERE f.id = ?";
	
	private static final String GET_INSTRUMENT_ID_QUERY = "SELECT instrument_id FROM data_file WHERE id = ?";
	
	private static final String SET_JOB_STATEMENT = "UPDATE data_file SET current_job = ? WHERE id = ?";
	
	private static final String ATMOSPHERIC_MEAS_COUNT_QUERY = "SELECT COUNT(*) FROM raw_data WHERE data_file_id = ? AND co2_type = " + RunType.RUN_TYPE_ATMOSPHERIC;
	
	private static final String OCEAN_MEAS_COUNT_QUERY = "SELECT COUNT(*) FROM raw_data WHERE data_file_id = ? AND co2_type = " + RunType.RUN_TYPE_WATER;
	
	private static final String STANDARDS_COUNT_QUERY = "SELECT COUNT(*) FROM gas_standards_data WHERE data_file_id = ?";
	
	private static final String GET_QC_FLAGS_QUERY = "SELECT DISTINCT qc_flag, COUNT(qc_flag) FROM qc WHERE data_file_id = ? GROUP BY qc_flag";

	private static final String GET_WOCE_FLAGS_QUERY = "SELECT DISTINCT woce_flag, COUNT(woce_flag) FROM qc WHERE data_file_id = ? GROUP BY woce_flag";

	private static final String TOUCH_FILE_STATEMENT = "UPDATE data_file SET last_touched = NOW() WHERE id = ?";
	
	private static final String SET_DELETE_FLAG_STATEMENT = "UPDATE data_file SET delete_flag = ? WHERE id = ?";

	private static final String GET_DELETE_FLAG_STATEMENT = "SELECT delete_flag FROM data_file WHERE id = ?";

	/**
	 * Store a file in the database and in the file store
	 * @param dataSource A data source
	 * @param appConfig The application configuration
	 * @param instrumentID The instrument ID
	 * @param dataFile The data file
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws FileExistsException If the file already exists in the system
	 * @throws DatabaseException If an error occurs while storing the file
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
			
				List<String> jobParameters = new ArrayList<String>(1);
				jobParameters.add(String.valueOf(generatedKeys.getLong(1)));
				JobManager.addJob(conn, owner, FileInfo.JOB_CLASS_EXTRACT, jobParameters);
				
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
	 * Determinsed whether a file with the specified ID exists in the database
	 * @param dataSource A data source
	 * @param fileId The file ID
	 * @return {@code true} if the file exists; {@code false} if it does not
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an error occurs
	 * @throws RecordNotFoundException If the file disappears between locating it and reading its details
	 */
	public static boolean fileExists(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		return (null != getFileDetails(dataSource, fileId));
	}
	
	/**
	 * Determinsed whether a file with the specified ID exists in the database
	 * @param conn A database connection
	 * @param fileId The file ID
	 * @return {@code true} if the file exists; {@code false} if it does not
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an error occurs
	 * @throws RecordNotFoundException If the file disappears between locating it and reading its details
	 */
	public static boolean fileExists(Connection conn, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		return (null != getFileDetails(conn, fileId));
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
	 */
	public static List<FileInfo> getUserFiles(DataSource dataSource, User user) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<FileInfo> fileInfo = new ArrayList<FileInfo>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_USER_FILES_QUERY);
			stmt.setLong(1, user.getDatabaseID());
			
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
	 * @param fileDetails The details of the file to be deleted
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an error occurs during deletion
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
	 * @throws RecordNotFoundException 
	 * @throws IOException 
	 * @throws RawDataFileException 
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
	 * Create a FileInfo object from the current record in a ResultSet.
	 * This method expects the ResultSet to have come from the FIND_FILE_BY_ID query.
	 * @param record The ResutSet whose current record is to be read
	 * @return A FileInfo object for the record
	 * @throws SQLException If the record is not of the right format
	 * @throws RecordNotFoundException 
	 * @throws MissingParamException 
	 * @throws DatabaseException 
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

		
		FileInfo result = new FileInfo(fileID, instrumentId, instrumentName, fileName, startDate, recordCount, currentJob, lastTouched, atmosphericMeasurements, oceanMeasurements, standards);
		updateFlagCounts(conn, result);
		return result;
	}
	
	public static void updateFlagCounts(Connection conn, FileInfo fileInfo) throws DatabaseException {
		
		fileInfo.clearAllCounts();
		
		PreparedStatement qcFlagsStatement = null;
		PreparedStatement woceFlagsStatement = null;
		ResultSet qcFlags = null;
		ResultSet woceFlags = null;
		
		try {
			
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
}
