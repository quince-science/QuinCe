package junit.uk.ac.exeter.QuinCe.web.system.ResourceManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import junit.uk.ac.exeter.QuinCe.TestBase.TestResourceManager;
import uk.ac.exeter.QuinCe.jobs.JobThreadPool;
import uk.ac.exeter.QuinCe.jobs.JobThreadPoolNotInitialisedException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for building the resource manager
 */
@FlywayTest
public class ResourceManagerTest extends BaseTest {

  /**
   * Check that the ResourceManager can be constructed
   */
  @Test
  public void testCreateEmptyResourceManager() {
    ResourceManager emptyResourceManager = new ResourceManager();
    assertNotNull(emptyResourceManager, "Empty resource manager not created");
  }

  /**
   * Check that a fully operational ResourceManager can be initialised
   */
  @Test
  public void testInitResourceManagerSuccessful() {
    ResourceManager resourceManager = new TestResourceManager(getDataSource());
    resourceManager.contextInitialized(servletContextEvent);

    // Check that the instance has been created
    assertNotNull(ResourceManager.getInstance(), "ResourceManager instance not created");

    // Check that the job thread pool has been initialised
    try {
      JobThreadPool.getInstance();
    } catch (JobThreadPoolNotInitialisedException e) {
      fail("Job Thread Pool not initialised by ResourceManager");
    }

    // Check that all the elements that should be accessible from the ResourceManager
    // are available
    assertNotNull(ResourceManager.getInstance().getConfig(), "Application Configuration not available from ResourceManager");
    assertNotNull(ResourceManager.getInstance().getDBDataSource(), "DataSource not available from ResourceManager");
    assertNotNull(ResourceManager.getInstance().getColumnConfig(), "ColumnConfig not available from ResourceManager");
    assertNotNull(ResourceManager.getInstance().getSensorsConfiguration(), "SensorsConfiguration not available from ResourceManager");
    assertNotNull(ResourceManager.getInstance().getRunTypeCategoryConfiguration(), "RunTypeCategoryConfiguration not available from ResourceManager");
  }

  /**
   * Check that bad extraction-time QC routines config is detected
   */
  @Test
  public void testInitResourceManagerBadExtractConfig() {
    assertThrows(RuntimeException.class, () -> {
      ResourceManager badResourceManager =
          new BrokenConfigTestResourceManager(getDataSource(),
              BrokenConfigTestResourceManager.FAILURE_FILE_EXTRACT_ROUTINES_CONFIG);

      badResourceManager.contextInitialized(servletContextEvent);
    });
  }

  /**
   * Check that bad general QC routines config is detected
   */
  @Test
  public void testInitResourceManagerBadQcConfig() {
    assertThrows(RuntimeException.class, () -> {
      ResourceManager badResourceManager =
        new BrokenConfigTestResourceManager(getDataSource(),
            BrokenConfigTestResourceManager.FAILURE_FILE_QC_ROUTINES_CONFIG);

      badResourceManager.contextInitialized(servletContextEvent);
    });
  }

  /**
   * Check that bad general columns config is detected
   */
  @Test
  public void testInitResourceManagerBadColumnConfig() {
    assertThrows(RuntimeException.class, () -> {
      ResourceManager badResourceManager =
        new BrokenConfigTestResourceManager(getDataSource(),
          BrokenConfigTestResourceManager.FAILURE_FILE_COLUMNS_CONFIG);

      badResourceManager.contextInitialized(servletContextEvent);
    });
  }

  /**
   * Check that bad export config is detected
   */
  @Test
  public void testInitResourceManagerBadExportConfig() {
    assertThrows(RuntimeException.class, () -> {
      ResourceManager badResourceManager =
        new BrokenConfigTestResourceManager(getDataSource(),
          BrokenConfigTestResourceManager.FAILURE_FILE_EXPORT_CONFIG);

      badResourceManager.contextInitialized(servletContextEvent);
    });
  }

  /**
   * Check that bad sensor config is detected
   */
  @Test
  public void testInitResourceManagerBadSensorConfig() {
    assertThrows(RuntimeException.class, () -> {
      ResourceManager badResourceManager =
        new BrokenConfigTestResourceManager(getDataSource(),
          BrokenConfigTestResourceManager.FAILURE_FILE_SENSOR_CONFIG);

      badResourceManager.contextInitialized(servletContextEvent);
    });
  }

  /**
   * Check that bad general run types config is detected
   */
  @Test
  public void testInitResourceManagerBadRunTypesConfig() {
    assertThrows(RuntimeException.class, () -> {
      ResourceManager badResourceManager =
        new BrokenConfigTestResourceManager(getDataSource(),
          BrokenConfigTestResourceManager.FAILURE_FILE_RUN_TYPES);

      badResourceManager.contextInitialized(servletContextEvent);
    });
  }
}
