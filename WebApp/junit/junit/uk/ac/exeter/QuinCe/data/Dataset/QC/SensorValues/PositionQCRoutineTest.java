package junit.uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.PositionQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class PositionQCRoutineTest extends PositionQCTestBase {

  protected List<Long> makeDataColumnIds() {
    List<Long> ids = new ArrayList<Long>();
    ids.add(10L);
    ids.add(11L);
    return ids;
  }

  private List<SensorValue> makeSensorValueList(long columnId, int size,
    double startValue) {
    List<SensorValue> list = new ArrayList<SensorValue>(size);

    long start = System.currentTimeMillis();
    LocalDateTime startTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    for (int i = 0; i < size; i++) {
      list.add(new SensorValue(start + i, columnId, startTime.plusSeconds(i),
        String.valueOf(startValue + i)));
    }

    return list;
  }

  private DatasetSensorValues makeDatasetSensorValues() {
    DatasetSensorValues datasetSensorValues = Mockito
      .mock(DatasetSensorValues.class);

    // Respond to getColumnValues with an empty list
    Mockito.when(datasetSensorValues.getColumnValues(Mockito.anyLong()))
      .thenAnswer(invocation -> {
        return new SearchableSensorValuesList(
          invocation.getArgument(0, Long.class));
      });

    return datasetSensorValues;
  }

  /**
   * Make a {@link SensorValue} with an empty value but good QC flags.
   *
   * @param columnId
   *          The column ID for the SensorValue
   * @return The SensorValue
   */
  private SensorValue makeMissingSensorValue(long columnId) {
    SensorValue result = new SensorValue(1L, columnId, LocalDateTime.now(),
      null);
    result.setUserQC(Flag.GOOD, "");
    return result;
  }

  @Test
  public void missingLonsTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(null,
        makeSensorValueList(SensorType.LATITUDE_ID, 2, 0D), makeInstrument(),
        makeDatasetSensorValues(), makeEmptyRunTypes());
    });
  }

  @Test
  public void missingLatsTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(makeSensorValueList(SensorType.LONGITUDE_ID, 2, 0D),
        null, makeInstrument(), makeDatasetSensorValues(), makeEmptyRunTypes());
    });
  }

  @Test
  public void missingInstrumentTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(makeSensorValueList(SensorType.LONGITUDE_ID, 2, 0D),
        makeSensorValueList(SensorType.LATITUDE_ID, 2, 0D), null,
        makeDatasetSensorValues(), makeEmptyRunTypes());
    });
  }

  @Test
  public void missingSensorValuesTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(makeSensorValueList(SensorType.LONGITUDE_ID, 2, 0D),
        makeSensorValueList(SensorType.LATITUDE_ID, 2, 0D), makeInstrument(),
        null, makeEmptyRunTypes());
    });
  }

  @Test
  public void missingRunTypesTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(makeSensorValueList(SensorType.LONGITUDE_ID, 2, 0D),
        makeSensorValueList(SensorType.LATITUDE_ID, 2, 0D), makeInstrument(),
        makeDatasetSensorValues(), null);
    });
  }

  @Test
  public void mismatchedLonsLatsTest() {
    List<SensorValue> lons = makeSensorValueList(SensorType.LONGITUDE_ID, 2,
      0D);
    lons.remove(0);

    assertThrows(RoutineException.class, () -> {
      new PositionQCRoutine(lons,
        makeSensorValueList(SensorType.LATITUDE_ID, 2, 0D), makeInstrument(),
        makeDatasetSensorValues(), makeEmptyRunTypes());
    });
  }

  @Test
  public void passingQCTest() throws Exception {
    PositionQCRoutine routine = new PositionQCRoutine(
      makeSensorValueList(SensorType.LONGITUDE_ID, 2, 0D),
      makeSensorValueList(SensorType.LATITUDE_ID, 2, 0D), makeInstrument(),
      makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(new ArrayList<SensorValue>());
  }

  @Test
  public void emptyQCTest() throws Exception {
    PositionQCRoutine routine = new PositionQCRoutine(
      new ArrayList<SensorValue>(), new ArrayList<SensorValue>(),
      makeInstrument(), makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);
  }

  @Test
  public void missingLonTest() throws Exception {

    SensorValue missingLon = makeMissingSensorValue(SensorType.LONGITUDE_ID);
    List<SensorValue> missingLons = new ArrayList<SensorValue>();
    missingLons.add(missingLon);

    List<SensorValue> lats = makeSensorValueList(SensorType.LATITUDE_ID, 1, 0D);
    SensorValue lat = lats.get(0);

    PositionQCRoutine routine = new PositionQCRoutine(missingLons, lats,
      makeInstrument(), makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);
    assertEquals(Flag.BAD, missingLon.getUserQCFlag());
    assertEquals(Measurement.POSITION_QC_PREFIX + "Missing",
      missingLon.getUserQCMessage());
    assertEquals(Flag.BAD, lat.getUserQCFlag());
    assertEquals(Measurement.POSITION_QC_PREFIX + "Missing",
      lat.getUserQCMessage());
  }

  @Test
  public void missingLatTest() throws Exception {

    List<SensorValue> lons = makeSensorValueList(SensorType.LONGITUDE_ID, 1,
      0D);
    SensorValue lon = lons.get(0);

    SensorValue missingLat = makeMissingSensorValue(SensorType.LATITUDE_ID);
    List<SensorValue> missingLats = new ArrayList<SensorValue>();
    missingLats.add(missingLat);

    PositionQCRoutine routine = new PositionQCRoutine(lons, missingLats,
      makeInstrument(), makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);
    assertEquals(Flag.BAD, lon.getUserQCFlag());
    assertEquals(Measurement.POSITION_QC_PREFIX + "Missing",
      lon.getUserQCMessage());
    assertEquals(Flag.BAD, missingLat.getUserQCFlag());
    assertEquals(Measurement.POSITION_QC_PREFIX + "Missing",
      missingLat.getUserQCMessage());
  }

  @Test
  public void missingLatLonTest() throws Exception {

    SensorValue missingLon = makeMissingSensorValue(SensorType.LONGITUDE_ID);
    List<SensorValue> missingLons = new ArrayList<SensorValue>();
    missingLons.add(missingLon);

    SensorValue missingLat = makeMissingSensorValue(SensorType.LATITUDE_ID);
    List<SensorValue> missingLats = new ArrayList<SensorValue>();
    missingLats.add(missingLat);

    PositionQCRoutine routine = new PositionQCRoutine(missingLons, missingLats,
      makeInstrument(), makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);
    assertEquals(Flag.BAD, missingLon.getUserQCFlag());
    assertEquals(Measurement.POSITION_QC_PREFIX + "Missing",
      missingLon.getUserQCMessage());
    assertEquals(Flag.BAD, missingLat.getUserQCFlag());
    assertEquals(Measurement.POSITION_QC_PREFIX + "Missing",
      missingLat.getUserQCMessage());
  }

  @ParameterizedTest
  @ValueSource(doubles = { -180, -179.999, -100.252, 0, 4.54, 179.999, 180 })
  public void validLonTest(double lon) throws Exception {

    SensorValue lonValue = new SensorValue(1L, SensorType.LONGITUDE_ID,
      LocalDateTime.now(), String.valueOf(lon));
    List<SensorValue> lons = new ArrayList<SensorValue>(1);
    lons.add(lonValue);

    PositionQCRoutine routine = new PositionQCRoutine(lons,
      makeSensorValueList(SensorType.LATITUDE_ID, 1, 0D), makeInstrument(),
      makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.ASSUMED_GOOD, lonValue.getUserQCFlag());
  }

  @ParameterizedTest
  @ValueSource(doubles = { -180.0001, -200, 200, 180.001, 500 })
  public void invalidLonTest(double lon) throws Exception {

    SensorValue lonValue = new SensorValue(1L, SensorType.LONGITUDE_ID,
      LocalDateTime.now(), String.valueOf(lon));
    List<SensorValue> lons = new ArrayList<SensorValue>(1);
    lons.add(lonValue);

    PositionQCRoutine routine = new PositionQCRoutine(lons,
      makeSensorValueList(SensorType.LATITUDE_ID, 1, 0D), makeInstrument(),
      makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.BAD, lonValue.getUserQCFlag());
  }

  @ParameterizedTest
  @ValueSource(doubles = { -90, -89.999, -40.252, 0, 4.54, 89.999, 90 })
  public void validLatTest(double lat) throws Exception {

    SensorValue latValue = new SensorValue(1L, SensorType.LONGITUDE_ID,
      LocalDateTime.now(), String.valueOf(lat));
    List<SensorValue> lats = new ArrayList<SensorValue>(1);
    lats.add(latValue);

    PositionQCRoutine routine = new PositionQCRoutine(
      makeSensorValueList(SensorType.LONGITUDE_ID, 1, 0D), lats,
      makeInstrument(), makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.ASSUMED_GOOD, latValue.getUserQCFlag());
  }

  @ParameterizedTest
  @ValueSource(doubles = { -90.001, -100, -180, 180, 223.424, 90.001 })
  public void invalidLatTest(double lat) throws Exception {

    SensorValue latValue = new SensorValue(1L, SensorType.LONGITUDE_ID,
      LocalDateTime.now(), String.valueOf(lat));
    List<SensorValue> lats = new ArrayList<SensorValue>(1);
    lats.add(latValue);

    PositionQCRoutine routine = new PositionQCRoutine(
      makeSensorValueList(SensorType.LONGITUDE_ID, 1, 0D), lats,
      makeInstrument(), makeDatasetSensorValues(), makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.BAD, latValue.getUserQCFlag());
  }
}
