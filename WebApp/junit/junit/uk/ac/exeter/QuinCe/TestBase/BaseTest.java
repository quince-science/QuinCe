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
 * Useful methods for tests. Extend this class to use them
 * @author Steve Jones
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"/resources/context/testContext.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class BaseTest {

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

  @AfterAll
  public static void destroyResourceManager() {
    ResourceManager.destroy();
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


  /**
   * Create a set of null and empty String values
   * @return
   */
  protected static Stream<String> createNullEmptyStrings() {
    return Stream.of(null, "", " ");
  }

  /**
   * Get the contents of a Test Set file as a stream
   * @param testSet The test set name
   * @return The contents stream
   * @throws IOException If the file cannot be read
   */
  protected Stream<TestSetLine> getTestSet(String testSet) throws IOException {

    File testSetFile = context.getResource("classpath:resources/testsets/" + testSet + ".csv").getFile();
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
