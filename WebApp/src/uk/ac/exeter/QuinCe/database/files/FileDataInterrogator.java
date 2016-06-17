package uk.ac.exeter.QuinCe.database.files;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Extracts data from the three file database tables in a single API
 * @author Steve Jones
 *
 */
public class FileDataInterrogator {
	
	private static final String COLUMN_ORIGINAL_FILE = "original";
	
	private static Map<String, String> COLUMN_MAPPINGS = null;
	
	private static final String GET_COLUMN_DATA_QUERY = "SELECT %%COLUMNS%% FROM raw_data INNER JOIN data_reduction"
					+ " ON raw_data.data_file_id = data_reduction.data_file_id AND raw_data.row = data_reduction.row"
					+ " WHERE raw_data.data_file_id = ? ORDER BY %%FIRST_COLUMN%% ASC";
	
	static {
		// Map input names from the web front end to database column names
		COLUMN_MAPPINGS = new HashMap<String, String>();
		COLUMN_MAPPINGS.put("dateTime", "raw_data.date_time");
		COLUMN_MAPPINGS.put("longitude", "raw_data.longitude");
		COLUMN_MAPPINGS.put("latitude", "raw_data.latitude");
		COLUMN_MAPPINGS.put("intakeTempMean", "data_reduction.mean_intake_temp");
		COLUMN_MAPPINGS.put("intakeTemp1", "raw_data.intake_temp_1");
		COLUMN_MAPPINGS.put("intakeTemp2", "raw_data.intake_temp_2");
		COLUMN_MAPPINGS.put("intakeTemp3", "raw_data.intake_temp_3");
		COLUMN_MAPPINGS.put("salinityMean", "data_reduction.mean_salinity");
		COLUMN_MAPPINGS.put("salinityTemp1", "raw_data.salinity_1");
		COLUMN_MAPPINGS.put("salinityTemp2", "raw_data.salinity_2");
		COLUMN_MAPPINGS.put("salinityTemp3", "raw_data.salinity_3");
		COLUMN_MAPPINGS.put("eqtMean", "data_reduction.mean_eqt");
		COLUMN_MAPPINGS.put("eqt1", "raw_data.eqt_1");
		COLUMN_MAPPINGS.put("eqt2", "raw_data.eqt_2");
		COLUMN_MAPPINGS.put("eqt3", "raw_data.eqt_3");
		COLUMN_MAPPINGS.put("eqpMean", "data_reduction.mean_eqp");
		COLUMN_MAPPINGS.put("eqp1", "raw_data.eqp_1");
		COLUMN_MAPPINGS.put("eqp2", "raw_data.eqp_2");
		COLUMN_MAPPINGS.put("eqp3", "raw_data.eqp_3");
		COLUMN_MAPPINGS.put("atmosPressure", "data_reduction.atmospheric_pressure");
		COLUMN_MAPPINGS.put("moistureMeasured", "raw_data.moisture");
		COLUMN_MAPPINGS.put("moistureTrue", "data_reduction.true_moisture");
		COLUMN_MAPPINGS.put("pH2O", "data_reduction.ph2O");
		COLUMN_MAPPINGS.put("co2Measured", "raw_data.co2");
		COLUMN_MAPPINGS.put("co2Dried", "data_reduction.dried_co2");
		COLUMN_MAPPINGS.put("co2Calibrated", "data_reduction.calibrated_co2");
		COLUMN_MAPPINGS.put("pCO2TEDry", "data_reduction.pco2_te_dry");
		COLUMN_MAPPINGS.put("pCO2TEWet", "data_reduction.pco2_te_wet");
		COLUMN_MAPPINGS.put("fCO2TE", "data_reduction.fco2_te");
		COLUMN_MAPPINGS.put("fCO2Final", "data_reduction.fco2");
	}
	
	public static String getCSVData(DataSource dataSource, long fileId, Instrument instrument, List<String> columns, boolean includeBad) throws MissingParamException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(columns, "columns");

		String output = null;
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;

