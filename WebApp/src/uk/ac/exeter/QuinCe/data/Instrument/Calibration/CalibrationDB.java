package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Database methods for database actions related to calibrations
 * @author Steve Jones
 *
 */
public abstract class CalibrationDB {

	/**
	 * Statement to add a new calibration to the database
	 * @see #addCalibration(DataSource, Calibration)
	 */
	private static final String ADD_CALIBRATION_STATEMENT = "INSERT INTO calibration "
			+ "(instrument_id, type, target, deployment_date, coefficients) "
			+ "VALUES (?, ?, ?, ?, ?)";
	
	/**
	 * Query for finding recent calibrations.
	 * @see #getCurrentCalibrations(DataSource, long)
	 */
	private static final String GET_RECENT_CALIBRATIONS_QUERY = "SELECT "
			+ "target, deployment_date, coefficients FROM calibration WHERE "
			+ "instrument_id = ? AND type = ? ORDER BY deployment_date DESC";
			
	
	/**
	 * Empty constructor. These classes must be singletons so the
	 * abstract methods can be declared. Individual instances can
	 * be retrieved from the concrete classes 
	 */
	protected CalibrationDB() {
		// Do nothing
	}
	
	/**
	 * Add a new calibration to the database
	 * @param dataSource A data source
	 * @param calibration The calibration
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public void addCalibration(DataSource dataSource, Calibration calibration) throws MissingParamException, DatabaseException {
		 MissingParam.checkMissing(dataSource, "dataSource");
		 MissingParam.checkMissing(calibration, "calibration");
		 
		 Connection conn = null;
		 PreparedStatement stmt = null;
		 
		 try {
			 conn = dataSource.getConnection();
			 stmt = conn.prepareStatement(ADD_CALIBRATION_STATEMENT);
			 stmt.setLong(1, calibration.getInstrumentId());
			 stmt.setString(2, calibration.getType());
			 stmt.setString(3, calibration.getTarget());
			 stmt.setLong(4, calibration.getDeploymentDateAsMillis());
			 stmt.setString(5, calibration.getCoefficientsAsDelimitedList());
			 
			 stmt.execute();
		 } catch (SQLException e) {
			 throw new DatabaseException("Error while storing calibration", e);
		 } finally {
			 DatabaseUtils.closeStatements(stmt);
			 DatabaseUtils.closeConnection(conn);
		 }
	}
	
	/**
	 * Get the most recent calibrations for each target 
	 * @param dataSource A data source
	 * @param instrumentId The instrument ID
	 * @return The calibrations
	 * @throws CalibrationException If the calibrations are internally inconsistent
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If any required records are missing
	 * @throws MissingParamException If any internal calls are missing required parameters
	 */
	public List<Calibration> getCurrentCalibrations(DataSource dataSource, long instrumentId) throws CalibrationException, DatabaseException, MissingParamException, RecordNotFoundException {
		
		List<String> targets = getTargets(dataSource, instrumentId);
		
		List<Calibration> result = new ArrayList<Calibration>(targets.size());
		for (String target : targets) {
			result.add(CalibrationFactory.createCalibration(dataSource, instrumentId, getCalibrationType(), target));
		}
		
		TreeSet<String> foundTargets = new TreeSet<String>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_RECENT_CALIBRATIONS_QUERY);
			stmt.setLong(1, instrumentId);
			stmt.setString(2, getCalibrationType());
			
			records = stmt.executeQuery();
			while (foundTargets.size() < targets.size() && records.next()) {
				String target = records.getString(1);
				int targetIndex = targets.indexOf(target);
				if (targetIndex == -1) {
					throw new UnknownCalibrationTargetException(instrumentId, getCalibrationType(), target);
				}
				
				if (!foundTargets.contains(target)) {
					Date calibrationDate = new Date(records.getLong(2));
					List<Double> coefficients = StringUtils.delimitedToDoubleList(records.getString(3));
					
					Calibration currentCalibration = result.get(targetIndex);
					currentCalibration.setDeploymentDateAsDate(calibrationDate);
					currentCalibration.setCoefficients(coefficients);
					
					foundTargets.add(target);
				}
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving calibrations", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}

		return result;
	}
	
	/**
	 * Get the list of calibration possible targets for a given instrument
	 * @param dataSource A data source
	 * @param instrumentId The instrument's database ID
	 * @return The targets
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If no gas standard run types are found
	 */
	public abstract List<String> getTargets(DataSource dataSource, long instrumentId) throws MissingParamException, DatabaseException, RecordNotFoundException;
	
	/**
	 * Get the calibration type for database actions
	 * @return The calibration type
	 */
	public abstract String getCalibrationType();
}
