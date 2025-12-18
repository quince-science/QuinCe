package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

/**
 * Tests for the {@link Calculators} class.
 */
public class CalculatorsTest extends BaseTest {

  /**
   * Test {@link Calculators#calcPH2O(Double, Double)}.
   */
  @Test
  public void calcPH2OTest() {
    Double temp = 11.17;
    Double salinity = 34.18548;
    Double targetpH2O = 0.012847;

    assertEquals(targetpH2O, Calculators.calcPH2O(salinity, temp), 0.000001);
  }

  /**
   * Test {@link Calculators#calcPH2O(Double, Double)} with {@code NaN}
   * salinity.
   */
  @Test
  public void calcPH2ONanSalinityTest() {
    assertEquals(Double.NaN, Calculators.calcPH2O(Double.NaN, 11.17));
  }

  /**
   * Test {@link Calculators#calcPH2O(Double, Double)} with {@code NaN}
   * salinity.
   */
  @Test
  public void calcPH2ONanTempTest() {
    assertEquals(Double.NaN, Calculators.calcPH2O(35.01, Double.NaN));
  }

  /**
   * Test {@link Calculators#calcpCO2TEWet(Double, Double, Double)}.
   */
  @Test
  public void calcPco2WetTest() {
    Double xCO2 = 350.43;
    Double pressure = 1020.32;
    Double pH2O = 0.016;

    assertEquals(347.26826, Calculators.calcpCO2TEWet(xCO2, pressure, pH2O),
      0.00001);
  }

  /**
   * Test {@link Calculators#calcpCO2TEWet(Double, Double, Double)} with a
   * {@code NaN} xCO₂ value.
   */
  @Test
  public void calcPco2WetNanXCO2Test() {
    Double xCO2 = Double.NaN;
    Double pressure = 1020.32;
    Double pH2O = 0.016;

    assertEquals(Double.NaN, Calculators.calcpCO2TEWet(xCO2, pressure, pH2O),
      0.00001);
  }

  /**
   * Test {@link Calculators#calcpCO2TEWet(Double, Double, Double)} with a
   * {@code NaN} pressure value.
   */
  @Test
  public void calcPco2WetNanPressureTest() {
    Double xCO2 = 350.43;
    Double pressure = Double.NaN;
    Double pH2O = 0.016;

    assertEquals(Double.NaN, Calculators.calcpCO2TEWet(xCO2, pressure, pH2O),
      0.00001);
  }

  /**
   * Test {@link Calculators#calcpCO2TEWet(Double, Double, Double)} with a
   * {@code NaN} pH₂O value.
   */
  @Test
  public void calcPco2WetNanPH2OTest() {
    Double xCO2 = 350.43;
    Double pressure = 1020.32;
    Double pH2O = Double.NaN;

    assertEquals(Double.NaN, Calculators.calcpCO2TEWet(xCO2, pressure, pH2O),
      0.00001);
  }

  /**
   * Test {@link Calculators#calcfCO2(Double, Double, Double, Double)}.
   */
  @Test
  public void calcFCO2Test() {
    Double pCO2 = 347.26;
    Double xCO2 = 350.43;
    Double pressure = 1020.32;
    Double temperature = 10.82;

    assertEquals(345.92746,
      Calculators.calcfCO2(pCO2, xCO2, pressure, temperature), 0.0001);
  }

  /**
   * Test {@link Calculators#calcfCO2(Double, Double, Double, Double)} with a
   * {@code NaN} pCO₂ value.
   */
  @Test
  public void calcFCO2NaNpCO2Test() {
    Double pCO2 = Double.NaN;
    Double xCO2 = 350.43;
    Double pressure = 1020.32;
    Double temperature = 10.82;

    assertEquals(Double.NaN,
      Calculators.calcfCO2(pCO2, xCO2, pressure, temperature), 0.0001);
  }

  /**
   * Test {@link Calculators#calcfCO2(Double, Double, Double, Double)} with a
   * {@code NaN} xCO₂ value.
   */
  @Test
  public void calcFCO2NaNXCO2Test() {
    Double pCO2 = 347.26;
    Double xCO2 = Double.NaN;
    Double pressure = 1020.32;
    Double temperature = 10.82;

    assertEquals(Double.NaN,
      Calculators.calcfCO2(pCO2, xCO2, pressure, temperature), 0.0001);
  }

  /**
   * Test {@link Calculators#calcfCO2(Double, Double, Double, Double)} with a
   * {@code NaN} pressure value.
   */
  @Test
  public void calcFCO2NaNPressureTest() {
    Double pCO2 = 347.26;
    Double xCO2 = 350.43;
    Double pressure = Double.NaN;
    Double temperature = 10.82;

    assertEquals(Double.NaN,
      Calculators.calcfCO2(pCO2, xCO2, pressure, temperature), 0.0001);
  }

  /**
   * Test {@link Calculators#calcfCO2(Double, Double, Double, Double)} with a
   * {@code NaN} temperature value.
   */
  @Test
  public void calcFCO2NaNTempTest() {
    Double pCO2 = 347.26;
    Double xCO2 = 350.43;
    Double pressure = 1020.32;
    Double temperature = Double.NaN;

    assertEquals(Double.NaN,
      Calculators.calcfCO2(pCO2, xCO2, pressure, temperature), 0.0001);
  }

  /**
   * Test {@link Calculators#kelvin(Double)} with a positive value.
   */
  @Test
  public void kelvinPositiveTest() {
    assertEquals(282.63D, Calculators.kelvin(9.48D), 0.01);
  }

