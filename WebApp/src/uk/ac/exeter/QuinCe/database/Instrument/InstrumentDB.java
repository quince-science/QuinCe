package uk.ac.exeter.QuinCe.database.Instrument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Database methods dealing with instruments
 * @author Steve Jones
 *
 */
public class InstrumentDB {

	////////// *** CONSTANTS *** ///////////////
	
	/**
	 * Statement for inserting an instrument record
	 */
	private static final String CREATE_INSTRUMENT_STATEMENT = "INSERT INTO instrument ("
			+ "owner, name,"
			+ "intake_temp_1_name, intake_temp_2_name, intake_temp_3_name, "
			+ "salinity_1_name, salinity_2_name, salinity_3_name, "
			+ "eqt_1_name, eqt_2_name, eqt_3_name, "
			+ "eqp_1_name, eqp_2_name, eqp_3_name, "
			+ "separator_char, date_format, time_format, lat_format, " 
			+ "lon_format, header_lines, has_atmospheric_pressure, samples_dried, "
			+ "run_type_col, date_col, year_col, month_col, day_col, "
			+ "time_col, hour_col, minute_col, second_col, "
			+ "latitude_col, north_south_col, longitude_col, east_west_col, "
			+ "intake_temp_1_col, intake_temp_2_col, intake_temp_3_col, "
			+ "salinity_1_col, salinity_2_col, salinity_3_col, "
			+ "eqt_1_col, eqt_2_col, eqt_3_col, "
			+ "eqp_1_col, eqp_2_col, eqp_3_col, "
			+ "atmospheric_pressure_col, moisture_col, co2_col) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
			+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Statement for inserting run types
	 */
	public static final String CREATE_RUN_TYPE_STATEMENT = "INSERT INTO run_types (instrument_id, run_name, run_type) VALUES (?, ?, ?)";
	
	/**
	 * Add an instrument to the database
	 * @throws MissingParamException If any of the required data are missing
	 * @throws DatabaseException If an error occurs while storing the instrument details
	 */
	public static void addInstrument(DataSource dataSource, User owner, Instrument instrument) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(owner, "owner");
		instrument.validate();
		
		Map<String, Integer> runTypes = instrument.getRunTypes();
		
