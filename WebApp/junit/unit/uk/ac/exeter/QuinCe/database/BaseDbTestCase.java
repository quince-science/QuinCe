package unit.uk.ac.exeter.QuinCe.database;

import java.sql.Connection;
import java.sql.DriverManager;

import org.dbunit.DatabaseTestCase;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;

public abstract class BaseDbTestCase extends DatabaseTestCase {
	
	public IDatabaseConnection getConnection() throws Exception {
		Connection jdbcConnection = 
				DriverManager.getConnection("jdbc:mysql://127.0.0.1/quince_test", "quince_dev", "quince_dev");
			        
		return new DatabaseConnection(jdbcConnection);
	}
}
