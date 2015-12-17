package uk.ac.exeter.QuinCe.database.Instrument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.CalibrationCoefficients;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class CalibrationDB {

	/**
	 * Statement for creating a sensor calibration parent record
	 */
	private static final String CREATE_CALIBRATION_STATEMENT = "INSERT INTO sensor_calibration ("
			+ "instrument_id, calibration_date) VALUES (?, ?)";
	
	/**
	 * Statement for creating the coefficients record for a specific sensor within
	 * a calibration
	 */
	private static final String CREATE_COEFFICIENTS_STATEMENT = "INSERT INTO calibration_coefficients ("
			+ "calibration_id, sensor, intercept, x, x2, x3, x4, x5) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Add a calibration to the database
	 * @param dataSource A data source
	 * @param instrumentID The ID of the instrument being calibrated
	 * @param calibrationDate The date of the calibration
	 * @param coefficients The calibration coefficients for each of the instrument's sensors
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while creating the database records
	 */
	public static void addCalibration(DataSource dataSource, long instrumentID, Date calibrationDate, List<CalibrationCoefficients> coefficients) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(calibrationDate, "calibrationDate");
		MissingParam.checkMissing(coefficients, "coefficients");
		
		Connection conn = null;
		PreparedStatement calibStmt = null;
		long calibID;
		List<PreparedStatement> coefficientStmts = new ArrayList<PreparedStatement>(coefficients.size());

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);

			// Store the main calibration record
			calibStmt = conn.prepareStatement(CREATE_CALIBRATION_STATEMENT, Statement.RETURN_GENERATED_KEYS);

			calibStmt.setLong(1, instrumentID);
			calibStmt.setDate(2, new java.sql.Date(calibrationDate.getTime()));
			
			calibStmt.execute();
			
			ResultSet generatedKeys = calibStmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				calibID = generatedKeys.getLong(1);
			
				// Store the coefficients
				for (CalibrationCoefficients coeffs : coefficients) {
					PreparedStatement coeffStmt = conn.prepareStatement(CREATE_COEFFICIENTS_STATEMENT);
					
					coeffStmt.setLong(1, calibID);
					coeffStmt.setString(2, coeffs.getSensorCode());
					coeffStmt.setDouble(3, coeffs.getIntercept());
					coeffStmt.setDouble(4, coeffs.getX());
					coeffStmt.setDouble(5, coeffs.getX2());
					coeffStmt.setDouble(6, coeffs.getX3());
					coeffStmt.setDouble(7, coeffs.getX4());
					coeffStmt.setDouble(8, coeffs.getX5());
					
					coeffStmt.execute();
					
					coefficientStmts.add(coeffStmt);
				}
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
			
			throw new DatabaseException("Error while storing new calibration records", e);
		} finally {
			for (PreparedStatement stmt : coefficientStmts) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != calibStmt) {
				try {
					calibStmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != conn) {
				try {
					conn.setAutoCommit(true);
					conn.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}		
	}
}
