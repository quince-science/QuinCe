package uk.ac.exeter.QuinCe.data.Instrument;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Database methods dealing with instruments
 * @author Steve Jones
 *
 */
public class InstrumentDB {

	////////// *** CONSTANTS *** ///////////////
	
	/**
	 * Statement for inserting an instrument record
	 */
	private static final String CREATE_INSTRUMENT_STATEMENT = "INSERT INTO instrument ("
			+ "owner, name," // 2
			+ "pre_flushing_time, post_flushing_time, minimum_water_flow" // 5
			+ ") VALUES (?, ?, ?, ?, ?)";
	
	/**
	 * Statement for inserting a file definition record
	 */
	private static final String CREATE_FILE_DEFINITION_STATEMENT = "INSERT INTO file_definition ("
			+ "instrument_id, description, column_separator, " // 3
			+ "header_type, header_lines, header_end_string, " // 6
			+ "column_header_rows, column_count, " // 8
			+ "lon_format, lon_value_col, lon_hemisphere_col, " // 11
			+ "lat_format, lat_value_col, lat_hemisphere_col, " // 14
			+ "date_time_col, date_time_props, date_col, date_props, " // 18
			+ "hours_from_start_col, hours_from_start_props, " // 20
			+ "jday_time_col, jday_col, year_col, month_col, day_col, " // 25
			+ "time_col, time_props, hour_col, minute_col, second_col" // 30
			+ ") VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Statement for inserting a file column definition record
	 */
	private static final String CREATE_FILE_COLUMN_STATEMENT = "INSERT INTO file_column ("
			+ "file_definition_id, file_column, primary_sensor, sensor_type, " // 4
			+ "sensor_name, value_column, depends_question_answer, " // 7
			+ "missing_value, post_calibrated" // 9
			+ ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Statement for inserting run types
	 */
	private static final String CREATE_RUN_TYPE_STATEMENT = "INSERT INTO run_type ("
			+ "file_id, run_name, category_code" // 3
			+ ") VALUES (?, ?, ?)";
	
	/**
	 * Query for retrieving the list of instruments owned by a particular user
	 */
	private static final String GET_INSTRUMENT_LIST_QUERY = "SELECT i.id, i.name, SUM(c.post_calibrated) "
			+ "FROM instrument AS i "
			+ "INNER JOIN file_definition AS d ON i.id = d.instrument_id "
			+ "INNER JOIN file_column AS c ON d.id = c.file_definition_id "
			+ "WHERE i.owner = ? "
			+ "GROUP BY i.id";

	/**
	 * Query to get all the run types of a given run type category
	 */
	private static final String GET_RUN_TYPES_QUERY = "SELECT CONCAT(f.description, ': ', r.run_name) AS run_type "
			+ "FROM file_definition AS f INNER JOIN run_type AS r ON f.id = r.file_id "
			+ "WHERE f.instrument_id = ? AND category_code = ? ORDER BY run_type";
	
	/**
	 * Query to get the list of sensors that require calibration for a given instrument
	 */
	private static final String GET_CALIBRATABLE_SENSORS_QUERY = "SELECT CONCAT(f.description, ': ', c.sensor_name) AS sensor"
			+ "FROM file_definition AS f INNER JOIN file_column AS c ON c.file_definition_id = f.id "
			+ "WHERE f.instrument_id = ? AND c.post_calibrated = true ORDER BY sensor";
	
	/**
	 * Store a new instrument in the database
	 * @param dataSource A data source
	 * @param instrument The instrument
	 * @throws MissingParamException If any required parameters are missing
	 * @throws InstrumentException If the Instrument object is invalid
	 * @throws DatabaseException If a database error occurs
	 * @throws IOException If any of the data cannot be converted for storage in the database
	 */
	public static void storeInstrument(DataSource dataSource, Instrument instrument) throws MissingParamException, InstrumentException, DatabaseException, IOException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(instrument, "instrument");
		
		// Validate the instrument. Will throw an exception
		instrument.validate(false);
		
