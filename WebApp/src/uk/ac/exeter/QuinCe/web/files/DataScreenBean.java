package uk.ac.exeter.QuinCe.web.files;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.QC.QCDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.database.files.FileDataInterrogator;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Managed bean to handle data for the main QC screen
 * 
 * @author Steve Jones
 *
 */
public class DataScreenBean extends BaseManagedBean {

	static {
		FORM_NAME = "dataScreen";
	}

	public static final String CURRENT_FILE_SESSION_ATTRIBUTE = "currentFile";
	
	/**
	 * Navigation result to display the QC screen
	 */
	public static final String PAGE_START = "data_screen";
	
	/**
	 * Navigation result to display the file list
	 */
	public static final String PAGE_END = "file_list";
	
	/**
	 * Indicator for HTML controls for the plot configuration popup
	 */
	private static final String POPUP_PLOT = "plot";
	
	/**
	 * Indicator for HTML controls for the map configuration popup
	 */
	private static final String POPUP_MAP = "map";
	
	/**
	 * The database ID of the data file being QCed
	 */
	private long fileId;
	
	/**
	 * The details of the file being edited
	 */
	private FileInfo fileDetails = null;
	
	/**
	 * The data columns being used in the left plot.
	 * 
	 * <p>
	 *   The columns are stored as a semi-colon separated list. The first
	 *   column is the X axis, and all subsequent columns will be displayed on the Y axis.
	 *   The Javascript on the user interface will be responsible for ensuring that
	 *   the columns are set appropriately; the bean will not perform any checks.
	 * </p>
	 */
	private String leftPlotColumns = null;
	
	/**
	 * The data for the left plot.
	 * 
	 * <p>
	 *   The data is stored as a JSON string, which will be parsed by the Javascript on
	 *   the user interface. The data is retrieved by the {@link #getPlotData(List)} method.
	 * </p>
	 */
	private String leftPlotData = null;
	
	/**
	 * The data columns being used in the right plot.
	 * 
	 * <p>
	 *   The columns are stored as a semi-colon separated list. The first
	 *   column is the X axis, and all subsequent columns will be displayed on the Y axis.
	 *   The Javascript on the user interface will be responsible for ensuring that
	 *   the columns are set appropriately; the bean will not perform any checks.
	 * </p>
	 */
	private String rightPlotColumns = null;
	
	/**
	 * The data for the right plot.
	 *
	 * <p>
	 *   The data is stored as a JSON string, which will be parsed by the Javascript on
	 *   the user interface. The data is retrieved by the {@link #getPlotData(List)} method.
	 * </p>
	 */
	private String rightPlotData = null;
	
	/**
	 * The type of CO<sub>2</sub> measurements being viewed.
	 * Can only be one of {@link RunType#RUN_TYPE_WATER} or {@link RunType#RUN_TYPE_ATMOSPHERIC}.  
	 */
	private int co2Type = RunType.RUN_TYPE_WATER;
	
	/**
	 * Indicates that records with the specified flags will also be included in the plot/map.
	 * 
	 * <p>
	 * 	By default, only records with the flags:
	 * </p>
	 * <ul>
	 *   <li>{@link Flag#VALUE_GOOD}</li>
	 *   <li>{@link Flag#VALUE_ASSUMED_GOOD}</li>
	 *   <li>{@link Flag#VALUE_QUESTIONABLE}</li>
	 *   <li>{@link Flag#VALUE_NEEDED}</li>
	 * </ul>
	 * 
	 * <p>
	 *   are displayed in plots and maps. Records with
	 *   other flags can be included if they are added
	 *   to this field.
	 * </p>
	 */
	private List<String> optionalFlags = null;
	
	/**
	 * The current table mode, which indicates which columns are to be displayed
	 */
	private String tableMode = "basic";
	
	/**
	 * The data for the table.
	 * 
	 * <p>
	 *   The table data is loaded dynamically from the server in chunks,
	 *   so the entire data set does not need to be loaded in one go. This
	 *   field will only contain the data for the currently visible chunk.
	 * </p>
	 */
	private String tableData = null;
	
	/**
	 * The list of table rows that are currently selected.
	 * 
	 * <p>
	 *   The list of rows is stored as a comma-separated list of row numbers from the data file.
	 * </p>
	 */
	private String selectedRows = null;
	
