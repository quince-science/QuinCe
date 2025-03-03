package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import uk.ac.exeter.QuinCe.web.Instrument.CalibrationEdit;
import uk.ac.exeter.QuinCe.web.Instrument.InvalidCalibrationEditException;

/**
 * Database methods for database actions related to {@link Calibration}s.
 */
public abstract class CalibrationDB {

  /**
   * Statement to add a new {@link Calibration} to the database.
   *
   * @see #addCalibration(Connection, CalibrationEdit)
   */
  private static final String ADD_CALIBRATION_STATEMENT = "INSERT INTO calibration "
    + "(instrument_id, type, target, deployment_date, coefficients, class) "
    + "VALUES (?, ?, ?, ?, ?, ?)";

  /**
   * Statement to update the details of a pre-existing {@link Calibration} in
   * the database.
   *
   * @see #updateCalibration(Connection, CalibrationEdit)
   */
  private static final String UPDATE_CALIBRATION_STATEMENT = "UPDATE calibration "
    + "SET instrument_id = ?, type = ?, target = ?, deployment_date = ?, "
    + "coefficients = ?, class = ? WHERE id = ?";

  /**
   * Statement to remove a {@link Calibration} from the database.
   *
   * @see #deleteCalibration(Connection, CalibrationEdit)
   */
  private static final String DELETE_CALIBRATION_STATEMENT = "DELETE FROM "
    + "calibration WHERE id = ?";

  /**
   * Query to determine whether a {@link Calibration} with a specified
   * {@link Instrument}, type and target already exists in the database.
   *
   * @see #calibrationExists(DataSource, Calibration)
   */
  private static final String CALIBRATION_EXISTS_QUERY = "SELECT "
    + "id FROM calibration WHERE instrument_id = ? AND "
    + "type = ? AND deployment_date = ? AND target = ?";

  /**
   * Query to get all calibrations of a given type for an {@link Instrument}.
   *
   * @see #getCalibrations(Connection, Instrument)
   */
  private static final String GET_CALIBRATIONS_QUERY = "SELECT "
    + "id, instrument_id, target, deployment_date, coefficients, class "
    + "FROM calibration WHERE " + "instrument_id = ? AND type = ? ORDER BY "
    + "target, deployment_date ASC";

  /**
   * Query to get all the calibrations for an {@link Instrument} grouped by
   * deployment date.
   *
   * @see #getCalibrationTimes(DataSource, Instrument)
   */
  private static final String CALIBRATION_TIMES_QUERY = "SELECT "
    + "deployment_date, type " // 2
    + "FROM calibration WHERE instrument_id = ? "
    + "GROUP BY deployment_date, type ORDER BY deployment_date ASC, type ASC";

  /**
   * Gson conversion type used to convert the {@link Calibration#coefficients}
   * from their JSON format in the database to a {@link Map}.
   *
   * @see #makeCoefficientsFromJson(String)
   */
  private static final Type coefficientsType = new TypeToken<Map<String, String>>() {
  }.getType();

  /**
   * Empty constructor.
   *
   * <p>
   * While most of the classes in QuinCe used to make database calls use
   * {@code static} methods exclusively, {@link Calibration}-related database
   * methods rely on {@code abstract} methods so that activities specific to the
   * sub-classes can vary as needed. These classes need to be instantiated as
   * singletons, and thus require a constructor to exist in this parent class.
   * </p>
   */
  protected CalibrationDB() {
    // Do nothing
  }

  /**
   * Add a new {@link Calibration} to the database.
   *
   * @param conn
   *          A database connection.
   * @param calibrationEdit
   *          The calibration details.
   * @throws DatabaseException
   *           If a database error occurs.
   * @see #ADD_CALIBRATION_STATEMENT
   */
  private void addCalibration(Connection conn, CalibrationEdit calibrationEdit)
    throws DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(calibrationEdit, "calibration");

    if (!calibrationEdit.validate()) {
      throw new ParameterException("Calibration coefficients",
        "Coefficients are invalid");
    }

    PreparedStatement stmt = null;
    ResultSet generatedKeys = null;