		Connection conn = null;
		PreparedStatement instrumentStatement = null;
		ResultSet instrumentKey = null;
		List<PreparedStatement> subStatements = new ArrayList<PreparedStatement>();
		List<ResultSet> fileDefinitionKeys = new ArrayList<ResultSet>();
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			// Create the instrument record
			instrumentStatement = makeCreateInstrumentStatement(conn, instrument);
			instrumentStatement.execute();
			instrumentKey = instrumentStatement.getGeneratedKeys();
			if (!instrumentKey.next()) {
				throw new DatabaseException("Instrument record was not created in the database");
			} else {
				// Store the database IDs for all the file definitions
				Map<String, Long> fileDefinitionIds = new HashMap<String, Long>(instrument.getFileDefinitions().size());
				
				long instrumentId = instrumentKey.getLong(1);
				instrument.setDatabaseId(instrumentId);
				
				// Now store the file definitions
				for (FileDefinition file : instrument.getFileDefinitions()) {
					PreparedStatement fileStatement = makeCreateFileDefinitionStatement(conn, file, instrumentId);
					subStatements.add(fileStatement);
					
					fileStatement.execute();
					ResultSet fileKey = fileStatement.getGeneratedKeys();
					fileDefinitionKeys.add(fileKey);
					
					if (!fileKey.next()) {
						throw new DatabaseException("File Definition record was not created in the database");
					} else {
						long fileId = fileKey.getLong(1); 
						fileDefinitionIds.put(file.getFileDescription(), fileId);

						// Run Types
						for (Map.Entry<String, RunTypeCategory> entry : file.getRunTypes().entrySet()) {
							PreparedStatement runTypeStatement = conn.prepareStatement(CREATE_RUN_TYPE_STATEMENT);
							runTypeStatement.setLong(1, fileId);
							runTypeStatement.setString(2, entry.getKey());
							runTypeStatement.setString(3, entry.getValue().getCode());
							
							runTypeStatement.execute();
							subStatements.add(runTypeStatement);
						}
					}
				}
				
				// Sensor assignments
				int databaseColumn = -1;
				
				for (Map.Entry<SensorType, Set<SensorAssignment>> sensorAssignmentsEntry : instrument.getSensorAssignments().entrySet()) {
					
					SensorType sensorType = sensorAssignmentsEntry.getKey();
					
					for (SensorAssignment assignment : sensorAssignmentsEntry.getValue()) {
						databaseColumn++;
						assignment.setDatabaseColumn(databaseColumn);
						
						PreparedStatement fileColumnStatement = conn.prepareStatement(CREATE_FILE_COLUMN_STATEMENT);
						fileColumnStatement.setLong(1, fileDefinitionIds.get(assignment.getDataFile()));
						fileColumnStatement.setInt(2, assignment.getColumn());
						fileColumnStatement.setBoolean(3, assignment.isPrimary());
						fileColumnStatement.setString(4, sensorType.getName());
						fileColumnStatement.setString(5, assignment.getSensorName());
						fileColumnStatement.setInt(6, databaseColumn);
						fileColumnStatement.setBoolean(7, assignment.getDependsQuestionAnswer());
						fileColumnStatement.setString(8, assignment.getMissingValue());
						fileColumnStatement.setBoolean(9, assignment.getPostCalibrated());
						
						fileColumnStatement.execute();
						subStatements.add(fileColumnStatement);						
					}
				}
			}
			
