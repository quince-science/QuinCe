package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDBFactory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Methods for manipulating data sets in the database
 * @author Steve Jones
 *
 */
public class DataSetDB {

	/**
	 * Query to get the defined data sets for a given instrument
	 * @see #getDataSets(DataSource, long)
	 */
	private static final String GET_DATASETS_QUERY = "SELECT "
			+ "id, instrument_id, name, start, end, status, properties, last_touched "
		    + "FROM dataset WHERE instrument_id = ? ORDER BY start ASC";
	
	/**
	 * Statement to add a new data set into the database
	 * @see #addDataSet(DataSource, DataSet)
	 */
	private static final String ADD_DATASET_STATEMENT = "INSERT INTO dataset "
			+ "(instrument_id, name, start, end, status, properties, last_touched) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Query to get a single data set by its ID
	 * @see #getDataSet(DataSource, long)
	 */
	private static final String GET_DATASET_QUERY = "SELECT "
			+ "id, instrument_id, name, start, end, status, properties, last_touched "
			+ "FROM dataset WHERE id = ?";
	
	/**
	 * Statement to set a data set's status
	 * @see #setDatasetStatus(DataSource, DataSet, int)
	 */
	private static final String SET_STATUS_STATEMENT = "UPDATE dataset "
			+ "SET status = ? WHERE id = ?";
	
	/**
	 * Statement to delete all records for a given dataset
	 */
	private static final String DELETE_DATASET_QUERY = "DELETE FROM dataset_data "
			+ "WHERE dataset_id = ?";
	
	/**
	 * Get the list of data sets defined for a given instrument
	 * @param dataSource A data source
	 * @param instrumentId The instrument's database ID
	 * @return The list of data sets
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 */
	public static List<DataSet> getDataSets(DataSource dataSource, long instrumentId) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkZeroPositive(instrumentId, "instrumentId");
		
