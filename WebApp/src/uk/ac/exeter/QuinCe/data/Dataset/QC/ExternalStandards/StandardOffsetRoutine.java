package uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards;

import java.util.Map;

import org.apache.commons.lang3.Range;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListException;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class StandardOffsetRoutine extends ExternalStandardsQCRoutine {

  public StandardOffsetRoutine(FlagScheme flagScheme) {
    super(flagScheme);
  }

  public StandardOffsetRoutine(FlagScheme flagScheme, SensorType sensorType,
    Map<Flag, Range<Double>> limits) {
    super(flagScheme, sensorType, limits);
  }

  @Override
  public String getShortMessage() {
    return "Standard offset too large";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Offset from specified standard concentration too large - was "
      + flag.getActualValue() + ", should be ";
  }

  @Override
  protected void qcAction(CalibrationSet externalStandards,
    SensorValuesList runTypeValues, SensorValuesList sensorValues)
    throws SensorValuesListException, RoutineException {

    for (SensorValue sensorValue : sensorValues.getRawValues()) {

      SensorValuesListValue runTypeValue = runTypeValues
        .getValueOnOrBefore(sensorValue.getCoordinate());
      if (null != runTypeValue) {
        String runType = runTypeValue.getStringValue();

        if (externalStandards.getTargets().contains(runType)) {
          double standardValue = externalStandards
            .getCalibrations(sensorValue.getCoordinate().getTime()).get(runType)
            .getDoubleCoefficient(sensorType.getShortName());

          double offset = Math
            .abs(sensorValue.getDoubleValue() - standardValue);

          if (!Double.isNaN(offset)) {
            RoutineFlag flag = getRangeFlag(offset, true);
            if (!flagScheme.isGood(flag, true)) {
              addFlag(sensorValue, flag);
            }
          }
        }
      }
    }
  }
}
