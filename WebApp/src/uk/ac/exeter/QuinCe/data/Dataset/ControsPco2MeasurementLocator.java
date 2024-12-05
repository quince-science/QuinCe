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

  private static final int RESPONSE_TIME_MULTIPLIER = 7;

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset, DatasetSensorValues sensorValues)
    throws MeasurementLocatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable variable = sensorConfig.getInstrumentVariable("CONTROS pCO₂");

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

      long defaultFlushingTime = Math
        .round(defaultRunTimeCoefficient.getValue()) * RESPONSE_TIME_MULTIPLIER;

      // Loop through all the rows, examining the zero/flush columns to decide
      // what to do
      List<SensorValue> flaggedSensorValues = new ArrayList<SensorValue>();
      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.getTimes().size());

      int currentStatus = NO_STATUS;
      int lastStatus = NO_STATUS;
      LocalDateTime currentStatusStart = null;

      for (LocalDateTime recordTime : sensorValues.getTimes()) {
        Map<Long, SensorValue> recordValues = sensorValues.get(recordTime);

        // If this record contains the zero status, then it's a main Contros
        // record. Otherwise it's not so we skip it
        if (recordValues.containsKey(zeroColumn)) {

          int recordStatus = getRecordStatus(recordValues, zeroColumn,
            flushingColumn);

          if (recordStatus != currentStatus) {
            lastStatus = currentStatus;
            currentStatus = recordStatus;
            currentStatusStart = recordTime;
          }

          boolean flushSensors = false;
          String runType;

          if (currentStatus == ZERO) {
            if (zeroFlushTime > 0
              && DateTimeUtils.secondsBetween(currentStatusStart,
                recordTime) <= zeroFlushTime) {
              flushSensors = true;
            }

            runType = Measurement.INTERNAL_CALIBRATION_RUN_TYPE;
          } else {
            if (recordStatus == FLUSHING) {
              flushSensors = true;
            } else {
              // If there hasn't been a FLUSHING flag since the last zero,
              // flush for the default period calculated from the response time.
              // Or add the flushing time to the point after the FLUSH mode
              if ((lastStatus == ZERO || lastStatus == FLUSHING)
                && DateTimeUtils.secondsBetween(currentStatusStart,
                  recordTime) <= defaultFlushingTime) {

                flushSensors = true;
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

      DataSetDataDB.storeSensorValues(conn, flaggedSensorValues);
      return measurements;

    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }
  }

  private int getRecordStatus(Map<Long, SensorValue> record, long zeroColumn,
    long flushingColumn) {
    int result;

    if (record.get(zeroColumn).getDoubleValue() == 1D) {
      result = ZERO;
    } else if (record.get(flushingColumn).getDoubleValue() == 1D) {
      result = FLUSHING;
    } else {
      result = MEASUREMENT;
    }

    return result;
  }

  private Measurement makeMeasurement(DataSet dataset, LocalDateTime time,
    Variable variable, String runType) {

    HashMap<Long, String> runTypes = new HashMap<Long, String>();
    runTypes.put(variable.getId(), runType);
    return new Measurement(dataset.getId(), time, runTypes);
  }
}
