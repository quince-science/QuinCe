package uk.ac.exeter.QuinCe.database.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import uk.ac.exeter.QCRoutines.config.RoutinesConfig;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QuinCe.data.DateTimeParseException;
import uk.ac.exeter.QuinCe.data.RawDataValues;
import uk.ac.exeter.QuinCe.data.StandardConcentration;
import uk.ac.exeter.QuinCe.data.StandardStub;
import uk.ac.exeter.QuinCe.data.Calculation.GasStandardMean;
import uk.ac.exeter.QuinCe.data.Calculation.GasStandardRuns;
import uk.ac.exeter.QuinCe.data.Instrument.GasStandardDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

public class RawDataDB {
	
	public static final double MISSING_VALUE = RoutinesConfig.NO_VALUE;

	private static final String ADD_MEASUREMENT_STATEMENT = "INSERT INTO raw_data "
			+ "(data_file_id, row, run_type_id, co2_type, date_time, longitude, latitude, "
			+ "intake_temp_1, intake_temp_2, intake_temp_3, "
			+ "salinity_1, salinity_2, salinity_3, "
			+ "eqt_1, eqt_2, eqt_3, eqp_1, eqp_2, eqp_3, "
			+ "air_flow_1, air_flow_2, air_flow_3, "
			+ "water_flow_1, water_flow_2, water_flow_3, "
			+ "xh2o, atmospheric_pressure, co2)"
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String ADD_STANDARD_STATEMENT = "INSERT INTO gas_standards_data "
			+ "(data_file_id, row, date_time, run_type_id, "
			+ "air_flow_1, air_flow_2, air_flow_3, "
			+ "xh2o, concentration, qc_flag, qc_message)"
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String CLEAR_RAW_DATA_STATEMENT = "DELETE FROM raw_data WHERE data_file_id = ?";

	private static final String CLEAR_GAS_STANDARDS_STATEMENT = "DELETE FROM gas_standards_data WHERE data_file_id = ?";

	private static final String GET_RAW_DATA_QUERY = "SELECT "
			+ "r.row, r.date_time, r.run_type_id, r.co2_type, r.longitude, r.latitude, r.intake_temp_1, r.intake_temp_2, r.intake_temp_3,"
			+ "r.salinity_1, r.salinity_2, r.salinity_3, r.eqt_1, r.eqt_2, r.eqt_3, r.eqp_1, r.eqp_2, r.eqp_3,"
			+ "r.air_flow_1, r.air_flow_2, r.air_flow_3, r.water_flow_1, r.water_flow_2, r.water_flow_3,"
			+ "r.xh2o, r.atmospheric_pressure, r.co2 "
			+ "FROM raw_data r INNER JOIN qc ON r.data_file_id = qc.data_file_id AND r.row = qc.row "
			+ "WHERE r.data_file_id = ? ORDER BY row ASC";
	
	private static final String GET_STANDARDS_DATA_QUERY = "SELECT run_type_id, date_time, xh2o, concentration "
			+ "FROM gas_standards_data WHERE data_file_id = ? AND qc_flag = " + Flag.VALUE_GOOD + " ORDER BY row ASC";
	
	private static final String RAW_DATA_TRIM_FLUSHING_RECORDS_QUERY = "SELECT row, run_type_id, date_time FROM raw_data "
			+ "WHERE data_file_id = ? ORDER BY row ASC";

	private static final String GAS_STANDARDS_TRIM_FLUSHING_RECORDS_QUERY = "SELECT row, run_type_id, date_time FROM gas_standards_data "
			+ "WHERE data_file_id = ? ORDER BY row ASC";
	
	private static final String SET_GAS_STANDARDS_INGNORE_FLAG_STMT = "UPDATE gas_standards_data SET qc_flag = " + Flag.VALUE_IGNORED
			+ ", qc_message = 'Flushing time' WHERE data_file_id = ? AND row = ?";
	
	private static final String CLEAR_GAS_STANDARDS_FLUSHING_IGNORE_STMT = "UPDATE gas_standards_data SET qc_flag = "  + Flag.VALUE_GOOD
			+ ", qc_message = NULL WHERE data_file_id = ? AND qc_flag = " + Flag.VALUE_IGNORED;


