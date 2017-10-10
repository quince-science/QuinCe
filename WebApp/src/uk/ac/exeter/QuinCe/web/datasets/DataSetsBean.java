package uk.ac.exeter.QuinCe.web.datasets;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

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
	 * Initialise/Reset the bean
	 */
	@PostConstruct
	public void initialise() {
		initialiseInstruments();
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
}
