package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.TestSetLine;
import uk.ac.exeter.QuinCe.TestBase.TestSetTest;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

@TestInstance(Lifecycle.PER_CLASS)
@FlywayTest(locationsForMigrate = { "resources/sql/testbase/user" })
public class SpeedQCRoutineTest extends TestSetTest {

  private static final int LON_1_COL = 0;

  private static final int LAT_1_COL = 1;

  private static final int FLAG_1_COL = 2;

  private static final int LON_2_COL = 3;

  private static final int LAT_2_COL = 4;

  private static final int FLAG_2_COL = 5;

  private static final int LON_3_COL = 6;

  private static final int LAT_3_COL = 7;

  private static final int FLAG_3_COL = 8;

  private static final int QC_1_COL = 9;

  private static final int QC_2_COL = 10;

  private static final int QC_3_COL = 11;

  private static final List<Long> LON_COLUMN_IDS = Arrays
    .asList(new Long[] { FileDefinition.LONGITUDE_COLUMN_ID });

  private static final List<Long> LAT_COLUMN_IDS = Arrays
    .asList(new Long[] { FileDefinition.LATITUDE_COLUMN_ID });

  @Override
  protected String getTestSetName() {
    return "SpeedQCRoutine";
  }

  @BeforeAll
  public void init() {
    initResourceManager();
  }

  @ParameterizedTest
  @MethodSource("getLines")
  public void speedQCTest(TestSetLine line) throws Exception {

    SensorAssignments sensorAssignments = Mockito.mock(SensorAssignments.class);
    Mockito
      .when(sensorAssignments
        .getSensorTypeForDBColumn(FileDefinition.LONGITUDE_COLUMN_ID))
      .thenReturn(SensorType.LONGITUDE_SENSOR_TYPE);
    Mockito
      .when(sensorAssignments
        .getSensorTypeForDBColumn(FileDefinition.LATITUDE_COLUMN_ID))
      .thenReturn(SensorType.LATITUDE_SENSOR_TYPE);

    Instrument instrument = Mockito.mock(Instrument.class);
    Mockito.when(instrument.getSensorAssignments())
      .thenReturn(sensorAssignments);

    DatasetSensorValues allSensorValues = new DatasetSensorValues(instrument);

    boolean has1 = true;
    boolean has2 = false;
    boolean has3 = false;

    Flag expectedFlag1 = null;
    Flag expectedFlag2 = null;
    Flag expectedFlag3 = null;

    // Read in first position
    String lon1 = line.getStringField(LON_1_COL, true);
    String lat1 = line.getStringField(LAT_1_COL, true);
    Flag flag1 = line.getFlagField(FLAG_1_COL);

    SensorValue svLon1 = SVTestUtils.makeSensorValue(1L,
      FileDefinition.LONGITUDE_COLUMN_ID, 10, lon1, flag1);

    SensorValue svLat1 = SVTestUtils.makeSensorValue(2L,
      FileDefinition.LATITUDE_COLUMN_ID, 10, lat1, flag1);

    allSensorValues.add(svLon1);
    allSensorValues.add(svLat1);

    expectedFlag1 = line.getFlagField(QC_1_COL);

    // Read in second position
    String lon2 = line.getStringField(LON_2_COL, true);
    String lat2 = line.getStringField(LAT_2_COL, true);
    Flag flag2 = line.getFlagField(FLAG_2_COL);

    if (null != lon2) {
      SensorValue svLon2 = SVTestUtils.makeSensorValue(3L,
        FileDefinition.LONGITUDE_COLUMN_ID, 11, lon2, flag2);

      SensorValue svLat2 = SVTestUtils.makeSensorValue(4L,
        FileDefinition.LATITUDE_COLUMN_ID, 11, lat2, flag2);

      allSensorValues.add(svLon2);
      allSensorValues.add(svLat2);

      has2 = true;
      expectedFlag2 = line.getFlagField(QC_2_COL);
    }

    // Read in third position
    String lon3 = line.getStringField(LON_3_COL, true);
    String lat3 = line.getStringField(LAT_3_COL, true);
    Flag flag3 = line.getFlagField(FLAG_3_COL);

    if (null != lon3) {
      SensorValue svLon3 = SVTestUtils.makeSensorValue(5L,
        FileDefinition.LONGITUDE_COLUMN_ID, 12, lon3, flag3);

      SensorValue svLat3 = SVTestUtils.makeSensorValue(6L,
        FileDefinition.LATITUDE_COLUMN_ID, 12, lat3, flag3);

      allSensorValues.add(svLon3);
      allSensorValues.add(svLat3);

      has3 = true;
      expectedFlag3 = line.getFlagField(QC_3_COL);
    }

    // Run the QC Routine
    SpeedQCRoutine qcRoutine = new SpeedQCRoutine(allSensorValues);
    qcRoutine.qcAction(null);

    if (has1) {
      assertTrue(SVTestUtils.checkAutoQC(
        allSensorValues.getSensorValues(LON_COLUMN_IDS).getRawValues(),
        expectedFlag1, Arrays.asList(new Long[] { 1L })), "Lon1 Flag Wrong");

      assertTrue(SVTestUtils.checkAutoQC(
        allSensorValues.getSensorValues(LAT_COLUMN_IDS).getRawValues(),
        expectedFlag1, Arrays.asList(new Long[] { 2L })), "Lat1 Flag Wrong");
    }

    if (has2) {
      assertTrue(SVTestUtils.checkAutoQC(
        allSensorValues.getSensorValues(LON_COLUMN_IDS).getRawValues(),
        expectedFlag2, Arrays.asList(new Long[] { 3L })), "Lon2 Flag Wrong");

      assertTrue(SVTestUtils.checkAutoQC(
        allSensorValues.getSensorValues(LAT_COLUMN_IDS).getRawValues(),
        expectedFlag2, Arrays.asList(new Long[] { 4L })), "Lat2 Flag Wrong");
    }

    if (has3) {
      assertTrue(SVTestUtils.checkAutoQC(
        allSensorValues.getSensorValues(LON_COLUMN_IDS).getRawValues(),
        expectedFlag3, Arrays.asList(new Long[] { 5L })), "Lon3 Flag Wrong");

      assertTrue(SVTestUtils.checkAutoQC(
        allSensorValues.getSensorValues(LAT_COLUMN_IDS).getRawValues(),
        expectedFlag3, Arrays.asList(new Long[] { 6L })), "Lat3 Flag Wrong");
    }
  }
}
