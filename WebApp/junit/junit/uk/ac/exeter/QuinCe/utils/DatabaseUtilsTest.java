package junit.uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Tests for the {@link DatabaseUtils} class.
 *
 * @author stevej
 *
 */
public class DatabaseUtilsTest extends BaseTest {

  /**
   * A simple SQL query we can use for the tests
   */
  private static final String BASIC_QUERY = "SELECT * FROM sensor_types";

  /**
   * Create a test table to store {@code long} values.
   *
   * @param conn
   *          A database connection.
   * @throws SQLException
   *           If the table cannot be created.
   */
  private void createLongTable(Connection conn) throws SQLException {
    try (PreparedStatement createStmt = conn
      .prepareStatement("CREATE TABLE test_longs (long  INT(11))");) {
      createStmt.execute();
    }
  }

  /**
   * Add a value to the test Longs table.
   *
   * <p>
   * {@link #createLongTable(Connection)} must be called before this method.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param value
   *          The value to add.
   * @throws SQLException
   *           If the record cannot be created.
   * @see #createLongTable(Connection)
   */
  private void addLongRecord(Connection conn, Long value) throws SQLException {
    try (PreparedStatement insertStmt = conn
      .prepareStatement("INSERT INTO test_longs VALUES (?)");) {

      if (null == value) {
        insertStmt.setNull(1, Types.INTEGER);
      } else {
        insertStmt.setLong(1, value);
      }
      insertStmt.execute();
    }
  }

  /**
   * Retrieve a Long value from the test Longs table using
   * {@link DatabaseUtils#getNullableLong(ResultSet, int)}.
   *
   * <p>
   * The method assumes there is one record in the table, and returns the first
   * value from a simple {@code SELECT *} statement.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @return The Long value of the first record.
   * @throws SQLException
   *           If no record can be retrieved.
   * @see #addLongRecord(Connection, Long)
   */
  private Long getLongRecord(Connection conn) throws SQLException {
    try (
      PreparedStatement stmt = conn
        .prepareStatement("SELECT * FROM test_longs");
      ResultSet records = stmt.executeQuery()) {

      records.next();
      return DatabaseUtils.getNullableLong(records, 1);
    }
  }

  /**
   * Test that closing an empty list of {@link ResultSet}s runs without error.
   */
  @Test
  public void closeResultSetsEmptyListTest() {
    DatabaseUtils.closeResultSets(new ArrayList<ResultSet>());
  }

  /**
   * Test that sending a list containing a {@code null} value to
   * {@link DatabaseUtils#closeResultSets(List)} does not throw an error.
   */
  @Test
  public void closeResultSetsNullListTest() {
    List<ResultSet> nullList = new ArrayList<ResultSet>();
    nullList.add(null);
    DatabaseUtils.closeResultSets(nullList);
  }

  /**
   * Test that a list of {@link ResultSet}s with one entry can be closed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeResultSetsOneListTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();
      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);) {

      ResultSet records = stmt.executeQuery();
      assertFalse(records.isClosed());

      List<ResultSet> setsToClose = new ArrayList<ResultSet>();
      setsToClose.add(records);
      DatabaseUtils.closeResultSets(setsToClose);

      assertTrue(records.isClosed());
    }
  }

  /**
   * Test that a list of {@link ResultSet}s with two entries can be closes both
   * entries.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeResultSetsTwoListTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();
      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);
      PreparedStatement stmt2 = conn.prepareStatement(BASIC_QUERY);) {

      ResultSet records = stmt.executeQuery();
      assertFalse(records.isClosed());

      ResultSet records2 = stmt2.executeQuery();
      assertFalse(records.isClosed());

      List<ResultSet> setsToClose = new ArrayList<ResultSet>();
      setsToClose.add(records);
      setsToClose.add(records2);
      DatabaseUtils.closeResultSets(setsToClose);

      assertTrue(records.isClosed());
      assertTrue(records2.isClosed());
    }
  }

  /**
   * Test that closing a single {@link ResultSet} via a parameter call works.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeResultSetsOneParamTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();
      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);) {

      ResultSet records = stmt.executeQuery();
      assertFalse(records.isClosed());

      DatabaseUtils.closeResultSets(records);

      assertTrue(records.isClosed());
    }
  }

  /**
   * Test that closing multiple {@link ResultSet}s via a parameter call works.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeResultSetsTwoParamsTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();
      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);
      PreparedStatement stmt2 = conn.prepareStatement(BASIC_QUERY);) {

      ResultSet records = stmt.executeQuery();
      assertFalse(records.isClosed());

      ResultSet records2 = stmt2.executeQuery();
      assertFalse(records.isClosed());

      DatabaseUtils.closeResultSets(records, records2);

      assertTrue(records.isClosed());
      assertTrue(records2.isClosed());
    }
  }

  /**
   * Test that closing a {@link ResultSet} that is already closed does not throw
   * an error.
   *
   * <p>
   * We only need to test the parameter version of the method, since it calls
   * the list version anyway.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeResultSetsClosedSetTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();
      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);) {

      ResultSet records = stmt.executeQuery();
      records.close();

      DatabaseUtils.closeResultSets(records);
    }
  }

  /**
   * Test that closing an empty list of {@link PreparedStatement}s runs without
   * error.
   */
  @Test
  public void closeStatementsEmptyListTest() {
    DatabaseUtils.closeStatements(new ArrayList<PreparedStatement>());
  }

