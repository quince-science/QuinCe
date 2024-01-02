package junit.uk.ac.exeter.QuinCe.data.Dataset.QC;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

@TestInstance(Lifecycle.PER_CLASS)
public class FlagLessSignificantTest extends FlagSignificanceTest {

  @ParameterizedTest
  @MethodSource("getLines")
  public void moreSignificantTest(TestSetLine line) throws Exception {
    Flag thisFlag = getThisFlag(line);
    Flag otherFlag = getOtherFlag(line);
    assertEquals(getExpectedResult(line),
      thisFlag.lessSignificantThan(otherFlag));
  }

  @Override
  protected String getTestSetName() {
    return "FlagLessSignificant";
  }
}
