package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;

public class RangeCheckRoutine extends AutoQCRoutine {

  /**
   * Questionable minimum parameter index
   */
  private static final int QUESTIONABLE_MIN_PARAM = 0;

  /**
   * Questionable minimum parameter index
   */
  private static final int QUESTIONABLE_MAX_PARAM = 1;

  /**
   * Questionable minimum parameter index
   */
  private static final int BAD_MIN_PARAM = 2;

  /**
   * Questionable minimum parameter index
   */
  private static final int BAD_MAX_PARAM = 3;

  /**
   * The minimum value of the range that will trigger a
   * {@link Flag#QUESTIONABLE}.
   */
  private double questionableMin = 0.0;

  /**
   * The maximum value of the range that will trigger a
   * {@link Flag#QUESTIONABLE}.
   */
  private double questionableMax = 0.0;

  /**
   * The minimum value of the range that will trigger a {@link Flag#BAD}.
   */
  private double badMin = 0.0;

  /**
   * The maximum value of the range that will trigger a {@link Flag#BAD}.
   */
  private double badMax = 0.0;

  /**
   * Indicates whether or not this range checker has a Questionable range
   * configured
   */
  private boolean hasQuestionableRange = false;

  /**
   * Indicates whether or not this range checker has a Bad range configured
   */
  private boolean hasBadRange = false;

  @Override
  protected void validateParameters() throws RoutineException {
    if (parameters.size() != 4) {
      throw new RoutineException("Incorrect number of parameters. Must be"
        + "<questionable_range_min>,<questionable_range_max>,<bad_range_min>,<bad_range_max>");
    }

    if (parameters.get(QUESTIONABLE_MIN_PARAM).trim().length() > 0
      || parameters.get(QUESTIONABLE_MAX_PARAM).trim().length() > 0) {

      hasQuestionableRange = true;
      try {
        questionableMin = Double.parseDouble(parameters.get(0));
        questionableMax = Double.parseDouble(parameters.get(1));
      } catch (NumberFormatException e) {
        throw new RoutineException(
          "Questionable range parameters must be numeric", e);
      }
    }

    if (parameters.get(BAD_MIN_PARAM).trim().length() > 0
      || parameters.get(BAD_MAX_PARAM).trim().length() > 0) {

      hasBadRange = true;
      try {
        badMin = Double.parseDouble(parameters.get(2));
        badMax = Double.parseDouble(parameters.get(3));
      } catch (NumberFormatException e) {
        throw new RoutineException("Bad range parameters must be numeric", e);
      }
    }

    if (hasQuestionableRange && hasBadRange) {
      if (badMin > questionableMin || badMax < questionableMax) {
        throw new RoutineException(
          "Bad range must be larger than questionable range");
      }
    }
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {
    for (SensorValue sensorValue : values) {
      Double value = sensorValue.getDoubleValue();

      if (!value.isNaN()) {
        if (hasBadRange && (value < badMin || value > badMax)) {
          addFlag(sensorValue, Flag.BAD, "" + badMin + ":" + badMax,
            String.valueOf(value));
        } else if (hasQuestionableRange
          && (value < questionableMin || value > questionableMax)) {
          addFlag(sensorValue, Flag.QUESTIONABLE,
            "" + questionableMin + ":" + questionableMax,
            String.valueOf(value));
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
    return "Out of range";
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
    return "Out of range - Should be in " + flag.getRequiredValue()
      + ", actual value is " + flag.getActualValue();
  }
}