		try {
			conn = dataSource.getConnection();
			
			StringBuffer columnList = new StringBuffer();
			for (int i = 0; i < columns.size(); i++) {
				columnList.append(COLUMN_MAPPINGS.get(columns.get(i)));
				if (i < columns.size() - 1) {
					columnList.append(',');
				}
			}

			String queryString = GET_COLUMN_DATA_QUERY.replaceAll("%%COLUMNS%%", columnList.toString());
			queryString = queryString.replaceAll("%%FIRST_COLUMN%%", COLUMN_MAPPINGS.get(columns.get(0)));
			
			stmt = conn.prepareStatement(queryString);
			stmt.setLong(1, fileId);
			
			records = stmt.executeQuery();
			
			StringBuffer outputBuffer = new StringBuffer();
			for (int i = 1; i <= columns.size(); i++) {
				outputBuffer.append(getColumnHeading(columns.get(i - 1), instrument));
				if (i < columns.size()) {
					outputBuffer.append(',');
				}
			}
			outputBuffer.append('\n');
			
			
			while (records.next()) {
				for (int i = 1; i <= columns.size(); i++) {
					if (columns.get(i - 1).equals("dateTime")) {
						outputBuffer.append(DateTimeUtils.formatDateTime(records.getTimestamp(i)));
					} else {
						outputBuffer.append(records.getString(i));
					}
					
					if (i < columns.size()) {
						outputBuffer.append(',');
					}
				}
				outputBuffer.append('\n');
			}
			
			output = outputBuffer.toString();
			
		} catch (SQLException e) {
			e.printStackTrace();
			output = "***ERROR - " + e.getMessage();
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return output;
	}
	
	private static String getColumnHeading(String columnName, Instrument instrument) {
		String result;
		
		switch (columnName) {
		case "dateTime": {
			result = "Date";
			break;
		}
		case "longitude": {
			result = "Longitude";
			break;
		}
		case "latitude": {
			result = "Latitude";
			break;
		}
		case "intakeTempMean": {
			if (instrument.getIntakeTempCount() == 1) {
				result = "Intake Temperature";
			} else {
				result = "Intake Temperature (mean)";
			}
			break;
		}
		case "intakeTemp1": {
			result = "Intake Temperature - " + instrument.getIntakeTempName1();
			break;
		}
		case "intakeTemp2": {
			result = "Intake Temperature - " + instrument.getIntakeTempName2();
			break;
		}
		case "intakeTemp3": {
			result = "Intake Temperature - " + instrument.getIntakeTempName3();
			break;
		}
		case "salinityMean": {
			if (instrument.getSalinityCount() == 1) {
				result = "Salinity";
			} else {
				result = "Salinity (mean)";
			}
			break;
		}
		case "salinity1": {
			result = "Salinity - " + instrument.getSalinityName1();
			break;
		}
		case "salinity2": {
			result = "Salinity - " + instrument.getSalinityName2();
			break;
		}
		case "salinity3": {
			result = "Salinity - " + instrument.getSalinityName3();
			break;
		}
		case "eqtMean": {
			if (instrument.getEqtCount() == 1) {
				result = "Equilibrator Temperature";
			} else {
				result = "Equilibrator Temperature (mean)";
			}
			break;
		}
		case "eqt1": {
			result = "Equilibrator Temperature - " + instrument.getEqtName1();
			break;
		}
		case "eqt2": {
			result = "Equilibrator Temperature - " + instrument.getEqtName2();
			break;
		}
		case "eqt3": {
			result = "Equilibrator Temperature - " + instrument.getEqtName3();
			break;
		}
		case "eqpMean": {
			if (instrument.getEqpCount() == 1) {
				result = "Equilibrator Pressure";
			} else {
				result = "Equilibrator Pressure (mean)";
			}
			break;
		}
		case "eqp1": {
			result = "Equilibrator Pressure - " + instrument.getEqpName1();
			break;
		}
		case "eqp2": {
			result = "Equilibrator Pressure - " + instrument.getEqpName2();
			break;
		}
		case "eqp3": {
			result = "Equilibrator Pressure - " + instrument.getEqpName3();
			break;
		}
		case "atmosPressure":
		{
			result = "Atmospheric Pressure";
			break;
		}
		case "moistureMeasured": {
			result = "Moisture (measured)";
			break;
		}
		case "moistureTrue": {
			result = "Moisture (true)";
			break;
		}
		case "pH2O": {
			result = "pH₂O";
			break;
		}
		case "co2Measured": {
			result = "CO₂ (measured)";
			break;
		}
		case "co2Dried": {
			result = "CO₂ (dried)";
			break;
		}
		case "co2Calibrated": {
			result = "CO₂ (calibrated)";
			break;
		}
		case "pCO2TEDry": {
			result = "pCO₂ TE Dry";
			break;
		}
		case "pCO2TEWet": {
			result = "pCO₂ TE Wet";
			break;
		}
		case "fCO2TE": {
			result = "fCO₂ TE";
			break;
		}
		case "fCO2Final": {
			result = "fCO₂";
			break;
		}
		default: {
			result = "Unknown";
		}
		}
		
		return result;
	}
	
	public static String validateColumnNames(List<String> columnNames) {
		
		String invalidColumn = null;
		
		Set<String> knownColumnNames = COLUMN_MAPPINGS.keySet();
		
		for (String columnName : columnNames) {
			if (!columnName.equals(COLUMN_ORIGINAL_FILE) && !knownColumnNames.contains(columnName)) {
				invalidColumn = columnName;
				break;
			}
		}
		
		return invalidColumn;
	}
}
