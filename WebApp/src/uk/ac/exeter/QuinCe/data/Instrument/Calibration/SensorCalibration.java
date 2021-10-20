package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * Abstract class for sensor calibrations. Sensor calibrations are of a common
 * type, but the calculations are made in different ways and may require
 * different parameters.
 *
 * @author Steve Jones
 */
public abstract class SensorCalibration extends Calibration {

  /**
   * Basic constructor
   *
   * @param instrumentId
   *          The instrument that the calibration is for
   * @param target
   *          The target sensor
   */
  protected SensorCalibration(Instrument instrument, String target) {
    super(instrument, SensorCalibrationDB.SENSOR_CALIBRATION_TYPE, target);
  }

  /**
   * Basic constructor with no target set
   *
   * @param instrumentId
   *          The instrument that the calibration is for
   */
  protected SensorCalibration(Instrument instrument) {
    super(instrument, SensorCalibrationDB.SENSOR_CALIBRATION_TYPE);
  }

  /**
   * Construct a complete sensor calibration object.
   *
   * @param instrumentId
   *          The instrument ID
   * @param target
   *          The target sensor
   * @param deploymentDate
   *          The deployment date
   * @param coefficients
   *          The calibration coefficients
   * @throws CalibrationException
   *           If the calibration details are invalid
   */
  protected SensorCalibration(long id, Instrument instrument, String target,
    LocalDateTime deploymentDate, Map<String, String> coefficients)
    throws CalibrationException {
    super(id, instrument, SensorCalibrationDB.SENSOR_CALIBRATION_TYPE, target);
    setDeploymentDate(deploymentDate);
    setCoefficients(coefficients);
    if (!validate()) {
      throw new CalibrationException(
        "Sensor calibration parameters are invalid");
    }
  }
}
