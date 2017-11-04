package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class for handling calibration data from within a data set
 * @author Steve Jones
 *
 */
public class CalibrationDataDB {

	/**
	 * Statement to delete all records for a given dataset
	 */
	private static final String DELETE_CALIBRATION_DATA_QUERY = "DELETE FROM calibration_data "
			+ "WHERE dataset_id = ?";

	
	/**
	 * Store a data set record in the database.
	 * 
	 * Measurement and calibration records are automatically detected
	 * and stored in the appropriate table.
	 * 
	 * @param conn A database connection
	 * @param record The record to be stored
	 * @param statement A previously generated statement for inserting a record. Can be null.
	 * @return A {@link PreparedStatement} that can be used for storing subsequent records
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DataSetException If a non-measurement record is supplied
	 * @throws DatabaseException If a database error occurs
	 * @throws NoSuchCategoryException If the record's Run Type is not recognised
	 */
	public static PreparedStatement storeCalibrationRecord(Connection conn, DataSetRawDataRecord record, PreparedStatement statement) throws MissingParamException, DataSetException, DatabaseException, NoSuchCategoryException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(record, "record");

		if (!record.isCalibration()) {
			throw new DataSetException("Record is not a calibration record");
		}
		
		try {
			if (null == statement) {
				statement = createInsertRecordStatement(conn, record);
			}
			
			statement.setLong(1, record.getDatasetId());
			statement.setLong(2, DateTimeUtils.dateToLong(record.getDate()));
			statement.setString(3, record.getRunType());
			
			int currentField = 3;
			SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
			for (SensorType sensorType : sensorConfig.getSensorTypes()) {
				if (sensorType.isCalibratedUsingData()) {
					currentField++;
					Double sensorValue = record.getSensorValue(sensorType.getName());
					if (null == sensorValue) {
						statement.setNull(currentField, Types.DOUBLE);
					} else {
						statement.setDouble(currentField, sensorValue);
					}
				}
			}
			
			statement.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Error storing dataset record", e);
		}
		
		return statement;
	}

	/**
	 * Create a statement to insert a new dataset record in the database
	 * @param conn A database connection
	 * @param record A dataset record
	 * @return The statement
	 * @throws MissingParamException If any required parameters are missing
	 * @throws SQLException If the statement cannot be created
	 */
	private static PreparedStatement createInsertRecordStatement(Connection conn, DataSetRawDataRecord record) throws MissingParamException, SQLException {
		
		List<String> fieldNames = new ArrayList<String>();
				
		fieldNames.add("dataset_id");
		fieldNames.add("date");
		fieldNames.add("run_type");

		SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
		for (SensorType sensorType : sensorConfig.getSensorTypes()) {
			if (sensorType.isCalibratedUsingData()) {
				fieldNames.add(sensorType.getDatabaseFieldName());
			}
		}
		
		return DatabaseUtils.createInsertStatement(conn, "calibration_data", fieldNames);
	}
	
	/**
	 * Delete all records for a given data set
	 * @param conn A database connection
	 * @param dataSet The data set
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public static void deleteDatasetData(Connection conn, DataSet dataSet) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(dataSet, "dataSet");
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(DELETE_CALIBRATION_DATA_QUERY);
			stmt.setLong(1, dataSet.getId());
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Error while deleting dataset data", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
}
