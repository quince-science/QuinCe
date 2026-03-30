package uk.ac.exeter.QuinCe.web.system;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction.DataReductionQCConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards.ExternalStandardsRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AbstractQCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.QCConfiguration;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.QCConfigurationDeserializer;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.QCRoutinesConfiguration;
import uk.ac.exeter.QuinCe.data.Export.ExportConfig;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.jobs.InvalidThreadCountException;
import uk.ac.exeter.QuinCe.jobs.JobThreadPool;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Utility class for handling resources required by the web application. The
 * Resource Manager is a singleton object, which is created during
 * initialisation of the web application
 */
public class ResourceManager implements ServletContextListener {

  /**
   * The name of the QC routines configuration for routines run during full
   * automatic QC (after data reduction)
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

  private SensorsConfiguration sensorsConfiguration;

  private RunTypeCategoryConfiguration runTypeCategoryConfiguration;

  private QCConfiguration qcRoutinesConfiguration;

  private QCConfiguration externalStandardsRoutinesConfiguration;

  private DataReductionQCConfiguration dataReductionQCRoutinesConfiguration;

  /**
   * The singleton instance of the resource manage
   */
  private static ResourceManager instance = null;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    ServletContext servletContext = event.getServletContext();
    String databaseName = servletContext.getInitParameter("database.name");

    Connection conn = null;

    try {
      dbDataSource = (DataSource) createInitialContext().lookup(databaseName);
    } catch (NamingException e) {
      throw new RuntimeException("Config failed: datasource not found", e);
    }

    try {
      conn = dbDataSource.getConnection();
    } catch (SQLException e) {
      throw new RuntimeException("Unable to get database connection", e);
    }

    try {
      String filePath = servletContext.getInitParameter("configuration.path");
      configuration = loadConfiguration(filePath);
    } catch (IOException e) {
      throw new RuntimeException("Config failed: could not read config file",
        e);
    }

    // Initialise the job thread pool
    try {
      JobThreadPool.initialise(1);
    } catch (InvalidThreadCountException e) {
      // Do nothing for now
    }

    // Initialise the sensors configuration
    try {
      sensorsConfiguration = new SensorsConfiguration(getDBDataSource());
    } catch (Exception e) {
      throw new RuntimeException("Could not load sensors configuration", e);
    }

    // Initialise run type category configuration
    try {
      runTypeCategoryConfiguration = new RunTypeCategoryConfiguration(conn,
        sensorsConfiguration);
    } catch (Exception e) {
      throw new RuntimeException("Could not load run type categories", e);
    }

    // Initialise the QC Routines configuration
    try {

      String fileContent = new String(
        Files.readAllBytes(
          Paths.get(configuration.getProperty("qc_routines.configfile"))),
        StandardCharsets.UTF_8);

      Gson gson = new GsonBuilder().registerTypeAdapter(QCConfiguration.class,
        new QCConfigurationDeserializer(sensorsConfiguration,
          QCRoutinesConfiguration.class))
        .create();

      qcRoutinesConfiguration = gson.fromJson(fileContent,
        QCConfiguration.class);
    } catch (Exception e) {
      throw new RuntimeException("Could not initialise QC Routines", e);
    }

    // Initialise the External Standards Routines configuration
    try {
      externalStandardsRoutinesConfiguration = new QCConfiguration();

      Gson gson = new GsonBuilder().registerTypeAdapter(QCConfiguration.class,
        new QCConfigurationDeserializer(sensorsConfiguration,
          ExternalStandardsRoutinesConfiguration.class))
        .create();

      String fileContent = new String(
        Files.readAllBytes(Paths.get(
          configuration.getProperty("externalstandards_routines.configfile"))),
        StandardCharsets.UTF_8);

      externalStandardsRoutinesConfiguration = gson.fromJson(fileContent,
        QCConfiguration.class);

    } catch (Exception e) {
      throw new RuntimeException(
        "Could not initialise External Standards Routines", e);
    }

    // Initialise the Data Reduction QC Routines configuration
    try {
      dataReductionQCRoutinesConfiguration = new DataReductionQCConfiguration(
        sensorsConfiguration,
        configuration.getProperty("data_reduction_qc_routines.configfile"));
    } catch (Exception e) {
      throw new RuntimeException("Could not initialise QC Routines", e);
    }

    // Initialise the file export options configuration
    try {
      ExportConfig.init(conn, sensorsConfiguration,
        configuration.getProperty("export.configfile"));
    } catch (Exception e) {
      throw new RuntimeException("Could not initialise export configuration",
        e);
    }

    DatabaseUtils.closeConnection(conn);

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
   *
   * @return The data source
   */
  public DataSource getDBDataSource() {
    return dbDataSource;
  }

  /**
   * Retrieve the application configuration
   *
   * @return The application configuration
   */
  public Properties getConfig() {
    return configuration;
  }

  public SensorsConfiguration getSensorsConfiguration() {
    return sensorsConfiguration;
  }

  public RunTypeCategoryConfiguration getRunTypeCategoryConfiguration() {
    return runTypeCategoryConfiguration;
  }

  public AbstractQCRoutinesConfiguration getQCRoutinesConfiguration(int basis) {
    return qcRoutinesConfiguration.get(basis);
  }

  public AbstractQCRoutinesConfiguration getExternalStandardsRoutinesConfiguration(
    int basis) {
    return externalStandardsRoutinesConfiguration.get(basis);
  }

  public DataReductionQCConfiguration getDataReductionQCRoutinesConfiguration() {
    return dataReductionQCRoutinesConfiguration;
  }

  /**
   * Load the application configuration
   *
   * @param filePath
   *          The path to the configuration file
   * @return The configuration Properties object
   * @throws FileNotFoundException
   *           If the file does not exist
   * @throws IOException
   *           If the file cannot be read
   */
  protected Properties loadConfiguration(String filePath)
    throws FileNotFoundException, IOException {
    Properties result = new Properties();
    result.load(new InputStreamReader(new FileInputStream(filePath),
      StandardCharsets.UTF_8));
    return result;
  }

  /**
   * Retrieve the singleton instance of the Resource Manager
   *
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
