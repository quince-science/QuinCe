package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ControsPco2MeasurementLocator implements MeasurementLocator {

  private static final int NO_STATUS = -1;

  private static final int ZERO = 0;

  private static final int FLUSHING = 1;

  private static final int MEASUREMENT = 2;

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable variable = sensorConfig.getInstrumentVariable("CONTROS pCO₂");

      float zeroFlushTime = Float.parseFloat(dataset.getAllProperties()
        .get(variable.getName()).getProperty("zero_flush"));

      SensorType zeroType = sensorConfig
        .getSensorType("Contros pCO₂ Zero Mode");
      SensorType flushingType = sensorConfig
        .getSensorType("Contros pCO₂ Flush Mode");
      SensorType rawSensorType = sensorConfig
        .getSensorType("Contros pCO₂ Raw Detector Signal");
      SensorType refSensorType = sensorConfig
        .getSensorType("Contros pCO₂ Reference Signal");

      // Assume one column of each type
      long zeroColumn = instrument.getSensorAssignments().getColumnIds(zeroType)
        .get(0);
      long flushingColumn = instrument.getSensorAssignments()
        .getColumnIds(flushingType).get(0);
      long rawColumn = instrument.getSensorAssignments()
        .getColumnIds(rawSensorType).get(0);
      long refColumn = instrument.getSensorAssignments()
        .getColumnIds(refSensorType).get(0);

      DatasetSensorValues sensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataset.getId(), false);

      // Loop through all the rows, examining the zero/flush columns to decide
      // what to do
      List<SensorValue> flaggedSensorValues = new ArrayList<SensorValue>();
      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.getTimes().size());

      int currentStatus = NO_STATUS;
      LocalDateTime currentStatusStart = null;

      for (LocalDateTime recordTime : sensorValues.getTimes()) {
        Map<Long, SensorValue> recordValues = sensorValues.get(recordTime);

        int recordStatus = getRecordStatus(recordValues, zeroColumn,
          flushingColumn);

        if (recordStatus != currentStatus) {
          currentStatus = recordStatus;
          currentStatusStart = recordTime;
        }

        boolean flushSensors = false;
        String runType;

        if (currentStatus == ZERO) {
          if (DateTimeUtils.secondsBetween(currentStatusStart,
            recordTime) <= zeroFlushTime) {
            flushSensors = true;
          }

          runType = Measurement.INTERNAL_CALIBRATION_RUN_TYPE;
        } else {
          if (recordStatus == FLUSHING) {
            flushSensors = true;
          }

          runType = Measurement.MEASUREMENT_RUN_TYPE;
        }

        if (flushSensors) {
          SensorValue rawValue = recordValues.get(rawColumn);
          SensorValue refValue = recordValues.get(refColumn);
          rawValue.setUserQC(Flag.FLUSHING, "Flushing");
          refValue.setUserQC(Flag.FLUSHING, "Flushing");
          flaggedSensorValues.add(rawValue);
          flaggedSensorValues.add(refValue);
        }

        measurements
          .add(makeMeasurement(dataset, recordTime, variable, runType));
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
