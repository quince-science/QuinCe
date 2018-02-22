package junit.uk.ac.exeter.QuinCe.web.system.ResourceManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.jobs.JobThreadPool;
import uk.ac.exeter.QuinCe.jobs.JobThreadPoolNotInitialisedException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Tests for building the resource manager
 */
public class ResourceManagerTest {

	/**
	 * Servlet Context
	 */
	@Mock protected static ServletContext servletContext;
	
	/**
	 * Servlet Context Event
	 */
	@Mock protected static ServletContextEvent servletContextEvent;
	
	@BeforeClass
	public static void createServletContextEvent() {
		
		// I have read that you shouldn't create mocks of classes that you don't own.
		// But I don't know how to do this any other way, so bollocks to it.
		servletContext = Mockito.mock(ServletContext.class);
		Mockito.doReturn(LocalTestResourceManager.DATABASE_NAME).when(servletContext).getInitParameter("database.name");
		Mockito.doReturn(LocalTestResourceManager.CONFIG_PATH).when(servletContext).getInitParameter("configuration.path");

		servletContextEvent = Mockito.mock(ServletContextEvent.class);
		Mockito.doReturn(servletContext).when(servletContextEvent).getServletContext();
	}

	/**
	 * Check that the ResourceManager can be constructed
	 */
	@Test
	public void testCreateEmptyResourceManager() {
		ResourceManager emptyResourceManager = new ResourceManager();
		assertNotNull("Empty resource manager not created", emptyResourceManager);
	}
	
	/**
	 * Check that a fully operational ResourceManager can be initialised
	 */
	@Test
	public void testInitResourceManagerSuccessful() {
		ResourceManager resourceManager = new LocalTestResourceManager();
		resourceManager.contextInitialized(servletContextEvent);
		
		// Check that the instance has been created
		assertNotNull("ResourceManager instance not created", ResourceManager.getInstance());
		
		// Check that the job thread pool has been initialised
		try {
			JobThreadPool.getInstance();
		} catch (JobThreadPoolNotInitialisedException e) {
			fail("Job Thread Pool not initialised by ResourceManager");
		}
		
		// Check that all the elements that should be accessible from the ResourceManager
		// are available
		assertNotNull("Application Configuration not available from ResourceManager", ResourceManager.getInstance().getConfig());
		assertNotNull("DataSource not available from ResourceManager", ResourceManager.getInstance().getDBDataSource());
		assertNotNull("ColumnConfig not available from ResourceManager", ResourceManager.getInstance().getColumnConfig());
		assertNotNull("SensorsConfiguration not available from ResourceManager", ResourceManager.getInstance().getSensorsConfiguration());
		assertNotNull("RunTypeCategoryConfiguration not available from ResourceManager", ResourceManager.getInstance().getRunTypeCategoryConfiguration());
	}
	
	/**
	 * Check that bad extraction-time QC routines config is detected
	 */
	@Test(expected = RuntimeException.class)
	public void testInitResourceManagerBadExtractConfig() {
		ResourceManager badResourceManager = new BrokenConfigTestResourceManager(BrokenConfigTestResourceManager.FAILURE_FILE_EXTRACT_ROUTINES_CONFIG);
		badResourceManager.contextInitialized(servletContextEvent);
	}
	
	/**
	 * Check that bad general QC routines config is detected
	 */
	@Test(expected = RuntimeException.class)
	public void testInitResourceManagerBadQcConfig() {
		ResourceManager badResourceManager = new BrokenConfigTestResourceManager(BrokenConfigTestResourceManager.FAILURE_FILE_QC_ROUTINES_CONFIG);
		badResourceManager.contextInitialized(servletContextEvent);
	}
	
	/**
	 * Check that bad general columns config is detected
	 */
	@Test(expected = RuntimeException.class)
	public void testInitResourceManagerBadColumnConfig() {
		ResourceManager badResourceManager = new BrokenConfigTestResourceManager(BrokenConfigTestResourceManager.FAILURE_FILE_COLUMNS_CONFIG);
		badResourceManager.contextInitialized(servletContextEvent);
	}
	
	/**
	 * Check that bad export config is detected
	 */
	@Test(expected = RuntimeException.class)
	public void testInitResourceManagerBadExportConfig() {
		ResourceManager badResourceManager = new BrokenConfigTestResourceManager(BrokenConfigTestResourceManager.FAILURE_FILE_EXPORT_CONFIG);
		badResourceManager.contextInitialized(servletContextEvent);
	}
	
	/**
	 * Check that bad sensor config is detected
	 */
	@Test(expected = RuntimeException.class)
	public void testInitResourceManagerBadSensorConfig() {
		ResourceManager badResourceManager = new BrokenConfigTestResourceManager(BrokenConfigTestResourceManager.FAILURE_FILE_SENSOR_CONFIG);
		badResourceManager.contextInitialized(servletContextEvent);
	}
	
	/**
	 * Check that bad general run types config is detected
	 */
	@Test(expected = RuntimeException.class)
	public void testInitResourceManagerBadRunTypesConfig() {
		ResourceManager badResourceManager = new BrokenConfigTestResourceManager(BrokenConfigTestResourceManager.FAILURE_FILE_RUN_TYPES);
		badResourceManager.contextInitialized(servletContextEvent);
	}
}