	/**
	 * The WOCE comment entered by the user. This will be applied to the selected records
	 * when {@link #applyWoceFlag()} is called.
	 */
	private String woceComment = null;
	
	/**
	 * The WOCE flag selected by the user. This will be applied to the selected records
	 * when {@link #applyWoceFlag()} is called.
	 */
	private int woceFlag = Flag.VALUE_NEEDED;
	
	/**
	 * The instrument that recorded the current data file
	 */
	Instrument instrument;
	
	/**
	 * Required basic constructor. This does nothing: all the actual construction
	 * is done in {@link #start()}.
	 */
	public DataScreenBean() {
		// Do nothing
	}

	/**
	 * Initialises the bean with the details of the selected data file.
	 * Any data from previous data files is removed first.
	 * @return The navigation to the QC screen
	 * @throws Exception If any errors occur
	 */
	public String start() throws Exception {
		clearData();
		loadFileDetails();
		return PAGE_START;
	}
	
	/**
	 * Clears all file data when QC for a data file is finished.
	 * @return The navigation to the data file list
	 */
	public String end() {
		clearData();
		return PAGE_END;
	}
	
	/**
	 * Clears all data regarding the current data file from the bean
	 */
	private void clearData() {
		fileDetails = null;
		leftPlotColumns = null;
		leftPlotData = null;
		rightPlotColumns = null;
		rightPlotData = null;
		optionalFlags = null;
		tableData = null;
	}
	
	/**
	 * Returns the database ID of the current data file
	 * @return The database ID of the data file
	 */
	public long getFileId() {
		return fileId;
	}
	
	/**
	 * Set the database ID of the current data file
	 * @param fileId The database ID of the data file
	 */
	public void setFileId(long fileId) {
		this.fileId = fileId;
	}
	
	/**
	 * Get the file details of the current data file
	 * @return The file details
	 */
	public FileInfo getFileDetails() {
		return fileDetails;
	}
	
	/**
	 * Get the columns to be displayed in the left plot. See {@link #leftPlotColumns}.
	 * @return The columns for the left plot
	 */
	public String getLeftPlotColumns() {
		return leftPlotColumns;
	}
	
	/**
	 * Set the columns to be displayed in the left plot. See {@link #leftPlotColumns}.
	 * @param leftPlotColumns The columns for the left plot
	 */
	public void setLeftPlotColumns(String leftPlotColumns) {
		this.leftPlotColumns = leftPlotColumns;
	}
	
	/**
	 * Get the data for the left plot. See {@link #leftPlotData}.
	 * @return The data for the left plot
	 */
	public String getLeftPlotData() {
		return leftPlotData;
	}
	
	/**
	 * Set the data for the left plot. See {@link #leftPlotData}.
	 * @param leftPlotData The data for the left plot
	 */
	public void setLeftPlotData(String leftPlotData) {
		this.leftPlotData = leftPlotData;
	}
	
	/**
	 * Get the columns to be displayed in the right plot. See {@link #rightPlotColumns}.
	 * @return The columns for the right plot
	 */
	public String getRightPlotColumns() {
		return rightPlotColumns;
	}
	
	/**
	 * Set the columns to be displayed in the right plot. See {@link #rightPlotColumns}.
	 * @param rightPlotColumns The columns for the right plot
	 */
	public void setRightPlotColumns(String rightPlotColumns) {
		this.rightPlotColumns = rightPlotColumns;
	}
	
	/**
	 * Get the data for the right plot. See {@link #rightPlotData}.
	 * @return The data for the right plot
	 */
	public String getRightPlotData() {
		return rightPlotData;
	}
	
	/**
	 * Set the data for the right plot. See {@link #rightPlotData}.
	 * @param rightPlotData The data for the right plot
	 */
	public void setRightPlotData(String rightPlotData) {
		this.rightPlotData = rightPlotData;
	}
	
	/**
	 * Get the type of CO<sub>2</sub> measurement being displayed.
	 * Will be one of {@link RunType#RUN_TYPE_WATER} or {@link RunType#RUN_TYPE_ATMOSPHERIC}.
	 * @return The type of measurement being displayed
	 */
	public int getCo2Type() {
		return co2Type;
	}
	
