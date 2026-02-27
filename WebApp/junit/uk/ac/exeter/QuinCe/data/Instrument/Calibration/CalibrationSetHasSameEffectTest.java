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
 * configuration of the {@link CalibrationSet} the result of
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

  /**
   * The column number in the Test Set file indicating whether or not the timing
   * of a {@link Calibration} alters its effect.
   */
  private static final int TIME_AFFECTS_CALIBRATION_COL = 0;

  /**
   * The column number in the Test Set file indicating whether or not the
   * post-calibration should be used in determining the effects of calibrations.
   */
  private static final int USE_POST_CALIBRATION_COL = 1;

  /**
   * The column number in the Test Set file indicating how the timestamp of the
   * second {@link CalibrationSet} should be moved in relation to the first.
   *
   * <p>
   * The {@link CalibrationSet}s created by this test contain three
   * {@link Calibration}s, one each at the start, middle and end of the set's
   * time period. The value of this column which of the {@link Calibration}s
   * should be offset from its default position in order to induce a difference
   * between the two {@link CalibrationSet}s that are compared.
   * </p>
   *
   * @see #START
   * @see #MIDDLE
   * @see #END
   */
  private static final int MOVE_COL = 2;

  /**
   * The column number in the Test Set file indicating whether or not the two
   * created {@link CalibrationSet}s should have the same effect.
   */
  private static final int RESULT_COL = 3;

  /**
   * Value indicating that the second {@code START} {@link Calibration}'s time
   * should be offset.
   *
   * @see #MOVE_COL
   */
  private static final String START = "START";

  /**
   * Value indicating that the second {@code MIDDLE} {@link Calibration}'s time
   * should be offset.
   *
   * @see #MOVE_COL
   */
  private static final String MIDDLE = "MIDDLE";

  /**
   * Value indicating that the second {@code END} {@link Calibration}'s time
   * should be offset.
   *
   * @see #MOVE_COL
   */
  private static final String END = "END";

  /**
   * Extract the flag indicating whether or not the timing of
   * {@link Calibration}s in the current test will alter their effects.
   *
   * @param line
   *          The test line.
   * @return {@code true} if timing should alter the calibration effects;
   *         {@code false} if it should not.
   */
  private boolean timeAffectsCalibration(TestSetLine line) {
    return line.getBooleanField(TIME_AFFECTS_CALIBRATION_COL);
  }

  /**
   * Extract the flag indicating whether or not post-calibrations should be
   * taken into account when assessing a {@link CalibrationSet}'s effects in the
   * current test.
   *
   * @param line
   *          The test line.
   * @return {@code true} if post-calibrations should be used; {@code false} if
   *         not.
   */
  private boolean usePostCalibration(TestSetLine line) {
    return line.getBooleanField(USE_POST_CALIBRATION_COL);
  }

  /**
   * Get the set of targets for the test {@link CalibrationSet}.
   *
   * <p>
   * For these tests only one target is required.
   * </p>
   *
   * @return The calibration targets.
   */
  private TreeMap<String, String> getTargets() {
    TreeMap<String, String> targets = new TreeMap<String, String>();
    targets.put("TARGET1", "TARGET1");
    return targets;
  }

  /**
   * Generate a time at the specified position in the testing period, offsetting
   * it if required.
   *
   * @param position
   *          The time position.
   * @param move
   *          Whether or not an offset should be applied.
   * @return The created time.
   *
   * @see #START
   * @see #MIDDLE
   * @see #END
   */
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

  /**
   * Generate a set of {@link Calibration}s for the beginning, middle and end of
   * the test time period.
   *
   * @param timeAffectsCalibration
   *          Indicates whether or not the timing of the {@link Calibration}
   *          alters its affect.
   * @param move
   *          Indicates which of the {@link Calibration}s should be moved in
   *          time to create a difference between the {@link CalibrationSet}s
   *          for the difference to be compared.
   * @return The created calibrations.
   * @throws Exception
   *           If the calibrations could not be created.
   */
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

  /**
   * Run a test from the test set.
   *
   * @param line
   *          The test set line.
   * @throws Exception
   *           If an error occurs during the test.
   */
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
