package uk.ac.exeter.QuinCe.data.Dataset.QC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

public class FlagTest extends BaseTest {
  @Test
  public void getSimpleFlagTest() throws InvalidFlagException {
    ExtendedFlag extendedFlag = new ExtendedFlag();
    Flag simpleFlag = extendedFlag.getSimpleFlag();
    assertEquals(Flag.class, simpleFlag.getClass());
  }

  private Flag makeSignificanceFlag(int significance) {
    return new Flag(1, "Significance", 'S', significance, false, false, 1);
  }

  @Test
  public void mostSignificantAllSameTest() {
    Flag tenFlag = makeSignificanceFlag(10);
    assertEquals(10, Flag.mostSignificant(tenFlag, tenFlag).getSignificance());
  }

  @Test
  public void mostSignificantEmptyTest() {
    assertNull(Flag.mostSignificant());
  }

  @Test
  public void mostSignificantVariousTest() {
    assertEquals(30, Flag.mostSignificant(makeSignificanceFlag(10),
      makeSignificanceFlag(30), makeSignificanceFlag(3)).getSignificance());
  }

  @Test
  public void mostSignificantNullEntryTest() {
    assertEquals(10, Flag
      .mostSignificant(makeSignificanceFlag(10), null, makeSignificanceFlag(3))
      .getSignificance());
  }

  @Test
  public void leastSignificantAllSameTest() {
    Flag tenFlag = makeSignificanceFlag(10);
    assertEquals(10, Flag.leastSignificant(tenFlag, tenFlag).getSignificance());
  }

  @Test
  public void leastSignificantEmptyTest() {
    assertNull(Flag.leastSignificant());
  }

  @Test
  public void leastSignificantVariousTest() {
    assertEquals(3, Flag.leastSignificant(makeSignificanceFlag(10),
      makeSignificanceFlag(30), makeSignificanceFlag(3)).getSignificance());
  }

  @Test
  public void leastSignificantNullEntryTest() {
    assertEquals(3, Flag
      .leastSignificant(makeSignificanceFlag(10), null, makeSignificanceFlag(3))
      .getSignificance());
  }

  /**
   * Extended {@link Flag} class for use in tests.
   */
  class ExtendedFlag extends Flag {

    private String string;

    protected ExtendedFlag() throws InvalidFlagException {
      super(1, "Extended", 'E', 10, false, false, 1);
      this.string = "Extended";
    }

    protected String getString() {
      return string;
    }
  }
}
