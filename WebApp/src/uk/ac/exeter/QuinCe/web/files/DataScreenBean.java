package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.FileInfo;
import uk.ac.exeter.QuinCe.data.Instrument;
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
	
	/**
	 * Required basic constructor. All the actual construction
	 * is done in init().
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
	
	private void loadFileDetails() throws MissingParamException, DatabaseException, ResourceException, RecordNotFoundException {
		fileDetails = DataFileDB.getFileDetails(ServletUtils.getDBDataSource(), fileId);
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
		
		String output = null;
		
		List<String> columns = StringUtils.delimitedToList(leftPlotColumns);
		
		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			Instrument instrument = InstrumentDB.getInstrument(dataSource, fileDetails.getInstrumentId());
			
			output = FileDataInterrogator.getCSVData(ServletUtils.getDBDataSource(), fileId, instrument, columns);
		} catch (Exception e) {
			output = "***ERROR: " + e.getMessage();
		}
		
		setLeftPlotData(output);
	}

	public void generateRightPlotData() {
		String output = null;
		
		List<String> columns = StringUtils.delimitedToList(rightPlotColumns);
		
		try {
			DataSource dataSource = ServletUtils.getDBDataSource();
			Instrument instrument = InstrumentDB.getInstrument(dataSource, fileDetails.getInstrumentId());
			
			output = FileDataInterrogator.getCSVData(ServletUtils.getDBDataSource(), fileId, instrument, columns);
		} catch (Exception e) {
			output = "***ERROR: " + e.getMessage();
		}
		
		setRightPlotData(output);
	}
}
