package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;

@TestInstance(Lifecycle.PER_CLASS)
public class LongitudeSpecificationTests extends TestSetTest {

  private static final int FORMAT_COL = 0;

  private static final int VALUE_COL = 1;

  private static final int HEMISPHERE_COL = 2;

  private static final int VALID_COL = 3;

  private static final int PARSED_COL = 4;

  @ParameterizedTest
  @MethodSource("getLines")
  public void longitudeTest(TestSetLine line) throws Exception {

    String formatCode = line.getStringField(FORMAT_COL, true);
    int format = LongitudeSpecification.class.getField(formatCode).getInt(null);

    List<String> dataLine = new ArrayList<String>();
    dataLine.add(line.getStringField(VALUE_COL, true));
    dataLine.add(line.getStringField(HEMISPHERE_COL, true));
    boolean hasHemisphere = null != line.getStringField(HEMISPHERE_COL, true);

    boolean shouldBeValid = line.getBooleanField(VALID_COL);
    String expectedResult = line.getStringField(PARSED_COL, false);

    String parsedValue = null;
    boolean exceptionRaised = false;

    try {
      int hemisphereCol = hasHemisphere ? 1 : -1;
      LongitudeSpecification spec = new LongitudeSpecification(format, 0,
        hemisphereCol);

      parsedValue = spec.getValue(dataLine);
    } catch (PositionException e) {
      exceptionRaised = true;
    }

    if (!shouldBeValid) {
      assertTrue(exceptionRaised, "Exception should have been raised");
    } else {
      assertFalse(exceptionRaised, "Exception incorrectly raised");
      assertEquals(expectedResult, parsedValue);
    }
  }

  @Override
  protected String getTestSetName() {
    return "LongitudeTests";
  }

}