	/**
	 * Get the type of CO<sub>2</sub> measurement to be displayed.
	 * Must be one of {@link RunType#RUN_TYPE_WATER} or {@link RunType#RUN_TYPE_ATMOSPHERIC}.
	 * The behaviour of the user interface is undefined if this is set to anything else.
	 * 
	 * @param co2Type The type of measurement
	 */
	public void setCo2Type(int co2Type) {
		this.co2Type = co2Type;
	}
	
	/**
	 * Get the list of flags for display of records in addition to the default set.
	 * See {@link #optionalFlags}.
	 *  
	 * @return The list of additional flags
	 */
	public List<String> getOptionalFlags() {
		return optionalFlags;
	}
	
	/**
	 * Set the list of flags for display of records in addition to the default set.
	 * See {@link #optionalFlags}.
	 * @param optionalFlags The list of additional flags
	 */
	public void setOptionalFlags(List<String> optionalFlags) {
		this.optionalFlags = optionalFlags;
	}
	
	/**
	 * Get the table display mode. This determines which columns are displayed in the table.
	 * @return The table display mode.
	 */
	public String getTableMode() {
		return tableMode;
	}
	
	/**
	 * Set the table display mode. This determines which columns are displayed in the table.
	 * @param tableMode The table display mode
	 */
	public void setTableMode(String tableMode) {
		this.tableMode = tableMode;
	}
	
	/**
	 * Get the currently loaded data for the table. See {@link #tableData}.
	 * @return The table data
	 */
	public String getTableData() {
		return tableData;
	}
	
	/**
	 * Set the currently loaded data for the table. See {@link #tableData}.
	 * @param tableData The table data
	 */
	public void setTableData(String tableData) {
		this.tableData = tableData;
	}
	
	/**
	 * Get the set of selected table rows. See {@link #selectedRows}.
	 * @return The selected rows.
	 */
	public String getSelectedRows() {
		return selectedRows;
	}
	
	/**
	 * Set the selected table rows. See {@link #selectedRows}.
	 * @param selectedRows The selected rows
	 */
	public void setSelectedRows(String selectedRows) {
		this.selectedRows = selectedRows;
	}
	
	/**
	 * Get the WOCE comment entered by the user.
	 * @return The WOCE comment
	 */
	public String getWoceComment() {
		return woceComment;
	}
	
	/**
	 * Record the WOCE comment entered by the user
	 * @param woceComment The WOCE comment
	 */
	public void setWoceComment(String woceComment) {
		this.woceComment = woceComment;
	}
	
	/**
	 * Get the WOCE flag selected by the user
	 * @return The WOCE flag
	 */
	public int getWoceFlag() {
		return woceFlag;
	}
	
	/**
	 * Record the WOCE flag selected by the user
	 * @param woceFlag The WOCE flag
	 */
	public void setWoceFlag(int woceFlag) {
		this.woceFlag = woceFlag;
	}
	
