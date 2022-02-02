package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroup;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;

public class SensorGroupsTest extends BaseTest {

  /**
   * Test the construction of the {@link SensorGroups} object and its default
   * group.
   */
  @Test
  public void basicConstructorTest() {

    SensorGroups groups = new SensorGroups();
    SensorGroup defaultGroup = groups.first();

    assertAll(() -> assertEquals(1, groups.size()),
      () -> assertFalse(groups.isComplete()),
      () -> assertEquals("Default", defaultGroup.getName()),
      () -> assertEquals(0, defaultGroup.size()),
      () -> assertFalse(defaultGroup.hasNext()),
      () -> assertFalse(defaultGroup.hasPrev()));
  }

  @Test
  public void addGroupBeforeFirst() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup", null);

    SensorGroup origGroup = groups.getGroup("Default");

    assertAll(() -> assertTrue(checkGroupOrder(groups, "NewGroup", "Default")),
      () -> assertEquals(groups.first().getName(), "NewGroup"),
      () -> assertFalse(groups.first().hasPrev()),
      () -> assertTrue(groups.first().hasNext()),
      () -> assertTrue(origGroup.hasPrev()),
      () -> assertFalse(origGroup.hasNext()));
  }

  @Test
  public void addGroupAfterLast() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup", "Default");
    SensorGroup origGroup = groups.getGroup("Default");
    SensorGroup newGroup = groups.getGroup("NewGroup");

    assertAll(() -> assertTrue(checkGroupOrder(groups, "Default", "NewGroup")),
      () -> assertFalse(origGroup.hasPrev()),
      () -> assertTrue(origGroup.hasNext()),
      () -> assertTrue(newGroup.hasPrev()),
      () -> assertFalse(newGroup.hasNext()));
  }

  @Test
  public void addGroupInMiddle() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup", "Default");
    groups.addGroup("Middle", "Default");

    assertAll(
      () -> assertTrue(
        checkGroupOrder(groups, "Default", "Middle", "NewGroup")),
      () -> assertEquals(3, groups.size()));
  }

  @Test
  public void addGroupNonExistentAfter() {
    SensorGroups groups = new SensorGroups();

    assertThrows(SensorGroupsException.class, () -> {
      groups.addGroup("NewGroup", "I Don't Exist");
    });
  }

  @Test
  public void addGroupExistingName() {
    SensorGroups groups = new SensorGroups();

    assertThrows(SensorGroupsException.class, () -> {
      groups.addGroup("Default", null);
    });
  }

  @Test
  public void deleteOnlyGroup() {
    SensorGroups groups = new SensorGroups();

    assertThrows(SensorGroupsException.class, () -> {
      groups.deleteGroup("Default");
    });
  }

  @Test
  public void deleteNonExistentGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();

    // Multiple groups to make sure we don't re-test deleting the only group
    groups.addGroup("NewGroup", null);

    assertThrows(SensorGroupsException.class, () -> {
      groups.deleteGroup("I Don't Exist");
    });
  }

  @Test
  public void deleteFirstGroup() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    groups.deleteGroup("Default");

    assertAll(
      () -> assertTrue(checkGroupOrder(groups, "NewGroup1", "NewGroup2")),
      () -> assertEquals("NewGroup1", groups.first().getName()));
  }

  @Test
  public void deleteMiddleGroup() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    groups.deleteGroup("NewGroup1");
    assertTrue(checkGroupOrder(groups, "Default", "NewGroup2"));
  }

  @Test
  public void deleteLastGroup() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    groups.deleteGroup("NewGroup2");
    assertTrue(checkGroupOrder(groups, "Default", "NewGroup1"));
  }

  @Test
  public void renameGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.renameGroup("Default", "NewName");
    assertEquals("NewName", groups.first().getName());
  }

  @Test
  public void renameGroupToExistingName() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();

    assertThrows(SensorGroupsException.class, () -> {
      groups.renameGroup("NewGroup1", "NewGroup2");
    });
  }

  @Test
  public void renameGroupToItself() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    groups.renameGroup("Default", "Default");
    assertEquals("Default", groups.first().getName());
  }

  @Test
  public void renameNonExistentGroup() {
    SensorGroups groups = new SensorGroups();
    assertThrows(SensorGroupsException.class, () -> {
      groups.renameGroup("I Don't Exist", "Anything");
    });
  }

  @Test
  public void asListTest() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();

    List<SensorGroup> groupsList = groups.asList();

    assertAll(() -> assertEquals(groups.size(), groupsList.size()),
      () -> assertEquals("Default", groupsList.get(0).getName()),
      () -> assertEquals("NewGroup1", groupsList.get(1).getName()),
      () -> assertEquals("NewGroup2", groupsList.get(2).getName()));
  }

  @Test
  public void getGroupExists() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    assertEquals("Default", groups.getGroup("Default").getName());
  }

  @Test
  public void getGroupDoesntExist() {
    SensorGroups groups = new SensorGroups();
    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroup("I Don't Exist");
    });
  }

  @Test
  public void groupExistsExists() {
    SensorGroups groups = new SensorGroups();
    assertTrue(groups.groupExists("Default"));
  }

  @Test
  public void groupExistsDoesntExist() {
    SensorGroups groups = new SensorGroups();
    assertFalse(groups.groupExists("I Don't Exist"));
  }

  @Test
  public void addAssignmentOnlyGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    SensorGroup group = groups.first();
    TreeSet<SensorAssignment> assignments = group.getMembers();
    assertEquals("Sensor 1", assignments.first().getSensorName());
  }

  @Test
  public void addAssignmentMultipleGroups() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup1", null);
    groups.addAssignment(makeAssignment("Sensor 1"));
    SensorGroup group = groups.first();
    TreeSet<SensorAssignment> assignments = group.getMembers();
    assertEquals("Sensor 1", assignments.first().getSensorName());
  }

  @Test
  public void addAssignmentExistsInFirstGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup1", null);

    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);

    assertThrows(SensorGroupsException.class, () -> {
      groups.addAssignment(assignment);
    });
  }

  @Test
  public void addAssignmentExistsInOtherGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();

    SensorAssignment assignment = makeAssignment("Sensor 1");

    // Add to the single group
    groups.addAssignment(assignment);

    // Add a new group as the first group
    groups.addGroup("NewGroup1", null);

    // Try to add the assignment again
    assertThrows(SensorGroupsException.class, () -> {
      groups.addAssignment(assignment);
    });
  }

  @Test
  public void containsDoesntContainNoOtherAssignments()
    throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    assertFalse(groups.contains(makeAssignment("Sensor 1")));
  }

  @Test
  public void containsDoesntContainHasOtherAssignments()
    throws SensorGroupsException {

    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup1", null);
    groups.addAssignment(makeAssignment("Sensor 2"));

    assertFalse(groups.contains(makeAssignment("Sensor 3")));
  }

  @Test
  public void containsInFirstGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    assertTrue(groups.contains(assignment));
  }

  @Test
  public void containsInOtherGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.addGroup("NewGroup1", null);
    assertTrue(groups.contains(assignment));
  }

  @Test
  public void getGroupNotInEmptyOnlyGroup() {
    SensorGroups groups = new SensorGroups();
    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroup(makeAssignment("Sensor 1"));
    });
  }

  @Test
  public void getGroupNotInPopulatedOnlyGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroup(makeAssignment("Sensor 2"));
    });
  }

  @Test
  public void getGroupNotInEmptyMultipleGroups() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup1", null);
    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroup(makeAssignment("Sensor 1"));
    });
  }

  @Test
  public void getGroupNotInPopulatedMultipleGroups()
    throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup1", null);
    groups.addAssignment(makeAssignment("Sensor 2"));

    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroup(makeAssignment("Sensor 3"));
    });
  }

  @Test
  public void getGroupInOnlyGroupOnlyAssignment() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    SensorGroup group = groups.getGroup(assignment);
    assertEquals("Default", group.getName());
  }

  @Test
  public void getGroupInOnlyGroupMultipleAssignments()
    throws SensorGroupsException {

    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.addAssignment(makeAssignment("Sensor 2"));
    SensorGroup group = groups.getGroup(assignment);
    assertEquals("Default", group.getName());
  }

  @Test
  public void getGroupInAnyGroupOnlyAssignment() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.addGroup("NewGroup1", null);
    SensorGroup group = groups.getGroup(assignment);
    assertEquals("Default", group.getName());
  }

  @Test
  public void getGroupInAnyGroupMultipleAssignments()
    throws SensorGroupsException {

    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.addAssignment(makeAssignment("Sensor 2"));
    groups.addGroup("NewGroup1", null);
    groups.addAssignment(makeAssignment("Sensor 3"));

    SensorGroup group = groups.getGroup(assignment);
    assertEquals("Default", group.getName());
  }

  @Test
  public void getGroupIndexDoesntExist() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    SensorGroup group = groups.getGroup("NewGroup1");
    groups.deleteGroup("NewGroup1");
    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroupIndex(group);
    });
  }

  @Test
  public void getGroupIndexFirst() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    SensorGroup group = groups.getGroup("Default");
    assertEquals(0, groups.getGroupIndex(group));
  }

  @Test
  public void getGroupIndexNotFirst() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    SensorGroup group = groups.getGroup("NewGroup1");
    assertEquals(1, groups.getGroupIndex(group));
  }

  @Test
  public void removeAssignmentNotAssignedOnlyGroup()
    throws SensorGroupsException {

    // Should run without any errors
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.remove(makeAssignment("Sensor 2"));
  }

  @Test
  public void removeAssignmentNotAssignedMultipleGroups()
    throws SensorGroupsException {

    // Should run without any errors
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup1", null);
    groups.addAssignment(makeAssignment("Sensor 2"));
    groups.remove(makeAssignment("Sensor 3"));
  }

  @Test
  public void removeAssignmentFromFirstGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup1", null);
    SensorAssignment assignment = makeAssignment("Sensor 2");
    groups.addAssignment(assignment);

    groups.remove(assignment);
    assertFalse(groups.contains(assignment));
  }

  @Test
  public void removeAssignmentFromOtherGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.addGroup("NewGroup1", null);
    groups.addAssignment(makeAssignment("Sensor 2"));

    groups.remove(assignment);
    assertFalse(groups.contains(assignment));
  }

  @Test
  public void removeAssignmentsNotAssignedOnlyGroup() {

    // Should run without errors
    SensorGroups groups = new SensorGroups();
    List<SensorAssignment> assignments = new ArrayList<SensorAssignment>();
    assignments.add(makeAssignment("Sensor 1"));
    assignments.add(makeAssignment("Sensor 2"));
    groups.remove(assignments);
  }

  @Test
  public void removeAssignmentsAcrossGroups() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment1 = makeAssignment("Sensor 1");
    SensorAssignment assignment2 = makeAssignment("Sensor 2");
    SensorAssignment assignment3 = makeAssignment("Sensor 3");
    SensorAssignment assignment4 = makeAssignment("Sensor 4");

    groups.addAssignment(assignment1);
    groups.addAssignment(assignment2);

    groups.addGroup("NewGroup1", null);
    groups.addAssignment(assignment3);
    groups.addAssignment(assignment4);

    List<SensorAssignment> assignments = new ArrayList<SensorAssignment>();
    assignments.add(assignment1);
    assignments.add(assignment3);

    groups.remove(assignments);

    assertAll(() -> assertFalse(groups.contains(assignment1)),
      () -> assertTrue(groups.contains(assignment2)),
      () -> assertFalse(groups.contains(assignment3)),
      () -> assertTrue(groups.contains(assignment4)));
  }

  @Test
  public void moveNonExistentSensor() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    assertThrows(SensorGroupsException.class, () -> {
      groups.moveSensor("Sensor 1", "NewGroup2");
    });
  }

  @Test
  public void moveSensorToNonExistentGroup() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    assertThrows(SensorGroupsException.class, () -> {
      groups.moveSensor("Sensor 1", "I Don't Exist");
    });
  }

  @Test
  public void moveSensorToOwnGroup() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();

    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);

    groups.moveSensor("Sensor 1", "Default");
    assertEquals("Default", groups.getGroup(assignment).getName());
  }

  @Test
  public void moveSensorToOtherGroup() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();

    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);

    groups.moveSensor("Sensor 1", "NewGroup2");
    assertEquals("NewGroup2", groups.getGroup(assignment).getName());
  }

  @Test
  public void groupPairs() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();

    List<SensorGroupPair> pairs = groups.getGroupPairs();

    SensorGroupPair pair1 = pairs.get(0);
    SensorGroupPair pair2 = pairs.get(1);

    assertAll(() -> assertEquals("Default", pair1.first().getName()),
      () -> assertEquals("NewGroup1", pair1.second().getName()),
      () -> assertEquals("Default".hashCode(), pair1.getId()),

      () -> assertEquals("NewGroup1", pair2.first().getName()),
      () -> assertEquals("NewGroup2", pair2.second().getName()),
      () -> assertEquals("NewGroup1".hashCode(), pair2.getId()));
  }

  @Test
  public void getPairById() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    assertEquals("Default",
      groups.getGroupPair("Default".hashCode()).first().getName());
  }

  @Test
  public void getNonExistentPair() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroupPair(-1);
    });
  }

  @Test
  public void deleteGroupAssignmentsTransferred() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.addGroup("NewGroup1", null);
    groups.deleteGroup("Default");
    assertTrue(groups.contains(assignment));
  }

  @Test
  public void initNoNextName() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    assertNull(groups.getGroup("Default").getNextLinkName());
  }

  @Test
  public void initNoPrevName() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    assertNull(groups.getGroup("Default").getPrevLinkName());
  }

  @Test
  public void setNextLinkNoSensor() {
    SensorGroups groups = new SensorGroups();

    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroup("Default").setLink("Sensor 1", SensorGroup.NEXT);
    });
  }

  @Test
  public void setPrevLinkNoSensor() {
    SensorGroups groups = new SensorGroups();

    assertThrows(SensorGroupsException.class, () -> {
      groups.getGroup("Default").setLink("Sensor 1", SensorGroup.PREVIOUS);
    });
  }

  @Test
  public void setNextLink() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    SensorGroup group = groups.getGroup("Default");
    group.setLink(assignment.getSensorName(), SensorGroup.NEXT);

    assertEquals("Sensor 1", group.getNextLinkName());
  }

  @Test
  public void setPrevLink() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    SensorGroup group = groups.getGroup("Default");
    group.setLink(assignment.getSensorName(), SensorGroup.PREVIOUS);

    assertEquals("Sensor 1", group.getPrevLinkName());
  }

  @Test
  public void setLinkInvalidDirection() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    SensorGroup group = groups.getGroup("Default");
    assertThrows(SensorGroupsException.class, () -> {
      group.setLink(assignment.getSensorName(), 767);
    });
  }

  @Test
  public void emptyGroupIsEmpty() {
    SensorGroups groups = new SensorGroups();
    assertTrue(groups.first().isEmpty());
  }

  @Test
  public void populatedGroupIsEmpty() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    assertFalse(groups.first().isEmpty());
  }

  @Test
  public void removeNextLinkAssignment() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.first().setLink("Sensor 1", SensorGroup.NEXT);
    groups.remove(assignment);
    assertNull(groups.first().getNextLinkName());
  }

  @Test
  public void removePrevLinkAssignment() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.first().setLink("Sensor 1", SensorGroup.PREVIOUS);
    groups.remove(assignment);
    assertNull(groups.first().getPrevLinkName());
  }

  @Test
  public void moveSensorNextLinkAssignment() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.first().setLink("Sensor 1", SensorGroup.NEXT);
    groups.moveSensor("Sensor 1", "NewGroup1");
    assertNull(groups.first().getNextLinkName());
  }

  @Test
  public void moveSensorPrevLinkAssignment() throws SensorGroupsException {
    SensorGroups groups = makeThreeGroups();
    SensorAssignment assignment = makeAssignment("Sensor 1");
    groups.addAssignment(assignment);
    groups.first().setLink("Sensor 1", SensorGroup.PREVIOUS);
    groups.moveSensor("Sensor 1", "NewGroup1");
    assertNull(groups.first().getPrevLinkName());
  }

  @Test
  public void isCompleteSingleGroup() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    assertTrue(groups.isComplete());
  }

  @Test
  public void isCompleteEmptySingleGroup() {
    SensorGroups groups = new SensorGroups();
    assertFalse(groups.isComplete());
  }

  @Test
  public void isCompleteEmptyOnlyOnePopulatedGroup()
    throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup", "Default");
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.first().setLink("Sensor 1", SensorGroup.NEXT);
    assertFalse(groups.isComplete());
  }

  @Test
  public void isCompleteAllPopulatedNoLinks() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup", null);
    groups.addAssignment(makeAssignment("Sensor 2"));
    assertFalse(groups.isComplete());
  }

  @Test
  public void isCompleteAllPopulatedNoNextLink() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup", null);
    groups.addAssignment(makeAssignment("Sensor 2"));
    groups.getGroup("Default").setLink("Sensor 1", SensorGroup.PREVIOUS);
    assertFalse(groups.isComplete());
  }

  @Test
  public void isCompleteAllPopulatedNoPrevLink() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup", null);
    groups.addAssignment(makeAssignment("Sensor 2"));
    groups.getGroup("NewGroup").setLink("Sensor 2", SensorGroup.NEXT);
    assertFalse(groups.isComplete());
  }

  @Test
  public void isCompleteComplete() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addAssignment(makeAssignment("Sensor 1"));
    groups.addGroup("NewGroup", null);
    groups.addAssignment(makeAssignment("Sensor 2"));
    groups.getGroup("Default").setLink("Sensor 1", SensorGroup.PREVIOUS);
    groups.getGroup("NewGroup").setLink("Sensor 2", SensorGroup.NEXT);
    assertTrue(groups.isComplete());
  }

  private SensorGroups makeThreeGroups() throws SensorGroupsException {
    SensorGroups groups = new SensorGroups();
    groups.addGroup("NewGroup1", "Default");
    groups.addGroup("NewGroup2", "NewGroup1");
    return groups;
  }

  private SensorAssignment makeAssignment(String sensorName) {
    SensorAssignment assignment = Mockito.mock(SensorAssignment.class);
    Mockito.when(assignment.getSensorName()).thenReturn(sensorName);
    return assignment;
  }

  private boolean checkGroupOrder(SensorGroups groups, String... names) {
    List<String> expectedNames = Arrays.asList(names);
    List<String> actualNames = groups.getGroupNames();

    boolean ok = true;

    if (actualNames.size() != expectedNames.size()) {
      ok = false;
    } else {
      for (int i = 0; i < actualNames.size(); i++) {
        if (!actualNames.get(i).equals(expectedNames.get(i))) {
          ok = false;
          break;
        }
      }
    }

    return ok;
  }
}
