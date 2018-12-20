package junit.uk.ac.exeter.QuinCe.User;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.DBTest;
import uk.ac.exeter.QuinCe.User.UserDB;

public class UserDBTest extends DBTest {


  @FlywayTest
  @Test
  public void databaseTest() throws Exception {
    UserDB.createUser(getDataSource(), "steve.jones@uib.no", "password".toCharArray(), "Steve", "Jones", false);
  }

}
