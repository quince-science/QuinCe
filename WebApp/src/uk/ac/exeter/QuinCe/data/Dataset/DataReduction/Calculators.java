package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

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

  public static Double calcSeaLevelPressure(Double measuredPressure,
    Double temperature, Float sensorHeight) {
    Double correction = (measuredPressure * MOLAR_MASS_AIR)
      / (Calculators.kelvin(temperature) * 8.314) * 9.8 * sensorHeight;
    return measuredPressure + correction;
  }
}
