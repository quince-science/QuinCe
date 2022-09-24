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
    if (parameters.size() != 1) {
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
    while (i < filteredValues.size() - 1) {
      currValue = filteredValues.get(i);

      prevValue = filteredValues.get(i - 1);
      nextValue = filteredValues.get(i + 1);

      // If the change is equal to the smallest possible change
      // we can't do the gradient check
      double valueDelta = Math
        .abs(currValue.getDoubleValue() - prevValue.getDoubleValue());
      if (valueDelta > minimumChange) {

        // time-increment
        double tDiff = ChronoUnit.NANOS.between(prevValue.getTime(),
          currValue.getTime()) / (60.0 * 1000000000);

        double deltaPerMin = valueDelta / tDiff;

        if (deltaPerMin > maxDeltaPerMinute) { // spike or gradient
          double deltaNext = Math.abs(
            nextValue.getDoubleValue() - prevValue.getDoubleValue()) / tDiff;
          if (deltaNext < maxDeltaPerMinute) { // Spike
            addFlag(currValue, Flag.BAD, maxDeltaPerMinute, deltaPerMin);
            i++;

          } else { // Gradient
            addFlag(prevValue, Flag.BAD, maxDeltaPerMinute, deltaPerMin);

            while ((deltaPerMin > maxDeltaPerMinute)
              && (i < filteredValues.size() - 1)) {
              addFlag(currValue, Flag.BAD, maxDeltaPerMinute, deltaPerMin);

              i++;
              currValue = filteredValues.get(i);
              prevValue = filteredValues.get(i - 1);

              tDiff = ChronoUnit.NANOS.between(prevValue.getTime(),
                currValue.getTime()) / (60.0 * 1000000000);

              deltaPerMin = Math
                .abs(currValue.getDoubleValue() - prevValue.getDoubleValue())
                / tDiff;
            }
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
