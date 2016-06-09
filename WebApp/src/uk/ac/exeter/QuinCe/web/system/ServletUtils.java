package uk.ac.exeter.QuinCe.web.system;

import java.util.Properties;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.DatabaseException;

/**
 * Utility functions for beans, filters, servlets etc.
 * @author Steve Jones
 *
 */
public class ServletUtils {

	public static ResourceManager getResourceManager() throws ResourceException {
		try {
			return ResourceManager.getInstance((ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext());
		} catch (Exception e) {
			throw new ResourceException("Error while retrieving database data source", e);
		}
	}
	
	/**
	 * Retrieve a database connection from the pool
	 * @return A database connection
	 * @throws DatabaseException If there is an error connecting to the database
	 */
	public static DataSource getDBDataSource() throws ResourceException {
		try {
			return getResourceManager().getDBDataSource();
		} catch (Exception e) {
			throw new ResourceException("Error while retrieving database data source", e);
		}
	}
	
	/**
	 * Retrieve the application configuration
	 * @return The application configuration
	 * @throws ResourceException If the retrieval fails
	 */
	public static Properties getAppConfig() throws ResourceException {
		try {
			return getResourceManager().getConfig();
		} catch (Exception e) {
			throw new ResourceException("Error while retrieving application configuration", e);
		}
	}
	
}
