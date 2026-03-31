package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.CoordinateException;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;

public class SVTestUtils extends BaseTest {

  protected static List<SensorValue> makeSensorValues(int[] minutes,
    Double[] values) throws CoordinateException {
    if (minutes.length != values.length) {
      throw new IllegalArgumentException(
        "Minutes and Values are different lengths");
    }

    List<SensorValue> result = new ArrayList<SensorValue>(minutes.length);

    for (int i = 0; i < minutes.length; i++) {
      result.add(new SensorValue(minutes[i], 1L, flagScheme, 1L,
        new TimeCoordinate(1L, LocalDateTime.of(2024, 1, 1, 0, minutes[i], 0)),
        String.valueOf(values[i]), new AutoQCResult(flagScheme),
        flagScheme.getGoodFlag(), null));
    }

    return result;
  }

  /**
   * Check the Auto QC flags for a set of {@link SensorValue}s.
   *
   * @param sensorValues
   *          The {@link SensorValue}s to check.
   * @param expectedFlag
   *          The expected QC {@link Flag}.
   * @param flaggedIds
   *          {@link SensorValue}s with the specified IDs should have the
   *          {@code expectedFlag}. All other values should have
   *          {@link Flag#GOOD}/{@link Flag#ASSUMED_GOOD} flags.
   * @return
   */
  protected static boolean checkAutoQC(List<SensorValue> sensorValues,
    Flag expectedFlag, List<Long> flaggedIds) {

    boolean result = true;

    for (SensorValue sv : sensorValues) {
      if (flaggedIds.contains(sv.getId())) {
        if (!(sv.getAutoQcFlag().equalSignificance(expectedFlag))) {
          result = false;
          // We could break early, but it's good for debugging to see the whole
          // set
        }
      } else if (!(flagScheme.isGood(sv.getAutoQcFlag(), true))) {
        result = false;
        // We could break early, but it's good for debugging to see the whole
        // set
      }
    }

    return result;
  }

  protected static SensorValue makeSensorValue(long id, long fileColumn,
    int minute, String value, Flag flag)
    throws InvalidFlagException, CoordinateException {

    TimeCoordinate coordinate = new TimeCoordinate(id,
      LocalDateTime.of(2024, 1, 1, 0, minute, 0));
    return new SensorValue(id, 1L, flagScheme, fileColumn, coordinate, value,
      new AutoQCResult(flagScheme), flag, "");
  }
}
