package junit.uk.ac.exeter.QuinCe.TestBase;

import java.io.File;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Files.FileStore;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * A version of the application's central {@link ResourceManager} for use with
 * test classes.
 *
 * <p>
 * This {@link ResourceManager} is customised to use the test environment
 * instead of the main application environment. This includes:
 * </p>
 * <ul>
 * <li>A mock {@link InitialContext} that links to the test H2 database</li>
 * <li>Application properties for the test environment</li>
 * <li>A temporary {@link FileStore} which will be automatically destroyed when
 * the tests are complete</li>
 * </ul>
 *
 * @see ResourceManager
 *
 * @author zuj007
 *
 */
public class TestResourceManager extends ResourceManager {

  /**
   * The name for the link to the H2 test database
   */
  protected static final String DATABASE_NAME = "test.database";

  /**
   * The data source to use with this ResourceManager
   */
  private DataSource dataSource;

  /**
   * The location of the test environment's application configuration file
   */
  protected static final String CONFIG_PATH = "./WebApp/junit/resources/configuration/quince.properties";

  /**
   * Base constructor
   *
   * @param dataSource
   *          A data source for the test H2 database
   */
  public TestResourceManager(DataSource dataSource) {
    this.dataSource = dataSource;
    initFileStore();
  }

  /**
   * Initialises the temporary {@link FileStore}
   */
  private void initFileStore() {
    // Create the file store, and request that it
    // be deleted on shutdown
    File fileStore = new File(getFileStorePath());
    if (fileStore.exists()) {
      fileStore.delete();
    }

    fileStore.mkdirs();
    fileStore.deleteOnExit();
  }

  /**
   * Creates a mock {@link InitialContext} that returns the {@link DataSource}
   * for the test H2 database.
   *
   * @see #TestResourceManager(DataSource)
   */
  @Override
  protected InitialContext createInitialContext() throws NamingException {

    InitialContext context = Mockito.mock(InitialContext.class);
    Mockito.doReturn(dataSource).when(context).lookup(DATABASE_NAME);
    return context;
  }

  /**
   * Build the path to the temporary {@link FileStore} location. This is created
   * in the system's temporary file location (e.g. {@code /tmp} on Unix).
   *
   * @return The file store path
   */
  protected String getFileStorePath() {
    return System.getProperty("java.io.tmpdir") + "/FILE_STORE";
  }
}
