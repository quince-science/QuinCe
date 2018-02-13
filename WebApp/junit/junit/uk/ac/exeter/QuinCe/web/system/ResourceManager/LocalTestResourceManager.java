package junit.uk.ac.exeter.QuinCe.web.system.ResourceManager;

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
		config.setProperty("filestore", "RESOURCES/FILE_STORE");
		config.setProperty("extract_routines.configfile", CONFIG_PATH_ROOT + "extract_routines_config.csv");
		config.setProperty("qc_routines.configfile", CONFIG_PATH_ROOT + "qc_routines_config.csv");
		config.setProperty("columns.configfile", CONFIG_PATH_ROOT + "eqpco2_column_config.csv");
		config.setProperty("export.configfile", CONFIG_PATH_ROOT + "export_config.csv");
		config.setProperty("sensors.configfile", CONFIG_PATH_ROOT + "sensor_config.csv");
		config.setProperty("runtypes.configfile", CONFIG_PATH_ROOT + "run_types_config.csv");
		config.setProperty("map.max_points", "1000");
		
		return config;
	}
}
