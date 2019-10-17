package junit.uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorConfigurationException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Tests for the {@link SensorType} class.
 *
 * @see SensorType
 *
 * @author Steve Jones
 *
 */
public class SensorTypeTest extends BaseTest {

  /**
   * Create a simple, valid {@link SensorType}. It has an ID (fixed to
   * {@code 1}), a name, and a group; all other information is {@code null}.
   *
   * @return The {@link SensorType}
   * @throws Exception
   *           If the object cannot be constructed
   */
  private SensorType getBasicSensorType() throws Exception {
    return new SensorType(1L, "Name", "Group", null, null, null, false, false,
      1, null, null, null);
  }

  /**
   * Tests that {@link SensorType} objects with invalid IDs cannot be created.
   *
   * @param id
   *          The test ID - provided by the test parameters.
   *
   * @see #createInvalidReferences()
   */
  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void invalidIdsTest(long id) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(id, "Name", "Group", null, null, null, false, false, 1,
        null, null, null);
    });
  }

  /**
   * Tests that {@link SensorType} objects with empty names cannot be created.
   *
   * @param name
   *          The test names - provided by the test parameters.
   *
   * @see #createNullEmptyStrings()
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void invalidNamesTest(String name) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(1L, name, "Group", null, null, null, false, false, 1, null,
        null, null);
    });
  }

  /**
   * Tests that {@link SensorType} objects with empty groups cannot be created.
   *
   * @param name
   *          The test groups - provided by the test parameters.
   *
   * @see #createNullEmptyStrings()
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void invalidGroupsTest(String group) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(1L, "Name", group, null, null, null, false, false, 1, null,
        null, null);
    });
  }

  /**
   * Create a set of invalid references for {@link SensorType} IDs ({@code 0}
   * and {@code -1}).
   *
   * @return The invalid IDs
   */
  @SuppressWarnings("unused")
  private static Stream<Long> createInvalidReferences() {
    return Stream.of(0L, -1L);
  }

  /**
   * Tests that {@link SensorType} objects with invalid parent IDs cannot be
   * created.
   *
   * @param parent
   *          The invalid parent IDs - provided by the test parameters.
   *
   * @see #createInvalidReferences()
   */
  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void invalidParentTest(Long parent) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(1, "Name", "Group", parent, null, null, false, false, 1,
        null, null, null);
    });
  }

  /**
   * Tests that {@link SensorType} objects with invalid Depends On IDs cannot be
   * created.
   *
   * @param dependsOn
   *          The invalid Depends On IDs - provided by the test parameters.
   *
   * @see #createInvalidReferences()
   */
  @ParameterizedTest
  @MethodSource("createInvalidReferences")
  public void invalidDependsOnTest(Long dependsOn) {
    assertThrows(MissingParamException.class, () -> {
      new SensorType(1, "Name", "Group", null, dependsOn, null, false, false, 1,
        null, null, null);
    });
  }

  /**
   * Tests that the {@link SensorType#getId()} method works.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void idTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals(1L, type.getId());
  }

  /**
   * Tests that the {@link SensorType#getName()} method works.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void nameTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals("Name", type.getName());
  }

  /**
   * Tests that the {@link SensorType#getGroup()} method works.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void groupTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals("Group", type.getGroup());
  }

  /**
   * Tests that the {@link SensorType#getParent()} and
   * {@link SensorType#hasParent()} methods work as expected when a
   * {@link SensorType} has no parent.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void nullParentTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, null, null,
      false, false, 1, null, null, null);
    assertEquals(SensorType.NO_PARENT, type.getParent());
    assertFalse(type.hasParent());
  }

  /**
   * Tests that a {@link SensorType} object that is its own parent cannot be
   * created.
   */
  @Test
  public void ownParentTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorType(1L, "Name", "Group", 1L, null, null, true, false, 1, null,
        null, null);
    });
  }

  /**
   * Tests that the {@link SensorType#getParent()} and
   * {@link SensorType#hasParent()} methods work as expected when a
   * {@link SensorType} has a parent {@link SensorType}.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void validParentTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", 2L, null, null, false,
      false, 1, null, null, null);
    assertEquals(2L, type.getParent());
    assertTrue(type.hasParent());
  }

  /**
   * Tests that the {@link SensorType#getDependsOn()} and
   * {@link SensorType#dependsOnOtherType()} methods work as expected when a
   * {@link SensorType} is not dependent on another.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void nullDependsOnTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, null, null,
      false, false, 1, null, null, null);
    assertEquals(SensorType.NO_DEPENDS_ON, type.getDependsOn());
    assertFalse(type.dependsOnOtherType());
  }

  /**
   * Tests that a {@link SensorType} that depends on itself cannot be created.
   */
  @Test
  public void ownDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorType(1L, "Name", "Group", null, 1L, null, true, false, 1, null,
        null, null);
    });
  }

  /**
   * Tests that the {@link SensorType#getDependsOn()} and
   * {@link SensorType#dependsOnOtherType()} methods work as expected when a
   * {@link SensorType} is dependent on another.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void dependsOnTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, 2L, null, false,
      false, 1, null, null, null);
    assertEquals(2L, type.getDependsOn());
    assertTrue(type.dependsOnOtherType());
  }

  /**
   * Tests that the {@link SensorType#getDependsQuestion()} and
   * {@link SensorType#hasDependsQuestion()} methods work when a
   * {@link SensorType} that depends on another {@link SensorType} without a
   * Depends Question is created.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #createNullEmptyStrings()
   */
  @ParameterizedTest
  @MethodSource("createNullEmptyStrings")
  public void nullDependsQuestionTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, null, null,
      false, false, 1, null, null, null);
    assertNull(type.getDependsQuestion());
    assertFalse(type.hasDependsQuestion());
  }

  /**
   * Tests that a Depends Question cannot be created if a {@link SensorType}
   * does not depend on another {@link SensorType}.
   */
  @Test
  public void dependsQuestionWithoutDependsOnTest() {
    assertThrows(SensorConfigurationException.class, () -> {
      new SensorType(1L, "Name", "Group", null, null, "Question?", false, false,
        1, null, null, null);
    });
  }

  /**
   * Tests that the {@link SensorType#getDependsQuestion()} and
   * {@link SensorType#hasDependsQuestion()} methods work when a
   * {@link SensorType} that depends on another {@link SensorType} with a
   * Depends Question is created.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #createNullEmptyStrings()
   */
  @Test
  public void dependsQuestionTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, 2L, "Question?",
      false, false, 1, null, null, null);
    assertNotNull(type.getDependsQuestion());
    assertTrue(type.hasDependsQuestion());
  }

  /**
   * Tests that a non-diagnostic {@link SensorType} can be created and that the
   * {@link SensorType#isDiagnostic()} method works as expected.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void notDiagnosticTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, null, null,
      false, false, 1, null, null, null);
    assertFalse(type.isDiagnostic());
  }

  /**
   * Tests that a diagnostic {@link SensorType} can be created and that the
   * {@link SensorType#isDiagnostic()} method works as expected.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void isDiagnosticTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, null, null,
      false, true, 1, null, null, null);
    assertTrue(type.isDiagnostic());
  }

  /**
   * Tests that a {@link SensorType} without internal calibration can be created
   * and that the {@link SensorType#hasInternalCalibration()} method works as
   * expected.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void notInternalCalibrationTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, null, null,
      false, false, 1, null, null, null);
    assertFalse(type.hasInternalCalibration());
  }

  /**
   * Tests that a {@link SensorType} with internal calibration can be created
   * and that the {@link SensorType#hasInternalCalibration()} method works as
   * expected.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   */
  @Test
  public void internalCalibrationTest() throws Exception {
    SensorType type = new SensorType(1L, "Name", "Group", null, null, null,
      true, false, 1, null, null, null);
    assertTrue(type.hasInternalCalibration());
  }

  /**
   * Tests that the {@link SensorType#equals(Object)} method returns
   * {@code false} when invoked with a non-{@link SensorType} object.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsRandomObjectTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals(ZoneOffset.UTC));
  }

  /**
   * Tests that the {@link SensorType#equals(Object)} method returns
   * {@code false} when invoked with a the name of a different
   * {@link SensorType} object.
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsWrongNameStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals("Flurble"));
  }

  /**
   * Tests that the {@link SensorType#equals(Object)} method returns
   * {@code false} when invoked with a the name of that object (the name is not
   * sufficient for {@code equals(Object) == true}).
   *
   * @throws Exception
   *           If the test {@link SensorType} cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void equalsActualNameStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertFalse(type.equals("Name"));
  }

  /**
   * Tests that a {@link SensorType} equals itself.
   *
   * @throws Exception
   *           If the test {@link SensorType}s cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void equalsSensorTypeTest() throws Exception {
    SensorType type = getBasicSensorType();
    SensorType type2 = getBasicSensorType();
    assertTrue(type.equals(type2));
  }

  /**
   * Tests that two different {@link SensorType} objects are not equal.
   *
   * @throws Exception
   *           If the test {@link SensorType}s cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void notEqualsSensorTypeTest() throws Exception {
    SensorType type = getBasicSensorType();
    SensorType type2 = new SensorType(4L, "Flurble", "Group", null, null, null,
      false, false, 1, null, null, null);
    assertFalse(type.equals(type2));
  }

  /**
   * Test that the {@link SensorType#compareTo(SensorType)} method orders
   * {@link SensorType}s by display order over name.
   *
   * @throws Exception
   *           If the test {@link SensorType}s cannot be created.
   */
  @Test
  public void compareToDiffDisplayOrderDiffNameTest() throws Exception {
    SensorType firstType = new SensorType(1L, "Bbbb", "Group", null, null, null,
      false, false, 1, null, null, null);
    SensorType secondType = new SensorType(2L, "Aaaa", "Group", null, null,
      null, false, false, 2, null, null, null);

    assertTrue(secondType.compareTo(firstType) > 0);
    assertTrue(firstType.compareTo(secondType) < 0);
  }

  /**
   * Test that the {@link SensorType#compareTo(SensorType)} method orders
   * {@link SensorType}s by display order when names are identical.
   *
   * @throws Exception
   *           If the test {@link SensorType}s cannot be created.
   */
  @Test
  public void compareToDiffDisplayOrderSameNameTest() throws Exception {
    SensorType firstType = new SensorType(1L, "Aaaa", "Group", null, null, null,
      false, false, 1, null, null, null);
    SensorType secondType = new SensorType(2L, "Aaaa", "Group", null, null,
      null, false, false, 2, null, null, null);

    assertTrue(secondType.compareTo(firstType) > 0);
    assertTrue(firstType.compareTo(secondType) < 0);
  }

  /**
   * Test that the {@link SensorType#compareTo(SensorType)} method orders
   * {@link SensorType}s by name when the display order is identical.
   *
   * @throws Exception
   *           If the test {@link SensorType}s cannot be created.
   */
  @Test
  public void compareToEqualOrderDifferentNameTest() throws Exception {
    SensorType firstType = new SensorType(1L, "Aaaa", "Group", null, null, null,
      false, false, 1, null, null, null);
    SensorType secondType = new SensorType(2L, "Bbbb", "Group", null, null,
      null, false, false, 1, null, null, null);

    assertTrue(secondType.compareTo(firstType) > 0);
    assertTrue(firstType.compareTo(secondType) < 0);
  }

  /**
   * Test that the {@link SensorType#compareTo(SensorType)} identical display
   * order and name gives a {@code 0} result.
   *
   * @throws Exception
   *           If the test {@link SensorType}s cannot be created.
   */
  @Test
  public void compareToEqualOrderAndNameTest() throws Exception {
    SensorType firstType = new SensorType(1L, "Aaaa", "Group", null, null, null,
      false, false, 1, null, null, null);
    SensorType secondType = new SensorType(2L, "Aaaa", "Group", null, null,
      null, false, false, 1, null, null, null);

    assertTrue(secondType.compareTo(firstType) == 0);
  }

  /**
   * Tests that the {@link SensorType#toString()} method works as expected.
   *
   * @throws Exception
   *           If the test {@link SensorType}s cannot be created.
   *
   * @see #getBasicSensorType()
   */
  @Test
  public void toStringTest() throws Exception {
    SensorType type = getBasicSensorType();
    assertEquals("Sensor Type: Name", type.toString());
  }
}
