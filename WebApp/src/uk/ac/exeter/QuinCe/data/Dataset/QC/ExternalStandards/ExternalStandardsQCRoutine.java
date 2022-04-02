package uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AbstractAutoQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public abstract class ExternalStandardsQCRoutine extends AbstractAutoQCRoutine {

  @Override
  public String getName() {
    return ResourceManager.getInstance()
      .getExternalStandardsRoutinesConfiguration().getRoutineName(this);
  }

  public void setSensorType(SensorType sensorType) {
    this.sensorType = sensorType;
  }

  /**
   * Perform the QC on the specified values.
   *
   * <p>
   * The method ensures that the Routine is correctly configured and then calls
   * {@link #qcAction(List)} to perform the actual QC.
   * </p>
   *
   * @param values
   *          The values to be QCed.
   * @throws RoutineException
   *           If the Routine is not configured correctly, or fails during
   *           processing.
   */
  public void qc(CalibrationSet calibrationSet,
    SearchableSensorValuesList runTypeValues,
    SearchableSensorValuesList sensorValues) throws RoutineException {

    checkSetup();
    qcAction(calibrationSet, runTypeValues, sensorValues);
  }

  protected abstract void qcAction(CalibrationSet calibrationSet,
    SearchableSensorValuesList runTypeValues,
    SearchableSensorValuesList sensorValues) throws RoutineException;
}
