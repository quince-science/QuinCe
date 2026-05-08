package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * Factory for creating {@link Calibration} objects.
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
   *          The high-level calibration type.
   * @param calibrationClass
   *          The class of the desired calibration object.
   * @param id
   *          The calibration's database ID.
   * @param instrument
   *          The instrument to which the calibration applies.
   * @param deploymentDate
   *          The deployment date (may be {@code null}).
   * @param target
   *          The target (sensor, external standard etc.) of the calibration.
   * @param coefficients
   *          The calibration coefficients.
   * @return The Calibration object.
   * @throws CalibrationException
   *           If the calibration type is not recognised.
   */
  public static Calibration createCalibration(String calibrationType,
    String calibrationClass, long id, Instrument instrument,
    LocalDateTime deploymentDate, String target,
    Map<String, String> coefficients) throws CalibrationException {
    Calibration result;

    switch (calibrationType) {
    case ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE: {
      try {
        result = ExternalStandardFactory.getExternalStandard(id, instrument,
          target, deploymentDate, coefficients);
      } catch (CalibrationException e) {
        throw e;
      } catch (InvocationTargetException e) {
        Throwable source = e.getCause();
        if (null == source) {
          source = e.getTargetException();
        }
        throw new CalibrationException(source);
      } catch (Exception e) {
        throw new CalibrationException(e);
      }
      break;
    }
    case CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE: {
      result = new CalculationCoefficient(id, instrument, target,
        deploymentDate, coefficients);
      break;
    }
    case SensorCalibrationDB.SENSOR_CALIBRATION_TYPE: {
      try {
        String fullClass = CALIBRATION_PACKAGE + "." + calibrationClass;

        @SuppressWarnings("unchecked")
        Class<? extends Calibration> clazz = (Class<? extends Calibration>) Class
          .forName(fullClass);

        Constructor<? extends Calibration> constructor = clazz.getConstructor(
          long.class, Instrument.class, String.class, LocalDateTime.class,
          Map.class);
        result = constructor.newInstance(id, instrument, target, deploymentDate,
          coefficients);
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
   * @throws CalibrationException
   *           If the source calibration is invalid.
   */
  public static Calibration clone(Calibration calibration)
    throws CalibrationException {

    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
    calibration.getCoefficients()
      .forEach(c -> map.put(c.getName(), c.getValue()));

    return createCalibration(calibration.getType(),
      calibration.getClass().getSimpleName(), calibration.getId(),
      calibration.getInstrument(), calibration.getDeploymentDate(),
      calibration.getTarget(), map);
  }
}