  /**
   * Test {@link Calculators#kelvin(Double)} with a zero value.
   */
  @Test
  public void kelvinZeroTest() {
    assertEquals(273.15D, Calculators.kelvin(0D), 0.01);
  }

  /**
   * Test {@link Calculators#kelvin(Double)} with a negative value.
   */
  @Test
  public void kelvinNegativeTest() {
    assertEquals(263.67D, Calculators.kelvin(-9.48D), 0.01);
  }

  /**
   * Test {@link Calculators#celsius(Double)} with a positive result.
   */
  @Test
  public void celsiusPositiveTest() {
    assertEquals(9.48D, Calculators.celsius(282.63D), 0.01);
  }

  /**
   * Test {@link Calculators#celsius(Double)} with a zero result.
   */
  @Test
  public void celsiusZeroTest() {
    assertEquals(0D, Calculators.celsius(273.15D), 0.01);
  }

  /**
   * Test {@link Calculators#celsius(Double)} with a negative result.
   */
  @Test
  public void celsiusNegativeTest() {
    assertEquals(-9.48D, Calculators.celsius(263.67D), 0.01);
  }

  /**
   * Test {@link Calculators#calcCO2AtSST(Double, Double, Double)}.
   */
  @Test
  public void calcCO2AtSSTTest() {
    assertEquals(385.9254D, Calculators.calcCO2AtSST(402.43D, 6.34D, 5.35D),
      0.0001D);
  }

  /**
   * Test {@link Calculators#calcSeaLevelPressure(Double, Double, Float)}.
   */
  @Test
  public void calcSeaLevelPressureTest() {
    assertEquals(1024.5142D,
      Calculators.calcSeaLevelPressure(1023.244D, 7.44D, 10.2F), 0.0001D);
  }

  /**
   * Test {@link Calculators#calcSeaLevelPressure(Double, Double, Float)} with a
   * {@code null} height value.
   */
  @Test
  public void calcSeaLevelPressureNoHeightTest() {
    assertEquals(1023.244D,
      Calculators.calcSeaLevelPressure(1023.244D, 7.44D, null), 0.0001D);
  }

  /**
   * Test
   * {@link Calculators#interpolate(double, double, double, double, double)}.
   */
  @Test
  public void interpolateDoublesTest() {

    double x0 = 26.533D;
    double y0 = 8.328D;
    double x1 = 60.952D;
    double y1 = 15.685D;
    double targetX = 37.765D;

    assertEquals(10.7288D, Calculators.interpolate(x0, y0, x1, y1, targetX),
      0.0001D);
  }

  /**
   * Test
   * {@link Calculators#interpolate(LocalDateTime, Double, LocalDateTime, Double, LocalDateTime)}
   * with {@code null} y values.
   */
  @Test
  public void interpolateTimesNullYsTest() {
    LocalDateTime time0 = LocalDateTime.of(2020, 1, 1, 12, 10, 00);
    Double y0 = null;
    LocalDateTime time1 = LocalDateTime.of(2020, 1, 1, 12, 43, 00);
    Double y1 = null;
    LocalDateTime targetTime = LocalDateTime.of(2020, 1, 1, 12, 11, 00);

    assertNull(Calculators.interpolate(time0, y0, time1, y1, targetTime));
  }

  /**
   * Test
   * {@link Calculators#interpolate(LocalDateTime, Double, LocalDateTime, Double, LocalDateTime)}
   * with a {@code null} first y value.
   */
  @Test
  public void interpolateTimesNullY0Test() {
    LocalDateTime time0 = LocalDateTime.of(2020, 1, 1, 12, 10, 00);
    Double y0 = null;
    LocalDateTime time1 = LocalDateTime.of(2020, 1, 1, 12, 43, 00);
    Double y1 = 50.602D;
    LocalDateTime targetTime = LocalDateTime.of(2020, 1, 1, 12, 11, 00);

    assertEquals(50.602D,
      Calculators.interpolate(time0, y0, time1, y1, targetTime), 0.0001D);
  }

  /**
   * Test
   * {@link Calculators#interpolate(LocalDateTime, Double, LocalDateTime, Double, LocalDateTime)}
   * with a {@code null} second y value.
   */
  @Test
  public void interpolateTimesNullY1Test() {
    LocalDateTime time0 = LocalDateTime.of(2020, 1, 1, 12, 10, 00);
    Double y0 = 5.666D;
    LocalDateTime time1 = LocalDateTime.of(2020, 1, 1, 12, 43, 00);
    Double y1 = null;
    LocalDateTime targetTime = LocalDateTime.of(2020, 1, 1, 12, 11, 00);

    assertEquals(5.666D,
      Calculators.interpolate(time0, y0, time1, y1, targetTime), 0.0001D);
  }

  /**
   * Test
   * {@link Calculators#interpolate(LocalDateTime, Double, LocalDateTime, Double, LocalDateTime)}
   * with all values.
   */
  @Test
  public void interpolateTimesTest() {
    LocalDateTime time0 = LocalDateTime.of(2020, 1, 1, 12, 10, 00);
    Double y0 = 5.666D;
    LocalDateTime time1 = LocalDateTime.of(2020, 1, 1, 12, 43, 00);
    Double y1 = 50.602D;
    LocalDateTime targetTime = LocalDateTime.of(2020, 1, 1, 12, 11, 00);

    assertEquals(7.0276D,
      Calculators.interpolate(time0, y0, time1, y1, targetTime), 0.0001D);
  }
}
