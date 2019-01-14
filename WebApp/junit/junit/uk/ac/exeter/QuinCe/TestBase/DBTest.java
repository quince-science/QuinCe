package junit.uk.ac.exeter.QuinCe.TestBase;

import javax.sql.DataSource;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import uk.ac.exeter.QuinCe.User.UserDB;

/**
 * Base class for all database tests. Provides base setup and useful methods
 * @author Steve Jones
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = {"/resources/context/testContext.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public abstract class DBTest {

  @Autowired
  protected ApplicationContext context;

  /**
   * Create a test user
   * @throws Exception On all errors
   */
  public void createUser() throws Exception {
    UserDB.createUser(getDataSource(), "test@test.com", "test".toCharArray(), "Testy", "McTestFace", false);
  }

  /**
   * Get a data source
   * @return A data source
   */
  protected DataSource getDataSource() {
    return (DataSource) context.getBean("dataSourceRef");
  }

}
