package junit.uk.ac.exeter.QuinCe.TestBase;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.sql.DataSource;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Base class for all database tests. Provides base setup and useful methods
 * @author Steve Jones
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"/resources/context/testContext.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public abstract class DBTest extends BaseTest {

  @Autowired
  protected ApplicationContext context;

  /**
   * Servlet Context
   */
  @Mock protected static ServletContext servletContext;

  /**
   * Servlet Context Event
   */
  @Mock protected static ServletContextEvent servletContextEvent;

  @BeforeAll
  public static void createServletContextEvent() {

    // I have read that you shouldn't create mocks of classes that you don't own.
    // But I don't know how to do this any other way, so bollocks to it.
    servletContext = Mockito.mock(ServletContext.class);
    Mockito.doReturn(TestResourceManager.DATABASE_NAME).when(servletContext).getInitParameter("database.name");
    Mockito.doReturn(TestResourceManager.CONFIG_PATH).when(servletContext).getInitParameter("configuration.path");

    servletContextEvent = Mockito.mock(ServletContextEvent.class);
    Mockito.doReturn(servletContext).when(servletContextEvent).getServletContext();
  }

  /**
   * Initialise the Resource Manager
   */
  public void initResourceManager() {
    if (null == ResourceManager.getInstance()) {
      ResourceManager resourceManager = new TestResourceManager(getDataSource());
      resourceManager.contextInitialized(servletContextEvent);
    }
  }

  /**
   * Get a data source
   * @return A data source
   */
  protected DataSource getDataSource() {
    return (DataSource) context.getBean("dataSourceRef");
  }

}
