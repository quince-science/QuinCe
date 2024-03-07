package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for storing and retrieving external standards information from the
 * database
 */
public class ExternalStandardDB extends CalibrationDB {

  /**
   * The calibration type for external standards
   */
  public static final String EXTERNAL_STANDARD_CALIBRATION_TYPE = "EXTERNAL_STANDARD";

  /**
   * Query to get the most recent standards for an instrument before a given
   * date
   */
  public static final String GET_STANDARD_SET_QUERY = "SELECT "
    + "c1.id, c1.target, c1.deployment_date, c1.coefficients, c1.class, c1.instrument_id "
    + "FROM calibration c1 INNER JOIN "
    + "(SELECT MAX(deployment_date) deployment_date, target, instrument_id "
    + "FROM calibration WHERE deployment_date < ? " + "AND instrument_id = ? "
    + "AND type = '" + EXTERNAL_STANDARD_CALIBRATION_TYPE + "' "
    + "GROUP BY target) "
    + "AS c2 ON c1.target = c2.target AND c1.deployment_date = c2.deployment_date "
    + "AND c1.instrument_id = c2.instrument_id";

  /**
   * The singleton instance of the class
   */
  private static ExternalStandardDB instance = null;

  /**
   * Basic constructor
   */
  public ExternalStandardDB() {
    super();
  }

  /**
   * Retrieve the singleton instance of the class
   *
   * @return The singleton
   */
  public static ExternalStandardDB getInstance() {
    if (null == instance) {
      instance = new ExternalStandardDB();
    }

    return instance;
  }

  /**
   * Destroy the singleton instance
   */
  public static void destroy() {
    instance = null;
  }

  @Override
  public Map<String, String> getTargets(Connection conn, Instrument instrument)
    throws MissingParamException, DatabaseException, RecordNotFoundException {
    List<String> standardNames = InstrumentDB.getRunTypes(conn,
      instrument.getId(), RunTypeCategory.INTERNAL_CALIBRATION.getType());
    if (standardNames.size() == 0) {
      throw new RecordNotFoundException(
        "No external standard names found for instrument "
          + instrument.getId());
    }

    Map<String, String> result = new TreeMap<String, String>();
    for (String name : standardNames) {
      result.put(name, name);
    }

    return result;
  }

  /**
   * Retrieve a CalibrationSet containing the external standards deployed
   * immediately before the specified date
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument for which the standards should be retrieved
   * @param date
   *          The date limit
   * @return The external standards
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the instrument does not exist
   */
  public CalibrationSet getStandardsSet(DataSource dataSource,
    Instrument instrument, LocalDateTime date)
    throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(dataSource, "dataSource");

    CalibrationSet result = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      result = getStandardsSet(conn, instrument, date);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving standards set", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Retrieve a CalibrationSet containing the external standards deployed
   * immediately before the specified date
   *
   * @param conn
   *          A database connection
   * @param instrumentId
   *          The instrument for which the standards should be retrieved
   * @param date
   *          The date limit
   * @return The external standards
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws RecordNotFoundException
   *           If the instrument does not exist
   */
  public CalibrationSet getStandardsSet(Connection conn, Instrument instrument,
    LocalDateTime date)
    throws DatabaseException, MissingParamException, RecordNotFoundException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkMissing(date, "date");

    CalibrationSet result = new CalibrationSet(instrument,
      EXTERNAL_STANDARD_CALIBRATION_TYPE, getTargets(conn, instrument));

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {

      stmt = conn.prepareStatement(GET_STANDARD_SET_QUERY);
      stmt.setLong(1, DateTimeUtils.dateToLong(date));
      stmt.setLong(2, instrument.getId());

      records = stmt.executeQuery();
      while (records.next()) {
        long id = records.getLong(1);
        String target = records.getString(2);
        LocalDateTime standardDate = DateTimeUtils
          .longToDate(records.getLong(3));
        Map<String, String> coefficients = CalibrationDB
          .makeCoefficientsFromJson(records.getString(4));
        String className = records.getString(5);
        result.add(CalibrationFactory.createCalibration(
          EXTERNAL_STANDARD_CALIBRATION_TYPE, className, id, instrument,
          standardDate, target, coefficients));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving standards set", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeResultSets(records);
    }

    return result;
  }

  @Override
  public String getCalibrationType() {
    return EXTERNAL_STANDARD_CALIBRATION_TYPE;
  }

  @Override
  public boolean priorCalibrationRequired() {
    return true;
  }
}
