package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class OutlierRoutine extends Routine {

  /**
   * The maximum number of standard deviations away from the mean a
   * value can be before it is considered an outlier.
   */
  private double stdevLimit;

  /**
   * Basic constructor
   * @param parameters The parameters
   * @throws QCRoutinesConfigurationException If the parameters are invalid
   */
  public OutlierRoutine(List<String> parameters)
    throws RoutineException {
    super(parameters);
  }

  @Override
  protected void validateParameters() throws RoutineException {
    if (parameters.size() != 1) {
      throw new RoutineException("Incorrect number of parameters. Must be <stdevLimit>");
    }

    try {
      stdevLimit = Double.parseDouble(parameters.get(0));
    } catch (NumberFormatException e) {
      throw new RoutineException("Standard deviation limit parameter must be numeric");
    }

    if (stdevLimit <= 0) {
      throw new RoutineException("Standard deviation limit must be greater than zero");
    }
  }

  @Override
  public void qcValues(List<SensorValue> values) throws RoutineException {

    int valueCount = 0;
    double mean = 0.0;
    double stdev = 0.0;

    for (SensorValue sensorValue: values) {
      Double value = sensorValue.getDoubleValue();
      if (!value.isNaN()) {
        valueCount++;

        if (valueCount == 1) {
          mean = value;
        } else {
          double d = value - mean;
          stdev += (valueCount - 1)*d*d/valueCount;
          mean += d/valueCount;
        }
      }
    }

    if (valueCount > 0) {
      // Finalise the stdev calculation
      stdev = Math.sqrt(stdev / valueCount);

      // Check all values to see if they're outside the limit
      for (SensorValue sensorValue : values) {
        if (!sensorValue.isNaN()) {
          double diffFromMean = Math.abs(sensorValue.getDoubleValue() - mean);

          if (diffFromMean > (stdev * stdevLimit)) {
            addFlag(sensorValue, Flag.BAD, stdevLimit, stdev);
          }
        }
      }
    }
  }

  /**
   * Get the short form QC message
   * @return The short QC message
   */
  public static String getShortMessage() {
    return "Standard deviation is too large";
  }

  /**
   * Get the long form QC message
   * @param requiredValue The value required by the routine
   * @param actualValue The value received by the routine
   * @return The long form message
   */
  public static String getLongMessage(String requiredValue, String actualValue) {
    return "Standard deviation is " + actualValue + ", should be <= " + requiredValue;
  }
}
