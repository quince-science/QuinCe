package uk.ac.exeter.QuinCe.data.Dataset.QC;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

/**
 * Tests for {@link FlagScheme} and {@link AbstractFlagScheme}.
 */
public class FlagSchemeTest extends BaseTest {

  /**
   * Test registration of Good flag.
   */
  @Test
  public void goodFlagSetupTest() {
    TestScheme flagScheme = new TestScheme();
    assertEquals(TestScheme.GOOD_FLAG, flagScheme.getGoodFlag());
  }

  /**
   * Test that Assumed Good flag is auto-registered correctly.
   */
  @Test
  public void assumedGoodFlagTest() {
    TestScheme flagScheme = new TestScheme();
    Flag assumedGoodFlag = flagScheme.getAssumedGoodFlag();
    assertEquals(-2, assumedGoodFlag.getValue());
    assertEquals('g', assumedGoodFlag.getCharacter());
    assertEquals(2, assumedGoodFlag.getExportValue());
  }

  @ParameterizedTest
  @CsvSource({ "false,true", "true,true" })
  public void isGoodGoodFlagTest(boolean includeAssumedGood,
    boolean isGoodResult) {
    TestScheme flagScheme = new TestScheme();
    assertEquals(isGoodResult,
      flagScheme.isGood(TestScheme.GOOD_FLAG, includeAssumedGood));
  }

  @ParameterizedTest
  @CsvSource({ "false,false", "true,true" })
  public void isGoodAssumedGoodFlagTest(boolean includeAssumedGood,
    boolean isGoodResult) {
    TestScheme flagScheme = new TestScheme();
    Flag assumedGoodFlag = flagScheme.getAssumedGoodFlag();
    assertEquals(isGoodResult,
      flagScheme.isGood(assumedGoodFlag, includeAssumedGood));
  }

}

class TestScheme extends AbstractFlagScheme {
  protected final static Flag GOOD_FLAG = new Flag(2, "Good", 'G', 2, true,
    false, 2);

  protected TestScheme() {
    registerGoodFlag(GOOD_FLAG);
  }

  @Override
  public int getBasis() {
    return -1;
  }

  @Override
  public String getName() {
    return "Good Flag Test";
  }

  @Override
  public Flag getBadFlag() {
    return Mockito.mock(Flag.class);
  }
}