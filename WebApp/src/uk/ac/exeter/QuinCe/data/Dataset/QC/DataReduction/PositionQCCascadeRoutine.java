package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.flywaydb.core.internal.util.StringUtils;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

public class PositionQCCascadeRoutine extends DataReductionQCRoutine {

  public PositionQCCascadeRoutine() {
    super();
    settings = new DataReductionQCRoutineSettings();
  }

  @Override
  protected void qcAction(Connection conn, Instrument instrument,
    DataSet dataSet, Variable variable,
    TreeMap<Measurement, ReadOnlyDataReductionRecord> dataReductionRecords,
    DatasetSensorValues allSensorValues, FlaggedItems flaggedItems)
    throws RoutineException {

    try {
      RunTypePeriods runTypePeriods = DataSetDataDB.getRunTypePeriods(conn,
        instrument, dataSet.getId());

      TreeSet<LocalDateTime> allTimes = new TreeSet<LocalDateTime>();
      allTimes.addAll(allSensorValues.getTimes());
      dataReductionRecords.keySet().stream().map(m -> m.getTime())
        .forEach(t -> allTimes.add(t));

      for (LocalDateTime time : allTimes) {
        PlotPageTableValue position = allSensorValues
          .getPositionTableValue(SensorType.LONGITUDE_ID, time, true);

        if (null != position && !position.getQcFlag().isGood()) {

          // Cascade the QC position to sensor values
          for (SensorValue value : allSensorValues.get(time).values()) {

            SensorType sensorType = instrument.getSensorAssignments()
              .getSensorTypeForDBColumn(value.getColumnId());

            boolean setCascade = true;

            // Don't set the flag for non-measurement calibration values - the
            // quality of the position doesn't matter if we're just calibrating
            // or whatever.
            if (sensorType.hasInternalCalibration()
              && !instrument.getRunTypeCategory(variable.getId(),
                runTypePeriods.getRunType(time)).isMeasurementType()) {
              setCascade = false;
            }

            if (setCascade) {
              value.setCascadingQC(position);
              flaggedItems.add(value);
            }
          }

          // and to the measurements
          for (Map.Entry<Measurement, ReadOnlyDataReductionRecord> entry : dataReductionRecords
            .entrySet()) {

            if (entry.getKey().getTime().equals(time)) {

              entry.getKey().getMeasurementValues().forEach(v -> {
                v.overrideQC(Flag.LOOKUP, StringUtils
                  .collectionToCommaDelimitedString(position.getSources()));
              });

              entry.getValue().setCascadingQC(position);
              flaggedItems.add(entry.getKey());
              flaggedItems.add(entry.getValue());
            }
          }
        }
      }
    } catch (

    Exception e) {
      throw new RoutineException(e);
    }

  }

  @Override
  public String getShortMessage() {
    return "Bad position";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Bad position";
  }
}
