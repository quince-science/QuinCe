package uk.ac.exeter.QuinCe.web.datasets;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
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
	}
}
