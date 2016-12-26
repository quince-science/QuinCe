package uk.ac.exeter.QuinCe.database.files;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;
import uk.ac.exeter.QuinCe.data.ExportOption;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.RawDataFile;
import uk.ac.exeter.QuinCe.data.RawDataFileException;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.Calculation.RawDataDB;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Extracts data from the three file database tables in a single API
 * @author Steve Jones
 *
 */
public class FileDataInterrogator {
	
	private static final String COLUMN_ORIGINAL_FILE = "original";
	
	private static final String COLUMN_RECORD_COUNT = "count";
	
	private static Map<String, String> COLUMN_MAPPINGS = null;
	
	private static final String GET_COLUMN_DATA_QUERY = "SELECT %%COLUMNS%% FROM raw_data "
					+ " INNER JOIN data_reduction ON raw_data.data_file_id = data_reduction.data_file_id AND raw_data.row = data_reduction.row"
					+ " INNER JOIN qc ON raw_data.data_file_id = qc.data_file_id AND raw_data.row = qc.row"
					+ " WHERE raw_data.data_file_id = ? AND raw_data.co2_type IN (%%CO2TYPES%%) AND qc.woce_flag IN (%%FLAGS%%)"
					+ " ORDER BY %%ORDER%% ASC";
	
	static {
		// Map input names from the web front end to database column names
		COLUMN_MAPPINGS = new HashMap<String, String>();
		COLUMN_MAPPINGS.put("row", "raw_data.row");
		COLUMN_MAPPINGS.put("dateTime", "raw_data.date_time");
		COLUMN_MAPPINGS.put("longitude", "raw_data.longitude");
		COLUMN_MAPPINGS.put("latitude", "raw_data.latitude");
		COLUMN_MAPPINGS.put("intakeTempMean", "data_reduction.mean_intake_temp");
		COLUMN_MAPPINGS.put("intakeTemp1", "raw_data.intake_temp_1");
		COLUMN_MAPPINGS.put("intakeTemp2", "raw_data.intake_temp_2");
		COLUMN_MAPPINGS.put("intakeTemp3", "raw_data.intake_temp_3");
		COLUMN_MAPPINGS.put("salinityMean", "data_reduction.mean_salinity");
		COLUMN_MAPPINGS.put("salinity1", "raw_data.salinity_1");
		COLUMN_MAPPINGS.put("salinity2", "raw_data.salinity_2");
		COLUMN_MAPPINGS.put("salinity3", "raw_data.salinity_3");
		COLUMN_MAPPINGS.put("air_flow_1", "raw_data.air_flow_1");
		COLUMN_MAPPINGS.put("air_flow_2", "raw_data.air_flow_2");
		COLUMN_MAPPINGS.put("air_flow_3", "raw_data.air_flow_3");
		COLUMN_MAPPINGS.put("water_flow_1", "raw_data.water_flow_1");
		COLUMN_MAPPINGS.put("water_flow_2", "raw_data.water_flow_2");
		COLUMN_MAPPINGS.put("water_flow_3", "raw_data.water_flow_3");
		COLUMN_MAPPINGS.put("eqtMean", "data_reduction.mean_eqt");
		COLUMN_MAPPINGS.put("eqt1", "raw_data.eqt_1");
		COLUMN_MAPPINGS.put("eqt2", "raw_data.eqt_2");
		COLUMN_MAPPINGS.put("eqt3", "raw_data.eqt_3");
		COLUMN_MAPPINGS.put("deltaT", "data_reduction.delta_temperature");
		COLUMN_MAPPINGS.put("eqpMean", "data_reduction.mean_eqp");
		COLUMN_MAPPINGS.put("eqp1", "raw_data.eqp_1");
		COLUMN_MAPPINGS.put("eqp2", "raw_data.eqp_2");
		COLUMN_MAPPINGS.put("eqp3", "raw_data.eqp_3");
		COLUMN_MAPPINGS.put("airFlow1", "raw_data.air_flow_1");
		COLUMN_MAPPINGS.put("airFlow2", "raw_data.air_flow_2");
		COLUMN_MAPPINGS.put("airFlow3", "raw_data.air_flow_3");
		COLUMN_MAPPINGS.put("waterFlow1", "raw_data.water_flow_1");
		COLUMN_MAPPINGS.put("waterFlow2", "raw_data.water_flow_2");
		COLUMN_MAPPINGS.put("waterFlow3", "raw_data.water_flow_3");
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
		COLUMN_MAPPINGS.put("qcFlag", "qc.qc_flag");
		COLUMN_MAPPINGS.put("qcMessage", "qc.qc_message");
		COLUMN_MAPPINGS.put("woceFlag", "qc.woce_flag");
		COLUMN_MAPPINGS.put("woceMessage", "qc.woce_message");
	}
	
