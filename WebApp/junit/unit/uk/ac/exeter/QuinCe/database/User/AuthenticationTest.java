package unit.uk.ac.exeter.QuinCe.database.User;

import static org.junit.Assert.*;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
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
		assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
	}

	@Test
	public void testBadEmailAuthenticationFailure() throws Exception {
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getDataSource(), "kfdjglkfdjglkj", TEST_USER_PASSWORD.toCharArray()));
	}

	@Test
	public void testBadPasswordAuthenticationFailure() throws Exception {
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, "lksjglkdfjg".toCharArray()));
	}
	
	@Test
	public void testChangePassword() throws Exception {
		UserDB.changePassword(getDataSource(), testUser, TEST_USER_PASSWORD.toCharArray(), "New Password".toCharArray());
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
		assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, "New Password".toCharArray()));
	}
	
	@Test
	public void testWithEmailCodeSet() throws Exception {
		UserDB.generateEmailVerificationCode(getDataSource(), testUser);
		assertEquals(UserDB.AUTHENTICATE_EMAIL_CODE_SET, UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray()));
	}
	
	@Test(expected=MissingDataException.class)
	public void testNullConnecton() throws Exception {
		DataSource nullConn = null;
		UserDB.authenticate(nullConn, TEST_USER_EMAIL, TEST_USER_PASSWORD.toCharArray());
	}
	
	@Test
	public void testNullEmail() throws Exception {
		String nullEmail = null;
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getDataSource(), nullEmail, TEST_USER_PASSWORD.toCharArray()));
	}

	@Test
	public void testNullPassword() throws Exception {
		char[] nullPassword = null;
		assertEquals(UserDB.AUTHENTICATE_FAILED, UserDB.authenticate(getDataSource(), TEST_USER_EMAIL, nullPassword));
	}

	@Test(expected=MissingDataException.class)
	public void testChangePasswordNullConneciton() throws Exception {
		DataSource nullConn = null;
		UserDB.changePassword(nullConn, testUser, TEST_USER_PASSWORD.toCharArray(), "lkdfjglkfdj".toCharArray());
	}
	
	@Test(expected=MissingDataException.class)
	public void testChangePasswordNullUser() throws Exception {
		User nullUser = null;
		UserDB.changePassword(getDataSource(), nullUser, TEST_USER_PASSWORD.toCharArray(), "lkdfjglkfdj".toCharArray());
	}
	
	@Test(expected=MissingDataException.class)
	public void testChangePasswordNullOld() throws Exception {
		char[] nullPassword = null;
		UserDB.changePassword(getDataSource(), testUser, nullPassword, "lkdfjglkfdj".toCharArray());
	}

	@Test(expected=MissingDataException.class)
	public void testChangePasswordNullNew() throws Exception {
		char[] nullPassword = null;
		UserDB.changePassword(getDataSource(), testUser, TEST_USER_PASSWORD.toCharArray(), nullPassword);
	}

	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}
}
