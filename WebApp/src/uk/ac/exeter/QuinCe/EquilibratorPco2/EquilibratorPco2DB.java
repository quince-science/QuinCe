package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Instance of {@link CalculationDB} for underway pCO2
 * @author Steve Jones
 *
 */
public class EquilibratorPco2DB extends CalculationDB {

	/**
	 * The name of the database table
	 */
	private static final String TABLE_NAME = "equilibrator_pco2"; 
	
	/**
	 * The statement to store calculation values
	 */
	private static final String STORE_CALCULATION_VALUES_STATEMENT = "UPDATE "
			+ TABLE_NAME + " SET "
			+ "delta_temperature = ?, true_moisture = ?, ph2o = ?, " // 3
			+ "dried_co2 = ?, calibrated_co2 = ?, pco2_te_dry = ?, " // 6
			+ "pco2_te_wet = ?, fco2_te = ?, fco2 = ? " // 9
			+ "WHERE measurement_id = ?";
	
	/**
	 * The statement to clear calculation values
	 */
	private static final String CLEAR_CALCULATION_VALUES_STATEMENT = "UPDATE "
			+ TABLE_NAME + " SET "
			+ "delta_temperature = NULL, true_moisture = NULL, ph2o = NULL, " // 3
			+ "dried_co2 = NULL, calibrated_co2 = NULL, pco2_te_dry = NULL, " // 6
			+ "pco2_te_wet = NULL, fco2_te = NULL, fco2 = NULL " // 9
			+ "WHERE measurement_id = ?";
	
	@Override
	public String getCalculationTable() {
		return TABLE_NAME;
	}

	@Override
	public void storeCalculationValues(Connection conn, long measurementId, Map<String, Double> values) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(measurementId, "measurementId");
		MissingParam.checkMissing(values, "values");
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(STORE_CALCULATION_VALUES_STATEMENT);
			
			stmt.setDouble(1, values.get("delta_temperature"));
			
			Double trueMoisture = values.get("true_moisture");
			if (null == trueMoisture) {
				stmt.setNull(2, Types.DOUBLE);
			} else {
				stmt.setDouble(2, trueMoisture);
			}

			stmt.setDouble(3, values.get("ph2o"));
			stmt.setDouble(4, values.get("dried_co2"));
			stmt.setDouble(5, values.get("calibrated_co2"));
			stmt.setDouble(6, values.get("pco2_te_dry"));
			stmt.setDouble(7, values.get("pco2_te_wet"));
			stmt.setDouble(8, values.get("fco2_te"));
			stmt.setDouble(9, values.get("fco2"));
			stmt.setLong(10, measurementId);
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Error storing calculations" , e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}

	@Override
	public void clearCalculationValues(Connection conn, long measurementId) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(measurementId, "measurementId");
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(CLEAR_CALCULATION_VALUES_STATEMENT);
			stmt.setLong(1, measurementId);
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Error storing calculations" , e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
}