		Connection conn = null;
		PreparedStatement instrStmt = null;
		List<PreparedStatement> runTypeStmts = new ArrayList<PreparedStatement>(runTypes.size());

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			instrStmt = conn.prepareStatement(CREATE_INSTRUMENT_STATEMENT, Statement.RETURN_GENERATED_KEYS);
			instrStmt.setInt(1, owner.getDatabaseID());
			instrStmt.setString(2, instrument.getName());
			instrStmt.setString(3, instrument.getIntakeTempName1());
			instrStmt.setString(4, instrument.getIntakeTempName2());
			instrStmt.setString(5, instrument.getIntakeTempName3());
			instrStmt.setString(6, instrument.getSalinityName1());
			instrStmt.setString(7, instrument.getSalinityName2());
			instrStmt.setString(8, instrument.getSalinityName3());
			instrStmt.setString(9, instrument.getEqtName1());
			instrStmt.setString(10, instrument.getEqtName2());
			instrStmt.setString(11, instrument.getEqtName3());
			instrStmt.setString(12, instrument.getEqpName1());
			instrStmt.setString(13, instrument.getEqpName2());
			instrStmt.setString(14, instrument.getEqpName3());
			instrStmt.setString(15, String.valueOf(instrument.getSeparatorChar()));
			instrStmt.setInt(16, instrument.getDateFormat());
			instrStmt.setInt(17, instrument.getTimeFormat());
			instrStmt.setInt(18, instrument.getLatFormat());
			instrStmt.setInt(19, instrument.getLonFormat());
			instrStmt.setInt(20, instrument.getHeaderLines());
			instrStmt.setBoolean(21, instrument.getHasAtmosphericPressure());
			instrStmt.setBoolean(22, instrument.getSamplesDried());
			instrStmt.setInt(23, instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
			instrStmt.setInt(24, instrument.getColumnAssignment(Instrument.COL_DATE));
			instrStmt.setInt(25, instrument.getColumnAssignment(Instrument.COL_YEAR));
			instrStmt.setInt(26, instrument.getColumnAssignment(Instrument.COL_MONTH));
			instrStmt.setInt(27, instrument.getColumnAssignment(Instrument.COL_DAY));
			instrStmt.setInt(28, instrument.getColumnAssignment(Instrument.COL_TIME));
			instrStmt.setInt(29, instrument.getColumnAssignment(Instrument.COL_HOUR));
			instrStmt.setInt(30, instrument.getColumnAssignment(Instrument.COL_MINUTE));
			instrStmt.setInt(31, instrument.getColumnAssignment(Instrument.COL_SECOND));
			instrStmt.setInt(32, instrument.getColumnAssignment(Instrument.COL_LATITUDE));
			instrStmt.setInt(33, instrument.getColumnAssignment(Instrument.COL_NORTH_SOUTH));
			instrStmt.setInt(34, instrument.getColumnAssignment(Instrument.COL_LONGITUDE));
			instrStmt.setInt(35, instrument.getColumnAssignment(Instrument.COL_EAST_WEST));
			instrStmt.setInt(36, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_1));
			instrStmt.setInt(37, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_2));
			instrStmt.setInt(38, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_3));
			instrStmt.setInt(39, instrument.getColumnAssignment(Instrument.COL_SALINITY_1));
			instrStmt.setInt(40, instrument.getColumnAssignment(Instrument.COL_SALINITY_2));
			instrStmt.setInt(41, instrument.getColumnAssignment(Instrument.COL_SALINITY_3));
			instrStmt.setInt(42, instrument.getColumnAssignment(Instrument.COL_EQT_1));
			instrStmt.setInt(43, instrument.getColumnAssignment(Instrument.COL_EQT_2));
			instrStmt.setInt(44, instrument.getColumnAssignment(Instrument.COL_EQT_3));
			instrStmt.setInt(45, instrument.getColumnAssignment(Instrument.COL_EQP_1));
			instrStmt.setInt(46, instrument.getColumnAssignment(Instrument.COL_EQP_2));
			instrStmt.setInt(47, instrument.getColumnAssignment(Instrument.COL_EQP_3));
			instrStmt.setInt(48, instrument.getColumnAssignment(Instrument.COL_ATMOSPHERIC_PRESSURE));
			instrStmt.setInt(49, instrument.getColumnAssignment(Instrument.COL_MOISTURE));
			instrStmt.setInt(50, instrument.getColumnAssignment(Instrument.COL_CO2));
			
			
			instrStmt.execute();
			
			ResultSet generatedKeys = instrStmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				instrument.setDatabaseID(generatedKeys.getLong(1));
			}

			for (Map.Entry<String, Integer> entry : runTypes.entrySet()) {
				PreparedStatement stmt = conn.prepareStatement(CREATE_RUN_TYPE_STATEMENT);
				stmt.setLong(1, instrument.getDatabaseID());
				stmt.setString(2, entry.getKey());
				stmt.setInt(3, entry.getValue());
				
				stmt.execute();
				
				runTypeStmts.add(stmt);
			}
			
			conn.commit();
			
		} catch (SQLException e) {
			
			if (null != conn) {
	            try {
	                System.err.print("Transaction is being rolled back");
	                conn.rollback();
	            } catch(SQLException excep) {
	                
	            }
			}
			
			throw new DatabaseException("Error while storing new instrument records", e);
		} finally {
			
			for (PreparedStatement stmt : runTypeStmts) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != instrStmt) {
				try {
					instrStmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != conn) {
				try {
					conn.setAutoCommit(true);
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
	}
}
