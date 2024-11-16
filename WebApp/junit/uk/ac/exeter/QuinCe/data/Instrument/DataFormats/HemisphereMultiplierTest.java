package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

public class HemisphereMultiplierTest extends BaseTest {

  private double applyMultiplier(double input, String hemisphere)
    throws InvalidHemisphereException {

    return HemisphereMultiplier.apply(input, hemisphere);
  }

  @Test
  public void test_N() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "N"));
  }

  @Test
  public void test_n() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "n"));
  }

  @Test
  public void test_S() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "S"));
  }

  @Test
  public void test_s() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "s"));
  }

  @Test
  public void test_E() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "E"));
  }

  @Test
  public void test_e() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "e"));
  }

  @Test
  public void test_W() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "W"));
  }

  @Test
  public void test_w() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "w"));
  }

  @Test
  public void testInvalid() {
    assertThrows(InvalidHemisphereException.class, () -> {
      applyMultiplier(1D, "fdljg");
    });
  }
}
