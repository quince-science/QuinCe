package uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValuesTest.QCCascade;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListTest;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;

/**
 * Test cascade of applying a diagnostic flag to a reliant {@link SensorValue}
 * by interpolation.
 *
 * <p>
 * Diagnostic flags should find affected {@link SensorValue}s by interpolation,
 * i.e. the flags should affect all nearby values. However, if there is a Good
 * diagnostic flag between the Bad flag and the affected {@link SensorValue},
 * then its effect will be neutralised because the period affected by the Bad
 * flag no longer encompasses the {@link SensorValue}.
 * </p>
 *
 * <p>
 * The test runs as follows:
 * </p>
 * <ul>
 * <li>There is a {@link SensorValue} at minute {@code 15} with a Good
 * flag.</li>
 * <li>There are Diagnostic {@link SensorValue}s at minutes
 * {@code 13, 14, 15}.</li>
 * <li>We adjust the flags on the Diagnostic values and test that the flag on
 * the {@link SensorValue} is set correctly.</li>
 * </ul>
 */
public class DiagnosticFlagInterpolationTest extends SensorValuesListTest {

  private SensorValue makeSensorValue(long id, long columnId, int minute)
    throws IllegalAccessException {
    SensorValue result = new SensorValue(1L, IcosFlagScheme.getInstance(),
      columnId,
      new TimeCoordinate(1L, LocalDateTime.of(2026, 04, 13, minute, 13, 0)),
      "1");
    result.setId(id);
    return result;
  }

  /**
   * @return
   */
  private static Stream<Arguments> manualDiagnosticFlagInterpolationParams() {

    Flag bad = IcosFlagScheme.getInstance().getBadFlag();
    Flag good = IcosFlagScheme.getInstance().getGoodFlag();
    Flag assumedGood = IcosFlagScheme.getInstance().getAssumedGoodFlag();

    return Stream.of(
      Arguments.of(bad, bad, bad, FlagScheme.LOOKUP_FLAG, "3,4,5"),
      Arguments.of(good, good, bad, FlagScheme.LOOKUP_FLAG, "5"),
      Arguments.of(good, bad, bad, FlagScheme.LOOKUP_FLAG, "4,5"),
      Arguments.of(bad, good, bad, FlagScheme.LOOKUP_FLAG, "5"),
      Arguments.of(bad, good, good, assumedGood, ""));

  }

  @FlywayTest(locationsForMigrate = {
    "resources/sql/testbase/DataReduction/base",
    "resources/sql/testbase/DataReduction/singleMeasurement" })
  @ParameterizedTest
  @MethodSource("manualDiagnosticFlagInterpolationParams")
  public void manualDiagnosticFlagInterpolationTest(Flag thirteenFlag,
    Flag fourteenFlag, Flag fifteenFlag, Flag expectedAffectedValueFlag,
    String lookup) throws Exception {

    initResourceManager();

    try (Connection conn = getConnection()) {

      DatasetSensorValues allSensorValues = getDatasetSensorValues();

      // Dummy SensorValues to fill out the data structures
      allSensorValues.add(makeSensorValue(1L, 4L, 15));

      // Affected SensorValue
      SensorValue affectedSensorValue = makeSensorValue(2L, 2L, 15);
      allSensorValues.add(affectedSensorValue);

      // Diagnostic Values
      SensorValue thirteen = makeSensorValue(3L, 5L, 13);
      thirteen.setUserQC(thirteenFlag, "comment");
      allSensorValues.add(thirteen);

      SensorValue fourteen = makeSensorValue(4L, 5L, 14);
      fourteen.setUserQC(fourteenFlag, "comment");
      allSensorValues.add(fourteen);

      SensorValue fifteen = makeSensorValue(5L, 5L, 15);
      fifteen.setUserQC(fifteenFlag, "comment");
      allSensorValues.add(fifteen);

      // Apply the QC cascade
      allSensorValues.applyQCCascade(thirteen, new RunTypePeriods());
      allSensorValues.applyQCCascade(fourteen, new RunTypePeriods());
      allSensorValues.applyQCCascade(fifteen, new RunTypePeriods());

      assertEquals(expectedAffectedValueFlag,
        affectedSensorValue.getUserQCFlag());
      assertEquals(lookup, affectedSensorValue.getUserQCMessage());
    }
  }
}
