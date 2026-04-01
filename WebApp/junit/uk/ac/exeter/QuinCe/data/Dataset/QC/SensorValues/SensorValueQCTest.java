package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.CoordinateException;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.TimeCoordinate;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.IcosFlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class SensorValueQCTest extends BaseTest {

  private static final long DATASET_ID = 1L;

  private SensorValue makeSensorValue(long id, long columnId)
    throws CoordinateException {
    return makeSensorValue(id, columnId, "12", flagScheme.getAssumedGoodFlag());
  }

  private SensorValue makeSensorValue(long id, long columnId, String value,
    Flag flag) throws CoordinateException {
    return new SensorValue(id, DATASET_ID, flagScheme, columnId,
      new TimeCoordinate(DATASET_ID, LocalDateTime.now(ZoneId.of("Z"))), value,
      null, flag, null);
  }

  private RoutineFlag makeAutoQCFlag(Flag flag) {
    RangeCheckRoutine routine = Mockito.mock(RangeCheckRoutine.class);
    Mockito.when(routine.getName()).thenReturn("SensorValues.RangeCheck");

    return new RoutineFlag(flagScheme, routine, flag, "0", "1");
  }

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void defaultQCTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    assertEquals(flagScheme.getGoodFlag(), value.getAutoQcFlag());
    assertEquals(flagScheme.getAssumedGoodFlag(), value.getUserQCFlag());
    assertEquals(flagScheme.getAssumedGoodFlag(),
      value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void autoQCBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(flagScheme.getBadFlag()));
    assertEquals(flagScheme.getBadFlag(), value.getAutoQcFlag());
    assertEquals(FlagScheme.NEEDED_FLAG, value.getUserQCFlag());
    assertEquals(flagScheme.getBadFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void autoQCQuestionableTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(IcosFlagScheme.QUESTIONABLE_FLAG));
    assertEquals(IcosFlagScheme.QUESTIONABLE_FLAG, value.getAutoQcFlag());
    assertEquals(FlagScheme.NEEDED_FLAG, value.getUserQCFlag());
    assertEquals(IcosFlagScheme.QUESTIONABLE_FLAG,
      value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void autoQCQuestionableBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(IcosFlagScheme.QUESTIONABLE_FLAG));
    value.addAutoQCFlag(makeAutoQCFlag(flagScheme.getBadFlag()));
    assertEquals(flagScheme.getBadFlag(), value.getAutoQcFlag());
    assertEquals(FlagScheme.NEEDED_FLAG, value.getUserQCFlag());
    assertEquals(flagScheme.getBadFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest
  @Test
  public void clearAutoQCNoUserQC()
    throws RecordNotFoundException, RoutineException, CoordinateException {
    SensorValue value = makeSensorValue(1L, 1L);
    value.addAutoQCFlag(makeAutoQCFlag(flagScheme.getBadFlag()));
    value.clearAutomaticQC();
    assertEquals(flagScheme.getAssumedGoodFlag(), value.getUserQCFlag());
  }

  @FlywayTest
  @Test
  public void clearAutoQCWithUserQC() throws RecordNotFoundException,
    RoutineException, InvalidFlagException, CoordinateException {
    SensorValue value = makeSensorValue(1L, 1L);
    value.addAutoQCFlag(makeAutoQCFlag(flagScheme.getBadFlag()));
    value.setUserQC(IcosFlagScheme.QUESTIONABLE_FLAG, "Q");
    value.clearAutomaticQC();
    assertEquals(IcosFlagScheme.QUESTIONABLE_FLAG, value.getUserQCFlag());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void userQCTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.setUserQC(flagScheme.getBadFlag(), "BAD");
    assertEquals(flagScheme.getGoodFlag(), value.getAutoQcFlag());
    assertEquals(flagScheme.getBadFlag(), value.getUserQCFlag());
    assertEquals(flagScheme.getBadFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void userThenAutoQCTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.setUserQC(flagScheme.getBadFlag(), "BAD");
    value.addAutoQCFlag(makeAutoQCFlag(IcosFlagScheme.QUESTIONABLE_FLAG));
    assertEquals(IcosFlagScheme.QUESTIONABLE_FLAG, value.getAutoQcFlag());
    assertEquals(flagScheme.getBadFlag(), value.getUserQCFlag());
    assertEquals(flagScheme.getBadFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void userOverrideWithLessSignificantTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(flagScheme.getBadFlag()));
    value.setUserQC(flagScheme.getGoodFlag(), "");
    assertEquals(flagScheme.getBadFlag(), value.getAutoQcFlag());
    assertEquals(flagScheme.getGoodFlag(), value.getUserQCFlag());
    assertEquals(flagScheme.getGoodFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void emptyValueBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L, "",
      flagScheme.getAssumedGoodFlag());
    allValues.add(value);

    assertEquals(flagScheme.getBadFlag(), value.getUserQCFlag());
    assertEquals(flagScheme.getBadFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void emptyValueAutoQCBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L, "",
      flagScheme.getAssumedGoodFlag());
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(IcosFlagScheme.QUESTIONABLE_FLAG));
    assertEquals(flagScheme.getBadFlag(), value.getUserQCFlag());
    assertEquals(flagScheme.getBadFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void emptyValueUserQCBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L, "",
      flagScheme.getAssumedGoodFlag());
    allValues.add(value);

    value.setUserQC(flagScheme.getGoodFlag(), "GOOD");
    assertEquals(flagScheme.getBadFlag(), value.getUserQCFlag());
    assertEquals(flagScheme.getBadFlag(), value.getDisplayFlag(allValues));
  }

  @FlywayTest
  @Test
  public void setLookupFailsTest() throws CoordinateException {
    SensorValue value = makeSensorValue(1L, 1L, "",
      flagScheme.getAssumedGoodFlag());
    assertThrows(InvalidFlagException.class, () -> {
      value.setUserQC(FlagScheme.LOOKUP_FLAG, "Nope");
    });
  }

  @FlywayTest
  @Test
  public void cannotOverrideFlushingTest()
    throws InvalidFlagException, CoordinateException {
    SensorValue value = makeSensorValue(1L, 1L, "12", FlagScheme.FLUSHING_FLAG);
    value.setUserQC(flagScheme.getGoodFlag(), "G");
    assertEquals(FlagScheme.FLUSHING_FLAG, value.getUserQCFlag());
  }

  @FlywayTest
  @Test
  public void cannotOverrideLookupTest()
    throws InvalidFlagException, CoordinateException {
    SensorValue value = makeSensorValue(1L, 1L, "12", FlagScheme.LOOKUP_FLAG);
    value.setUserQC(flagScheme.getGoodFlag(), "G");
    assertEquals(FlagScheme.LOOKUP_FLAG, value.getUserQCFlag());
  }

  @FlywayTest
  @Test
  public void cascadeUserFlagTest()
    throws InvalidFlagException, CoordinateException {
    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(flagScheme.getBadFlag(), "Source Bad");

    SensorValue target = makeSensorValue(2L, 2L);
    target.setCascadingQC(source);

    assertEquals(FlagScheme.LOOKUP_FLAG, target.getUserQCFlag());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void oneCascadeTest()
    throws InvalidFlagException, RecordNotFoundException, RoutineException,
    MissingParamException, DatabaseException, InstrumentException,
    SensorGroupsException, CoordinateException {
    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(flagScheme.getBadFlag(), "Source Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source);

    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source);
    allValues.add(target);

    assertEquals(flagScheme.getBadFlag(), target.getDisplayFlag(allValues));
    assertEquals("Source Bad", target.getDisplayQCMessage(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void twoCascadesTest()
    throws InvalidFlagException, RecordNotFoundException, RoutineException,
    MissingParamException, DatabaseException, InstrumentException,
    SensorGroupsException, CoordinateException {
    SensorValue source1 = makeSensorValue(1L, 1L);
    source1.setUserQC(flagScheme.getBadFlag(), "One Bad");
    SensorValue source2 = makeSensorValue(2L, 1L);
    source2.setUserQC(flagScheme.getBadFlag(), "Two Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source1);
    target.setCascadingQC(source2);

    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source1);
    allValues.add(source2);
    allValues.add(target);

    assertEquals(flagScheme.getBadFlag(), target.getDisplayFlag(allValues));
    assertEquals("One Bad;Two Bad", target.getDisplayQCMessage(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void removeOneCascadeTest()
    throws InvalidFlagException, RecordNotFoundException, RoutineException,
    MissingParamException, DatabaseException, InstrumentException,
    SensorGroupsException, CoordinateException {
    SensorValue source1 = makeSensorValue(1L, 1L);
    source1.setUserQC(flagScheme.getBadFlag(), "One Bad");
    SensorValue source2 = makeSensorValue(2L, 1L);
    source2.setUserQC(flagScheme.getBadFlag(), "Two Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source1);
    target.setCascadingQC(source2);

    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source1);
    allValues.add(source2);
    allValues.add(target);

    target.removeCascadingQC(source1.getId());

    assertEquals(flagScheme.getBadFlag(), target.getDisplayFlag(allValues));
    assertEquals("Two Bad", target.getDisplayQCMessage(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void removeAllCascadeAutoQCTest()
    throws InvalidFlagException, RecordNotFoundException, RoutineException,
    MissingParamException, DatabaseException, InstrumentException,
    SensorGroupsException, CoordinateException {

    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(flagScheme.getBadFlag(), "Source Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.addAutoQCFlag(makeAutoQCFlag(flagScheme.getBadFlag()));
    target.setCascadingQC(source);

    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source);
    allValues.add(target);

    target.removeCascadingQC(source.getId());

    assertEquals(FlagScheme.NEEDED_FLAG, target.getUserQCFlag());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument", "resources/sql/testbase/dataset" })
  @Test
  public void removeAllCascadeNoAutoQCTest()
    throws InvalidFlagException, RecordNotFoundException, RoutineException,
    MissingParamException, DatabaseException, InstrumentException,
    SensorGroupsException, CoordinateException {

    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(flagScheme.getBadFlag(), "Source Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source);

    DatasetSensorValues allValues = new DatasetSensorValues(DataSetDB
      .getDataSet(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source);
    allValues.add(target);

    target.removeCascadingQC(source.getId());

    assertEquals(flagScheme.getAssumedGoodFlag(), target.getUserQCFlag());
  }
}
