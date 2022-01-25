package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

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

  public void deleteOffset(SensorGroupPair groupPair, LocalDateTime time) {
    offsets.get(groupPair).removeIf(o -> o.getTime().equals(time));
  }

  protected LinkedHashMap<SensorGroupPair, TreeSet<SensorOffset>> getMap() {
    return offsets;
  }

  public List<SensorValue> applyOffsets(SensorGroupPair groupPair,
    List<SensorValue> sensorValues) {

    List<SensorValue> result = new ArrayList<SensorValue>(sensorValues.size());

    for (SensorValue sensorValue : sensorValues) {
      if (sensorValue.getUserQCFlag().isGood()) {
        long offset = getOffset(groupPair, sensorValue.getTime());
        LocalDateTime newTime = sensorValue.getTime().minus(offset,
          ChronoUnit.MILLIS);
        result.add(new SensorValue(sensorValue, newTime));
      }
    }

    return result;
  }

  private long getOffset(SensorGroupPair group, LocalDateTime time) {

    long result;

    TreeSet<SensorOffset> groupOffsets = offsets.get(group);

    if (groupOffsets.size() == 0) {
      result = 0L;
    } else {
      // Dummy SensorOffset object to use for comparisons
      SensorOffset testOffset = new SensorOffset(time, Long.MIN_VALUE);

      SensorOffset before = groupOffsets.floor(testOffset);
      SensorOffset after = groupOffsets.ceiling(testOffset);

      // See if there's an offset of the exact same time
      if (null != after && after.getTime().equals(time)) {
        result = after.getOffset();
      } else if (null == before) {
        result = after.getOffset();
      } else if (null == after) {
        result = before.getOffset();
      } else {

        long beforeMillis = DateTimeUtils.dateToLong(before.getTime());
        long afterMillis = DateTimeUtils.dateToLong(after.getTime());

        double timeDifference = afterMillis - beforeMillis;
        double offsetDifference = after.getOffset() - before.getOffset();

        double offsetPerMillis = offsetDifference / timeDifference;

        long timePos = DateTimeUtils.dateToLong(time) - beforeMillis;

        result = (long) Math
          .floor(before.getOffset() + (timePos * offsetPerMillis));
      }
    }

    return result;
  }
}
