package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * Factory for creating {@link Calibration} objects
 *
 * @author Steve Jones
 *
 */
public class CalibrationFactory {

  /**
   * The package containing calibration classes
   */
  private static final String CALIBRATION_PACKAGE = "uk.ac.exeter.QuinCe.data.Instrument.Calibration";

  /**
   * Create a calibration object. The specific type of the calibration object is
   * dependent on the parameters passed in.
   *
   * @param calibrationType
   *          The high-level calibration type
   * @param calibrationClass
   *          The class of the desired calibration object
   * @param instrumentId
   *          The instrument to which the calibration applies
   * @param deploymentDate
   *          The deployment date (may be null)
   * @param target
   *          The target (sensor, external standard etc) of the calibration
   * @param coefficients
   *          The calibration coefficients
   * @return The Calibration object
   */
  public static Calibration createCalibration(String calibrationType,
    String calibrationClass, long id, Instrument instrument,
    LocalDateTime deploymentDate, String target,
    Map<String, String> coefficients) {
    Calibration result;

    switch (calibrationType) {
    case ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE: {
      try {
        result = ExternalStandardFactory.getExternalStandard(id, instrument,
          target, deploymentDate, coefficients);
      } catch (CalibrationException e) {
        throw e;
      } catch (Exception e) {
        throw new CalibrationException(e);
      }
      break;
    }
    case CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE: {
      try {
        result = new CalculationCoefficient(id, instrument, target,
          deploymentDate, coefficients);
      } catch (CalibrationException e) {
        throw e;
      } catch (Exception e) {
        throw new CalibrationException(e);
      }
      break;
    }
    case SensorCalibrationDB.SENSOR_CALIBRATION_TYPE: {

      try {
        String fullClass = CALIBRATION_PACKAGE + "." + calibrationClass;
        Class<?> clazz = Class.forName(fullClass);

        Constructor<?> constructor = clazz.getConstructor(long.class,
          Instrument.class, String.class, LocalDateTime.class, Map.class);
        result = (Calibration) constructor.newInstance(id, instrument, target,
          deploymentDate, coefficients);
      } catch (CalibrationException e) {
        throw e;
      } catch (Exception e) {
        throw new CalibrationException(e);
      }

      break;
    }
    default: {
      throw new UnrecognisedCalibrationTypeException(calibrationType);
    }
    }

    return result;
  }

  /**
   * Create a clone of a {@link Calibration} object.
   *
   * @param calibration
   *          The {@link Calibration}.
   * @return The clone.
   */
  public static Calibration clone(Calibration calibration) {

    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
    calibration.getCoefficients()
      .forEach(c -> map.put(c.getName(), c.getValue()));

    return createCalibration(calibration.getType(),
      calibration.getClass().getSimpleName(), calibration.getId(),
      calibration.getInstrument(), calibration.getDeploymentDate(),
      calibration.getTarget(), map);
  }
}
