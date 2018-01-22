package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for storing and retrieving external standards information from the database
 * @author Steve Jones
 *
 */
public class ExternalStandardDB extends CalibrationDB {

	/**
	 * Query to get the most recent standards for an instrument before a given date
	 */
	public static final String GET_STANDARD_SET_QUERY = "SELECT "
		    + "c1.target, c1.deployment_date, c1.coefficients, c1.class "
			+ "FROM calibration c1 INNER JOIN "
		    + "(SELECT MAX(deployment_date) deployment_date, target "
			+ "FROM calibration WHERE deployment_date < ? GROUP BY target) "
		    + "AS c2 ON c1.target = c2.target AND c1.deployment_date = c2.deployment_date WHERE instrument_id = ?";
	
	/**
	 * The calibration type for external standards
	 */
	public static final String EXTERNAL_STANDARD_CALIBRATION_TYPE = "EXTERNAL_STANDARD";
	
	/**
	 * The singleton instance of the class
	 */
	private static ExternalStandardDB instance = null;
	
	/**
	 * Basic constructor
	 */
	public ExternalStandardDB() {
		super();
	}
	
	/**
	 * Retrieve the singleton instance of the class
	 * @return The singleton
	 */
	public static ExternalStandardDB getInstance() {
		if (null == instance) {
			instance = new ExternalStandardDB();
		}
		
		return instance;
	}
	
	/**
	 * Destroy the singleton instance
	 */
	public static void destroy() {
		instance = null;
	}
	
	@Override
	public List<String> getTargets(Connection conn, long instrumentId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		List<String> standardNames = InstrumentDB.getRunTypes(conn, instrumentId, RunTypeCategory.EXTERNAL_STANDARD_CATEGORY.getCode());
		if (standardNames.size() == 0) {
			throw new RecordNotFoundException("No external standard names found for instrument " + instrumentId);
		}

		return Collections.unmodifiableList(standardNames);
	}

	/**
	 * Retrieve a CalibrationSet containing the external standards deployed immediately before the specified date
	 * @param conn A database connection
	 * @param instrumentId The instrument for which the standards should be retrieved
	 * @param date The date limit
	 * @return The external standards
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 * @throws RecordNotFoundException If the instrument does not exist
	 */
	public CalibrationSet getStandardsSet(Connection conn, long instrumentId, LocalDateTime date) throws DatabaseException, MissingParamException, RecordNotFoundException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(instrumentId, "instrumentId");
		MissingParam.checkMissing(date, "date");
		
		CalibrationSet result = new CalibrationSet(instrumentId, EXTERNAL_STANDARD_CALIBRATION_TYPE, getTargets(conn, instrumentId));
		
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			
			stmt = conn.prepareStatement(GET_STANDARD_SET_QUERY);
			stmt.setLong(1, DateTimeUtils.dateToLong(date));
			stmt.setLong(2, instrumentId);
			
			records = stmt.executeQuery();
			while (records.next()) {
				String target = records.getString(1);
				LocalDateTime standardDate = DateTimeUtils.longToDate(records.getLong(2));
				String coefficients = records.getString(3);
				String className = records.getString(4);
				result.add(CalibrationFactory.createCalibration(EXTERNAL_STANDARD_CALIBRATION_TYPE, className, instrumentId, standardDate, target, coefficients));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving standards set", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeResultSets(records);
		}
		
		return result;
	}
	
	@Override
	public String getCalibrationType() {
		return EXTERNAL_STANDARD_CALIBRATION_TYPE;
	}
}
