package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.StringUtils;

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
   *          The calibration coefficients as a String of semi-colon separated
   *          numbers
   * @return The Calibration object
   */
  public static Calibration createCalibration(String calibrationType,
    String calibrationClass, long id, Instrument instrument,
    LocalDateTime deploymentDate, String target, String coefficients) {

    try {
      List<String> parsedCoefficients = StringUtils
        .delimitedToList(coefficients, ";");
      return createCalibration(calibrationType, calibrationClass, id,
        instrument, deploymentDate, target, parsedCoefficients);
    } catch (NumberFormatException e) {
      throw new CalibrationException(
        "Invalid coefficients list: " + coefficients);
    }
  }

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
    LocalDateTime deploymentDate, String target, List<String> coefficients) {
    Calibration result;

    switch (calibrationType) {
    case ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE: {
      try {
        result = new ExternalStandard(id, instrument, target, deploymentDate,
          coefficients);
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
          Instrument.class, String.class, LocalDateTime.class, List.class);
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
    return createCalibration(calibration.getType(),
      calibration.getClass().getSimpleName(), calibration.getId(),
      calibration.getInstrument(), calibration.getDeploymentDate(),
      calibration.getTarget(), calibration.getCoefficientsAsDelimitedList());
  }
}
