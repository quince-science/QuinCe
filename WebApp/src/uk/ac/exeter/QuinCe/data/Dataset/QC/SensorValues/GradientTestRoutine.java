package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.temporal.ChronoUnit;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;

public class GradientTestRoutine extends AutoQCRoutine {

  /**
   * The maximum delta between values, in units per minute
   */
  private double maxDeltaPerMinute;

  @Override
  protected void validateParameters() throws RoutineException {
    // copied from HighDeltaRoutine
    if (null == parameters || parameters.size() != 1) {
      throw new RoutineException(
        "Incorrect number of parameters. Must be <maxDelta>");
    }

    try {
      maxDeltaPerMinute = Double.parseDouble(parameters.get(0));
    } catch (NumberFormatException e) {
      throw new RoutineException("Max delta parameter must be numeric");
    }

    if (maxDeltaPerMinute <= 0) {
      throw new RoutineException("Max duration must be greater than zero");
    }
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {

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
        double tDiff = ChronoUnit.NANOS.between(prevValue.getTime(),
          currValue.getTime()) / (60.0 * 1000000000);

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
            addFlag(currValue, Flag.BAD, maxDeltaPerMinute, deltaPerMin);

            // We know that this is a single spike, so we can skip checking the
            // next value.
            i++;
          } else {
            // This is a gradient, so mark the previous and current points as
            // the starting point of the gradient. We can't be certain which
            // "side" of the change is wrong.
            addFlag(prevValue, Flag.BAD, maxDeltaPerMinute, deltaPerMin);
            addFlag(currValue, Flag.BAD, maxDeltaPerMinute, deltaPerMin);
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
