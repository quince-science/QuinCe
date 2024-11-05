package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.time.LocalDateTime;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

public class PositionQCCascadeRoutine {

  public void run(Instrument instrument, DatasetSensorValues allSensorValues,
    RunTypePeriods runTypePeriods) throws RoutineException {

    try {
      TreeSet<LocalDateTime> allTimes = new TreeSet<LocalDateTime>();
      allTimes.addAll(allSensorValues.getTimes());

      for (LocalDateTime time : allTimes) {
        PlotPageTableValue position = allSensorValues
          .getPositionTableValue(SensorType.LONGITUDE_ID, time);

        if (null != position
          && position.getType() != PlotPageTableValue.NAN_TYPE) {
          // Cascade the QC position to sensor values
          for (SensorValue value : allSensorValues.get(time).values()) {

            SensorType sensorType = instrument.getSensorAssignments()
              .getSensorTypeForDBColumn(value.getColumnId());

            boolean setCascade = true;

            // Don't set the flag for non-measurement calibration values - the
            // quality of the position doesn't matter if we're just calibrating
            // or whatever.
            if (sensorType.hasInternalCalibration() && !instrument
              .isMeasurementRunType(runTypePeriods.getRunType(time))) {
              setCascade = false;
            }

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
}
