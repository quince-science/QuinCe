package unit.uk.ac.exeter.QuinCe.database.User;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import unit.uk.ac.exeter.QuinCe.database.BaseDbTest;

public class GenerateCodesTest extends BaseDbTest {

	@Before
	public void setUp() throws Exception {
		destroyTestUser();
		createTestUser();
	}
	
	@Test
	public void testGenerateVerificationCode() throws Exception {
		UserDB.generateEmailVerificationCode(getConnection(), testUser);
		assertNotNull(testUser.getEmailVerificationCode());
		
		User retrievedUser = UserDB.getUser(getConnection(), TEST_USER_EMAIL);
		assertNotNull(retrievedUser.getEmailVerificationCode());
	}

	@Test
	public void testGeneratePasswordResetCode() throws Exception {
		UserDB.generatePasswordResetCode(getConnection(), testUser);
		assertNotNull(testUser.getPasswordResetCode());
		
		User retrievedUser = UserDB.getUser(getConnection(), TEST_USER_EMAIL);
		assertNotNull(retrievedUser.getPasswordResetCode());
	}

	@After
	public void tearDown() throws Exception {
		destroyTestUser();
	}
}
