package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;

/**
 * Holds information regarding time offsets between sensor groups.
 *
 * @author Steve Jones
 *
 */
public class SensorOffsets {

  /**
   * The offsets defined for each sensor group pair.
   */
  private LinkedHashMap<SensorGroupPair, TreeSet<SensorOffset>> offsets;

  public SensorOffsets(SensorGroups sensorGroups) {
    offsets = new LinkedHashMap<SensorGroupPair, TreeSet<SensorOffset>>();
    sensorGroups.getGroupPairs()
      .forEach(p -> offsets.put(p, new TreeSet<SensorOffset>()));
  }

  public TreeSet<SensorOffset> getOffsets(SensorGroupPair sensorGroupPair) {
    return offsets.get(sensorGroupPair);
  }

  public void addOffset(SensorGroupPair groupPair, LocalDateTime time,
    long offsetMillis) {

    offsets.get(groupPair).add(new SensorOffset(time, offsetMillis));
  }

  protected LinkedHashMap<SensorGroupPair, TreeSet<SensorOffset>> getMap() {
    return offsets;
  }
}
