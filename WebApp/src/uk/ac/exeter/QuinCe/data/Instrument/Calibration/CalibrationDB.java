package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.ParameterException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Database methods for database actions related to calibrations
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
   * JSON -> Coefficient map conversion type.
   *
   * @see #makeCoefficientsFromJson(String)
   */
  private static final Type coefficientsType = new TypeToken<Map<String, String>>() {
  }.getType();

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
      stmt.setString(5, calibration.getCoefficientsJson());
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
      stmt.setString(5, calibration.getCoefficientsJson());
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

  public TreeMap<String, TreeSet<Calibration>> getCalibrations(
    DataSource dataSource, Instrument instrument)
    throws DatabaseException, MissingParamException, CalibrationException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "instrument");

    try (Connection conn = dataSource.getConnection();) {
      return getCalibrations(conn, instrument);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    }
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
   * @throws CalibrationException
   */
  public TreeMap<String, TreeSet<Calibration>> getCalibrations(Connection conn,
    Instrument instrument)
    throws MissingParamException, DatabaseException, CalibrationException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(instrument, "instrument");

    TreeMap<String, TreeSet<Calibration>> calibrations = new TreeMap<String, TreeSet<Calibration>>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_CALIBRATIONS_QUERY);
      stmt.setLong(1, instrument.getId());
      stmt.setString(2, getCalibrationType());

      records = stmt.executeQuery();
      while (records.next()) {
        Calibration calibration = calibrationFromResultSet(records, instrument);
        if (!calibrations.containsKey(calibration.getTarget())) {
          calibrations.put(calibration.getTarget(), new TreeSet<Calibration>());
        }

        calibrations.get(calibration.getTarget()).add(calibration);
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return calibrations;
  }

  private Calibration calibrationFromResultSet(ResultSet record,
    Instrument instrument) throws SQLException, CalibrationException {
    long id = record.getLong(1);
    String target = record.getString(3);
    LocalDateTime deploymentDate = DateTimeUtils.longToDate(record.getLong(4));

    Map<String, String> coefficients = makeCoefficientsFromJson(
      record.getString(5));

    String calibrationClass = record.getString(6);

    return CalibrationFactory.createCalibration(getCalibrationType(),
      calibrationClass, id, instrument, deploymentDate, target, coefficients);
  }

  protected static Map<String, String> makeCoefficientsFromJson(String json) {
    return new Gson().fromJson(json, coefficientsType);
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
   * Build a {@link CalibrationSet} object to cover the time period of the
   * specified {@link DataSet}.
   * 
   * @param conn
   * @param instrument
   * @param dataset
   * @return
   * @throws MissingParamException
   * @throws DatabaseException
   * @throws RecordNotFoundException
   * @throws InstrumentException
   * @throws CalibrationException
   */
  public CalibrationSet getCalibrationSet(Connection conn, DataSet dataset)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException, CalibrationException {

    return getCalibrationSet(conn, dataset.getInstrument(), dataset.getStart(),
      dataset.getEnd());
  }

  public CalibrationSet getCalibrationSet(Connection conn,
    Instrument instrument, LocalDateTime start, LocalDateTime end)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException, CalibrationException {

    TreeMap<String, TreeSet<Calibration>> allCalibrations = getCalibrations(
      conn, instrument);

    return new CalibrationSet(getTargets(conn, instrument), start, end,
      allowCalibrationChangeInDataset(), allCalibrations);

  }

  public CalibrationSet getCalibrationSet(DataSource dataSource,
    Instrument instrument, LocalDateTime start, LocalDateTime end)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException, CalibrationException {

    try (Connection conn = dataSource.getConnection();) {
      return getCalibrationSet(conn, instrument, start, end);
    } catch (SQLException e) {
      throw new DatabaseException("Error getting calibration set", e);
    }
  }

  /**
   * Indicates whether a calibration can change within a datasets.
   *
   * @return {@code true} if a calibration values can change within the bounds
   *         of a dataset; {@code false} if they cannot.
   */
  public abstract boolean allowCalibrationChangeInDataset();
}
