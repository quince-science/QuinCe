package junit.uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import junit.uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.PositionQCRoutine;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

@TestInstance(Lifecycle.PER_CLASS)
public class PositionQCCalibrationFlagTest extends TestSetTest {

  private static final int MINUTE_COL = 0;

  private static final int POSITION_VALID_COL = 1;

  private static final int RUN_TYPE_COL = 2;

  private static final int EXPECT_BAD_FLAG_COL = 3;

  private static final String MEASUREMENT_RUN_TYPE = "M";

  private static final long DATASET_ID = 1L;

  private static final long SENSOR_ID = 1L;

  private LocalDateTime makeTime(int minute) {
    return LocalDateTime.of(2000, 1, 1, 10, minute, 0);
  }

  private List<SensorValue> makeSensorValueList(long columnId, int minute,
    boolean empty) {

    return Arrays.asList(makeSensorValue(columnId, minute, empty));
  }

  private SensorValue makeSensorValue(long columnId, int minute,
    boolean empty) {
    return new SensorValue(DATASET_ID, columnId, makeTime(minute),
      empty ? null : "24");
  }

  private Instrument makeInstrument() throws RecordNotFoundException {

    // SensorAssignemnts
    SensorAssignments sensorAssignments = Mockito.mock(SensorAssignments.class);
    Mockito.when(sensorAssignments.getSensorColumnIds())
      .thenReturn(Arrays.asList(SENSOR_ID));

    // The mock will always return the same SensorType, with
    // hasInternalCalibrations() set as required in the method call.
    SensorType sensorType = Mockito.mock(SensorType.class);
    Mockito.when(sensorType.hasInternalCalibration()).thenReturn(true);

    Mockito.when(sensorAssignments.getSensorTypeForDBColumn(Mockito.anyLong()))
      .thenReturn(sensorType);

    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getSensorAssignments())
      .thenReturn(sensorAssignments);
    Mockito.when(instrument.getMeasurementRunTypes())
      .thenReturn(Arrays.asList(MEASUREMENT_RUN_TYPE));

    return instrument;
  }

  private DatasetSensorValues makeDatasetSensorValues(int minute,
    String runType) {

    SearchableSensorValuesList sensorValues = new SearchableSensorValuesList(
      SENSOR_ID);
    sensorValues.add(makeSensorValue(SENSOR_ID, minute, false));

    DatasetSensorValues datasetSensorValues = Mockito
      .mock(DatasetSensorValues.class);

    // Respond to getColumnValues with an empty list
    Mockito.when(datasetSensorValues.getColumnValues(SENSOR_ID))
      .thenAnswer(invocation -> {
        return sensorValues;
      });

    return datasetSensorValues;
  }

  private SearchableSensorValuesList makeRunTypes(int minute, String runType) {
    SearchableSensorValuesList runTypes = new SearchableSensorValuesList(
      SensorType.RUN_TYPE_ID);
    runTypes.add(new SensorValue(DATASET_ID, SensorType.RUN_TYPE_ID,
      makeTime(minute), runType));
    return runTypes;
  }

  @ParameterizedTest
  @MethodSource("getLines")
  public void runTypeFlagTest(TestSetLine line) throws Exception {

    int minute = line.getIntField(MINUTE_COL);
    boolean posValid = line.getBooleanField(POSITION_VALID_COL);
    String runType = line.getStringField(RUN_TYPE_COL, true);
    Flag expectedFlag = line.getFlagField(EXPECT_BAD_FLAG_COL);

    DatasetSensorValues sensorValues = makeDatasetSensorValues(minute, runType);

    PositionQCRoutine routine = new PositionQCRoutine(
      makeSensorValueList(SensorType.LONGITUDE_ID, minute, !posValid),
      makeSensorValueList(SensorType.LATITUDE_ID, minute, !posValid),
      makeInstrument(), sensorValues, makeRunTypes(minute, runType));

    routine.qc(null);

    SensorValue sensorValue = sensorValues.getColumnValues(SENSOR_ID)
      .get(makeTime(minute));
    assertEquals(expectedFlag, sensorValue.getUserQCFlag());
  }

  @Override
  protected String getTestSetName() {
    return "PositionQC_CalibrationFlag";
  }
}
