package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.ParameterException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Database methods for database actions related to calibrations
 * @author Steve Jones
 *
 */
public abstract class CalibrationDB {

  /**
   * Statement to add a new calibration to the database
   * @see #addCalibration(DataSource, Calibration)
   */
  private static final String ADD_CALIBRATION_STATEMENT = "INSERT INTO calibration "
      + "(instrument_id, type, target, deployment_date, coefficients, class) "
      + "VALUES (?, ?, ?, ?, ?, ?)";

  /**
   * Query for finding recent calibrations.
   * @see #getCurrentCalibrations(DataSource, long)
   */
  private static final String GET_RECENT_CALIBRATIONS_QUERY = "SELECT "
      + "instrument_id, target, deployment_date, coefficients, class FROM calibration WHERE "
      + "instrument_id = ? AND type = ? AND deployment_date <= ? "
      + "ORDER BY deployment_date DESC";

  /**
   * Query to determine whether a calibration already exists
   * @see #calibrationExists(DataSource, Calibration)
   */
  private static final String CALIBRATION_EXISTS_QUERY = "SELECT "
      + "COUNT(*) FROM calibration WHERE instrument_id = ? AND "
      + "type = ? AND deployment_date = ? AND target = ?";

  /**
   * Query to get all calibrations of a given type for an instrument
   */
  private static final String GET_CALIBRATIONS_QUERY = "SELECT "
      + "instrument_id, target, deployment_date, coefficients, class FROM calibration WHERE "
      + "instrument_id = ? AND type = ? ORDER BY "
      + "target, deployment_date ASC";

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
   * @throws DatabaseException If a database error occurs
   * @throws ParameterException If any required parameters are missing or the calibration is invalid
   */
  public void addCalibration(DataSource dataSource, Calibration calibration) throws DatabaseException, ParameterException {
     MissingParam.checkMissing(dataSource, "dataSource");
     MissingParam.checkMissing(calibration, "calibration");
     MissingParam.checkMissing(calibration.getDeploymentDate(), "calibration deployment date");

     if (!calibration.validate()) {
       throw new ParameterException("Calibration coefficients", "Coefficients are invalid");
     }

     Connection conn = null;
     PreparedStatement stmt = null;

     try {
       conn = dataSource.getConnection();
       stmt = conn.prepareStatement(ADD_CALIBRATION_STATEMENT);
       stmt.setLong(1, calibration.getInstrumentId());
       stmt.setString(2, calibration.getType());
       stmt.setString(3, calibration.getTarget());
       stmt.setLong(4, DateTimeUtils.dateToLong(calibration.getDeploymentDate()));
       stmt.setString(5, calibration.getCoefficientsAsDelimitedList());
       stmt.setString(6, calibration.getClass().getSimpleName());

       stmt.execute();
     } catch (SQLException e) {
       throw new DatabaseException("Error while storing calibration", e);
     } finally {
       DatabaseUtils.closeStatements(stmt);
       DatabaseUtils.closeConnection(conn);
     }
  }

