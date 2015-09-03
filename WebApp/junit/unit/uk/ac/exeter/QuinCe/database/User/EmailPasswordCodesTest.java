package unit.uk.ac.exeter.QuinCe.database.User;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.junit.After;
import org.junit.Before;

import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.User.NoSuchUserException;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingDataException;

public class EmailPasswordCodesTest extends BaseDbTest {

	@Before
	public void setUp() throws Exception {
		destroyTestUser();
		createTestUser();
	}
	
	@Test
	public void testSetEmailVerificationCode() throws Exception {
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		assertNotNull(testUser.getEmailVerificationCode());
		assertNotNull(testUser.getEmailVerificationCodeTime());
		
		User dbCheckUser = UserDB.getUser(getConnection(), TEST_USER_EMAIL);
		assertNotNull(dbCheckUser.getEmailVerificationCode());
		assertNotNull(dbCheckUser.getEmailVerificationCodeTime());
	}
	
	@Test
	public void testSetPasswordResetCode() throws Exception {
		UserDB.generatePasswordResetCode(getConnection(), testUser);
		assertNotNull(testUser.getPasswordResetCode());
		assertNotNull(testUser.getPasswordResetCodeTime());
		
		User dbCheckUser = UserDB.getUser(getConnection(), TEST_USER_EMAIL);
		assertNotNull(dbCheckUser.getPasswordResetCode());
		assertNotNull(dbCheckUser.getPasswordResetCodeTime());
	}
	
	@Test
	public void testCheckEmailVerificationCodeGood() throws Exception {
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		String storedCode = testUser.getEmailVerificationCode();
		assertEquals(UserDB.CODE_OK, UserDB.checkEmailVerificationCode(getConnection(), TEST_USER_EMAIL, storedCode));
	}

	@Test
	public void testCheckPasswordResetCodeGood() throws Exception {
		UserDB.generatePasswordResetCode(getConnection(), testUser);
		String storedCode = testUser.getPasswordResetCode();
		assertEquals(UserDB.CODE_OK, UserDB.checkPasswordResetCode(getConnection(), TEST_USER_EMAIL, storedCode));
	}

	@Test
	public void testCheckEmailVerificationCodeBad() throws Exception {
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		assertEquals(UserDB.CODE_FAILED, UserDB.checkEmailVerificationCode(getConnection(), TEST_USER_EMAIL, "lkfdjglkfdjg"));
	}

	@Test
	public void testCheckPasswordResetCodeBad() throws Exception {
		UserDB.generatePasswordResetCode(getConnection(), testUser);
		assertEquals(UserDB.CODE_FAILED, UserDB.checkPasswordResetCode(getConnection(), TEST_USER_EMAIL, "lkfdjglkfdjg"));
	}

	@Test
	public void testCheckEmailVerifificationCodeExpired() throws Exception {
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		String storedCode = testUser.getEmailVerificationCode();
		
		PreparedStatement stmt = getConnection().prepareStatement("UPDATE user SET email_code_time = ? WHERE id = ?");
		stmt.setTimestamp(1, getOldTimestamp());
		stmt.setInt(2, testUser.getDatabaseID());
		stmt.execute();
		
		assertEquals(UserDB.CODE_EXPIRED, UserDB.checkEmailVerificationCode(getConnection(), TEST_USER_EMAIL, storedCode));
	}
	
	@Test
	public void testCheckPasswordResetCodeExpired() throws Exception {
		UserDB.generatePasswordResetCode(getConnection(), testUser);
		String storedCode = testUser.getPasswordResetCode();
		
		PreparedStatement stmt = getConnection().prepareStatement("UPDATE user SET password_code_time = ? WHERE id = ?");
		stmt.setTimestamp(1, getOldTimestamp());
		stmt.setInt(2, testUser.getDatabaseID());
		stmt.execute();
		
		assertEquals(UserDB.CODE_EXPIRED, UserDB.checkPasswordResetCode(getConnection(), TEST_USER_EMAIL, storedCode));
	}
	
	@Test
	public void testCheckEmailVerificationCodeNoCodeSet() throws Exception {
		assertEquals(UserDB.CODE_FAILED, UserDB.checkEmailVerificationCode(getConnection(), TEST_USER_EMAIL, "dsfgfd"));
	}
	
	@Test
	public void testCheckPasswordResetCodeNoCodeSet() throws Exception {
		assertEquals(UserDB.CODE_FAILED, UserDB.checkPasswordResetCode(getConnection(), TEST_USER_EMAIL, "dsfgfd"));
	}
	
