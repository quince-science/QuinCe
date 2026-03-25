package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
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
    Instrument instrument, DataSet dataset, DatasetSensorValues allSensorValues)
    throws MeasurementLocatorException {

    try {
      // Get the run types for the dataset, along with the coordinates that they
      // are set at.
      TreeMap<Coordinate, String> runTypes = new TreeMap<Coordinate, String>();

      SensorValuesList runTypeSensorValues = allSensorValues.getRunTypes();

      runTypeSensorValues.getValues()
        .forEach(v -> runTypes.put(v.getCoordinate(), v.getStringValue()));

      Set<Long> measurementColumnIds = new HashSet<Long>();

      for (Variable variable : instrument.getVariables()) {
        if (variable.hasInternalCalibrations()) {
          measurementColumnIds.addAll(instrument.getSensorAssignments()
            .getColumnIds(variable.getCoreSensorType()));
        }
      }

      SensorValuesList sensorValues = allSensorValues
        .getSensorValues(measurementColumnIds, true);

      // Now log all the coordinates as new measurements, with the run type from
      // the same coordinate or immediately before.
      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.valuesSize());

      ArrayList<Coordinate> runTypeCoordinates = null;

      if (null != runTypes) {
        runTypeCoordinates = new ArrayList<Coordinate>(runTypes.keySet());
      }

      int currentRunTypeCoordinate = 0;

      for (SensorValuesListValue value : sensorValues.getValues()) {
        Coordinate measurementCoordinate = value.getCoordinate();

        // Get the run type for this measurement
        String runType = null;
        if (null != runTypes) {

          // Find the run type immediately before or at the same coordinate as
          // the measurement
          if (runTypeCoordinates.get(currentRunTypeCoordinate)
            .isAfter(measurementCoordinate)) {
            // There is no run type for this measurement. This isn't allowed!
            throw new MeasurementLocatorException(
              "No run type available in Dataset " + dataset.getId()
                + " at coordinate " + measurementCoordinate.toString());
          } else {
            while (currentRunTypeCoordinate < runTypeCoordinates.size() - 1
              && runTypeCoordinates.get(currentRunTypeCoordinate)
                .isBefore(measurementCoordinate)) {
              currentRunTypeCoordinate++;
            }

            runType = runTypes
              .get(runTypeCoordinates.get(currentRunTypeCoordinate));
          }
        }

        Map<Long, String> runTypeMap = new HashMap<Long, String>();
        runTypeMap.put(Measurement.RUN_TYPE_DEFINES_VARIABLE, runType);

        measurements.add(new Measurement(dataset.getId(),
          dataset.getFlagScheme(), measurementCoordinate, runTypeMap));
      }

      return measurements;

    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }
  }
}
