package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.ReadOnlyDataReductionRecord;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.FlaggedItems;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * QC Routine to detect whether values that are calibrated with external
 * standards had all required standards available.
 */
public class MissingStandardsRoutine extends DataReductionQCRoutine {

  @Override
  protected void qcAction(Connection conn, Instrument instrument,
    DataSet dataSet, Variable variable,
    TreeMap<Measurement, ReadOnlyDataReductionRecord> dataReductionRecords,
    DatasetSensorValues allSensorValues, FlaggedItems flaggedItems)
    throws RoutineException {

    try {
      RunTypePeriods runTypePeriods = DataSetDataDB.getRunTypePeriods(conn,
        instrument, dataSet.getId());

      // All values detected by this routine get the same flag
      RoutineFlag flag = new RoutineFlag(this, Flag.BAD, null, null);

      for (Map.Entry<Measurement, ReadOnlyDataReductionRecord> entry : dataReductionRecords
        .entrySet()) {

        Measurement measurement = entry.getKey();

        // Loop through all SensorTypes with internal calibrations
        for (SensorType sensorType : measurement
          .getMeasurementValueSensorTypes()) {
          if (sensorType.hasInternalCalibration()) {

            Set<String> beforeCalibsFound = new HashSet<String>();
            Set<String> afterCalibsFound = new HashSet<String>();
            boolean calibrationsOK = false;

            MeasurementValue value = measurement
              .getMeasurementValue(sensorType);

            for (long ssvid : value.getSupportingSensorValueIds()) {
              SensorValue ssValue = allSensorValues.getById(ssvid);

              if (instrument.getSensorAssignments()
                .isOfSensorType(ssValue.getColumnId(), sensorType)) {

                String runType = runTypePeriods.getRunType(ssValue.getTime());
                if (instrument.getRunTypeCategory(variable.getId(), runType)
                  .equals(RunTypeCategory.INTERNAL_CALIBRATION)) {

                  if (ssValue.getTime().isBefore(measurement.getTime())) {
                    beforeCalibsFound.add(runType);
                  } else {
                    afterCalibsFound.add(runType);
                  }

                  if (beforeCalibsFound.size() >= 2
                    && afterCalibsFound.size() >= 2) {

                    calibrationsOK = true;
                    break;
                  }
                }
              }
            }

            if (!calibrationsOK) {
              flagSensors(instrument, measurement, entry.getValue(),
                allSensorValues, flag, flaggedItems, false);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new RoutineException(e);
    }

  }

  @Override
  public String getShortMessage() {
    return "Missing prior and/or post gas standards";
  }

  @Override
  public String getLongMessage(RoutineFlag flag) {
    return "Missing prior and/or post gas standards";
  }
}
