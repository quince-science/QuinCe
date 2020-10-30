package junit.uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.Calculators;

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
   * Test {@link Calculators#calcPH2O(Double, Double)} with NaN salinity.
   */
  @Test
  public void calcPH2ONanSalinityTest() {
    assertEquals(Double.NaN, Calculators.calcPH2O(Double.NaN, 11.17));
  }

  /**
   * Test {@link Calculators#calcPH2O(Double, Double)} with NaN salinity.
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
   * Test {@link Calculators#calcpCO2TEWet(Double, Double, Double)} with a NaN
   * xCO2 value.
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
   * Test {@link Calculators#calcpCO2TEWet(Double, Double, Double)} with a NaN
   * pressure value.
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
   * Test {@link Calculators#calcpCO2TEWet(Double, Double, Double)} with a NaN
   * pH2O value.
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
   * NaN pCO2 value.
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
   * NaN xCO2 value.
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
   * NaN pressure value.
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
   * NaN temperature value.
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

}
