package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public class GradientTestRoutine extends Routine {

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
  public GradientTestRoutine(List<String> parameters) throws RoutineException {
    super(parameters);
    // TODO Auto-generated constructor stub
  }

  @Override
  protected void validateParameters() throws RoutineException {
    // copied from HighDeltaRoutine
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
    SensorValue prevValue = null;
    SensorValue currValue = null;
    SensorValue nextValue = null;
    int gradientStart = 0;

    List<SensorValue> filteredValues = filterMissingValues(values);

    int i = 1;
    while (i < filteredValues.size() - 1) {
      currValue = filteredValues.get(i);

      prevValue = filteredValues.get(i - 1);
      nextValue = filteredValues.get(i + 1);

      // calculate observed change
      double obsChange = Math.abs(currValue.getDoubleValue()
        - (nextValue.getDoubleValue() + prevValue.getDoubleValue()) / 2);
      // calculate expected change
      double expChange = Math
        .abs((nextValue.getDoubleValue() - prevValue.getDoubleValue()) / 2);

      // Eqs from Ylva Ericson CPH
      // Spike: Test value = | cV - (nV + pV)/2 | - | (nV - pV) / 2 |,
      // Gradient: Test value = | cV - (nV + pV)/2 | = obsChange
      // fitGradient = obsChange;
      double fitSpike = obsChange - expChange;

      if (obsChange > maxDelta) {
        if (fitSpike > maxDelta) {
          // spike, add flag to currValue
          addFlag(currValue, Flag.BAD, maxDelta, fitSpike);
          System.out
            .println("Setting spike-flag " + currValue.getDoubleValue());
          i++;
        } else { // gradient
          gradientStart = i - 1;
          addFlag(prevValue, Flag.BAD, maxDelta, obsChange);

          while (obsChange > maxDelta) {
            addFlag(currValue, Flag.BAD, maxDelta, obsChange);

            i++;
            currValue = filteredValues.get(i);
            prevValue = filteredValues.get(i - 1);
            nextValue = filteredValues.get(i + 1);

            obsChange = Math.abs(currValue.getDoubleValue()
              - (nextValue.getDoubleValue() + prevValue.getDoubleValue()) / 2);
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
  public static String getShortMessage() {
    return "Gradient too steep, changes too quickly";
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
    return "Gradient too steep. Changes too quickly - " + actualValue
      + "/min, limit is " + requiredValue + "/min";
  }
}
