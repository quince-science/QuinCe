package unit.uk.ac.exeter.QuinCe.data;

import static org.junit.Assert.*;

import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;

import uk.ac.exeter.QuinCe.data.User;

public class UserTest {

	User testUser;
	
	/**
	 * Makes a user object. This is used in individual tests
	 * as well as in the setUp method.
	 * 
	 * @return A User object
	 */
	private User makeUser() {
		return new User(1, "s.d.jones@exeter.ac.uk", "Steve", "Jones");
	}

	
	@Before
	public void setUp() {
		testUser = makeUser();
	}
	
	@Test
	public void testConstruction() {
		// Tests the User object returned by makeUser
		assertNotNull(makeUser());
	}
	
	@Test
	public void testConstructedID() {
		assertEquals(1, testUser.getDatabaseID());
	}
	
	@Test
	public void testConstructedEmail() {
		assertEquals("s.d.jones@exeter.ac.uk", testUser.getEmailAddress());
	}
	
	@Test
	public void testConstructedGivenName() {
		assertEquals("Steve", testUser.getGivenName());
	}
	
	@Test
	public void testConstructedSurname() {
		assertEquals("Jones", testUser.getSurname());
	}
	
	@Test
	public void testConstructedFullName() {
		assertEquals("Steve Jones", testUser.getFullName());
	}
	
	@Test
	public void testSetID() {
		testUser.setDatabaseID(2);
		assertEquals(2, testUser.getDatabaseID());
	}
	
	@Test
	public void testSetEmailCode() {
		Timestamp time = new Timestamp(System.currentTimeMillis());		
		testUser.setEmailVerificationCode("abcd", time);
		assertEquals("abcd", testUser.getEmailVerificationCode());
		assertEquals(time, testUser.getEmailVerificationCodeTime());
	}
	
	@Test
	public void testSetPasswordCode() {
		Timestamp time = new Timestamp(System.currentTimeMillis());		
		testUser.setPasswordResetCode("abcd", time);
		assertEquals("abcd", testUser.getPasswordResetCode());
		assertEquals(time, testUser.getPasswordResetCodeTime());
	}
}
