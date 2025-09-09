package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Test the {@link PositionQCRoutineTest}.
 *
 * <p>
 * This generates sets of longitude and latitude values, with expected QC flags,
 * and creates a {@link DatasetSensorValues} with every combination of the two.
 * This is passed to the test routine, and then the values are checked against
 * their expected flags.
 * </p>
 */
@TestInstance(Lifecycle.PER_CLASS)
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user" })
public class PositionQCRoutineTest extends BaseTest {

  public static Instrument mockInstrument() {
    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getSensorAssignments())
      .thenReturn(Mockito.mock(SensorAssignments.class));

    return instrument;
  }

  /**
   * Generate test longitude values.
   *
   * @return The test longitudes.
   */
  private static LinkedHashMap<Double, Flag> generateLongitudes() {
    LinkedHashMap<Double, Flag> longitudes = new LinkedHashMap<Double, Flag>();
    longitudes.put(null, Flag.BAD);
    longitudes.put(Double.NaN, Flag.BAD);
    longitudes.put(-185D, Flag.BAD);
    longitudes.put(-180D, Flag.GOOD);
    longitudes.put(-1D, Flag.GOOD);
    longitudes.put(0D, Flag.GOOD);
    longitudes.put(1D, Flag.GOOD);
    longitudes.put(180D, Flag.GOOD);
    longitudes.put(185D, Flag.BAD);

    return longitudes;
  }

  /**
   * Generate test latitude values.
   *
   * @return The test latitudes.
   */
  private static LinkedHashMap<Double, Flag> generateLatitudes() {
    LinkedHashMap<Double, Flag> latitudes = new LinkedHashMap<Double, Flag>();
    latitudes.put(null, Flag.BAD);
    latitudes.put(Double.NaN, Flag.BAD);
    latitudes.put(-95D, Flag.BAD);
    latitudes.put(-90D, Flag.GOOD);
    latitudes.put(-1D, Flag.GOOD);
    latitudes.put(0D, Flag.GOOD);
    latitudes.put(1D, Flag.GOOD);
    latitudes.put(90D, Flag.GOOD);
    latitudes.put(95D, Flag.BAD);

    return latitudes;
  }

  /**
   * Generate position values with expected QC flags.
   *
   * @return The positions.
   */
  private static Stream<Arguments> generatePositions() {

    List<Arguments> arguments = new ArrayList<Arguments>();

    for (Map.Entry<Double, Flag> longitude : generateLongitudes().entrySet()) {
      for (Map.Entry<Double, Flag> latitude : generateLatitudes().entrySet()) {
        arguments.add(Arguments.of(longitude.getKey(), latitude.getKey(), Flag
          .getMostSignificantFlag(longitude.getValue(), latitude.getValue())));
      }
    }

    return arguments.stream();
  }

  /**
   * Run the generated position tests.
   *
   * @param longitude
   *          The longitude.
   * @param latitude
   *          The latitude.
   * @param expectedFlag
   *          The expected auto QC flag.
   * @throws Exception
   *           If any errors occur.
   */
  @ParameterizedTest
  @MethodSource("generatePositions")
  public void positionQCTest(Double longitude, Double latitude,
    Flag expectedFlag) throws Exception {

    initResourceManager();

    DatasetSensorValues sensorValues = new DatasetSensorValues(
      mockInstrument());

    LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    SensorValue longitudeSensorValue = new SensorValue(1L, 1L,
      SensorType.LONGITUDE_ID, time,
      null == longitude ? null : String.valueOf(longitude), new AutoQCResult(),
      Flag.ASSUMED_GOOD, null);

    sensorValues.add(longitudeSensorValue);

    SensorValue latitudeSensorValue = new SensorValue(2L, 1L,
      SensorType.LATITUDE_ID, time,
      null == latitude ? null : String.valueOf(latitude), new AutoQCResult(),
      Flag.ASSUMED_GOOD, null);

    sensorValues.add(latitudeSensorValue);

    PositionQCRoutine routine = new PositionQCRoutine(sensorValues);
    routine.qc(null, null);

    for (SensorValue sv : sensorValues.getAllPositionSensorValues()) {
      assertEquals(expectedFlag, sv.getAutoQcFlag());
    }
  }

  /**
   * Check behaviour when there is a latitude but no longitude.
   *
   * @throws Exception
   *           If any error occurs.
   */
  @Test
  public void noLongitudeTest() throws Exception {
    initResourceManager();

    DatasetSensorValues sensorValues = new DatasetSensorValues(
      mockInstrument());

    LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    SensorValue latitudeSensorValue = new SensorValue(2L, 1L,
      SensorType.LATITUDE_ID, time, "0", new AutoQCResult(), Flag.ASSUMED_GOOD,
      null);

    sensorValues.add(latitudeSensorValue);

    PositionQCRoutine routine = new PositionQCRoutine(sensorValues);
    routine.qc(null, null);

    for (SensorValue sv : sensorValues.getAllPositionSensorValues()) {
      assertEquals(Flag.BAD, sv.getAutoQcFlag());
    }
  }

  /**
   * Check behaviour if there is a longitude but no latitude.
   *
   * @throws Exception
   *           If any error occurs.
   */
  @Test
  public void noLatitudeTest() throws Exception {
    initResourceManager();

    DatasetSensorValues sensorValues = new DatasetSensorValues(
      mockInstrument());

    LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

    SensorValue longitudeSensorValue = new SensorValue(2L, 1L,
      SensorType.LONGITUDE_ID, time, "0", new AutoQCResult(), Flag.ASSUMED_GOOD,
      null);

    sensorValues.add(longitudeSensorValue);

    PositionQCRoutine routine = new PositionQCRoutine(sensorValues);
    routine.qc(null, null);

    for (SensorValue sv : sensorValues.getAllPositionSensorValues()) {
      assertEquals(Flag.BAD, sv.getAutoQcFlag());
    }
  }

  /**
   * Check behaviour when there is a latitude and longitude at different
   * timestamps.
   *
   * <p>
   * All position values should have a partner at the same timestamp.
   * </p>
   *
   * @throws Exception
   *           If any error occurs.
   */
  @Test
  public void differentTimesTest() throws Exception {

    initResourceManager();

    DatasetSensorValues sensorValues = new DatasetSensorValues(
      mockInstrument());

    SensorValue longitudeSensorValue = new SensorValue(2L, 1L,
      SensorType.LONGITUDE_ID, LocalDateTime.of(2025, 1, 1, 0, 0, 0), "0",
      new AutoQCResult(), Flag.ASSUMED_GOOD, null);

    sensorValues.add(longitudeSensorValue);

    SensorValue latitudeSensorValue = new SensorValue(2L, 1L,
      SensorType.LATITUDE_ID, LocalDateTime.of(2025, 1, 1, 0, 1, 0), "0",
      new AutoQCResult(), Flag.ASSUMED_GOOD, null);

    sensorValues.add(latitudeSensorValue);

    PositionQCRoutine routine = new PositionQCRoutine(sensorValues);
    routine.qc(null, null);

    for (SensorValue sv : sensorValues.getAllPositionSensorValues()) {
      assertEquals(Flag.BAD, sv.getAutoQcFlag());
    }

  }
}
