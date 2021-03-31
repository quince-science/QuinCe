package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
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
 *
 * @author Steve Jones
 *
 */
public abstract class CalibrationDB {

  /**
   * Statement to add a new calibration to the database
   *
   * @see #addCalibration(DataSource, Calibration)
   */
  private static final String ADD_CALIBRATION_STATEMENT = "INSERT INTO calibration "
    + "(instrument_id, type, target, deployment_date, coefficients, class) "
    + "VALUES (?, ?, ?, ?, ?, ?)";

  private static final String UPDATE_CALIBRATION_STATEMENT = "UPDATE calibration "
    + "SET instrument_id = ?, type = ?, target = ?, deployment_date = ?, "
    + "coefficients = ?, class = ? WHERE id = ?";

  private static final String DELETE_CALIBRATION_STATEMENT = "DELETE FROM "
    + "calibration WHERE id = ?";

  /**
   * Query for finding recent calibrations.
   *
   * @see #getCurrentCalibrations(DataSource, long)
   */
  private static final String GET_RECENT_CALIBRATIONS_QUERY = "SELECT "
    + "id, instrument_id, target, deployment_date, coefficients, class "
    + "FROM calibration WHERE "
    + "instrument_id = ? AND type = ? AND deployment_date <= ? "
    + "ORDER BY deployment_date DESC";

  private static final String GET_POST_CALIBRATIONS_QUERY = "SELECT "
    + "id, instrument_id, target, deployment_date, coefficients, class "
    + "FROM calibration WHERE "
    + "instrument_id = ? AND type = ? AND deployment_date >= ? "
    + "ORDER BY deployment_date ASC";

  /**
   * Query to determine whether a calibration already exists
   *
   * @see #calibrationExists(DataSource, Calibration)
   */
  private static final String CALIBRATION_EXISTS_QUERY = "SELECT "
    + "id FROM calibration WHERE instrument_id = ? AND "
    + "type = ? AND deployment_date = ? AND target = ?";

  /**
   * Query to get all calibrations of a given type for an instrument
   */
  private static final String GET_CALIBRATIONS_QUERY = "SELECT "
    + "id, instrument_id, target, deployment_date, coefficients, class "
    + "FROM calibration WHERE " + "instrument_id = ? AND type = ? ORDER BY "
    + "target, deployment_date ASC";

  /**
   * Empty constructor. These classes must be singletons so the abstract methods
   * can be declared. Individual instances can be retrieved from the concrete
   * classes
   */
  protected CalibrationDB() {
    // Do nothing
  }

  /**
   * Add a new calibration to the database
   *
   * @param dataSource
   *          A data source
   * @param calibration
   *          The calibration
   * @throws DatabaseException
   *           If a database error occurs
   * @throws ParameterException
   *           If any required parameters are missing or the calibration is
   *           invalid
   */
  public void addCalibration(DataSource dataSource, Calibration calibration)
    throws DatabaseException, ParameterException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(calibration, "calibration");
    MissingParam.checkMissing(calibration.getDeploymentDate(),
      "calibration deployment date");

    if (!calibration.validate()) {
      throw new ParameterException("Calibration coefficients",
        "Coefficients are invalid");
    }

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet generatedKeys = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(ADD_CALIBRATION_STATEMENT,
        Statement.RETURN_GENERATED_KEYS);

      stmt.setLong(1, calibration.getInstrument().getId());
      stmt.setString(2, calibration.getType());
      stmt.setString(3, calibration.getTarget());
      stmt.setLong(4,
        DateTimeUtils.dateToLong(calibration.getDeploymentDate()));
      stmt.setString(5, calibration.getCoefficientsAsDelimitedList());
      stmt.setString(6, calibration.getClass().getSimpleName());

      stmt.execute();

