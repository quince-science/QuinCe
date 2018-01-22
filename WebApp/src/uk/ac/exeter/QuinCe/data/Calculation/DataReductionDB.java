package uk.ac.exeter.QuinCe.data.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.RunType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * This class provides methods for storing data reduction calculation results
 * in the database.
 * 
 * @author Steve Jones
 *
 */
@Deprecated
public class DataReductionDB {

	/**
	 * Statement to remove all data reduction data for a given data file
	 * @see #clearDataReductionData(Connection, long)
	 */
	private static final String CLEAR_DATA_REDUCTION_STATEMENT = "DELETE FROM data_reduction WHERE data_file_id = ?";
	
	/**
	 * Statement to store a data reduction calculation result in the database for a given row in a given data file
	 * @see #storeRow(Connection, long, int, boolean, int, double, double, double, double, double, double, double, double, double, double, double, double, double)
	 */
	private static final String STORE_ROW_STATEMENT = "INSERT INTO data_reduction ("
			+ "data_file_id, row, co2_type, mean_intake_temp, mean_salinity, mean_eqt, delta_temperature, mean_eqp, "
			+ "true_xh2o, dried_co2, calibrated_co2, pco2_te_dry, ph2o, pco2_te_wet, fco2_te, fco2) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Statement to update a data reduction calculation result for a given row in a given data file
	 * @see #storeRow(Connection, long, int, boolean, int, double, double, double, double, double, double, double, double, double, double, double, double, double)
	 */
	private static final String UPDATE_ROW_STATEMENT = "UPDATE data_reduction SET "
			+ "mean_intake_temp = ?, mean_salinity = ?, mean_eqt = ?, delta_temperature = ?, mean_eqp = ?, "
			+ "true_xh2o = ?, dried_co2 = ?, calibrated_co2 = ?, pco2_te_dry = ?, "
			+ "ph2o = ?, pco2_te_wet = ?, fco2_te = ?, fco2 = ? "
			+ "WHERE data_file_id = ? AND row = ?";
			
	/**
	 * Query to retrieve the data reduction result for a given row in a given data file
	 * @see #rowExists(Connection, long, int)
	 */
	private static final String FIND_ROW_STATEMENT = "SELECT COUNT(*) FROM data_reduction WHERE data_file_id = ? AND row = ?";
	
