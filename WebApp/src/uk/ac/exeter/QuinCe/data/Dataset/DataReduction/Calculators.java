package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Provides static methods for common calculations in data reduction.
 *
 * @author Steve Jones
 *
 */
public class Calculators {

  /**
   * The conversion factor from Pascals to Atmospheres
   */
  private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

  private static final double MOLAR_MASS_AIR = 28.97e-3;

  /**
   * Convert a temperature in °C to °K
   *
   * @param celsius
   *          Celsius temperature
   * @return Kelvin temperature
   */
  public static Double kelvin(Double celsius) {
    return celsius + 273.15;
  }

  /**
   * Convert a temperature in °K to °C
   *
   * @param kelvin
   *          Kelvin temperature
   * @return Celsius temperature
   */
  public static Double celsius(Double kelvin) {
    return kelvin - 273.15;
  }

  /**
   * Convert a pressure in hPa it atmospheres
   *
   * @param hPa
   *          Pressure in hPa
   * @return Pressure in atmospheres
   */
  public static Double hPaToAtmospheres(Double hPa) {
    return hPa * 100 * PASCALS_TO_ATMOSPHERES;
  }

  /**
   * Converts pCO<sub>2</sub> to fCO<sub>2</sub>
   *
   * @param pco2
   *          pCO<sub>2</sub> at target temperature
   * @param xCO2InGas
   *          The calibrated, dried xCO<sub>2</sub> value
   * @param pressure
   *          The pressure in hPa
   * @param temperature
   *          The temperature in °C
   * @return The fCO<sub>2</sub> value
   */
  public static Double calcfCO2(Double pco2, Double xCO2InGas, Double pressure,
    Double temperature) {

    Double kelvin = Calculators.kelvin(temperature);
    Double B = -1636.75 + 12.0408 * kelvin - 0.0327957 * Math.pow(kelvin, 2)
      + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
    Double delta = 57.7 - 0.118 * kelvin;

    return pco2 * Math.exp(((B + 2 * Math.pow(1 - xCO2InGas * 1e-6, 2) * delta)
      * hPaToAtmospheres(pressure)) / (82.0575 * kelvin));
  }

  /**
   * Calculates pCO<sub>2</sub> in water
   *
   * @param xCO2
   *          The dry, calibrated xCO<sub>2</sub> value
   * @param pressure
   *          The pressure
   * @param pH2O
   *          The water vapour pressure
   * @return pCO<sub>2</sub> in water
   */
  public static Double calcpCO2TEWet(Double xCO2, Double pressure,
    Double pH2O) {
    return xCO2 * (hPaToAtmospheres(pressure) - pH2O);
  }

  /**
   * Calculates the water vapour pressure (pH<sub>2</sub>O). From Weiss and
   * Price (1980)
   *
   * @param salinity
   *          Salinity
   * @param temperature
   *          Temperature (in celsius)
   * @return The calculated pH2O value
   */
  public static Double calcPH2O(Double salinity, Double temperature) {
    double kelvin = Calculators.kelvin(temperature);
    return Math.exp(24.4543 - 67.4509 * (100 / kelvin)
      - 4.8489 * Math.log(kelvin / 100) - 0.000544 * salinity);
  }

  /**
   * Adjust a measured pressure to sea level.
   *
   * If the supplied {@code sensorHeight} is {@code null}, assume that no
   * correction is required.
   *
   * @param measuredPressure
   *          The measured pressure.
   * @param temperature
   *          The temperature at which the pressure was measured.
   * @param sensorHeight
   *          The height of the sensor.
   * @return The adjusted pressure.
   */
  public static Double calcSeaLevelPressure(Double measuredPressure,
    Double temperature, Float sensorHeight) {

    Double result = measuredPressure;

    if (null != sensorHeight) {
      Double correction = (measuredPressure * MOLAR_MASS_AIR)
        / (Calculators.kelvin(temperature) * 8.314) * 9.8 * sensorHeight;
      result = measuredPressure + correction;
    }

    return result;
  }

  public static Double interpolate(LocalDateTime time0, Double y0,
    LocalDateTime time1, Double y1, LocalDateTime measurementTime) {
    Double result = null;

    if (null != y0 && null != y1) {
      double x0 = DateTimeUtils.dateToLong(time0);
      double x1 = DateTimeUtils.dateToLong(time1);
      result = interpolate(x0, y0, x1, y1,
        DateTimeUtils.dateToLong(measurementTime));
    } else if (null != y0) {
      result = y0;
    } else if (null != y1) {
      result = y1;
    }

    return result;
  }

  public static double interpolate(double x0, double y0, double x1, double y1,
    double x) {

    return (y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0);
  }

  public static BigDecimal interpolate(BigDecimal x0, BigDecimal y0,
    BigDecimal x1, BigDecimal y1, BigDecimal x) {

    BigDecimal result = null;

    boolean priorNull = null == x0 || null == y0;
    boolean postNull = null == x1 || null == y1;

    if (!priorNull && !postNull) {
      BigDecimal X1minusX = x1.subtract(x);
      BigDecimal XminusX0 = x.subtract(x0);
      BigDecimal X1minusX0 = x1.subtract(x0);

      BigDecimal Y0timesX1minusX = y0.multiply(X1minusX);
      BigDecimal Y1timesXminusX0 = y1.multiply(XminusX0);

      BigDecimal top = Y0timesX1minusX.add(Y1timesXminusX0);

      result = top.divide(X1minusX0, 10, RoundingMode.HALF_UP);
    } else if (!priorNull) {
      result = y0;
    } else if (!postNull) {
      result = y1;
    }

    return result;
  }

  public static Double interpolate(Map.Entry<Double, Double> prior,
    Map.Entry<Double, Double> post, Double x) {

    Double result = null;

    if (!isNull(prior) && !isNull(post)) {
      double x0 = prior.getKey();
      double y0 = prior.getValue();
      double x1 = post.getKey();
      double y1 = post.getValue();
      result = Calculators.interpolate(x0, y0, x1, y1, x);
    } else if (!isNull(prior)) {
      result = prior.getValue();
    } else if (!isNull(post)) {
      result = post.getValue();
    }

    return result;
  }

  private static boolean isNull(Map.Entry<Double, Double> mapEntry) {

    boolean result = false;

    if (null == mapEntry) {
      result = true;
    } else if (null == mapEntry.getKey() || mapEntry.getKey().isNaN()) {
      result = true;
    } else if (null == mapEntry.getValue() || mapEntry.getValue().isNaN()) {
      result = true;
    }

    return result;
  }

  /**
   * Calculates pCO<sub>2</sub> at the intake (sea surface) temperature. From
   * Takahashi et al. (2009)
   *
   * @param pco2TEWet
   *          The pCO<sub>2</sub> at equilibrator temperature
   * @param eqt
   *          The equilibrator temperature
   * @param sst
   *          The intake temperature
   * @return The pCO<sub>2</sub> at intake temperature
   */
  public static Double calcCO2AtSST(Double co2AtEquilibrator, Double eqt,
    Double sst) {
    return co2AtEquilibrator
      * Math.exp(0.0423 * (Calculators.kelvin(sst) - Calculators.kelvin(eqt)));
  }
}
