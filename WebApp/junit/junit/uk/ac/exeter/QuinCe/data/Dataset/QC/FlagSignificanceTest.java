package junit.uk.ac.exeter.QuinCe.data.Dataset.QC;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;

public abstract class FlagSignificanceTest extends TestSetTest {

  protected static final int THIS_COL = 0;

  protected static final int OTHER_COL = 1;

  protected static final int RESULT_COL = 2;

  protected Flag getThisFlag(TestSetLine line) throws InvalidFlagException {
    return new Flag(line.getCharField(THIS_COL));
  }

  protected Flag getOtherFlag(TestSetLine line) throws InvalidFlagException {
    return new Flag(line.getCharField(OTHER_COL));
  }

  protected boolean getExpectedResult(TestSetLine line) {
    return line.getBooleanField(RESULT_COL);
  }
}
