package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
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
 * A data structure to hold all the sensor values for a dataset,
 * grouped by date and sensor type. Primarily used for determining
 * which sensor values to use during data reduction of a data set.
 *
 * The structure is a three level tree of
 * Time -> Sensor Type -> Values,
 * thus holding all values for all variables (grouped by sensor
 * type) at each time stamp.
 *
 * The time stamps are stored in order; sensor types and values can be in any order
 * within a time stamp.
 *
 * Values are in date order.
 *
 * @author Steve Jones
 *
 */
public class DateColumnGroupedSensorValues
extends TreeMap<LocalDateTime, Map<SensorType, List<SensorValue>>> {

  private Instrument instrument;

  public DateColumnGroupedSensorValues(Instrument instrument) {
    super();
    this.instrument = instrument;
  }

  /**
   * Add a SensorValue to the structure in the correct place according to its
   * timestamp and sensor type
   * @param value The value to add
   * @throws RecordNotFoundException If the values do not match the instrument's
   *                                 sensor assignments
   */
  public void add(SensorValue value) throws RecordNotFoundException {

    SensorAssignments sensorAssignments = instrument.getSensorAssignments();

    if (!containsKey(value.getTime())) {
      put(value.getTime(), new HashMap<SensorType, List<SensorValue>>());
    }

    Map<SensorType, List<SensorValue>> sensorTypeMap = get(value.getTime());
    SensorType sensorType = sensorAssignments.getSensorTypeForDBColumn(value.getColumnId());
    if (!sensorTypeMap.containsKey(sensorType)) {
      sensorTypeMap.put(sensorType, new ArrayList<SensorValue>());
    }

    List<SensorValue> valueList = sensorTypeMap.get(sensorType);
    valueList.add(value);
  }

  /**
   * Add all the sensors in a collection to the data structure
   * @param values The values to be added
   * @throws RecordNotFoundException If the values do not match the instrument's
   *                                 sensor assignments
   */
  public void addAll(Collection<SensorValue> values) throws RecordNotFoundException {
    for (SensorValue value : values) {
      add(value);
    }
  }
}
