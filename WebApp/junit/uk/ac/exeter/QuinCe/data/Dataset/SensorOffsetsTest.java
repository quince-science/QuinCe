package uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignmentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroup;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupPair;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class SensorOffsetsTest extends BaseTest {

  @BeforeEach
  public void init() {
    initResourceManager();
  }

  @FlywayTest
  @Test
  public void addOffset() throws Exception {
    SensorGroups sensorGroups = makeSensorGroups();
    List<SensorGroupPair> pairs = sensorGroups.getGroupPairs();

    SensorOffsets offsets = new SensorOffsets(sensorGroups);
    offsets.addOffset(pairs.get(0), LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      1000L);

    TreeSet<SensorOffset> pairOffsets = offsets.getOffsets(pairs.get(0));
    assertAll(() -> assertEquals(1, pairOffsets.size()),
      () -> assertEquals(LocalDateTime.of(2021, 01, 01, 12, 00, 00),
        pairOffsets.first().getTime()),
      () -> assertEquals(1000L, pairOffsets.first().getOffset()));
  }

  @FlywayTest
  @Test
  public void offsetsOrdered() throws Exception {
    SensorGroups sensorGroups = makeSensorGroups();
    List<SensorGroupPair> pairs = sensorGroups.getGroupPairs();

    SensorOffsets offsets = new SensorOffsets(sensorGroups);
    offsets.addOffset(pairs.get(0), LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      1000L);
    offsets.addOffset(pairs.get(0), LocalDateTime.of(2021, 01, 01, 10, 00, 00),
      1000L);

    TreeSet<SensorOffset> pairOffsets = offsets.getOffsets(pairs.get(0));
    LocalDateTime time1 = pairOffsets.first().getTime();
    LocalDateTime time2 = pairOffsets.last().getTime();

    assertAll(
      () -> assertEquals(LocalDateTime.of(2021, 01, 01, 10, 00, 00), time1),
      () -> assertEquals(LocalDateTime.of(2021, 01, 01, 12, 00, 00), time2));
  }

  @FlywayTest
  @Test
  public void duplicateOffsetTime() throws Exception {
    SensorGroups sensorGroups = makeSensorGroups();
    List<SensorGroupPair> pairs = sensorGroups.getGroupPairs();

    SensorOffsets offsets = new SensorOffsets(sensorGroups);
    offsets.addOffset(pairs.get(0), LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      1000L);

    assertThrows(SensorOffsetsException.class, () -> {
      offsets.addOffset(pairs.get(0),
        LocalDateTime.of(2021, 01, 01, 12, 00, 00), 1000L);
    });
  }

  @FlywayTest
  @Test
  public void deleteOffset() throws Exception {

    SensorGroups sensorGroups = makeSensorGroups();
    List<SensorGroupPair> pairs = sensorGroups.getGroupPairs();

    SensorOffsets offsets = new SensorOffsets(sensorGroups);
    offsets.addOffset(pairs.get(0), LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      1000L);
    offsets.addOffset(pairs.get(0), LocalDateTime.of(2021, 01, 01, 10, 00, 00),
      1000L);

    offsets.deleteOffset(pairs.get(0),
      LocalDateTime.of(2021, 01, 01, 12, 00, 00));

    assertAll(() -> assertEquals(1, offsets.getOffsets(pairs.get(0)).size()),
      () -> assertEquals(LocalDateTime.of(2021, 01, 01, 10, 00, 00),
        offsets.getOffsets(pairs.get(0)).first().getTime()));

  }

  @FlywayTest
  @Test
  public void getOffsetNoOffsets() throws Exception {
    SensorGroups sensorGroups = makeSensorGroups();
    SensorAssignment base = sensorGroups.getGroup("Group 2").getMembers()
      .first();
    SensorAssignment target = sensorGroups.getGroup("Group 1").getMembers()
      .first();
    SensorOffsets offsets = new SensorOffsets(sensorGroups);

    TimeCoordinate coordinate = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 12, 00, 00));

    DatasetSensorValues allSensorValues = Mockito
      .mock(DatasetSensorValues.class);
    Mockito.when(allSensorValues.getCoordinates())
      .thenReturn(Collections.singletonList(coordinate));

    assertEquals(coordinate,
      offsets.getOffsetTime(coordinate, base, target, allSensorValues));
  }

  @FlywayTest
  @ParameterizedTest
  @CsvSource({ "30, 29", "20, 19", "40, 39" })
  public void getOffsetSingleOffset(int minuteToOffset, int expectedMinute)
    throws Exception {
    SensorGroups sensorGroups = makeSensorGroups();
    SensorGroupPair groupPair = sensorGroups.getGroupPairs().get(0);
    SensorAssignment base = sensorGroups.getGroup("Group 2").getMembers()
      .first();
    SensorAssignment target = sensorGroups.getGroup("Group 1").getMembers()
      .first();
    SensorOffsets offsets = new SensorOffsets(sensorGroups);

    offsets.addOffset(groupPair, LocalDateTime.of(2021, 01, 01, 12, 30, 00),
      60000L);

    TimeCoordinate timeToOffset = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 12, minuteToOffset, 00));
    TimeCoordinate expectedTime = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 12, expectedMinute, 00));

    DatasetSensorValues allSensorValues = Mockito
      .mock(DatasetSensorValues.class);
    Mockito.when(allSensorValues.getCoordinates()).thenReturn(
      Arrays.asList(new Coordinate[] { timeToOffset, expectedTime }));

    assertEquals(expectedTime,
      offsets.getOffsetTime(timeToOffset, base, target, allSensorValues));
  }

  @FlywayTest
  @ParameterizedTest
  @CsvSource({ "8, 6", "10, 8", "20, 17", "30, 26", "50, 46" })
  public void getOffsetMultipleOffsets(int minuteToOffset, int expectedMinute)
    throws Exception {
    SensorGroups sensorGroups = makeSensorGroups();
    SensorGroupPair groupPair = sensorGroups.getGroupPairs().get(0);
    SensorAssignment base = sensorGroups.getGroup("Group 2").getMembers()
      .first();
    SensorAssignment target = sensorGroups.getGroup("Group 1").getMembers()
      .first();
    SensorOffsets offsets = new SensorOffsets(sensorGroups);

    offsets.addOffset(groupPair, LocalDateTime.of(2021, 01, 01, 18, 10, 00),
      120000L);
    offsets.addOffset(groupPair, LocalDateTime.of(2021, 01, 01, 18, 30, 00),
      240000L);

    TimeCoordinate timeToOffset = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 18, minuteToOffset, 00));
    TimeCoordinate expectedTime = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 18, expectedMinute, 00));

    DatasetSensorValues allSensorValues = Mockito
      .mock(DatasetSensorValues.class);
    Mockito.when(allSensorValues.getCoordinates()).thenReturn(
      Arrays.asList(new Coordinate[] { timeToOffset, expectedTime }));

    assertEquals(expectedTime,
      offsets.getOffsetTime(timeToOffset, base, target, allSensorValues));
  }

  @FlywayTest
  @ParameterizedTest
  @CsvSource({ "8, 10", "10, 12", "20, 23", "30, 34", "50, 54" })
  public void getReverseOffsetMultipleOffsets(int minuteToOffset,
    int expectedMinute) throws Exception {
    SensorGroups sensorGroups = makeSensorGroups();
    SensorGroupPair groupPair = sensorGroups.getGroupPairs().get(0);
    SensorAssignment base = sensorGroups.getGroup("Group 1").getMembers()
      .first();
    SensorAssignment target = sensorGroups.getGroup("Group 2").getMembers()
      .first();
    SensorOffsets offsets = new SensorOffsets(sensorGroups);

    offsets.addOffset(groupPair, LocalDateTime.of(2021, 01, 01, 18, 10, 00),
      120000L);
    offsets.addOffset(groupPair, LocalDateTime.of(2021, 01, 01, 18, 30, 00),
      240000L);

    TimeCoordinate timeToOffset = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 18, minuteToOffset, 00));
    TimeCoordinate expectedTime = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 18, expectedMinute, 00));

    DatasetSensorValues allSensorValues = Mockito
      .mock(DatasetSensorValues.class);
    Mockito.when(allSensorValues.getCoordinates()).thenReturn(
      Arrays.asList(new Coordinate[] { timeToOffset, expectedTime }));

    assertEquals(expectedTime,
      offsets.getOffsetTime(timeToOffset, base, target, allSensorValues));
  }

  @FlywayTest
  @ParameterizedTest
  @CsvSource({ "3, 1, -7", "3, 2, -2", "2, 1, -5", "1, 2, 5", "1, 3, 7",
    "2, 3, 2" })
  public void getOffsetAcrossGroups(int baseSensor, int targetSensor,
    long expectedOffset) throws Exception {

    SensorGroups sensorGroups = makeSensorGroups();
    SensorOffsets offsets = new SensorOffsets(sensorGroups);

    // Set offsets for group pairs
    SensorGroupPair pair1 = sensorGroups.getGroupPairs().get(0);
    offsets.addOffset(pair1, LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      300000L);

    SensorGroupPair pair2 = sensorGroups.getGroupPairs().get(1);
    offsets.addOffset(pair2, LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      120000L);

    SensorAssignment base = sensorGroups.getGroup("Group " + baseSensor)
      .getMembers().first();
    SensorAssignment target = sensorGroups.getGroup("Group " + targetSensor)
      .getMembers().first();

    TimeCoordinate timeToOffset = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 18, 30, 00));

    DatasetSensorValues allSensorValues = Mockito
      .mock(DatasetSensorValues.class);
    Mockito.when(allSensorValues.getCoordinates())
      .thenReturn(Collections.singletonList(timeToOffset));

    TimeCoordinate offsetTime = offsets.getOffsetTime(timeToOffset, base,
      target, allSensorValues);

    long minutesDifference = ChronoUnit.MINUTES.between(timeToOffset.getTime(),
      offsetTime.getTime());

    assertEquals(expectedOffset, minutesDifference);
  }

  @FlywayTest
  @ParameterizedTest
  @CsvSource({ "1, 0", "2, -5", "3, -7" })
  public void getOffsetToFirstGroup(int baseSensor, long expectedOffset)
    throws Exception {

    SensorGroups sensorGroups = makeSensorGroups();
    SensorOffsets offsets = new SensorOffsets(sensorGroups);

    // Set offsets for group pairs
    SensorGroupPair pair1 = sensorGroups.getGroupPairs().get(0);
    offsets.addOffset(pair1, LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      300000L);

    SensorGroupPair pair2 = sensorGroups.getGroupPairs().get(1);
    offsets.addOffset(pair2, LocalDateTime.of(2021, 01, 01, 12, 00, 00),
      120000L);

    SensorAssignment base = sensorGroups.getGroup("Group " + baseSensor)
      .getMembers().first();

    TimeCoordinate timeToOffset = new TimeCoordinate(
      LocalDateTime.of(2021, 01, 01, 18, 30, 00));

    DatasetSensorValues allSensorValues = Mockito
      .mock(DatasetSensorValues.class);
    Mockito.when(allSensorValues.getCoordinates())
      .thenReturn(Collections.singletonList(timeToOffset));

    TimeCoordinate offsetTime = offsets.offsetToFirstGroup(timeToOffset, base,
      allSensorValues);

    long minutesDifference = ChronoUnit.MINUTES.between(timeToOffset.getTime(),
      offsetTime.getTime());

    assertEquals(expectedOffset, minutesDifference);
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void sensorValuesOffsets() throws Exception {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    SensorGroups sensorGroups = new SensorGroups();
    sensorGroups.renameGroup("Default", "Group 1");
    sensorGroups.addGroup("Group 2", "Group 1");

    sensorGroups.addAssignment(makeAssignment("Sensor 1", "File 1", 1,
      sensorConfig.getSensorType("Water Temperature")));
    sensorGroups.addAssignment(makeAssignment("Sensor 2", "File 1", 2,
      sensorConfig.getSensorType("Salinity")));
    sensorGroups.moveSensor("Sensor 2", "Group 2");

    SensorOffsets offsets = new SensorOffsets(sensorGroups);
    SensorGroupPair pair = sensorGroups.getGroupPairs().get(0);
    offsets.addOffset(pair, LocalDateTime.of(2021, 01, 01, 13, 20, 00),
      120000L);
    offsets.addOffset(pair, LocalDateTime.of(2021, 01, 01, 13, 40, 00),
      360000L);

    List<SensorValue> sensorValues = new ArrayList<SensorValue>();
    sensorValues.add(makeSensorValue(10, flagScheme.getGoodFlag()));
    sensorValues.add(makeSensorValue(20, flagScheme.getGoodFlag()));
    sensorValues.add(makeSensorValue(30, flagScheme.getGoodFlag()));
    sensorValues.add(makeSensorValue(40, flagScheme.getGoodFlag()));
    sensorValues.add(makeSensorValue(50, flagScheme.getGoodFlag()));

    Instrument instrument = InstrumentDB.getInstrument(getConnection(), 1L);
    DatasetSensorValues allSensorValues = new DatasetSensorValues(
      Mockito.mock(DataSet.class));
    Mockito.when(allSensorValues.getInstrument()).thenReturn(instrument);
    Mockito.when(allSensorValues.getDatasetId()).thenReturn(1L);
    Mockito.when(allSensorValues.getFlagScheme()).thenReturn(flagScheme);

    for (SensorValue sensorValue : sensorValues) {
      allSensorValues.add(sensorValue);
    }

    List<SensorValue> appliedOffsets = offsets.applyOffsets(pair, sensorValues,
      allSensorValues);

    assertAll(
      () -> assertEquals(8,
        appliedOffsets.get(0).getCoordinate().getTime()
          .get(ChronoField.MINUTE_OF_HOUR)),
      () -> assertEquals(18,
        appliedOffsets.get(1).getCoordinate().getTime()
          .get(ChronoField.MINUTE_OF_HOUR)),
      () -> assertEquals(26,
        appliedOffsets.get(2).getCoordinate().getTime()
          .get(ChronoField.MINUTE_OF_HOUR)),
      () -> assertEquals(34,
        appliedOffsets.get(3).getCoordinate().getTime()
          .get(ChronoField.MINUTE_OF_HOUR)),
      () -> assertEquals(44, appliedOffsets.get(4).getCoordinate().getTime()
        .get(ChronoField.MINUTE_OF_HOUR)));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void sensorValuesOffsetsFilterNotGood() throws Exception {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    SensorGroups sensorGroups = new SensorGroups();
    sensorGroups.renameGroup("Default", "Group 1");
    sensorGroups.addGroup("Group 2", "Group 1");

    sensorGroups.addAssignment(makeAssignment("Sensor 1", "File 1", 1,
      sensorConfig.getSensorType("Water Temperature")));
    sensorGroups.addAssignment(makeAssignment("Sensor 2", "File 1", 2,
      sensorConfig.getSensorType("Salinity")));
    sensorGroups.moveSensor("Sensor 2", "Group 2");

    SensorOffsets offsets = new SensorOffsets(sensorGroups);
    SensorGroupPair pair = sensorGroups.getGroupPairs().get(0);
    offsets.addOffset(pair, LocalDateTime.of(2021, 01, 01, 13, 20, 00),
      120000L);
    offsets.addOffset(pair, LocalDateTime.of(2021, 01, 01, 13, 40, 00),
      360000L);

    List<SensorValue> sensorValues = new ArrayList<SensorValue>();
    sensorValues.add(makeSensorValue(10, flagScheme.getGoodFlag()));
    sensorValues.add(makeSensorValue(20, flagScheme.getGoodFlag()));
    sensorValues.add(makeSensorValue(30, flagScheme.getBadFlag()));
    sensorValues.add(makeSensorValue(40, flagScheme.getGoodFlag()));
    sensorValues.add(makeSensorValue(50, IcosFlagScheme.QUESTIONABLE_FLAG));

    Instrument instrument = InstrumentDB.getInstrument(getConnection(), 1L);
    DatasetSensorValues allSensorValues = new DatasetSensorValues(
      Mockito.mock(DataSet.class));
    Mockito.when(allSensorValues.getInstrument()).thenReturn(instrument);
    Mockito.when(allSensorValues.getDatasetId()).thenReturn(1L);
    Mockito.when(allSensorValues.getFlagScheme()).thenReturn(flagScheme);

    for (SensorValue sensorValue : sensorValues) {
      allSensorValues.add(sensorValue);
    }

    List<SensorValue> appliedOffsets = offsets.applyOffsets(pair, sensorValues,
      allSensorValues);

    assertAll(() -> assertEquals(3, appliedOffsets.size()),
      () -> assertEquals(8,
        appliedOffsets.get(0).getCoordinate().getTime()
          .get(ChronoField.MINUTE_OF_HOUR)),
      () -> assertEquals(18,
        appliedOffsets.get(1).getCoordinate().getTime()
          .get(ChronoField.MINUTE_OF_HOUR)),
      () -> assertEquals(34, appliedOffsets.get(2).getCoordinate().getTime()
        .get(ChronoField.MINUTE_OF_HOUR)));
  }

  private SensorGroups makeSensorGroups() throws Exception {
    SensorGroups groups = new SensorGroups();

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    groups.renameGroup("Default", "Group 1");
    groups.addGroup("Group 2", "Group 1");
    groups.addGroup("Group 3", "Group 2");

    groups.addAssignment(makeAssignment("Sensor 1", "File 1", 1,
      sensorConfig.getSensorType("Water Temperature")));
    groups.addAssignment(makeAssignment("Sensor 2", "File 1", 2,
      sensorConfig.getSensorType("Salinity")));
    groups.addAssignment(makeAssignment("Sensor 3", "File 1", 3,
      sensorConfig.getSensorType("Air Temperature")));

    groups.moveSensor("Sensor 1", "Group 1");
    groups.moveSensor("Sensor 2", "Group 2");
    groups.moveSensor("Sensor 3", "Group 3");

    groups.getGroup("Group 1").setLink("Sensor 1", SensorGroup.NEXT);
    groups.getGroup("Group 2").setLink("Sensor 2", SensorGroup.PREVIOUS);
    groups.getGroup("Group 2").setLink("Sensor 2", SensorGroup.NEXT);
    groups.getGroup("Group 3").setLink("Sensor 3", SensorGroup.PREVIOUS);
    groups.getGroup("Group 3").setLink("Sensor 3", SensorGroup.NEXT);

    return groups;
  }

  private SensorAssignment makeAssignment(String name, String file, int column,
    SensorType sensorType) throws SensorAssignmentException {
    return new SensorAssignment(file, column, sensorType, name, true, false,
      null);
  }

  private SensorValue makeSensorValue(int minute, Flag flag) {
    SensorValue sensorValue = Mockito.mock(SensorValue.class);
    Mockito.when(sensorValue.getCoordinate()).thenReturn(
      new TimeCoordinate(LocalDateTime.of(2021, 01, 01, 13, minute, 00)));
    Mockito.when(sensorValue.getUserQCFlag()).thenReturn(flag);
    return sensorValue;
  }
}
