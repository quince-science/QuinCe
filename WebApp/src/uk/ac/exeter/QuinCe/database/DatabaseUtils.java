package uk.ac.exeter.QuinCe.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * A utility class providing useful methods for dealing with
 * database-related objects
 * 
 * @author Steve Jones
 */
public class DatabaseUtils {

	/**
	 * Indicates that this item does not have a database ID, implying
	 * that it is not (yet) stored in the database.
	 */
	public static final int NO_DATABASE_RECORD = -1;
	
	/**
	 * Close a set of {@link java.sql.ResultSet} objects, ignoring any errors
	 * @param results The ResultSets
	 */
	public static void closeResultSets(ResultSet... results) {
		for (ResultSet result : results) {
			if (null != result) {
				try {
					result.close();
				} catch(SQLException e) {
					// Do nothing
				}
			}
		}			
	}
	
	/**
	 * Close a set of {@link java.sql.PreparedStatement} objects, ignoring any errors
	 * @param statements The PreparedStatements
	 */
	public static void closeStatements(List<PreparedStatement> statements) {
		for (PreparedStatement stmt : statements) {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
	}
	
	/**
	 * Close a set of {@link java.sql.PreparedStatement} objects, ignoring any errors
	 * @param statements The statements
	 */
	public static void closeStatements(PreparedStatement... statements) {
		closeStatements(Arrays.asList(statements));
	}
	
	/**
	 * Close a database connection, ignoring any errors.
	 * All connections have their auto-commit flag set to true.
	 * @param conn The database connection
	 */
	public static void closeConnection(Connection conn) {
		if (null != conn) {
			try {
				if (!conn.getAutoCommit()) {
					conn.rollback();
				}
				conn.close();
			} catch (SQLException e) {
				// Do nothing
			}
		}
	}
	
	/**
	 * Roll back an open transaction
	 * @param conn The database connection
	 */
	public static void rollBack(Connection conn) {
		if (null != conn) {
			try {
				conn.rollback();
			} catch (SQLException e) {
				// DO nothing
			}
		}
	}
	
	/**
	 * Retrieve a date/time from the database. For the actual data, all times are recorded in UTC,
	 * so this method ensures that the retrieved {@link java.util.Calendar} object is in UTC.
	 * 
	 * @param records The results from which the date/time must be retrieved
	 * @param columnIndex The colum index of the date/time
	 * @return The date/time in UTC
	 * @throws SQLException If an error occurs while reading from the database record
	 * @see java.util.Calendar#getInstance(TimeZone, Locale)
	 */
	public static Calendar getUTCDateTime(ResultSet records, int columnIndex) throws SQLException {
		Calendar result = DateTimeUtils.getUTCCalendarInstance();
		result.setTimeInMillis(records.getTimestamp(columnIndex).getTime());
		return result;
	}
}
