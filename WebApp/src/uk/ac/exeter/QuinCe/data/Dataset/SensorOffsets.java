package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroup;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Holds information regarding time offsets between sensor groups.
 */
public class SensorOffsets {

  /**
   * The sensor groups
   */
  private SensorGroups sensorGroups;

  /**
   * The offsets defined for each sensor group pair.
   */
  private LinkedHashMap<SensorGroupPair, TreeSet<SensorOffset>> offsets;

  public SensorOffsets(SensorGroups sensorGroups) {
    this.sensorGroups = sensorGroups;

    offsets = new LinkedHashMap<SensorGroupPair, TreeSet<SensorOffset>>();
    sensorGroups.getGroupPairs()
      .forEach(p -> offsets.put(p, new TreeSet<SensorOffset>()));
  }

  public TreeSet<SensorOffset> getOffsets(SensorGroupPair sensorGroupPair) {
    return offsets.get(sensorGroupPair);
  }

  public void addOffset(SensorGroupPair groupPair, LocalDateTime time,
    long offsetMillis) throws SensorOffsetsException {

    TreeSet<SensorOffset> pairOffsets = offsets.get(groupPair);

    if (containsTime(pairOffsets, time)) {
      throw new SensorOffsetsException(
        "Offset already exists with specified time");
    }

    pairOffsets.add(new SensorOffset(time, offsetMillis));
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

  /**
   * Get the time to use for a {@link SensorAssignment} relative to a base
   * {@link SensorAssignment} taking into account the offsets across sensor
   * groups.
   *
   * @param time
   *          The time to be offset.
   * @param base
   *          The base assignment.
   * @param target
   *          The assignment whose offset time is required.
   * @return The offset time.
   */
  public LocalDateTime getOffsetTime(LocalDateTime time, SensorAssignment base,
    SensorAssignment target) throws SensorGroupsException {

    LocalDateTime result = time;

    SensorGroup baseGroup = sensorGroups.getGroup(base);
    int baseGroupIndex = sensorGroups.getGroupIndex(baseGroup);

    SensorGroup offsetGroup = sensorGroups.getGroup(target);
    int offsetGroupIndex = sensorGroups.getGroupIndex(offsetGroup);

    if (baseGroupIndex != offsetGroupIndex) {
      int startIndex = Math.min(baseGroupIndex, offsetGroupIndex);
      SensorGroup startGroup = startIndex == baseGroupIndex ? baseGroup
        : offsetGroup;

      int endIndex = Math.max(baseGroupIndex, offsetGroupIndex);
      SensorGroup endGroup = endIndex == baseGroupIndex ? baseGroup
        : offsetGroup;

      Iterator<SensorGroupPair> iterator = offsets.keySet().iterator();

      // Find the pair that starts with our first group
      SensorGroupPair currentPair = iterator.next();
      while (!currentPair.first().equals(startGroup)) {
        currentPair = iterator.next();
      }

      Duration totalOffset = Duration.ofMillis(0L);

      // Process pairs until the pair that ends with the end group
      boolean finished = false;
      while (!finished) {

        LocalDateTime timeToOffset = time.plus(totalOffset);

        totalOffset = totalOffset
          .plus(Duration.ofMillis(getOffset(currentPair, timeToOffset)));
        if (currentPair.second().equals(endGroup)) {
          finished = true;
        } else {
          currentPair = iterator.next();
        }
      }

      // If the base group was first, we add the offset. Otherwise we subtract
      // it
      if (baseGroupIndex < offsetGroupIndex) {
        result = time.plus(totalOffset);
      } else {
        result = time.minus(totalOffset);
      }

    }

    return result;
  }

  public LocalDateTime offsetToFirstGroup(LocalDateTime time,
    SensorAssignment baseAssignment) throws SensorGroupsException {
    // Find an assignment from the first group
    SensorAssignment firstGroupAssignment = sensorGroups.first().getMembers()
      .first();

    return getOffsetTime(time, baseAssignment, firstGroupAssignment);
  }

  private static boolean containsTime(TreeSet<SensorOffset> offsets,
    LocalDateTime time) {

    return offsets.stream().filter(o -> o.getTime().equals(time)).findAny()
      .isPresent();
  }

}
