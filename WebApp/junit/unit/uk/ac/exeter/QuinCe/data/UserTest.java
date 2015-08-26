package unit.uk.ac.exeter.QuinCe.data;

import static org.junit.Assert.*;

import org.junit.After;
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
		return new User("s.d.jones@exeter.ac.uk", "Steve", "Jones");
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
	public void testGetEmail() {
		assertEquals("s.d.jones@exeter.ac.uk", testUser.getEmailAddress());
	}
	
	@Test
	public void testGetGivenName() {
		assertEquals("Steve", testUser.getGivenName());
	}
	
	@Test
	public void testGetSurname() {
		assertEquals("Jones", testUser.getSurname());
	}
	
	@Test
	public void testGetFullName() {
		assertEquals("Steve Jones", testUser.getFullName());
	}
	

}
