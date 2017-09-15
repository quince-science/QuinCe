package uk.ac.exeter.QuinCe.data.Instrument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationCoefficients;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationStub;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * This class provides database calls related to instrument calibrations.
 * 
 * @author Steve Jones
 *
 */
public class CalibrationDB {

	/**
	 * Statement for creating a sensor calibration parent record
	 * @see #addCalibration(DataSource, long, Date, List)
	 */
	private static final String CREATE_CALIBRATION_STATEMENT = "INSERT INTO sensor_calibration ("
			+ "instrument_id, calibration_date) VALUES (?, ?)";
	
	/**
	 * Statement for creating the coefficients record for a specific sensor within
	 * a calibration
	 * @see #addCalibration(DataSource, long, Date, List)
	 * @see #updateCalibration(DataSource, long, Date, List)
	 */
	private static final String CREATE_COEFFICIENTS_STATEMENT = "INSERT INTO calibration_coefficients ("
			+ "calibration_id, sensor, intercept, x, x2, x3, x4, x5) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Statement for retrieving the list of calibrations for a given instrument.
	 * The list is ordered by descending date.
	 * @see #getCalibrationList(DataSource, long)
	 */
	private static final String GET_CALIBRATION_LIST_QUERY = "SELECT id, calibration_date FROM "
			+ "sensor_calibration WHERE instrument_id = ? ORDER BY calibration_date DESC";
	
	/**
	 * Statement for retrieving the calibration stub details for a given calibration
	 * @see #getCalibrationStub(DataSource, long)
	 */
	private static final String GET_CALIBRATION_STUB_QUERY = "SELECT id, instrument_id, calibration_date FROM "
			+ "sensor_calibration WHERE id = ?";
	
	/**
	 * Statement for retrieving the calibration coefficients for a given calibration
	 * @see #getCalibrationCoefficients(DataSource, CalibrationStub)
	 */
	private static final String GET_COEFFICIENTS_QUERY = "SELECT sensor, intercept, x, x2, x3, x4, x5 FROM "
			+ "calibration_coefficients WHERE calibration_id = ? ORDER BY sensor ASC";
	
	/**
	 * Statement to change the date for a given calibration
	 * @see #updateCalibration(DataSource, long, Date, List)
	 */
	private static final String UPDATE_CALIBRATION_STATEMENT = "UPDATE sensor_calibration SET calibration_date = ? WHERE id = ?";
	
	/**
	 * Statement to remove all calibration coefficients for a given calibration.
	 * This is used prior to adding the revised coefficients.
	 * @see #deleteCalibration(DataSource, long)
	 * @see #updateCalibration(DataSource, long, Date, List)
	 */
	private static final String REMOVE_COEFFICIENTS_STATEMENT = "DELETE FROM calibration_coefficients WHERE calibration_id = ?";
	
	/**
	 * Statement for finding a calibration record with a given ID
	 * @see #calibrationExists(Connection, long)
	 */
	private static final String FIND_CALIBRATION_QUERY = "SELECT id FROM sensor_calibration WHERE id = ?";
	
	/**
	 * Statement for removing a calibration record.
	 * @see #deleteCalibration(DataSource, long)
	 */
	private static final String REMOVE_CALIBRATION_STATEMENT = "DELETE FROM sensor_calibration WHERE id = ?";
	
	/**
	 * Statement to find calibration dates between two given dates
	 * @see #getCalibrationDatesBetween(DataSource, long, Calendar, Calendar)
	 * @see #getCalibrationsForFile(DataSource, long, Calendar, Calendar)
	 */
	private static final String GET_CALIBRATIONS_BETWEEN_QUERY = "SELECT id, calibration_date FROM sensor_calibration WHERE instrument_id = ? AND calibration_date > ? AND calibration_date <= ? ORDER BY calibration_date ASC";
	
	/**
	 * Statement to find the latest calibration date before a given date
	 * @see #getCalibrationDateBefore(DataSource, long, Calendar)
	 * @see #getCalibrationsForFile(DataSource, long, Calendar, Calendar)
	 */
	private static final String GET_CALIBRATION_BEFORE_QUERY = "SELECT id, calibration_date FROM sensor_calibration WHERE instrument_id = ? AND calibration_date <= ? ORDER BY calibration_date DESC LIMIT 1";
	