	public static String getCSVData(DataSource dataSource, Properties appConfig, long fileId, Instrument instrument, List<String> columns, int co2Type, List<Integer> includeFlags) throws MissingParamException, DatabaseException, RawDataFileException {
		return getCSVData(dataSource, appConfig, fileId, instrument, columns, ",", co2Type, includeFlags, 0, -1);
	}
	
	public static String getCSVData(DataSource dataSource, Properties appConfig, long fileId, Instrument instrument, ExportOption exportOption) throws MissingParamException, DatabaseException, RawDataFileException {
		return getCSVData(dataSource, appConfig, fileId, instrument, exportOption.getColumns(), exportOption.getSeparator(), exportOption.getCo2Type(), exportOption.getFlags(), 0, -1);
	}
	
	public static String getCSVData(DataSource dataSource, Properties appConfig, long fileId, Instrument instrument, List<String> columns, String separator, int co2Type, List<Integer> includeFlags, int start, int length) throws MissingParamException, DatabaseException, RawDataFileException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(instrument, "instrument");
		MissingParam.checkMissing(columns, "columns");
		MissingParam.checkMissing(includeFlags, "includeFlags");
		MissingParam.checkZeroPositive(start, "start");

		// Data output variable
		String output = null;
		
		// Variables for original data. May not be used
		RawDataFile originalFile = null;
		List<List<String>> originalHeaderLines = null;
		int lastUsedLine = 0;
		
		// Variables for getting data from database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		boolean includesOriginalFile = columns.contains(COLUMN_ORIGINAL_FILE);

