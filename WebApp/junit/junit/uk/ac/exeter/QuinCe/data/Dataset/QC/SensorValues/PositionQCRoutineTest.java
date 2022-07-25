package junit.uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
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

    List<SensorValue> list = new ArrayList<SensorValue>();

    long start = System.currentTimeMillis();
    LocalDateTime startTime = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    for (int i = 0; i < size; i++) {
      list.add(new SensorValue(Math.abs(columnId) * 1000 + i, start + i,
        columnId, startTime.plusSeconds(i), String.valueOf(startValue + i),
        new AutoQCResult(), Flag.ASSUMED_GOOD, ""));
    }

    return list;
  }

  private DatasetSensorValues makeDatasetSensorValues(int lonSize, int latSize)
    throws Exception {

    return makeDatasetSensorValues(
      makeSensorValueList(SensorType.LONGITUDE_ID, lonSize, 0D),
      makeSensorValueList(SensorType.LATITUDE_ID, latSize, 0D));
  }

  private DatasetSensorValues makeDatasetSensorValues(List<SensorValue> lons,
    List<SensorValue> lats) throws Exception {

    DatasetSensorValues datasetSensorValues = new DatasetSensorValues(
      makeInstrument());

    for (SensorValue lon : lons) {
      datasetSensorValues.add(lon);
    }
    for (SensorValue lat : lats) {
      datasetSensorValues.add(lat);
    }

    return datasetSensorValues;
  }

  @Test
  public void missingInstrumentTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(null, makeDatasetSensorValues(1, 1),
        makeEmptyRunTypes());
    });
  }

  @Test
  public void missingSensorValuesTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(makeInstrument(), null, makeEmptyRunTypes());
    });
  }

  @Test
  public void missingRunTypesTest() {
    assertThrows(MissingParamException.class, () -> {
      new PositionQCRoutine(makeInstrument(), makeDatasetSensorValues(1, 1),
        null);
    });
  }

  @Test
  public void passingQCTest() throws Exception {
    PositionQCRoutine routine = new PositionQCRoutine(makeInstrument(),
      makeDatasetSensorValues(1, 1), makeEmptyRunTypes());

    routine.qc(null);
  }

  @Test
  public void emptyQCTest() throws Exception {
    PositionQCRoutine routine = new PositionQCRoutine(makeInstrument(),
      makeDatasetSensorValues(0, 0), makeEmptyRunTypes());

    routine.qc(null);
  }

  public void validLonTest() throws Exception {
    SensorValue lonValue = new SensorValue(1000L, 1L, SensorType.LONGITUDE_ID,
      LocalDateTime.of(2000, 1, 1, 0, 0, 0), "0.0", new AutoQCResult(),
      Flag.ASSUMED_GOOD, "");
    List<SensorValue> lons = new ArrayList<SensorValue>(1);
    lons.add(lonValue);

    PositionQCRoutine routine = new PositionQCRoutine(makeInstrument(),
      makeDatasetSensorValues(lons,
        makeSensorValueList(SensorType.LATITUDE_ID, 1, 0D)),
      makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.ASSUMED_GOOD, lonValue.getUserQCFlag());
  }

  public void invalidLonTest() throws Exception {
    SensorValue lonValue = new SensorValue(1000L, 1L, SensorType.LONGITUDE_ID,
      LocalDateTime.of(2000, 1, 1, 0, 0, 0), "9000.0", new AutoQCResult(),
      Flag.BAD, "Bad");
    List<SensorValue> lons = new ArrayList<SensorValue>(1);
    lons.add(lonValue);

    PositionQCRoutine routine = new PositionQCRoutine(makeInstrument(),
      makeDatasetSensorValues(lons,
        makeSensorValueList(SensorType.LATITUDE_ID, 1, 0D)),
      makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.BAD, lonValue.getUserQCFlag());
  }

  public void validLatTest() throws Exception {

    SensorValue latValue = new SensorValue(1000L, 1L, SensorType.LATITUDE_ID,
      LocalDateTime.of(2000, 1, 1, 0, 0, 0), "0.0", new AutoQCResult(),
      Flag.ASSUMED_GOOD, "");
    List<SensorValue> lats = new ArrayList<SensorValue>(1);
    lats.add(latValue);

    PositionQCRoutine routine = new PositionQCRoutine(makeInstrument(),
      makeDatasetSensorValues(
        makeSensorValueList(SensorType.LONGITUDE_ID, 1, 0D), lats),
      makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.ASSUMED_GOOD, latValue.getUserQCFlag());
  }

  public void invalidLatTest() throws Exception {

    SensorValue latValue = new SensorValue(1000L, 1L, SensorType.LATITUDE_ID,
      LocalDateTime.of(2000, 1, 1, 0, 0, 0), "9000.0", new AutoQCResult(),
      Flag.BAD, "");
    List<SensorValue> lats = new ArrayList<SensorValue>(1);
    lats.add(latValue);

    PositionQCRoutine routine = new PositionQCRoutine(makeInstrument(),
      makeDatasetSensorValues(
        makeSensorValueList(SensorType.LONGITUDE_ID, 1, 0D), lats),
      makeEmptyRunTypes());

    routine.qc(null);

    assertEquals(Flag.BAD, latValue.getUserQCFlag());
  }

}
