package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Range;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class HighDeltaRoutine extends AutoQCRoutine {

  public HighDeltaRoutine(FlagScheme flagScheme) {
    super(flagScheme);
  }

  public HighDeltaRoutine(FlagScheme flagScheme, SensorType sensorType,
    Map<Flag, Range<Double>> limits) {
    super(flagScheme, sensorType, limits);
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {
    SensorValue lastValue = null;

    double maxDelta = limits.get(flagScheme.getBadFlag()).getMaximum();

    for (SensorValue sensorValue : values) {
      if (null == lastValue) {
        if (!sensorValue.isNaN()) {
          lastValue = sensorValue;
        }
      } else {

        // Calculate the change between this record and the previous one
        if (!sensorValue.isNaN()) {
          double minutesDifference = ChronoUnit.SECONDS.between(
            lastValue.getCoordinate().getTime(),
            sensorValue.getCoordinate().getTime()) / 60.0;

          double valueDelta = Math
            .abs(sensorValue.getDoubleValue() - lastValue.getDoubleValue());

          double deltaPerMinute = valueDelta / minutesDifference;

          if (deltaPerMinute > maxDelta) {
            addFlag(sensorValue, flagScheme.getBadFlag(), maxDelta,
              deltaPerMinute);
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
