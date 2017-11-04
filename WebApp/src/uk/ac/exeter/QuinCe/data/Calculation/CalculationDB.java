package uk.ac.exeter.QuinCe.data.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

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
			fields.add("auto_qc_flag");
			fields.add("auto_qc_message");
			fields.add("user_qc_flag");
			fields.add("user_qc_comment");

			insertStatement = DatabaseUtils.createInsertStatement(conn, getCalculationTable(), fields);
		}
		
		return insertStatement;

	}
}
