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
import uk.ac.exeter.QuinCe.data.CalibrationStub;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.SensorCode;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
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
	 * Statement for retrieving the list of calibrations for a given instrument.
	 * The list is ordered by descending date.
	 */
	private static final String GET_CALIBRATION_LIST_QUERY = "SELECT id, calibration_date FROM "
			+ "sensor_calibration WHERE instrument_id = ? ORDER BY calibration_date DESC";
	
	/**
	 * Statement for retrieving the calibration stub details for a given calibration
	 */
	private static final String GET_CALIBRATION_STUB_QUERY = "SELECT id, instrument_id, calibration_date FROM "
			+ "sensor_calibration WHERE id = ?";
	
	/**
	 * Statement for retrieving the calibration coefficients for a given calibration
	 */
	private static final String GET_COEFFICIENTS_QUERY = "SELECT sensor, intercept, x, x2, x3, x4, x4 FROM "
			+ "calibration_coefficients WHERE calibration_id = ? ORDER BY sensor ASC";
	
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
		ResultSet generatedKeys = null;
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
			
			generatedKeys = calibStmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				calibID = generatedKeys.getLong(1);
			
				// Store the coefficients
				for (CalibrationCoefficients coeffs : coefficients) {
					PreparedStatement coeffStmt = conn.prepareStatement(CREATE_COEFFICIENTS_STATEMENT);
					
					coeffStmt.setLong(1, calibID);
					coeffStmt.setString(2, coeffs.getSensorCode().toString());
					coeffStmt.setDouble(3, coeffs.getIntercept());
					coeffStmt.setDouble(4, coeffs.getX());
					coeffStmt.setDouble(5, coeffs.getX2());
					coeffStmt.setDouble(6, coeffs.getX3());
					coeffStmt.setDouble(7, coeffs.getX4());
					coeffStmt.setDouble(8, coeffs.getX5());
					
					coeffStmt.execute();
					
					coefficientStmts.add(coeffStmt);
				}
			} else {
				throw new DatabaseException("Parent calibration record not created");
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

			if (null != generatedKeys) {
				try {
					generatedKeys.close();
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
	
	/**
	 * Retrieve the list of calibrations for a specific instrument from the database
	 * @param dataSource A data source
	 * @param instrumentID The instrument ID
	 * @return The list of calibrations.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the list
	 */
	public static List<CalibrationStub> getCalibrationList(DataSource dataSource, long instrumentID) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<CalibrationStub> result = new ArrayList<CalibrationStub>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_CALIBRATION_LIST_QUERY);
			stmt.setLong(1, instrumentID);
			
			records = stmt.executeQuery();
			while (records.next()) {
				result.add(new CalibrationStub(records.getLong(1), instrumentID, records.getDate(2)));
			}
			
			return result;
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving calibrations list", e);
		} finally {
			if (null != records) {
				try {
					records.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != conn) {
				try {
					conn.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}	
	}
	
	/**
	 * Retrieve a calibration stub object for a given calibration ID
	 * @param dataSource A data source
	 * @param calibrationID The calibration ID
	 * @return The calibration stub
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the stub
	 * @throws RecordNotFoundException If the calibration ID does not exist in the database
	 */
	public static CalibrationStub getCalibrationStub(DataSource dataSource, long calibrationID) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(calibrationID, "calibrationID");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		CalibrationStub stub = null;
		
		try {
		
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_CALIBRATION_STUB_QUERY);
			stmt.setLong(1, calibrationID);
			
			records = stmt.executeQuery();
			if (!records.next()) {
				throw new RecordNotFoundException("Could not find calibration with ID " + calibrationID);				
			} else {
				stub = new CalibrationStub(records.getLong(1), records.getLong(2), records.getDate(3));
				return stub;
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving calibration stub", e);
		} finally {
			if (null != records) {
				try {
					records.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != conn) {
				try {
					conn.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
	}

	public static List<CalibrationCoefficients> getCalibrationCoefficients(DataSource dataSource, CalibrationStub calibration) throws MissingParamException, RecordNotFoundException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(calibration, "calibration");
				
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<CalibrationCoefficients> coefficients = new ArrayList<CalibrationCoefficients>();
		
		try {

			// Get the parent instrument details
			Instrument instrument = InstrumentDB.getInstrument(dataSource, calibration.getInstrumentId());
			
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_COEFFICIENTS_QUERY);
			stmt.setLong(1, calibration.getId());
			records = stmt.executeQuery();
			
			while (records.next()) {
				CalibrationCoefficients coeffs = new CalibrationCoefficients(new SensorCode(records.getString(1), instrument));
				coeffs.setIntercept(records.getDouble(2));
				coeffs.setX(records.getDouble(3));
				coeffs.setX2(records.getDouble(4));
				coeffs.setX3(records.getDouble(5));
				coeffs.setX4(records.getDouble(6));
				coeffs.setX5(records.getDouble(7));
				
				coefficients.add(coeffs);
			}
			
			if (coefficients.size() == 0) {
				throw new RecordNotFoundException("Could not find any calibration coefficients for calibration " + calibration.getId());
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving calibration coefficients", e);
		} finally {
			if (null != records) {
				try {
					records.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
			
			if (null != conn) {
				try {
					conn.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
		
		return coefficients;
	}
	
}
