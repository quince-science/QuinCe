package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Methods for storing and retrieving sensor calibrations from the database
 * @author Steve Jones
 *
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
  public List<String> getTargets(Connection conn, long instrumentId) throws MissingParamException, DatabaseException, RecordNotFoundException {
    return InstrumentDB.getCalibratableSensors(conn, instrumentId);
  }

  @Override
  public String getCalibrationType() {
    return SENSOR_CALIBRATION_TYPE;
  }
}
