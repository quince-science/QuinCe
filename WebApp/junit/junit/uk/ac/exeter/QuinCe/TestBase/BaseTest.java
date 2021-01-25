package junit.uk.ac.exeter.QuinCe.TestBase;

import java.util.List;
import java.util.stream.Stream;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.sql.DataSource;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * This is a base test class that provides basic setup for the test
 * infrastructure along with some useful methods for common test tasks.
 * <p>
 * The setup provides the following:
 * </p>
 * <ul>
 * <li>A global {@link ApplicationContext}</li>
 * <li>A mock WebApp context with an H2 database setup and other required items.
 * This is initialised before each test class (see
 * {@link #createServletContextEvent}).</li>
 * <li>Setup for tests using the Flyway database migration framework.</li>
 * </ul>
 *
 * <p>
 * Any tests that use the {@code @FlywayTest} annotation will have a database
 * initialised using the main application's database schema and default records.
 * Additional custom migrations for specific tests can be defined using
 * {@code @FlywayTest locationsForMigrate = {"resources/sql/..."}}.
 * </p>
 *
 * @author Steve Jones
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "/resources/context/testContext.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
  FlywayTestExecutionListener.class })
public class BaseTest {

  /**
   * A global application context
   */
  @Autowired
  protected ApplicationContext context;

  /**
   * A mock Servlet Context
   */
  @Mock
  protected static ServletContext servletContext;

  @Mock
  protected static FacesContext facesContext;

  /**
   * A mock Servlet Context Event
   */
  @Mock
  protected static ServletContextEvent servletContextEvent;

  /**
   * Initialises the mock {@link ServletContext} and
   * {@link ServletContextEvent}.
   */
  @BeforeAll
  public static void createServletContextEvent() {

    // I have read that you shouldn't create mocks of classes that you don't
    // own.
    // But I don't know how to do this any other way, so bollocks to it.
    servletContext = Mockito.mock(ServletContext.class);
    Mockito.doReturn(TestResourceManager.DATABASE_NAME).when(servletContext)
      .getInitParameter("database.name");
    Mockito.doReturn(TestResourceManager.CONFIG_PATH).when(servletContext)
      .getInitParameter("configuration.path");

    facesContext = MockFacesContext.mockFacesContext();

    servletContextEvent = Mockito.mock(ServletContextEvent.class);
    Mockito.doReturn(servletContext).when(servletContextEvent)
      .getServletContext();
  }

  /**
   * Ensures that the {@link ResourceManager} is destroyed after every test
   * class.
   */
  @AfterAll
  public static void globalTeardown() {
    facesContext.release();
    ResourceManager.destroy();
  }

  /**
   * Initialise the Resource Manager using the Mocked {@link ServletContext} and
   * {@link ServletContextEvent} created by
   * {@link #createServletContextEvent()}.
   */
  public void initResourceManager() {
    if (null == ResourceManager.getInstance()) {
      ResourceManager resourceManager = new TestResourceManager(
        getDataSource());
      resourceManager.contextInitialized(servletContextEvent);
    }
  }

  /**
   * Get a data source linked to the H2 test database defined in the
   * {@link #context}.
   *
   * @return A data source
   */
  protected DataSource getDataSource() {
    return (DataSource) context.getBean("dataSourceRef");
  }

  /**
   * Create a {@link Stream} of {@code null} and empty String values
   *
   * <p>
   * Some tests need to check behaviours with empty {@link String} values. This
   * method provides a {@link Stream} of empty {@link String}s that can be used
   * as input to a {@link ParameterizedTest}.
   * </p>
   *
   * @return A {@link Stream} of various empty {@link String} values.
   */
  protected static Stream<String> createNullEmptyStrings() {
    return Stream.of(null, "", " ", "  ", "\t", "\n");
  }

  /**
   * Create a set of invalid references for {@link SensorType} IDs ({@code 0}
   * and {@code -1}).
   *
   * @return The invalid IDs
   */
  protected static Stream<Long> createInvalidReferences() {
    return Stream.of(0L, -1L);
  }

  /**
   * Check that two lists contain the same values in the same order.
   *
   * @param list1
   *          The first list.
   * @param list2
   *          The second list.
   * @return {@code true} if the lists contain the same values; {@code false}
   *         otherwise.
   */
  protected boolean listsEqual(List<?> list1, List<?> list2) {
    boolean result = true;

    if (list1.size() != list2.size()) {
      result = false;
    } else if (list1.size() != 0) {
      for (int i = 0; i < list1.size(); i++) {
        if (!list1.get(i).equals(list2.get(i))) {
          result = false;
          break;
        }
      }
    }

    return result;
  }
}
