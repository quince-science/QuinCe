package uk.ac.exeter.QuinCe.data.Dataset.QC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

/**
 * Basic tests for the {@link Flag} class.
 */
public class FlagTest extends BaseTest {

  private static Stream<Arguments> charConstructorTestParams() {
    return Stream.of(Arguments.of('C', Flag.NOT_CALIBRATED),
      Arguments.of('1', Flag.NOT_CALIBRATED), Arguments.of('G', Flag.GOOD),
      Arguments.of('2', Flag.GOOD), Arguments.of('A', Flag.ASSUMED_GOOD),
      Arguments.of('Q', Flag.QUESTIONABLE),
      Arguments.of('3', Flag.QUESTIONABLE), Arguments.of('B', Flag.BAD),
      Arguments.of('4', Flag.BAD), Arguments.of('N', Flag.NEEDED),
      Arguments.of('F', Flag.FLUSHING), Arguments.of('X', Flag.NO_QC),
      Arguments.of('L', Flag.LOOKUP));
  }

  /**
   * Test the character constructor used by other unit tests.
   *
   * @throws InvalidFlagException
   */
  @ParameterizedTest
  @MethodSource("charConstructorTestParams")
  public void charConstructorTest(char character, Flag expectedFlag)
    throws InvalidFlagException {
    Flag flag = new Flag(character);
    assertEquals(expectedFlag, flag);
  }

  /**
   * Test the character constructor with an invalid character.
   */
  @Test
  public void invalidCharConstructorTest() {
    assertThrows(InvalidFlagException.class, () -> {
      new Flag('Z');
    });
  }

  /**
   * Parameters for {@link #flagValueTest(Flag, int)}.
   *
   * @return The test parameters.
   *
   * @see #flagValueTest(Flag, int)
   */
  private static Stream<Arguments> flagValueTestParams() {
    return Stream.of(Arguments.of(Flag.NO_QC, Flag.VALUE_NO_QC),
      Arguments.of(Flag.NOT_CALIBRATED, Flag.VALUE_NOT_CALIBRATED),
      Arguments.of(Flag.ASSUMED_GOOD, Flag.VALUE_ASSUMED_GOOD),
      Arguments.of(Flag.GOOD, Flag.VALUE_GOOD),
      Arguments.of(Flag.QUESTIONABLE, Flag.VALUE_QUESTIONABLE),
      Arguments.of(Flag.BAD, Flag.VALUE_BAD),
      Arguments.of(Flag.NEEDED, Flag.VALUE_NEEDED),
      Arguments.of(Flag.FLUSHING, Flag.VALUE_FLUSHING),
      Arguments.of(Flag.LOOKUP, Flag.VALUE_LOOKUP));
  }

  /**
   * Ensure that the defined flags have the correct numeric values.
   *
   * @param flag
   *          The flag.
   * @param value
   *          The expected value.
   *
   * @see #flagValueParams()
   */
  @ParameterizedTest
  @MethodSource("flagValueTestParams")
  public void flagValueTest(Flag flag, int value) {
    assertEquals(flag.getFlagValue(), value);
  }

  /**
   * Test the value constructor with an invalid value.
   */
  @Test
  public void invalidValueConstructorTest() {
    assertThrows(InvalidFlagException.class, () -> {
      new Flag(7);
    });
  }

  private static Stream<Arguments> woceValueFromFlagTestParams() {
    return Stream.of(Arguments.of(Flag.NO_QC, -1),
      Arguments.of(Flag.NOT_CALIBRATED, 1), Arguments.of(Flag.ASSUMED_GOOD, 2),
      Arguments.of(Flag.GOOD, 2), Arguments.of(Flag.QUESTIONABLE, 3),
      Arguments.of(Flag.BAD, 4), Arguments.of(Flag.NEEDED, -1),
      Arguments.of(Flag.FLUSHING, -1), Arguments.of(Flag.LOOKUP, -1));
  }

