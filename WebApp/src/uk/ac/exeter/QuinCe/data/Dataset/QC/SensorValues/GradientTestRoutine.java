package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Range;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class GradientTestRoutine extends AutoQCRoutine {

  public GradientTestRoutine(FlagScheme flagScheme) {
    super(flagScheme);
  }

  public GradientTestRoutine(FlagScheme flagScheme, SensorType sensorType,
    Map<Flag, Range<Double>> limits) {
    super(flagScheme, sensorType, limits);
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {

    double maxDeltaPerMinute = limits.get(flagScheme.getBadFlag()).getMaximum();

    SensorValue prevValue = null;
    SensorValue currValue = null;
    SensorValue nextValue = null;

    List<SensorValue> filteredValues = filterMissingValues(values);

    // If the smallest change in values is larger than the specified delta,
    // increase the delta accordingly. Otherwise we'll get false positives
    // where the sensor cannot meet the standards of the delta.
    double minimumChange = SensorValue.getMinimumChange(values);

    int i = 1;
    while (i < filteredValues.size()) {
      currValue = filteredValues.get(i);

      prevValue = filteredValues.get(i - 1);
      nextValue = (i + 1) < filteredValues.size() ? filteredValues.get(i + 1)
        : null;

      // If the change is equal to the smallest possible change
      // we can't do the gradient check
      double valueDelta = currValue.getDoubleValue()
        - prevValue.getDoubleValue();
      if (Math.abs(valueDelta) > minimumChange) {

        // time-increment
        double tDiff = ChronoUnit.NANOS.between(
          prevValue.getCoordinate().getTime(),
          currValue.getCoordinate().getTime()) / (60.0 * 1000000000);

        double deltaPerMin = Math.abs(valueDelta / tDiff);

        if (deltaPerMin > maxDeltaPerMinute) { // spike or gradient

          boolean spike = false;

          if (null != nextValue) {
            double deltaPerMinPrevToNext = Math.abs(
              nextValue.getDoubleValue() - prevValue.getDoubleValue()) / tDiff;
            if (deltaPerMinPrevToNext < maxDeltaPerMinute) {
              spike = true;
            }
          }

          if (spike) {
            // This is a single point spike, so flag the current value (which is
            // the spike)
            addFlag(currValue, flagScheme.getBadFlag(), maxDeltaPerMinute,
              deltaPerMin);

            // We know that this is a single spike, so we can skip checking the
            // next value.
            i++;
          } else {
            // This is a gradient, so mark the previous and current points as
            // the starting point of the gradient. We can't be certain which
            // "side" of the change is wrong.
            addFlag(prevValue, flagScheme.getBadFlag(), maxDeltaPerMinute,
              deltaPerMin);
            addFlag(currValue, flagScheme.getBadFlag(), maxDeltaPerMinute,
              deltaPerMin);
          }
        }
      }
      i++;
    }
  }

  /**
   * Get the short form QC message
   *
   * @return The short QC message
   */
  @Override
  public String getShortMessage() {
    return "Changes too quickly";
  }

  /**
   * Get the long form QC message
   *
   * @param requiredValue
   *          The value required by the routine
   * @param actualValue
   *          The value received by the routine
   * @return The long form message
   */
  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Changes too quickly - " + flag.getActualValue() + "/min, limit is "
      + flag.getRequiredValue() + "/min";
  }
}
