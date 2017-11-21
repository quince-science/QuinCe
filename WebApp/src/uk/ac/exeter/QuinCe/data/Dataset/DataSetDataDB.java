package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class for handling database queries related to the
 * {@code dataset_data} table
 * @author Steve Jones
 *
 */
public class DataSetDataDB {

	/**
	 * Query to get all measurements for a data set
	 */
	private static final String GET_ALL_MEASUREMENTS_QUERY = "SELECT * FROM dataset_data WHERE dataset_id = ?";
	
	/**
	 * The name of the ID column
	 */
	private static final String ID_COL = "id";
	
	/**
	 * The name of the date column
	 */
	private static final String DATE_COL = "date";
	
	/**
	 * The name of the longitude column
	 */
	private static final String LON_COL = "longitude";
	
	/**
	 * The name of the latitude column
	 */
	private static final String LAT_COL = "latitude";
	
	/**
	 * The name of the run type column
	 */
	private static final String RUN_TYPE_COL = "run_type";
	
	/**
	 * The name of the diagnostic values column
	 */
	private static final String DIAGNOSTIC_COL = "diagnostic_values";
	
	/**
	 * The name of the dataset ID column
	 */
	private static final String DATASET_COL = "dataset_id";
	
	/**
	 * Store a data set record in the database.
	 * 
	 * Measurement and calibration records are automatically detected
	 * and stored in the appropriate table.
	 * 
	 * @param conn A database connection
	 * @param record The record to be stored
	 * @param datasetDataStatement A previously generated statement for inserting a record. Can be null.
	 * @return A {@link PreparedStatement} that can be used for storing subsequent records
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DataSetException If a non-measurement record is supplied
	 * @throws DatabaseException If a database error occurs
	 * @throws NoSuchCategoryException If the record's Run Type is not recognised
	 */
	public static PreparedStatement storeRecord(Connection conn, DataSetRawDataRecord record, PreparedStatement datasetDataStatement) throws MissingParamException, DataSetException, DatabaseException, NoSuchCategoryException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(record, "record");

		if (!record.isMeasurement()) {
			throw new DataSetException("Record is not a measurement");
		}
		
		ResultSet createdKeys = null;
		
		try {
			if (null == datasetDataStatement) {
				datasetDataStatement = createInsertRecordStatement(conn, record);
			}
			
			datasetDataStatement.setLong(1, record.getDatasetId());
			datasetDataStatement.setLong(2, DateTimeUtils.dateToLong(record.getDate()));
			datasetDataStatement.setDouble(3, record.getLongitude());
			datasetDataStatement.setDouble(4, record.getLatitude());
			datasetDataStatement.setString(5, record.getRunType());
			datasetDataStatement.setString(6, record.getDiagnosticValuesString());
			
			int currentField = 6;
			SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
			for (SensorType sensorType : sensorConfig.getSensorTypes()) {
				if (sensorType.isUsedInCalculation()) {
					currentField++;
					Double sensorValue = record.getSensorValue(sensorType.getName());
					if (null == sensorValue) {
						datasetDataStatement.setNull(currentField, Types.DOUBLE);
					} else {
						datasetDataStatement.setDouble(currentField, sensorValue);
					}
				}
			}
			
			datasetDataStatement.execute();
			
			CalculationDB calculationDB = CalculationDBFactory.getCalculationDB();
			
			createdKeys = datasetDataStatement.getGeneratedKeys();
			while (createdKeys.next()) {
				calculationDB.createCalculationRecord(conn, createdKeys.getLong(1));
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error storing dataset record", e);
		} finally {
			DatabaseUtils.closeResultSets(createdKeys);
		}
		
		return datasetDataStatement;
	}

	/**
	 * Get all the measurement records for a dataset
	 * @param conn A database connection
	 * @param dataSet The data set
	 * @return The measurement records
     * @throws DatabaseException If a database error occurs
     * @throws MissingParamException If any required parameters are missing
	 */
	public static List<DataSetRawDataRecord> getMeasurements(Connection conn, DataSet dataSet) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(dataSet, "dataSet");
		
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		List<DataSetRawDataRecord> result = new ArrayList<DataSetRawDataRecord>();
		
		int idCol = -1;
		int dateCol = -1;
		int lonCol = -1;
		int latCol = -1;
		int runTypeCol = -1;
		int diagnosticCol = -1;

		SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
		Map<Integer, String> sensorColumns = new HashMap<Integer, String>();
		
		try {
			stmt = conn.prepareStatement(GET_ALL_MEASUREMENTS_QUERY);
			stmt.setLong(1, dataSet.getId());
			
			records = stmt.executeQuery();
			ResultSetMetaData rsmd = records.getMetaData();
			
			while (records.next()) {
				
				// Get the column indices if we haven't already got them
				if (idCol == -1) {
					for (int i = 1; i <= rsmd.getColumnCount(); i++) {
						String columnName = rsmd.getColumnName(i);
						switch (columnName) {
						case ID_COL: {
							idCol = i;
							break;
						}
						case DATE_COL: {
							dateCol = i;
							break;
						}
						case LON_COL: {
							lonCol = i;
							break;
						}
						case LAT_COL: {
							latCol = i;
							break;
						}
						case RUN_TYPE_COL: {
							runTypeCol = i;
							break;
						}
						case DIAGNOSTIC_COL: {
							diagnosticCol = i;
							break;
						}
						case DATASET_COL: {
							// Do nothing
							break;
						}
						default: {
							// This is a sensor field. Get the sensor name from the sensors configuration
							for (SensorType sensorType : sensorConfig.getSensorTypes()) {
								if (sensorType.isUsedInCalculation()) {
									if (columnName.equals(sensorType.getDatabaseFieldName())) {
										sensorColumns.put(i, sensorType.getName());
									}
								}
							}
						}
						}
					}
				}
				
				long id = records.getLong(idCol);
				LocalDateTime date = DateTimeUtils.longToDate(records.getLong(dateCol));
				double longitude = records.getDouble(lonCol);
				double latitude = records.getDouble(latCol);
				String runType = records.getString(runTypeCol);
				RunTypeCategory runTypeCategory = ResourceManager.getInstance().getRunTypeCategoryConfiguration().getCategory(runType);
				String diagnosticValues = records.getString(diagnosticCol);
				
				DataSetRawDataRecord measurement = new DataSetRawDataRecord(dataSet, id, date, longitude, latitude, runType, runTypeCategory);
				measurement.setDiagnosticValues(diagnosticValues);
				
				for (Map.Entry<Integer, String> entry : sensorColumns.entrySet()) {
					measurement.setSensorValue(entry.getValue(), records.getDouble(entry.getKey()));
				}
				
				result.add(measurement);
			}
			
			return result;
			
		} catch (Exception e) {
			throw new DatabaseException("Error while retrieving measurements", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
		}
		
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
		fieldNames.add("longitude");
		fieldNames.add("latitude");
		fieldNames.add("run_type");
		fieldNames.add("diagnostic_values");

		SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
		for (SensorType sensorType : sensorConfig.getSensorTypes()) {
			if (sensorType.isUsedInCalculation()) {
				fieldNames.add(sensorType.getDatabaseFieldName());
			}
		}
		
		return DatabaseUtils.createInsertStatement(conn, "dataset_data", fieldNames, Statement.RETURN_GENERATED_KEYS);
	}
}
