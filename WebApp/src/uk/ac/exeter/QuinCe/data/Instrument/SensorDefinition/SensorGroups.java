package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
 * <p>
 * Group names are case-insensitive and must be unique.
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

      if (null != groupAfter) {
        groupAfter.setPrevGroup(group);
      }
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

  /**
   * Check that this set of sensor groups is complete and ready for use.
   * 
   * <p>
   * Ensures that all groups have members and that linking sensors have been
   * defined where needed.
   * </p>
   * 
   * <p>
   * Note that this does not check the validity of the data structure (e.g. that
   * group links are valid).
   * </p>
   * 
   * @return
   */
  public boolean isComplete() {
    boolean complete = true;

    if (null == firstGroup) {
      complete = false;
    } else {

      SensorGroup group = firstGroup;

      while (complete && null != group) {
        if (group.size() == 0) {
          complete = false;
        } else if (group.hasPrev() && null == group.getPrevLink()) {
          complete = false;
        } else if (group.hasNext() && null == group.getNextLink()) {
          complete = false;
        } else {
          group = group.getNextGroup();
        }
      }

    }

    return complete;
  }

  /**
   * Get the names of all the groups in order.
   *
   * @return The group names.
   */
  public List<String> getGroupNames() {
    return stream().map(g -> g.getName()).collect(Collectors.toList());
  }

  public void renameGroup(String from, String to) throws SensorGroupsException {
    if (!from.equals(to)) {
      Optional<SensorGroup> group = getGroup(from);
      if (group.isEmpty()) {
        throw new SensorGroupsException("Cannot find group '" + from + "'");
      }

      if (groupExists(to)) {
        throw new SensorGroupsException(
          "There is already a group named '" + to + "'");
      }

      group.get().setName(to);
    }
  }

  /**
   * Get the named group.
   *
   * @param name
   *          The group name.
   * @return The group, or
   */
  public Optional<SensorGroup> getGroup(String name) {
    return stream().filter(g -> g.getName().equalsIgnoreCase(name.trim()))
      .findAny();
  }

  /**
   * Determine whether or not a group with the specified name exists.
   *
   * @param name
   *          The group name.
   * @return {@code true} if the group exists.
   */
  public boolean groupExists(String name) {
    return getGroup(name).isPresent();
  }

  /**
   * Add a new group after the specified group, or at the beginning if
   * {@code after} is {@code null}.
   *
   * @param name
   *          The new group's name
   * @param after
   *          The name of the group that will be before this group.
   * @throws SensorGroupsException
   */
  public void addGroup(String name, String after) throws SensorGroupsException {

    name = name.trim();
    after = null == after ? null : after.trim();

    if (groupExists(name)) {
      throw new SensorGroupsException("Group '" + name + "' already exists");
    }

    if (null == after) {
      SensorGroup newGroup = new SensorGroup(name);
      newGroup.setNextGroup(firstGroup);
      firstGroup.setPrevGroup(newGroup);
      firstGroup = newGroup;
    } else {
      Optional<SensorGroup> priorGroup = getGroup(after);
      if (priorGroup.isEmpty()) {
        throw new SensorGroupsException(
          "After group '" + after + "' does not exist");
      } else {
        SensorGroup newGroup = new SensorGroup(name);

        // New group links back to prior group
        newGroup.setPrevGroup(priorGroup.get());

        // New group links forward to prior group's old next
        newGroup.setNextGroup(priorGroup.get().getNextGroup());

        // Prior group's old next links back to new group
        if (null != newGroup.getNextGroup()) {
          newGroup.getNextGroup().setPrevGroup(newGroup);
        }

        // Prior group links forward to new group
        priorGroup.get().setNextGroup(newGroup);
      }
    }
  }

  /**
   * Delete the specified sensor group. All assigned sensors are moved to a
   * neighbouring group.
   *
   * @param group
   *          The group to be deleted.
   * @throws SensorGroupsException
   */
  public void deleteGroup(String group) throws SensorGroupsException {
    if (size() == 1) {
      throw new SensorGroupsException("Cannot delete the only sensor group");
    }

    Optional<SensorGroup> findGroup = getGroup(group);
    if (findGroup.isEmpty()) {
      throw new SensorGroupsException("Group '" + group + "' does not exist");
    } else {

      SensorGroup deleteGroup = findGroup.get();

      SensorGroup prevGroup = deleteGroup.getPrevGroup();
      SensorAssignment prevLink = deleteGroup.getPrevLink();
      SensorGroup nextGroup = deleteGroup.getNextGroup();
      SensorAssignment nextLink = deleteGroup.getNextLink();

      // Move all the group's members to a neighbouring group
      SensorGroup neighbourGroup = null == nextGroup ? prevGroup : nextGroup;
      neighbourGroup.addAssignments(deleteGroup.getMembers());

      if (null != prevGroup) {
        prevGroup.setNextGroup(nextGroup);
        prevGroup.setNextLink(nextLink);
      }

      if (null != nextGroup) {
        nextGroup.setPrevGroup(prevGroup);
        nextGroup.setPrevLink(prevLink);
      }

      if (firstGroup.equals(deleteGroup)) {
        firstGroup = nextGroup;
      }
    }
  }

  /**
   * Move a sensor from its current group to another group.
   *
   * @param sensorName
   *          The sensor to be moved.
   * @param groupName
   *          The sensor's new group.
   * @throws SensorGroupsException
   */
  public void moveSensor(String sensorName, String groupName)
    throws SensorGroupsException {

    Optional<SensorGroup> sourceGroup = getSensorGroup(sensorName);
    if (sourceGroup.isEmpty()) {
      throw new SensorGroupsException("Sensor '" + sensorName + "' not found");
    }

    Optional<SensorAssignment> assignment = sourceGroup.get()
      .getAssignment(sensorName);
    if (assignment.isEmpty()) {
      // This should be caught above. But just in case...
      throw new SensorGroupsException("Sensor '" + sensorName + "' not found");
    }

    // If a sensor is moving to its own group, do nothing
    if (!sourceGroup.get().getName().equalsIgnoreCase(groupName)) {
      Optional<SensorGroup> destinationGroup = getGroup(groupName);
      if (destinationGroup.isEmpty()) {
        throw new SensorGroupsException("Group '" + groupName + "' not found");
      }

      sourceGroup.get().remove(assignment.get());
      destinationGroup.get().addAssignment(assignment.get());
    }
  }

  private Optional<SensorGroup> getSensorGroup(String sensorName) {
    return stream().filter(g -> g.contains(sensorName)).findAny();
  }

  public String getLinksJson() {
    JsonObject json = new JsonObject();

    stream().forEach(g -> {
      JsonArray array = new JsonArray();
      array.add(g.getPreviousLinkName());
      array.add(g.getNextLinkName());
      json.add(g.getName(), array);
    });

    return new Gson().toJson(json);
  }
}
