package uk.ac.exeter.QuinCe.web.datasets;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for handling review of calibration data
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ReviewCalibrationDataBean extends BaseManagedBean {

	/**
	 * Navigation to the calibration data plot page
	 */
	private static final String NAV_PLOT = "calibration_data_plot";
	
	/**
	 * Navigation to the dataset list
	 */
	private static final String NAV_DATASET_LIST = "dataset_list";
	
	/**
	 * The ID of the data set being processed
	 */
	private long datasetId;
	
	/**
	 * The data set being processed
	 */
	private DataSet dataset;
		
	/**
	 * Initialise the required data for the bean
	 */
	public void init() {
		try {
			dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the dataset ID
	 * @return The dataset ID
	 */
	public long getDatasetId() {
		return datasetId;
	}
	
	/**
	 * Set the dataset ID
	 * @param datasetId The dataset ID
	 */
	public void setDatasetId(long datasetId) {
		this.datasetId = datasetId;
	}
	
	/**
	 * Get the current DataSet object
	 * @return The data set
	 */
	public DataSet getDataset() {
		return dataset;
	}
	
	/**
	 * Start the calibration data review
	 * @return Navigation to the plot page
	 */
	public String start() {
		init();
		return NAV_PLOT;
	}
	
	/**
	 * Finish the calibration data validation
	 * @return
	 */
	public String finish() {
		return NAV_DATASET_LIST;
	}
}