	/**
	 * Load details of the selected data file into the bean.
	 * 
	 * This only loads details used for referencing the data file and its
	 * general details; the actual data for the plots and table will be loaded
	 * dynamically at a later stage.
	 * 
	 * @throws MissingParamException If any parameters to the underlying data retrieval calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws ResourceException If the application resources cannot be accessed
	 * @throws RecordNotFoundException If the selected data file (or any of its related records) cannot be found
	 */
	private void loadFileDetails() throws MissingParamException, DatabaseException, ResourceException, RecordNotFoundException {
		fileDetails = DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), fileId);
		DataFileDB.touchFile(ServletUtils.getDBDataSource(), fileId);
		instrument = InstrumentDB.getInstrumentByFileId(ServletUtils.getDBDataSource(), fileId);
	}
	
	/**
	 * Generate the check boxes to select columns for the data plots.
	 * @return The HTML for the check boxes
	 * @throws MissingParamException If any parameters for underlying data retrieval calls are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any required database records are mising
	 * @throws ResourceException If the application resources cannot be accessed
	 */
	public String getPlotPopupEntries() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		
		Instrument instrument = InstrumentDB.getInstrument(ServletUtils.getDBDataSource(), fileDetails.getInstrumentId());
		
		StringBuffer output = new StringBuffer();
		
		output.append("<table><tr>");
		
		// First column
		output.append("<td><table>");
		
		output.append(makePlotCheckbox("datetime", "dateTime", "Date/Time"));
		output.append(makePlotCheckbox("longitude", "longitude", "Longitude"));
		output.append(makePlotCheckbox("latitude", "latitude", "Latitude"));

		// Intake temperature
		if (instrument.getIntakeTempCount() == 1) {
			output.append(makePlotCheckbox("intakeTemp", "intakeTempMean", "Intake Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Intake Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("intakeTemp", "intakeTempMean", "Mean"));
			
			if (instrument.hasIntakeTemp1()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp1", instrument.getIntakeTempName1()));
			}
			
			if (instrument.hasIntakeTemp2()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp2", instrument.getIntakeTempName2()));
			}
			
			if (instrument.hasIntakeTemp3()) {
				output.append(makePlotCheckbox("intakeTemp", "intakeTemp3", instrument.getIntakeTempName3()));
			}
			
			output.append("</table></td></tr>");
		}

		// Salinity
		if (instrument.getSalinityCount() == 1) {
			output.append(makePlotCheckbox("salinity", "salinityMean", "Salinity"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Salinity:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("salinity", "salinityMean", "Mean"));
			
			if (instrument.hasSalinity1()) {
				output.append(makePlotCheckbox("salinity", "salinity1", instrument.getSalinityName1()));
			}
			
			if (instrument.hasSalinity2()) {
				output.append(makePlotCheckbox("salinity", "salinity2", instrument.getSalinityName2()));
			}
			
			if (instrument.hasSalinity3()) {
				output.append(makePlotCheckbox("salinity", "salinity3", instrument.getSalinityName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// End of first column/start of second
		output.append("</table></td><td><table>");
		
		boolean flowSensor = false;
		
		if (instrument.getAirFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Air Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasAirFlow1()) {
				output.append(makePlotCheckbox("airFlow", "airFlow1", instrument.getAirFlowName1()));
			}
			
			if (instrument.hasAirFlow2()) {
				output.append(makePlotCheckbox("airFlow", "airFlow2", instrument.getAirFlowName2()));
			}
			
			if (instrument.hasAirFlow3()) {
				output.append(makePlotCheckbox("airFlow", "airFlow3", instrument.getAirFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (instrument.getWaterFlowCount() > 0) {
			flowSensor = true;
			
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Water Flow:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			if (instrument.hasWaterFlow1()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow1", instrument.getWaterFlowName1()));
			}
			
			if (instrument.hasWaterFlow2()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow2", instrument.getWaterFlowName2()));
			}
			
			if (instrument.hasWaterFlow3()) {
				output.append(makePlotCheckbox("waterFlow", "waterFlow3", instrument.getWaterFlowName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		if (flowSensor) {
			// End of 2nd column/start of 3rd
			output.append("</table></td><td><table>");
		}

		// Equilibrator temperature
		if (instrument.getEqtCount() == 1) {
			output.append(makePlotCheckbox("eqt", "eqtMean", "Equilibrator Temperature"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Temperature:</td></tr>");
			output.append("<tr><td></td><td><table>");
			
			output.append(makePlotCheckbox("eqt", "eqtMean", "Mean"));
			
			if (instrument.hasEqt1()) {
				output.append(makePlotCheckbox("eqt", "eqt1", instrument.getEqtName1()));
			}
			
			if (instrument.hasEqt2()) {
				output.append(makePlotCheckbox("eqt", "eqt2", instrument.getEqtName2()));
			}
			
			if (instrument.hasEqt3()) {
				output.append(makePlotCheckbox("eqt", "eqt3", instrument.getEqtName3()));
			}
			
			output.append("</table></td></tr>");
		}
		
		// Delta T
		output.append(makePlotCheckbox("deltaT", "deltaT", "Δ Temperature"));

		// Equilibrator Pressure
		if (instrument.getEqpCount() == 1) {
			output.append(makePlotCheckbox("eqp", "eqpMean", "Equilibrator Pressure"));
		} else {
			output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Equilibrator Pressure:</td></tr>");
			output.append("<tr><td></td><td><table>");

			output.append(makePlotCheckbox("eqp", "eqpMean", "Mean"));
			
			if (instrument.hasEqp1()) {
				output.append(makePlotCheckbox("eqp", "eqp1", instrument.getEqpName1()));
			}
			
			if (instrument.hasEqp2()) {
				output.append(makePlotCheckbox("eqp", "eqp2", instrument.getEqpName2()));
			}
			
			if (instrument.hasEqp3()) {
				output.append(makePlotCheckbox("eqp", "eqp3", instrument.getEqpName3()));
			}

			output.append("</table></td></tr>");
		}
		
		// Atmospheric Pressure
		/*
		 * We'll put this in when we get to doing atmospheric stuff.
		 * It needs to specify whether it's measured or from external data
		 * 
		output.append(makePlotCheckbox("atmosPressure", "atmospressure", "Atmospheric Pressure"));
		output.append("</td><td>Atmospheric Pressure</td></tr>");
		*/
		
		// Moisture
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">Moisture:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makePlotCheckbox("moisture", "moistureMeasured", "Measured"));
		output.append(makePlotCheckbox("moisture", "moistureTrue", "True"));
		
		output.append("</table></td></tr>");

		// pH2O
		output.append(makePlotCheckbox("pH2O", "pH2O", "pH<sub>2</sub>O"));

		// End of 3rd column/Start of 4th column
		output.append("</table></td><td><table>");

		// CO2
		output.append("<tr><td colspan=\"2\" class=\"minorHeading\">CO<sub>2</sub>:</td></tr>");
		output.append("<tr><td></td><td><table>");

		output.append(makePlotCheckbox("co2", "co2Measured", "Measured"));

		if (!instrument.getSamplesDried()) {
			output.append(makePlotCheckbox("co2", "co2Dried", "Dried"));
		}

		output.append(makePlotCheckbox("co2", "co2Calibrated", "Calibrated"));
		output.append(makePlotCheckbox("co2", "pCO2TEDry", "pCO<sub>2</sub> TE Dry"));
		output.append(makePlotCheckbox("co2", "pCO2TEWet", "pCO<sub>2</sub> TE Wet"));
		output.append(makePlotCheckbox("co2", "fCO2TE", "fCO<sub>2</sub> TE"));
		output.append(makePlotCheckbox("co2", "fCO2Final", "fCO<sub>2</sub> Final"));

		output.append("</table></td></tr>");

		// End of column 4
		output.append("</td></table>");
		
		// End of outer table
		output.append("</tr></table>");
		
		return output.toString();
	}
	
	/**
	 * Generate the HTML for a checkbox in the plot column selection popup
	 * @param group The group that will contain the checkbox
	 * @param field The name of the column
	 * @param label The label for the checkbox
	 * @return The checkbox HTML
	 */
	private String makePlotCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_PLOT, group, field, label);
	}
	
	/**
	 * Generate the HTML for a checkbox in the map column selection popup
	 * @param group The group that will contain the checkbox
	 * @param field The name of the column
	 * @param label The label for the checkbox
	 * @return The checkbox HTML
	 */
	private String makeMapCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_MAP, group, field, label);
	}
	
	/**
	 * Generate the HTML for a column selection checkbox
	 * @param popupType Either {@link #POPUP_PLOT} or {@link #POPUP_MAP}
	 * @param group The group that will contain the checkbox
	 * @param field The name of the column
	 * @param label The label for the checkbox
	 * @return The checkbox HTML
	 */
	private String makeCheckbox(String popupType, String group, String field, String label) {

		String inputID = popupType + "_" + group + "_" + field;
		
		StringBuffer checkbox = new StringBuffer();
		checkbox.append("<tr><td><input type=\"checkbox\" id=\"");
		checkbox.append(inputID);
		checkbox.append("\" value=\"");
		checkbox.append(field);
		checkbox.append("\"/></td><td><label for=\"");
		checkbox.append(inputID);
		checkbox.append("\">");
		checkbox.append(label);
		checkbox.append("</label></td></tr>");
		
		return checkbox.toString();
	}
	
	/**
	 * Generate the data for the left plot. See {@link #getPlotData(List)}.
	 */
	public void generateLeftPlotData() {
		List<String> columns = StringUtils.delimitedToList(leftPlotColumns);
		setLeftPlotData(getPlotData(columns)); 
	}

	/**
	 * Generate the data for the right plot. See {@link #getPlotData(List)}.
	 */
	public void generateRightPlotData() {
		List<String> columns = StringUtils.delimitedToList(rightPlotColumns);
		setRightPlotData(getPlotData(columns)); 
	}
	
	/**
	 * Retrieve the data for a plot from the database as a JSON string.
	 * @param columns The list of columns for the plot. The first column will be for the X axis, and the subsequent columns will be display on the Y axis.
	 * @return The plot data
	 */
	private String getPlotData(List<String> columns) {
		
		String output;
		
		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			// Add in the row number and flags as the first Y-axis columns. We need it for syncing the graphs and the table
			// The list returned from delimitedToList does not allow inserting, so we have to do it the hard way.
			List<String> submittedColumnList = new ArrayList<String>(columns.size() + 1);
			
			// Add the X axis
			submittedColumnList.add(columns.get(0));
			
			// Now the row number
			submittedColumnList.add("row");
			
			// Add QC and WOCE flags
			submittedColumnList.add("qcFlag");
			submittedColumnList.add("woceFlag");
			
			// And the Y axis columns
			submittedColumnList.addAll(columns.subList(1, columns.size()));
			
			output = FileDataInterrogator.getJsonData(dataSource, fileId, co2Type, submittedColumnList, getIncludeFlags(), 1, 0, true, false);
		} catch (Exception e) {
			e.printStackTrace();
			output = "***ERROR: " + e.getMessage();
		}
		
		return output;
	}

	/**
	 * Retrieve the data for the table from the database as a JSON string.
	 * The data is stored in {@link #tableData}.
	 */
	public void generateTableData() {

		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			List<String> columns = new ArrayList<String>();
			columns.add("dateTime");
			columns.add("row");
			columns.add("longitude");
			columns.add("latitude");
			
			if (instrument.getIntakeTempCount() == 1) {
				columns.add("intakeTempMean");
			} else {
				if (instrument.hasIntakeTemp1()) {
					columns.add("intakeTemp1");
				}
				if (instrument.hasIntakeTemp2()) {
					columns.add("intakeTemp2");
				}
				if (instrument.hasIntakeTemp3()) {
					columns.add("intakeTemp3");
				}
				
				columns.add("intakeTempMean");
			}
			
			if (instrument.getSalinityCount() == 1) {
				columns.add("salinityMean");
			} else {
				if (instrument.hasSalinity1()) {
					columns.add("salinity1");
				}
				if (instrument.hasSalinity2()) {
					columns.add("salinity2");
				}
				if (instrument.hasSalinity3()) {
					columns.add("salinity3");
				}
				
				columns.add("salinityMean");
			}
			
			if (instrument.getEqtCount() == 1) {
				columns.add("eqtMean");
			} else {
				if (instrument.hasEqt1()) {
					columns.add("eqt1");
				}
				if (instrument.hasEqt2()) {
					columns.add("eqt2");
				}
				if (instrument.hasEqt3()) {
					columns.add("eqt3");
				}
				
				columns.add("eqtMean");
			}
			
			columns.add("deltaT");
			
			if (instrument.getEqpCount() == 1) {
				columns.add("eqpMean");
			} else {
				if (instrument.hasEqp1()) {
					columns.add("eqp1");
				}
				if (instrument.hasEqp2()) {
					columns.add("eqp2");
				}
				if (instrument.hasEqp3()) {
					columns.add("eqp3");
				}
				
				columns.add("eqtMean");
			}
			
			columns.add("atmosPressure");
			columns.add("moistureMeasured");
			columns.add("moistureTrue");
			columns.add("pH2O");
			columns.add("co2Measured");
			columns.add("co2Dried");
			columns.add("co2Calibrated");
			columns.add("pCO2TEDry");
			columns.add("pCO2TEWet");
			columns.add("fCO2TE");
			columns.add("fCO2Final");
			columns.add("qcFlag");
			columns.add("qcMessage");
			columns.add("woceFlag");
			columns.add("woceMessage");
			
			setTableData(FileDataInterrogator.getJsonData(dataSource, fileId, co2Type, columns, getIncludeFlags(), 0, 0, true, true));
		} catch (Exception e) {
			e.printStackTrace();
			setTableData("***ERROR: " + e.getMessage());
		}
	}
	
	/**
	 * Retrieve the list of column headings for the data table. The result is a JSON string representing a Javascript array.
	 * @return The list of column headings
	 */
	public String getTableHeadings() {

		StringBuffer output = new StringBuffer('[');
		
		output.append("['Date/Time', 'Row', 'Longitude', 'Latitude', ");
			
		if (instrument.getIntakeTempCount() == 1) {
			output.append("'Intake Temp', ");
		} else {
			if (instrument.hasIntakeTemp1()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName1());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp2()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName2());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp3()) {
				output.append("'Intake Temp:<br/>");
				output.append(instrument.getIntakeTempName3());
				output.append("', ");
			}
			
			output.append("'Intake Temp:<br/>Mean', ");
		}
			
		if (instrument.getSalinityCount() == 1) {
			output.append("'Salinity', ");
		} else {
			if (instrument.hasSalinity1()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName1());
				output.append("', ");
			}
			if (instrument.hasSalinity2()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName2());
				output.append("', ");
			}
			if (instrument.hasSalinity3()) {
				output.append("'Salinity:<br/>");
				output.append(instrument.getSalinityName3());
				output.append("', ");
			}
			
			output.append("'Salinity:<br/>Mean', ");
		}

		if (instrument.getEqtCount() == 1) {
			output.append("'Equil. Temp', ");
		} else {
			if (instrument.hasEqt1()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName1());
				output.append("', ");
			}
			if (instrument.hasEqt2()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName2());
				output.append("', ");
			}
			if (instrument.hasEqt3()) {
				output.append("'Equil. Temp:<br/>");
				output.append(instrument.getEqtName3());
				output.append("', ");
			}
			
			output.append("'Equil. Temp:<br/>Mean', ");
		}
		
		output.append("'Δ Temperature', ");

		if (instrument.getEqpCount() == 1) {
			output.append("'Equil. Pressure', ");
		} else {
			if (instrument.hasEqp1()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName1());
				output.append("', ");
			}
			if (instrument.hasEqp2()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName2());
				output.append("', ");
			}
			if (instrument.hasEqp3()) {
				output.append("'Equil. Pressure:<br/>");
				output.append(instrument.getEqpName3());
				output.append("', ");
			}
			
			output.append("'Equil. Pressure:<br/>Mean', ");
		}

		output.append("'Atmos. Pressure', 'Moisture (Measured)', 'Moisture (True)', 'pH₂O', 'CO₂ Measured', 'CO₂ Dried', 'CO₂ Calibrated', 'pCO₂ TE Dry', "
				+ "'pCO₂ TE Wet', 'fCO₂ TE', 'fCO₂ Final', 'QC Flag', 'QC Message', 'WOCE Flag', 'WOCE Message']");
		
		return output.toString();
	}

	/**
	 * Generate the list of WOCE flags that will be used to select records to be displayed on the data screen.
	 * Includes the default set of flags plus any other set in {@link #optionalFlags}.
	 * @return The list of flags
	 */
	private List<Integer> getIncludeFlags() {
		List<Integer> includeFlags = new ArrayList<Integer>();
		includeFlags.add(Flag.VALUE_GOOD);
		includeFlags.add(Flag.VALUE_ASSUMED_GOOD);
		includeFlags.add(Flag.VALUE_QUESTIONABLE);
		includeFlags.add(Flag.VALUE_NEEDED);
		
		if (null != optionalFlags) {
			for (String optionalFlag : optionalFlags) {
				includeFlags.add(Integer.parseInt(optionalFlag));
			}
		}
		
		return includeFlags;
	}
	
	/**
	 * Retrieve the details of the instrument for the current data file
	 * @return The instrument details
	 */
	public Instrument getInstrument() {
		return instrument;
	}
	
	/**
	 * Apply the automatically generated QC flags to the rows selected in the table
	 */
	public void acceptQCFlags() {
		try {
			QCDB.acceptQCFlags(ServletUtils.getDBDataSource(), fileId, getSelectedRows());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Apply the entered WOCE flag and comment to the rows selected in the table
	 */
	public void applyWoceFlag() {
		try {
			QCDB.setWoceFlags(ServletUtils.getDBDataSource(), fileId, getSelectedRows(), getWoceFlag(), getWoceComment());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
