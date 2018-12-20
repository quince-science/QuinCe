package junit.uk.ac.exeter.QuinCe.User;

import java.net.URL;
import java.net.URLClassLoader;

import javax.sql.DataSource;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import uk.ac.exeter.QuinCe.User.UserDB;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = {"/resources/context/testContext.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class UserDBTest {

  @Autowired
  protected ApplicationContext context;

  @FlywayTest
  @Test
  public void databaseTest() throws Exception {
    final DataSource dataSource = (DataSource) context.getBean("dataSourceRef");
    UserDB.createUser(dataSource, "steve.jones@uib.no", "password".toCharArray(), "Steve", "Jones", false);
  }

}
