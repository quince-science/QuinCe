package junit.uk.ac.exeter.QuinCe.TestBase;

import java.io.File;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Test version of the ResourceManager, that creates
 * a mock InitialContext when required
 * @author zuj007
 *
 */
public class TestResourceManager extends ResourceManager {

  /**
   * The dummy test database name
   */
  protected static final String DATABASE_NAME = "test.database";

  /**
   * The data source to use with this ResourceManager
   */
  private DataSource dataSource;

  /**
   * The configuration file name
   */
  protected static final String CONFIG_PATH = "./WebApp/junit/resources/configuration/quince.properties";

  public TestResourceManager(DataSource dataSource) {
    this.dataSource = dataSource;
    initFileStore();
  }

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
   * Create a mock InitialContext that returns a mock DataSource
   */
  @Override
  protected InitialContext createInitialContext() throws NamingException {

    InitialContext context = Mockito.mock(InitialContext.class);
    Mockito.doReturn(dataSource).when(context).lookup(DATABASE_NAME);
    return context;
  }

  /**
   * Build the path to the file store location used for testing
   * @return The file store path
   */
  protected String getFileStorePath() {
    return System.getProperty("java.io.tmpdir") + "/FILE_STORE";
  }
}
