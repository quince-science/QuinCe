package unit.uk.ac.exeter.QuinCe.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;

public abstract class BaseDbTest {
	
	protected User testUser = null;
	
	protected static final String TEST_USER_EMAIL = "s.d.jones@exeter.ac.uk";
	protected static final String TEST_USER_PASSWORD = "mypassword";
	
	private DataSource dataSource;
	
	public DataSource getDataSource() throws Exception {
		if (null == dataSource) {
			dataSource = new TestDataSource();
		}
		
		return dataSource;
	}
	
	protected void createTestUser() throws Exception {
		testUser = UserDB.createUser(getDataSource(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray(), "Steve", "Jones", false);
	}
	
	protected void destroyTestUser() throws Exception {
		Connection connection = getDataSource().getConnection();
		PreparedStatement stmt = connection.prepareStatement("DELETE FROM user WHERE email = '" + TEST_USER_EMAIL + "'");
		stmt.execute();
		stmt.close();
		connection.close();
	}
}
