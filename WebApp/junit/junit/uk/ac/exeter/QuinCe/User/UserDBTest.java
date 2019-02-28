package junit.uk.ac.exeter.QuinCe.User;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.User.UserDB;

public class UserDBTest extends BaseTest {

  private void createUser() throws Exception {
    UserDB.createUser(getDataSource(), "test@test.com",
      "test".toCharArray(), "Testy", "McTestFace", false);
  }

  @FlywayTest
  @Test
  public void authenticateSuccessfulTest() throws Exception {
    createUser();
    assertEquals(UserDB.AUTHENTICATE_OK, UserDB.authenticate(getDataSource(), "test@test.com", "test".toCharArray()));
  }

}
