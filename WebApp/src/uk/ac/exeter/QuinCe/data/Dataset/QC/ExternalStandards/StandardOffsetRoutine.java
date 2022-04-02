package uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards;

import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class StandardOffsetRoutine extends ExternalStandardsQCRoutine {

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
  protected void validateParameters() throws RoutineException {
  }

  @Override
  protected void qcAction(CalibrationSet calibrationSet,
    SearchableSensorValuesList runTypeValues,
    SearchableSensorValuesList sensorValues) throws RoutineException {

    for (SensorValue sensorValue : sensorValues) {

      SensorValue runTypeValue = runTypeValues.get(sensorValue.getTime());
      if (null != runTypeValue) {
        String runType = runTypeValue.getValue();
        if (calibrationSet.containsTarget(runType)) {

          try {
            double standardValue = calibrationSet.getCalibrationValue(runType,
              sensorType.getShortName());

            double offset = Math
              .abs(sensorValue.getDoubleValue() - standardValue);
            if (!Double.isNaN(offset)
              && Math.abs(offset) > Double.valueOf(parameters.get(0))) {

              addFlag(sensorValue, Flag.BAD, parameters.get(0), offset);
            }

          } catch (RecordNotFoundException e) {
            // We don't mind
          }
        }
      }
    }
  }
}
