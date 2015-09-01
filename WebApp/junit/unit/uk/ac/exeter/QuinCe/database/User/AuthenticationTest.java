package unit.uk.ac.exeter.QuinCe.database.User;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.exeter.QuinCe.database.User.UserDB;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;

public class AuthenticationTest extends BaseDbTest {

	@Before
	public void setUp() throws Exception {
		destroyTestUser();
		createTestUser();
	}
	
	@Test
	public void testGoodAuthentication() throws Exception {
		assertTrue(UserDB.authenticate(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
	}

	@Test
	public void testBadEmailAuthenticationFailure() throws Exception {
		assertFalse(UserDB.authenticate(getConnection(), "kfdjglkfdjglkj", TEST_USER_PASSWORD.toCharArray()));
	}

	@Test
	public void testBadPasswordAuthenticationFailure() throws Exception {
		assertFalse(UserDB.authenticate(getConnection(), TEST_USER_EMAIL, "lksjglkdfjg".toCharArray()));
	}
	
	@Test
	public void changePasswordTest() throws Exception {
		UserDB.changePassword(getConnection(), testUser, TEST_USER_PASSWORD.toCharArray(), "New Password".toCharArray());
		assertFalse(UserDB.authenticate(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
		assertTrue(UserDB.authenticate(getConnection(), TEST_USER_EMAIL, "New Password".toCharArray()));
		
	}

	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}
}
