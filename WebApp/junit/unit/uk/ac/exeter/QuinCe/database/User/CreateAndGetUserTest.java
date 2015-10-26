package unit.uk.ac.exeter.QuinCe.database.User;

import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.database.User.UserExistsException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;
import static org.junit.Assert.*;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;

public class CreateAndGetUserTest extends BaseDbTest {

	@Before
	public void setUp() throws Exception {
		destroyTestUser();
	}
	
	@Test
	public void testCreateUser() throws Exception {
		
		// Create the user record
		UserDB.createUser(getDataSource(), TEST_USER_EMAIL, "mypassword".toCharArray(), "Steve", "Jones", false);
		
		// Find the user record
		User createdUser = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
		
		assertEquals(TEST_USER_EMAIL, createdUser.getEmailAddress());
		assertEquals("Steve", createdUser.getGivenName());
		assertEquals("Jones", createdUser.getSurname());
		assertNull(createdUser.getEmailVerificationCode());
		assertNull(createdUser.getEmailVerificationCodeTime());
		assertNull(createdUser.getPasswordResetCode());
		assertNull(createdUser.getPasswordResetCodeTime());
	}
	
	@Test
	public void testCreateUserWithVerificationCode() throws Exception {
		// Create the user record
		UserDB.createUser(getDataSource(), TEST_USER_EMAIL, "mypassword".toCharArray(), "Steve", "Jones", true);
		
		// Find the user record
		User createdUser = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
		
		assertNotNull(createdUser.getEmailVerificationCode());
		assertNotNull(createdUser.getEmailVerificationCodeTime());
	}

	
	@Test(expected=MissingParamException.class)
	public void testNullConnection() throws Exception {
		DataSource nullConnection = null;
		UserDB.createUser(nullConnection, TEST_USER_EMAIL, "mypassword".toCharArray(), "Steve", "Jones", false);
	}
	
	@Test(expected=MissingParamException.class)
	public void testNullEmail() throws Exception {
		String nullEmail = null;
		UserDB.createUser(getDataSource(), nullEmail, "mypassword".toCharArray(), "Steve", "Jones", false);
	}
	
	@Test(expected=MissingParamException.class)
	public void testNullPassword() throws Exception {
		char[] nullPassword = null;
		UserDB.createUser(getDataSource(), TEST_USER_EMAIL, nullPassword, "Steve", "Jones", false);
	}
	
	@Test(expected=MissingParamException.class)
	public void testNullGivenName() throws Exception {
		String nullGivenName = null;
		UserDB.createUser(getDataSource(), TEST_USER_EMAIL, "mypassword".toCharArray(), nullGivenName, "Jones", false);
	}
	
	@Test(expected=MissingParamException.class)
	public void testNullSurname() throws Exception {
		String nullSurname = null;
		UserDB.createUser(getDataSource(), TEST_USER_EMAIL, "mypassword".toCharArray(), "Steve", nullSurname, false);
	}
	
	@Test(expected=UserExistsException.class)
	public void testCreateDuplicateUser() throws Exception {
		createTestUser();
		UserDB.createUser(getDataSource(), TEST_USER_EMAIL, "mypassword".toCharArray(), "Keith", "Imposter", false);
	}
	
	@Test(expected=MissingParamException.class)
	public void testGetUserNullConnection() throws Exception {
		createTestUser();
		DataSource nullConn = null;
		UserDB.getUser(nullConn, TEST_USER_EMAIL);
	}
	
	@Test(expected=MissingParamException.class)
	public void testGetUserNullEmail() throws Exception {
		String nullEmail = null;
		UserDB.getUser(getDataSource(), nullEmail);
	}
	
	@Test
	public void testGetUserNoSuchUser() throws Exception {
		User foundUser = UserDB.getUser(getDataSource(), TEST_USER_EMAIL);
		assertNull(foundUser);
	}
	
	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}

}
