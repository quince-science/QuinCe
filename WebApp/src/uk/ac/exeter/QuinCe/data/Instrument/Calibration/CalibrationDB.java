package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Database methods for database actions related to calibrations
 * @author Steve Jones
 *
 */
public abstract class CalibrationDB {

	/**
	 * Statement to add a new calibration to the database
	 */
	private static final String ADD_CALIBRATION_STATEMENT = "INSERT INTO calibration "
			+ "(instrument_id, type, target, deployment_date, coefficients) "
			+ "VALUES (?, ?, ?, ?, ?)";
	
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
			 stmt.setString(5, calibration.getValuesAsDelimitedList());
			 
			 stmt.execute();
		 } catch (SQLException e) {
			 throw new DatabaseException("Error while storing calibration", e);
		 } finally {
			 DatabaseUtils.closeStatements(stmt);
			 DatabaseUtils.closeConnection(conn);
		 }
	}
	
}
