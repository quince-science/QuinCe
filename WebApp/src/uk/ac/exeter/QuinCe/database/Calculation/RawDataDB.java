package uk.ac.exeter.QuinCe.database.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.DateTimeParseException;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.InstrumentException;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;

public class RawDataDB {

	private static final String ADD_MEASUREMENT_STATEMENT = "INSERT INTO raw_data "
			+ "(data_file_id, row, co2_type, date_time, longitude, latitude,"
			+ "intake_temp_1, intake_temp_2, intake_temp_3,"
			+ "salinity_1, salinity_2, salinity_3,"
			+ "eqt_1, eqt_2, eqt_3, eqp_1, eqp_2, eqp_3, co2)"
			+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String ADD_STANDARD_STATEMENT = "INSERT INTO gas_standards_data "
			+ "(data_file_id, row, date_time, run_id, concentration)"
			+ " VALUES(?, ?, ?, ?, ?)";
	
	private static final String CLEAR_RAW_DATA_STATEMENT = "DELETE FROM raw_data WHERE data_file_id = ?";

	private static final String CLEAR_GAS_STANDARDS_STATEMENT = "DELETE FROM gas_standards_data WHERE data_file_id = ?";

	public static void clearRawData(DataSource dataSource, long fileId) throws DatabaseException {
		
		Connection conn = null;
		PreparedStatement rawDataStmt = null;
		PreparedStatement gasStandardsStmt = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
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
			DatabaseUtils.closeConnection(conn);
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
			stmt.setDouble(5, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_CO2))));

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
			
			String runType = line.get(instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
			stmt.setInt(3, instrument.getRunTypeCode(runType));
			
			stmt.setTimestamp(4, new Timestamp(instrument.getDateFromLine(line).getTimeInMillis()));
			stmt.setDouble(5, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_LONGITUDE))));
			stmt.setDouble(6, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_LATITUDE))));
			
			if (instrument.hasIntakeTemp1()) {
				stmt.setDouble(7, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_1))));
			} else {
				stmt.setNull(7, Types.DOUBLE);
			}
			
			if (instrument.hasIntakeTemp2()) {
				stmt.setDouble(8, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_2))));
			} else {
				stmt.setNull(8, Types.DOUBLE);
			}
			
			if (instrument.hasIntakeTemp3()) {
				stmt.setDouble(9, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_3))));
			} else {
				stmt.setNull(9, Types.DOUBLE);
			}
			
			if (instrument.hasSalinity1()) {
				stmt.setDouble(10, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_SALINITY_1))));
			} else {
				stmt.setNull(10, Types.DOUBLE);
			}
			
			if (instrument.hasSalinity2()) {
				stmt.setDouble(11, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_SALINITY_2))));
			} else {
				stmt.setNull(11, Types.DOUBLE);
			}
			
			if (instrument.hasSalinity3()) {
				stmt.setDouble(12, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_SALINITY_3))));
			} else {
				stmt.setNull(12, Types.DOUBLE);
			}
			
			if (instrument.hasEqt1()) {
				stmt.setDouble(13, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQT_1))));
			} else {
				stmt.setNull(13, Types.DOUBLE);
			}
			
			if (instrument.hasEqt2()) {
				stmt.setDouble(14, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQT_2))));
			} else {
				stmt.setNull(14, Types.DOUBLE);
			}
			
			if (instrument.hasEqt3()) {
				stmt.setDouble(15, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQT_3))));
			} else {
				stmt.setNull(15, Types.DOUBLE);
			}
			
			if (instrument.hasEqp1()) {
				stmt.setDouble(16, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQP_1))));
			} else {
				stmt.setNull(16, Types.DOUBLE);
			}
			
			if (instrument.hasEqp2()) {
				stmt.setDouble(17, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQP_2))));
			} else {
				stmt.setNull(17, Types.DOUBLE);
			}
			
			if (instrument.hasEqp3()) {
				stmt.setDouble(18, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_EQP_3))));
			} else {
				stmt.setNull(18, Types.DOUBLE);
			}
			
			stmt.setDouble(19, Double.parseDouble(line.get(instrument.getColumnAssignment(Instrument.COL_CO2))));;

			stmt.execute();
			
		} catch (SQLException|InstrumentException|DateTimeParseException e) {
			throw e;
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
}
