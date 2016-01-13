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

import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.data.RawDataFileException;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
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
	private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file (instrument_id, filename, start_date, record_count, current_job, job_status, last_touched) "
		+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

	/**
	 * Query to find all the data files for a given user
	 */
	private static final String GET_USER_FILES_QUERY = "SELECT f.id, i.id, i.name, f.filename, f.start_date, f.record_count, f.current_job, f.job_status, f.last_touched FROM instrument AS i INNER JOIN data_file AS f ON i.id = f.instrument_id"
			+ " WHERE i.owner = ? ORDER BY f.last_touched ASC";
	
	/**
	 * Statement to delete a file
	 */
	private static final String DELETE_FILE_STATEMENT = "DELETE FROM data_file WHERE id = ?";
	
	/**
	 * Query to find a file using its ID
	 */
	private static final String FIND_FILE_BY_ID_QUERY = "SELECT f.id, i.id, i.name, f.filename, f.start_date, f.record_count, f.current_job, f.job_status, f.last_touched FROM instrument AS i INNER JOIN data_file AS f ON i.id = f.instrument_id"
			+ " WHERE f.id = ?";
	
	/**
	 * Statement to update a file's job status
	 */
	private static final String SET_JOB_STATUS_STATEMENT = "UPDATE data_file SET job_status = ? WHERE id = ?";
	
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
			stmt.setInt(6, FileInfo.STATUS_CODE_WAITING);
			stmt.setDate(7, new java.sql.Date(System.currentTimeMillis()));
			
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
	 */
	public static boolean fileExists(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException {
		return (null != getFileDetails(dataSource, fileId));
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
				fileInfo.add(makeFileInfo(records));
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
	
	public static FileInfo getFileDetails(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException {
		
		FileInfo result = null;
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
				
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet record = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(FIND_FILE_BY_ID_QUERY);
			stmt.setLong(1, fileId);
			record = stmt.executeQuery();
			if (record.next()) {
				result = makeFileInfo(record);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while searching for the file", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
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
	 * Sets the status of the current job. This can be 0 to 100 to represent
	 * the progress of a running job, or -1 or -2 to represent waiting or errored
	 * jobs respectively.
	 * 
	 * @param conn A database connection
	 * @param fileId The database ID of the file to be updated
	 * @param status The status
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an error occurs
	 */
	public static void setJobStatus(Connection conn, long fileId, int status) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(SET_JOB_STATUS_STATEMENT);
			stmt.setInt(1, status);
			stmt.setLong(2, fileId);
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while updating the file's job progress", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
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
	public static RawDataFile getRawDataFile(DataSource dataSource, Properties appConfig, long fileId) throws MissingParamException, DatabaseException {
		
		try {
			FileInfo fileInfo = getFileDetails(dataSource, fileId);
			return FileStore.getFile(dataSource, appConfig, fileInfo);
		} catch (RecordNotFoundException|IOException e) {
			throw new DatabaseException("An error occurred while retrieving the file from the file store", e);
		}
	}
	
	/**
	 * Create a FileInfo object from the current record in a ResultSet.
	 * This method expects the ResultSet to have come from the FIND_FILE_BY_ID query.
	 * @param record The ResutSet whose current record is to be read
	 * @return A FileInfo object for the record
	 * @throws SQLException If the record is not of the right format
	 */
	private static FileInfo makeFileInfo(ResultSet record) throws SQLException {
		long fileID = record.getLong(1);
		long instrumentId = record.getLong(2);
		String instrumentName = record.getString(3);
		String fileName = record.getString(4);
		Calendar startDate = Calendar.getInstance();
		startDate.setTime(record.getDate(5));
		int recordCount = record.getInt(6);
		int currentJob = record.getInt(7);
		int jobStatus = record.getInt(8);
		Calendar lastTouched = Calendar.getInstance();
		lastTouched.setTime(record.getDate(9));
		
		return new FileInfo(fileID, instrumentId, instrumentName, fileName, startDate, recordCount, currentJob, jobStatus, lastTouched);
	}
}
