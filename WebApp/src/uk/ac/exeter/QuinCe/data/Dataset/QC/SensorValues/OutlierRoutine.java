package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Range;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class OutlierRoutine extends AutoQCRoutine {

  public OutlierRoutine(FlagScheme flagScheme, SensorType sensorType,
    Map<Flag, Range<Double>> limits) {
    super(flagScheme, sensorType, limits);
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {

    double stdevLimit = limits.get(flagScheme.getBadFlag()).getMaximum();

    int valueCount = 0;
    double mean = 0.0;
    double stdev = 0.0;

    for (SensorValue sensorValue : values) {
      Double value = sensorValue.getDoubleValue();
      if (!value.isNaN()) {
        valueCount++;

        if (valueCount == 1) {
          mean = value;
        } else {
          double d = value - mean;
          stdev += (valueCount - 1) * d * d / valueCount;
          mean += d / valueCount;
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
            addFlag(sensorValue, flagScheme.getBadFlag(), stdevLimit, stdev);
          }
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
    return "Standard deviation is too large";
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
    return "Standard deviation is " + flag.getActualValue() + ", should be <= "
      + flag.getRequiredValue();
  }
}
