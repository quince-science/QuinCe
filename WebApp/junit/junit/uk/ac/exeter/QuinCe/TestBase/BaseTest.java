package junit.uk.ac.exeter.QuinCe.TestBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
 * {@see #createServletContextEvent}).</li>
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
   * A mock Servlet Contextt
   */
  @Mock
  protected static ServletContext servletContext;

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
   * Create a stream of {@code null} and empty String values
   *
   * <p>
   * Some tests need to check behaviours with empty {@link String} values. This
   * method provides a {@link Stream} of three types of empty {@link String}
   * ({@code null}, {@code ""} and {@code " "}) that can be used as input to a
   * {@link ParameterizedTest}.
   * </p>
   *
   * @return A {@link Stream} of various empty {@link String} values.
   */
  protected static Stream<String> createNullEmptyStrings() {
    return Stream.of(null, "", " ");
  }

  /**
   * Get the contents of a Test Set file as a stream
   *
   * <p>
   * Some tests take a large combination of inputs and have a corresponding
   * number of expected results. Instead of providing all these in code, it can
   * be easier to build an input file containing the test criteria.
   * </p>
   *
   * <p>
   * This method provides a means for a test to have its criteria defined in a
   * {@code .csv} file. It reads a given file, and provides a {@link Stream} of
   * {@link TestSetLine} objects each representing a single line in the file,
   * which can be used as input to a {@link ParameterizedTest}.
   * </p>
   *
   * <p>
   * Note that this functionality knows nothing about the structure of any given
   * {@code .csv} file - it is up to the test to know which columns it needs to
   * read for its own purposes.
   * </p>
   *
   * <p>
   * The input parameter for this method is the base filename of the test set
   * file (without extension). This is converted to the path
   * {@code resources/testsets/<parameter>.csv}.
   * </p>
   *
   * @see TestSetLine
   *
   * @param testSet
   *          The base name of the test set file
   * @return The {@link Stream} of lines from the file as {@link TestSetLine}
   *         objects
   * @throws IOException
   *           If the file cannot be read
   */
  protected Stream<TestSetLine> getTestSet(String testSet) throws IOException {

    File testSetFile = context
      .getResource("classpath:resources/testsets/" + testSet + ".csv")
      .getFile();
    List<TestSetLine> lines = new ArrayList<TestSetLine>();

    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(testSetFile));
      // Skip the first line
      in.readLine();

      int lineNumber = 1;
      String line;
      while ((line = in.readLine()) != null) {
        lineNumber++;
        lines.add(new TestSetLine(lineNumber, line.split(",")));
      }
    } catch (Exception e) {
      throw e;
    } finally {
      try {
        if (null != in) {
          in.close();
        }
      } catch (Exception e) {
        // Meh
      }
    }

    return lines.stream();
  }
}
