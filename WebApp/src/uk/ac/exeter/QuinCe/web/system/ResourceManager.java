package uk.ac.exeter.QuinCe.web.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.config.ColumnConfig;
import uk.ac.exeter.QCRoutines.config.ConfigException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.QCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Export.ExportConfig;
import uk.ac.exeter.QuinCe.data.Export.ExportException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.jobs.InvalidThreadCountException;
import uk.ac.exeter.QuinCe.jobs.JobThreadPool;

/**
 * Utility class for handling resources required by the web application.
 * The Resource Manager is a singleton object, which is created during
 * initialisation of the web application
 * @author Steve Jones
 *
 */
public class ResourceManager implements ServletContextListener {

  /**
   * The name of the QC Routines configuration for routines run when files
   * are first uploaded
   */
  public static final String INITIAL_CHECK_ROUTINES_CONFIG = "InitialCheck";

  /**
   * The name of the QC routines configuration for routines run during
   * full automatic QC (after data reduction)
   */
  public static final String QC_ROUTINES_CONFIG = "QC";

  /**
   * The application's data source
   */
  private DataSource dbDataSource;

  /**
   * The application's configuration
   */
  private Properties configuration;

  /**
   * The column configuration used by the QC routines
   */
  private ColumnConfig columnConfig;

  private SensorsConfiguration sensorsConfiguration;

  private RunTypeCategoryConfiguration runTypeCategoryConfiguration;

  private QCRoutinesConfiguration qcRoutinesConfiguration;

  /**
   * The singleton instance of the resource manage
   */
  private static ResourceManager instance = null;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    ServletContext servletContext = event.getServletContext();
    String databaseName = servletContext.getInitParameter("database.name");
    try {
      dbDataSource = (DataSource) createInitialContext().lookup(databaseName);
    } catch (NamingException e) {
      throw new RuntimeException("Config failed: datasource not found", e);
    }

    try {
      String filePath = servletContext.getInitParameter("configuration.path");
      configuration = loadConfiguration(filePath);
    } catch (IOException e) {
      throw new RuntimeException("Config failed: could not read config file", e);
    }

    // Initialise the job thread pool
    try {
      JobThreadPool.initialise(3);
    } catch (InvalidThreadCountException e) {
      // Do nothing for now
    }

    // Initialise the column config
    try {
      ColumnConfig.init(configuration.getProperty("columns.configfile"));
      columnConfig = ColumnConfig.getInstance();
    } catch (ConfigException e) {
      throw new RuntimeException("Could not initialise data column configuration", e);
    }

    // Initialise the sensors configuration
    try {
      sensorsConfiguration = new SensorsConfiguration(getDBDataSource());
    } catch (Exception e) {
      throw new RuntimeException("Could not load sensors configuration", e);
    }

    // Initialise run type category configuration
    try {
      runTypeCategoryConfiguration = new RunTypeCategoryConfiguration(new File(configuration.getProperty("runtypes.configfile")));
    } catch (RunTypeCategoryException e) {
      throw new RuntimeException("Could not load sensors configuration", e);
    }

    // Initialise the QC Routines configuration
    try {
      qcRoutinesConfiguration = new QCRoutinesConfiguration(sensorsConfiguration, configuration.getProperty("qc_routines.configfile"));
    } catch (Exception e) {
      throw new RuntimeException("Could not initialise QC Routines", e);
    }

    // Initialise the file export options configuration
    try {
      ExportConfig.init(this, configuration.getProperty("export.configfile"));
    } catch (ExportException e) {
      throw new RuntimeException("Could not initialise export configuration", e);
    }

    instance = this;
  }

  protected InitialContext createInitialContext() throws NamingException {
    return new InitialContext();
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    // NOOP.
  }

  /**
   * Retrieve the application's data source
   * @return The data source
   */
  public DataSource getDBDataSource() {
    return dbDataSource;
  }

  /**
   * Retrieve the application configuration
   * @return The application configuration
   */
  public Properties getConfig() {
    return configuration;
  }

  /**
   * Retrieve the column configuration for the QC routines
   * @return The column configuration
   */
  public ColumnConfig getColumnConfig() {
    return columnConfig;
  }

  public SensorsConfiguration getSensorsConfiguration() {
    return sensorsConfiguration;
  }

  public RunTypeCategoryConfiguration getRunTypeCategoryConfiguration() {
    return runTypeCategoryConfiguration;
  }

  public QCRoutinesConfiguration getQCRoutinesConfiguration() {
    return qcRoutinesConfiguration;
  }

  /**
   * Load the application configuration
   * @param filePath The path to the configuration file
   * @return The configuration Properties object
   * @throws FileNotFoundException If the file does not exist
   * @throws IOException If the file cannot be read
   */
  protected Properties loadConfiguration(String filePath) throws FileNotFoundException, IOException {
    Properties result = new Properties();
    result.load(new FileInputStream(new File(filePath)));
    return result;
  }

  /**
   * Retrieve the singleton instance of the Resource Manager
   * @return The resource manager
   */
  public static ResourceManager getInstance() {
    return instance;
  }

  /**
   * Destroy the current ResourceManager instance
   */
  public static void destroy() {
    instance = null;
  }
}
