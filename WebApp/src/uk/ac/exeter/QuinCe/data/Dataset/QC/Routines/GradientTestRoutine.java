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

    for (int i = 1; i < values.size(); i++) {
      currValue = values.get(i);

      prevValue = values.get(i - 1);
      nextValue = values.get(i + 1);

      if (!currValue.isNaN()) {

        int r = 1;
        while (prevValue.isNaN()) {
          r++;
          prevValue = values.get(i - r);
        }
        r = 1;
        while (nextValue.isNaN()) {
          r++;
          nextValue = values.get(i + r);
        }

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
            i++;
          } else { // gradient
            addFlag(prevValue, Flag.BAD, maxDelta, obsChange);
            addFlag(currValue, Flag.BAD, maxDelta, obsChange);
          }
        }

      }
    }
  }
}