		try {

			// Get the original file contents
			if (includesOriginalFile) {
				// Load original data from file
				originalFile = DataFileDB.getRawDataFile(dataSource, appConfig, fileId);
				originalHeaderLines = originalFile.getHeaderLines();
			}
			
			
			// Build the database query for the remaining columns.
			// Note that we always include the date to help with
			// searching the original file data
			conn = dataSource.getConnection();
			
			StringBuffer databaseColumnList = new StringBuffer();
			databaseColumnList.append(COLUMN_MAPPINGS.get("dateTime"));
			databaseColumnList.append(',');
			
			for (int col = 0; col < columns.size(); col++) {
				if (!columns.get(col).equals(COLUMN_ORIGINAL_FILE)) {
					String column = columns.get(col);
					if (instrumentHasColumn(instrument, column)) {
						databaseColumnList.append(COLUMN_MAPPINGS.get(column));
						databaseColumnList.append(',');
					}
				}
			}
			
			// We always add a comma separator, so we remove the last character
			// Normally we'd check as we built the string, but the optional 'original' field
			// makes it awkward.
			databaseColumnList.deleteCharAt(databaseColumnList.length() - 1);
			StringBuffer co2Types = new StringBuffer();

			if (co2Type == RunType.RUN_TYPE_BOTH) {
				co2Types.append(RunType.RUN_TYPE_WATER);
				co2Types.append(',');
				co2Types.append(RunType.RUN_TYPE_ATMOSPHERIC);
			} else {
				co2Types.append(co2Type);
			}
			
			StringBuffer flags = new StringBuffer();
			for (int i = 0; i < includeFlags.size(); i++) {
				flags.append(includeFlags.get(i));
				if (i < includeFlags.size() - 1) {
					flags.append(',');
				}
			}

			String queryString = GET_COLUMN_DATA_QUERY.replaceAll("%%COLUMNS%%", databaseColumnList.toString());
			queryString = queryString.replaceAll("%%CO2TYPES%%", co2Types.toString());
			queryString = queryString.replaceAll("%%FLAGS%%", flags.toString());
			
			stmt = conn.prepareStatement(queryString);
			stmt.setLong(1, fileId);
			
			records = stmt.executeQuery();
			
			StringBuffer outputBuffer = new StringBuffer();

			// Build the first header line. This contains all headers
			// exported from the database and the first header line from
			// the data file (if it exists).
			for (int col = 0; col < columns.size(); col++) {
				
				String columnName = columns.get(col);
				boolean columnAdded = true;
				
				if (columnName.equals(COLUMN_ORIGINAL_FILE)) {
					if (originalHeaderLines.size() > 0) {
						
						// For the first header line from the original file
						outputBuffer.append(makeDelimitedHeaderLine(originalHeaderLines.get(0), separator));
					} else {
						
						// If there was no header line, fill in separators
						// for the correct number of columns
						for (int rawCol = 0; rawCol < instrument.getRawFileColumnCount(); rawCol++) {
							outputBuffer.append(separator);
						}
					}
				} else {
					
					// For columns from the database, add the column header
					if (instrumentHasColumn(instrument, columnName)) {
						outputBuffer.append(getColumnHeading(columnName, instrument));
					} else {
						columnAdded = false;
					}
				}
				
				if (columnAdded) {
					outputBuffer.append(separator);
				}
			}
			
			// Strip the trailing separator
			outputBuffer.deleteCharAt(outputBuffer.length() - 1);
			
			outputBuffer.append("\n");
			
			// Now add any remaining header lines from the original file
			// if there are any
			if (includesOriginalFile) {
				
				// For all the header lines after the first one...
				for (int headerLine = 1; headerLine < originalHeaderLines.size(); headerLine++) {
					
					// Find the column for the original data
					for (int col = 0; col < columns.size(); col++) {
						String columnName = columns.get(col);
						boolean addSeparator = true;
												
						if (columnName.equals(COLUMN_ORIGINAL_FILE)) {
							// Add the header line
							outputBuffer.append(makeDelimitedHeaderLine(originalHeaderLines.get(headerLine), separator));
						} else {
							if (!instrumentHasColumn(instrument, columnName)) {
								addSeparator = false;
							}
						}

						// If this isn't the last column, add a separator
						// (non-original columns will get this too, to give empty columns)
						if (addSeparator) {
							outputBuffer.append(separator);
						}
					}
				}
				
				// Strip the trailing separator
				outputBuffer.deleteCharAt(outputBuffer.length() - 1);

				outputBuffer.append("\n");
			}
			
			// Now we add the data!
			while (records.next()) {

				int currentDBColumn = 1;
				
				// Get the date from the first column
				Calendar rowDate = DatabaseUtils.getUTCDateTime(records, currentDBColumn);
				
				// Loop through all the columns. Database columns are offset by 1
				// for the 
				for (int col = 0; col < columns.size(); col++) {
					String columnName = columns.get(col);
					boolean addSeparator = true;
					
					if (columnName.equals("dateTime")) {
						
						// Handle the date/time as a special case 
						currentDBColumn++;
						outputBuffer.append(formatField(records, currentDBColumn, columnName, true));
						
					} else if (!columnName.equals(COLUMN_ORIGINAL_FILE)) {
						
						// Database fields are only added if the instrument has them
						if (!instrumentHasColumn(instrument, columnName)) {
							addSeparator = false;
						} else {
							currentDBColumn++;
							outputBuffer.append(formatField(records, currentDBColumn, columnName, true));
						}
					} else {
						// Find the line corresponding to the date from the database
						int originalFileLine = originalFile.findLineByDate(rowDate, lastUsedLine);
						List<String> originalLine = originalFile.getLineData(originalFileLine);
						
						// Copy the values in
						for (int field = 0; field < originalLine.size(); field++) {
							outputBuffer.append(originalLine.get(field));
							if (field < originalLine.size() - 1) {
								outputBuffer.append(separator);
							}
						}
						
						// Store the line location as the starting point for the next
						// search
						lastUsedLine = originalFileLine;
					}
					
					if (addSeparator) {
						outputBuffer.append(separator);
					}
				}

				// Strip the trailing separator
				outputBuffer.deleteCharAt(outputBuffer.length() - 1);

				// The end of this record
				outputBuffer.append('\n');
			}
			
			output = outputBuffer.toString();
			
		} catch (Exception e) {
			e.printStackTrace();
			output = "***ERROR - " + e.getMessage();
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return output;
	}

