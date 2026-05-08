package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.LocalDateTime;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

public class PositionQCCascadeRoutine extends Routine {

  public static final String ROUTINE_NAME = "Position QC Cascade";

  public PositionQCCascadeRoutine(FlagScheme flagScheme) {
    super(flagScheme, null);
  }

  public void run(Instrument instrument, DatasetSensorValues allSensorValues,
    RunTypePeriods runTypePeriods) throws RoutineException {

    stubCheck();

    try {
      TreeSet<Coordinate> allCoordiantes = new TreeSet<Coordinate>();
      allCoordiantes.addAll(allSensorValues.getCoordinates());

      for (Coordinate coordinate : allCoordiantes) {
        PlotPageTableValue position = allSensorValues
          .getPositionTableValue(SensorType.LONGITUDE_ID, coordinate);

        // If the position is empty, all SensorValues are Bad.
        if (null == position) {
          RoutineFlag missingPositionFlag = new RoutineFlag(flagScheme, this,
            flagScheme.getBadFlag(), "Any Position", "null");

          for (SensorValue value : allSensorValues.get(coordinate).values()) {
            if (shouldApplyFlag(instrument, runTypePeriods,
              coordinate.getTime(), value)) {
              value.addAutoQCFlag(missingPositionFlag);
              value.setUserQC(flagScheme.getBadFlag(), getShortMessage());
            }
          }
        } else if (position.getType() != PlotPageTableValue.NAN_TYPE) {
          // Cascade the Position QC to sensor values
          for (SensorValue value : allSensorValues.get(coordinate).values()) {
            boolean setCascade = shouldApplyFlag(instrument, runTypePeriods,
              coordinate.getTime(), value);

            removePositionCascadeQC(value, allSensorValues);

            if (setCascade && !flagScheme
              .isGood(position.getQcFlag(allSensorValues), true)) {
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

    if (sensorType.equals(SensorType.RUN_TYPE_SENSOR_TYPE)
      || (sensorType.hasInternalCalibration()
        && !instrument.isMeasurementRunType(runTypePeriods.getRunType(time)))) {
      result = false;
    }

    return result;
  }

  private void removePositionCascadeQC(SensorValue value,
    DatasetSensorValues allSensorValues) {
    if (value.getUserQCFlag().equals(FlagScheme.LOOKUP_FLAG)) {
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