	/**
	 * Add a calibration to the database
	 * @param dataSource A data source
	 * @param instrumentID The ID of the instrument being calibrated
	 * @param calibrationDate The date of the calibration
	 * @param coefficients The calibration coefficients for each of the instrument's sensors
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while creating the database records
	 * @see #CREATE_CALIBRATION_STATEMENT
	 * @see #CREATE_COEFFICIENTS_STATEMENT
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

			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while storing new calibration records", e);
		} finally {
			DatabaseUtils.closeStatements(coefficientStmts);
			DatabaseUtils.closeResultSets(generatedKeys);
			DatabaseUtils.closeStatements(calibStmt);
			DatabaseUtils.closeConnection(conn);
		}		
	}
	
	/**
	 * Retrieve the list of calibrations for a specific instrument from the database. The calibrations are
	 * returned as {@link CalibrationStub} objects, which contain the basic details of the calibration but
	 * not the coefficients for the sensors. These can be retrieved using the
	 * {@link #getCalibrationCoefficients(DataSource, CalibrationStub)} method.
	 * 
	 * @param dataSource A data source
	 * @param instrumentID The instrument ID
	 * @return The list of calibrations.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the list
	 * @see CalibrationStub
	 * @see #GET_CALIBRATION_LIST_QUERY
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
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}	
	}
	
	/**
	 * Retrieve a {@link CalibrationStub} object for a given calibration ID. This contains the basic details
	 * of the calibration, but does not contain the calibration coefficients for the instrument's sensors.
	 * These can be retrieved using the {@link #getCalibrationCoefficients(DataSource, CalibrationStub)} method.
	 * 
	 * @param dataSource A data source
	 * @param calibrationID The calibration ID
	 * @return The calibration stub
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the stub
	 * @throws RecordNotFoundException If the calibration ID does not exist in the database
	 * @see #GET_CALIBRATION_STUB_QUERY 
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
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}

	/**
	 * Retrieve the calibration coefficients for an instrument's sensors given a {@link CalibrationStub}
	 * object that contains the instrument ID and the date of the calibration.
	 * 
	 * @param dataSource A data source
	 * @param calibration The basic calibration details
	 * @return The calibration coefficients for each of the instrument's sensors
	 * 
	 * @throws MissingParamException If any of the parameters are {@code null}
	 * @throws RecordNotFoundException If the specified calibration does not exist in the database
	 * @throws DatabaseException If a database error occurs
	 * @throws ResourceException If the application configuration cannot be retrieved
	 * @throws InstrumentException If any instrument details are invalid
	 * @see #GET_COEFFICIENTS_QUERY
	 */
	public static List<CalibrationCoefficients> getCalibrationCoefficients(DataSource dataSource, CalibrationStub calibration) throws MissingParamException, RecordNotFoundException, DatabaseException, InstrumentException, ResourceException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(calibration, "calibration");
				
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<CalibrationCoefficients> coefficients = new ArrayList<CalibrationCoefficients>();
		
