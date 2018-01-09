package uk.ac.exeter.QuinCe.data.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.data.NoSuchColumnException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Class for dealing with database calls related to calculation data
 * @author Steve Jones
 *
 */
public abstract class CalculationDB {

	/**
	 * The statement for inserting a new calculation record.
	 * Created as required by {@link #createCalculationRecord(Connection, long)}.
	 */
	private PreparedStatement insertStatement = null;
	
	/**
	 * Get the name of the database table where calculation data is stored
	 * @return The table name
	 */
	public abstract String getCalculationTable();
	
	/**
	 * Create a calculation record for the given measurement record
	 * @param conn A database connection
	 * @param measurementId The measurement's ID
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 */
	public void createCalculationRecord(Connection conn, long measurementId) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(measurementId, "measurementId");
		
		try {
			PreparedStatement statement = getInsertStatement(conn);
			
			statement.setLong(1, measurementId);
			statement.setInt(2, Flag.VALUE_NOT_SET);
			statement.setNull(3, Types.VARCHAR);
			statement.setInt(4, Flag.VALUE_NOT_SET);
			statement.setNull(5, Types.VARCHAR);
			
			statement.execute();
		
		} catch (SQLException e) {
			throw new DatabaseException("Error while creating calculation record", e);
		}
	}
	
	/**
	 * Generate the insert statement for a new calculation record
	 * @param conn A database connection
	 * @return The insert statement
	 * @throws MissingParamException If any required parameters are missing
	 * @throws SQLException If the statement cannot be created
	 */
	private PreparedStatement getInsertStatement(Connection conn) throws MissingParamException, SQLException {
		
		if (null == insertStatement) {
			List<String> fields = new ArrayList<String>();
			
			fields.add("measurement_id");
			fields.add("auto_flag");
			fields.add("auto_message");
			fields.add("user_flag");
			fields.add("user_message");

			insertStatement = DatabaseUtils.createInsertStatement(conn, getCalculationTable(), fields);
		}
		
		return insertStatement;
	}
	
	/**
	 * Delete the calculation data for a given data set
	 * @param conn A database connection
	 * @param dataSet The data set
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public void deleteDatasetCalculationData(Connection conn, DataSet dataSet) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(dataSet, "dataSet");
		
		PreparedStatement stmt = null;
		
		try {
			// TODO I think this could be done better. But maybe not.
			String deleteStatement = "DELETE c.* FROM " + getCalculationTable() + " AS c INNER JOIN dataset_data AS d ON c.measurement_id = d.id WHERE d.dataset_id = ?";

			stmt = conn.prepareStatement(deleteStatement);
			stmt.setLong(1, dataSet.getId());
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Error while deleting dataset data", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	/**
	 * Get the Automatic QC flag for a measurement
	 * @param conn A database connection
	 * @param measurementId The measurement ID
	 * @return The automatic QC flag
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the measurement does not exist
	 * @throws InvalidFlagException The the flag value is invalid
	 */
	public Flag getAutoQCFlag(Connection conn, long measurementId) throws MissingParamException, DatabaseException, RecordNotFoundException, InvalidFlagException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(measurementId, "measurementId");
	
		Flag result = null;
		PreparedStatement stmt = null;
		ResultSet record = null;
		
		try {
			// TODO I think this could be done better. But maybe not.
			String flagStatement = "SELECT auto_flag FROM " + getCalculationTable() + " WHERE measurement_id = ?";
			
			stmt = conn.prepareStatement(flagStatement);
			stmt.setLong(1, measurementId);
			
			record = stmt.executeQuery();
			if (!record.next()) {
				throw new RecordNotFoundException("Cannot find calculation record", getCalculationTable(), measurementId);
			} else {
				result = new Flag(record.getInt(1));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving QC flag", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return result;
	}
	
	/**
	 * Get the automatic QC messages for a given measurement
	 * @param conn A datbase connection
	 * @param measurementId The measurement ID
	 * @return The QC messages
	 * @throws MessageException If the messages cannot be parsed
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the measurement cannot be found
	 */
	public List<Message> getQCMessages(Connection conn, long measurementId) throws MessageException, DatabaseException, RecordNotFoundException, MissingParamException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(measurementId, "measurementId");
		
		PreparedStatement stmt = null;
		ResultSet record = null;
		List<Message> result = null;
		
		try {
			// TODO I think this could be done better. But maybe not.
			String query = "SELECT auto_message FROM " + getCalculationTable() + " WHERE measurement_id = ?";
			
			stmt = conn.prepareStatement(query);
			
			stmt.setLong(1, measurementId);
			record = stmt.executeQuery();
			
			if (!record.next()) {
				throw new RecordNotFoundException("Cannot find calculation record", getCalculationTable(), measurementId);
			} else {
				result = RebuildCode.getMessagesFromRebuildCodes(record.getString(1));
			}
			
			return result;
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while retrieving QC messages", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
	}

	/**
	 * Store the QC information for a given record
	 * @param conn A database connection
	 * @param record The record
	 * @throws MessageException If the messages cannot be serialized for storage
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public void storeQC(Connection conn, CalculationRecord record) throws MissingParamException, DatabaseException, MessageException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(record, "record");
		
		PreparedStatement stmt = null;
		
		try {
			// TODO I think this could be done better. But maybe not.
			String sql = "UPDATE " + getCalculationTable() + " SET auto_flag = ?, "
					+ "auto_message = ?, user_flag = ?, user_message = ? "
					+ "WHERE measurement_id = ?";
			
			stmt = conn.prepareStatement(sql);
			
			stmt.setInt(1, record.getAutoFlag().getFlagValue());
			String rebuildCodes = RebuildCode.getRebuildCodes(record.getAutoQCMessages());
			if (null == rebuildCodes || rebuildCodes.length() == 0) {
				stmt.setNull(2, Types.VARCHAR);
			} else {
				stmt.setString(2, RebuildCode.getRebuildCodes(record.getAutoQCMessages()));
			}
			
			stmt.setInt(3, record.getUserFlag().getFlagValue());
			String userMessage = record.getUserMessage();
			if (null == userMessage || userMessage.length() == 0) {
				stmt.setNull(4, Types.VARCHAR);
			} else {
				stmt.setString(4, record.getUserMessage());
			}
			stmt.setLong(5, record.getLineNumber());
			
			stmt.execute();
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while storing QC info", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}

	/**
	 * Store the calculation values for a given measurement. This method
	 * must only update an existing record in the database.
	 * @param conn A database connection
	 * @param measurementId The measurement's database ID
	 * @param values The values to be stored
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public abstract void storeCalculationValues(Connection conn, long measurementId, Map<String, Double> values) throws MissingParamException, DatabaseException;
	
	/**
	 * Add the calculation values to a {@link CalculationRecord}
	 * @param dataSource A data source
	 * @param record The record for which values should be retrieved
	 * @return The calculation values
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the record does not exist
	 * @throws MessageException If the automatic QC messages cannot be parsed
	 * @throws NoSuchColumnException If the automatic QC messages cannot be parsed 
	 */
	public Map<String, Double> getCalculationValues(DataSource dataSource, CalculationRecord record) throws MissingParamException, DatabaseException, RecordNotFoundException, NoSuchColumnException, MessageException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(record, "record");
		
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			return getCalculationValues(conn, record);
		} catch (SQLException e) {
			throw new DatabaseException("Error while getting calculation values", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Add the calculation values to a {@link CalculationRecord}
	 * @param conn A database connection
	 * @param record The record for which values should be retrieved
	 * @return The calculation values
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the record does not exist
	 * @throws MessageException If the automatic QC messages cannot be parsed
	 * @throws NoSuchColumnException If the automatic QC messages cannot be parsed 
	 */
	public abstract Map<String, Double> getCalculationValues(Connection conn, CalculationRecord record) throws MissingParamException, DatabaseException, RecordNotFoundException, NoSuchColumnException, MessageException;
	
	/**
	 * Clear the calculation values for a given measurement. This method
	 * must only update an existing record in the database.
	 * @param conn A database connection
	 * @param measurementId The measurement's database ID
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public abstract void clearCalculationValues(Connection conn, long measurementId) throws MissingParamException, DatabaseException;
	
	/**
	 * Get the list of column headings for calculation fields
	 * @return The column headings
	 */
	public abstract List<String> getCalculationColumnHeadings();
	
	/**
	 * Get the list of measurement IDs for a dataset that can be manipulated by the user.
	 * This is basically all the IDs that have not been flagged as FATAL or INGNORED. 
	 * @param dataSource A data source
	 * @param datasetId The dataset ID
	 * @return The selectable measurement IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
   	 */
	public List<Long> getSelectableMeasurementIds(DataSource dataSource, long datasetId) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkZeroPositive(datasetId, "datasetId");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		List<Long> ids = new ArrayList<Long>();
		
		try {
			conn = dataSource.getConnection();
			
			String sql = "SELECT c.measurement_id FROM "
				+ getCalculationTable()
				+ " c INNER JOIN dataset_data d ON c.measurement_id = d.id "
				+ " WHERE d.dataset_id = ? AND c.user_flag NOT IN ("
				+ Flag.VALUE_FATAL
				+ ","
				+ Flag.VALUE_IGNORED
				+ ") ORDER BY c.measurement_id ASC";
			
			stmt = conn.prepareStatement(sql);
			stmt.setLong(1, datasetId);
			
			records = stmt.executeQuery();
			
			while (records.next()) {
				ids.add(records.getLong(1));
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while getting measurement IDs", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return ids;
	}
	
	/**
	 * Accept the automatic QC flag as the final QC result for a set of rows
	 * @param dataSource A data source
	 * @param rows The rows' database IDs
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
  	 * @throws MessageException If the automatic QC messages cannot be extracted
	 */
	public void acceptAutoQc(DataSource dataSource, List<Long> rows) throws MissingParamException, DatabaseException, MessageException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(rows, "rows", true);
		
		Connection conn = null;
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>(rows.size() + 1);
		ResultSet records = null;

		String readSql = "SELECT measurement_id, auto_flag, auto_message FROM "
				+ getCalculationTable()
				+ " WHERE measurement_id IN ("
				+ StringUtils.listToDelimited(rows)
				+ ")";
		
		String writeSql = "UPDATE "
				+ getCalculationTable()
				+ " SET user_flag = ?, user_message = ?"
				+ " WHERE measurement_id = ?";
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			PreparedStatement readStatement = conn.prepareStatement(readSql);
			statements.add(readStatement);

			records = readStatement.executeQuery();
			
			while (records.next()) {
				
				long id = records.getLong(1);
				int flag = records.getInt(2);
				List<Message> messages = RebuildCode.getMessagesFromRebuildCodes(records.getString(3));
				
				StringBuilder outputMessages = new StringBuilder();
				for (int i = 0; i < messages.size(); i++) {
					outputMessages.append(messages.get(i).getShortMessage());
					if (i < messages.size() - 1) {
						outputMessages.append(';');
					}
				}
				
				PreparedStatement writeStatement = conn.prepareStatement(writeSql);
				statements.add(writeStatement);
				
				writeStatement.setInt(1, flag);
				writeStatement.setString(2, outputMessages.toString());
				writeStatement.setLong(3, id);
				
				writeStatement.execute();
			}

			conn.commit();
			
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while accepting auto QC", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(statements);
			DatabaseUtils.closeConnection(conn);
		}
	}
}
