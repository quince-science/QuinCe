package unit.uk.ac.exeter.QuinCe.database;

import java.sql.Connection;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class DatabaseConnectionTest extends BaseDbTest {

	@Test
	public void testConnection() throws Exception {
		Connection connection = getConnection();
		boolean isNull = (null == connection);
		assertFalse(isNull);
	}
	
	@After
	public void tearDown() {
		closeConnection();
	}
}