	@Test
	public void testCheckEmailVerificationCodeNoTimestamp() throws Exception {
		// This should never happen, but we'll test the safeguard anyway :)
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		String storedCode = testUser.getEmailVerificationCode();

		PreparedStatement stmt = getConnection().prepareStatement("UPDATE user SET email_code_time = NULL WHERE id = ?");
		stmt.setInt(1, testUser.getDatabaseID());
		stmt.execute();
		
		assertEquals(UserDB.CODE_EXPIRED, UserDB.checkEmailVerificationCode(getConnection(), TEST_USER_EMAIL, storedCode));
	}
	
	@Test
	public void testCheckPasswordResetCodeNoTimestamp() throws Exception {
		// This should never happen, but we'll test the safeguard anyway :)
		UserDB.generatePasswordResetCode(getConnection(), testUser);
		String storedCode = testUser.getPasswordResetCode();

		PreparedStatement stmt = getConnection().prepareStatement("UPDATE user SET password_code_time = NULL WHERE id = ?");
		stmt.setInt(1, testUser.getDatabaseID());
		stmt.execute();
		
		assertEquals(UserDB.CODE_EXPIRED, UserDB.checkPasswordResetCode(getConnection(), TEST_USER_EMAIL, storedCode));
	}

	@Test(expected=DatabaseException.class)
	public void testGenerateEmailVerificationCodeNullConnection() throws Exception {
		Connection nullConn = null;
		UserDB.generateEmailVerificationCode(nullConn, testUser);
	}
	
	@Test(expected=MissingDataException.class)
	public void testGenerateEmailVerificationCodeNullUser() throws Exception {
		User nullUser = null;
		UserDB.generateEmailVerificationCode(getConnection(), nullUser);
	}
	
	@Test(expected=DatabaseException.class)
	public void testGeneratePasswordResetCodeNullConnection() throws Exception {
		Connection nullConn = null;
		UserDB.generatePasswordResetCode(nullConn, testUser);
	}
	
	@Test(expected=MissingDataException.class)
	public void testGeneratePasswordResetCodeNullUser() throws Exception {
		User nullUser = null;
		UserDB.generatePasswordResetCode(getConnection(), nullUser);
	}
	
	@Test(expected=NoSuchUserException.class)
	public void testGenerateEmailVerificationCodeNoSuchUser() throws Exception {
		User unregisteredUser = new User(-999, "dummy@test.com", "No", "Body");
		UserDB.generateEmailVerificationCode(getConnection(), unregisteredUser);
	}
	
	@Test(expected=NoSuchUserException.class)
	public void testGeneratePasswordResetCodeNoSuchUser() throws Exception {
		User unregisteredUser = new User(-999, "dummy@test.com", "No", "Body");
		UserDB.generatePasswordResetCode(getConnection(), unregisteredUser);
	}
	
	@Test(expected=DatabaseException.class)
	public void testCheckEmailVerificationCodeNullConnection() throws Exception {
		Connection nullConn = null;
		UserDB.checkEmailVerificationCode(nullConn, TEST_USER_EMAIL, "jhfdsglkjfdlkg");
	}
	
	@Test(expected=MissingDataException.class)
	public void testCheckEmailVerificationCodeNullEmail() throws Exception {
		String nullEmail = null;
		UserDB.checkEmailVerificationCode(getConnection(), nullEmail, ";ljdflkgjfd");
	}

	@Test(expected=MissingDataException.class)
	public void testCheckEmailVerificationCodeNullCode() throws Exception {
		String nullCode = null;
		UserDB.checkEmailVerificationCode(getConnection(), TEST_USER_EMAIL, nullCode);
	}

	@Test(expected=DatabaseException.class)
	public void testCheckPasswordResetCodeNullConnection() throws Exception {
		Connection nullConn = null;
		UserDB.checkPasswordResetCode(nullConn, TEST_USER_EMAIL, "jhfdsglkjfdlkg");
	}
	
	@Test(expected=MissingDataException.class)
	public void testCheckPasswordResetCodeNullEmail() throws Exception {
		String nullEmail = null;
		UserDB.checkPasswordResetCode(getConnection(), nullEmail, ";ljdflkgjfd");
	}

	@Test(expected=MissingDataException.class)
	public void testCheckPasswordResetCodeNullCode() throws Exception {
		String nullCode = null;
		UserDB.checkPasswordResetCode(getConnection(), TEST_USER_EMAIL, nullCode);
	}

	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}
	
	private Timestamp getOldTimestamp() {
		long millis = System.currentTimeMillis() - (100 * DateTimeUtils.MILLIS_PER_HOUR);
		return new Timestamp(millis);
	}
}
