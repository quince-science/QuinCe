package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.LocalDateTime;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

public class PositionQCCascadeRoutine implements Routine {

  public static final String ROUTINE_NAME = "Position QC Cascade";

  public void run(Instrument instrument, DatasetSensorValues allSensorValues,
    RunTypePeriods runTypePeriods) throws RoutineException {

    try {
      TreeSet<LocalDateTime> allTimes = new TreeSet<LocalDateTime>();
      allTimes.addAll(allSensorValues.getTimes());

      for (LocalDateTime time : allTimes) {
        PlotPageTableValue position = allSensorValues
          .getPositionTableValue(SensorType.LONGITUDE_ID, time);

        // If the position is empty, all SensorValues are Bad.
        if (null == position) {

          RoutineFlag missingPositionFlag = new RoutineFlag(this, Flag.BAD,
            "Any Position", "null");

          for (SensorValue value : allSensorValues.get(time).values()) {
            if (shouldApplyFlag(instrument, runTypePeriods, time, value)) {
              value.addAutoQCFlag(missingPositionFlag);
              value.setUserQC(Flag.BAD, getShortMessage());
            }
          }
        } else if (position.getType() != PlotPageTableValue.NAN_TYPE) {
          // Cascade the Position QC to sensor values
          for (SensorValue value : allSensorValues.get(time).values()) {

            SensorType sensorType = instrument.getSensorAssignments()
              .getSensorTypeForDBColumn(value.getColumnId());

            boolean setCascade = shouldApplyFlag(instrument, runTypePeriods,
              time, value);

            removePositionCascadeQC(value, allSensorValues);

            if (setCascade && !position.getQcFlag(allSensorValues).isGood()) {
              value.setCascadingQC(position);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RoutineException(e);
    }
  }

  /**
   * Determine whether a {@link SensorValue} should have Position QC Cascades
   * applied to it.
   *
   * <p>
   * If the {@link SensorValue} is not part of a measurement (e.g. is an
   * internal calibration), it should not be flagged.
   * </p>
   *
   * @param instrument
   *          The Instrument that the SensorValue belongs to.
   * @param runTypePeriods
   *          The run type periods for the dataset that the SensorValue belongs
   *          to.
   * @param time
   *          The base timestamp for the selected SensorValue.
   * @param value
   *          The SensorValue.
   * @return {@code true} if the SensorValue should have Position QC flags
   *         cascaded to it; {@code false} if not.
   *
   * @throws RecordNotFoundException
   *           If the SensorValue details cannot be retrieved.
   * @throws RunTypeCategoryException
   *           If the Run Type cannot be determined.
   */
  private boolean shouldApplyFlag(Instrument instrument,
    RunTypePeriods runTypePeriods, LocalDateTime time, SensorValue value)
    throws RecordNotFoundException, RunTypeCategoryException {

    boolean result = true;

    SensorType sensorType = instrument.getSensorAssignments()
      .getSensorTypeForDBColumn(value.getColumnId());

    if (sensorType.hasInternalCalibration()
      && !instrument.isMeasurementRunType(runTypePeriods.getRunType(time))) {
      result = false;
    }

    return result;
  }

  private void removePositionCascadeQC(SensorValue value,
    DatasetSensorValues allSensorValues) {
    if (value.getUserQCFlag().equals(Flag.LOOKUP)) {
      SortedSet<Long> sources = StringUtils
        .delimitedToLongSet(value.getUserQCMessage());

      for (Long sourceID : sources) {
        SensorValue source = allSensorValues.getById(sourceID);
        if (source.isPosition()) {
          value.removeCascadingQC(sourceID);
        }
      }
    }
  }

  @Override
  public String getName() {
    return ROUTINE_NAME;
  }

  @Override
  public String getShortMessage() {
    return "Invalid/missing position";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Invalid/missing position";
  }
}