    try {
      stmt = conn.prepareStatement(ADD_CALIBRATION_STATEMENT);

      stmt.setLong(1, calibrationEdit.getInstrument().getId());
      stmt.setString(2, calibrationEdit.getType());
      stmt.setString(3, calibrationEdit.getTarget());
      stmt.setLong(4,
        DateTimeUtils.dateToLong(calibrationEdit.getDeploymentDate()));
      stmt.setString(5, calibrationEdit.getCoefficientsJson());
      stmt.setString(6, calibrationEdit.getCalibrationClass().getSimpleName());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing calibration", e);
    } finally {
      DatabaseUtils.closeResultSets(generatedKeys);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Update an existing {@link Calibration}.
   *
   * @param conn
   *          A database connection.
   * @param calibrationEdit
   *          The {@link Calibration} details.
   * @throws DatabaseException
   *           If a database error occurs.
   * @see #UPDATE_CALIBRATION_STATEMENT
   */
  private void updateCalibration(Connection conn,
    CalibrationEdit calibrationEdit) throws DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(calibrationEdit, "calibration");
    MissingParam.checkMissing(calibrationEdit.getDeploymentDate(),
      "calibration deployment date");

    if (!calibrationEdit.validate()) {
      throw new ParameterException("Calibration coefficients",
        "Coefficients are invalid");
    }

    try (PreparedStatement stmt = conn
      .prepareStatement(UPDATE_CALIBRATION_STATEMENT);) {

      stmt.setLong(1, calibrationEdit.getInstrument().getId());
      stmt.setString(2, calibrationEdit.getType());
      stmt.setString(3, calibrationEdit.getTarget());
      stmt.setLong(4,
        DateTimeUtils.dateToLong(calibrationEdit.getDeploymentDate()));
      stmt.setString(5, calibrationEdit.getCoefficientsJson());
      stmt.setString(6, calibrationEdit.getCalibrationClass().getSimpleName());
      stmt.setLong(7, calibrationEdit.getCalibrationId());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing calibration", e);
    }
  }

  /**
   * Remove a {@link Calibration} from the database.
   *
   * @param conn
   *          A database connection.
   * @param calibrationEdit
   *          The {@link Calibration} details.
   * @throws DatabaseException
   * @see #DELETE_CALIBRATION_STATEMENT
   */
  private void deleteCalibration(Connection conn,
    CalibrationEdit calibrationEdit) throws DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkPositive(calibrationEdit.getCalibrationId(),
      "edit calibrationId");

