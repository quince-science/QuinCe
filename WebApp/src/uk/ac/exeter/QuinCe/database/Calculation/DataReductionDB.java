package uk.ac.exeter.QuinCe.database.Calculation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class DataReductionDB {

	private static final String CLEAR_DATA_REDUCTION_STATEMENT = "DELETE FROM data_reduction WHERE data_file_id = ?";
	
	private static final String STORE_ROW_STATEMENT = "INSERT INTO data_reduction ("
			+ "data_file_id, row, co2_type, mean_intake_temp, mean_salinity, mean_eqt, delta_temperature, mean_eqp, "
			+ "true_moisture, dried_co2, calibrated_co2, pco2_te_dry, ph2o, pco2_te_wet, fco2_te, fco2) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String UPDATE_ROW_STATEMENT = "UPDATE data_reduction SET "
			+ "mean_intake_temp = ?, mean_salinity = ?, mean_eqt = ?, delta_temperature = ?, mean_eqp = ?, "
			+ "true_moisture = ?, dried_co2 = ?, calibrated_co2 = ?, pco2_te_dry = ?, "
			+ "ph2o = ?, pco2_te_wet = ?, fco2_te = ?, fco2 = ? "
			+ "WHERE data_file_id = ? AND row = ?";
			
	
	private static final String FIND_ROW_STATEMENT = "SELECT COUNT(*) FROM data_reduction WHERE data_file_id = ? AND row = ?";
	
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
	
	public static void storeRow(Connection conn, long fileId, int row, boolean overwrite,
			int co2Type, double meanIntakeTemp, double meanSalinity, double meanEqt, double deltaTemperature, double meanEqp,
			double trueMoisture, double driedCo2, double calibratedCo2, double pCo2TEDry, double pH2O, double pCo2TEWet,
			double fco2TE, double fco2) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(fileId, "fileId");
		MissingParam.checkPositive(row, "row");
		
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
				stmt.setDouble(9, trueMoisture);
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
				stmt.setDouble(6, trueMoisture);
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
			
			if (null != stmt) {
				stmt.execute();
			}
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while storing the row", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
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
