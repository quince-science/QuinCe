package uk.ac.exeter.QuinCe.data.Dataset.QC;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;

@TestInstance(Lifecycle.PER_CLASS)
public class FlagEqualSignificanceTest extends FlagSignificanceTest {

  @ParameterizedTest
  @MethodSource("getLines")
  public void equalSignificanceTest(TestSetLine line) throws Exception {
    Flag thisFlag = getThisFlag(line);
    Flag otherFlag = getOtherFlag(line);
    assertEquals(getExpectedResult(line),
      thisFlag.equalSignificance(otherFlag));
  }

  @Override
  protected String getTestSetName() {
    return "FlagEqualSignificance";
  }
}
