package uk.ac.exeter.QuinCe.database.files;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
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
	private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file (instrument_id, filename, start_date, record_count, last_touched) VALUES (?, ?, ?, ? , ?)";

	/**
	 * Query to find all the data files for a given user
	 */
	private static final String GET_USER_FILES_QUERY = "SELECT f.id, i.id, i.name, f.filename, f.start_date, f.record_count, f.current_job, f.job_status, f.last_touched FROM instrument AS i INNER JOIN data_file AS f ON i.id = f.instrument_id"
			+ " WHERE i.owner = ? ORDER BY f.last_touched ASC";
	
	/**
	 * Statement to delete a file
	 */
	private static final String DELETE_FILE_STATEMENT = "DELETE from data_file WHERE id = ?";
	
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
	public static void storeFile(DataSource dataSource, Properties appConfig, long instrumentID, RawDataFile dataFile) throws MissingParamException, FileExistsException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(appConfig, "appConfig");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(dataFile, "file");
		
		if (fileExists(dataSource, instrumentID, dataFile.getFileName())) {
			throw new FileExistsException(dataSource, instrumentID, dataFile.getFileName());
		}
		
		Connection conn = null;
		PreparedStatement stmt = null;
		
		
		try {
			
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			stmt = conn.prepareStatement(ADD_FILE_STATEMENT);
			stmt.setLong(1, instrumentID);
			stmt.setString(2, dataFile.getFileName());
			stmt.setDate(3, new java.sql.Date(dataFile.getStartDate().getTimeInMillis()));
			stmt.setInt(4, dataFile.getRecordCount());
			stmt.setDate(5, new java.sql.Date(System.currentTimeMillis()));
			
			stmt.execute();
			
			// Store the file
			FileStore.storeFile(appConfig, instrumentID, dataFile);
			
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
				long fileID = records.getLong(1);
				long instrumentId = records.getLong(2);
				String instrumentName = records.getString(3);
				String fileName = records.getString(4);
				Calendar startDate = Calendar.getInstance();
				startDate.setTime(records.getDate(5));
				int recordCount = records.getInt(6);
				int currentJob = records.getInt(7);
				int jobStatus = records.getInt(8);
				Calendar lastTouched = Calendar.getInstance();
				lastTouched.setTime(records.getDate(9));
				
				fileInfo.add(new FileInfo(fileID, instrumentId, instrumentName, fileName, startDate, recordCount, currentJob, jobStatus, lastTouched));
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
}
