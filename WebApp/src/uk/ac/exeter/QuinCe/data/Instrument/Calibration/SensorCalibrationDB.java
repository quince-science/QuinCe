package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for storing and retrieving sensor calibrations from the database
 */
public class SensorCalibrationDB extends CalibrationDB {

  /**
   * Indentifier for sensor calibrations
   */
  public static final String SENSOR_CALIBRATION_TYPE = "SENSOR_CALIBRATION";

  /**
   * The singleton instance of the class
   */
  private static SensorCalibrationDB instance = null;

  /**
   * Basic constructor
   */
  public SensorCalibrationDB() {
    super();
  }

  /**
   * Retrieve the singleton instance of the class
   *
   * @return The singleton
   */
  public static SensorCalibrationDB getInstance() {
    if (null == instance) {
      instance = new SensorCalibrationDB();
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
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {

    return InstrumentDB.getCalibratableSensors(conn, instrument.getId());
  }

  @Override
  public String getCalibrationType() {
    return SENSOR_CALIBRATION_TYPE;
  }

  @Override
  public boolean allowCalibrationChangeInDataset() {
    return true;
  }
}