			conn.commit();
		} catch (SQLException e) {
			boolean rollbackOK = true;
			
			try {
				conn.rollback();
			} catch (SQLException e2) {
				rollbackOK = false;
			}
			
			throw new DatabaseException("Exception while storing instrument", e, rollbackOK);
		} finally {
			DatabaseUtils.closeResultSets(fileDefinitionKeys);
			DatabaseUtils.closeStatements(subStatements);
			DatabaseUtils.closeResultSets(instrumentKey);
			DatabaseUtils.closeStatements(instrumentStatement);
			DatabaseUtils.closeConnection(conn);
		}
	}

	/**
	 * Make the statement used to create an instrument record in the database
	 * @param conn A database connection
	 * @param instrument The instrument
	 * @return The database statement
	 * @throws SQLException If an error occurs while building the statement
	 */
	private static PreparedStatement makeCreateInstrumentStatement(Connection conn, Instrument instrument) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(CREATE_INSTRUMENT_STATEMENT, Statement.RETURN_GENERATED_KEYS);
		stmt.setLong(1, instrument.getOwnerId()); // owner
		stmt.setString(2, instrument.getName()); // name
		stmt.setInt(3, instrument.getPreFlushingTime()); // pre_flushing_time
		stmt.setInt(4, instrument.getPostFlushingTime()); // post_flushing_time
		stmt.setInt(5, instrument.getMinimumWaterFlow()); // minimum_water_flow
		
		return stmt;
	}
	
	/**
	 * Create a statement for adding a file definition to the database
	 * @param conn A database connection
	 * @param file The file definition
	 * @param instrumentId The database ID of the instrument to which the file belongs
	 * @return The statement
	 * @throws SQLException If the statement cannot be built
	 * @throws IOException If any Properties objects cannot be serialized into Strings for storage
	 */
	private static PreparedStatement makeCreateFileDefinitionStatement(Connection conn, FileDefinition file, long instrumentId) throws SQLException, IOException {
		
		PreparedStatement stmt = conn.prepareStatement(CREATE_FILE_DEFINITION_STATEMENT, Statement.RETURN_GENERATED_KEYS);
	
		stmt.setLong(1, instrumentId); // instrument_id
		stmt.setString(2, file.getFileDescription()); // description 
		stmt.setString(3, file.getSeparator()); // separator
		stmt.setInt(4, file.getHeaderType()); // header_type
		
		if (file.getHeaderType() == FileDefinition.HEADER_TYPE_LINE_COUNT) {
			stmt.setInt(5, file.getHeaderLines()); // header_lines
			stmt.setNull(6, Types.VARCHAR); // header_end_string
		} else {
			stmt.setNull(5, Types.INTEGER); // header_lines
			stmt.setString(6, file.getHeaderEndString()); // header_end_string
		}
		
		stmt.setInt(7, file.getColumnHeaderRows()); // column_header_rows
		stmt.setInt(8, file.getColumnCount()); // column_count
		
		addPositionAssignment(stmt, file.getLongitudeSpecification(), 9, 10, 11); // longitude
		addPositionAssignment(stmt, file.getLatitudeSpecification(), 12, 13, 14); // latitude
		
		DateTimeSpecification dateTimeSpec = file.getDateTimeSpecification();
		addDateTimeAssignment(stmt, 15, 16, DateTimeSpecification.DATE_TIME, dateTimeSpec); // date_time_col
		addDateTimeAssignment(stmt, 17, 18, DateTimeSpecification.DATE, dateTimeSpec); // date
		addDateTimeAssignment(stmt, 19, 20, DateTimeSpecification.HOURS_FROM_START, dateTimeSpec); // hours_from_start
		addDateTimeAssignment(stmt, 21, -1, DateTimeSpecification.JDAY_TIME, dateTimeSpec); // jday_time_col
		addDateTimeAssignment(stmt, 22, -1, DateTimeSpecification.JDAY, dateTimeSpec); // jday_col
		addDateTimeAssignment(stmt, 23, -1, DateTimeSpecification.YEAR, dateTimeSpec); // year_col
		addDateTimeAssignment(stmt, 24, -1, DateTimeSpecification.MONTH, dateTimeSpec); // jmonth_col
		addDateTimeAssignment(stmt, 25, -1, DateTimeSpecification.DAY, dateTimeSpec); // jday_col
		addDateTimeAssignment(stmt, 26, 27, DateTimeSpecification.TIME, dateTimeSpec); // time_col
		addDateTimeAssignment(stmt, 28, -1, DateTimeSpecification.HOUR, dateTimeSpec); // hour_col
		addDateTimeAssignment(stmt, 29, -1, DateTimeSpecification.MINUTE, dateTimeSpec); // minute_col
		addDateTimeAssignment(stmt, 30, -1, DateTimeSpecification.SECOND, dateTimeSpec); // second_col
		
		return stmt;
	}
	
	/**
	 * Add a position assignment fields to a statement for inserting a file definition
	 * @param stmt The file definition statement
	 * @param posSpec The position specification
	 * @param formatIndex The index in the statement of the format field
	 * @param valueIndex The index in the statement of the value field
	 * @param hemisphereIndex The index in the statement of the hemisphere field
	 * @throws SQLException If adding the assignment fails
	 */
	private static void addPositionAssignment(PreparedStatement stmt, PositionSpecification posSpec, int formatIndex, int valueIndex, int hemisphereIndex) throws SQLException {
		stmt.setInt(formatIndex, posSpec.getFormat()); // pos_format
		stmt.setInt(valueIndex, posSpec.getValueColumn()); // pos_value_col
		
		if (posSpec.hemisphereRequired()) {
			stmt.setInt(hemisphereIndex, posSpec.getHemisphereColumn()); // pos_hemisphere_col
		} else {
			stmt.setInt(hemisphereIndex, -1); // pos_hemisphere_col
		}
	}
	
	/**
	 * Add a date/time column assignment to a statement for inserting a file definition
	 * @param stmt The file definition statement
	 * @param stmtColumnIndex The index in the statement to be set
	 * @param stmtPropsIndex The index in the statement to be set. If no properties are to be stored, set to -1
	 * @param assignmentId The required date/time assignment
	 * @param dateTimeSpec The file's date/time specification
	 * @throws SQLException If adding the assignment fails
	 * @throws IOException If the Properties object cannot be serialized into a String
	 */
	private static void addDateTimeAssignment(PreparedStatement stmt, int stmtColumnIndex, int stmtPropsIndex, int assignmentId, DateTimeSpecification dateTimeSpec) throws SQLException, IOException {
		
		int column = -1;
		Properties properties = null;
		
		DateTimeColumnAssignment assignment = dateTimeSpec.getAssignment(assignmentId);
		if (null != assignment) {
			if (assignment.isAssigned()) {
				column = assignment.getColumn();
				properties = assignment.getProperties();
			}
		}
		
		if (column == -1) {
			stmt.setInt(stmtColumnIndex, -1);
			if (stmtPropsIndex != -1) {
				stmt.setNull(stmtPropsIndex, Types.VARCHAR);
			}
		} else {
			stmt.setInt(stmtColumnIndex, column);

			if (null == properties && stmtPropsIndex != 1) {
				stmt.setNull(stmtPropsIndex, Types.VARCHAR);
			} else {
				StringWriter writer = new StringWriter();
				properties.store(writer, null);
				stmt.setString(stmtPropsIndex, writer.toString());
			}
		}
	}
	
	/**
	 * Returns a list of instruments owned by a given user.
	 * The list contains {@link InstrumentStub} objects, which just contain
	 * the details required for lists of instruments in the UI.
	 * 
	 * The list is ordered by the name of the instrument.
	 * 
	 * @param dataSource A data source
	 * @param owner The owner whose instruments are to be listed
	 * @return The list of instruments
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurred
	 */
	public static List<InstrumentStub> getInstrumentList(DataSource dataSource, User owner) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(owner, "owner");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet instruments = null;
		List<InstrumentStub> instrumentList = new ArrayList<InstrumentStub>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_INSTRUMENT_LIST_QUERY);
			stmt.setLong(1, owner.getDatabaseID());
			
			instruments = stmt.executeQuery();
			while (instruments.next()) {
				boolean hasCalibratableSensors = (instruments.getInt(3) > 0);
				InstrumentStub record = new InstrumentStub(instruments.getLong(1), instruments.getString(2), hasCalibratableSensors);
				instrumentList.add(record);
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving instrument list", e);
		} finally {
			DatabaseUtils.closeResultSets(instruments);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return instrumentList;
	}

	/**
	 * Determine whether an instrument with a given name and owner exists
	 * @param dataSource A data source
	 * @param owner The owner
	 * @param name The instrument name
	 * @return {@code true} if the instrument exists; {@code false} if it does not
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public static boolean instrumentExists(DataSource dataSource, User owner, String name) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(owner, "owner");
		MissingParam.checkMissing(name, "name");
		
		boolean exists = false;
		
		for (InstrumentStub instrument : getInstrumentList(dataSource, owner)) {
			if (instrument.getName().equalsIgnoreCase(name)) {
				exists = true;
				break;
			}
		}
		
		return exists;
	}
	
	/**
	 * Retrieve a complete {@link Instrument} from the database
	 * @param dataSource A data source
	 * @param instrumentID The instrument's database ID
	 * @return The instrument object
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 * @throws RecordNotFoundException If the instrument ID does not exist
	 */
	public static Instrument getInstrument(DataSource dataSource, long instrumentID) throws DatabaseException, MissingParamException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		
		Connection conn = null;
		
		try {
			conn = dataSource.getConnection();
			return getInstrument(conn, instrumentID);
		} catch (SQLException e) {
			throw new DatabaseException("Error while updating record counts", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}
	}

	
	/**
	 * Returns a complete instrument object for the specified instrument ID
	 * @param conn A database connetion
	 * @param instrumentID The instrument ID
	 * @return The complete Instrument object
	 * @throws MissingParamException If the data source is not supplied
	 * @throws DatabaseException If an error occurs while retrieving the instrument details
	 * @throws RecordNotFoundException If the specified instrument cannot be found
	 */
	public static Instrument getInstrument(Connection conn, long instrumentID) throws MissingParamException, DatabaseException, RecordNotFoundException {
		return null;
		// TODO Reinstate
	}
	
	/**
	 * Get the Instrument object associated with a give data file,
	 * identified by its database ID
	 * @param dataSource A data source
	 * @param fileId The data file ID
	 * @return The instrument object
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If an unexpected database error occurs
	 * @throws RecordNotFoundException If the file ID is not in the database
	 */
	public static Instrument getInstrumentByFileId(DataSource dataSource, long fileId) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(fileId, "fileId");
		
		long instrumentId = DataFileDB.getInstrumentId(dataSource, fileId);
		return getInstrument(dataSource, instrumentId);
	}

	/**
	 * Get the names of all run types of a given run type category in a given instrument
	 * @param dataSource A data source
	 * @param instrumentId The instrument's database ID
	 * @param categoryCode The run type category code
	 * @return The list of run types
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public static List<String> getRunTypes(DataSource dataSource, long instrumentId, String categoryCode) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		List<String> runTypes = null;
		
		Connection conn = null;
		try {
			
			conn = dataSource.getConnection();
			runTypes = getRunTypes(conn, instrumentId, categoryCode);
		} catch (SQLException e) {
			throw new DatabaseException("Error while getting run types", e);
		} finally {
			DatabaseUtils.closeConnection(conn);
		}

		return runTypes;
	}

	/**
	 * Get the names of all run types of a given run type category in a given instrument
	 * @param conn A database connection
	 * @param instrumentId The instrument's database ID
	 * @param categoryCode The run type category code
	 * @return The list of run types
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public static List<String> getRunTypes(Connection conn, long instrumentId, String categoryCode) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(instrumentId, "instrumentId");
		MissingParam.checkMissing(categoryCode, "categoryCode");
		
		List<String> runTypes = new ArrayList<String>();
		
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			stmt = conn.prepareStatement(GET_RUN_TYPES_QUERY);
			stmt.setLong(1, instrumentId);
			stmt.setString(2, categoryCode);
			
			records = stmt.executeQuery();
			while (records.next()) {
				runTypes.add(records.getString(1));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while getting run types", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
		}

		return runTypes;
	}
	
	/**
	 * Get a list of all the sensors on a particular instrument that require calibration.
	 * 
	 * <p>
	 *   Each sensor will be listed in the form of
	 *   {@code <file>: <sensorName>}
	 * </p>
	 * 
	 * @param dataSource A data source
	 * @param instrumentId The instrument ID
	 * @return The list of calibratable sensors
	 * @throws MissingParamException If any required parameters are missing
	 * @throws DatabaseException If a database error occurs
	 */
	public static List<String> getCalibratableSensors(DataSource dataSource, long instrumentId) throws MissingParamException, DatabaseException {
		
		List<String> result = new ArrayList<String>();
		
		MissingParam.checkMissing(dataSource, "dataSource");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_CALIBRATABLE_SENSORS_QUERY);
			stmt.setLong(1, instrumentId);
			
			records = stmt.executeQuery();
			while (records.next()) {
				result.add(records.getString(1));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while getting run types", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		
		return result;
	}
}
