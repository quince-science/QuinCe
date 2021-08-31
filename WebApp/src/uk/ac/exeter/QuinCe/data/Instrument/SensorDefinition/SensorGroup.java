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
    return members.remove(assignment);
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
    return null != prevGroupLink;
  }

  /**
   * Determines whether or not this group has a following group.
   *
   * @return {@code true} if the group has a following group.
   */
  public boolean hasNext() {
    return null != nextGroupLink;
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
  public SensorAssignment getPrevLink() {
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
  public SensorAssignment getNextLink() {
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
   */
  protected void setPrevLink(SensorAssignment assignment) {
    this.prevGroupLink = assignment;
  }

  /**
   * Specify the {@link SensorAssignment} used as the link to the next group.
   *
   * @param group
   *          The linking assignment.
   */
  protected void setNextLink(SensorAssignment assignment) {
    this.nextGroupLink = assignment;
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

  protected Optional<SensorAssignment> getAssignment(String sensorName) {
    return members.stream()
      .filter(m -> m.getSensorName().equalsIgnoreCase(sensorName)).findAny();
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
}
