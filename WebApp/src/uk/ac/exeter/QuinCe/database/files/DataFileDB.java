package uk.ac.exeter.QuinCe.database.files;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.RawDataFile;
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
	private static final String ADD_FILE_STATEMENT = "INSERT INTO data_file (instrument_id, filename, last_touched) VALUES (?, ?, ?)";
	
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
			stmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
			
			stmt.execute();
			
			// Store the file
			FileStore.storeFile(appConfig, instrumentID, dataFile);
			
			conn.commit();
			
		} catch (Exception e) {
			DatabaseUtils.rollBack(conn);
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
}
