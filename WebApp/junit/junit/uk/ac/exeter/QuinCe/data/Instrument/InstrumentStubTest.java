package junit.uk.ac.exeter.QuinCe.data.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.DBTest;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

public class InstrumentStubTest extends DBTest {

  private static final String TEST_INSTRUMENT_NAME = "Test Instrument";

  @Test
  public void constructorTest() {
    InstrumentStub stub = new InstrumentStub(1L, TEST_INSTRUMENT_NAME);
    assertTrue(stub.getId() == 1L);
    assertTrue(stub.getName().equals(TEST_INSTRUMENT_NAME));
  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/user",
    "resources/sql/testbase/instrument"
  })
  @Test
  public void getFullInstrumentTest() throws Exception {

    initResourceManager();

    // Get the test instrument ID
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet record = null;
    long id = -1;
    String name = null;

    try {
      conn = getDataSource().getConnection();
      stmt = conn.prepareStatement("SELECT id, name FROM instrument");
      record = stmt.executeQuery();
      record.next();
      id = record.getLong(1);
      name = record.getString(2);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    // Make a stub and get the full instrument from it
    InstrumentStub stub = new InstrumentStub(id, name);
    Instrument instrument = stub.getFullInstrument();
    assertEquals(instrument.getDatabaseId(), id);
    assertEquals(instrument.getName(), name);
  }
}
