package unit.uk.ac.exeter.QuinCe.database.User;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.junit.After;
import org.junit.Before;

import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

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
	
	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}
	
	private Timestamp getOldTimestamp() {
		long millis = System.currentTimeMillis() - (100 * DateTimeUtils.MILLIS_PER_HOUR);
		return new Timestamp(millis);
	}
}
