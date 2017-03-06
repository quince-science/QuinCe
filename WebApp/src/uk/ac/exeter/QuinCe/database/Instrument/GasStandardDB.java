package uk.ac.exeter.QuinCe.database.Instrument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.RunType;
import uk.ac.exeter.QuinCe.data.StandardConcentration;
import uk.ac.exeter.QuinCe.data.StandardStub;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Methods for handling records of gas standards in the database.
 * 
 * <p>
 *   There are two terms to understand with this class. The first is a Standards Deployment,
 *   which represents an installation of a complete set of gas standard bottles. The second
 *   is a Standard Concentration, which is the actual concentration of a single bottle within
 *   a deployment. A Standard Concentration is sometimes associated with its identification
 *   in the data file (its {@link RunType}), but this is not always required.
 * </p>
 * 
 * @author Steve Jones
 *
 */
public class GasStandardDB {

	/**
	 * Statement for creating a gas standards deployment record
	 */
	private static final String CREATE_STANDARD_STATEMENT = "INSERT INTO gas_standard_deployment ("
			+ "instrument_id, deployed_date) VALUES (?, ?)";
	
	/**
	 * Statement for creating the concentration record for a specific gas standard
	 * within a deployment
	 */
	private static final String CREATE_CONCENTRATIONS_STATEMENT = "INSERT INTO gas_standard_concentration ("
			+ "standard_id, standard_name, concentration) "
			+ "VALUES (?, ?, ?)";
	
	/**
	 * Statement for retrieving the list of standards deployments for a given instrument.
	 * The list is ordered by descending date.
	 */
	private static final String GET_STANDARD_LIST_QUERY = "SELECT id, deployed_date FROM "
			+ "gas_standard_deployment WHERE instrument_id = ? ORDER BY deployed_date DESC";
	
	/**
	 * Statement for retrieving the stub details for a given standards deployment
	 */
	private static final String GET_STANDARD_STUB_QUERY = "SELECT id, instrument_id, deployed_date FROM "
			+ "gas_standard_deployment WHERE id = ?";
	
	/**
	 * Statement for retrieving the concentrations for a given standards deployment
	 */
	private static final String GET_CONCENTRATIONS_QUERY = "SELECT standard_name, concentration FROM "
			+ "gas_standard_concentration WHERE standard_id = ? ORDER BY standard_name ASC";
	
	/**
	 * Statement to update the date for a given standards deployment
	 */
	private static final String UPDATE_STANDARD_STATEMENT = "UPDATE gas_standard_deployment SET "
			+ "deployed_date = ? WHERE id = ?";
	
	/**
	 * Statement to remove all concentrations for a given standards deployment.
	 * This is used prior to adding the revised concentrations.
	 */
	private static final String REMOVE_CONCENTRATIONS_STATEMENT = "DELETE FROM gas_standard_concentration WHERE standard_id = ?";
	
	/**
	 * Statement for finding a standards deployment record with a given ID
	 */
	private static final String FIND_STANDARD_QUERY = "SELECT id FROM gas_standard_deployment WHERE id = ?";
	
	/**
	 * Statement for removing a standards deployment record.
	 */
	private static final String REMOVE_STANDARD_STATEMENT = "DELETE FROM gas_standard_deployment WHERE id = ?";
	
	/**
	 * Query to get the names of the gas standards run types for a given instrument.
	 * @see RunType
	 */
	private static final String GET_STANDARD_NAMES_QUERY = "SELECT run_name FROM run_types WHERE "
			+ "instrument_id = ? AND run_type = " + RunType.RUN_TYPE_STANDARD;
	
	/**
	 * Statement to find standards deployment dates between two given dates
	 */
	private static final String GET_DATES_BETWEEN_QUERY = "SELECT deployed_date FROM gas_standard_deployment WHERE instrument_id = ? AND deployed_date >= ? AND deployed_date <= ? ORDER BY deployed_date ASC";
	
	/**
	 * Statement to find the latest standards deployment date before a given date
	 */
	private static final String GET_DATE_BEFORE_QUERY = "SELECT deployed_date FROM gas_standard_deployment WHERE instrument_id = ? AND deployed_date <= ? ORDER BY deployed_date DESC LIMIT 1";
	
