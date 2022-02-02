package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Defines a group of sensors that are physically located in the same place for
 * the purposes of data reduction.
 *
 * @see SensorGroups
 * @author Steve Jones.
 *
 */
public class SensorGroup {

  public static final int PREVIOUS = 0;

  public static final int NEXT = 1;

  /**
   * The name of the group.
   */
  private String name;

  /**
   * The sensors in this group.
   */
  private TreeSet<SensorAssignment> members;

  /**
   * The group that comes before this group.
   */
  private SensorGroup prevGroup = null;

  /**
   * The sensor used as the link to the previous group.
   */
  private SensorAssignment prevGroupLink = null;

  /**
   * The group that comes after this group.
   */
  private SensorGroup nextGroup = null;

  /**
   * The sensor used as the link to the next group;
   */
  private SensorAssignment nextGroupLink = null;

  /**
   * Construct an empty group that is not linked to anything.
   *
   * @param name
   *          The group name.
   */
  protected SensorGroup(String name) {
    this.name = name;
    this.members = new TreeSet<SensorAssignment>();
  }

  /**
   * Add a {@link SensorAssignment} to the group.
   *
   * @param assignment
   *          The assignment to add.
   */
  protected void addAssignment(SensorAssignment assignment) {
    members.add(assignment);
  }

  /**
   * Add a {@link Collection} of {@link SensorAssignment}s to the group.
   *
   * @param assignments
   *          The assignments to add.
   */
  protected void addAssignments(Collection<SensorAssignment> assignments) {
    members.addAll(assignments);
  }

  /**
   * Determine whether or not the specified {@link SensorAssignment} is in this
   * group.
   *
   * @param assignment
   *          The assignment to be found.
   * @return {@code true} if the assignment is in this group; {@code false} if
   *         it is not.
   */
  protected boolean contains(SensorAssignment assignment) {
    return members.contains(assignment);
  }

  /**
   * Determine whether or not the specified sensor name is in this group.
   *
   * @param assignment
   *          The sensor to be found.
   * @return {@code true} if the assignment is in this group; {@code false} if
   *         it is not.
   */
  protected boolean contains(String sensorName) {
    return members.stream()
      .filter(m -> m.getSensorName().equalsIgnoreCase(sensorName)).findAny()
      .isPresent();
  }

  /**
   * Get the next group in the list.
   *
   * @return The next group.
   */
  protected SensorGroup getNextGroup() {
    return nextGroup;
  }

  /**
   * Get the previous group in the list.
   *
   * @return The previous group.
   */
  protected SensorGroup getPrevGroup() {
    return prevGroup;
  }

  /**
   * Remove the specified sensor assignment from this group if it is a member.
   *
   * @param assignment
   *          The assignment to remove.
   * @return {@code true} if the assignment was a member of this group and it
   *         was removed.
   */
  protected boolean remove(SensorAssignment assignment) {
    boolean result = members.remove(assignment);

    if (result) {
      if (null != nextGroupLink && nextGroupLink.equals(assignment)) {
        nextGroupLink = null;
      }
      if (null != prevGroupLink && prevGroupLink.equals(assignment)) {
        prevGroupLink = null;
      }
    }

    return result;
  }

  /**
   * Get the name of this group.
   *
   * @return The group name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the names of this group's members.
   *
   * @return The member names.
   */
  public TreeSet<SensorAssignment> getMembers() {
    return members;
  }

  /**
   * Determines whether or not this group has a previous group.
   *
   * @return {@code true} if the group has a previous group.
   */
  public boolean hasPrev() {
    return null != prevGroup;
  }

  /**
   * Determines whether or not this group has a following group.
   *
   * @return {@code true} if the group has a following group.
   */
  public boolean hasNext() {
    return null != nextGroup;
  }

  /**
   * Get the {@link SensorAssignment} used as the link to the previous group.
   *
   * <p>
   * Return {@code null} if there is no previous group.
   * </p>
   *
   * @return The link to the previous group.
   */
  protected SensorAssignment getPrevLink() {
    return prevGroupLink;
  }

  /**
   * Get the {@link SensorAssignment} used as the link to the next group.
   *
   * <p>
   * Return {@code null} if there is no next group.
   * </p>
   *
   * @return The link to the next group.
   */
  protected SensorAssignment getNextLink() {
    return nextGroupLink;
  }

  /**
   * Specify the previous group in the list.
   *
   * @param group
   *          The previous group.
   */
  protected void setPrevGroup(SensorGroup group) {
    this.prevGroup = group;
  }

  /**
   * Specify the next group in the list.
   *
   * @param group
   *          The next group.
   */
  protected void setNextGroup(SensorGroup group) {
    this.nextGroup = group;
  }