      generatedKeys = stmt.getGeneratedKeys();
      if (generatedKeys.next()) {
        calibration.setId(generatedKeys.getLong(1));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while storing calibration", e);
    } finally {
      DatabaseUtils.closeResultSets(generatedKeys);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  public void updateCalibration(DataSource dataSource, Calibration calibration)
    throws DatabaseException, ParameterException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(calibration, "calibration");
    MissingParam.checkMissing(calibration.getDeploymentDate(),
      "calibration deployment date");

    if (!calibration.validate()) {
      throw new ParameterException("Calibration coefficients",
        "Coefficients are invalid");
    }

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(UPDATE_CALIBRATION_STATEMENT);) {

      stmt.setLong(1, calibration.getInstrument().getId());
      stmt.setString(2, calibration.getType());
      stmt.setString(3, calibration.getTarget());
      stmt.setLong(4,
        DateTimeUtils.dateToLong(calibration.getDeploymentDate()));
      stmt.setString(5, calibration.getCoefficientsAsDelimitedList());
      stmt.setString(6, calibration.getClass().getSimpleName());
      stmt.setLong(7, calibration.getId());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing calibration", e);
    }
  }

  public void deleteCalibration(DataSource dataSource, long calibrationId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(calibrationId, "calibrationId");

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(DELETE_CALIBRATION_STATEMENT);) {

      stmt.setLong(1, calibrationId);
      stmt.execute();

    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting calibration", e);
    }

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
   * @throws InstrumentException
   */
  public CalibrationSet getMostRecentCalibrations(DataSource dataSource,
    Instrument instrument, LocalDateTime date)
    throws CalibrationException, DatabaseException, MissingParamException,
    RecordNotFoundException, InstrumentException {

    CalibrationSet result = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getMostRecentCalibrations(conn, instrument, date);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    } finally {
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
   * @throws InstrumentException
   */
  public CalibrationSet getMostRecentCalibrations(Connection conn,
    Instrument instrument, LocalDateTime date)
    throws CalibrationException, DatabaseException, MissingParamException,
    RecordNotFoundException, InstrumentException {

    CalibrationSet result = new CalibrationSet(instrument, getCalibrationType(),
      getTargets(conn, instrument));

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_RECENT_CALIBRATIONS_QUERY);
      stmt.setLong(1, instrument.getId());
      stmt.setString(2, getCalibrationType());
      // Get epoch milliseconds
      stmt.setLong(3, DateTimeUtils.dateToLong(date));
      records = stmt.executeQuery();
      while (!result.isComplete() && records.next()) {
        String target = records.getString(1);

        if (!result.containsTarget(target)) {
          result.add(calibrationFromResultSet(records, instrument));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  public CalibrationSet getCalibrationsAfter(Connection conn,
    Instrument instrument, LocalDateTime date)
    throws CalibrationException, DatabaseException, MissingParamException,
    RecordNotFoundException, InstrumentException {

    CalibrationSet result = new CalibrationSet(instrument, getCalibrationType(),
      getTargets(conn, instrument));

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_POST_CALIBRATIONS_QUERY);
      stmt.setLong(1, instrument.getId());
      stmt.setString(2, getCalibrationType());
      // Get epoch milliseconds
      stmt.setLong(3, DateTimeUtils.dateToLong(date));
      records = stmt.executeQuery();
      while (!result.isComplete() && records.next()) {
        String target = records.getString(1);

        if (!result.containsTarget(target)) {
          result.add(calibrationFromResultSet(records, instrument));
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
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
   * @throws InstrumentException
   */
  public CalibrationSet getCurrentCalibrations(DataSource dataSource,
    Instrument instrument) throws CalibrationException, DatabaseException,
    MissingParamException, RecordNotFoundException, InstrumentException {

    return getMostRecentCalibrations(dataSource, instrument,
      LocalDateTime.now());
  }

  /**
   * Retrieve all calibrations from the database of a given type, grouped by
   * target and ordered by date
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument
   * @param class
   *          The calibration type
   * @return The calibrations
   * @throws MissingParamException
   * @throws DatabaseException
   */
  public TreeMap<String, List<Calibration>> getCalibrations(
    DataSource dataSource, Instrument instrument)
    throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "instrument");

    TreeMap<String, List<Calibration>> calibrations = new TreeMap<String, List<Calibration>>();

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_CALIBRATIONS_QUERY);
      stmt.setLong(1, instrument.getId());
      stmt.setString(2, getCalibrationType());

      records = stmt.executeQuery();
      while (records.next()) {
        Calibration calibration = calibrationFromResultSet(records, instrument);
        if (!calibrations.containsKey(calibration.getTarget())) {
          calibrations.put(calibration.getTarget(),
            new ArrayList<Calibration>());
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

  private Calibration calibrationFromResultSet(ResultSet record,
    Instrument instrument) throws SQLException {
    long id = record.getLong(1);
    String target = record.getString(3);
    LocalDateTime deploymentDate = DateTimeUtils.longToDate(record.getLong(4));
    List<String> coefficients = StringUtils.delimitedToList(record.getString(5),
      ";");
    String calibrationClass = record.getString(6);

    return CalibrationFactory.createCalibration(getCalibrationType(),
      calibrationClass, id, instrument, deploymentDate, target, coefficients);
  }

  /**
   * Determine whether or not a calibration exists that coincides with the
   * specified calibration.
   *
   * <p>
   * This checks instrument, type, target and deployment date. If a match is
   * found with the same ID as the supplied calibration, this is not reported
   * since it's obvious that a calibration will clash with itself.
   * </p>
   *
   * @param dataSource
   *          A data source
   * @param calibration
   *          The calibration to be compared
   * @return {@code true} if a calibration exists; {@code false} otherwise
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public boolean calibrationExists(DataSource dataSource,
    Calibration calibration) throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(calibration, "calibration");

    boolean result = false;

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(CALIBRATION_EXISTS_QUERY);
      stmt.setLong(1, calibration.getInstrument().getId());
      stmt.setString(2, getCalibrationType());
      stmt.setLong(3,
        DateTimeUtils.dateToLong(calibration.getDeploymentDate()));
      stmt.setString(4, calibration.getTarget());

      records = stmt.executeQuery();
      if (records.next()) {
        if (records.getLong(1) != calibration.getId()) {
          result = true;
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
   * Get the list of possible calibration targets for a given instrument
   *
   * @param dataSource
   *          A data source
   * @param instrumentId
   *          The instrument's database ID
   * @return The targets
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If no external standard run types are found
   */
  public Map<String, String> getTargets(DataSource dataSource,
    Instrument instrument) throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException {

    Connection conn = null;
    Map<String, String> result = null;

    try {
      conn = dataSource.getConnection();
      result = getTargets(conn, instrument);
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting calibration targets", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the list of possible calibration targets for a given instrument
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument's database ID
   * @return The targets
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If no external standard run types are found
   */
  public abstract Map<String, String> getTargets(Connection conn,
    Instrument instrument) throws MissingParamException, DatabaseException,
    RecordNotFoundException, InstrumentException;

  /**
   * Get the calibration type for database actions
   *
   * @return The calibration type
   */
  public abstract String getCalibrationType();

  /**
   * Specifies whether or not a dataset must have a calibration prior to its
   * start date.
   *
   * @return {@code true} if datasets must be preceded by a calibration;
   *         {@code false} if not.
   */
  public abstract boolean priorCalibrationRequired();
}