  /**
   * Test that sending a list containing a {@code null} value to
   * {@link DatabaseUtils#closeStatements(List)} does not throw an error.
   */
  @Test
  public void closeStatementsNullListTest() {
    List<PreparedStatement> nullList = new ArrayList<PreparedStatement>();
    nullList.add(null);
    DatabaseUtils.closeStatements(nullList);
  }

  /**
   * Test that a list of {@link PreparedStatement}s with one entry can be
   * closed.
   *
   * @throws Exception
   *           If any internal errors are encountered. s
   */
  @FlywayTest
  @Test
  public void closeStatementsOneListTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);
      assertFalse(stmt.isClosed());

      List<PreparedStatement> stmtsToClose = new ArrayList<PreparedStatement>();
      stmtsToClose.add(stmt);
      DatabaseUtils.closeStatements(stmtsToClose);

      assertTrue(stmt.isClosed());
    }
  }

  /**
   * Test that a list of {@link PreparedStatement}s with two entries can be
   * closes both entries.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeStatementsTwoListTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);
      assertFalse(stmt.isClosed());

      PreparedStatement stmt2 = conn.prepareStatement(BASIC_QUERY);
      assertFalse(stmt.isClosed());

      List<PreparedStatement> stmtsToClose = new ArrayList<PreparedStatement>();
      stmtsToClose.add(stmt);
      stmtsToClose.add(stmt2);
      DatabaseUtils.closeStatements(stmtsToClose);

      assertTrue(stmt.isClosed());
      assertTrue(stmt2.isClosed());
    }
  }

  /**
   * Test that closing a single {@link PreparedStatement} via a parameter call
   * works.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeStatementsOneParamTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);
      assertFalse(stmt.isClosed());

      DatabaseUtils.closeStatements(stmt);

      assertTrue(stmt.isClosed());
    }
  }

  /**
   * Test that closing multiple {@link PreparedStatement}s via a parameter call
   * works.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeStatementsTwoParamsTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);
      assertFalse(stmt.isClosed());

      PreparedStatement stmt2 = conn.prepareStatement(BASIC_QUERY);
      assertFalse(stmt2.isClosed());

      DatabaseUtils.closeStatements(stmt, stmt2);

      assertTrue(stmt.isClosed());
      assertTrue(stmt2.isClosed());
    }
  }

  /**
   * Test that closing a {@link PreparedStatement} that is already closed does
   * not throw an error.
   *
   * <p>
   * We only need to test the parameter version of the method, since it calls
   * the list version anyway.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeSatementsClosedSetTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      PreparedStatement stmt = conn.prepareStatement(BASIC_QUERY);
      stmt.close();

      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Test that {@link DatabaseUtils#getNullableLong(ResultSet, int)} correctly
   * retrieves a normal Long value.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getNullableLongNumberTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      createLongTable(conn);
      addLongRecord(conn, Long.valueOf(23L));

      assertEquals(Long.valueOf(23L), getLongRecord(conn));
    }
  }

  /**
   * Test that {@link DatabaseUtils#getNullableLong(ResultSet, int)} correctly
   * retrieves a zero Long value.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getNullableLongZeroTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      createLongTable(conn);
      addLongRecord(conn, Long.valueOf(0L));

      assertEquals(Long.valueOf(0L), getLongRecord(conn));
    }
  }

  /**
   * Test that {@link DatabaseUtils#getNullableLong(ResultSet, int)} correctly
   * retrieves a zero Long value.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void getNullableLongNullTest() throws Exception {

    try (Connection conn = getDataSource().getConnection();) {

      createLongTable(conn);
      addLongRecord(conn, null);

      assertNull(getLongRecord(conn));
    }
  }

  /**
   * Test that rolling back a connection works.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void rollbackTest() throws Exception {

    // Execute a statement to empty the sensor_types table, but roll back the
    // transaction.
    try (Connection conn = getDataSource().getConnection()) {

      conn.setAutoCommit(false);

      try (PreparedStatement stmt = conn
        .prepareStatement("DELETE FROM variable_sensors")) {
        stmt.execute();
      }

      DatabaseUtils.rollBack(conn);

    }

    // Check that there are still sensor_types records.
    try (Connection conn = getDataSource().getConnection();
      PreparedStatement stmt = conn
        .prepareStatement("SELECT * FROM variable_sensors");
      ResultSet records = stmt.executeQuery()) {

      assertTrue(records.next());

    }
  }

  /**
   * Test that rolling back an auto-commit connection does not throw an error.
   * Also check that the rollback call does not affect the attempted actions.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void rollBackAutoCommitTest() throws Exception {
    // Execute a statement to empty the sensor_types table, but roll back the
    // transaction.
    try (Connection conn = getDataSource().getConnection()) {

      try (PreparedStatement stmt = conn
        .prepareStatement("DELETE FROM variable_sensors")) {
        stmt.execute();
      }

      DatabaseUtils.rollBack(conn);

    }

    // Check that there are still sensor_types records.
    try (Connection conn = getDataSource().getConnection();
      PreparedStatement stmt = conn
        .prepareStatement("SELECT * FROM variable_sensors");
      ResultSet records = stmt.executeQuery()) {

      assertFalse(records.next());
    }
  }

  /**
   * Test that rolling back a {@code null} test does not fail.
   */
  @Test
  public void rollbackNullTest() {
    DatabaseUtils.rollBack(null);
  }

  /**
   * Test that a single connection can be closed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void closeConnectionSingleTest() throws Exception {
    Connection conn = getDataSource().getConnection();
    assertFalse(conn.isClosed());

    DatabaseUtils.closeConnection(conn);
    assertTrue(conn.isClosed());
  }

  /**
   * Test that a single connection can be closed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void closeConnectionMultipleTest() throws Exception {
    Connection conn = getDataSource().getConnection();
    Connection conn2 = getDataSource().getConnection();
    assertFalse(conn.isClosed());
    assertFalse(conn2.isClosed());

    DatabaseUtils.closeConnection(conn, conn2);
    assertTrue(conn.isClosed());
    assertTrue(conn2.isClosed());
  }

  /**
   * Check that a connection with {@code autoCommit == false} has its changes
   * rolled back on close.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest
  @Test
  public void closeConnectionRollbackTest() throws Exception {

    Connection conn = getDataSource().getConnection();
    conn.setAutoCommit(false);

    try (PreparedStatement stmt = conn
      .prepareStatement("DELETE FROM variable_sensors")) {
      stmt.execute();
    }

    DatabaseUtils.closeConnection(conn);

    // Check that the connection is close
    assertTrue(conn.isClosed());

    // Check that the changes weren't committed
    try (Connection conn2 = getDataSource().getConnection();
      PreparedStatement stmt = conn2
        .prepareStatement("SELECT * FROM variable_sensors");
      ResultSet records = stmt.executeQuery()) {

      assertTrue(records.next());
    }
  }

  /**
   * Test that the autoCommit flag is set when connections are closed.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void closeConnectionSetAutoCommitTest() throws Exception {
    Connection conn = Mockito.mock(Connection.class);
    DatabaseUtils.closeConnection(conn);
    Mockito.verify(conn).setAutoCommit(true);
  }

  /**
   * Test that {@link DatabaseUtils#getDatabaseFieldName(String)} replaces
   * special characters as it should.
   *
   * @param input
   *          The input value.
   * @param expected
   *          The expected output value.
   */
  @ParameterizedTest
  @CsvSource(value = { "java|java", "with space|with_space",
    "with(brackets)|withbrackets", "with,comma|withcomma" }, delimiter = '|')
  public void getDatabaseFieldNameTest(String input, String expected) {
    assertEquals(expected, DatabaseUtils.getDatabaseFieldName(input));
  }

  /**
   * Test that {@link DatabaseUtils#getDatabaseFieldName(String)} works with
   * null values.
   */
  @Test
  public void getDatabaseFieldNameNullTest() {
    assertNull(DatabaseUtils.getDatabaseFieldName(null));
  }

  /**
   * Test for exception in
   * {@link DatabaseUtils#makeInStatementSql(String, int...)} with a null SQL
   * string.
   */
  @Test
  public void makeInStatementSqlNullStringTest() {
    assertThrows(MissingParamException.class, () -> {
      DatabaseUtils.makeInStatementSql(null, 4);
    });
  }

  /**
   * Test for exception in
   * {@link DatabaseUtils#makeInStatementSql(String, int...)} with a null size
   * string.
   */
  @Test
  public void makeInStatementSqlNullInSizeTest() {
    assertThrows(MissingParamException.class, () -> {
      DatabaseUtils.makeInStatementSql("SELECT * FROM table", null);
    });
  }

  /**
   * Test that a mismatch of IN parameters and sizes (IN &lt; sizes) is
   * correctly detected in
   * {@link DatabaseUtils#makeInStatementSql(String, int...)}.
   */
  @Test
  public void makeInStatementSqlTooFewInTest() {
    assertThrows(MissingParamException.class, () -> {
      DatabaseUtils.makeInStatementSql(
        "SELECT * FROM table WHERE a IN " + DatabaseUtils.IN_PARAMS_TOKEN, 4,
        5);
    });
  }

  /**
   * Test that a mismatch of IN parameters and sizes (sizes &lt; IN) is
   * correctly detected in
   * {@link DatabaseUtils#makeInStatementSql(String, int...)}.
   */
  @Test
  public void makeInStatementSqlTooFewSizesTest() {
    assertThrows(MissingParamException.class, () -> {
      DatabaseUtils.makeInStatementSql(
        "SELECT * FROM table WHERE a IN " + DatabaseUtils.IN_PARAMS_TOKEN
          + " AND b IN " + DatabaseUtils.IN_PARAMS_TOKEN,
        4);
    });
  }

  /**
   * Test that a mismatch of IN parameters and sizes (no IN parameters) is
   * correctly detected in
   * {@link DatabaseUtils#makeInStatementSql(String, int...)}.
   */
  @Test
  public void makeInStatementSqlZeroInsTest() {
    assertThrows(MissingParamException.class, () -> {
      DatabaseUtils.makeInStatementSql("SELECT * FROM table", 4);
    });
  }

  /**
   * Test {@link DatabaseUtils#makeInStatementSql(String, int...)} with one IN
   * parameter.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void makeInStatementsSqlOneInTest() throws Exception {
    String input = "SELECT * FROM table WHERE a IN "
      + DatabaseUtils.IN_PARAMS_TOKEN;
    String output = "SELECT * FROM table WHERE a IN (?,?,?)";

    assertEquals(output, DatabaseUtils.makeInStatementSql(input, 3));
  }

  /**
   * Test {@link DatabaseUtils#makeInStatementSql(String, int...)} with two IN
   * parameters.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void makeInStatementsSqlTwoInTest() throws Exception {
    String input = "SELECT * FROM table WHERE a IN "
      + DatabaseUtils.IN_PARAMS_TOKEN + " AND b IN "
      + DatabaseUtils.IN_PARAMS_TOKEN;
    String output = "SELECT * FROM table WHERE a IN (?,?,?) AND b IN (?,?,?,?)";

    assertEquals(output, DatabaseUtils.makeInStatementSql(input, 3, 4));
  }
}
