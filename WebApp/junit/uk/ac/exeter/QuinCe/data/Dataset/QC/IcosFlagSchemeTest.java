package uk.ac.exeter.QuinCe.data.Dataset.QC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;

public class IcosFlagSchemeTest extends BaseTest {

  private static IcosFlagScheme flagScheme = IcosFlagScheme.getInstance();

  private static Stream<Arguments> charConstructorTestParams() {
    return Stream.of(Arguments.of('C', IcosFlagScheme.NO_CALIBRATION_FLAG),
      Arguments.of('G', flagScheme.getGoodFlag()),
      Arguments.of('g', flagScheme.getAssumedGoodFlag()),
      Arguments.of('Q', IcosFlagScheme.QUESTIONABLE_FLAG),
      Arguments.of('B', flagScheme.getBadFlag()),
      Arguments.of('N', FlagScheme.NEEDED_FLAG),
      Arguments.of('F', FlagScheme.FLUSHING_FLAG),
      Arguments.of('X', FlagScheme.NO_QC_FLAG),
      Arguments.of('L', FlagScheme.LOOKUP_FLAG));
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
    Flag flag = flagScheme.getFlag(character);
    assertEquals(expectedFlag, flag);
  }

  @ParameterizedTest
  @CsvSource("3, Z")
  public void invalidCharConstructorTest(char character) {
    assertThrows(FlagException.class, () -> {
      flagScheme.getFlag(character);
    });
  }

  /**
   * Test the value constructor with an invalid value.
   */
  @Test
  public void invalidValueConstructorTest() {
    assertThrows(FlagException.class, () -> {
      flagScheme.getFlag(7);
    });
  }

}