	private static String formatField(ResultSet records, int columnIndex, String columnName, boolean asString) throws SQLException, MessageException {
		
		StringBuffer result = new StringBuffer();
		
		switch (columnName) {
		case "dateTime": {
			Calendar colDate = DatabaseUtils.getUTCDateTime(records, columnIndex);
			
			if (asString) {
				result.append(DateTimeUtils.formatDateTime(colDate));
			} else {
				// We return the date as a milliseconds value, which can
				// be parsed into a Date object by the Javascript
				result.append(colDate.getTimeInMillis());
			}
			break;
		}
		case "row": {
			result.append(records.getInt(columnIndex));
			break;
		}
		case "qcFlag":
		case "woceFlag": {
			result.append(records.getString(columnIndex));
			break;
		}
		case "qcMessage": {
			List<Message> messages = RebuildCode.getMessagesFromRebuildCodes(records.getString(columnIndex));
			
			for (int i = 0; i < messages.size(); i++) {
				result.append(messages.get(i).getShortMessage());
				if (i < messages.size() - 1) {
					result.append(';');
				}
			}
			break;
		}
		default: {
			String value = records.getString(columnIndex);
			if (null == value) {
				if (asString) {
					result.append("");
				} else {
					result.append("null");
				}
			} else if (StringUtils.isNumeric(value)) {
				Double doubleValue = Double.parseDouble(value);
				
				if (doubleValue == RawDataDB.MISSING_VALUE) {
					
					if (asString) {
						// The calling method will wrap this in quotes, so we don't have to
						result.append("NaN");
					} else {
						result.append("\"NaN\"");
					}
				} else {
					result.append(String.format(Locale.ENGLISH, "%.3f", Double.parseDouble(value)));
				}
			} else {
				result.append(value.replaceAll("\n", "\\n"));
			}
		}
		}
		
		return result.toString();
	}
	
	public static String getJsonData(DataSource dataSource, long fileId, int co2Type, List<String> columns, List<Integer> includeFlags, int start, int length, boolean sortByFirstColumn, boolean valuesAsStrings) throws MissingParamException, MessageException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(columns, "columns");
		MissingParam.checkMissing(includeFlags, "includeFlags");
		
		String output = null;
		
		// Variables for getting data from database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;

