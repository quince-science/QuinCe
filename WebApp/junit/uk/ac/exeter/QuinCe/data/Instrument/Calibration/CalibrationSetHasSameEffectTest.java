package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;

/**
 * Tests for the {@link CalibrationSet#hasSameEffect(CalibrationSet)} method.
 *
 * <p>
 * This method is tested by producing the same {@link CalibrationSet} twice,
 * with one of the {@link Calibration} dates changed. Depending on the
 * confiugration of the {@link CalibrationSet} the result of
 * {@link CalibrationSet#hasSameEffect(CalibrationSet)} will change.
 * </p>
 *
 * <p>
 * The base {@link CalibrationSet} has one target, with {@link Calibration}s on
 * the 5th, 10th and 15th of the month. The {@link CalibrationSet}'s range is
 * from the 7th to the 12, so there is a prior, intermediate and post
 * calibration. Each of the {@link Calibration}s is moved by one day in turn,
 * with different combinations of the {@code timeAffectsCalibration} and
 * {@code usePostCalibration} flags.
 * </p>
 *
 * @see CalibrationSetTest
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CalibrationSetHasSameEffectTest extends TestSetTest {

  private static final int TIME_AFFECTS_CALIBRATION_COL = 0;

  private static final int USE_POST_CALIBRATION_COL = 1;

  private static final int MOVE_COL = 2;

  private static final int RESULT_COL = 3;

  private static final String START = "START";

  private static final String MIDDLE = "MIDDLE";

  private static final String END = "END";

  private boolean timeAffectsCalibration(TestSetLine line) {
    return line.getBooleanField(TIME_AFFECTS_CALIBRATION_COL);
  }

  private boolean usePostCalibration(TestSetLine line) {
    return line.getBooleanField(USE_POST_CALIBRATION_COL);
  }

  private TreeMap<String, String> getTargets() {
    TreeMap<String, String> targets = new TreeMap<String, String>();
    targets.put("TARGET1", "TARGET1");
    return targets;
  }

  private LocalDateTime makeTime(String position, boolean move) {
    int day;

    switch (position) {
    case START: {
      day = 5;
      break;
    }
    case MIDDLE: {
      day = 10;
      break;
    }
    case END: {
      day = 15;
      break;
    }
    default: {
      throw new IllegalArgumentException("Unrecognised position " + position);
    }
    }

    if (move) {
      day += 1;
    }

    return CalibrationSetTest.makeTime(day);
  }

  private TreeMap<String, TreeSet<Calibration>> makeCalibrations(
    boolean timeAffectsCalibration, String move) throws Exception {

    TreeMap<String, TreeSet<Calibration>> calibrations = new TreeMap<String, TreeSet<Calibration>>();

    TreeSet<Calibration> calibs = new TreeSet<Calibration>();

    calibs.add(new TestCalibration(1L, CalibrationSetTest.instrument, "TARGET1",
      makeTime(START, move.equals(START)),
      CalibrationSetTest.makeCoefficient("10"), timeAffectsCalibration));

    calibs.add(new TestCalibration(2L, CalibrationSetTest.instrument, "TARGET1",
      makeTime(MIDDLE, move.equals(MIDDLE)),
      CalibrationSetTest.makeCoefficient("20"), timeAffectsCalibration));

    calibs.add(new TestCalibration(3L, CalibrationSetTest.instrument, "TARGET1",
      makeTime(END, move.equals(END)), CalibrationSetTest.makeCoefficient("30"),
      timeAffectsCalibration));

    calibrations.put("TARGET1", calibs);
    return calibrations;
  }

  @ParameterizedTest
  @MethodSource("getLines")
  public void hasSameEffectTest(TestSetLine line) throws Exception {

    CalibrationSet originalCalibrationSet = new CalibrationSet(getTargets(),
      CalibrationSetTest.makeTime(7), CalibrationSetTest.makeTime(12),
      CalibrationSetTest.makeDbInstance(timeAffectsCalibration(line), true,
        usePostCalibration(line)),
      makeCalibrations(timeAffectsCalibration(line), ""));

    CalibrationSet changedCalibrationSet = new CalibrationSet(getTargets(),
      CalibrationSetTest.makeTime(7), CalibrationSetTest.makeTime(12),
      CalibrationSetTest.makeDbInstance(timeAffectsCalibration(line), true,
        usePostCalibration(line)),
      makeCalibrations(timeAffectsCalibration(line),
        line.getStringField(MOVE_COL, false)));

    assertEquals(line.getBooleanField(RESULT_COL),
      changedCalibrationSet.hasSameEffect(originalCalibrationSet));
  }

  @Override
  protected String getTestSetName() {
    return "CalibrationSetHasSameEffectTest";
  }

}
