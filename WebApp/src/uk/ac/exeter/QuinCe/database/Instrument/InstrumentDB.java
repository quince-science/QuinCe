package uk.ac.exeter.QuinCe.database.Instrument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.database.files.DataFileDB;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

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
			+ "owner, name,"
			+ "intake_temp_1_name, intake_temp_2_name, intake_temp_3_name, "
			+ "salinity_1_name, salinity_2_name, salinity_3_name, "
			+ "eqt_1_name, eqt_2_name, eqt_3_name, "
			+ "eqp_1_name, eqp_2_name, eqp_3_name, "
			+ "separator_char, date_format, time_format, lat_format, " 
			+ "lon_format, header_lines, has_atmospheric_pressure, samples_dried, "
			+ "run_type_col, date_col, year_col, month_col, day_col, "
			+ "time_col, hour_col, minute_col, second_col, "
			+ "latitude_col, north_south_col, longitude_col, east_west_col, "
			+ "intake_temp_1_col, intake_temp_2_col, intake_temp_3_col, "
			+ "salinity_1_col, salinity_2_col, salinity_3_col, "
			+ "eqt_1_col, eqt_2_col, eqt_3_col, "
			+ "eqp_1_col, eqp_2_col, eqp_3_col, "
			+ "atmospheric_pressure_col, moisture_col, co2_col, raw_col_count, "
			+ "pre_flushing_time, post_flushing_time) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
			+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
			+ "?, ?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * Statement for retrieving all the details of a specific instrument
	 */
	private static final String GET_INSTRUMENT_QUERY = "SELECT owner, name, "
			+ "intake_temp_1_name, intake_temp_2_name, intake_temp_3_name, "
			+ "salinity_1_name, salinity_2_name, salinity_3_name, "
			+ "eqt_1_name, eqt_2_name, eqt_3_name, "
			+ "eqp_1_name, eqp_2_name, eqp_3_name, "
			+ "separator_char, date_format, time_format, lat_format, " 
			+ "lon_format, header_lines, has_atmospheric_pressure, samples_dried, "
			+ "run_type_col, date_col, year_col, month_col, day_col, "
			+ "time_col, hour_col, minute_col, second_col, "
			+ "latitude_col, north_south_col, longitude_col, east_west_col, "
			+ "intake_temp_1_col, intake_temp_2_col, intake_temp_3_col, "
			+ "salinity_1_col, salinity_2_col, salinity_3_col, "
			+ "eqt_1_col, eqt_2_col, eqt_3_col, "
			+ "eqp_1_col, eqp_2_col, eqp_3_col, "
			+ "atmospheric_pressure_col, moisture_col, co2_col, raw_col_count, "
			+ "pre_flushing_time, post_flushing_time "
			+ "FROM instrument WHERE id = ? ORDER BY name";
			
	
	/**
	 * Statement for inserting run types
	 */
	private static final String CREATE_RUN_TYPE_STATEMENT = "INSERT INTO run_types (instrument_id, run_name, run_type) VALUES (?, ?, ?)";
	
	/**
	 * Query for retrieving the list of instruments owned by a particular user
	 */
	private static final String GET_INSTRUMENT_LIST_QUERY = "SELECT id, name FROM instrument WHERE owner = ? ORDER BY name ASC";
	
	private static final String GET_RUN_TYPES_QUERY = "SELECT id, run_name, run_type FROM run_types WHERE instrument_id = ?";
	
	/**
	 * Add an instrument to the database
	 * @throws MissingParamException If any of the required data are missing
	 * @throws DatabaseException If an error occurs while storing the instrument details
	 */
	public static void addInstrument(DataSource dataSource, Instrument instrument) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(instrument, "instrument");
		instrument.validate();
		
		TreeSet<RunType> runTypes = instrument.getRunTypes();
		
		Connection conn = null;
		PreparedStatement instrStmt = null;
		ResultSet generatedKeys = null;
		List<PreparedStatement> runTypeStmts = new ArrayList<PreparedStatement>(runTypes.size());

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			instrStmt = conn.prepareStatement(CREATE_INSTRUMENT_STATEMENT, Statement.RETURN_GENERATED_KEYS);
			instrStmt.setLong(1, instrument.getOwnerId());
			instrStmt.setString(2, instrument.getName());
			instrStmt.setString(3, instrument.getIntakeTempName1());
			instrStmt.setString(4, instrument.getIntakeTempName2());
			instrStmt.setString(5, instrument.getIntakeTempName3());
			instrStmt.setString(6, instrument.getSalinityName1());
			instrStmt.setString(7, instrument.getSalinityName2());
			instrStmt.setString(8, instrument.getSalinityName3());
			instrStmt.setString(9, instrument.getEqtName1());
			instrStmt.setString(10, instrument.getEqtName2());
			instrStmt.setString(11, instrument.getEqtName3());
			instrStmt.setString(12, instrument.getEqpName1());
			instrStmt.setString(13, instrument.getEqpName2());
			instrStmt.setString(14, instrument.getEqpName3());
			instrStmt.setString(15, String.valueOf(instrument.getSeparatorChar()));
			instrStmt.setInt(16, instrument.getDateFormat());
			instrStmt.setInt(17, instrument.getTimeFormat());
			instrStmt.setInt(18, instrument.getLatFormat());
			instrStmt.setInt(19, instrument.getLonFormat());
			instrStmt.setInt(20, instrument.getHeaderLines());
			instrStmt.setBoolean(21, instrument.getHasAtmosphericPressure());
			instrStmt.setBoolean(22, instrument.getSamplesDried());
			instrStmt.setInt(23, instrument.getColumnAssignment(Instrument.COL_RUN_TYPE));
			instrStmt.setInt(24, instrument.getColumnAssignment(Instrument.COL_DATE));
			instrStmt.setInt(25, instrument.getColumnAssignment(Instrument.COL_YEAR));
			instrStmt.setInt(26, instrument.getColumnAssignment(Instrument.COL_MONTH));
			instrStmt.setInt(27, instrument.getColumnAssignment(Instrument.COL_DAY));
			instrStmt.setInt(28, instrument.getColumnAssignment(Instrument.COL_TIME));
			instrStmt.setInt(29, instrument.getColumnAssignment(Instrument.COL_HOUR));
			instrStmt.setInt(30, instrument.getColumnAssignment(Instrument.COL_MINUTE));
			instrStmt.setInt(31, instrument.getColumnAssignment(Instrument.COL_SECOND));
			instrStmt.setInt(32, instrument.getColumnAssignment(Instrument.COL_LATITUDE));
			instrStmt.setInt(33, instrument.getColumnAssignment(Instrument.COL_NORTH_SOUTH));
			instrStmt.setInt(34, instrument.getColumnAssignment(Instrument.COL_LONGITUDE));
			instrStmt.setInt(35, instrument.getColumnAssignment(Instrument.COL_EAST_WEST));
			instrStmt.setInt(36, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_1));
			instrStmt.setInt(37, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_2));
			instrStmt.setInt(38, instrument.getColumnAssignment(Instrument.COL_INTAKE_TEMP_3));
			instrStmt.setInt(39, instrument.getColumnAssignment(Instrument.COL_SALINITY_1));
			instrStmt.setInt(40, instrument.getColumnAssignment(Instrument.COL_SALINITY_2));
			instrStmt.setInt(41, instrument.getColumnAssignment(Instrument.COL_SALINITY_3));
			instrStmt.setInt(42, instrument.getColumnAssignment(Instrument.COL_EQT_1));
			instrStmt.setInt(43, instrument.getColumnAssignment(Instrument.COL_EQT_2));
			instrStmt.setInt(44, instrument.getColumnAssignment(Instrument.COL_EQT_3));
			instrStmt.setInt(45, instrument.getColumnAssignment(Instrument.COL_EQP_1));
			instrStmt.setInt(46, instrument.getColumnAssignment(Instrument.COL_EQP_2));
			instrStmt.setInt(47, instrument.getColumnAssignment(Instrument.COL_EQP_3));
			instrStmt.setInt(48, instrument.getColumnAssignment(Instrument.COL_ATMOSPHERIC_PRESSURE));
			instrStmt.setInt(49, instrument.getColumnAssignment(Instrument.COL_MOISTURE));
			instrStmt.setInt(50, instrument.getColumnAssignment(Instrument.COL_CO2));
			instrStmt.setInt(51, instrument.getRawFileColumnCount());
			instrStmt.setInt(52, instrument.getPreFlushingTime());
			instrStmt.setInt(53, instrument.getPostFlushingTime());
			
			
			instrStmt.execute();
			
			generatedKeys = instrStmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				instrument.setDatabaseId(generatedKeys.getLong(1));

				for (RunType runType : runTypes) {
					PreparedStatement stmt = conn.prepareStatement(CREATE_RUN_TYPE_STATEMENT);
					stmt.setLong(1, instrument.getDatabaseId());
					stmt.setString(2, runType.getName());
					stmt.setInt(3, runType.getRunType());
					
					stmt.execute();
					
					runTypeStmts.add(stmt);
					
					// Add the instrument id to the run type  object
					runType.setInstrumentID(instrument.getDatabaseId());
				}
			} else {
				throw new DatabaseException("Parent instrument record not created");
			}
			
			conn.commit();
			
		} catch (SQLException e) {
			
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while storing new instrument records", e);
		} finally {
			DatabaseUtils.closeResultSets(generatedKeys);
			DatabaseUtils.closeStatements(runTypeStmts);
			DatabaseUtils.closeStatements(instrStmt);
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Returns a list of instruments owned by a given user.
	 * The list contains InstrumentStub objects, which just contain
	 * each instrument's ID and name.
	 * 
	 * The list is ordered by the name of the instrument.
	 * 
	 * @param owner The owner whose instruments are to be listed
	 * @return The list of instruments
	 * @throws MissingParamException 
	 * @throws DatabaseException 
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
				InstrumentStub record = new InstrumentStub(instruments.getLong(1), instruments.getString(2));
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
	 * @param dataSource A data source
	 * @param instrumentID The instrument ID
	 * @return The complete Instrument object
	 * @throws MissingParamException If the data source is not supplied
	 * @throws DatabaseException If an error occurs while retrieving the instrument details
	 * @throws RecordNotFoundException If the specified instrument cannot be found
	 */
	public static Instrument getInstrument(Connection conn, long instrumentID) throws MissingParamException, DatabaseException, RecordNotFoundException {

		MissingParam.checkMissing(conn, "conn");
		
		PreparedStatement instrStmt = null;
		ResultSet record = null;
		PreparedStatement runTypeStmt = null;
		ResultSet runTypeRecords = null;
		Instrument instrument = null;
		TreeSet<RunType> runTypes = new TreeSet<RunType>();
		
		try {
			instrStmt = conn.prepareStatement(GET_INSTRUMENT_QUERY);
			instrStmt.setLong(1, instrumentID);
			
			record = instrStmt.executeQuery();
			if (!record.next()) {
				throw new RecordNotFoundException("Instrument with id " + instrumentID + " does not exist");
			} else {
				instrument = new Instrument(record.getLong(1));
				instrument.setDatabaseId(instrumentID);
				instrument.setName(record.getString(2));
				instrument.setIntakeTempName1(record.getString(3));
				instrument.setIntakeTempName2(record.getString(4));
				instrument.setIntakeTempName3(record.getString(5));
				instrument.setSalinityName1(record.getString(6));
				instrument.setSalinityName2(record.getString(7));
				instrument.setSalinityName3(record.getString(8));
				instrument.setEqtName1(record.getString(9));
				instrument.setEqtName2(record.getString(10));
				instrument.setEqtName3(record.getString(11));
				instrument.setEqpName1(record.getString(12));
				instrument.setEqpName2(record.getString(13));
				instrument.setEqpName3(record.getString(14));
				instrument.setSeparatorChar(record.getString(15).toCharArray()[0]);
				instrument.setDateFormat(record.getInt(16));
				instrument.setTimeFormat(record.getInt(17));
				instrument.setLatFormat(record.getInt(18));
				instrument.setLonFormat(record.getInt(19));
				instrument.setHeaderLines(record.getInt(20));
				instrument.setHasAtmosphericPressure(record.getBoolean(21));
				instrument.setSamplesDried(record.getBoolean(22));
				instrument.setColumnAssignment(Instrument.COL_RUN_TYPE, record.getInt(23));
				instrument.setColumnAssignment(Instrument.COL_DATE, record.getInt(24));
				instrument.setColumnAssignment(Instrument.COL_YEAR, record.getInt(25));
				instrument.setColumnAssignment(Instrument.COL_MONTH, record.getInt(26));
				instrument.setColumnAssignment(Instrument.COL_DAY, record.getInt(27));
				instrument.setColumnAssignment(Instrument.COL_TIME, record.getInt(28));
				instrument.setColumnAssignment(Instrument.COL_HOUR, record.getInt(29));
				instrument.setColumnAssignment(Instrument.COL_MINUTE, record.getInt(30));
				instrument.setColumnAssignment(Instrument.COL_SECOND, record.getInt(31));
				instrument.setColumnAssignment(Instrument.COL_LATITUDE, record.getInt(32));
				instrument.setColumnAssignment(Instrument.COL_NORTH_SOUTH, record.getInt(33));
				instrument.setColumnAssignment(Instrument.COL_LONGITUDE, record.getInt(34));
				instrument.setColumnAssignment(Instrument.COL_EAST_WEST, record.getInt(35));
				instrument.setColumnAssignment(Instrument.COL_INTAKE_TEMP_1, record.getInt(36));
				instrument.setColumnAssignment(Instrument.COL_INTAKE_TEMP_2, record.getInt(37));
				instrument.setColumnAssignment(Instrument.COL_INTAKE_TEMP_3, record.getInt(38));
				instrument.setColumnAssignment(Instrument.COL_SALINITY_1, record.getInt(39));
				instrument.setColumnAssignment(Instrument.COL_SALINITY_2, record.getInt(40));
				instrument.setColumnAssignment(Instrument.COL_SALINITY_3, record.getInt(41));
				instrument.setColumnAssignment(Instrument.COL_EQT_1, record.getInt(42));
				instrument.setColumnAssignment(Instrument.COL_EQT_2, record.getInt(43));
				instrument.setColumnAssignment(Instrument.COL_EQT_3, record.getInt(44));
				instrument.setColumnAssignment(Instrument.COL_EQP_1, record.getInt(45));
				instrument.setColumnAssignment(Instrument.COL_EQP_2, record.getInt(46));
				instrument.setColumnAssignment(Instrument.COL_EQP_3, record.getInt(47));
				instrument.setColumnAssignment(Instrument.COL_ATMOSPHERIC_PRESSURE, record.getInt(48));
				instrument.setColumnAssignment(Instrument.COL_MOISTURE, record.getInt(49));
				instrument.setColumnAssignment(Instrument.COL_CO2, record.getInt(50));
				instrument.setRawFileColumnCount(record.getInt(51));
				instrument.setPreFlushingTime(record.getInt(52));
				instrument.setPostFlushingTime(record.getInt(53));
				
				runTypeStmt = conn.prepareStatement(GET_RUN_TYPES_QUERY);
				runTypeStmt.setLong(1, instrumentID);
				runTypeRecords = runTypeStmt.executeQuery();
				while (runTypeRecords.next()) {
					runTypes.add(new RunType(runTypeRecords.getLong(1), instrumentID, runTypeRecords.getString(2), runTypeRecords.getInt(3)));
				}
				
				instrument.setRunTypes(runTypes);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving instrument details", e);
		} finally {
			DatabaseUtils.closeResultSets(record, runTypeRecords);
			DatabaseUtils.closeStatements(instrStmt, runTypeStmt);
		}
		
		return instrument;
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
}
