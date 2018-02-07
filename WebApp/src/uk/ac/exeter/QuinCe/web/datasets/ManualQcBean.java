package uk.ac.exeter.QuinCe.web.datasets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecordFactory;
import uk.ac.exeter.QuinCe.data.Calculation.CommentSet;
import uk.ac.exeter.QuinCe.data.Calculation.CommentSetEntry;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.PlotPageBean;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableList;

/**
 * User QC bean
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ManualQcBean extends PlotPageBean {

	/**
	 * Navigation to the calibration data plot page
	 */
	private static final String NAV_PLOT = "user_qc";
	
	/**
	 * Navigation to the dataset list
	 */
	private static final String NAV_DATASET_LIST = "dataset_list";

	/**
	 * The number of sensor columns
	 */
	private int sensorColumnCount = 0;
	
	/**
	 * The number of calculation columns
	 */
	private int calculationColumnCount = 0;
	
	/**
	 * The column containing the auto QC flag
	 */
	private int autoFlagColumn = 0;
	
	/**
	 * The column containing the manual QC flag
	 */
	private int userFlagColumn = 0;

	/**
	 * The set of comments for the user QC dialog. Stored as a Javascript array of entries, with each entry containing
	 * Comment, Count and Flag value
	 */
	private String userCommentList = "[]";
	
	/**
	 * The worst flag set on the selected rows
	 */
	private Flag worstSelectedFlag = Flag.GOOD;
	
	/**
	 * The user QC flag
	 */
	private int userFlag;
	
	/**
	 * The user QC comment
	 */
	private String userComment;
	
	/**
	 * Initialise the required data for the bean
	 */
	@Override
	public void init() {
		setTableMode("sensors");
	}
	
	@Override
	protected List<Long> loadRowIds() throws Exception {
		return DataSetDataDB.getMeasurementIds(getDataSource(), getDatasetId());
	}

	@Override
	protected String buildTableHeadings() throws Exception {
		List<String> dataHeadings = DataSetDataDB.getDatasetDataColumnNames(getDataSource(), getDataset());
		sensorColumnCount = dataHeadings.size() - 4; // Skip id, date, lat, lon
		List<String> calculationHeadings = CalculationDBFactory.getCalculationDB().getCalculationColumnHeadings();
		calculationColumnCount = calculationHeadings.size();
	
		StringBuilder headings = new StringBuilder();
		
		headings.append('[');

		for (String heading : dataHeadings) {
			headings.append('"');
			headings.append(heading);
			headings.append("\",");
		}
		
		for (int i = 0; i < calculationHeadings.size(); i++) {
			headings.append('"');
			headings.append(calculationHeadings.get(i));
			headings.append('"');
			
			headings.append(',');
		}
		
		headings.append("\"Automatic QC\",\"Automatic QC Message\",\"Manual QC\",\"Manual QC Message\"]");

		// Columns are zero-based, so we don't need to add one to get to the auto flag column
		autoFlagColumn = dataHeadings.size() + calculationHeadings.size();
		userFlagColumn = autoFlagColumn + 2;
		
		return headings.toString();
	}

	@Override
	protected String getScreenNavigation() {
		return NAV_PLOT;
	}

	@Override
	protected String buildSelectableRows() throws Exception {
		List<Long> ids = CalculationDBFactory.getCalculationDB().getSelectableMeasurementIds(getDataSource(), getDatasetId());
		
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
	protected String buildPlotLabels(int plotIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String loadTableData(int start, int length) throws Exception {
		List<DataSetRawDataRecord> datasetData = DataSetDataDB.getMeasurements(getDataSource(), getDataset(), start, length);
		
		List<CalculationRecord> calculationData = new ArrayList<CalculationRecord>(datasetData.size());
		for (DataSetRawDataRecord record : datasetData) {
			CalculationRecord calcRecord = CalculationRecordFactory.makeCalculationRecord(getDatasetId(), record.getId());
			CalculationDBFactory.getCalculationDB().getCalculationValues(getDataSource(), calcRecord);
			calculationData.add(calcRecord);
		}
		
		StringBuilder json = new StringBuilder();
		json.append('[');
		int rowId = start - 1;
		for (int i = 0; i < datasetData.size(); i++) {
			rowId++;
			
			DataSetRawDataRecord dsData = datasetData.get(i);
			CalculationRecord calcData = calculationData.get(i);
			
			json.append('{');
			json.append(StringUtils.makeJsonField("DT_RowId", "row" + rowId, true));
			json.append(',');
			
			int columnIndex = 0;
			json.append(StringUtils.makeJsonField(columnIndex, dsData.getId())); // ID
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, DateTimeUtils.dateToLong(dsData.getDate()))); // Date
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, dsData.getLongitude())); // Longitude
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, dsData.getLatitude())); // Latitude
			json.append(',');

			for (Map.Entry<String, Double> entry : dsData.getSensorValues().entrySet()) {
				columnIndex++;
				Double value = entry.getValue();
				if (null == value) {
					json.append(StringUtils.makeJsonNull(columnIndex));
				} else {
					json.append(StringUtils.makeJsonField(columnIndex, entry.getValue()));
				}
				json.append(',');
			}

			List<String> calcColumns = calcData.getCalculationColumns();
			
			for (int j = 0; j < calcColumns.size(); j++) {
				columnIndex++;
				json.append(StringUtils.makeJsonField(columnIndex, calcData.getNumericValue(calcColumns.get(j))));
				json.append(',');
			}
			
			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getAutoFlag()));
			json.append(',');

			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getAutoQCMessagesString(), true));
			json.append(',');
			
			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getUserFlag()));
			json.append(',');
			
			columnIndex++;
			json.append(StringUtils.makeJsonField(columnIndex, calcData.getUserMessage(), true));
			
			json.append('}');
			if (i < datasetData.size() - 1) {
				json.append(',');
			}
		}
		
		json.append(']');
		
		return json.toString();
	}

	/**
	 * Finish the calibration data validation
	 * @return Navigation to the data set list
	 */
	public String finish() {
		return NAV_DATASET_LIST;
	}

	@Override
	public String getAdditionalTableData() {
		StringBuilder json = new StringBuilder();
		json.append("{\"sensorColumnCount\":");
		json.append(sensorColumnCount);
		json.append(",\"calculationColumnCount\":");
		json.append(calculationColumnCount);
		json.append(",\"flagColumns\":[");
		json.append(autoFlagColumn);
		json.append(',');
		json.append(userFlagColumn);
		json.append("]}");
		
		return json.toString();
	}

	/**
	 * Apply the automatically generated QC flags to the rows selected in the table
	 */
	public void acceptAutoQc() {
		try {
			CalculationDBFactory.getCalculationDB().acceptAutoQc(getDataSource(), getSelectedRowsList());
			dirty = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the set of comments for the WOCE dialog
	 * @return The comments for the WOCE dialog
	 */
	public String getUserCommentList() {
		return userCommentList;
	}

	/**
	 * Dummy: Sets the set of comments for the WOCE dialog
	 * @param userCommentList The comments; ignored
	 */
	public void setUserCommentList(String userCommentList) {
		// Do nothing
	}

	/**
	 * Get the worst flag set on the selected rows
	 * @return The worst flag on the selected rows
	 */
	public int getWorstSelectedFlag() {
		return worstSelectedFlag.getFlagValue();
	}
	
	/**
	 * Dummy: Set the worst selected flag
	 * @param worstSelectedFlag The flag; ignored
	 */
	public void setWorstSelectedFlag(int worstSelectedFlag) {
		// Do nothing
	}

	/**
	 * Generate the list of comments for the WOCE dialog
	 */
	public void generateUserCommentList() {

		worstSelectedFlag = Flag.GOOD;
		
		StringBuilder list = new StringBuilder();
		list.append('[');
		
		try {
			CommentSet comments = CalculationDBFactory.getCalculationDB().getCommentsForRows(getDataSource(), getSelectedRowsList());
			for (CommentSetEntry entry : comments) {
				list.append("[\"");
				list.append(entry.getComment());
				list.append("\",");
				list.append(entry.getFlag().getFlagValue());
				list.append(",");
				list.append(entry.getCount());
				list.append("],");
				
				if (entry.getFlag().moreSignificantThan(worstSelectedFlag)) {
					worstSelectedFlag = entry.getFlag();
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			list.append("[\"Existing comments could not be retrieved\", 4, 1],");
			worstSelectedFlag = Flag.BAD;
		}
		
		// Remove the trailing comma from the last entry
		if (list.charAt(list.length() - 1) == ',') {
			list.deleteCharAt(list.length() - 1);
		}
		list.append(']');
		
		userCommentList = list.toString();
	}

	/**
	 * @return the userComment
	 */
	public String getUserComment() {
		return userComment;
	}

	/**
	 * @param userComment the userComment to set
	 */
	public void setUserComment(String userComment) {
		this.userComment = userComment;
	}

	/**
	 * @return the userFlag
	 */
	public int getUserFlag() {
		return userFlag;
	}

	/**
	 * @param userFlag the userFlag to set
	 */
	public void setUserFlag(int userFlag) {
		this.userFlag = userFlag;
	}

	/**
	 * Apply the entered WOCE flag and comment to the rows selected in the table
	 */
	public void applyManualFlag() {
		try {
			CalculationDBFactory.getCalculationDB().applyManualFlag(getDataSource(), getSelectedRowsList(), userFlag, userComment);
			dirty = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Variable getDefaultPlot1XAxis() {
		return variables.getVariableWithLabel("Date/Time");
	}

	@Override
	protected List<Variable> getDefaultPlot1YAxis() {
		List<Variable> result = new ArrayList<Variable>(1);
		result.add(variables.getVariableWithLabel("Intake Temperature"));
		return result;
	}

	@Override
	protected Variable getDefaultPlot2XAxis() {
		return variables.getVariableWithLabel("Date/Time");
	}

	@Override
	protected List<Variable> getDefaultPlot2YAxis() {
		List<Variable> result = new ArrayList<Variable>(1);
		result.add(variables.getVariableWithLabel("Final fCO2"));
		return result;
	}

	@Override
	protected Variable getDefaultMap1Variable() {
		return variables.getVariableWithLabel("Intake Temperature");
	}

	@Override
	protected Variable getDefaultMap2Variable() {
		return variables.getVariableWithLabel("Final fCO2");
	}

	@Override
	protected void buildVariableList(VariableList variables) throws Exception {
		DataSetDataDB.populateVariableList(getDataSource(), getDataset(), variables);
		CalculationDBFactory.getCalculationDB().populateVariableList(variables);
	}

	@Override
	protected String getData(List<String> fields) throws Exception {
		return CalculationDBFactory.getCalculationDB().getJsonData(getDataSource(), getDataset(), fields);
	}
}