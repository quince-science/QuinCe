package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.temporal.ChronoUnit;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;

public class HighDeltaRoutine extends AutoQCRoutine {

  /**
   * The maximum delta between values, in units per minute
   */
  private double maxDelta;

  @Override
  protected void validateParameters() throws RoutineException {
    if (parameters.size() != 1) {
      throw new RoutineException(
        "Incorrect number of parameters. Must be <maxDelta>");
    }

    try {
      maxDelta = Double.parseDouble(parameters.get(0));
    } catch (NumberFormatException e) {
      throw new RoutineException("Max delta parameter must be numeric");
    }

    if (maxDelta <= 0) {
      throw new RoutineException("Max duration must be greater than zero");
    }
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {
    SensorValue lastValue = null;

    for (SensorValue sensorValue : values) {
      if (null == lastValue) {
        if (!sensorValue.isNaN()) {
          lastValue = sensorValue;
        }
      } else {

        // Calculate the change between this record and the previous one
        if (!sensorValue.isNaN()) {
          double minutesDifference = ChronoUnit.SECONDS
            .between(lastValue.getTime(), sensorValue.getTime()) / 60.0;

          double valueDelta = Math
            .abs(sensorValue.getDoubleValue() - lastValue.getDoubleValue());

          double deltaPerMinute = valueDelta / minutesDifference;

          if (deltaPerMinute > maxDelta) {
            addFlag(sensorValue, Flag.BAD, maxDelta, deltaPerMinute);
          }

          lastValue = sensorValue;
        }
      }
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
