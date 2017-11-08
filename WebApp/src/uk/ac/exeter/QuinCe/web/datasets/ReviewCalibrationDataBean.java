package uk.ac.exeter.QuinCe.web.datasets;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
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
	 * The name for selecting all gas standards
	 */
	private static final String ALL_NAME = "All Gas Standards";
	
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
	 * Tree model for gas standards selection
	 */
	private TreeNode gasStandardsTree;
	
	/**
	 * The selected gas standard
	 */
	private TreeNode selectedStandard;
	
	/**
	 * The labels for the plot
	 */
	private String plotLabels;
	
	/**
	 * Data for the plot
	 */
	private String plotCsv;
		
	/**
	 * Initialise the required data for the bean
	 */
	public void init() {
		try {
			dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
			buildGasStandardsTree();
			getCalibrationData();
			
			// TODO This should be built dynamically in CalibrationDataDB
			plotLabels = "Date/Time;CO2";
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
	 * Get the tree containing the gas standards
	 * @return The gas standards tree
	 */
	public TreeNode getGasStandardsTree() {
		return gasStandardsTree;
	}
	
	/**
	 * Finish the calibration data validation
	 * @return Navigation to the data set list
	 */
	public String finish() {
		return NAV_DATASET_LIST;
	}
	
	/**
	 * Build the Gas Standards tree
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 */
	private void buildGasStandardsTree() throws MissingParamException, DatabaseException {
		gasStandardsTree = new DefaultTreeNode("Gas Standards Tree Root", null);
		TreeNode gasStandardsNode = new DefaultTreeNode(ALL_NAME, gasStandardsTree);
		gasStandardsNode.setExpanded(true);
		gasStandardsTree.getChildren().add(gasStandardsNode);
		for (String runType : InstrumentDB.getRunTypes(getDataSource(), dataset.getInstrumentId(), "STD")) {
			gasStandardsNode.getChildren().add(new DefaultTreeNode(runType, gasStandardsNode));
		}
		
		// Select the All Standards node
		selectedStandard = gasStandardsNode;
		gasStandardsNode.setSelected(true);
	}
	
	/**
	 * Get the selected gas standards
	 * @return The selected gas standards
	 */
	public TreeNode getSelectedStandard() {
		return selectedStandard;
	}
	
	/**
	 * Set the selected gas standards
	 * @param selectedStandard The selected standards
	 */
	public void setSelectedStandard(TreeNode selectedStandard) {
		this.selectedStandard = selectedStandard;
	}
	
	/**
	 * Get the CSV data for the plot
	 * @return The plot data
	 */
	public String getPlotCsv() {
		return plotCsv;
	}
	
	/**
	 * Dummy method for setting plot csv; does nothing
	 */
	public void setPlotCsv(String plotCsv) {
		// Dummy
	}
	
	/**
	 * Load the calibration data for the page
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
 	 */
	public void getCalibrationData() throws MissingParamException, DatabaseException {
		String standardName = (String) selectedStandard.getData();
		if (standardName.equals(ALL_NAME)) {
			standardName = null;
		}
		
		plotCsv = CalibrationDataDB.getCalibrationJson(getDataSource(), datasetId, standardName);
	}
	
	/**
	 * Get the labels for the plot
	 * @return The plot labels
	 */
	public String getPlotLabels() {
		return plotLabels;
	}
	
	/**
	 * Set the labels for the plot (dummy)
	 * @dummy The supplied labels. Ignored.
	 */
	public void setPlotLabels(String dummy) {
		// Do nothing
	}
}
