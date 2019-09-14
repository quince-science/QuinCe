package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.time.temporal.ChronoUnit;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class HighDeltaRoutine extends Routine {

  /**
   * The maximum delta between values, in units per minute
   */
  private double maxDelta;

  /**
   * Basic constructor
   * 
   * @param parameters
   *          The parameters
   * @throws QCRoutinesConfigurationException
   *           If the parameters are invalid
   */
  public HighDeltaRoutine(List<String> parameters) throws RoutineException {
    super(parameters);
  }

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
  public void qcValues(List<SensorValue> values) throws RoutineException {
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
  public static String getShortMessage() {
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
  public static String getLongMessage(String requiredValue,
    String actualValue) {
    return "Changes too quickly - " + actualValue + "/min, limit is "
      + requiredValue + "/min";
  }
}
