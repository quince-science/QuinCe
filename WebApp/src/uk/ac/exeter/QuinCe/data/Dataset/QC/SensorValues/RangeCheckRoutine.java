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

public class RangeCheckRoutine extends AutoQCRoutine {

  public RangeCheckRoutine(FlagScheme flagScheme) {
    super(flagScheme);
  }

  public RangeCheckRoutine(FlagScheme flagScheme, SensorType sensorType,
    Map<Flag, Range<Double>> limits) {
    super(flagScheme, sensorType, limits);
  }

  @Override
  protected void qcAction(List<SensorValue> values) throws RoutineException {
    for (SensorValue sensorValue : values) {
      Double value = sensorValue.getDoubleValue();

      if (!value.isNaN()) {
        RoutineFlag flag = getRangeFlag(value);
        if (null != flag) {
          addFlag(sensorValue, flag);
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
