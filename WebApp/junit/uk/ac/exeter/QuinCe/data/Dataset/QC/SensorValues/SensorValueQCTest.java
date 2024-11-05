package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class SensorValueQCTest extends BaseTest {

  private static final long DATASET_ID = 1L;

  private SensorValue makeSensorValue(long id, long columnId) {
    return makeSensorValue(id, columnId, "12", Flag.ASSUMED_GOOD);
  }

  private SensorValue makeSensorValue(long id, long columnId, String value,
    Flag flag) {
    return new SensorValue(id, DATASET_ID, columnId,
      LocalDateTime.now(ZoneId.of("Z")), value, null, flag, null);
  }

  private RoutineFlag makeAutoQCFlag(Flag flag) {
    return new RoutineFlag(new RangeCheckRoutine(), flag, "0", "1");
  }

  @BeforeEach
  public void setup() {
    initResourceManager();
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void defaultQCTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    assertEquals(Flag.GOOD, value.getAutoQcFlag());
    assertEquals(Flag.ASSUMED_GOOD, value.getUserQCFlag());
    assertEquals(Flag.ASSUMED_GOOD, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void autoQCBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(Flag.BAD));
    assertEquals(Flag.BAD, value.getAutoQcFlag());
    assertEquals(Flag.NEEDED, value.getUserQCFlag());
    assertEquals(Flag.BAD, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void autoQCQuestionableTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(Flag.QUESTIONABLE));
    assertEquals(Flag.QUESTIONABLE, value.getAutoQcFlag());
    assertEquals(Flag.NEEDED, value.getUserQCFlag());
    assertEquals(Flag.QUESTIONABLE, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void autoQCQuestionableBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(Flag.QUESTIONABLE));
    value.addAutoQCFlag(makeAutoQCFlag(Flag.BAD));
    assertEquals(Flag.BAD, value.getAutoQcFlag());
    assertEquals(Flag.NEEDED, value.getUserQCFlag());
    assertEquals(Flag.BAD, value.getDisplayFlag(allValues));
  }

  @FlywayTest
  @Test
  public void clearAutoQCNoUserQC()
    throws RecordNotFoundException, RoutineException {
    SensorValue value = makeSensorValue(1L, 1L);
    value.addAutoQCFlag(makeAutoQCFlag(Flag.BAD));
    value.clearAutomaticQC();
    assertEquals(Flag.ASSUMED_GOOD, value.getUserQCFlag());
  }

  @FlywayTest
  @Test
  public void clearAutoQCWithUserQC()
    throws RecordNotFoundException, RoutineException, InvalidFlagException {
    SensorValue value = makeSensorValue(1L, 1L);
    value.addAutoQCFlag(makeAutoQCFlag(Flag.BAD));
    value.setUserQC(Flag.QUESTIONABLE, "Q");
    value.clearAutomaticQC();
    assertEquals(Flag.QUESTIONABLE, value.getUserQCFlag());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void userQCTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.setUserQC(Flag.BAD, "BAD");
    assertEquals(Flag.GOOD, value.getAutoQcFlag());
    assertEquals(Flag.BAD, value.getUserQCFlag());
    assertEquals(Flag.BAD, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void userThenAutoQCTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.setUserQC(Flag.BAD, "BAD");
    value.addAutoQCFlag(makeAutoQCFlag(Flag.QUESTIONABLE));
    assertEquals(Flag.QUESTIONABLE, value.getAutoQcFlag());
    assertEquals(Flag.BAD, value.getUserQCFlag());
    assertEquals(Flag.BAD, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void userOverrideWithLessSignificantTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(Flag.BAD));
    value.setUserQC(Flag.GOOD, "");
    assertEquals(Flag.BAD, value.getAutoQcFlag());
    assertEquals(Flag.GOOD, value.getUserQCFlag());
    assertEquals(Flag.GOOD, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void emptyValueBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L, "", Flag.ASSUMED_GOOD);
    allValues.add(value);

    assertEquals(Flag.BAD, value.getUserQCFlag());
    assertEquals(Flag.BAD, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void emptyValueAutoQCBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L, "", Flag.ASSUMED_GOOD);
    allValues.add(value);

    value.addAutoQCFlag(makeAutoQCFlag(Flag.QUESTIONABLE));
    assertEquals(Flag.BAD, value.getUserQCFlag());
    assertEquals(Flag.BAD, value.getDisplayFlag(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void emptyValueUserQCBadTest() throws Exception {
    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));

    SensorValue value = makeSensorValue(1L, 1L, "", Flag.ASSUMED_GOOD);
    allValues.add(value);

    value.setUserQC(Flag.GOOD, "GOOD");
    assertEquals(Flag.BAD, value.getUserQCFlag());
    assertEquals(Flag.BAD, value.getDisplayFlag(allValues));
  }

  @FlywayTest
  @Test
  public void setLookupFailsTest() {
    SensorValue value = makeSensorValue(1L, 1L, "", Flag.ASSUMED_GOOD);
    assertThrows(InvalidFlagException.class, () -> {
      value.setUserQC(Flag.LOOKUP, "Nope");
    });
  }

  @FlywayTest
  @Test
  public void cannotOverrideFlushingTest() throws InvalidFlagException {
    SensorValue value = makeSensorValue(1L, 1L, "12", Flag.FLUSHING);
    value.setUserQC(Flag.GOOD, "G");
    assertEquals(Flag.FLUSHING, value.getUserQCFlag());
  }

  @FlywayTest
  @Test
  public void cannotOverrideLookupTest() throws InvalidFlagException {
    SensorValue value = makeSensorValue(1L, 1L, "12", Flag.LOOKUP);
    value.setUserQC(Flag.GOOD, "G");
    assertEquals(Flag.LOOKUP, value.getUserQCFlag());
  }

  @FlywayTest
  @Test
  public void cascadeUserFlagTest() throws InvalidFlagException {
    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(Flag.BAD, "Source Bad");

    SensorValue target = makeSensorValue(2L, 2L);
    target.setCascadingQC(source);

    assertEquals(Flag.LOOKUP, target.getUserQCFlag());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void oneCascadeTest() throws InvalidFlagException,
    RecordNotFoundException, RoutineException, MissingParamException,
    DatabaseException, InstrumentException, SensorGroupsException {
    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(Flag.BAD, "Source Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source);

    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source);
    allValues.add(target);

    assertEquals(Flag.BAD, target.getDisplayFlag(allValues));
    assertEquals("Source Bad", target.getDisplayQCMessage(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void twoCascadesTest() throws InvalidFlagException,
    RecordNotFoundException, RoutineException, MissingParamException,
    DatabaseException, InstrumentException, SensorGroupsException {
    SensorValue source1 = makeSensorValue(1L, 1L);
    source1.setUserQC(Flag.BAD, "One Bad");
    SensorValue source2 = makeSensorValue(2L, 1L);
    source2.setUserQC(Flag.BAD, "Two Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source1);
    target.setCascadingQC(source2);

    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source1);
    allValues.add(source2);
    allValues.add(target);

    assertEquals(Flag.BAD, target.getDisplayFlag(allValues));
    assertEquals("One Bad;Two Bad", target.getDisplayQCMessage(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void removeOneCascadeTest() throws InvalidFlagException,
    RecordNotFoundException, RoutineException, MissingParamException,
    DatabaseException, InstrumentException, SensorGroupsException {
    SensorValue source1 = makeSensorValue(1L, 1L);
    source1.setUserQC(Flag.BAD, "One Bad");
    SensorValue source2 = makeSensorValue(2L, 1L);
    source2.setUserQC(Flag.BAD, "Two Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source1);
    target.setCascadingQC(source2);

    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source1);
    allValues.add(source2);
    allValues.add(target);

    target.removeCascadingQC(source1.getId());

    assertEquals(Flag.BAD, target.getDisplayFlag(allValues));
    assertEquals("Two Bad", target.getDisplayQCMessage(allValues));
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void removeAllCascadeAutoQCTest() throws InvalidFlagException,
    RecordNotFoundException, RoutineException, MissingParamException,
    DatabaseException, InstrumentException, SensorGroupsException {

    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(Flag.BAD, "Source Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.addAutoQCFlag(makeAutoQCFlag(Flag.BAD));
    target.setCascadingQC(source);

    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source);
    allValues.add(target);

    target.removeCascadingQC(source.getId());

    assertEquals(Flag.NEEDED, target.getUserQCFlag());
  }

  @FlywayTest(locationsForMigrate = { "resources/sql/testbase/user",
    "resources/sql/testbase/instrument" })
  @Test
  public void removeAllCascadeNoAutoQCTest() throws InvalidFlagException,
    RecordNotFoundException, RoutineException, MissingParamException,
    DatabaseException, InstrumentException, SensorGroupsException {

    SensorValue source = makeSensorValue(1L, 1L);
    source.setUserQC(Flag.BAD, "Source Bad");

    SensorValue target = makeSensorValue(20L, 2L);
    target.setCascadingQC(source);

    DatasetSensorValues allValues = new DatasetSensorValues(InstrumentDB
      .getInstrument(ResourceManager.getInstance().getDBDataSource(), 1L));
    allValues.add(source);
    allValues.add(target);

    target.removeCascadingQC(source.getId());

    assertEquals(Flag.ASSUMED_GOOD, target.getUserQCFlag());
  }
}
