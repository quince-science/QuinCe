package uk.ac.exeter.QuinCe.web.files;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.database.files.FileDataInterrogator;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class DataScreenBean extends BaseManagedBean {

	static {
		FORM_NAME = "dataScreen";
	}

	public static final String CURRENT_FILE_SESSION_ATTRIBUTE = "currentFile";
	
	public static final String PAGE_START = "data_screen";
	
	public static final String PAGE_END = "file_list";
	
	private static final String POPUP_PLOT = "plot";
	
	private static final String POPUP_MAP = "map";
	
	private long fileId;
	
	private FileInfo fileDetails;
	
	private String leftPlotColumns = null;
	
	private String leftPlotData = null;
	
	private String rightPlotColumns = null;
	
	private String rightPlotData = null;
	
	private int co2Type = RunType.RUN_TYPE_WATER;
	
	private List<String> optionalFlags = null;
	
	private String tableMode = "basic";
	
	private String tableJsonData = null;
	
	private int tableDataDraw;
	
	private int tableDataStart;
	
	private int tableDataLength;
	
	private int recordCount = -1;
	
	Instrument instrument;
	
	/**
	 * Required basic constructor. All the actual construction
	 * is done in start().
	 */
	public DataScreenBean() {
		// Do nothing
	}

	public String start() throws Exception {
		clearData();
		loadFileDetails();
		return PAGE_START;
	}
	
	public String end() {
		clearData();
		return PAGE_END;
	}
	
	private void clearData() {
		fileDetails = null;
	}
	
	public long getFileId() {
		return fileId;
	}
	
	public void setFileId(long fileId) {
		this.fileId = fileId;
	}
	
	public FileInfo getFileDetails() {
		return fileDetails;
	}
	
	public String getLeftPlotColumns() {
		return leftPlotColumns;
	}
	
	public void setLeftPlotColumns(String leftPlotColumns) {
		this.leftPlotColumns = leftPlotColumns;
	}
	
	public String getLeftPlotData() {
		return leftPlotData;
	}
	
	public void setLeftPlotData(String leftPlotData) {
		this.leftPlotData = leftPlotData;
	}
	
	public String getRightPlotColumns() {
		return rightPlotColumns;
	}
	
	public void setRightPlotColumns(String rightPlotColumns) {
		this.rightPlotColumns = rightPlotColumns;
	}
	
	public String getRightPlotData() {
		return rightPlotData;
	}
	
	public void setRightPlotData(String rightPlotData) {
		this.rightPlotData = rightPlotData;
	}
	
	public int getCo2Type() {
		return co2Type;
	}
	
	public void setCo2Type(int co2Type) {
		this.co2Type = co2Type;
	}
	
	public List<String> getOptionalFlags() {
		return optionalFlags;
	}
	
	public void setOptionalFlags(List<String> optionalFlags) {
		this.optionalFlags = optionalFlags;
		
		// Reset the record count, so it is retrieved from the database again.
		recordCount = -1;
	}
	
	public String getTableMode() {
		return tableMode;
	}
	
	public void setTableMode(String tableMode) {
		this.tableMode = tableMode;
	}
	
	public String getTableJsonData() {
		return tableJsonData;
	}
	
	public void setTableJsonData(String tableJsonData) {
		this.tableJsonData = tableJsonData;
	}
	
	public int getTableDataDraw() {
		return tableDataDraw;
	}
	
	public void setTableDataDraw(int tableDataDraw) {
		this.tableDataDraw = tableDataDraw;
	}
	
	public int getTableDataStart() {
		return tableDataStart;
	}
	
	public void setTableDataStart(int tableDataStart) {
		this.tableDataStart = tableDataStart;
	}
	
	public int getTableDataLength() {
		return tableDataLength;
	}
	
	public void setTableDataLength(int tableDataLength) {
		this.tableDataLength = tableDataLength;
	}
	
	public int getRecordCount() {
		return recordCount;
	}
	
	public void setRecordCount(int recordCount) {
		this.recordCount = recordCount;
	}
	
	private void loadFileDetails() throws MissingParamException, DatabaseException, ResourceException, RecordNotFoundException {
		fileDetails = DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), fileId);
		instrument = InstrumentDB.getInstrumentByFileId(ServletUtils.getDBDataSource(), fileId);
	}
	
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

		// End of second column/Start of 3rd column
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

		// End of column 3
		output.append("</td></table>");
		
		// End of outer table
		output.append("</tr></table>");
		
		return output.toString();
	}
	
	private String makePlotCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_PLOT, group, field, label);
	}
	
	private String makeMapCheckbox(String group, String field, String label) {
		return makeCheckbox(POPUP_MAP, group, field, label);
	}
	
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
	
	public void generateLeftPlotData() {
		List<String> columns = StringUtils.delimitedToList(leftPlotColumns);
		setLeftPlotData(getPlotData(columns)); 
	}

	public void generateRightPlotData() {
		List<String> columns = StringUtils.delimitedToList(rightPlotColumns);
		setRightPlotData(getPlotData(columns)); 
	}
	
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
			
			output = FileDataInterrogator.getJsonData(dataSource, fileId, co2Type, submittedColumnList, getIncludeFlags(), 1, 0, false);
		} catch (Exception e) {
			e.printStackTrace();
			output = "***ERROR: " + e.getMessage();
		}
		
		return output;
	}

	public void generateTableData() {

		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			
			if (recordCount < 0) {
				setRecordCount(FileDataInterrogator.getRecordCount(dataSource, fileId, co2Type, getIncludeFlags()));
			}
			
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
			
			setTableJsonData(FileDataInterrogator.getJsonData(dataSource, fileId, co2Type, columns, getIncludeFlags(), tableDataStart, tableDataLength, true));
		
		} catch (Exception e) {
			e.printStackTrace();
			setTableJsonData("***ERROR: " + e.getMessage());
		}
	}
	
	public String getTableHeadings() {

		StringBuffer output = new StringBuffer('[');
		
		output.append("['Date/Time', 'Row', 'Longitude', 'Latitude', ");
			
		if (instrument.getIntakeTempCount() == 1) {
			output.append("'Intake Temp', ");
		} else {
			if (instrument.hasIntakeTemp1()) {
				output.append("'Intake Temp: ");
				output.append(instrument.getIntakeTempName1());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp2()) {
				output.append("'Intake Temp: ");
				output.append(instrument.getIntakeTempName2());
				output.append("', ");
			}
			if (instrument.hasIntakeTemp3()) {
				output.append("'Intake Temp: ");
				output.append(instrument.getIntakeTempName3());
				output.append("', ");
			}
			
			output.append("'Intake Temp: Mean', ");
		}
			
		if (instrument.getSalinityCount() == 1) {
			output.append("'Salinity', ");
		} else {
			if (instrument.hasSalinity1()) {
				output.append("'Salinity: ");
				output.append(instrument.getSalinityName1());
				output.append("', ");
			}
			if (instrument.hasSalinity2()) {
				output.append("'Salinity: ");
				output.append(instrument.getSalinityName2());
				output.append("', ");
			}
			if (instrument.hasSalinity3()) {
				output.append("'Salinity: ");
				output.append(instrument.getSalinityName3());
				output.append("', ");
			}
			
			output.append("'Salinity: Mean', ");
		}

		if (instrument.getEqtCount() == 1) {
			output.append("'Equil. Temp', ");
		} else {
			if (instrument.hasEqt1()) {
				output.append("'Equil. Temp: ");
				output.append(instrument.getEqtName1());
				output.append("', ");
			}
			if (instrument.hasEqt2()) {
				output.append("'Equil. Temp: ");
				output.append(instrument.getEqtName2());
				output.append("', ");
			}
			if (instrument.hasEqt3()) {
				output.append("'Equil. Temp: ");
				output.append(instrument.getEqtName3());
				output.append("', ");
			}
			
			output.append("'Equil. Temp: Mean', ");
		}

		if (instrument.getEqpCount() == 1) {
			output.append("'Equil. Pressure', ");
		} else {
			if (instrument.hasEqp1()) {
				output.append("'Equil. Pressure: ");
				output.append(instrument.getEqpName1());
				output.append("', ");
			}
			if (instrument.hasEqp2()) {
				output.append("'Equil. Pressure: ");
				output.append(instrument.getEqpName2());
				output.append("', ");
			}
			if (instrument.hasEqp3()) {
				output.append("'Equil. Pressure: ");
				output.append(instrument.getEqpName3());
				output.append("', ");
			}
			
			output.append("'Equil. Pressure: Mean', ");
		}

		output.append("'Atmos. Pressure', 'Moisture (Measured)', 'Moisture (True)', 'pH₂O', 'CO₂ Measured', 'CO₂ Dried', 'CO₂ Calibrated', 'pCO₂ TE Dry', "
				+ "'pCO₂ TE Wet', 'fCO₂ TE', 'fCO₂ Final', 'QC Flag', 'QC Message', 'WOCE Flag', 'WOCE Message']");
		
		return output.toString();
	}

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
	
	public Instrument getInstrument() {
		return instrument;
	}
}