	/**
	 * Statement to find the latest standards deployment date before a given date
	 */
	private static final String GET_DATE_AFTER_QUERY = "SELECT deployed_date FROM gas_standard_deployment WHERE instrument_id = ? AND deployed_date >= ? ORDER BY deployed_date DESC LIMIT 1";
	
	/**
	 * Statement to find the latest standards deployment date before a given date
	 */
	private static final String GET_STANDARD_BEFORE_QUERY = "SELECT id, deployed_date FROM gas_standard_deployment WHERE instrument_id = ? AND deployed_date <= ? ORDER BY deployed_date DESC LIMIT 1";
	
	/**
	 * Add a standards deployment to the database
	 * @param dataSource A data source
	 * @param instrumentID The ID of the instrument
	 * @param deployedDate The date of the standard deployment
	 * @param concentrations The concentrations for each of the gas standards
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while creating the database records
	 * @see #CREATE_STANDARD_STATEMENT
	 * @see #CREATE_CONCENTRATIONS_STATEMENT
	 */
	public static void addStandard(DataSource dataSource, long instrumentID, Date deployedDate, List<StandardConcentration> concentrations) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(deployedDate, "deployedDate");
		MissingParam.checkMissing(concentrations, "concentrations");
		
		Connection conn = null;
		PreparedStatement calibStmt = null;
		ResultSet generatedKeys = null;
		long calibID;
		List<PreparedStatement> concentrationStmts = new ArrayList<PreparedStatement>(concentrations.size());

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);

			// Store the main standard record
			calibStmt = conn.prepareStatement(CREATE_STANDARD_STATEMENT, Statement.RETURN_GENERATED_KEYS);
			calibStmt.setLong(1, instrumentID);
			calibStmt.setDate(2, new java.sql.Date(deployedDate.getTime()));
			calibStmt.execute();
			
			generatedKeys = calibStmt.getGeneratedKeys();
			if (generatedKeys.next()) {
				calibID = generatedKeys.getLong(1);
			
				// Store the coefficients
				for (StandardConcentration concentration : concentrations) {
					PreparedStatement concStmt = conn.prepareStatement(CREATE_CONCENTRATIONS_STATEMENT);
					
					concStmt.setLong(1, calibID);
					concStmt.setString(2, concentration.getStandardName());
					concStmt.setDouble(3, concentration.getConcentration());
					
					concStmt.execute();
					
					concentrationStmts.add(concStmt);
				}
			} else {
				throw new DatabaseException("Parent standard record not created");
			}

			conn.commit();
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while storing new standard records", e);
		} finally {
			DatabaseUtils.closeStatements(concentrationStmts);
			DatabaseUtils.closeResultSets(generatedKeys);
			DatabaseUtils.closeStatements(calibStmt);
			DatabaseUtils.closeConnection(conn);
		}		
	}
	
	/**
	 * Retrieve the list of standards deployments for a specific instrument from the database
	 * @param dataSource A data source
	 * @param instrumentID The instrument ID
	 * @return The list of standards deployments
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the list
	 * @see #GET_STANDARD_LIST_QUERY
	 */
	public static List<StandardStub> getStandardList(DataSource dataSource, long instrumentID) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<StandardStub> result = new ArrayList<StandardStub>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_STANDARD_LIST_QUERY);
			stmt.setLong(1, instrumentID);
			
			records = stmt.executeQuery();
			while (records.next()) {
				result.add(new StandardStub(records.getLong(1), instrumentID, records.getDate(2)));
			}
			
			return result;
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving standards list", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}	
	}
	
	/**
	 * Retrieve a standards deployment stub object for a given standard ID
	 * @param dataSource A data source
	 * @param standardID The standard ID
	 * @return The standards deployment stub
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the stub
	 * @throws RecordNotFoundException If the standard ID does not exist in the database
	 * @see StandardStub
	 * @see #GET_STANDARD_STUB_QUERY
	 */
	public static StandardStub getStandardStub(DataSource dataSource, long standardID) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(standardID, "standardID");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		StandardStub stub = null;
		
		try {
		
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_STANDARD_STUB_QUERY);
			stmt.setLong(1, standardID);
			
			records = stmt.executeQuery();
			if (!records.next()) {
				throw new RecordNotFoundException("Could not find standard with ID " + standardID);				
			} else {
				stub = new StandardStub(records.getLong(1), records.getLong(2), records.getDate(3));
				return stub;
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving standard stub", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}

	/**
	 * Retrieve the set of standard concentrations for a given gas standards deployment
	 * @param dataSource A data source
	 * @param standard The gas standard whose concentrations should be retrieved
	 * @return The standard concentrations
	 * @throws MissingParamException If any parameters are missing
	 * @throws RecordNotFoundException If any database records are missing
	 * @throws DatabaseException If a database error occurs
	 * @see #GET_CONCENTRATIONS_QUERY
	 */
	public static List<StandardConcentration> getConcentrations(DataSource dataSource, StandardStub standard) throws MissingParamException, RecordNotFoundException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(standard, "standard");
				
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<StandardConcentration> concentrations = new ArrayList<StandardConcentration>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_CONCENTRATIONS_QUERY);
			stmt.setLong(1, standard.getId());
			records = stmt.executeQuery();
			
			while (records.next()) {
				StandardConcentration concentration = new StandardConcentration(records.getString(1));
				concentration.setConcentration(records.getDouble(2));	
				concentrations.add(concentration);
			}
			
			if (concentrations.size() == 0) {
				throw new RecordNotFoundException("Could not find any concentrations for standard " + standard.getId());
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving concentrations", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return concentrations;
	}

	/**
	 * Retrieve a lookup table of the gas standard concentrations for a given standards deployment
	 * @param dataSource A data source
	 * @param standard The standards deployment
	 * @return The gas standard concentrations
	 * @throws MissingParamException If any parameters are missing
	 * @throws RecordNotFoundException If any required database records are missing
	 * @throws DatabaseException If a database error occurs
	 * @see #GET_CONCENTRATIONS_QUERY
	 */
	public static Map<String, StandardConcentration> getConcentrationsMap(DataSource dataSource, StandardStub standard) throws MissingParamException, RecordNotFoundException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(standard, "standard");
				
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		Map<String, StandardConcentration> concentrations = new HashMap<String, StandardConcentration>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_CONCENTRATIONS_QUERY);
			stmt.setLong(1, standard.getId());
			records = stmt.executeQuery();
			
			while (records.next()) {
				StandardConcentration concentration = new StandardConcentration(records.getString(1));
				concentration.setConcentration(records.getDouble(2));	
				concentrations.put(records.getString(1), concentration);
			}
			
			if (concentrations.size() == 0) {
				throw new RecordNotFoundException("Could not find any concentrations for standard " + standard.getId());
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving concentrations", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return concentrations;
	}

	/**
	 * Update the gas standard concentrations for a given standards deployment.
	 * @param dataSource A data source
	 * @param standardID The database ID of the standards deployment
	 * @param deployedDate The date of the deployment
	 * @param concentrations The gas standard concentrations
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws RecordNotFoundException If the standards deployment does not exist
	 * @see #UPDATE_STANDARD_STATEMENT
	 * @see #REMOVE_CONCENTRATIONS_STATEMENT
	 * @see #CREATE_CONCENTRATIONS_STATEMENT
	 */
	public static void updateStandard(DataSource dataSource, long standardID, Date deployedDate, List<StandardConcentration> concentrations) throws MissingParamException, DatabaseException, RecordNotFoundException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(standardID, "standardID");
		MissingParam.checkMissing(deployedDate, "deployedDate");
		MissingParam.checkMissing(concentrations, "concentrations");
		
		Connection conn = null;
		PreparedStatement updateStandardStmt = null;
		PreparedStatement removeConcentrationsStmt = null;
		List<PreparedStatement> concentrationStmts = new ArrayList<PreparedStatement>(concentrations.size());
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			if (!standardExists(conn, standardID)) {
				throw new RecordNotFoundException("Could not find standard " + standardID);
			}

			// Update the standard date 
			updateStandardStmt = conn.prepareStatement(UPDATE_STANDARD_STATEMENT);
			updateStandardStmt.setDate(1, new java.sql.Date(deployedDate.getTime()));
			updateStandardStmt.setLong(2, standardID);
			
			updateStandardStmt.execute();
			
			// Remove the existing coefficients
			removeConcentrationsStmt = conn.prepareStatement(REMOVE_CONCENTRATIONS_STATEMENT);
			removeConcentrationsStmt.setLong(1, standardID);
			removeConcentrationsStmt.execute();
			
			// Add the updated coefficients
			for (StandardConcentration concentration : concentrations) {
				PreparedStatement concentrationStmt = conn.prepareStatement(CREATE_CONCENTRATIONS_STATEMENT);
				
				concentrationStmt.setLong(1, standardID);
				concentrationStmt.setString(2, concentration.getStandardName());
				concentrationStmt.setDouble(3, concentration.getConcentration());

				concentrationStmt.execute();
				
				concentrationStmts.add(concentrationStmt);
			}

			conn.commit();
			
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while updating standard " + standardID, e);
		} finally {
			DatabaseUtils.closeStatements(removeConcentrationsStmt, updateStandardStmt);
			DatabaseUtils.closeStatements(concentrationStmts);
			DatabaseUtils.closeConnection(conn);
		}
	}
	
	/**
	 * Search for an existing standards deployment record with a given ID
	 * @param conn A database connection
	 * @param standardID The standard ID
	 * @return {@code true} if the standard exists; {@code false} if it does not.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while searching the database
	 * @see #FIND_STANDARD_QUERY
	 */
	private static boolean standardExists(Connection conn, long standardID) throws MissingParamException, DatabaseException {
		
		boolean result = false;
		
		MissingParam.checkMissing(conn, "conn");
		MissingParam.checkPositive(standardID, "standardID");
		
		PreparedStatement stmt = null;
		ResultSet records = null;
		
		try {
			stmt = conn.prepareStatement(FIND_STANDARD_QUERY);
			stmt.setLong(1, standardID);
			
			records = stmt.executeQuery();
			result = records.next();
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for standard");
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
		}
		
		return result;
	}

	/**
	 * Remove a standards deployment from the database
	 * @param dataSource A data source
	 * @param standardID The standard ID
	 * @throws MissingParamException If any parameters are missing
	 * @throws DatabaseException If a database error occurs
	 * @see #REMOVE_CONCENTRATIONS_STATEMENT
	 * @see #REMOVE_STANDARD_STATEMENT
	 */
	public static void deleteStandard(DataSource dataSource, long standardID) throws MissingParamException, DatabaseException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(standardID, "standardID");
		
		Connection conn = null;
		PreparedStatement removeCoeffsStmt = null;
		PreparedStatement removeCalibStmt = null;
		
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			removeCoeffsStmt = conn.prepareStatement(REMOVE_CONCENTRATIONS_STATEMENT);
			removeCoeffsStmt.setLong(1, standardID);
			removeCoeffsStmt.execute();
			
			removeCalibStmt = conn.prepareStatement(REMOVE_STANDARD_STATEMENT);
			removeCalibStmt.setLong(1, standardID);
			removeCalibStmt.execute();
			
			conn.commit();
			
		} catch (SQLException e) {
			DatabaseUtils.rollBack(conn);
			throw new DatabaseException("Error while deleting calibraion " + standardID, e);
		} finally {
			DatabaseUtils.closeStatements(removeCoeffsStmt, removeCalibStmt);
			DatabaseUtils.closeConnection(conn);
		}
	}

	/**
	 * Retrieve the list of gas standard names for a given instrument.
	 * The standard names are the Run Types recorded for standards runs in the data file.
	 * 
	 * @param dataSource A data source
	 * @param instrumentID The instrument ID
	 * @return The list of standard names
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while searching the database
	 * @throws RecordNotFoundException If no standards are found
	 * @see #GET_STANDARD_NAMES_QUERY
	 * @see RunType
	 */
	public static List<String> getStandardNames(DataSource dataSource, long instrumentID) throws MissingParamException, DatabaseException, RecordNotFoundException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<String> result = new ArrayList<String>();
		
		try {
		
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_STANDARD_NAMES_QUERY);
			stmt.setLong(1, instrumentID);
			records = stmt.executeQuery();
			
			while (records.next()) {
				result.add(records.getString(1));
			}
			
			if (result.size() == 0) {
				throw new RecordNotFoundException("No gas standards found for instrument " + instrumentID);
			}
			
			return result;
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while retrieving list of gas standards", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
	}

	/**
	 * Return a list of standards deployment dates for a given instrument that
	 * fall between two specified dates
	 * @param dataSource A data source
	 * @param instrumentID The database ID of the instrument
	 * @param firstDate The first date
	 * @param lastDate The last date
	 * @return A list of standard dates
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the dates
	 * @see #GET_DATES_BETWEEN_QUERY
	 */
	public static List<Calendar> getStandardDatesBetween(DataSource dataSource, long instrumentID, Calendar firstDate, Calendar lastDate) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(firstDate, "firstDate");
		MissingParam.checkMissing(lastDate, "lastDate");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		List<Calendar> result = new ArrayList<Calendar>();
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_DATES_BETWEEN_QUERY);
			stmt.setLong(1, instrumentID);
			stmt.setDate(2, new java.sql.Date(DateTimeUtils.setMidnight(firstDate).getTime().getTime()));
			stmt.setDate(3, new java.sql.Date(DateTimeUtils.setMidnight(lastDate).getTime().getTime()));
			
			records = stmt.executeQuery();
			while (records.next()) {
				Calendar standardDate = DateTimeUtils.getUTCCalendarInstance();
				standardDate.setTime(records.getDate(1));
				result.add(standardDate);
			}
			
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for gas standard dates", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}
		
		return result;
	}

	/**
	 * Find the last standards deployment date for a specified instrument before a given date
	 * @param dataSource A data source
	 * @param instrumentId The instrument ID
	 * @param date The date
	 * @return The last standards deployment date before the given date. If there is no date, returns null.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the date.
	 * @see #GET_DATE_BEFORE_QUERY
	 */
	public static Calendar getStandardDateBefore(DataSource dataSource, long instrumentId, Calendar date) throws MissingParamException, DatabaseException {
		return getStandardDateFromQuery(dataSource, instrumentId, date, GET_DATE_BEFORE_QUERY);
	}
	
	/**
	 * Find the first standards deployment date for a specified instrument after a given date
	 * @param dataSource A data source
	 * @param instrumentId The instrument ID
	 * @param date The date
	 * @return The first standards deployment date after the given date. If there is no date, returns null.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the date.
	 * @see #GET_DATE_AFTER_QUERY
	 */
	public static Calendar getStandardDateAfter(DataSource dataSource, long instrumentId, Calendar date) throws MissingParamException, DatabaseException {
		return getStandardDateFromQuery(dataSource, instrumentId, date, GET_DATE_AFTER_QUERY);
	}
	
	private static Calendar getStandardDateFromQuery(DataSource dataSource, long instrumentId, Calendar date, String query) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentId, "instrumentId");
		MissingParam.checkMissing(date, "date");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		Calendar result = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(query);
			stmt.setLong(1, instrumentId);
			stmt.setDate(2, new java.sql.Date(DateTimeUtils.setMidnight(date).getTime().getTime()));
			
			records = stmt.executeQuery();
			if (records.next()) {
				result = DateTimeUtils.getUTCCalendarInstance();
				result.setTime(records.getDate(1));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for gas standard dates", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}

		return result;
	}
	
	/**
	 * Find the last gas standard for a specified instrument before a given date
	 * @param dataSource A data source
	 * @param instrumentID The instrument ID
	 * @param date The date
	 * @return The last standard date before the given date. If there is no date, returns null.
	 * @throws MissingParamException If any of the parameters are missing
	 * @throws DatabaseException If an error occurs while retrieving the date.
	 */
	public static StandardStub getStandardBefore(DataSource dataSource, long instrumentID, Calendar date) throws MissingParamException, DatabaseException {
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkPositive(instrumentID, "instrumentID");
		MissingParam.checkMissing(date, "date");
		
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet records = null;
		StandardStub result = null;
		
		try {
			conn = dataSource.getConnection();
			stmt = conn.prepareStatement(GET_STANDARD_BEFORE_QUERY);
			stmt.setLong(1, instrumentID);
			stmt.setDate(2, new java.sql.Date(DateTimeUtils.setMidnight(date).getTime().getTime()));
			
			records = stmt.executeQuery();
			if (records.next()) {
				result = new StandardStub(records.getLong(1), instrumentID, records.getDate(2));
			}
		} catch (SQLException e) {
			throw new DatabaseException("Error while searching for gas standard dates", e);
		} finally {
			DatabaseUtils.closeResultSets(records);
			DatabaseUtils.closeStatements(stmt);
			DatabaseUtils.closeConnection(conn);
		}

		return result;
	}

}