		List<DataSet> result = new ArrayList<DataSet>();
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_DATASETS_QUERY);
			stmt.setLong(1, instrumentId);
			
			records = stmt.executeQuery();
			
			while (records.next()) {
				result.add(dataSetFromRecord(records));
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving data sets", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}
	
	/**
	 * Create a DataSet object from a search result
	 * @param record The search result
	 * @return The Data Set object
	 * @throws SQLException If the data cannot be extracted from the result
	 */
	private static DataSet dataSetFromRecord(ResultSet record) throws SQLException {
		
		long id = record.getLong(1);
		long instrumentId = record.getLong(2);
		String name = record.getString(3);
		LocalDateTime start = DateTimeUtils.longToDate(record.getLong(4));
		LocalDateTime end = DateTimeUtils.longToDate(record.getLong(5));
		int status = record.getInt(6);
		Properties properties = null;
		LocalDateTime lastTouched = DateTimeUtils.longToDate(record.getLong(8));
		
		return new DataSet(id, instrumentId, name, start, end, status, properties, lastTouched);
	}
	
	/**
	 * Store a new data set in the database.
	 * 
	 * The created data set's ID is stored in the provided {@link DataSet} object
	 * @param dataSource A data source
	 * @param dataSet The data set to be stored
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 */
	public static void addDataSet(DataSource dataSource, DataSet dataSet) throws DatabaseException, MissingParamException {
		
		// TODO Validate the data set
		// TODO Make sure it's not a duplicate of an existing data set
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(dataSet, "dataSet");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet addedKeys = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(ADD_DATASET_STATEMENT, PreparedStatement.RETURN_GENERATED_KEYS);
			
			stmt.setLong(1, dataSet.getInstrumentId());
			stmt.setString(2, dataSet.getName());
			stmt.setLong(3, DateTimeUtils.dateToLong(dataSet.getStart()));
			stmt.setLong(4, DateTimeUtils.dateToLong(dataSet.getEnd()));
			stmt.setLong(5, dataSet.getStatus());
			stmt.setNull(6, Types.VARCHAR);
			stmt.setLong(7, DateTimeUtils.dateToLong(LocalDateTime.now()));
			
			stmt.execute();
			
			// Add the database ID to the database object
			addedKeys = stmt.getGeneratedKeys();
			addedKeys.next();
			dataSet.setId(addedKeys.getLong(1));
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while adding data set", e);
		} finally {
			DatabaseUtils.closeResultSets(addedKeys);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Get a data set using its database ID
	 * @param dataSource A data source
	 * @param id The data set's id
	 * @return The data set
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 * @throws RecordNotFoundException If the data set does not exist
	 */
	public static DataSet getDataSet(DataSource dataSource, long id) throws DatabaseException, MissingParamException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		
		DataSet result = null;
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			result = getDataSet(conn, id);
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving data sets", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}
	
	/**
	 * Get a data set using its database ID
	 * @param conn A database connection
	 * @param id The data set's id
	 * @return The data set
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 * @throws RecordNotFoundException If the data set does not exist
	 */
	public static DataSet getDataSet(Connection conn, long id) throws DatabaseException, MissingParamException, RecordNotFoundException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkZeroPositive(id, "id");
		
		DataSet result = null;
		
		PreparedStatement stmt = null;
		ResultSet record = null;
		
		try {
			stmt = conn.prepareStatement(GET_DATASET_QUERY);
			stmt.setLong(1, id);
			
			record = stmt.executeQuery();
			
			if (!record.next()) {
				throw new RecordNotFoundException("Data set does not exist", "dataset", id);
			} else {
				result = dataSetFromRecord(record);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving data sets", e);
		} finally {
			DatabaseUtils.closeResultSets(record);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return result;
	}
	
	/**
	 * Set the status of a {@link DataSet}.
	 * @param dataSource A data source
	 * @param dataSet The data set
	 * @param status The new status
	 * @throws MissingParamException If any required parameters are missing
	 * @throws InvalidDataSetStatusException If the status is invalid
	 * @throws DatabaseException If a database error occurs
	 */
	public static void setDatasetStatus(DataSource dataSource, DataSet dataSet, int status) throws MissingParamException, InvalidDataSetStatusException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			setDatasetStatus(conn, dataSet, status);
		} catch (SQLException e) {
			throw new DatabaseException("Error while setting dataset status", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	
	/**
	 * Set the status of a {@link DataSet}.
	 * @param conn A database connection
	 * @param dataSet The data set
	 * @param status The new status
	 * @throws MissingParamException If any required parameters are missing
	 * @throws InvalidDataSetStatusException If the status is invalid
	 * @throws DatabaseException If a database error occurs
	 */
	public static void setDatasetStatus(Connection conn, DataSet dataSet, int status) throws MissingParamException, InvalidDataSetStatusException, DatabaseException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkMissing(dataSet, "dataSet");
		
		if (!DataSet.validateStatus(status)) {
			throw new InvalidDataSetStatusException(status);
		}
		
		PreparedStatement stmt = null;
		
		try {
			stmt = conn.prepareStatement(SET_STATUS_STATEMENT);
			
			stmt.setInt(1, status);
			stmt.setLong(2, dataSet.getId());
			
			stmt.execute();
			
			dataSet.setStatus(status);
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while setting data set status", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
	
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
	 */
	public static PreparedStatement storeRecord(Connection conn, DataSetRawDataRecord record, PreparedStatement datasetDataStatement) throws MissingParamException, DataSetException, DatabaseException {
		
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
			datasetDataStatement.setString(5, record.getRunType().getName());
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
			
			CalculationDB calculationDB = CalculationDBFactory.getCalculatioDB();
			
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
			stmt = conn.prepareStatement(DELETE_DATASET_QUERY);
			stmt.setLong(1, dataSet.getId());
			
			stmt.execute();
		} catch (SQLException e) {
			throw new DatabaseException("Error while deleting dataset data", e);
		} finally {
			DatabaseUtils.closeStatements(stmt);
		}
	}
}
