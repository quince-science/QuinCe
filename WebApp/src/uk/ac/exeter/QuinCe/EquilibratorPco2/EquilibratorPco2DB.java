package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

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
	
	/**
	 * The query to retrieve calculation values
	 */
	private static final String GET_CALCULATION_VALUES_STATEMENT = "SELECT "
			+ "delta_temperature, true_moisture, ph2o, " // 3
			+ "dried_co2, calibrated_co2, pco2_te_dry, " // 6
			+ "pco2_te_wet, fco2_te, fco2 " // 9
			+ "FROM " + TABLE_NAME
			+ " WHERE measurment_id = ?";
			
	
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
	public Map<String, Double> getCalculationValues(Connection conn, long measurementId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(measurementId, "measurementId");

		Map<String, Double> values = new HashMap<String, Double>();
		
		PreparedStatement stmt = null;
		ResultSet record = null;
		
		try {
			stmt = conn.prepareStatement(GET_CALCULATION_VALUES_STATEMENT);
			stmt.setLong(1, measurementId);
			
			record = stmt.executeQuery();
			
			if (!record.next()) {
				throw new RecordNotFoundException("Calculation data record not found", TABLE_NAME, measurementId);
			} else {
				values.put("delta_temperature", record.getDouble(1));
				values.put("true_moisture", record.getDouble(2));
				values.put("ph2o", record.getDouble(3));
				values.put("dried_co2", record.getDouble(4));
				values.put("calibrated_co2", record.getDouble(5));
				values.put("pco2_te_dry", record.getDouble(6));
				values.put("pco2_te_wet", record.getDouble(7));
				values.put("fco2_te", record.getDouble(8));
				values.put("fco2", record.getDouble(9));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error storing calculations" , e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return values;
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
