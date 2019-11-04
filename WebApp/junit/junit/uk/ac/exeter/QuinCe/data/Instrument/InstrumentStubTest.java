package junit.uk.ac.exeter.QuinCe.data.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
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

  /**
   * Load the {@code id} and {@code name} of the instrument in the testbase
   * database.
   *
   * @throws SQLException
   *           If the instrument details cannot be retrieved
   */
  public void loadDBInstrument() throws SQLException {
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
   */
  @Test
  public void constructorTest() {
    InstrumentStub stub = new InstrumentStub(1L, TEST_INSTRUMENT_NAME);
    assertTrue(stub.getId() == 1L);
    assertTrue(stub.getName().equals(TEST_INSTRUMENT_NAME));
  }

  /**
   * Test that a complete {@link Instrument} object can be retrieved using an
   * {@link InstrumentStub} object.
   *
   * <p>
   * Constructs an {@link InstrumentStub} based on the {@code id} and {@name} of
   * the test base instrument in the database, and then calls
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
   * @throws DatabaseException
   *           If an error occurs while retrieving the full Instrument from the
   *           database
   * @throws RecordNotFoundException
   *           If the instrument record cannot be found in the database
   * @throws ResourceException
   *           If the data source cannot be retrieved from the
   *           {@link ResourceManager}
   * @throws InstrumentException
   *           If any instrument details are invalid
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentTest()
    throws SQLException, MissingParamException, DatabaseException,
    RecordNotFoundException, ResourceException, InstrumentException {

    loadDBInstrument();

    // Make a stub and get the full instrument from it
    InstrumentStub stub = new InstrumentStub(dbInstrumentId, dbInstrumentName);
    Instrument instrument = stub.getFullInstrument();
    assertEquals(instrument.getDatabaseId(), dbInstrumentId);
    assertEquals(instrument.getName(), dbInstrumentName);
  }

  /**
   * Test that {@link InstrumentStub#getFullInstrument()} with an invalid
   * {@code id} and valid {@name} fails
   *
   * @throws SQLException
   *           If the test instrument details cannot be retrieved
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentBadIdTest() throws SQLException {
    loadDBInstrument();

    InstrumentStub stub = new InstrumentStub(badInstrumentId, dbInstrumentName);
    assertThrows(RecordNotFoundException.class, () -> stub.getFullInstrument());
  }

  /**
   * Test that {@link InstrumentStub#getFullInstrument()} with a valid
   * {@code id} and valid {@name} fails.
   *
   * <p>
   * The {@link InstrumentStub#getFullInstrument()} method contains an assertion
   * to ensure that the retrieved instrument name matches the name in the stub.
   * </p>
   *
   * @throws SQLException
   *           If the test instrument details cannot be retrieved
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentBadNameTest() throws SQLException {
    loadDBInstrument();

    InstrumentStub stub = new InstrumentStub(dbInstrumentId, badInstrumentName);
    assertThrows(AssertionError.class, () -> stub.getFullInstrument());
  }

  /**
   * Test that {@link InstrumentStub#getFullInstrument()} with a invalid
   * {@code id} and invalid {@name} fails
   *
   * @throws SQLException
   *           If the test instrument details cannot be retrieved
   */
  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void getFullInstrumentBadIDNameTest() throws SQLException {
    loadDBInstrument();

    InstrumentStub stub = new InstrumentStub(badInstrumentId,
      badInstrumentName);
    assertThrows(RecordNotFoundException.class, () -> stub.getFullInstrument());
  }
}
