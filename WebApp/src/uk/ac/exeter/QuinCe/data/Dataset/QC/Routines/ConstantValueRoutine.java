package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class ConstantValueRoutine extends Routine {

  /**
   * The maximum time that a value can remain constant (in minutes)
   */
  private double maxDuration;

  /**
   * Basic constructor
   * @param parameters The parameters
   * @throws QCRoutinesConfigurationException If the parameters are invalid
   */
  public ConstantValueRoutine(List<String> parameters)
    throws RoutineException {
    super(parameters);
  }

  @Override
  protected void validateParameters() throws RoutineException {
    if (parameters.size() != 1) {
      throw new RoutineException("Incorrect number of parameters. Must be <maxDuration>");
    }

    try {
      maxDuration = Double.parseDouble(parameters.get(0));
    } catch (NumberFormatException e) {
      throw new RoutineException("Max duration parameter must be numeric");
    }

    if (maxDuration <= 0) {
      throw new RoutineException("Max duration must be greater than zero");
    }
  }

  @Override
  public void qcValues(List<SensorValue> values) throws RoutineException {

    List<SensorValue> valueCollection = new ArrayList<SensorValue>();

    for (SensorValue value : values) {
      if (!value.isNaN()) {
        // If there's no record stored, this is the first of a new constant value
        if (valueCollection.size() == 0) {
          valueCollection.add(value);
        } else {
          if (equalsConstant(value, valueCollection.get(0))) {
            // If it equals the value in the first record, then it's still a
            // constant value
            valueCollection.add(value);
          } else {
            // The value is no longer constant.
            // See how long it was constant for
            doDurationCheck(valueCollection);

            // Clear the list of constant records and start again
            valueCollection.clear();
            valueCollection.add(value);
          }

        }
      }
    }

    if (valueCollection.size() > 1) {
      doDurationCheck(valueCollection);
    }
  }

  /**
   * Determines whether or not the value in the passed record is identical to that
   * in the list of constant records. Null values always return a 'not constant' result.
   * @param record The record to be checked
   * @param firstRecord The first record of the period of constant values
   * @return {@code true} if the value in the record equals that in the list of
   *         constant records; {@code false} otherwise.
   * @throws RoutineException If the value cannot be compared.
   */
  private boolean equalsConstant(SensorValue value, SensorValue firstValue)
    throws RoutineException {

    boolean result = false;

    try {
      result = (value.getDoubleValue().equals(firstValue.getDoubleValue()));
    } catch (NumberFormatException e) {
      throw new RoutineException("Cannot compare non-numeric values", e);
    }

    return result;
  }

  /**
   * See how long the value has been constant in the set of stored records.
   * If the value is constant for longer than the maximum time, flag each record accordingly.
   * @param constantRecords The records to be checked
   * @throws RoutineException If the records cannot be flagged.
   */
  private void doDurationCheck(List<SensorValue> constantValues)
    throws RoutineException {

    // For measurements taken a long time apart, the value can easily be constant.
    // For example, measurements taken hourly can happily have the same value, but
    // if the constant check is set for 30 minutes it will always be triggered.
    //
    // Therefore we make sure there's more than two consecutive measurements with the
    // constant value.
    if (constantValues.size() > 2) {

      long minutesDifference = ChronoUnit.MINUTES.between(
        constantValues.get(0).getTime(),
        constantValues.get(constantValues.size() - 1).getTime());

      if (minutesDifference > maxDuration) {
        for (SensorValue value : constantValues) {
          addFlag(value, Flag.BAD, maxDuration, minutesDifference);
        }
      }
    }
  }

  /**
   * Get the short form QC message
   * @return The short QC message
   */
  public static String getShortMessage() {
    return "Constant for too long";
  }

  /**
   * Get the long form QC message
   * @param requiredValue The value required by the routine
   * @param actualValue The value received by the routine
   * @return The long form message
   */
  public static String getLongMessage(String requiredValue, String actualValue) {
    return "Constant for " + actualValue + " minutes - limit is "
            + requiredValue + " minutes";
  }
}
