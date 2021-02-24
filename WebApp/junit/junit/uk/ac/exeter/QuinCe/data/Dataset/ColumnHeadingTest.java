package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;

public class ColumnHeadingTest extends BaseTest {

  @Test
  public void basicConstructorTest() {
    ColumnHeading columnHeading = new ColumnHeading(1L, "Short", "Long", "Code",
      "units", true, false);

    assertEquals(1L, columnHeading.getId());
    assertEquals("Short", columnHeading.getShortName());
    assertEquals("Long", columnHeading.getLongName());
    assertEquals("Code", columnHeading.getCodeName());
    assertEquals("units", columnHeading.getUnits());
    assertEquals(true, columnHeading.hasQC());
    assertEquals(false, columnHeading.includeType());

    assertEquals("Short [units]", columnHeading.getShortName(true));
    assertEquals("Long [units]", columnHeading.getLongName(true));
    assertEquals("Code [units]", columnHeading.getCodeName(true));
    assertEquals("Short", columnHeading.getShortName(false));
    assertEquals("Long", columnHeading.getLongName(false));
    assertEquals("Code", columnHeading.getCodeName(false));
  }

  @ParameterizedTest
  @MethodSource("booleans")
  public void booleanConstructorTest(boolean bool) {
    ColumnHeading heading = new ColumnHeading(1L, "Short", "Long", "Code",
      "units", bool, bool);

    assertEquals(bool, heading.hasQC());
    assertEquals(bool, heading.includeType());
  }

  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void blankUnitsTest(String units) {
    ColumnHeading heading = new ColumnHeading(1L, "Short", "Long", "Code",
      units, false, false);

    assertEquals("Short", heading.getShortName(true));
    assertEquals("Long", heading.getLongName(true));
    assertEquals("Code", heading.getCodeName(true));
  }

  @Test
  public void copyConstructorTest() {
    ColumnHeading source = new ColumnHeading(1L, "Short", "Long", "Code",
      "units", false, false);

    ColumnHeading destination = new ColumnHeading(source);

    assertEquals(1L, destination.getId());
    assertEquals("Short", destination.getShortName());
    assertEquals("Long", destination.getLongName());
    assertEquals("Code", destination.getCodeName());
    assertEquals("units", destination.getUnits());
    assertEquals(false, destination.hasQC());
    assertEquals(false, destination.includeType());

    assertEquals("Short [units]", destination.getShortName(true));
    assertEquals("Long [units]", destination.getLongName(true));
    assertEquals("Code [units]", destination.getCodeName(true));
    assertEquals("Short", destination.getShortName(false));
    assertEquals("Long", destination.getLongName(false));
    assertEquals("Code", destination.getCodeName(false));
  }
}
