package uk.ac.exeter.QuinCe.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class providing useful methods for dealing with database-related
 * objects
 */
public class DatabaseUtils {

  /**
   * Indicates that this item does not have a database ID, implying that it is
   * not (yet) stored in the database.
   */
  public static final int NO_DATABASE_RECORD = -1;

  /**
   * Token to mark the place where the parameters for an IN clause should be in
   * an SQL string.
   *
   * @see #makeInStatementSql(String, int...)
   */
  public static final String IN_PARAMS_TOKEN = "%%IN_PARAMS%%";

  /**
   * Close a set of {@link java.sql.ResultSet} objects, ignoring any errors
   *
   * @param results
   *          The ResultSets
   */
  public static void closeResultSets(List<ResultSet> results) {
    for (ResultSet result : results) {
      if (null != result) {
        try {
          if (!result.isClosed()) {
            result.close();
          }
        } catch (SQLException e) {
          // Do nothing
        }
      }
    }
  }

  /**
   * Close a set of {@link java.sql.ResultSet} objects, ignoring any errors
   *
   * @param results
   *          The ResultSets
   */
  public static void closeResultSets(ResultSet... results) {
    closeResultSets(Arrays.asList(results));
  }

  /**
   * Close a set of {@link java.sql.PreparedStatement} objects, ignoring any
   * errors
   *
   * @param statements
   *          The PreparedStatements
   */
  public static void closeStatements(List<PreparedStatement> statements) {
    for (PreparedStatement stmt : statements) {
      if (null != stmt) {
        try {
          if (!stmt.isClosed()) {
            stmt.close();
          }
        } catch (SQLException e) {
          // Do nothing
        }
      }
    }
  }

  /**
   * Close a set of {@link java.sql.PreparedStatement} objects, ignoring any
   * errors
   *
   * @param statements
   *          The statements
   */
  public static void closeStatements(PreparedStatement... statements) {
    closeStatements(Arrays.asList(statements));
  }

  /**
   * Close a set of database connections, rolling back any transactions and
   * ignoring any errors. All connections have their auto-commit flag set to
   * true.
   *
   * @param conns
   *          The database connections.
   */
  public static void closeConnection(Connection... conns) {
    for (Connection conn : conns) {
      if (null != conn) {
        try {
          if (!conn.getAutoCommit()) {
            conn.rollback();
            conn.setAutoCommit(true);
          }
        } catch (SQLException e) {
          // Do nothing
        } finally {
          try {
            conn.close();
          } catch (SQLException e) {
            // NOOP
          }
        }
      }
    }
  }

  /**
   * Roll back an open transaction
   *
   * @param conn
   *          The database connection
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
   * Construct an SQL Prepared Statement string that contains one or more IN
   * parameters with the given number of parameters.
   *
   * The query must contain at least one {@link #IN_PARAMS_TOKEN}.
   *
   * @param query
   *          The query
   * @param inSize
   *          The number of items in the IN parameter(s)
   * @return The generated SQL statement
   * @throws MissingParamException
   *           If any required parameters are missing.
   */
  public static String makeInStatementSql(String query, int... inSize)
    throws MissingParamException {

    MissingParam.checkMissing(query, "query");
    MissingParam.checkMissing(inSize, "inSize", false);

    StringBuilder sql = new StringBuilder();

    int currentPos = 0;
    for (int i = 0; i < inSize.length; i++) {
      int tokenPos = query.indexOf(IN_PARAMS_TOKEN, currentPos);
      if (tokenPos == -1) {
        throw new MissingParamException(
          "Mismatch between parameter tokens and parameter size list");
      }

      sql.append(query.substring(currentPos, tokenPos));
      currentPos = tokenPos;

      sql.append('(');
      for (int j = 0; j < inSize[i]; j++) {
        sql.append("?");
        if (j < inSize[i] - 1) {
          sql.append(',');
        }
      }
      sql.append(')');

      currentPos += IN_PARAMS_TOKEN.length();
    }

    if (query.indexOf(IN_PARAMS_TOKEN, currentPos) > -1) {
      throw new MissingParamException(
        "Mismatch between parameter tokens and parameter size list");
    }

    sql.append(query.substring(currentPos));

    return sql.toString();
  }

  /**
   * Get the database field name for a human-readable data field name
   *
   * <p>
   * The database field name is the full name converted to lower case and with
   * spaces replaced by underscores. Brackets and other odd characters that
   * upset MySQL are removed.
   * </p>
   *
   * @param fullName
   *          The human-readable name.
   * @return The database field name.
   */
  public static String getDatabaseFieldName(String fullName) {
    String result = null;
    if (null != fullName) {
      result = fullName.replaceAll(" ", "_").replaceAll("[,\\(\\)]", "")
        .toLowerCase();
    }

    return result;
  }

  /**
   * Get a long value from a recordset, handling null values as null
   *
   * @param rs
   *          The recordset
   * @param column
   *          The column index
   * @return The column value
   * @throws SQLException
   *           If the value cannot be retrieved
   */
  public static Long getNullableLong(ResultSet rs, int column)
    throws SQLException {
    Long result = null;

    long value = rs.getInt(column);
    if (!rs.wasNull()) {
      result = value;
    }

    return result;
  }
}
