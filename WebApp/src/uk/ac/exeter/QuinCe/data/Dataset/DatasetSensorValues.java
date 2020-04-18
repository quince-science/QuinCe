package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Data structure holding all the {@code SensorValue}s for a dataset, accessible
 * by different lookups.
 *
 * @author Steve Jones
 *
 */
public class DatasetSensorValues {

  private Map<Long, SensorValue> valuesById;

  private Map<Long, SearchableSensorValuesList> valuesByColumn;

  private Map<SensorType, TreeSet<SensorValue>> valuesBySensorType;

  private TreeMap<LocalDateTime, Map<Long, SensorValue>> valuesByDateAndColumn;

  private final Instrument instrument;

  public DatasetSensorValues(Instrument instrument) {
    valuesById = new HashMap<Long, SensorValue>();
    valuesByColumn = new HashMap<Long, SearchableSensorValuesList>();
    valuesBySensorType = new HashMap<SensorType, TreeSet<SensorValue>>();
    valuesByDateAndColumn = new TreeMap<LocalDateTime, Map<Long, SensorValue>>();

    this.instrument = instrument;
  }

  public void add(SensorValue sensorValue) throws RecordNotFoundException {

    SensorType sensorType = instrument.getSensorAssignments()
      .getSensorTypeForDBColumn(sensorValue.getColumnId());

    addById(sensorValue);
    addByColumn(sensorValue);
    addBySensorType(sensorValue, sensorType);
    addByDateAndColumn(sensorValue);
  }

  public Set<Long> getColumnIds() {
    return valuesByColumn.keySet();
  }

  public SearchableSensorValuesList getColumnValues(long columnId) {
    return valuesByColumn.get(columnId);
  }

  public SensorValue getById(long id) {
    return valuesById.get(id);
  }

  public TreeSet<SensorValue> getBySensorType(SensorType sensorType) {
    return valuesBySensorType.get(sensorType);
  }

  public Collection<SensorValue> getAll() {
    return valuesById.values();
  }

  private void addById(SensorValue sensorValue) {
    valuesById.put(sensorValue.getId(), sensorValue);
  }

  private void addByColumn(SensorValue sensorValue) {
    long columnId = sensorValue.getColumnId();
    if (!valuesByColumn.containsKey(columnId)) {
      valuesByColumn.put(columnId, new SearchableSensorValuesList());
    }

    valuesByColumn.get(columnId).add(sensorValue);
  }

  private void addBySensorType(SensorValue sensorValue, SensorType sensorType) {
    if (!valuesBySensorType.containsKey(sensorType)) {
      valuesBySensorType.put(sensorType, new TreeSet<SensorValue>());
    }

    valuesBySensorType.get(sensorType).add(sensorValue);
  }

  private void addByDateAndColumn(SensorValue sensorValue)
    throws RecordNotFoundException {

    LocalDateTime time = sensorValue.getTime();

    if (!valuesByDateAndColumn.containsKey(time)) {
      valuesByDateAndColumn.put(time, new HashMap<Long, SensorValue>());
    }

    valuesByDateAndColumn.get(time).put(sensorValue.getColumnId(), sensorValue);
  }

  /**
   * Remove all stateful searches with the specified prefix
   *
   * @param searchIdPrefix
   */
  public void destroySearchesWithPrefix(String searchIdPrefix) {

    for (SearchableSensorValuesList list : valuesByColumn.values()) {
      list.destroySearchesWithPrefix(searchIdPrefix);
    }
  }

  public boolean containsSearchWithPrefix(String searchIdPrefix) {
    boolean result = false;

    for (SearchableSensorValuesList list : valuesByColumn.values()) {
      if (list.hasSearchWithPrefix(searchIdPrefix)) {
        result = true;
        break;
      }
    }

    return result;
  }

  public List<LocalDateTime> getTimes() {
    return new ArrayList<LocalDateTime>(valuesByDateAndColumn.keySet());
  }

  public Map<Long, SensorValue> get(LocalDateTime time) {

    return valuesByDateAndColumn.get(time);
  }
}
