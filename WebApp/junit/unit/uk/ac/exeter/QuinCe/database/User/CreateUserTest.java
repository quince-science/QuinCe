package unit.uk.ac.exeter.QuinCe.database.User;

import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;
import static org.junit.Assert.*;

import java.sql.PreparedStatement;

import org.junit.After;
import org.junit.Before;

public class CreateUserTest extends BaseDbTest {

	@Before
	public void setUp() throws Exception {
		destroyTestUser();
	}
	
	@Test
	public void testCreateUser() throws Exception {
		
		// Create the user record
		UserDB.createUser(getConnection(), TEST_USER_EMAIL, "mypassword".toCharArray(), "Steve", "Jones");
		
		// Find the user record
		User createdUser = UserDB.getUser(getConnection(), TEST_USER_EMAIL);
		
		assertEquals(TEST_USER_EMAIL, createdUser.getEmailAddress());
		assertEquals("Steve", createdUser.getGivenName());
		assertEquals("Jones", createdUser.getSurname());
		assertNull(createdUser.getEmailVerificationCode());
		assertNull(createdUser.getPasswordResetCode());
	}
	
	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}

}