  /**
   * Test that all Flags give the correct WOCE value.
   *
   * @param flag
   *          The Flag.
   * @param expectedWoceValue
   *          The expected WOCE value.
   */
  @ParameterizedTest
  @MethodSource("woceValueFromFlagTestParams")
  public void woceValueFromFlagTest(Flag flag, int expectedWoceValue) {
    assertEquals(expectedWoceValue, flag.getWoceValue());
  }

  private static Stream<Arguments> woceValueFromFlagValueTestParams() {
    return Stream.of(Arguments.of(Flag.VALUE_NO_QC, -1),
      Arguments.of(Flag.VALUE_NOT_CALIBRATED, 1),
      Arguments.of(Flag.VALUE_ASSUMED_GOOD, 2),
      Arguments.of(Flag.VALUE_GOOD, 2),
      Arguments.of(Flag.VALUE_QUESTIONABLE, 3), Arguments.of(Flag.VALUE_BAD, 4),
      Arguments.of(Flag.VALUE_NEEDED, -1),
      Arguments.of(Flag.VALUE_FLUSHING, -1),
      Arguments.of(Flag.VALUE_LOOKUP, -1));
  }

  /**
   * Test that all Flags give the correct WOCE value.
   *
   * @param flagValue
   *          The Flag value.
   * @param expectedWoceValue
   *          The expected WOCE value.
   */
  @ParameterizedTest
  @MethodSource("woceValueFromFlagValueTestParams")
  public void woceValueFromFlagValueTest(int flagValue, int expectedWoceValue) {
    assertEquals(expectedWoceValue, Flag.getWoceValue(flagValue));
  }

  /**
   * Test the WOCE value from an invalid Flag value.
   */
  @Test
  public void woceValueFromInvalidValueTest() {
    assertEquals(-1, Flag.getWoceValue(7));
  }

  private static Stream<Arguments> isGoodTestParams() {
    return Stream.of(Arguments.of(Flag.NO_QC, false),
      Arguments.of(Flag.NOT_CALIBRATED, true),
      Arguments.of(Flag.ASSUMED_GOOD, true), Arguments.of(Flag.GOOD, true),
      Arguments.of(Flag.QUESTIONABLE, false), Arguments.of(Flag.BAD, false),
      Arguments.of(Flag.NEEDED, false), Arguments.of(Flag.FLUSHING, false),
      Arguments.of(Flag.LOOKUP, false));
  }

  /**
   * Test the {@link Flag#isGood()} method for all Flag types.
   *
   * @param flag
   *          The Flag.
   * @param expectedIsGood
   *          The expected result from {@link Flag#isGood()}.
   */
  @ParameterizedTest
  @MethodSource("isGoodTestParams")
  public void isGoodTest(Flag flag, boolean expectedIsGood) {
    assertEquals(expectedIsGood, flag.isGood());
  }

  @Test
  public void flagFromExtendedFlagTest() throws InvalidFlagException {
    ExtendedFlag extendedFlag = new ExtendedFlag(Flag.VALUE_BAD, "test");
    Flag simpleFlag = extendedFlag.getSimpleFlag();
    assertEquals(Flag.class, simpleFlag.getClass());
  }

  @Test
  public void mostSignificantAllSameTest() {
    assertEquals(Flag.BAD, Flag.getMostSignificantFlag(Flag.BAD, Flag.BAD));
  }

  @Test
  public void mostSignificantEmptyTest() {
    assertNull(Flag.getMostSignificantFlag());
  }

  @Test
  public void mostSignificantVariousTest() {
    assertEquals(Flag.BAD,
      Flag.getMostSignificantFlag(Flag.QUESTIONABLE, Flag.BAD, Flag.GOOD));
  }

  @Test
  public void mostSignificantNullEntryTest() {
    assertEquals(Flag.QUESTIONABLE,
      Flag.getMostSignificantFlag(Flag.QUESTIONABLE, null, Flag.GOOD));
  }
}

/**
 * Extended {@link Flag} class for use in tests.
 */
class ExtendedFlag extends Flag {

  private String string;

  protected ExtendedFlag(int value, String string) throws InvalidFlagException {
    super(value);
    this.string = string;
  }

  protected String getString() {
    return string;
  }
}
