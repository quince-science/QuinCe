package uk.ac.exeter.QuinCe.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Miscellaneous database utilities
 * @author Steve Jones
 *
 */
public class DatabaseUtils {

	/**
	 * Indicates that this item does not have a database ID, implying
	 * that it is not (yet) stored in the database.
	 */
	public static final int NO_DATABASE_RECORD = -1;
	
	/**
	 * Close a set of ResultSet objects, ignoring any errors
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
	 * Close a set of PreparedStatement objets, ignoring any errors
	 * @param statements The statements
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
	 * Close a set of PreparedStatement objets, ignoring any errors
	 * @param statements The statements
	 */
	public static void closeStatements(PreparedStatement... statements) {
		closeStatements(Arrays.asList(statements));
	}
	
	/**
	 * Close a database connection, ignoring any errors.
	 * Any uncommitted changes are rolled back.
	 * @param conn The connection
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
	
	public static Calendar getUTCDateTime(ResultSet records, int columnIndex) throws SQLException {
		Calendar result = DateTimeUtils.getUTCCalendarInstance();
		result.setTimeInMillis(records.getTimestamp(columnIndex).getTime());
		return result;
	}
}