    try (PreparedStatement stmt = conn
      .prepareStatement(DELETE_CALIBRATION_STATEMENT);) {

      stmt.setLong(1, calibrationEdit.getCalibrationId());
      stmt.execute();

    } catch (SQLException e) {
      throw new DatabaseException("Error while deleting calibration", e);
    }
  }

  /**
   * Retrieve all calibrations for an {@link Instrument} of a given type,
   * grouped by target and ordered by deployment date.
   *
   * @param dataSource
   *          A data source.
   * @param instrument
   *          The instrument.
   * @return The calibrations.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws CalibrationException
   *           If any of the retrieve calibration details are invalid.
   * @see #getCalibrations(Connection, Instrument)
   */
  public TreeMap<String, TreeSet<Calibration>> getCalibrations(
    DataSource dataSource, Instrument instrument)
    throws DatabaseException, CalibrationException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "instrument");

    try (Connection conn = dataSource.getConnection();) {
      return getCalibrations(conn, instrument);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving calibrations", e);
    }
  }

  /**
   * Retrieve all calibrations for an {@link Instrument} of a given type,
   * grouped by target and ordered by deployment date.
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The instrument.
   * @return The calibrations.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws CalibrationException
   *           If any of the retrieve calibration details are invalid.
   */
  public TreeMap<String, TreeSet<Calibration>> getCalibrations(Connection conn,
    Instrument instrument) throws DatabaseException, CalibrationException {
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

  /**
   * Build a {@link Calibration} from the current record in a {@link ResultSet}.
   *
   * @param record
   *          The {@link ResultSet}.
   * @param instrument
   *          The {@link Instrument} that the {@link Calibration} belongs to.
   * @return The {@link Calibration} object.
   * @throws SQLException
   *           If an error occurs while reading the record.
   * @throws CalibrationException
   *           If any of the read calibration details are invalid.
   * @see CalibrationFactory#createCalibration(String, String, long, Instrument,
   *      LocalDateTime, String, Map)
   */
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

  /**
   * Convert a {@link Calibration}'s coefficients JSON string (as they are
   * stored in the database) to a {@link Map}.
   *
   * @param json
   *          The coefficients JSON string.
   * @return The coefficients {@link Map}.
   * @see #coefficientsType
   */
  protected static Map<String, String> makeCoefficientsFromJson(String json) {
    return new Gson().fromJson(json, coefficientsType);
  }

  /**
   * Determine whether or not a calibration exists that coincides with the
   * specified calibration.
   *
   * <p>
   * This checks the instrument, type, target and deployment date. If a match is
   * found with the same database ID as the supplied calibration, this is not
   * reported since it's obvious that a calibration will clash with itself.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param calibration
   *          The calibration to be compared.
   * @return {@code true} if a matching calibration exists; {@code false}
   *         otherwise.
   * @throws DatabaseException
   *           If a database error occurs.
   * @see #CALIBRATION_EXISTS_QUERY
   */
  public boolean calibrationExists(DataSource dataSource,
    Calibration calibration) throws DatabaseException {
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
   * Get the list of possible calibration targets for a given
   * {@link Instrument}.
   *
   * <p>
   * This is a wrapper function to {@link #getTargets(Connection, Instrument)},
   * which is an {@code abstract} method to be implemented by sub-classes for
   * each {@link Calibration} type. The method will therefore only return
   * targets for the relevant {@link Calibration} type.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param instrument
   *          The instrument.
   * @return The targets
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws RecordNotFoundException
   *           If no targets are found.
   * @see #getTargets(Connection, Instrument)
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

    return new CalibrationSet(getTargets(conn, instrument), start, end, this,
      allCalibrations);
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
   * Indicates whether a calibration can change within a {@link DataSet}.
   *
   * @return {@code true} if a calibration values can change within the bounds
   *         of a dataset; {@code false} if they cannot.
   */
  public abstract boolean allowCalibrationChangeInDataset();

  /**
   * Indicates whether post-calibrations are used in the processing of a
   * {@link DataSet}.
   *
   * <p>
   * Note that if this function returns {@code true} it will not prevent a
   * {@link DataSet} from being processed, but flags will be set on the
   * {@link DataSet} to indicate that a post-calibration was not used.
   * </p>
   *
   * @return {@code true} if post-calibrations are used; {@code false} if not.
   */
  public abstract boolean usePostCalibrations();

  /**
   * Indicates whether or not the time of a {@link Calibration} impacts the
   * effect it has on the calibration of a {@link DataSet}.
   *
   * @return {@code true} if the time of a {@link Calibration} changes its
   *         effect on a {@link DataSet}; {@code false} otherwise.
   *
   */
  public abstract boolean timeAffectesCalibration();

  /**
   * Indicates whether or not a complete set of {@link Calibration}s is required
   * to process a {@link DataSet}.
   *
   * <p>
   * In practice, this boils down to a complete set of {@link Calibrations}
   * being set before the beginning of the {@link DataSet}; changes to fewer
   * than the complete set later on result in a {@link CalibrationSet} building
   * a set based on the updated {@link Calibration}s and the ones before the
   * {@link DataSet} that have not changed.
   * </p>
   *
   * @return {@code true} if a complete set of {@link Calibration}s is required,
   *         {@code false} if not.
   */
  public abstract boolean completeSetRequired();

  public void commitEdits(DataSource dataSource,
    Collection<CalibrationEdit> edits)
    throws DatabaseException, InvalidCalibrationEditException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(edits, "edits", false);

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      for (CalibrationEdit edit : edits) {

        switch (edit.getAction()) {
        case CalibrationEdit.ADD: {
          addCalibration(conn, edit);
          break;
        }
        case CalibrationEdit.EDIT: {
          updateCalibration(conn, edit);
          break;
        }
        case CalibrationEdit.DELETE: {
          deleteCalibration(conn, edit);
          break;
        }
        default: {
          throw new InvalidCalibrationEditException(
            "Invalid action " + edit.getAction());
        }
        }
      }

      conn.commit();
    } catch (SQLException e) {
      DatabaseUtils.rollBack(conn);
      throw new DatabaseException("Error storing calibration edits", e);
    } finally {
      try {
        conn.setAutoCommit(true);
      } catch (Exception e) {
        // NOOP
      }
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Retrieve the times of all {@link Calibration}s defined for an
   * {@link Instrument}.
   *
   * <p>
   * Returns a {@link Map} of {@code Calibration Type -> Times}, so for each
   * calibration type there is a {@link List} of the times for which
   * {@link Calibration}s have been defined (in ascending time order). There is
   * only one entry per combination of calibration type/time, so multiple
   * calibrations defined for the same type and time will appear only once.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The Instrument.
   * @return The calibration times.
   * @throws DatabaseException
   */
  public static TreeMap<LocalDateTime, List<String>> getCalibrationTimes(
    DataSource dataSource, Instrument instrument) throws DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrument, "instrument");

    TreeMap<LocalDateTime, List<String>> result = new TreeMap<LocalDateTime, List<String>>();

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn
        .prepareStatement(CALIBRATION_TIMES_QUERY);) {

      stmt.setLong(1, instrument.getId());

      try (ResultSet records = stmt.executeQuery()) {

        while (records.next()) {
          LocalDateTime time = DateTimeUtils.longToDate(records.getLong(1));
          String type = records.getString(2);

          if (!result.containsKey(time)) {
            result.put(time, new ArrayList<String>());
          }

          result.get(time).add(type);
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error getting calibration info", e);
    }

    return result;
  }
}
