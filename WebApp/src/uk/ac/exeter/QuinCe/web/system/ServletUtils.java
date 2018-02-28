package uk.ac.exeter.QuinCe.web.system;

import java.util.Properties;

import javax.sql.DataSource;

/**
 * Utility functions for beans, filters, servlets etc.
 * @author Steve Jones
 *
 */
public class ServletUtils {

  /**
   * Get the application's resource mananger
   * @return The resource manager
   * @throws ResourceException If the resource manager cannot be retrieved
   */
  public static ResourceManager getResourceManager() throws ResourceException {
    try {
      return ResourceManager.getInstance();
    } catch (Exception e) {
      throw new ResourceException("Error while retrieving database data source", e);
    }
  }

  /**
   * Retrieve a database connection from the pool
   * @return A database connection
   * @throws ResourceException If there is an error connecting to the database
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
