package uk.ac.exeter.QuinCe.data.Dataset.QC.ExternalStandards;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Range;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValuesListException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AbstractAutoQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public abstract class ExternalStandardsQCRoutine extends AbstractAutoQCRoutine {

  public ExternalStandardsQCRoutine(FlagScheme flagScheme) {
    super(flagScheme);
  }

  public ExternalStandardsQCRoutine(FlagScheme flagScheme,
    SensorType sensorType, Map<Flag, Range<Double>> limits) {
    super(flagScheme, sensorType, limits);
  }

  @Override
  public String getName() {
    return ResourceManager.getInstance()
      .getExternalStandardsRoutinesConfiguration(flagScheme.getBasis())
      .getRoutineName(this);
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
  public void qc(CalibrationSet calibrationSet, SensorValuesList runTypeValues,
    SensorValuesList sensorValues)
    throws SensorValuesListException, RoutineException {

    checkSetup();
    qcAction(calibrationSet, runTypeValues, sensorValues);
  }

  protected abstract void qcAction(CalibrationSet calibrationSet,
    SensorValuesList runTypeValues, SensorValuesList sensorValues)
    throws RoutineException, SensorValuesListException;
}
