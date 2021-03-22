package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ControsPco2MeasurementLocator implements MeasurementLocator {

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException {

    try {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance()
        .getSensorsConfiguration();

      Variable variable = sensorConfig.getInstrumentVariable("CONTROS pCO₂");

      SensorType zeroType = sensorConfig
        .getSensorType("Contros pCO₂ Zero Mode");
      SensorType flushingType = sensorConfig
        .getSensorType("Contros pCO₂ Flush Mode");
      SensorType rawSensorType = sensorConfig
        .getSensorType("Contros pCO₂ Raw Detector Signal");
      SensorType refSensorType = sensorConfig
        .getSensorType("Contros pCO₂ Reference Signal");

      HashSet<LocalDateTime> zeroTimes = DataSetDataDB
        .getFilteredSensorValueTimes(conn, instrument, dataset, zeroType, "1");

      HashSet<LocalDateTime> flushingTimes = DataSetDataDB
        .getFilteredSensorValueTimes(conn, instrument, dataset, flushingType,
          "1");

      List<Long> rawColumns = instrument.getSensorAssignments()
        .getColumnIds(rawSensorType);
      List<Long> refColumns = instrument.getSensorAssignments()
        .getColumnIds(refSensorType);

      List<SensorValue> rawSensorValues = DataSetDataDB
        .getSensorValuesForColumns(conn, dataset.getId(), rawColumns);
      List<SensorValue> refSensorValues = DataSetDataDB
        .getSensorValuesForColumns(conn, dataset.getId(), refColumns);

      // I believe that there will always be an equal number of raw and ref
      // values. If this assumption
      // ever breaks, we'll get an error and know that we have to deal with it.
      if (rawSensorValues.size() != refSensorValues.size()) {
        throw new MeasurementLocatorException(
          "Raw and Ref sensor values lists are different sizes");
      }

      List<SensorValue> flaggedSensorValues = new ArrayList<SensorValue>();
      List<Measurement> measurements = new ArrayList<Measurement>(
        rawSensorValues.size());

      for (int i = 0; i < rawSensorValues.size(); i++) {
        SensorValue rawValue = rawSensorValues.get(i);

        if (zeroTimes.contains(rawValue.getTime())) {
          HashMap<Long, String> runTypes = new HashMap<Long, String>();
          runTypes.put(variable.getId(),
            Measurement.INTERNAL_CALIBRATION_RUN_TYPE);

          measurements.add(
            new Measurement(dataset.getId(), rawValue.getTime(), runTypes));
        } else if (flushingTimes.contains(rawValue.getTime())) {
          rawValue.setUserQC(Flag.FLUSHING, "Flushing");
          flaggedSensorValues.add(rawValue);

          SensorValue refValue = refSensorValues.get(i);
          refValue.setUserQC(Flag.FLUSHING, "Flushing");
          flaggedSensorValues.add(refValue);
        } else {
          HashMap<Long, String> runTypes = new HashMap<Long, String>();
          runTypes.put(variable.getId(), Measurement.MEASUREMENT_RUN_TYPE);

          measurements.add(
            new Measurement(dataset.getId(), rawValue.getTime(), runTypes));
        }
      }

      DataSetDataDB.storeSensorValues(conn, flaggedSensorValues);
      return measurements;

    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }
  }
}
