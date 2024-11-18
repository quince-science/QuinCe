package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class SimpleMeasurementLocator extends MeasurementLocator {

  private final Variable variable;

  public SimpleMeasurementLocator(Variable variable) {
    this.variable = variable;
  }

  @Override
  public List<Measurement> locateMeasurements(Connection conn,
    Instrument instrument, DataSet dataset, DatasetSensorValues allSensorValues)
    throws MeasurementLocatorException {

    try {
      Set<Long> measurementColumnIds = new HashSet<Long>();
      measurementColumnIds.addAll(instrument.getSensorAssignments()
        .getColumnIds(variable.getCoreSensorType()));

      // Get all the sensor values for the identified columns
      SensorValuesList sensorValues = allSensorValues
        .getSensorValues(measurementColumnIds);

      // Since we're only dealing with one variable, we don't want multiple
      // measurements for the same time. Get all the unique times and make
      // measurements from them. Ignore any SensorValues with NaN values.

      HashSet<LocalDateTime> seenTimes = new HashSet<LocalDateTime>(
        sensorValues.valuesSize());

      List<Measurement> measurements = new ArrayList<Measurement>(
        sensorValues.valuesSize());

      sensorValues.getValues().stream()
        .filter(v -> !seenTimes.contains(v.getTime())).forEach(v -> {
          // Add the new time to the set of seen times
          seenTimes.add(v.getTime());

          // Make the measurement
          measurements.add(
            new Measurement(dataset.getId(), v.getTime(), makeRunTypeMap()));
        });

      return measurements;

    } catch (Exception e) {
      throw new MeasurementLocatorException(e);
    }
  }

  private Map<Long, String> makeRunTypeMap() {
    // Set up the Run Type map we'll use for all measurements
    Map<Long, String> runTypeMap = new HashMap<Long, String>();
    runTypeMap.put(variable.getId(), Measurement.MEASUREMENT_RUN_TYPE);
    return runTypeMap;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((variable == null) ? 0 : variable.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SimpleMeasurementLocator other = (SimpleMeasurementLocator) obj;
    if (variable == null) {
      if (other.variable != null)
        return false;
    } else if (!variable.equals(other.variable))
      return false;
    return true;
  }
}
