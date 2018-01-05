package uk.ac.exeter.QuinCe.web.datasets;

import java.util.List;

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
import uk.ac.exeter.QuinCe.web.PlotPageBean;

/**
 * Bean for handling review of calibration data
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ReviewCalibrationDataBean extends PlotPageBean {

	/**
	 * The name for selecting all external standards
	 */
	private static final String ALL_NAME = "All External Standards";
	
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
	 * Tree model for external standards selection
	 */
	private TreeNode externalStandardsTree;
	
	/**
	 * The selected external standard
	 */
	private TreeNode selectedStandard;
	
	/**
	 * Indicates whether or not the selected calibrations should be used
	 */
	private boolean useCalibrations = true;
	
	/**
	 * The message attached to calibrations that should not be used
	 */
	private String useCalibrationsMessage = null;
	
	/**
	 * Initialise the required data for the bean
	 */
	public void init() {
		try {
			dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
			buildExternalStandardsTree();
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
	 * Get the tree containing the external standards
	 * @return The external standards tree
	 */
	public TreeNode getExternalStandardsTree() {
		return externalStandardsTree;
	}
	
	/**
	 * Finish the calibration data validation
	 * @return Navigation to the data set list
	 */
	public String finish() {
		return NAV_DATASET_LIST;
	}
	
	/**
	 * Build the External Standards tree
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 */
	private void buildExternalStandardsTree() throws MissingParamException, DatabaseException {
		externalStandardsTree = new DefaultTreeNode("External Standards Tree Root", null);
		TreeNode externalStandardsNode = new DefaultTreeNode(ALL_NAME, externalStandardsTree);
		externalStandardsNode.setExpanded(true);
		externalStandardsTree.getChildren().add(externalStandardsNode);
		for (String runType : InstrumentDB.getRunTypes(getDataSource(), dataset.getInstrumentId(), "EXT")) {
			externalStandardsNode.getChildren().add(new DefaultTreeNode(runType, externalStandardsNode));
		}
		
		// Select the All Standards node
		selectedStandard = externalStandardsNode;
		externalStandardsNode.setSelected(true);
	}
	
	/**
	 * Get the selected external standards
	 * @return The selected external standards
	 */
	public TreeNode getSelectedStandard() {
		return selectedStandard;
	}
	
	/**
	 * Set the selected external standards
	 * @param selectedStandard The selected standards
	 */
	public void setSelectedStandard(TreeNode selectedStandard) {
		this.selectedStandard = selectedStandard;
	}
	
	@Override
	public String loadPlotData(int plotIndex) throws Exception {
		String result = null;
		
		if (plotIndex == 1) {
			result = CalibrationDataDB.getJsonPlotData(getDataSource(), datasetId, getStandardSearchString());
		}
		
		return result;
	}
	
	@Override
	protected List<Long> loadRowIds() throws Exception {
		return CalibrationDataDB.getCalibrationRowIds(getDataSource(), datasetId, getStandardSearchString());
	}
	
	/**
	 * Get the search string for the selected standard
	 * @return The search string
	 */
	private String getStandardSearchString() {
		String standardName = (String) selectedStandard.getData();
		if (standardName.equals(ALL_NAME)) {
			standardName = null;
		}
		
		return standardName;
	}
	
	@Override
	protected String buildTableHeadings() {
		StringBuilder headings = new StringBuilder();
		
		headings.append('[');
		headings.append("\"ID\",");
		headings.append("\"Date\",");
		headings.append("\"Run Type\",");
		headings.append("\"CO2\",");
		headings.append("\"Use?\",");
		headings.append("\"Use Message\"");
		headings.append(']');
		
		return headings.toString();
	}
	
	@Override
	protected String buildSelectableRows() throws Exception {
		List<Long> ids = CalibrationDataDB.getCalibrationRowIds(getDataSource(), getDatasetId(), getStandardSearchString());
		StringBuilder result = new StringBuilder();
		result.append('[');
		for (int i = 0; i < ids.size(); i++) {
			result.append(ids.get(i));
			if (i < ids.size() - 1) {
				result.append(',');
			}
		}
		result.append(']');
		
		return result.toString();
	}

	@Override
	protected String getScreenNavigation() {
		return NAV_PLOT;
	}

	@Override
	protected String buildPlotLabels(int plotIndex) {
		String result = null;
		
		if (plotIndex == 1) {
			// TODO This should be built dynamically in CalibrationDataDB
			return "[\"Date\",\"ID\",\"CO2\"]";
		}

		return result;
	}

	@Override
	protected String loadTableData(int start, int length) throws Exception {
		return CalibrationDataDB.getJsonTableData(getDataSource(), datasetId, getStandardSearchString(), start, length);
	}
	
	/**
	 * Get the flag indicating whether the selected calibrations are to be used
	 * @return The use calibrations flag
	 */
	public boolean getUseCalibrations() {
		return useCalibrations;
	}
	
	/**
	 * Set the flag indicating whether the selected calibrations are to be used
	 * @param useCalibrations The use calibrations flag
	 */
	public void setUseCalibrations(boolean useCalibrations) {
		this.useCalibrations = useCalibrations;
	}
	
	/**
	 * Get the message that will be attached to calibrations which aren't being used
	 * @return The message for unused calibrations
	 */
	public String getUseCalibrationsMessage() {
		return useCalibrationsMessage;
	}
	
	/**
	 * Set the message that will be attached to calibrations which aren't being used
	 * @param useCalibrationsMessage The message for unused calibrations
	 */
	public void setUseCalibrationsMessage(String useCalibrationsMessage) {
		this.useCalibrationsMessage = useCalibrationsMessage;
	}

	/**
	 * Set the usage status of the selected rows
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
	 */
	public void setCalibrationUse() throws MissingParamException, DatabaseException {
		
		try {
			CalibrationDataDB.setCalibrationUse(getDataSource(), getSelectedRowsList(), useCalibrations, useCalibrationsMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
