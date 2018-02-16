package junit.uk.ac.exeter.QuinCe.web.system.ResourceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

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
public class LocalTestResourceManager extends ResourceManager {

	/**
	 * No failure file
	 */
	protected static final int FAILURE_FILE_NONE = 0;
	
	/**
	 * Extraction QC Routines file failure
	 */
	protected static final int FAILURE_FILE_EXTRACT_ROUTINES_CONFIG = 1;
	
	/**
	 * Main QC Routines file failure
	 */
	protected static final int FAILURE_FILE_QC_ROUTINES_CONFIG = 2;
	
	/**
	 * Column config file failure
	 */
	protected static final int FAILURE_FILE_COLUMNS_CONFIG = 3;
	
	/**
	 * Export config file failure
	 */
	protected static final int FAILURE_FILE_EXPORT_CONFIG = 4;
	
	/**
	 * Sensors config file failure
	 */
	protected static final int FAILURE_FILE_SENSOR_CONFIG = 5;
	
	/**
	 * Run types file failure
	 */
	protected static final int FAILURE_FILE_RUN_TYPES = 6;
	
	/**
	 * The configuration file on which to fail
	 */
	private int failureFile = FAILURE_FILE_NONE;
	
	/**
	 * The dummy test database name
	 */
	protected static final String DATABASE_NAME = "test.database";
	
	/**
	 * The configuration directory
	 */
	protected static final String CONFIG_PATH_ROOT = "./WebApp/junit/resources/configuration/";

	/**
	 * The configuration file name
	 */
	protected static final String CONFIG_PATH = "DUMMY_PROPERTIES_PATH";
	
	public LocalTestResourceManager() {
		initFileStore();
	}
	
	public LocalTestResourceManager(int failureFile) {
		initFileStore();
		this.failureFile = failureFile;
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
		
		DataSource dataSource = Mockito.mock(DataSource.class);
		InitialContext context = Mockito.mock(InitialContext.class);
		
		Mockito.doReturn(dataSource).when(context).lookup(DATABASE_NAME);
		return context;
	}
	
	/**
	 * Create a fixed set of configuration properties
	 */
	@Override
	protected Properties loadConfiguration(String filePath) throws FileNotFoundException, IOException {
		
		Properties config = new Properties();
		
		config.setProperty("app.urlstub", "http://localhost:8080/QuinCe");
		config.setProperty("email.starttls", "false");
		config.setProperty("email.ssl", "false");
		config.setProperty("email.hostname", "smtp.test");
		config.setProperty("email.port", "25");
		config.setProperty("email.fromname", "QuinCe");
		config.setProperty("email.fromaddress", "quince@uib.no");
		config.setProperty("filestore", getFileStorePath());
		config.setProperty("extract_routines.configfile", CONFIG_PATH_ROOT + "extract_routines_config.csv");
		config.setProperty("qc_routines.configfile", CONFIG_PATH_ROOT + "qc_routines_config.csv");
		config.setProperty("columns.configfile", CONFIG_PATH_ROOT + "eqpco2_column_config.csv");
		config.setProperty("export.configfile", CONFIG_PATH_ROOT + "export_config.csv");
		config.setProperty("sensors.configfile", CONFIG_PATH_ROOT + "sensor_config.csv");
		config.setProperty("runtypes.configfile", CONFIG_PATH_ROOT + "run_types_config.csv");
		config.setProperty("map.max_points", "1000");
		
		String failureFileId = null;
		
		switch (failureFile) {
		case FAILURE_FILE_EXTRACT_ROUTINES_CONFIG: {
			failureFileId = "extract_routines.configfile";
			break;
		}
		case FAILURE_FILE_QC_ROUTINES_CONFIG: {
			failureFileId = "qc_routines.configfile";
			break;
		}
		case FAILURE_FILE_COLUMNS_CONFIG: {
			failureFileId = "columns.configfile";
			break;
		}
		case FAILURE_FILE_EXPORT_CONFIG: {
			failureFileId = "export.configfile";
			break;
		}
		case FAILURE_FILE_SENSOR_CONFIG: {
			failureFileId = "sensors.configfile";
			break;
		}
		case FAILURE_FILE_RUN_TYPES: {
			failureFileId = "runtypes.configfile";
			break;
		}
		}
		
		if (null != failureFileId) {
			config.setProperty(failureFileId, "missing_file.csv");
		}
		
		return config;
	}
	
	/**
	 * Build the path to the file store location used for testing
	 * @return The file store path
	 */
	private String getFileStorePath() {
		return System.getProperty("java.io.tmpdir") + "/FILE_STORE";
	}
}
