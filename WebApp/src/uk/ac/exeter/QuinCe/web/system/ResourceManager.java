package uk.ac.exeter.QuinCe.web.system;

import java.io.File;
import java.io.FileInputStream;
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
import uk.ac.exeter.QCRoutines.config.RoutinesConfig;
import uk.ac.exeter.QuinCe.data.Export.ExportConfig;
import uk.ac.exeter.QuinCe.data.Export.ExportException;
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
	
	/**
	 * The singleton instance of the resource manage
	 */
	private static ResourceManager instance = null;
	
	@Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        String databaseName = servletContext.getInitParameter("database.name");
        try {
        	dbDataSource = (DataSource) new InitialContext().lookup(databaseName);
        } catch (NamingException e) {
            throw new RuntimeException("Config failed: datasource not found", e);
        }
        
        try {
            String filePath = (String) servletContext.getInitParameter("configuration.path");
            configuration = new Properties();
            configuration.load(new FileInputStream(new File(filePath)));
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

       	// Initialise the Extraction Check Routines configuration
       	try {
       		RoutinesConfig.init(INITIAL_CHECK_ROUTINES_CONFIG, configuration.getProperty("extract_routines.configfile"));
       	} catch (ConfigException e) {
       		throw new RuntimeException("Could not initialise Extraction Check Routines", e);
       	}
       	
       	// Initialise the QC Routines configuration
       	try {
       		RoutinesConfig.init(QC_ROUTINES_CONFIG, configuration.getProperty("qc_routines.configfile"));
       	} catch (ConfigException e) {
       		throw new RuntimeException("Could not initialise QC Routines", e);
       	}
       	
       	// Initialise the file export options configuration
       	try {
       		ExportConfig.init(configuration.getProperty("export.configfile"));
       	} catch (ExportException e) {
       		throw new RuntimeException("Could not initialise export configuration", e);
       	}
       	
       	instance = this;
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
    
    /**
     * Retrieve the singleton instance of the Resource Manager
     * @return
     */
    public static ResourceManager getInstance() {
        return instance;
    }
}
