package uk.ac.exeter.QuinCe.web.datasets;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

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
	 * Initialise the required data for the bean
	 */
	public void init() {
		try {
			dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
			buildGasStandardsTree();
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
		TreeNode gasStandardsNode = new DefaultTreeNode("All Gas Standards", gasStandardsTree);
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
	public TreeNode getSelectedStandards() {
		return selectedStandard;
	}
	
	/**
	 * Set the selected gas standards
	 * @param selectedStandards The selected standards
	 */
	public void setSelectedStandards(TreeNode selectedStandards) {
		this.selectedStandard = selectedStandards;
	}
}