	/**
	 * Removes all the data reduction calculation results for the specified data file
	 * @param dataSource A data source
	 * @param fileId The database ID of the data file
	 * @throws DatabaseException If a database error occurs
	 * @see #clearDataReductionData(Connection, long)
	 */
	public static void clearDataReductionData(DataSource dataSource, long fileId) throws DatabaseException {
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			clearDataReductionData(conn, fileId);
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while clearing out previous data", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}

	/**
	 * Removes all the data reduction calculation results for the specified data file
	 * @param conn A database connection
	 * @param fileId The database ID of the data file
	 * @throws DatabaseException If a database error occurs
	 * @see #CLEAR_DATA_REDUCTION_STATEMENT
	 */
	public static void clearDataReductionData(Connection conn, long fileId) throws DatabaseException {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(CLEAR_DATA_REDUCTION_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.execute();
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("An error occurred while clearing out previous data", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	/**
	 * <p>Store the data reduction calculation results for a row in a data file.</p>
	 * 
	 * <p>
	 *   This method will check to see if the specified row already has a result stored.
	 *   If it does, the {@code overwrite} parameter determines whether or not the existing
	 *   record should be replaced. If {@code overwrite} is {@code false}, no action will be taken.
	 * </p>
	 * @param conn A database connection
	 * @param fileId The database ID of the data file
	 * @param row The row number
	 * @param overwrite Specifies whether or not existing rows should be overwritten
	 * @param co2Type Indicates whether this is an ocean or atmospheric CO<sub>2</sub> measurement. See {@link RunType}.
	 * @param meanIntakeTemp The mean intake temperature from all (used) intake temperature sensors
	 * @param meanSalinity The mean salinity from all (used) salinity sensors
	 * @param meanEqt The mean equilibrator temperature from all (used) equilibrator temperature sensors
	 * @param deltaTemperature The difference between the mean intake temperature and the mean equilibrator temperature
	 * @param meanEqp The mean equilibrator pressure from all (used) equilibrator pressure sensors
	 * @param trueMoisture The calculated true moisture of the measurement. If the instrument fully dries samples before measurement, this should be identical to the measured moisture.
	 * @param driedCo2 The calculated dried CO<sub>2</sub> value of the measurement. If the instrument fully dries samples before measurement, this should be identical to the measured CO<sub>2</sub>.
	 * @param calibratedCo2 The CO<sub>2</sub> value after calibration to the gas standards 
	 * @param pCo2TEDry The partial pressure of CO<sub>2</sub> at equilibrator temperature 
	 * @param pH2O The vapour pressure of water
	 * @param pCo2TEWet The partial pressure of CO<sub>2</sub> adjusted to its level in water
	 * @param fco2TE The fugacity of CO<sub>2</sub> at equilibrator temperature
	 * @param fco2 The fugacity of CO<sub>2</sub> at the sea surface temperature (the final result of the calculation)
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any of the parameters are missing
	 * @see #STORE_ROW_STATEMENT
	 * @see #UPDATE_ROW_STATEMENT
	 */
	public static void storeRow(Connection conn, long fileId, int row, boolean overwrite,
			int co2Type, double meanIntakeTemp, double meanSalinity, double meanEqt, double deltaTemperature, double meanEqp,
			double xh2o, double driedCo2, double calibratedCo2, double pCo2TEDry, double pH2O, double pCo2TEWet,
			double fco2TE, double fco2) throws DatabaseException, MissingParamException {
		
		//TODO Should deltaTemperature be calculated by this method or left to the calling method?
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkPositive(row, "row");
		//TODO Should we check all the calculation parameters where possible?
		
		PreparedStatement stmt = null;
		
		try {
			if (!rowExists(conn, fileId, row)) { 
				stmt = conn.prepareStatement(STORE_ROW_STATEMENT);
				stmt.setLong(1, fileId);
				stmt.setInt(2, row);
				stmt.setInt(3, co2Type);
				stmt.setDouble(4, meanIntakeTemp);
				stmt.setDouble(5, meanSalinity);
				stmt.setDouble(6, meanEqt);
				stmt.setDouble(7, deltaTemperature);
				stmt.setDouble(8, meanEqp);
				stmt.setDouble(9, xh2o);
				stmt.setDouble(10, driedCo2);
				stmt.setDouble(11, calibratedCo2);
				stmt.setDouble(12, pCo2TEDry);
				stmt.setDouble(13, pH2O);
				stmt.setDouble(14, pCo2TEWet);
				stmt.setDouble(15, fco2TE);
				stmt.setDouble(16, fco2);
			} else if (overwrite) {
				stmt = conn.prepareStatement(UPDATE_ROW_STATEMENT);
				stmt.setDouble(1, meanIntakeTemp);
				stmt.setDouble(2, meanSalinity);
				stmt.setDouble(3, meanEqt);
				stmt.setDouble(4, deltaTemperature);
				stmt.setDouble(5, meanEqp);
				stmt.setDouble(6, xh2o);
				stmt.setDouble(7, driedCo2);
				stmt.setDouble(8, calibratedCo2);
				stmt.setDouble(9, pCo2TEDry);
				stmt.setDouble(10, pH2O);
				stmt.setDouble(11, pCo2TEWet);
				stmt.setDouble(12, fco2TE);
				stmt.setDouble(13, fco2);
				stmt.setLong(14, fileId);
				stmt.setInt(15, row);
			}
			
			// TODO If overwrite is false and a row exists, should we throw an error or something?
			if (null != stmt) {
				stmt.execute();
			}
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while storing the row", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
	/**
	 * Determines whether or not the results of the data reduction calculations have been stored for the given row
	 * in the given data file.
	 * 
	 * @param conn A database connection
	 * @param fileId The database ID of the data file
	 * @param row The row number
	 * @return {@code true} if a result has been stored for this row; {@code false} otherwise
	 * @throws DatabaseException If a database error occurs
	 * @see #FIND_ROW_STATEMENT
	 */
	private static boolean rowExists(Connection conn, long fileId, int row) throws DatabaseException {
		
		boolean result = false;
		
		PreparedStatement stmt = null;
		ResultSet recordCount = null;
		
		try {
			stmt = conn.prepareStatement(FIND_ROW_STATEMENT);
			stmt.setLong(1, fileId);
			stmt.setInt(2, row);
			
			recordCount = stmt.executeQuery();
			recordCount.first();
			result = recordCount.getInt(1) > 0;
			
			stmt.executeQuery();
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while search for a row", e);
		} finally {
			DatabaseUtils.closeResultSets(recordCount);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return result;
	}
}