		try {

			// Get the parent instrument details
			Instrument instrument = InstrumentDB.getInstrument(dataSource, calibration.getInstrumentId(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
			
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
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return coefficients;
	}

	/**
	 * Updates the details of an existing calibration in the database.
	 * 
	 * @param dataSource A data source
	 * @param calibrationID The database ID of the calibration to be updated
	 * @param calibrationDate The date of the calibration
	 * @param coefficients The calibration coefficients for the instrument's sensors
	 * @throws MissingParamException If any of the required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the specified calibration database ID does not exist
	 * @see #UPDATE_CALIBRATION_STATEMENT
	 * @see #REMOVE_COEFFICIENTS_STATEMENT
	 * @see #CREATE_COEFFICIENTS_STATEMENT
	 */
	public static void updateCalibration(DataSource dataSource, long calibrationID, Date calibrationDate, List<CalibrationCoefficients> coefficients) throws MissingParamException, DatabaseException, RecordNotFoundException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(calibrationID, "calibrationID");
		MissingParam.checkMissing(calibrationDate, "calibrationDate");
		MissingParam.checkMissing(coefficients, "coefficients");
		
		Connection conn = null;
		PreparedStatement updateCalibStmt = null;
		PreparedStatement removeCoeffsStmt = null;
		List<PreparedStatement> coefficientStmts = new ArrayList<PreparedStatement>(coefficients.size());
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			if (!calibrationExists(conn, calibrationID)) {
				throw new RecordNotFoundException("Could not find calibration " + calibrationID);
			}

			// Update the calibration date 
			updateCalibStmt = conn.prepareStatement(UPDATE_CALIBRATION_STATEMENT);
			updateCalibStmt.setDate(1, new java.sql.Date(calibrationDate.getTime()));
			updateCalibStmt.setLong(2, calibrationID);
			
			updateCalibStmt.execute();
			
			// Remove the existing coefficients
			removeCoeffsStmt = conn.prepareStatement(REMOVE_COEFFICIENTS_STATEMENT);
			removeCoeffsStmt.setLong(1, calibrationID);
			removeCoeffsStmt.execute();
			
			// Add the updated coefficients
			for (CalibrationCoefficients coeffs : coefficients) {
				PreparedStatement coeffStmt = conn.prepareStatement(CREATE_COEFFICIENTS_STATEMENT);
				
				coeffStmt.setLong(1, calibrationID);
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

			conn.commit();
			
		} catch (SQLException e) {

			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while updating calibration " + calibrationID, e);
		} finally {
			DatabaseUtils.closeStatements(removeCoeffsStmt, updateCalibStmt);
			DatabaseUtils.closeStatements(coefficientStmts);
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Search for an existing calibration record with a given ID
	 * 
	 * @param conn A database connection
	 * @param calibrationID The calibration's database ID
	 * @return {@code true} if the calibration exists; {@code false} if it does not.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while searching the database
	 * @see #FIND_CALIBRATION_QUERY
	 */
	private static boolean calibrationExists(Connection conn, long calibrationID) throws MissingParamException, DatabaseException {
		
		boolean result = false;
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(calibrationID, "calibrationID");
		
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			stmt = conn.prepareStatement(FIND_CALIBRATION_QUERY);
			stmt.setLong(1, calibrationID);
			
			records = stmt.executeQuery();
			result = records.next();
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for calibration");
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return result;
	}

	/**
	 * Remove a calibration from the database
	 * 
	 * @param dataSource A data source
	 * @param calibrationID The calibration ID
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @see #REMOVE_COEFFICIENTS_STATEMENT
	 * @see #REMOVE_CALIBRATION_STATEMENT
	 */
	public static void deleteCalibration(DataSource dataSource, long calibrationID) throws MissingParamException, DatabaseException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(calibrationID, "calibrationID");
		
		Connection conn = null;
		PreparedStatement removeCoeffsStmt = null;
		PreparedStatement removeCalibStmt = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			removeCoeffsStmt = conn.prepareStatement(REMOVE_COEFFICIENTS_STATEMENT);
			removeCoeffsStmt.setLong(1, calibrationID);
			removeCoeffsStmt.execute();
			
			removeCalibStmt = conn.prepareStatement(REMOVE_CALIBRATION_STATEMENT);
			removeCalibStmt.setLong(1, calibrationID);
			removeCalibStmt.execute();
			
			conn.commit();
			
		} catch (SQLException e) {

			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while deleting calibraion " + calibrationID, e);
		} finally {
			DatabaseUtils.closeStatements(removeCoeffsStmt, removeCalibStmt);
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Return a list of calibration dates for a given instrument that
	 * fall between two specified dates
	 * @param dataSource A data source
	 * @param instrumentID The database ID of the instrument
	 * @param firstDate The first date
	 * @param lastDate The last date
	 * @return A list of calibration dates
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the dates
	 * @see #GET_CALIBRATIONS_BETWEEN_QUERY
	 */
	public static List<Calendar> getCalibrationDatesBetween(DataSource dataSource, long instrumentID, Calendar firstDate, Calendar lastDate) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(firstDate, "firstDate");
		MissingParam.checkMissing(lastDate, "lastDate");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<Calendar> result = new ArrayList<Calendar>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_CALIBRATIONS_BETWEEN_QUERY);
			stmt.setLong(1, instrumentID);
			stmt.setDate(2, new java.sql.Date(DateTimeUtils.setMidnight(firstDate).getTime().getTime()));
			stmt.setDate(3, new java.sql.Date(DateTimeUtils.setMidnight(lastDate).getTime().getTime()));
			
			records = stmt.executeQuery();
			while (records.next()) {
				Calendar calibrationDate = DateTimeUtils.getUTCCalendarInstance();
				calibrationDate.setTime(records.getDate(2));
				result.add(calibrationDate);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for calibration dates", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}

	/**
	 * Find the last calibration date for a specified instrument before a given date
	 * @param dataSource A data source
	 * @param instrumentID The instrument ID
	 * @param date The date
	 * @return The last calibration date before the given date. If there is no date, returns null.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the date.
	 * @see #GET_CALIBRATION_BEFORE_QUERY
	 */
	public static Calendar getCalibrationDateBefore(DataSource dataSource, long instrumentID, Calendar date) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(date, "date");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		Calendar result = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_CALIBRATION_BEFORE_QUERY);
			stmt.setLong(1, instrumentID);
			stmt.setDate(2, new java.sql.Date(DateTimeUtils.setMidnight(date).getTime().getTime()));
			
			records = stmt.executeQuery();
			if (records.next()) {
				result = DateTimeUtils.getUTCCalendarInstance();
				result.setTime(records.getDate(2));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for calibration dates", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}

		return result;
	}
	
	/**
	 * <p>Return the list of instrument calibrations that encompass a given data file, provided as the first and last record
	 * dates in that file. The calibrations returned include all those that happened during the file's duration, as well as
	 * the calibration immediately preceding the file's first record, as that will be the calibration in place for the first
	 * record(s).</p>
	 * 
	 * <p>The calibrations are returned as {@link CalibrationStub} objects, which contain the basic details
	 * of the calibration, but not the calibration coefficients for the instrument's sensors.
	 * These can be retrieved using the {@link #getCalibrationCoefficients(DataSource, CalibrationStub)} method.</p>
	 * 
	 * @param dataSource A data source 
	 * @param instrumentId The ID of the instrument whose calibrations are to be retrieved
	 * @param startDate The date of the first record in the data file
	 * @param endDate The date of the last record in the instrument file
	 * @return The set of calibrations that occurred 
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws InstrumentException If there is no calibration preceding the start of the data file
	 * @see #GET_CALIBRATIONS_BETWEEN_QUERY
	 * @see #GET_CALIBRATION_BEFORE_QUERY
	 */
	public static List<CalibrationStub> getCalibrationsForFile(DataSource dataSource, long instrumentId, Calendar startDate, Calendar endDate) throws MissingParamException, DatabaseException, InstrumentException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentId, "instrumentId");
		MissingParam.checkMissing(startDate, "startDate");
		MissingParam.checkMissing(endDate, "endDate");
		
		List<CalibrationStub> calibrations = new ArrayList<CalibrationStub>();
		
		Connection conn = null;
		PreparedStatement beforeStmt = null;
		PreparedStatement duringStmt = null;
		ResultSet beforeRecord = null;
		ResultSet duringRecords = null;
		
		try {
			conn = dataSource.getConnection();
			
			beforeStmt = conn.prepareStatement(GET_CALIBRATION_BEFORE_QUERY);
			beforeStmt.setLong(1, instrumentId);
			beforeStmt.setDate(2, new java.sql.Date(DateTimeUtils.setMidnight(startDate).getTime().getTime()));
			beforeRecord = beforeStmt.executeQuery();
			if (!beforeRecord.next()) {
				throw new InstrumentException("There is no calibration available prior to this file");
			} else {
				calibrations.add(new CalibrationStub(beforeRecord.getLong(1), instrumentId, beforeRecord.getDate(2)));
			}
			
			duringStmt = conn.prepareStatement(GET_CALIBRATIONS_BETWEEN_QUERY);
			duringStmt.setLong(1, instrumentId);
			duringStmt.setDate(2, new java.sql.Date(DateTimeUtils.setMidnight(startDate).getTime().getTime()));
			duringStmt.setDate(3, new java.sql.Date(DateTimeUtils.setMidnight(endDate).getTime().getTime()));
			duringRecords = duringStmt.executeQuery();
			while (duringRecords.next()) {
				calibrations.add(new CalibrationStub(duringRecords.getLong(1), instrumentId, duringRecords.getDate(2)));
			}
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while searching for calibrations", e);
		} finally {
			DatabaseUtils.closeResultSets(beforeRecord, duringRecords);
			DatabaseUtils.closeStatements(beforeStmt, duringStmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return calibrations;
	}
}