  /**
   * Specify the {@link SensorAssignment} used as the link to the previous
   * group.
   *
   * @param assignment
   *          The linking assignment.
   * @throws SensorGroupsException
   */
  protected void setPrevLink(SensorAssignment assignment)
    throws SensorGroupsException {

    if (null == assignment) {
      this.prevGroupLink = null;
    } else {
      if (!members.contains(assignment)) {
        throw new SensorGroupsException("Assignment '"
          + assignment.getSensorName() + "' not a member of this group");
      }

      this.prevGroupLink = assignment;
    }
  }

  /**
   * Specify the {@link SensorAssignment} used as the link to the next group.
   *
   * @param group
   *          The linking assignment.
   * @throws SensorGroupsException
   */
  protected void setNextLink(SensorAssignment assignment)
    throws SensorGroupsException {

    if (null == assignment) {
      this.nextGroupLink = null;
    } else {
      if (!members.contains(assignment)) {
        throw new SensorGroupsException("Assignment '"
          + assignment.getSensorName() + "' not a member of this group");
      }

      this.nextGroupLink = assignment;

    }
  }

  /**
   * Get the name of the sensor used as the link to the next group.
   *
   * <p>
   * Returns {@code null} if there is no next group or no link is defined.
   * </p>
   *
   * @return The name of the link sensor.
   */
  public String getNextLinkName() {
    return null == nextGroupLink ? null : nextGroupLink.getSensorName();
  }

  /**
   * Get the name of the sensor used as the link to the previous group.
   *
   * <p>
   * Returns {@code null} if there is no previous group or no link is defined.
   * </p>
   *
   * @return The name of the link sensor.
   */
  public String getPrevLinkName() {
    return null == prevGroupLink ? null : prevGroupLink.getSensorName();
  }

  /**
   * Set the sensor used as the link to the next group using its name.
   *
   * @param sensorName
   *          The sensor name.
   * @throws SensorGroupsException
   *           If the sensor is not a member of the group.
   */
  private void setNextLinkName(String sensorName) throws SensorGroupsException {
    if (sensorName.trim().length() > 0) {
      nextGroupLink = get(sensorName);
    }
  }

  /**
   * Set the sensor used as the link to the previous group using its name.
   *
   * @param sensorName
   *          The sensor name.
   * @throws SensorGroupsException
   *           If the sensor is not a member of the group.
   */
  private void setPrevLinkName(String sensorName) throws SensorGroupsException {
    if (sensorName.trim().length() > 0) {
      prevGroupLink = get(sensorName);
    }
  }

  /**
   * Get a {@link SensorAssignment} from the group using its sensor name.
   *
   * @param senosrName
   *          The sensor name.
   * @return The {@link SensorAssignment}.
   * @throws SensorGroupsException
   *           If the sensor is not a member of the group.
   */
  private SensorAssignment get(String sensorName) throws SensorGroupsException {

    SensorAssignment result = null;

    if (sensorName.trim().length() > 0) {
      Optional<SensorAssignment> assignment = members.stream()
        .filter(m -> m.getSensorName().equals(sensorName)).findAny();

      if (assignment.isEmpty()) {
        throw new SensorGroupsException(
          "Sensor '" + sensorName + "' is not a member of the group");
      } else {
        result = assignment.get();
      }
    }

    return result;
  }

  /**
   * Give this group a new name.
   *
   * @param name
   *          The new name.
   */
  protected void setName(String name) {
    this.name = name.trim();
  }

  /**
   * Get the number of members in this group.
   *
   * @return The number of members.
   */
  public int size() {
    return members.size();
  }

  /**
   * Indicates whether or not this group is empty.
   *
   * @return {@code true} if the group has no members.
   */
  public boolean isEmpty() {
    return members.isEmpty();
  }

  protected SensorAssignment getAssignment(String sensorName)
    throws SensorGroupsException {

    Optional<SensorAssignment> assignment = members.stream()
      .filter(m -> m.getSensorName().equalsIgnoreCase(sensorName)).findAny();

    if (assignment.isEmpty()) {
      throw new SensorGroupsException(
        "Assignment '" + sensorName + "' not found");
    }

    return assignment.get();
  }

  public void setLink(String sensor, int direction)
    throws SensorGroupsException {

    switch (direction) {
    case PREVIOUS: {
      setPrevLinkName(sensor);
      break;
    }
    case NEXT: {
      setNextLinkName(sensor);
      break;
    }
    default: {
      throw new SensorGroupsException("Invalid link direction");
    }
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    SensorGroup other = (SensorGroup) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return name;
  }
}
