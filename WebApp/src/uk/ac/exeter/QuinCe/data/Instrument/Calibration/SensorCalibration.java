package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Abstract class for sensor calibrations. Sensor calibrations are of a common type,
 * but the calculations are made in different ways and may require different parameters.
 *
 * @author Steve Jones
 */
public abstract class SensorCalibration extends Calibration {

  /**
   * Basic constructor
   * @param instrumentId The instrument that the calibration is for
   * @param target The target sensor
   */
  protected SensorCalibration(long instrumentId, String target) {
    super(instrumentId, SensorCalibrationDB.SENSOR_CALIBRATION_TYPE, target);
  }

  /**
   * Basic constructor with no target set
   * @param instrumentId The instrument that the calibration is for
   */
  protected SensorCalibration(long instrumentId) {
    super(instrumentId, SensorCalibrationDB.SENSOR_CALIBRATION_TYPE);
  }

  /**
   * Construct a complete sensor calibration object.
   * @param instrumentId The instrument ID
   * @param target The target sensor
   * @param deploymentDate The deployment date
   * @param coefficients The calibration coefficients
   * @throws CalibrationException If the calibration details are invalid
   */
  protected SensorCalibration(long instrumentId, String target, LocalDateTime deploymentDate, List<Double> coefficients) throws CalibrationException {
    super(instrumentId, SensorCalibrationDB.SENSOR_CALIBRATION_TYPE, target);
    setDeploymentDate(deploymentDate);
    setCoefficients(coefficients);
    if (!validate()) {
      throw new CalibrationException("Sensor calibration parameters are invalid");
    }
  }
}
