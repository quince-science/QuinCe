package uk.ac.exeter.QuinCe.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Wrapper class for {@link PreparedStatement} that automatically batches calls
 * to it.
 * 
 * <p>
 * The size of the batch is limited to reduce memory usage. Once a preset number
 * of calls have been batched, they are executed automatically.
 * </p>
 * 
 * <p>
 * {@link Connection#setAutoCommit(boolean)} should be used for transaction
 * control as with a normal {@link PreparedStatement}.
 * </p>
 */
public class AutoBatchPreparedStatement implements AutoCloseable {

  /**
   * The maximum size of a batch before it will be executed.
   */
  private static final int BATCH_SIZE = 100000;

  /**
   * The wrapped {@link PreparedStatement}.
   */
  private PreparedStatement stmt;

  /**
   * The current batch size.
   */
  private int batch = 0;

  /**
   * Set up a wrapped {@link PreparedStatement}.
   * 
   * @param conn
   *          A database connection.
   * @param sql
   *          The statement SQL.
   * @throws SQLException
   */
  public AutoBatchPreparedStatement(Connection conn, String sql)
    throws SQLException {
    stmt = conn.prepareStatement(sql);
  }

  /**
   * Convenience dummy equivalent to {@link PreparedStatement#execute()} that
   * redirects to adding the statement to the batch.
   * 
   * @throws SQLException
   *           If the statement cannot be added to the batch.
   * @see #addBatch()
   */
  public void execute() throws SQLException {
    addBatch();
  }

  /**
   * Add the statement to the current batch, and execute the batch if the number
   * of commands has reached the maximum batch size.
   * 
   * @throws SQLException
   *           If the statement cannot be added to the batch.
   * 
   * @see PreparedStatement#addBatch()
   * @see PreparedStatement#executeBatch()
   */
  public void addBatch() throws SQLException {
    stmt.addBatch();
    batch += 1;
    if (batch >= BATCH_SIZE) {
      System.out.println("Batch");
      stmt.executeBatch();
      batch = 0;
    }
  }

  /**
   * Close the wrapped {@link PreparedStatement}, first executing any commands
   * in the batch queue.
   * 
   * @throws SQLException
   *           If a database error occurs.
   * 
   * @see PreparedStatement#executeBatch()
   * @see PreparedStatement#close()
   */
  public void close() throws SQLException {
    System.out.println("Batch and close");
    stmt.executeBatch();
    stmt.close();
  }

  /**
   * Wrapper for {@link PreparedStatement#setLong(int, long)}.
   * 
   * @param index
   *          The parameter index.
   * @param value
   *          The parameter value.
   * 
   * @throws SQLException
   *           If the parameter cannot be set.
   */
  public void setLong(int index, long value) throws SQLException {
    stmt.setLong(index, value);
  }

  /**
   * Wrapper for {@link PreparedStatement#setNull(int, int)}.
   * 
   * @param index
   *          The parameter index.
   * @param sqlType
   *          The SQL type code.
   * 
   * @throws SQLException
   *           If the parameter cannot be set.
   */
  public void setNull(int index, int sqlType) throws SQLException {
    stmt.setNull(index, sqlType);
  }

  /**
   * Wrapper for {@link PreparedStatement#setLong(int, int)}.
   * 
   * @param index
   *          The parameter index.
   * @param value
   *          The parameter value.
   * 
   * @throws SQLException
   *           If the parameter cannot be set.
   */
  public void setInt(int index, int value) throws SQLException {
    stmt.setInt(index, value);
  }

  /**
   * Wrapper for {@link PreparedStatement#setLong(int, String)}.
   * 
   * @param index
   *          The parameter index.
   * @param value
   *          The parameter value.
   * 
   * @throws SQLException
   *           If the parameter cannot be set.
   */
  public void setString(int index, String value) throws SQLException {
    stmt.setString(index, value);
  }
}
