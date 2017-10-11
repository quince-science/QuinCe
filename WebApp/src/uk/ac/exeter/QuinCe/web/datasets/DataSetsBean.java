package uk.ac.exeter.QuinCe.web.datasets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for handling the creation and management of data sets
 * @author Steve Jones
 */
@ManagedBean
@SessionScoped
public class DataSetsBean extends BaseManagedBean {

	/**
	 * Navigation string for the New Dataset page
	 */
	private static final String NAV_NEW_DATASET = "new_dataset";
	
	/**
	 * Navigation string for the datasets list
	 */
	private static final String NAV_DATASET_LIST = "dataset_list";
	
	/**
	 * The data sets for the current instrument
	 */
	private List<DataSet> dataSets;
	
	/**
	 * The data files for the instrument in JSON format
	 */
	private String filesJson;
	
	/**
	 * The file definitions for the current instrument in JSON format for the timeline
	 */
	private String fileDefinitionsJson;

	/**
	 * The data sets for the current instrument in JSON format
	 */
	private String dataSetsJson;
		
	/**
	 * Initialise/Reset the bean
	 */
	@PostConstruct
	public void initialise() {
		try {
			initialiseInstruments();
			if (null == currentFullInstrument) {
				currentFullInstrument = InstrumentDB.getInstrument(getDataSource(), getCurrentInstrument(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
			}
			loadDataSets();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start the dataset definition procedure
	 * @return The navigation to the dataset definition page
	 */
	public String startNewDataset() {
		initialise();
		filesJson = null;
		fileDefinitionsJson = null;

		return NAV_NEW_DATASET;
	}
	
	/**
	 * Navigate to the datasets list
	 * @return The navigation string
	 */
	public String goToList() {
		return NAV_DATASET_LIST;
	}
	
	/**
	 * Get the data sets for the current instrument
	 * @return The data sets
	 */
	public List<DataSet> getDataSets() {
		return dataSets;
	}
	
	/**
	 * Load the list of data sets for the instrument from the database
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	private void loadDataSets() throws MissingParamException, DatabaseException {
		dataSets = DataSetDB.getDataSets(getDataSource(), currentFullInstrument.getDatabaseId());
		buildDataSetsJson();
	}
	
	/**
	 * Get the data set details for the instrument in JSON format
	 * for the timeline
	 * @return The data sets JSON
	 */
	public String getDataSetsJson() {
		if (null == dataSetsJson) {
			buildDataSetsJson();
		}
		
		return dataSetsJson;
	}
	
	/**
	 * Get the data files for the current instrument in JSON format
	 * for the timeline
	 * @return The data files JSON
	 */
	public String getFilesJson() {
		if (null == filesJson) {
			buildFilesJson();
		}
		
		return filesJson;
	}
	
	/**
	 * Get the file definitions for the current instrument in JSON format
	 * for the timeline
	 * @return The file definitions JSON
	 */
	public String getFileDefinitionsJson() {
		if (null == filesJson) {
			buildFilesJson();
		}
		
		return fileDefinitionsJson;
	}
	
	/**
	 * Build the timeline JSON string for the data sets
	 */
	private void buildDataSetsJson() {
		if (null == dataSets) {
			initialise();
		}
		
		dataSetsJson = "[]";
	}
	
	/**
	 * Build the timeline JSON string for the data files
	 */
	private void buildFilesJson() {
		try {
			if (null == currentFullInstrument) {
				currentFullInstrument = InstrumentDB.getInstrument(getDataSource(), getCurrentInstrument(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
			}

			// Make the list of file definitions
			Map<String, Integer> definitionIds = new HashMap<String, Integer>();
			
			
			StringBuilder fdJson = new StringBuilder();
			
			fdJson.append('[');

			for (int i = 0; i < currentFullInstrument.getFileDefinitions().size(); i++) {
				FileDefinition definition = currentFullInstrument.getFileDefinitions().get(i);
				
				// Store the definition number for use when building the files JSON below
				definitionIds.put(definition.getFileDescription(), i);
				
				fdJson.append('{');
				fdJson.append("\"id\":");
				fdJson.append(i);
				fdJson.append(", \"content\":\"");
				fdJson.append(definition.getFileDescription());
				fdJson.append("\", \"order\":");
				fdJson.append(i);
				fdJson.append('}');
				
				if (i < currentFullInstrument.getFileDefinitions().size() - 1) {
					fdJson.append(',');
				}
			}
			
			
			fdJson.append(']');
			
			fileDefinitionsJson = fdJson.toString();
			
			
			// Now the actual files
			List<DataFile> dataFiles = DataFileDB.getUserFiles(getDataSource(), getAppConfig(), getUser(), currentFullInstrument.getDatabaseId());

			StringBuilder fJson = new StringBuilder();
			fJson.append('[');
			
			for (int i = 0; i < dataFiles.size(); i++) {
				DataFile file = dataFiles.get(i);
				
				fJson.append('{');
				fJson.append("\"type\":\"range\", \"group\":");
				fJson.append(definitionIds.get(file.getFileDefinition().getFileDescription()));
				fJson.append(",\"start\":\"");
				fJson.append(DateTimeUtils.toJsonDate(file.getStartDate()));
				fJson.append("\",\"end\":\"");
				fJson.append(DateTimeUtils.toJsonDate(file.getEndDate()));
				fJson.append("\",\"content\":\"");
				fJson.append(file.getFilename());
				fJson.append("\",\"title\":\"");
				fJson.append(file.getFilename());
				fJson.append("\"}");
				
				if (i < dataFiles.size() - 1) {
					fJson.append(',');
				}
			}
			
			fJson.append(']');
			
			filesJson = fJson.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
