package junit.uk.ac.exeter.QuinCe.User;

import static org.junit.Assert.assertEquals;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.Before;
import org.junit.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.DBTest;
import uk.ac.exeter.QuinCe.User.UserDB;

public class UserDBTest extends DBTest {

  @Before
  public void initialize() throws Exception {
    createUser();
  }

  @FlywayTest
  @Test
  public void authenticateSuccessfulTest() throws Exception {
    assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(), "test@test.com", "test".toCharArray()));
  }

}
