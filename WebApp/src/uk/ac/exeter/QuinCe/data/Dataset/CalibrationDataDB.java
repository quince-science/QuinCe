package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

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
 * Class for handling calibration data from within a data set.
 * 
 * <p>
 *   This is likely to be replaced
 *   when the new calibration data handling system is written
 *   (see Github issue #556).
 * </p>
 * 
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
	 * Statement to retrieve all calibration data for a dataset
	 */
	private static final String ALL_CALIBRATION_DATA_QUERY = "SELECT * FROM calibration_data "
			+ "WHERE dataset_id = ?";
	
	/**
	 * Statement to retrieve calibration data for a specific run type for a dataset
	 */
	private static final String SELECTED_CALIBRATION_DATA_QUERY = "SELECT * FROM calibration_data "
			+ "WHERE dataset_id = ? AND run_type = ?";
	
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
				statement = DatabaseUtils.createInsertStatement(conn, "calibration_data", createAllFieldsList());
			}
			
			statement.setLong(1, record.getDatasetId());
			statement.setLong(2, DateTimeUtils.dateToLong(record.getDate()));
			statement.setString(3, record.getRunType());
			statement.setBoolean(4, true);
			statement.setNull(5, Types.VARCHAR);
			
			int currentField = 5;
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
	 * Generate a list of all the fields in the {@code calibration_data} table
	 * @return The field list
	 */
	private static List<String> createAllFieldsList() {
		List<String> fieldNames = new ArrayList<String>();
		
		fieldNames.add("dataset_id");
		fieldNames.add("date");
		fieldNames.add("run_type");
		fieldNames.add("use_record");
		fieldNames.add("use_message");

		SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
		for (SensorType sensorType : sensorConfig.getSensorTypes()) {
			if (sensorType.isCalibratedUsingData()) {
				fieldNames.add(sensorType.getDatabaseFieldName());
			}
		}
		
		return fieldNames;
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
	
	/**
	 * Get the gas standard data for a given data set and standard in CSV format
	 * If {@code standardName} is {@code null}, all gas standards will be included in the results
	 * @param dataSource A data source
	 * @param datasetId The database ID of the data set
	 * @param standardName The name of the standard ({@code null} for all standards)
	 * @return The standards data
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
 	 */
	public static String getCalibrationCSV(DataSource dataSource, long datasetId, String standardName) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkZeroPositive(datasetId, "datasetId");
		
		List<String> calibrationFields = new ArrayList<String>();
		SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
		for (SensorType sensorType : sensorConfig.getSensorTypes()) {
			if (sensorType.isCalibratedUsingData()) {
				calibrationFields.add(sensorType.getName());
			}
		}

		StringBuilder csv = new StringBuilder();
		csv.append("Date,");
		for (int i = 0; i < calibrationFields.size(); i++) {
			csv.append(calibrationFields.get(i));
			if (i < calibrationFields.size() -1) {
				csv.append(',');
			}
		}
		csv.append("\n");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			conn = dataSource.getConnection();
			if (null == standardName) {
				stmt = conn.prepareStatement(ALL_CALIBRATION_DATA_QUERY);
				stmt.setLong(1, datasetId);
			} else {
				stmt = conn.prepareStatement(SELECTED_CALIBRATION_DATA_QUERY);
				stmt.setLong(1, datasetId);
				stmt.setString(2, standardName);
			}
			
			records = stmt.executeQuery();
			
			while (records.next()) {

				// This is only going to be used for the graph for now, so just grab the columns we're interested in
				
				/*
				csv.append(records.getLong(1)); // id
				csv.append(',');
				csv.append(records.getLong(2)); // dataset_id
				csv.append(',');
				*/
				csv.append(records.getLong(3)); // date
				csv.append(',');
				
				/*
				csv.append(records.getString(4)); // run_type
				csv.append(',');
				csv.append(records.getInt(5)); // use_record
				csv.append(',');
				csv.append(records.getString(6)); // use_message
				csv.append(',');
				*/

				for (int i = 0; i < calibrationFields.size(); i++) {
					csv.append(records.getDouble(6 + i + 1));
					if (i < calibrationFields.size() -1) {
						csv.append(',');
					}
				}
				
				csv.append('\n');
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error retrieving calibration data", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return csv.toString();
	}
}