  /**
   * Get the most recent calibrations for each target
   * @param dataSource A data source
   * @param instrumentId The instrument ID
   * @return The calibrations
   * @throws CalibrationException If the calibrations are internally inconsistent
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If any required records are missing
   * @throws MissingParamException If any internal calls are missing required parameters
   */
  public CalibrationSet getMostRecentCalibrations(DataSource dataSource,
      long instrumentId, LocalDateTime date) throws CalibrationException,
      DatabaseException, MissingParamException, RecordNotFoundException {

    CalibrationSet result = new CalibrationSet(instrumentId, getCalibrationType(), getTargets(dataSource, instrumentId));

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_RECENT_CALIBRATIONS_QUERY);
      stmt.setLong(1, instrumentId);
      stmt.setString(2, getCalibrationType());
      // Get epoch milliseconds
      stmt.setLong(3, DateTimeUtils.dateToLong(date));
      records = stmt.executeQuery();
      while (!result.isComplete() && records.next()) {
        String target = records.getString(1);

        if (!result.containsTarget(target)) {
          result.add(calibrationFromResultSet(records));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the most recent calibrations for each target
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument ID
   * @return The calibrations
   * @throws CalibrationException
   *           If the calibrations are internally inconsistent
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If any required records are missing
   * @throws MissingParamException
   *           If any internal calls are missing required parameters
   */
  public CalibrationSet getCurrentCalibrations(DataSource dataSource,
      long instrumentId) throws CalibrationException, DatabaseException,
      MissingParamException, RecordNotFoundException {
    return getMostRecentCalibrations(dataSource, instrumentId,
        LocalDateTime.now());
  }

  /**
   * Retrieve all calibrations from the database of a given type,
   * grouped by target and ordered by date
   * @param dataSource A data source
   * @param instrumentId The instrument
   * @param class The calibration type
   * @return The calibrations
   * @throws MissingParamException
   * @throws DatabaseException
   */
  public TreeMap<String, List<Calibration>> getCalibrations(DataSource dataSource, long instrumentId) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    TreeMap<String, List<Calibration>> calibrations = new TreeMap<String, List<Calibration>>();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_CALIBRATIONS_QUERY);
      stmt.setLong(1, instrumentId);
      stmt.setString(2, getCalibrationType());

      records = stmt.executeQuery();
      while (records.next()) {
        Calibration calibration = calibrationFromResultSet(records);
        if (!calibrations.containsKey(calibration.getTarget())) {
          calibrations.put(calibration.getTarget(), new ArrayList<Calibration>());
        }

        calibrations.get(calibration.getTarget()).add(calibration);
      }


    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }


    return calibrations;
  }

  private Calibration calibrationFromResultSet(ResultSet record) throws SQLException {
    long instrumentId = record.getLong(1);
    String target = record.getString(2);
    LocalDateTime deploymentDate = DateTimeUtils.longToDate(record.getLong(3));
    List<Double> coefficients = StringUtils.delimitedToDoubleList(record.getString(4));
    String calibrationClass = record.getString(5);

    return CalibrationFactory.createCalibration(getCalibrationType(), calibrationClass, instrumentId, deploymentDate, target, coefficients);
  }

  /**
   * Determine whether or not a calibration exists that coincides with the
   * specified calibration (checks instrument, type, target and deployment date).
   * @param dataSource A data source
   * @param calibration The calibration to be compared
   * @return {@code true} if a calibration exists; {@code false} otherwise
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  public boolean calibrationExists(DataSource dataSource, Calibration calibration) throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(calibration, "calibration");

    boolean result = false;

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(CALIBRATION_EXISTS_QUERY);
      stmt.setLong(1, calibration.getInstrumentId());
      stmt.setString(2, getCalibrationType());
      stmt.setLong(3, DateTimeUtils.dateToLong(calibration.getDeploymentDate()));
      stmt.setString(4, calibration.getTarget());

      records = stmt.executeQuery();
      records.next();
      int recordCount = records.getInt(1);
      if (recordCount != 0) {
        result = true;
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the list of possible calibration targets for a given instrument
   * @param dataSource A data source
   * @param instrumentId The instrument's database ID
   * @return The targets
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If no external standard run types are found
   */
  public Map<String, String> getTargets(DataSource dataSource, long instrumentId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    Connection conn = null;
    Map<String, String> result = null;

    try {
      conn = dataSource.getConnection();
      result = getTargets(conn, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting calibration targets", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the list of possible calibration targets for a given instrument
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The targets
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If no external standard run types are found
   */
  public abstract Map<String, String> getTargets(Connection conn, long instrumentId) throws MissingParamException, DatabaseException, RecordNotFoundException;

  /**
   * Get the calibration type for database actions
   * @return The calibration type
   */
  public abstract String getCalibrationType();
}
