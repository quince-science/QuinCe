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
		assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
	}

	@Test
	public void testBadEmailAuthenticationFailure() throws Exception {
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getConnection(), "kfdjglkfdjglkj", TEST_USER_PASSWORD.toCharArray()));
	}

	@Test
	public void testBadPasswordAuthenticationFailure() throws Exception {
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, "lksjglkdfjg".toCharArray()));
	}
	
	@Test
	public void changePasswordTest() throws Exception {
		UserDB.changePassword(getConnection(), testUser, TEST_USER_PASSWORD.toCharArray(), "New Password".toCharArray());
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
		assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, "New Password".toCharArray()));
	}
	
	@Test
	public void testWithEmailCodeSet() throws Exception {
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		assertEquals(UserDB.AUTHENTICATE_EMAIL_CODE_SET, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
	}

	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}
}
