package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
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
    throws CalibrationException {

    try {
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
    } catch (Exception e) {
      throw new CalibrationException(e);
    }
  }

  @Override
  public String getCalibrationType() {
    return EXTERNAL_STANDARD_CALIBRATION_TYPE;
  }

  @Override
  public boolean allowCalibrationChangeInDataset() {
    return true;
  }

  @Override
  public boolean usePostCalibrations() {
    return false;
  }

  @Override
  public boolean timeAffectesCalibration() {
    return false;
  }

  @Override
  public boolean completeSetRequired() {
    return true;
  }
}
