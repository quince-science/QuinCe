package junit.uk.ac.exeter.QuinCe.web.system.ResourceManager;

import static org.junit.Assert.assertNotNull;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ResourceManagerTest {

	/**
	 * Servlet Context
	 */
	@Mock private static ServletContext servletContext;
	
	/**
	 * Servlet Context Event
	 */
	@Mock private static ServletContextEvent servletContextEvent;
	
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
		assertNotNull(emptyResourceManager);
	}
	
	/**
	 * Check that a fully operational ResourceManager can be initialised
	 */
	@Test
	public void testInitResourceManagerSuccessful() {
		ResourceManager resourceManager = new LocalTestResourceManager();
		resourceManager.contextInitialized(servletContextEvent);
	}
	
}
