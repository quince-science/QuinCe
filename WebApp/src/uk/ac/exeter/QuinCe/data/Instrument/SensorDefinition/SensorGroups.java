package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Represents the sensors for an instrument in physical groups.
 *
 * <p>
 * The sensors for an instrument may be located in different physical locations,
 * which needs to be taken into account during data reduction; for example, it
 * takes water a significant amount of time to travel from one end of the ship
 * to the other, so if the intake temperature sensor and CO₂ sensor are at
 * opposite ends, the calculation must offset the time taken.
 * </p>
 *
 * <p>
 * Offsets between groups of sensors are identified by a linking sensor in each
 * group. For example, two groups may be linked by Intake Temperature in the
 * first group and Equilibrator Temperature in the second. The data reduction
 * routines will use values from these sensors to calculate the offset to be
 * applied.
 * </p>
 *
 * <p>
 * This structure is a linked list of groups, with the first group representing
 * the earliest point in time of the flow of water/other medium through the
 * instrument. Each entry in the list is a {@link SensorGroup} object, which
 * maintains the links they are adjacent to and which sensors are used for
 * offset calculations.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class SensorGroups implements Iterable<SensorGroup> {

  private static final String DEFAULT_GROUP_NAME = "Default";

  /**
   * The first group in the list.
   */
  private SensorGroup firstGroup;

  /**
   * Create a new set of sensor groups containing a single list.
   */
  public SensorGroups() {
    firstGroup = new SensorGroup(DEFAULT_GROUP_NAME);
  }

  /**
   * Create a new set of sensor groups containing a single list. Add all sensors
   * from the supplied {@link SensorAssignments}.
   *
   * @param sensorAssignments
   *          The sensor assignments.
   */
  public SensorGroups(SensorAssignments sensorAssignments) {
    firstGroup = new SensorGroup(DEFAULT_GROUP_NAME);
    sensorAssignments.getAllAssignments().forEach(firstGroup::addAssignment);
  }

  /**
   * Build a SensorGroups object from Json.
   *
   * @param json
   *          The JSON element.
   * @param sensorAssignments
   *          The instrument's SensorAssignments
   * @throws SensorGroupsException
   *           If the JSON structure is invalid.
   */
  public SensorGroups(JsonElement json, SensorAssignments sensorAssignments)
    throws SensorGroupsException {

    Gson gson = new GsonBuilder().registerTypeAdapter(SensorGroup.class,
      new SensorGroupDeserializer(sensorAssignments)).create();

    JsonArray array = json.getAsJsonArray();

    try {
      Iterator<JsonElement> iterator = array.iterator();

      SensorGroup lastGroup = null;

      while (iterator.hasNext()) {
        JsonObject groupJson = iterator.next().getAsJsonObject();

        SensorGroup group = gson.fromJson(groupJson, SensorGroup.class);

        if (null == firstGroup) {
          firstGroup = group;
        } else {
          insertGroup(lastGroup, group);
        }

        lastGroup = group;
      }
    } catch (JsonParseException e) {
      throw new SensorGroupsException("Invalid Sensor Group JSON", e);
    }

    // Make sure we extracted the correct number of groups
    if (size() != array.size()) {
      throw new SensorGroupsException(
        "Failed to parse JSON - incorrect list size");
    }

    validate(sensorAssignments);
  }

  private void insertGroup(SensorGroup groupBefore, SensorGroup group) {

    if (null == groupBefore) {
      group.setNextGroup(firstGroup);
      firstGroup = group;
    } else {
      SensorGroup groupAfter = groupBefore.getNextGroup();
      groupBefore.setNextGroup(group);
      group.setPrevGroup(groupBefore);
      group.setNextGroup(groupAfter);
      groupAfter.setPrevGroup(group);
    }
  }

  /**
   * Add a sensor to the first group.
   *
   * @param assignment
   *          The sensor.
   * @throws SensorGroupsException
   *           If the assignment has already been added.
   */
  public void addAssignment(SensorAssignment assignment)
    throws SensorGroupsException {
    if (contains(assignment)) {
      throw new SensorGroupsException("Assignment has already been added");
    }
    firstGroup.addAssignment(assignment);
  }

  /**
   * Determine whether or not the specified {@link SensorAssignment} is present
   * anywhere in the groups.
   *
   * @param assignment
   *          The assignment to find.
   */
  public boolean contains(SensorAssignment assignment) {
    boolean found = false;

    SensorGroup nextGroup = firstGroup;
    while (!found && null != nextGroup) {
      if (nextGroup.contains(assignment)) {
        found = true;
      } else {
        nextGroup = nextGroup.getNextGroup();
      }
    }

    return found;
  }

  /**
   * Remove all the specified assignments from the list of groups.
   *
   * <p>
   * It does not matter which group any of the assignments are in. Any
   * assignments that are not in the list at all are ignored silently.
   * </p>
   *
   * @param sensorAssignments
   *          The assignments to be removed.
   */
  public void remove(Collection<SensorAssignment> sensorAssignments) {
    sensorAssignments.forEach(this::removeAssignment);
  }

  /**
   * Remove a specified assignment from the list of groups.
   *
   * <p>
   * It does not matter which group any of the assignment is in. If the
   * assignment is not in any group then no action is taken.
   * </p>
   *
   * @param sensorAssignments
   *          The assignments to be removed.
   */
  public void removeAssignment(SensorAssignment assignment) {
    boolean removed = false;

    SensorGroup nextGroup = firstGroup;
    while (!removed && null != nextGroup) {
      removed = nextGroup.remove(assignment);
    }
  }

  @Override
  public Iterator<SensorGroup> iterator() {
    return new SensorGroupIterator(firstGroup);
  }

  /**
   * Get all the groups as a {@link Stream}.
   * 
   * @return A {@link Stream} of the groups.
   */
  public Stream<SensorGroup> stream() {
    int characteristics = Spliterator.DISTINCT | Spliterator.NONNULL
      | Spliterator.ORDERED;

    Spliterator<SensorGroup> spliterator = Spliterators
      .spliteratorUnknownSize(iterator(), characteristics);
    return StreamSupport.stream(spliterator, false);
  }

  /**
   * Get all the groups as a {@link List}.
   * 
   * @return A {@link List} of the groups.
   */
  public List<SensorGroup> asList() {
    return stream().collect(Collectors.toList());
  }

  /**
   * Get the number of groups.
   *
   * @return The number of groups.
   */
  public long size() {
    return stream().count();
  }

  private void validate(SensorAssignments assignments)
    throws SensorGroupsException {
    // TODO Implement!
  }
}
