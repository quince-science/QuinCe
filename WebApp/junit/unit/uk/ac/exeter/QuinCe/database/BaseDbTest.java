package unit.uk.ac.exeter.QuinCe.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;

public abstract class BaseDbTest {
	
	protected User testUser = null;
	
	protected static final String TEST_USER_EMAIL = "s.d.jones@exeter.ac.uk";
	protected static final String TEST_USER_PASSWORD = "mypassword";
	
	private static Connection connection = null;
	
	public Connection getConnection() throws Exception {
		if (null == connection) {
			connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1/quince_test", "quince_dev", "quince_dev");
		}
		
		return connection;
	}
	
	protected void createTestUser() throws Exception {
		testUser = UserDB.createUser(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray(), "Steve", "Jones");
	}
	
	protected void destroyTestUser() throws Exception {
		PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM user WHERE email = '" + TEST_USER_EMAIL + "'");
		stmt.execute();
		stmt.close();
	}
}
