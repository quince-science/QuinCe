package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ControsPco2MeasurementLocator extends MeasurementLocator {

  private static final int NO_STATUS = -1;

  private static final int ZERO = 0;

  private static final int FLUSHING = 1;

  private static final int MEASUREMENT = 2;

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset, DatasetSensorValues sensorValues)
    throws MeasurementLocatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable variable = sensorConfig.getInstrumentVariable(getVariableName());

      long zeroFlushTime = Math.round(Double.parseDouble(dataset
        .getAllProperties().get(variable.getName()).getProperty("zero_flush")));

      SensorType zeroType = sensorConfig.getSensorType("Zero Mode");
      SensorType flushingType = sensorConfig.getSensorType("Flush Mode");
      SensorType rawSensorType = sensorConfig
        .getSensorType("Raw Detector Signal");
      SensorType refSensorType = sensorConfig.getSensorType("Reference Signal");

      // Assume one column of each type
      long zeroColumn = instrument.getSensorAssignments().getColumnIds(zeroType)
        .get(0);
      long flushingColumn = instrument.getSensorAssignments()
        .getColumnIds(flushingType).get(0);
      long rawColumn = instrument.getSensorAssignments()
        .getColumnIds(rawSensorType).get(0);
      long refColumn = instrument.getSensorAssignments()
        .getColumnIds(refSensorType).get(0);

      CalibrationSet calibrationSet = CalculationCoefficientDB.getInstance()
        .getCalibrationSet(conn, dataset);

      CalculationCoefficient defaultRunTimeCoefficient = CalculationCoefficient
        .getCoefficient(calibrationSet, variable, "Response Time",
          dataset.getStart());

      long totalFlushingTime = Math.round(defaultRunTimeCoefficient.getValue());

      List<LocalDateTime> times = sensorValues.getTimes();

      // Loop through all the rows, examining the zero/flush columns to decide
      // what to do
      List<SensorValue> flaggedSensorValues = new ArrayList<SensorValue>();
      List<Measurement> measurements = new ArrayList<Measurement>(times.size());

      /*
       * These values store the status of the record in the source data file
       * (based on the ZERO and FLUSH columns).
       *
       * If the status of any record is changed by the code, it will have no
       * effect here.
       */
      int currentRecordStatus = NO_STATUS;
      int lastRecordStatus = NO_STATUS;
      LocalDateTime currentRecordStatusStart = null;

      // The time at which the last FLUSHING status started
      LocalDateTime lastFlushingStatusStart = null;

      for (LocalDateTime recordTime : times) {

        Map<Long, SensorValue> recordValues = sensorValues.get(recordTime);

        /*
         * If there is no ZERO value in the record, then this is not from the
         * CONTROS sensor and we ignore it.
         */
        if (recordValues.containsKey(zeroColumn)) {
          int recordStatus = getRecordStatus(recordValues, zeroColumn,
            flushingColumn);

          if (recordStatus != NO_STATUS) {

            if (recordStatus != currentRecordStatus) {
              lastRecordStatus = currentRecordStatus;
              currentRecordStatus = recordStatus;
              currentRecordStatusStart = recordTime;

              if (recordStatus == FLUSHING) {
                lastFlushingStatusStart = recordTime;
              }
            }

            boolean flushSensors = false;
            String runType;

            if (recordStatus == ZERO) {
              if (zeroFlushTime > 0
                && DateTimeUtils.secondsBetween(currentRecordStatusStart,
                  recordTime) <= zeroFlushTime) {
                flushSensors = true;
              }

              runType = Measurement.INTERNAL_CALIBRATION_RUN_TYPE;
            } else {
              if (recordStatus == FLUSHING) {
                flushSensors = true;
              } else {

                /*
                 * This is a measurement. However, if it's just after a ZERO or
                 * FLUSHING then the instrument may still need flushing.
                 */

                /*
                 * If the last status was FLUSHING, then see if we are within
                 * the total flushing time since the start of that period.
                 */
                if (lastRecordStatus == FLUSHING) {
                  if (DateTimeUtils.secondsBetween(lastFlushingStatusStart,
                    recordTime) <= totalFlushingTime) {

                    flushSensors = true;
                  }
                }

                /*
                 * If the last status was ZERO (or we are at the start of the
                 * dataset), then see how much time has passed since the first
                 * measurement in this sequence. If it is less than the total
                 * flushing time then we are still flushing.
                 */
                if (lastRecordStatus == NO_STATUS || lastRecordStatus == ZERO) {
                  if (DateTimeUtils.secondsBetween(currentRecordStatusStart,
                    recordTime) <= totalFlushingTime) {
                    flushSensors = true;
                  }
                }
              }

              runType = Measurement.MEASUREMENT_RUN_TYPE;
            }

            SensorValue rawValue = recordValues.get(rawColumn);
            SensorValue refValue = recordValues.get(refColumn);

            if (flushSensors) {
              rawValue.setUserQC(Flag.FLUSHING, "Flushing");
              refValue.setUserQC(Flag.FLUSHING, "Flushing");
              flaggedSensorValues.add(rawValue);
              flaggedSensorValues.add(refValue);
              recordStatus = FLUSHING;
            } else if (rawValue.getUserQCFlag().equals(Flag.FLUSHING)) {
              /*
               * If the Response Time has been changed, some values that were
               * marked FLUSHING should have that flag removed.
               */
              rawValue.removeUserQC(true);
              refValue.removeUserQC(true);
              flaggedSensorValues.add(rawValue);
              flaggedSensorValues.add(refValue);
            }

            if (recordStatus != FLUSHING) {
              measurements
                .add(makeMeasurement(dataset, recordTime, variable, runType));
            }
          }
        }
      }

      DataSetDataDB.storeSensorValues(conn, flaggedSensorValues);
      return measurements;

    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }
  }

  protected String getVariableName() {
    return "CONTROS pCO₂";
  }

  private int getRecordStatus(Map<Long, SensorValue> record, long zeroColumn,
    long flushingColumn) {
    int result;

    /*
     * If either Flush or Zero methods are not exactly one or zero, the record
     * has no status.
     */
    boolean zeroExact = exactFlag(record.get(zeroColumn).getDoubleValue());
    boolean flushExact = exactFlag(record.get(flushingColumn).getDoubleValue());

    if (!zeroExact || !flushExact) {
      result = NO_STATUS;
    } else if (record.get(zeroColumn).getDoubleValue() == 1D) {
      result = ZERO;
    } else if (record.get(flushingColumn).getDoubleValue() == 1D) {
      result = FLUSHING;
    } else {
      result = MEASUREMENT;
    }

    return result;
  }

  private boolean exactFlag(Double flag) {
    return flag == 0D || flag == 1D;
  }

  private Measurement makeMeasurement(DataSet dataset, LocalDateTime time,
    Variable variable, String runType) {

    HashMap<Long, String> runTypes = new HashMap<Long, String>();
    runTypes.put(variable.getId(), runType);
    return new Measurement(dataset.getId(), time, runTypes);
  }
}
