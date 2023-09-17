package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class RunTypeMeasurementLocator extends MeasurementLocator {

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset) throws MeasurementLocatorException {

    try {
      // Get the run types for the dataset, along with the times that they are
      // set
      TreeMap<LocalDateTime, String> runTypes = new TreeMap<LocalDateTime, String>();

      DatasetSensorValues allSensorValues = DataSetDataDB.getSensorValues(conn,
        instrument, dataset.getId(), false, true);

      SensorValuesList runTypeSensorValues = allSensorValues.getRunTypes();

      runTypeSensorValues.getValues()
        .forEach(v -> runTypes.put(v.getTime(), v.getStringValue()));

      Set<Long> measurementColumnIds = new HashSet<Long>();

      for (Variable variable : instrument.getVariables()) {
        if (variable.hasInternalCalibrations()) {
          measurementColumnIds.addAll(instrument.getSensorAssignments()
            .getColumnIds(variable.getCoreSensorType()));
        }
      }

      SensorValuesList sensorValues = allSensorValues
        .getSensorValues(measurementColumnIds);

      // Now log all the times as new measurements, with the run type from the
      // same time or immediately before.
      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.valuesSize());

      ArrayList<LocalDateTime> runTypeTimes = null;

      if (null != runTypes) {
        runTypeTimes = new ArrayList<LocalDateTime>(runTypes.keySet());
      }

      int currentRunTypeTime = 0;

      for (SensorValuesListValue value : sensorValues.getValues()) {
        LocalDateTime measurementTime = value.getTime();

        // Get the run type for this measurement
        String runType = null;
        if (null != runTypes) {

          // Find the run type immediately before or at the same time as the
          // measurement
          if (runTypeTimes.get(currentRunTypeTime).isAfter(measurementTime)) {
            // There is no run type for this measurement. This isn't allowed!
            throw new MeasurementLocatorException(
              "No run type available in Dataset " + dataset.getId()
                + " at time " + measurementTime.toString());
          } else {
            while (currentRunTypeTime < runTypeTimes.size() - 1 && runTypeTimes
              .get(currentRunTypeTime).isBefore(measurementTime)) {
              currentRunTypeTime++;
            }

            runType = runTypes.get(runTypeTimes.get(currentRunTypeTime));
          }
        }

        Map<Long, String> runTypeMap = new HashMap<Long, String>();
        runTypeMap.put(Measurement.RUN_TYPE_DEFINES_VARIABLE, runType);

        measurements
          .add(new Measurement(dataset.getId(), measurementTime, runTypeMap));
      }

      return measurements;

    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }
  }
}
