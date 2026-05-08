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
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
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

  private static DatasetSensorValues mockDatasetSensorValues()
    throws Exception {
    DatasetSensorValues datasetSensorValues = new DatasetSensorValues(
      Mockito.mock(DataSet.class));

    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getBasis()).thenReturn(Instrument.BASIS_TIME);
    Mockito.when(instrument.getSensorAssignments())
      .thenReturn(Mockito.mock(SensorAssignments.class));

    Mockito.when(datasetSensorValues.getInstrument()).thenReturn(instrument);
    Mockito.when(datasetSensorValues.getFlagScheme()).thenReturn(flagScheme);

    return datasetSensorValues;
  }

  /**
   * Generate test longitude values.
   *
   * @return The test longitudes.
   */
  private static LinkedHashMap<Double, Flag> generateLongitudes() {
    LinkedHashMap<Double, Flag> longitudes = new LinkedHashMap<Double, Flag>();
    longitudes.put(null, flagScheme.getBadFlag());
    longitudes.put(Double.NaN, flagScheme.getBadFlag());
    longitudes.put(-185D, flagScheme.getBadFlag());
    longitudes.put(-180D, flagScheme.getGoodFlag());
    longitudes.put(-1D, flagScheme.getGoodFlag());
    longitudes.put(0D, flagScheme.getGoodFlag());
    longitudes.put(1D, flagScheme.getGoodFlag());
    longitudes.put(180D, flagScheme.getGoodFlag());
    longitudes.put(185D, flagScheme.getBadFlag());

    return longitudes;
  }

  /**
   * Generate test latitude values.
   *
   * @return The test latitudes.
   */
  private static LinkedHashMap<Double, Flag> generateLatitudes() {
    LinkedHashMap<Double, Flag> latitudes = new LinkedHashMap<Double, Flag>();
    latitudes.put(null, flagScheme.getBadFlag());
    latitudes.put(Double.NaN, flagScheme.getBadFlag());
    latitudes.put(-95D, flagScheme.getBadFlag());
    latitudes.put(-90D, flagScheme.getGoodFlag());
    latitudes.put(-1D, flagScheme.getGoodFlag());
    latitudes.put(0D, flagScheme.getGoodFlag());
    latitudes.put(1D, flagScheme.getGoodFlag());
    latitudes.put(90D, flagScheme.getGoodFlag());
    latitudes.put(95D, flagScheme.getBadFlag());

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
        arguments.add(Arguments.of(longitude.getKey(), latitude.getKey(),
          Flag.mostSignificant(longitude.getValue(), latitude.getValue())));
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

    DatasetSensorValues sensorValues = mockDatasetSensorValues();

    TimeCoordinate time = new TimeCoordinate(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));

    SensorValue longitudeSensorValue = new SensorValue(1L, 1L, flagScheme,
      SensorType.LONGITUDE_ID, time,
      null == longitude ? null : String.valueOf(longitude),
      new AutoQCResult(flagScheme), flagScheme.getAssumedGoodFlag(), null);

    sensorValues.add(longitudeSensorValue);

    SensorValue latitudeSensorValue = new SensorValue(2L, 1L, flagScheme,
      SensorType.LATITUDE_ID, time,
      null == latitude ? null : String.valueOf(latitude),
      new AutoQCResult(flagScheme), flagScheme.getAssumedGoodFlag(), null);

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

    DatasetSensorValues sensorValues = mockDatasetSensorValues();

    TimeCoordinate time = new TimeCoordinate(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));

    SensorValue latitudeSensorValue = new SensorValue(2L, 1L, flagScheme,
      SensorType.LATITUDE_ID, time, "0", new AutoQCResult(flagScheme),
      flagScheme.getAssumedGoodFlag(), null);

    sensorValues.add(latitudeSensorValue);

    PositionQCRoutine routine = new PositionQCRoutine(sensorValues);
    routine.qc(null, null);

    for (SensorValue sv : sensorValues.getAllPositionSensorValues()) {
      assertEquals(flagScheme.getBadFlag(), sv.getAutoQcFlag());
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

    DatasetSensorValues sensorValues = mockDatasetSensorValues();

    TimeCoordinate time = new TimeCoordinate(
      LocalDateTime.of(2025, 1, 1, 0, 0, 0));

    SensorValue longitudeSensorValue = new SensorValue(2L, 1L, flagScheme,
      SensorType.LONGITUDE_ID, time, "0", new AutoQCResult(flagScheme),
      flagScheme.getAssumedGoodFlag(), null);

    sensorValues.add(longitudeSensorValue);

    PositionQCRoutine routine = new PositionQCRoutine(sensorValues);
    routine.qc(null, null);

    for (SensorValue sv : sensorValues.getAllPositionSensorValues()) {
      assertEquals(flagScheme.getBadFlag(), sv.getAutoQcFlag());
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

    DatasetSensorValues sensorValues = mockDatasetSensorValues();

    SensorValue longitudeSensorValue = new SensorValue(2L, 1L, flagScheme,
      SensorType.LONGITUDE_ID,
      new TimeCoordinate(LocalDateTime.of(2025, 1, 1, 0, 0, 0)), "0",
      new AutoQCResult(flagScheme), flagScheme.getAssumedGoodFlag(), null);

    sensorValues.add(longitudeSensorValue);

    SensorValue latitudeSensorValue = new SensorValue(2L, 1L, flagScheme,
      SensorType.LATITUDE_ID,
      new TimeCoordinate(LocalDateTime.of(2025, 1, 1, 0, 1, 0)), "0",
      new AutoQCResult(flagScheme), flagScheme.getAssumedGoodFlag(), null);

    sensorValues.add(latitudeSensorValue);

    PositionQCRoutine routine = new PositionQCRoutine(sensorValues);
    routine.qc(null, null);

    for (SensorValue sv : sensorValues.getAllPositionSensorValues()) {
      assertEquals(flagScheme.getBadFlag(), sv.getAutoQcFlag());
    }

  }
}
