package junit.uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.HemisphereMultiplier;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.InvalidHemisphereException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class HemisphereMultiplierTest extends BaseTest {

  @Test
  public void nullOneConstructorTest() {
    assertThrows(MissingParamException.class, () -> {
      new HemisphereMultiplier(null, Arrays.asList("S", "South"));
    });
  }

  @Test
  public void emptyOneConstructorTest() {
    assertThrows(MissingParamException.class, () -> {
      new HemisphereMultiplier(new ArrayList<String>(),
        Arrays.asList("S", "South"));
    });
  }

  @Test
  public void nullMinusOneConstructorTest() {
    assertThrows(MissingParamException.class, () -> {
      new HemisphereMultiplier(Arrays.asList("N", "North"), null);
    });
  }

  @Test
  public void emptyMinusOneConstructorTest() {
    assertThrows(MissingParamException.class, () -> {
      new HemisphereMultiplier(new ArrayList<String>(),
        new ArrayList<String>());
    });
  }

  private double applyMultiplier(double input, String hemisphere)
    throws InvalidHemisphereException {
    HemisphereMultiplier m = new HemisphereMultiplier(
      Arrays.asList("N", "North"), Arrays.asList("S", "South"));
    return m.apply(input, hemisphere);

  }

  @Test
  public void firstOneEntryCaseMatch() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "N"));
  }

  @Test
  public void secondOneEntryCaseMatch() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "North"));
  }

  @Test
  public void firstOneEntryCaseNotMatch() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "n"));
  }

  @Test
  public void secondOneEntryCaseNotMatch() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(1D, "nOrTH"));
  }

  @Test
  public void firstMinusOneEntryCaseMatch() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "S"));
  }

  @Test
  public void secondMinusOneEntryCaseMatch() throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "South"));
  }

  @Test
  public void firstMinusOneEntryCaseNotMatch()
    throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "s"));
  }

  @Test
  public void secondMinusOneEntryCaseNotMatch()
    throws InvalidHemisphereException {
    assertEquals(1D, applyMultiplier(-1D, "SouTh"));
  }

  @Test
  public void noMatchTest() {
    assertThrows(InvalidHemisphereException.class, () -> {
      applyMultiplier(1D, "West");
    });
  }
}
