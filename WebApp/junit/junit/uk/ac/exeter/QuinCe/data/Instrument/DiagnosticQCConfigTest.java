package junit.uk.ac.exeter.QuinCe.data.Instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Instrument.DiagnosticQCConfig;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

public class DiagnosticQCConfigTest extends BaseTest {

  /**
   * Get the range min for a non-set sensor. Should be null.
   */
  @Test
  public void getMinNonExistentSensor() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    assertNull(config.getRangeMin(sensor));
  }

  /**
   * Get the range min for a non-set sensor. Should be null.
   */
  @Test
  public void getMaxNonExistentSensor() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    assertNull(config.getRangeMax(sensor));
  }

  /**
   * Set a minimum value, and leave the max unset.
   */
  @Test
  public void setMinNullMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    assertEquals(5D, config.getRangeMin(sensor));
    assertNull(config.getRangeMax(sensor));
  }

  /**
   * Set a max value and leave the min unset.
   */
  @Test
  public void setMaxNullMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 1D);
    assertEquals(1D, config.getRangeMax(sensor));
    assertNull(config.getRangeMin(sensor));
  }

  /**
   * Set a min with a larger max.
   */
  @Test
  public void setMinLargerMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 10D);
    config.setRangeMin(sensor, 5D);
    assertEquals(5D, config.getRangeMin(sensor));
    assertEquals(10D, config.getRangeMax(sensor));
  }

  /**
   * Set a max with a smaller min
   */
  @Test
  public void setMaxSmallerMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 10D);
    assertEquals(5D, config.getRangeMin(sensor));
    assertEquals(10D, config.getRangeMax(sensor));
  }

  /**
   * Set min larger than max. Values should get swapped.
   */
  @Test
  public void setMinSmallerMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 5D);
    config.setRangeMin(sensor, 10D);
    assertEquals(5D, config.getRangeMin(sensor));
    assertEquals(10D, config.getRangeMax(sensor));
  }

  /**
   * Set max smaller than min. Values should get swapped.
   */
  @Test
  public void setMaxLargerMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 10D);
    config.setRangeMax(sensor, 5D);
    assertEquals(5D, config.getRangeMin(sensor));
    assertEquals(10D, config.getRangeMax(sensor));
  }

  /**
   * Set min equal to max.
   */
  @Test
  public void setMinEqualsMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 5D);
    config.setRangeMin(sensor, 5D);
    assertEquals(5D, config.getRangeMin(sensor));
    assertEquals(5D, config.getRangeMax(sensor));
  }

  /**
   * Set max equal to min.
   */
  @Test
  public void setMaxEqualsMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 5D);
    assertEquals(5D, config.getRangeMin(sensor));
    assertEquals(5D, config.getRangeMax(sensor));
  }

  /**
   * Check that range check of NaN value returns {@code true} when there's no
   * range set.
   */
  @Test
  public void rangeCheckNanNoMinMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    assertTrue(config.isInRange(sensor, Double.NaN));
  }

  /**
   * Check that range check of NaN value returns {@code true} when there's only
   * a min range set.
   */
  @Test
  public void rangeCheckNanOnlyMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    assertTrue(config.isInRange(sensor, Double.NaN));
  }

  /**
   * Check that range check of NaN value returns {@code true} when there's only
   * a max range set.
   */
  @Test
  public void rangeCheckNanOnlyMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 5D);
    assertTrue(config.isInRange(sensor, Double.NaN));
  }

  /**
   * Check that range check of NaN value returns {@code true} when both min and
   * max are set.
   */
  @Test
  public void rangeCheckNanMinMaxSet() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 10D);
    assertTrue(config.isInRange(sensor, Double.NaN));
  }

  /**
   * Checking a value with no range set should be OK.
   */
  @Test
  public void rangeCheckNonExistentSensor() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    assertTrue(config.isInRange(sensor, 5D));
  }

  /**
   * Check a value with only min set; value below the min.
   */
  @Test
  public void rangeCheckMinOnlyBelowMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    assertFalse(config.isInRange(sensor, 1D));
  }

  /**
   * Check a value with only min set; value equals the min.
   */
  @Test
  public void rangeCheckMinOnlyEqualsMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    assertTrue(config.isInRange(sensor, 5D));
  }

  /**
   * Check a value with only min set; value above the min.
   */
  @Test
  public void rangeCheckMinOnlyAboveMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    assertTrue(config.isInRange(sensor, 10D));
  }

  /**
   * Check a value with only min set; value below the min.
   */
  @Test
  public void rangeCheckMaxOnlyBelowMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 5D);
    assertTrue(config.isInRange(sensor, 1D));
  }

  /**
   * Check a value with only min set; value equals the min.
   */
  @Test
  public void rangeCheckMaxOnlyEqualsMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 5D);
    assertTrue(config.isInRange(sensor, 5D));
  }

  /**
   * Check a value with only min set; value above the min.
   */
  @Test
  public void rangeCheckMaxOnlyAboveMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMax(sensor, 5D);
    assertFalse(config.isInRange(sensor, 10D));
  }

  /**
   * Check a value with a non-equal range set; value below minimum.
   */
  @Test
  public void rangeCheckBothSetBelowMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 10D);
    assertFalse(config.isInRange(sensor, 0D));
  }

  /**
   * Check a value with a non-equal range set; value equals minimum.
   */
  @Test
  public void rangeCheckBothSetEqualsMin() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 10D);
    assertTrue(config.isInRange(sensor, 5D));
  }

  /**
   * Check a value with a non-equal range set; value within range.
   */
  @Test
  public void rangeCheckBothSetInRange() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 10D);
    assertTrue(config.isInRange(sensor, 7D));
  }

  /**
   * Check a value with a non-equal range set; value equals max.
   */
  @Test
  public void rangeCheckBothSetEqualsMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 10D);
    assertTrue(config.isInRange(sensor, 10D));
  }

  /**
   * Check a value with a non-equal range set; value above max.
   */
  @Test
  public void rangeCheckBothSetAboveMax() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 10D);
    assertFalse(config.isInRange(sensor, 15D));
  }

  /**
   * Check a value with an equal range set; value below.
   */
  @Test
  public void rangeCheckEqualMinMaxBelow() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 5D);
    assertFalse(config.isInRange(sensor, 4D));
  }

  /**
   * Check a value with an equal range set; value below.
   */
  @Test
  public void rangeCheckEqualMinMaxEqual() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 5D);
    assertTrue(config.isInRange(sensor, 5D));
  }

  /**
   * Check a value with an equal range set; value below.
   */
  @Test
  public void rangeCheckEqualMinMaxAbove() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment sensor = makeSensorAssignment(1);
    config.setRangeMin(sensor, 5D);
    config.setRangeMax(sensor, 5D);
    assertFalse(config.isInRange(sensor, 6D));
  }

  /**
   * We should get an empty list of assignments for an unregistered diagnostic
   * sensor.
   */
  @Test
  public void getRunTypesUnregisteredDiagnosticSensor() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement = makeSensorAssignment(2);
    assertEquals(0, config.getAssignedRunTypes(diagnostic, measurement).size());
  }

  /**
   * We should get an empty list of assignments for an unregistered measurement
   * sensor.
   */
  @Test
  public void getRunTypesUnregisteredMeasurementSensor() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement = makeSensorAssignment(2);
    config.setRangeMin(diagnostic, 1D);
    assertEquals(0, config.getAssignedRunTypes(diagnostic, measurement).size());
    assertFalse(config.hasAssignedRunTypes(diagnostic));
    assertFalse(config.hasAssignedRunTypes(diagnostic, measurement));
  }

  @Test
  public void assignRunTypes() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement = makeSensorAssignment(2);
    List<String> runTypes = Arrays.asList("A", "B");
    config.setAssignedRunTypes(diagnostic, measurement, runTypes);
    assertTrue(CollectionUtils.isEqualCollection(runTypes,
      config.getAssignedRunTypes(diagnostic, measurement)));
    assertTrue(config.hasAssignedRunTypes(diagnostic));
    assertTrue(config.hasAssignedRunTypes(diagnostic, measurement));
  }

  @Test
  public void overwriteRunTypes() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement = makeSensorAssignment(2);
    List<String> runTypes1 = Arrays.asList("A", "B");
    List<String> runTypes2 = Arrays.asList("A");

    config.setAssignedRunTypes(diagnostic, measurement, runTypes1);
    config.setAssignedRunTypes(diagnostic, measurement, runTypes2);
    assertTrue(CollectionUtils.isEqualCollection(runTypes2,
      config.getAssignedRunTypes(diagnostic, measurement)));
    assertTrue(config.hasAssignedRunTypes(diagnostic));
    assertTrue(config.hasAssignedRunTypes(diagnostic, measurement));
  }

  @Test
  public void unsetMeasurementRunTypes() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement1 = makeSensorAssignment(11);
    SensorAssignment measurement2 = makeSensorAssignment(22);

    List<String> runTypes = Arrays.asList("A", "B");
    config.setAssignedRunTypes(diagnostic, measurement1, runTypes);
    assertTrue(config.hasAssignedRunTypes(diagnostic));
    assertTrue(config.hasAssignedRunTypes(diagnostic, measurement1));
    assertFalse(config.hasAssignedRunTypes(diagnostic, measurement2));
  }

  @Test
  public void removeRunTypesEmptyList() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement = makeSensorAssignment(11);
    List<String> runTypes = Arrays.asList("A", "B");

    config.setAssignedRunTypes(diagnostic, measurement, runTypes);
    config.setAssignedRunTypes(diagnostic, measurement,
      new ArrayList<String>());

    assertEquals(0, config.getAssignedRunTypes(diagnostic, measurement).size());
    assertFalse(config.hasAssignedRunTypes(diagnostic, measurement));
    assertFalse(config.hasAssignedRunTypes(diagnostic));
  }

  @Test
  public void removeRunTypesNull() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement = makeSensorAssignment(11);
    List<String> runTypes = Arrays.asList("A", "B");

    config.setAssignedRunTypes(diagnostic, measurement, runTypes);
    config.setAssignedRunTypes(diagnostic, measurement, null);

    assertEquals(0, config.getAssignedRunTypes(diagnostic, measurement).size());
    assertFalse(config.hasAssignedRunTypes(diagnostic, measurement));
    assertFalse(config.hasAssignedRunTypes(diagnostic));
  }

  @Test
  public void removeOneMeasurementRunTypes() {
    DiagnosticQCConfig config = new DiagnosticQCConfig();
    SensorAssignment diagnostic = makeSensorAssignment(1);
    SensorAssignment measurement1 = makeSensorAssignment(11);
    SensorAssignment measurement2 = makeSensorAssignment(22);
    List<String> runTypes1 = Arrays.asList("A", "B");
    List<String> runTypes2 = Arrays.asList("C", "D");

    config.setAssignedRunTypes(diagnostic, measurement1, runTypes1);
    config.setAssignedRunTypes(diagnostic, measurement2, runTypes2);

    config.setAssignedRunTypes(diagnostic, measurement1, null);

    assertFalse(config.hasAssignedRunTypes(diagnostic, measurement1));
    assertTrue(config.hasAssignedRunTypes(diagnostic, measurement2));
    assertTrue(config.hasAssignedRunTypes(diagnostic));
  }

  /**
   * Make a mock {@link SensorType} with the specified ID.
   *
   * @param id
   *          The ID.
   * @return The mock SensorType
   */
  private SensorType makeSensorType(long id) {
    SensorType sensorType = Mockito.mock(SensorType.class);
    Mockito.when(sensorType.getId()).thenReturn(id);
    return sensorType;
  }

  /**
   * Make a mock {@link SensorAssignment} for the specified column.
   *
   * <p>
   * The assignment's sensor type is given the same ID as the column.
   * </p>
   *
   * @param column
   *          The column number.
   * @return The SensorAssignment.
   */
  private SensorAssignment makeSensorAssignment(int column) {
    SensorType sensorType = makeSensorType(column);
    SensorAssignment assignment = Mockito.mock(SensorAssignment.class);
    Mockito.when(assignment.getDataFile()).thenReturn("DataFile");
    Mockito.when(assignment.getColumn()).thenReturn(column);
    Mockito.when(assignment.getSensorType()).thenReturn(sensorType);
    return assignment;
  }
}
