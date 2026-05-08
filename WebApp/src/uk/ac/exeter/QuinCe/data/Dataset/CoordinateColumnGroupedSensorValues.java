package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * A data structure to hold all the sensor values for a dataset, grouped by
 * {@link Coordinate} and sensor type. Primarily used for determining which
 * sensor values to use during data reduction of a data set.
 *
 * The structure is a three level tree of Coordinate -&gt; Sensor Type -&gt;
 * Values, thus holding all values for all variables (grouped by sensor type) at
 * each {@link Coordinate}.
 *
 * The time stamps are stored in order; sensor types and values can be in any
 * order within a time stamp.
 */
@SuppressWarnings("serial")
public class CoordinateColumnGroupedSensorValues
  extends TreeMap<Coordinate, Map<SensorType, List<SensorValue>>> {

  /**
   * The instrument that these values belong to
   */
  private Instrument instrument;

  public CoordinateColumnGroupedSensorValues(Instrument instrument) {
    super();
    this.instrument = instrument;
  }

  /**
   * Add a SensorValue to the structure in the correct place according to its
   * timestamp and sensor type
   *
   * @param value
   *          The value to add
   * @throws RecordNotFoundException
   *           If the values do not match the instrument's sensor assignments
   */
  public void add(SensorValue value) throws RecordNotFoundException {

    SensorAssignments sensorAssignments = instrument.getSensorAssignments();

    if (!containsKey(value.getCoordinate())) {
      put(value.getCoordinate(), new HashMap<SensorType, List<SensorValue>>());
    }

    Map<SensorType, List<SensorValue>> sensorTypeMap = get(
      value.getCoordinate());
    SensorType sensorType = sensorAssignments
      .getSensorTypeForDBColumn(value.getColumnId());
    if (!sensorTypeMap.containsKey(sensorType)) {
      sensorTypeMap.put(sensorType, new ArrayList<SensorValue>());
    }

    List<SensorValue> valueList = sensorTypeMap.get(sensorType);
    valueList.add(value);
  }

  /**
   * Add all the sensors in a collection to the data structure
   *
   * @param values
   *          The values to be added
   * @throws RecordNotFoundException
   *           If the values do not match the instrument's sensor assignments
   */
  public void addAll(Collection<SensorValue> values)
    throws RecordNotFoundException {
    for (SensorValue value : values) {
      add(value);
    }
  }

  /**
   * Get the first {@link Coordinate} in this set of sensor values.
   *
   * @return The first coordinate.
   */
  public Coordinate getFirstCoordiante() {
    return keySet().iterator().next();
  }
}