		try {
			conn = dataSource.getConnection();
			stmt = makeFileDataStatement(conn, fileId, columns, co2Type, includeFlags, start, length, sortByFirstColumn);
			
			records = stmt.executeQuery();
			ResultSetMetaData rsmd = records.getMetaData();
			int columnCount = rsmd.getColumnCount();

			StringBuffer outputBuffer = new StringBuffer();
			outputBuffer.append('[');
			
			boolean hasRecords = false;
			while (records.next()) {
				hasRecords = true;
				
				outputBuffer.append('[');
				for (int col = 1; col <= columnCount; col++) {
					

					// The first column is always the date/time (it's added automatically)
					// Plus we check the other columns too (which are zero-based, and the automatic dateTime accounts for one)
					String columnName = columns.get(col - 1);
					
					if (valuesAsStrings) {
						outputBuffer.append('\"');
					}
					outputBuffer.append(formatField(records, col, columnName, valuesAsStrings));
					
					if (valuesAsStrings) {
						outputBuffer.append('\"');
					}
					
					if (col < columnCount) {
						outputBuffer.append(',');
					}
				}

				outputBuffer.append("],");
			}
			
			// Remove the trailing comma from the last record
			if (hasRecords) {
				outputBuffer.deleteCharAt(outputBuffer.length() - 1);
			}
			outputBuffer.append(']');
			
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
	
	public static int getRecordCount(DataSource dataSource, long fileId, int co2Type, List<Integer> includeFlags) throws MissingParamException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(includeFlags, "includeFlags");
		
		int count = 0;
		
		// Variables for getting data from database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;

		try {
			List<String> columns = new ArrayList<String>(1);
			columns.add(COLUMN_RECORD_COUNT);
			
			conn = dataSource.getConnection();
			stmt = makeFileDataStatement(conn, fileId, columns, co2Type, includeFlags, -1, -1, false);
			
			records = stmt.executeQuery();

			if (records.next()) {
				count = records.getInt(1);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			count = 0;
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}

		return count;
	}
	
	private static String getColumnHeading(String columnName, Instrument instrument) {
		String result;
		
		switch (columnName) {
		case "row": {
			result = "Row";
			break;
		}
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
		case "deltaT": {
			result = "Delta Temperature";
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
		case "airFlow1": {
			result = "Air Flow - " + instrument.getAirFlowName1();
			break;
		}
		case "airFlow2": {
			result = "Air Flow - " + instrument.getAirFlowName2();
			break;
		}
		case "airFlow3": {
			result = "Air Flow - " + instrument.getAirFlowName3();
			break;
		}
		case "waterFlow1": {
			result = "Water Flow - " + instrument.getWaterFlowName1();
			break;
		}
		case "waterFlow2": {
			result = "Water Flow - " + instrument.getWaterFlowName2();
			break;
		}
		case "waterFlow3": {
			result = "Water Flow - " + instrument.getWaterFlowName3();
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
		case "qcFlag": {
			result = "Automatic QC Flag";
			break;
		}
		case "qcMessage": {
			result = "Automatic QC Message";
			break;
		}
		case "woceFlag": {
			result = "WOCE Flag";
			break;
		}
		case "woceMessage": {
			result = "WOCE Message";
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
	
	private static String makeDelimitedHeaderLine(List<String> headerLine, String separator) {
		StringBuffer output = new StringBuffer();
		
		for (int i = 0; i < headerLine.size(); i++) {
			output.append(headerLine.get(i));
			if (i < headerLine.size() - 1) {
				output.append(separator);
			}
		}
		
		return output.toString();
	}

	private static PreparedStatement makeFileDataStatement(Connection conn, long fileId, List<String> columns, int co2Type, List<Integer> includeFlags, int start, int length, boolean sortByFirstColumn) throws SQLException {
		
		PreparedStatement stmt = null;

		try {
			String databaseColumnList = makeDatabaseColumnList(columns);
			String co2Types = makeCo2Types(co2Type);
			String flags = makeFlags(includeFlags);

			String queryString = GET_COLUMN_DATA_QUERY.replaceAll("%%COLUMNS%%", databaseColumnList);
			queryString = queryString.replaceAll("%%CO2TYPES%%", co2Types);
			queryString = queryString.replaceAll("%%FLAGS%%", flags);
			
			if (!sortByFirstColumn) {
				queryString = queryString.replaceAll("%%ORDER%%", "raw_data.row");
			} else {
				queryString = queryString.replaceAll("%%ORDER%%", COLUMN_MAPPINGS.get(columns.get(0)));
			}
			
			if (length > 0) {
				queryString += " LIMIT " + start + "," + length;
			}

			stmt = conn.prepareStatement(queryString);
			stmt.setLong(1, fileId);
		} catch (SQLException e) {
			DatabaseUtils.closeStatements(stmt);
			throw e;
		}
		
		return stmt;
	}
	
	private static String makeDatabaseColumnList(List<String> columns) {
		
		StringBuffer databaseColumnList = new StringBuffer();
		
		if (columns.contains(COLUMN_RECORD_COUNT)) {
			databaseColumnList.append("COUNT(*)");
		} else {
			for (int col = 0; col < columns.size(); col++) {
				if (!columns.get(col).equals(COLUMN_ORIGINAL_FILE)) {
					databaseColumnList.append(COLUMN_MAPPINGS.get(columns.get(col)));
					databaseColumnList.append(',');
				}
			}
			
			// We always add a comma separator, so we remove the last character
			// Normally we'd check as we built the string, but the optional 'original' field
			// makes it awkward.
			databaseColumnList.deleteCharAt(databaseColumnList.length() - 1);
		}

		return databaseColumnList.toString();
	}
	
	private static String makeCo2Types(int co2Type) {
		StringBuffer co2Types = new StringBuffer();

		if (co2Type == RunType.RUN_TYPE_BOTH) {
			co2Types.append(RunType.RUN_TYPE_WATER);
			co2Types.append(',');
			co2Types.append(RunType.RUN_TYPE_ATMOSPHERIC);
		} else {
			co2Types.append(co2Type);
		}
		
		return co2Types.toString();
	}
	
	private static String makeFlags(List<Integer> includeFlags) {
		StringBuffer flags = new StringBuffer();
		for (int i = 0; i < includeFlags.size(); i++) {
			flags.append(includeFlags.get(i));
			if (i < includeFlags.size() - 1) {
				flags.append(',');
			}
		}
		
		return flags.toString();
	}

	private static boolean instrumentHasColumn(Instrument instrument, String column) {
		boolean result = true;
		
		switch (column) {
		case "intakeTemp1": {
			result = instrument.hasIntakeTemp1();
			break;
		}
		case "intakeTemp2": {
			result = instrument.hasIntakeTemp2();
			break;
		}
		case "intakeTemp3": {
			result = instrument.hasIntakeTemp3();
			break;
		}
		case "salinity1": {
			result = instrument.hasSalinity1();
			break;
		}
		case "salinity2": {
			result = instrument.hasSalinity2();
			break;
		}
		case "salinity3": {
			result = instrument.hasSalinity3();
			break;
		}
		case "eqt1": {
			result = instrument.hasEqt1();
			break;
		}
		case "eqt2": {
			result = instrument.hasEqt2();
			break;
		}
		case "eqt3": {
			result = instrument.hasEqt3();
			break;
		}
		case "eqp1": {
			result = instrument.hasEqp1();
			break;
		}
		case "eqp2": {
			result = instrument.hasEqp2();
			break;
		}
		case "eqp3": {
			result = instrument.hasEqp3();
			break;
		}
		case "airFlow1": {
			result = instrument.hasAirFlow1();
			break;
		}
		case "airFlow2": {
			result = instrument.hasAirFlow2();
			break;
		}
		case "airFlow3": {
			result = instrument.hasAirFlow3();
			break;
		}
		case "waterFlow1": {
			result = instrument.hasWaterFlow1();
			break;
		}
		case "waterFlow2": {
			result = instrument.hasWaterFlow2();
			break;
		}
		case "waterFlow3": {
			result = instrument.hasWaterFlow3();
			break;
		}
		}

		return result;
	}

}
