package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Methods for manipulating data sets in the database
 * @author Steve Jones
 *
 */
public class DataSetDB {

	/**
	 * Query to get the defined data sets for a given instrument
	 */
	private static final String GET_DATASETS_QUERY = "SELECT "
			+ "id, instrument_id, name, start, end, status, properties, last_touched "
		    + "FROM dataset WHERE instrument_id = ? ORDER BY start ASC";
	
	/**
	 * Statement to add a new data set into the database
	 */
	private static final String ADD_DATASET_STATEMENT = "INSERT INTO dataset "
			+ "(instrument_id, name, start, end, status, properties, last_touched) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Get the list of data sets defined for a given instrument
	 * @param dataSource A data source
	 * @param instrumentId The instrument's database ID
	 * @return The list of data sets
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 */
	public static List<DataSet> getDataSets(DataSource dataSource, long instrumentId) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkZeroPositive(instrumentId, "instrumentId");
		
		List<DataSet> result = new ArrayList<DataSet>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_DATASETS_QUERY);
			stmt.setLong(1, instrumentId);
			
			records = stmt.executeQuery();
			
			while (records.next()) {
				result.add(dataSetFromRecord(records));
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving data sets", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}
	
	/**
	 * Create a DataSet object from a search result
	 * @param record The search result
	 * @return The Data Set object
	 * @throws SQLException If the data cannot be extracted from the result
	 */
	private static DataSet dataSetFromRecord(ResultSet record) throws SQLException {
		
		long id = record.getLong(1);
		long instrumentId = record.getLong(2);
		String name = record.getString(3);
		LocalDateTime start = DateTimeUtils.longToDate(record.getLong(4));
		LocalDateTime end = DateTimeUtils.longToDate(record.getLong(5));
		int status = record.getInt(6);
		Properties properties = null;
		LocalDateTime lastTouched = DateTimeUtils.longToDate(record.getLong(8));
		
		return new DataSet(id, instrumentId, name, start, end, status, properties, lastTouched);
	}
	
	public static void addDataSet(DataSource dataSource, DataSet dataSet) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet addedKeys = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(ADD_DATASET_STATEMENT, PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setLong(1, dataSet.getInstrumentId());
			stmt.setString(2, dataSet.getName());
			stmt.setLong(3, DateTimeUtils.dateToLong(dataSet.getStart()));
			stmt.setLong(4, DateTimeUtils.dateToLong(dataSet.getEnd()));
			stmt.setLong(5, dataSet.getStatus());
			stmt.setNull(6, Types.VARCHAR);
			stmt.setLong(7, DateTimeUtils.dateToLong(LocalDateTime.now()));
			
			stmt.execute();
			
			// Add the database ID to the database object
			addedKeys = stmt.getGeneratedKeys();
			addedKeys.next();
			dataSet.setId(addedKeys.getLong(1));
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while adding data set", e);
		} finally {
			DatabaseUtils.closeResultSets(addedKeys);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}
}
