package uk.ac.exeter.QuinCe.TestBase;

import static org.mockito.ArgumentMatchers.anyBoolean;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Stream;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
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

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.User.LoginBean;
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
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "/resources/context/testContext.xml" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
  FlywayTestExecutionListener.class })
public class BaseTest {

  /**
   * A global application context.
   */
  @Autowired
  protected ApplicationContext context;

  /**
   * A mock Servlet Context.
   */
  @Mock
  protected static ServletContext servletContext;

  /**
   * A mock Java Server Faces context.
   */
  @Mock
  protected static FacesContext facesContext;

  /**
   * A mock External Context.
   */
  @Mock
  protected static ExternalContext externalContext;

  /**
   * A mock Servlet Context Event.
   */
  @Mock
  protected static ServletContextEvent servletContextEvent;

  /**
   * Initialises the mock {@link ServletContext} and
   * {@link ServletContextEvent}.
   */
  @BeforeAll
  public static void createServletContextEvent() {

    /*
     * I have read that you shouldn't create mocks of classes that you don't
     * own. But I don't know how to do this any other way, so bollocks to it.
     */
    servletContext = Mockito.mock(ServletContext.class);
    Mockito.doReturn(TestResourceManager.DATABASE_NAME).when(servletContext)
      .getInitParameter("database.name");
    Mockito.doReturn(TestResourceManager.CONFIG_PATH).when(servletContext)
      .getInitParameter("configuration.path");

    facesContext = MockFacesContext.mockFacesContext();

    servletContextEvent = Mockito.mock(ServletContextEvent.class);
    Mockito.doReturn(servletContext).when(servletContextEvent)
      .getServletContext();

    externalContext = Mockito.mock(ExternalContext.class);
    Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);

    TestBaseSession session = new TestBaseSession();
    Mockito.when(externalContext.getSession(anyBoolean())).thenReturn(session);
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
   * Set the specified user as the logged in user in the session.
   *
   * @param userId
   *          The user's ID.
   * @throws Exception
   *           If the user cannot be retrieved from the database.
   */
  public void loginUser(long userId) throws Exception {
    ((HttpSession) externalContext.getSession(false)).setAttribute(
      LoginBean.USER_SESSION_ATTR, UserDB.getUser(getConnection(), userId));
  }

  /**
   * Get a {@link DataSource} linked to the H2 test database defined in the
   * {@link #context}, that can be used to obtain database {@link Connection}s.
   *
   * @return A {@link DataSource}.
   */
  protected DataSource getDataSource() {
    return (DataSource) context.getBean("dataSourceRef");
  }

  /**
   * Get a connection to the H2 test database defined in the {@link #context}.
   *
   * @return A database connection.
   * @throws SQLException
   *           If the connection cannot be retrieved.
   */
  protected Connection getConnection() throws SQLException {
    return getDataSource().getConnection();
  }

  /**
   * Create a {@link Stream} of {@code null} and empty String values.
   *
   * <p>
   * Some tests need to check behaviours with empty {@link String} values. This
   * method provides a {@link Stream} of empty {@link String}s that can be used
   * as input to a {@link ParameterizedTest}, including whitespace and
   * {@code null} values.
   * </p>
   *
   * @return A {@link Stream} of various empty {@link String} values.
   */
  protected static Stream<String> createNullEmptyStrings() {
    return Stream.of(null, "", " ", "  ", "\t", "\n");
  }

  /**
   * Creates a simple {@link Stream} containing the two possible {@code boolean}
   * values.
   *
   * @return A {@link Stream} of boolean values.
   */
  protected static Stream<Boolean> booleans() {
    return Stream.of(true, false);
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

  /**
   * Check that the supplied {@link List} of {@link String}s matches the
   * specified {@link String} values.
   *
   * @param list
   *          The {@link List}.
   * @param strings
   *          The {@link String} values.
   * @return {@code true} if the two sets of values match; {@code false} if they
   *         do not.
   */
  protected boolean listsEqual(List<String> list, String... strings) {
    return listsEqual(list, Arrays.asList(strings));
  }

  /**
   * Determine whether or not two {@link Set}s contain the same elements in the
   * same order.
   *
   * @param set1
   *          The first {@link Set}.
   * @param set2
   *          The second {@link Set}.
   * @return {@code true} if the two sets are equal; {@code false} otherwise.
   */
  protected boolean setsEqual(Set<?> set1, Set<?> set2) {
    boolean result = true;

    if (set1.size() != set2.size()) {
      result = false;
    } else {
      Iterator<?> iter1 = set1.iterator();
      Iterator<?> iter2 = set2.iterator();

      while (iter1.hasNext()) {
        if (!iter1.next().equals(iter2.next())) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Check that two maps contain the same values in the same order.
   *
   * @param map1
   *          The first map.
   * @param map2
   *          The second map.
   * @return {@code true} if the maps contain the same values; {@code false}
   *         otherwise.
   */
  protected boolean mapsEqual(SortedMap<?, ?> map1, SortedMap<?, ?> map2) {
    boolean result = true;

    if (map1.size() != map2.size()) {
      result = false;
    } else {
      result = CollectionUtils.isEqualCollection(map1.keySet(), map2.keySet());
      if (result) {
        result = CollectionUtils.isEqualCollection(map1.values(),
          map2.values());
      }
    }

    return result;
  }

  /**
   * Determine whether or not a list of {@link LocalDateTime} objects is in
   * ascending order.
   *
   * <p>
   * Identical values are allowed.
   * </p>
   *
   * @param list
   *          The list.
   * @return {@code true} if the list is ordered; {@code false} otherwise.
   */
  protected boolean timesOrdered(List<LocalDateTime> list) {
    boolean result = true;

    for (int i = 1; i < list.size(); i++) {
      LocalDateTime prev = list.get(i - 1);
      LocalDateTime current = list.get(i);
      if (prev.isAfter(current)) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Check that two integers have the same sign (both negative, both zero or
   * both positive).
   *
   * @param value1
   *          The first value.
   * @param value2
   *          The second value.
   * @return {@code true} if the two values have the same sign; {@code false}
   *         otherwise.
   */
  protected boolean sameSign(int value1, int value2) {
    boolean result = false;

    if (value1 < 0 && value2 < 0) {
      result = true;
    } else if (value1 == 0 && value2 == 0) {
      result = true;
    } else if (value1 > 0 && value2 > 0) {
      result = true;
    }

    return result;
  }

  /**
   * Check that the supplied {@link Throwable} is not {@code null} and that a
   * call to {@code Throwable#getMessage()} returns the specified
   * {@link String}.
   *
   * @param e
   *          The {@link Throwable} to check.
   * @param message
   *          The expected message.
   * @return {@code true} if the message is correct; {@code false} if it is not.
   */
  protected boolean throwableWithMessage(Throwable e, String message) {
    if (null == e) {
      return false;
    } else {
      return message.equals(e.getMessage());
    }
  }
}