	public static void clearRawData(DataSource dataSource, long fileId) throws DatabaseException {
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			clearRawData(conn, fileId);
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while clearing out previous data", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}

	public static void clearRawData(Connection conn, long fileId) throws DatabaseException {
		
		PreparedStatement rawDataStmt = null;
		PreparedStatement gasStandardsStmt = null;
		
		try {
			rawDataStmt = conn.prepareStatement(CLEAR_RAW_DATA_STATEMENT);
			rawDataStmt.setLong(1, fileId);
			rawDataStmt.execute();
			
			gasStandardsStmt = conn.prepareStatement(CLEAR_GAS_STANDARDS_STATEMENT);
			gasStandardsStmt.setLong(1, fileId);
			gasStandardsStmt.execute();
			
			conn.commit();
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("An error occurred while clearing out previous data", e);
		} finally {
			DatabaseUtils.closeStatements(rawDataStmt, gasStandardsStmt);
		}
	}
	
	public static void storeRawData(Connection conn, Instrument instrument, long fileId, int lineNumber, List<String> line) throws InstrumentException, DateTimeParseException, SQLException {
		String runType = line.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
		if (instrument.isMeasurementRunType(runType)) {
			storeMeasurement(conn, instrument, fileId, lineNumber, line);
		} else if (instrument.isStandardRunType(runType)) {
			storeStandard(conn, instrument, fileId, lineNumber, line);
		}
	}
	
	private static void storeStandard(Connection conn, Instrument instrument, long fileId, int lineNumber, List<String> line) throws InstrumentException, DateTimeParseException, SQLException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(ADD_STANDARD_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, lineNumber);
			stmt.setTimestamp(3, new Timestamp(instrument.getDateFromLine(line).getTimeInMillis()));
			stmt.setLong(4, instrument.getRunTypeId(line.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE))));
			
			if (instrument.hasAirFlow1()) {
				stmt.setDouble(5, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_AIR_FLOW_1))));
			} else {
				stmt.setNull(5, Types.DOUBLE);
			}
			
			if (instrument.hasAirFlow2()) {
				stmt.setDouble(6, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_AIR_FLOW_2))));
			} else {
				stmt.setNull(6, Types.DOUBLE);
			}
			
			if (instrument.hasAirFlow3()) {
				stmt.setDouble(7, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_AIR_FLOW_3))));
			} else {
				stmt.setNull(7, Types.DOUBLE);
			}
			
			if (!instrument.getSamplesDried()) {
				stmt.setDouble(8, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_XH2O))));
			} else {
				stmt.setNull(8, Types.DOUBLE);
			}
			
			stmt.setDouble(9, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_CO2))));
			
			stmt.setInt(10, Flag.VALUE_GOOD);
			stmt.setNull(11, Types.VARCHAR);

			stmt.execute();
			
		} catch (SQLException|InstrumentException|DateTimeParseException e) {
			throw e;
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	private static void storeMeasurement(Connection conn, Instrument instrument, long fileId, int lineNumber, List<String> line) throws SQLException, InstrumentException, DateTimeParseException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(ADD_MEASUREMENT_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, lineNumber);
			
			stmt.setLong(3, instrument.getRunTypeId(line.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE))));

			
			String runType = line.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
			stmt.setInt(4, instrument.getRunTypeCode(runType));
			
			stmt.setTimestamp(5, new Timestamp(instrument.getDateFromLine(line).getTimeInMillis()));
			stmt.setDouble(6, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_LONGITUDE))));
			stmt.setDouble(7, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_LATITUDE))));
			
			if (instrument.hasIntakeTemp1()) {
				stmt.setDouble(8, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_1))));
			} else {
				stmt.setNull(8, Types.DOUBLE);
			}
			
			if (instrument.hasIntakeTemp2()) {
				stmt.setDouble(9, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_2))));
			} else {
				stmt.setNull(9, Types.DOUBLE);
			}
			
			if (instrument.hasIntakeTemp3()) {
				stmt.setDouble(10, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_3))));
			} else {
				stmt.setNull(10, Types.DOUBLE);
			}
			
			if (instrument.hasSalinity1()) {
				stmt.setDouble(11, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_SALINITY_1))));
			} else {
				stmt.setNull(11, Types.DOUBLE);
			}
			
			if (instrument.hasSalinity2()) {
				stmt.setDouble(12, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_SALINITY_2))));
			} else {
				stmt.setNull(12, Types.DOUBLE);
			}
			
			if (instrument.hasSalinity3()) {
				stmt.setDouble(13, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_SALINITY_3))));
			} else {
				stmt.setNull(13, Types.DOUBLE);
			}
			
			if (instrument.hasEqt1()) {
				stmt.setDouble(14, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQT_1))));
			} else {
				stmt.setNull(14, Types.DOUBLE);
			}
			
			if (instrument.hasEqt2()) {
				stmt.setDouble(15, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQT_2))));
			} else {
				stmt.setNull(15, Types.DOUBLE);
			}
			
			if (instrument.hasEqt3()) {
				stmt.setDouble(16, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQT_3))));
			} else {
				stmt.setNull(16, Types.DOUBLE);
			}
			
			if (instrument.hasEqp1()) {
				stmt.setDouble(17, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQP_1))));
			} else {
				stmt.setNull(17, Types.DOUBLE);
			}
			
			if (instrument.hasEqp2()) {
				stmt.setDouble(18, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQP_2))));
			} else {
				stmt.setNull(18, Types.DOUBLE);
			}
			
			if (instrument.hasEqp3()) {
				stmt.setDouble(19, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQP_3))));
			} else {
				stmt.setNull(19, Types.DOUBLE);
			}
			
			if (instrument.hasAirFlow1()) {
				stmt.setDouble(20, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_AIR_FLOW_1))));
			} else {
				stmt.setNull(20, Types.DOUBLE);
			}
			
			if (instrument.hasAirFlow2()) {
				stmt.setDouble(21, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_AIR_FLOW_2))));
			} else {
				stmt.setNull(21, Types.DOUBLE);
			}
			
			if (instrument.hasAirFlow3()) {
				stmt.setDouble(22, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_AIR_FLOW_3))));
			} else {
				stmt.setNull(22, Types.DOUBLE);
			}
			
			if (instrument.hasWaterFlow1()) {
				stmt.setDouble(23, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_WATER_FLOW_1))));
			} else {
				stmt.setNull(23, Types.DOUBLE);
			}
			
			if (instrument.hasWaterFlow2()) {
				stmt.setDouble(24, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_WATER_FLOW_2))));
			} else {
				stmt.setNull(24, Types.DOUBLE);
			}
			
			if (instrument.hasWaterFlow3()) {
				stmt.setDouble(25, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_WATER_FLOW_3))));
			} else {
				stmt.setNull(25, Types.DOUBLE);
			}
			
			if (!instrument.getSamplesDried()) {
				stmt.setDouble(26, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_XH2O))));
			} else {
				stmt.setNull(26, Types.DOUBLE);
			}
			
			if (instrument.getHasAtmosphericPressure()) {
				stmt.setDouble(27, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_ATMOSPHERIC_PRESSURE))));
			} else {
				stmt.setNull(27, Types.DOUBLE);
			}
			stmt.setDouble(28, parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_CO2))));;
			
			stmt.execute();
			
		} catch (SQLException|InstrumentException|DateTimeParseException e) {
			throw e;
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	public static List<RawDataValues> getRawData(DataSource dataSource, long fileId, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(instrument, "instrument");

		Connection conn = null;
		List<RawDataValues> result = null;
		
		try {
			conn = dataSource.getConnection();
			
			result = getRawData(conn, fileId, instrument);
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving raw data", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}
	
	public static List<RawDataValues> getRawData(Connection conn, long fileId, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(instrument, "instrument");
		
		List<RawDataValues> rawData = new ArrayList<RawDataValues>();
		
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			stmt = conn.prepareStatement(GET_RAW_DATA_QUERY);
			stmt.setLong(1, fileId);
			
			records = stmt.executeQuery();

			if (!records.first()) {
				throw new RecordNotFoundException("No measurement data found for file " + fileId);
			}

			records.beforeFirst();
			while (records.next()) {
				RawDataValues values = new RawDataValues(fileId, records.getInt(1));
				
				Calendar time = DateTimeUtils.getUTCCalendarInstance();
				time.setTime(records.getDate(2));
				values.setTime(time);
				values.setRunTypeId(records.getLong(3));
				values.setCo2Type(records.getInt(4));
				values.setLongitude(records.getDouble(5));
				values.setLatitude(records.getDouble(6));
				values.setIntakeTemp1(records.getDouble(7));
				values.setIntakeTemp2(records.getDouble(8));
				values.setIntakeTemp3(records.getDouble(9));
				values.setSalinity1(records.getDouble(10));
				values.setSalinity2(records.getDouble(11));
				values.setSalinity3(records.getDouble(12));
				values.setEqt1(records.getDouble(13));
				values.setEqt2(records.getDouble(14));
				values.setEqt3(records.getDouble(15));
				values.setEqp1(records.getDouble(16));
				values.setEqp2(records.getDouble(17));
				values.setEqp3(records.getDouble(18));
				values.setAirFlow1(records.getDouble(19));
				values.setAirFlow2(records.getDouble(20));
				values.setAirFlow3(records.getDouble(21));
				values.setWaterFlow1(records.getDouble(22));
				values.setWaterFlow2(records.getDouble(23));
				values.setWaterFlow3(records.getDouble(24));
				values.setXh2o(records.getDouble(25));
				
				if (instrument.getHasAtmosphericPressure()) {
					values.setAtmosphericPressure(records.getDouble(26));
				}
				
				values.setCo2(records.getDouble(27));
				
				rawData.add(values);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving raw data", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return rawData;
	}
	
	public static GasStandardRuns getGasStandardRuns(DataSource dataSource, long fileId, Instrument instrument) throws MissingParamException, DatabaseException, RecordNotFoundException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkMissing(instrument, "instrument");

		GasStandardRuns result = new GasStandardRuns(fileId, instrument);
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_STANDARDS_DATA_QUERY);
			stmt.setLong(1, fileId);
			records = stmt.executeQuery();

			Map<String, StandardConcentration> priorConcentrations = null;
			Calendar nextStandardsDate = null;
			long currentRun = -1;
			Calendar startTime = null;
			Calendar endTime = null;
			double xh2oTotal = 0;
			double concentrationTotal = 0;
			int recordCount = 0;
			
			if (!records.first()) {
				throw new RecordNotFoundException("No standards data found for file " + fileId);
			} else {
				// Load the gas standard concentrations for the first record
				currentRun = records.getLong(1);
				startTime = DateTimeUtils.getUTCCalendarInstance();
				startTime.setTime(records.getTimestamp(2));
				
				StandardStub priorDeployment = GasStandardDB.getStandardBefore(dataSource, instrument.getDatabaseId(), startTime);
				priorConcentrations = GasStandardDB.getConcentrationsMap(dataSource, priorDeployment);
				nextStandardsDate = GasStandardDB.getStandardDateAfter(dataSource, instrument.getDatabaseId(), startTime);
				
			}
			
			records.beforeFirst();
			
			while (records.next()) {
				long runTypeId = records.getLong(1);
				Calendar time = DateTimeUtils.getUTCCalendarInstance();
				time.setTime(records.getTimestamp(2));
				double xh2o = records.getDouble(3);
				double concentration = records.getDouble(4);
				
				
				
				if (runTypeId != currentRun) {
					double meanXh2o = xh2oTotal / (double) recordCount;
					double meanConcentration = concentrationTotal / (double) recordCount;
					String runTypeName = instrument.getRunTypeName(currentRun);
					
					GasStandardMean standardMean = new GasStandardMean(priorConcentrations.get(runTypeName), startTime, endTime, meanConcentration, meanXh2o);
					result.addStandardMean(standardMean);
					
					xh2oTotal = 0;
					concentrationTotal = 0;
					recordCount = 0;
					currentRun = runTypeId;
					startTime = time;
					endTime = time;
					
					if (null != nextStandardsDate && time.after(nextStandardsDate)) {
						StandardStub priorDeployment = GasStandardDB.getStandardBefore(dataSource, instrument.getDatabaseId(), time);
						priorConcentrations = GasStandardDB.getConcentrationsMap(dataSource, priorDeployment);
						nextStandardsDate = GasStandardDB.getStandardDateAfter(dataSource, instrument.getDatabaseId(), time);
					}
				}
				
				endTime = time;
				xh2oTotal += xh2o;
				concentrationTotal += concentration;
				recordCount++;
			}
			
			double meanXh2o = xh2oTotal / (double) recordCount;
			double meanConcentration = concentrationTotal / (double) recordCount;
			String runTypeName = instrument.getRunTypeName(currentRun);
			GasStandardMean standardMean = new GasStandardMean(priorConcentrations.get(runTypeName), startTime, endTime, meanXh2o, meanConcentration);
			result.addStandardMean(standardMean);

		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while retrieving gas standard data for file " + fileId, e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}
	
	public static List<TrimFlushingRecord> getTrimFlushingRecords(Connection conn, long fileId) throws DatabaseException {
		
		List<TrimFlushingRecord> trimFlushingRecords = new ArrayList<TrimFlushingRecord>();
		
		trimFlushingRecords.addAll(getTableTrimFlushingRecords(conn, fileId, RAW_DATA_TRIM_FLUSHING_RECORDS_QUERY, TrimFlushingRecord.RAW_DATA));
		trimFlushingRecords.addAll(getTableTrimFlushingRecords(conn, fileId, GAS_STANDARDS_TRIM_FLUSHING_RECORDS_QUERY, TrimFlushingRecord.GAS_STANDARDS_DATA));
		
		
		Collections.sort(trimFlushingRecords);
		return trimFlushingRecords;
	}
	
	private static List<TrimFlushingRecord> getTableTrimFlushingRecords(Connection conn, long fileId, String query, int recordType) throws DatabaseException {
		
		List<TrimFlushingRecord> tableTrimFlushingRecords = new ArrayList<TrimFlushingRecord>();
		
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			stmt = conn.prepareStatement(query);
			stmt.setLong(1,  fileId);
			records = stmt.executeQuery();
			
			while (records.next()) {
				int row = records.getInt(1);
				long runTypeId = records.getLong(2);
				DateTime dateTime = new DateTime(records.getTimestamp(3).getTime(), DateTimeZone.UTC);
				
				tableTrimFlushingRecords.add(new TrimFlushingRecord(recordType, row, dateTime, runTypeId));
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while retrieving trim flushing records", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return tableTrimFlushingRecords;
	}
	
	public static void setGasStandardIgnoreFlag(Connection conn, long fileId, TrimFlushingRecord record) throws DatabaseException {
		
		if (record.getIgnore()) {
			PreparedStatement stmt = null;
			
			if (record.getRecordType() == TrimFlushingRecord.GAS_STANDARDS_DATA) {
				try {
					stmt = conn.prepareStatement(SET_GAS_STANDARDS_INGNORE_FLAG_STMT);
					stmt.setLong(1, fileId);
					stmt.setInt(2, record.getRow());
					stmt.execute();
				} catch (SQLException e) {
					throw new DatabaseException("Error while setting gas standard ignore flag on row " + record.getRow() + ", file " + fileId, e);
				} finally {
					DatabaseUtils.closeStatements(stmt);
				}
			}				
		}
	}
	
	public static void clearGasStandardIgnoreFlags(Connection conn, long fileId) throws DatabaseException {
		
		PreparedStatement rawDataStatement = null;
		PreparedStatement gasStandardsStatement = null;
		
		try {
			gasStandardsStatement = conn.prepareStatement(CLEAR_GAS_STANDARDS_FLUSHING_IGNORE_STMT);
			gasStandardsStatement.setLong(1, fileId);
			gasStandardsStatement.execute();
			
			conn.commit();
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while clearing flushing time ignore flags for file " + fileId, e);
		} finally {
			DatabaseUtils.closeStatements(rawDataStatement, gasStandardsStatement);
		}
	}
	
	private static double parseDouble(String value) {
		
		double result;
		
		if (StringUtils.isNumeric(value)) {
			result = Double.parseDouble(value);
		} else {
			result = MISSING_VALUE;
		}
		
		return result;
	}
}
