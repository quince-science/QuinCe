package unit.uk.ac.exeter.QuinCe.database.User;

import static org.junit.Assert.*;

import java.sql.Connection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.utils.MissingDataException;
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
	public void testChangePassword() throws Exception {
		UserDB.changePassword(getConnection(), testUser, TEST_USER_PASSWORD.toCharArray(), "New Password".toCharArray());
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
		assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, "New Password".toCharArray()));
	}
	
	@Test
	public void testWithEmailCodeSet() throws Exception {
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		assertEquals(UserDB.AUTHENTICATE_EMAIL_CODE_SET, UserDB.authenticate(getConnection(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
	}
	
	@Test(expected=DatabaseException.class)
	public void testNullConnecton() throws Exception {
		Connection nullConn = null;
		UserDB.authenticate(nullConn, TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray());
	}
	
	@Test(expected=MissingDataException.class)
	public void testNullEmail() throws Exception {
		String nullEmail = null;
		UserDB.authenticate(getConnection(), nullEmail, TEST_USER_PASSWORD.toCharArray());
	}

	@Test(expected=MissingDataException.class)
	public void testNullPassword() throws Exception {
		char[] nullPassword = null;
		UserDB.authenticate(getConnection(), TEST_USER_EMAIL, nullPassword);
	}

	@Test(expected=DatabaseException.class)
	public void testChangePasswordNullConneciton() throws Exception {
		Connection nullConn = null;
		UserDB.changePassword(nullConn, testUser, TEST_USER_PASSWORD.toCharArray(), "lkdfjglkfdj".toCharArray());
	}
	
	@Test(expected=MissingDataException.class)
	public void testChangePasswordNullUser() throws Exception {
		User nullUser = null;
		UserDB.changePassword(getConnection(), nullUser, TEST_USER_PASSWORD.toCharArray(), "lkdfjglkfdj".toCharArray());
	}
	
	@Test(expected=MissingDataException.class)
	public void testChangePasswordNullOld() throws Exception {
		char[] nullPassword = null;
		UserDB.changePassword(getConnection(), testUser, nullPassword, "lkdfjglkfdj".toCharArray());
	}

	@Test(expected=MissingDataException.class)
	public void testChangePasswordNullNew() throws Exception {
		char[] nullPassword = null;
		UserDB.changePassword(getConnection(), testUser, TEST_USER_PASSWORD.toCharArray(), nullPassword);
	}

	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}
}
