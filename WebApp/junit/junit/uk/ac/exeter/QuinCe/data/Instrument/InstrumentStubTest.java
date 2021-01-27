package junit.uk.ac.exeter.QuinCe.data.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for the {@link InstrumentStub} class.
 *
 * <p>
 * Some tests use the instrument defined in the test base
 * {@code WebApp/junit/resources/sql/testbase/instrument}, while others use a
 * fake instrument defined in the class.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class InstrumentStubTest extends BaseTest {

  /**
   * The name of the fake test instrument.
   *
   * <p>
   * This instrument is never created in the database - it is used to test the
   * internal methods of the {@link InstrumentStub} class.
   * </p>
   */
  private static final String TEST_INSTRUMENT_NAME = "InstrumentStubTest Instrument";

  /**
   * The ID of the instrument in the testbase database.
   */
  private long dbInstrumentId;

  /**
   * The name of the instrument in the testbase database.
   */
  private String dbInstrumentName;

  /**
   * An instrument ID that does not exist in the database
   */
  private long badInstrumentId = 9000000L;

  /**
   * An instrument name that does not exist in the database
   */
  private String badInstrumentName = "9 million bicycles";

  @AfterEach
  public void tearDown() {
    ResourceManager.destroy();
  }

  /**
   * Load the {@code id} and {@code name} of the instrument in the testbase
   * database.
   *
   * @throws SQLException
   *           If any internal errors are encountered.
   */
  private void loadDBInstrument() throws SQLException {
    initResourceManager();

    // Get the test instrument ID
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet record = null;

    try {
      conn = getDataSource().getConnection();
      stmt = conn.prepareStatement("SELECT id, name FROM instrument");
      record = stmt.executeQuery();
      record.next();
      dbInstrumentId = record.getLong(1);
      dbInstrumentName = record.getString(2);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Test that an {@link InstrumentStub} object can be created.
   *
   * <p>
   * Also tests that the {@link InstrumentStub#getId()} and
   * {@link InstrumentStub#getName()} methods work.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @Test
  public void validConstructorTest() throws Exception {
    InstrumentStub stub = new InstrumentStub(1L, TEST_INSTRUMENT_NAME);
    assertTrue(stub.getId() == 1L);
    assertTrue(stub.getName().equals(TEST_INSTRUMENT_NAME));
  }

  /**
   * Test that {@link InstrumentStub}s with invalid IDs cannot be created.
   *
   * <p>
   * Note that this test does not check for missing IDs in the database. That is
   * done by {@link #getFullInstrumentIdNotInDatabaseTest()}.
   * </p>
   *
   * @param id
   *          The invalid ID
   *
   * @see #createInvalidReferences()
   */
  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void invalidIdConstructorTest(long id) {
    assertThrows(MissingParamException.class,
      () -> new InstrumentStub(id, TEST_INSTRUMENT_NAME));
  }

  /**
   * Test that {@link InstrumentStub}s with invalid names cannot be created.
   *
   * <p>
   * Note that this test does not check for missing names in the database. That
   * is done by {@link #getFullInstrumentMismatchedIDAndNameTest()} and
   * {@link #getFullInstrumentMissingIDAndNameTest()}.
   * </p>
   *
   * @param name
   *          The invalid name
   *
   * @see #createNullEmptyStrings()
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void invalidNameConstructorTest(String name) {
    assertThrows(MissingParamException.class,
      () -> new InstrumentStub(1L, name));
  }

  /**
   * Test that a complete {@link Instrument} object can be retrieved using an
   * {@link InstrumentStub} object.
   *
   * <p>
   * Constructs an {@link InstrumentStub} based on the {@code id} and
   * {@code name} of the test base instrument in the database, and then calls
   * {@link InstrumentStub#getFullInstrument()} to retrieve the full
   * {@link Instrument} object.
   * </p>
   *
   * <p>
   * Note that the contents of the {@link Instrument} object are not checked
   * beyond making sure that the {@code id} and {@code name} match the
   * {@link InstrumentStub}. Other checks will be performed in tests for the
   * {@link InstrumentDB}.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentTest() throws Exception {

    loadDBInstrument();

    // Make a stub and get the full instrument from it
    InstrumentStub stub = new InstrumentStub(dbInstrumentId, dbInstrumentName);
    Instrument instrument = stub.getFullInstrument();
    assertEquals(instrument.getDatabaseId(), dbInstrumentId);
    assertEquals(instrument.getName(), dbInstrumentName);
  }

  /**
   * Test that {@link InstrumentStub#getFullInstrument()} with an ID not in the
   * database fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentIdNotInDatabaseTest() throws Exception {
    loadDBInstrument();

    InstrumentStub stub = new InstrumentStub(badInstrumentId, dbInstrumentName);
    assertThrows(RecordNotFoundException.class, () -> stub.getFullInstrument());
  }

  /**
   * Test that {@link InstrumentStub#getFullInstrument()} with a valid ID but a
   * different name fails
   *
   * <p>
   * The {@link InstrumentStub#getFullInstrument()} method contains an assertion
   * to ensure that the retrieved instrument name matches the name in the stub.
   * </p>
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentMismatchedIDAndNameTest() throws Exception {
    loadDBInstrument();

    InstrumentStub stub = new InstrumentStub(dbInstrumentId, badInstrumentName);
    assertThrows(AssertionError.class, () -> stub.getFullInstrument());
  }

  /**
   * Test that {@link InstrumentStub#getFullInstrument()} with neither an ID nor
   * a name in the database fails.
   *
   * @throws Exception
   *           If any internal errors are encountered.
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentMissingIDAndNameTest() throws Exception {
    loadDBInstrument();

    InstrumentStub stub = new InstrumentStub(badInstrumentId,
      badInstrumentName);
    assertThrows(RecordNotFoundException.class, () -> stub.getFullInstrument());
  }
}
