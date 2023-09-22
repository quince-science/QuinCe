package junit.uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;

@TestInstance(Lifecycle.PER_CLASS)
public class DateTimeSpecificationRequiredTypeTests extends TestSetTest {

  private static final int ASSIGNED_COL = 0;

  private static final int REQUIRED_COL = 1;

  /**
   * Test that an empty specification for a file without a header returns the
   * correct required types.
   */
  @Test
  public void getRequiredTypesNoAssignmentsNoHeaderTest() {
    DateTimeSpecification spec = new DateTimeSpecification(false);

    List<Integer> expected = new ArrayList<Integer>();
    expected.add(DateTimeSpecification.DATE_TIME);
    expected.add(DateTimeSpecification.DATE);
    expected.add(DateTimeSpecification.YEAR);
    expected.add(DateTimeSpecification.JDAY_TIME);
    expected.add(DateTimeSpecification.JDAY);
    expected.add(DateTimeSpecification.MONTH);
    expected.add(DateTimeSpecification.DAY);
    expected.add(DateTimeSpecification.TIME);
    expected.add(DateTimeSpecification.HOUR);
    expected.add(DateTimeSpecification.MINUTE);
    expected.add(DateTimeSpecification.SECOND);
    expected.add(DateTimeSpecification.UNIX);

    assertTrue(listsEqual(expected, spec.getRequiredTypes()));
  }

  /**
   * Test that an empty specification for a file with a header returns the
   * correct required types.
   */
  @Test
  public void getRequiredTypesNoAssignmentsWithHeaderTest() {
    DateTimeSpecification spec = new DateTimeSpecification(true);

    List<Integer> expected = new ArrayList<Integer>();
    expected.add(DateTimeSpecification.DATE_TIME);
    expected.add(DateTimeSpecification.HOURS_FROM_START);
    expected.add(DateTimeSpecification.SECONDS_FROM_START);
    expected.add(DateTimeSpecification.DATE);
    expected.add(DateTimeSpecification.YEAR);
    expected.add(DateTimeSpecification.JDAY_TIME);
    expected.add(DateTimeSpecification.JDAY);
    expected.add(DateTimeSpecification.MONTH);
    expected.add(DateTimeSpecification.DAY);
    expected.add(DateTimeSpecification.TIME);
    expected.add(DateTimeSpecification.HOUR);
    expected.add(DateTimeSpecification.MINUTE);
    expected.add(DateTimeSpecification.SECOND);
    expected.add(DateTimeSpecification.UNIX);

    assertTrue(listsEqual(expected, spec.getRequiredTypes()));
  }

  @Test
  public void getRequiredTypesHoursFromStartAssignedTest() throws Exception {
    DateTimeSpecification spec = new DateTimeSpecification(true);
    spec.assignHoursFromStart(0, "", "", "");
    assertEquals(0, spec.getRequiredTypes().size());
  }

  @ParameterizedTest
  @MethodSource("getLines")
  public void getRequiredTypesTests(TestSetLine line) throws Exception {

    List<Integer> assigned = pipesToList(
      line.getStringField(ASSIGNED_COL, false));

    List<Integer> required = pipesToList(
      line.getStringField(REQUIRED_COL, false));

    DateTimeSpecification spec = new DateTimeSpecification(true);

    for (int i = 0; i < assigned.size(); i++) {
      spec.assign(assigned.get(i), i, "");
    }

    assertTrue(listsEqual(required, spec.getRequiredTypes()));
  }

  private List<Integer> pipesToList(String pipes) {
    String[] split = pipes.split("\\|");
    List<Integer> list = new ArrayList<Integer>(split.length);
    for (int i = 0; i < split.length; i++) {
      if (split[i].length() > 0) {
        list.add(Integer.parseInt(split[i]));
      }
    }

    return list;
  }

  @Override
  protected String getTestSetName() {
    return "DateTimeSpecificationRequiredAssignments";
  }
}
