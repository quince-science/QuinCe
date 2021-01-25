package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.MeasurementValue;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class SearchableSensorValuesListTest extends BaseTest {

  private SearchableSensorValuesList rangeTestList() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1);

    list.add(makeSensorValue(1, 20));
    list.add(makeSensorValue(2, 25));
    list.add(makeSensorValue(3, 30));
    list.add(makeSensorValue(4, 35));
    list.add(makeSensorValue(5, 40));
    list.add(makeSensorValue(6, 45));

    return list;
  }

  private SensorValue makeSensorValue(long id, int minute) {
    return makeSensorValue(id, minute, Flag.GOOD, false);
  }

  private SensorValue makeSensorValue(long id, int minute, Flag flag,
    boolean needed) {

    AutoQCResult autoQC;
    if (!needed) {
      autoQC = mockAutoQC(Flag.GOOD);
    } else {
      autoQC = mockAutoQC(flag);
    }

    Flag userFlag = needed ? Flag.NEEDED : flag;

    return new SensorValue(id, 1, 1, makeTime(minute), "29.111", autoQC,
      userFlag, "Dummy");
  }

  /**
   * Generate a mock {@link AutoQCResult} object that returns the specified flag
   *
   * @param flag
   *          The auto QC flag
   * @return The mock object
   */
  private AutoQCResult mockAutoQC(Flag flag) {
    AutoQCResult autoQC = Mockito.mock(AutoQCResult.class);
    Mockito.when(autoQC.getOverallFlag()).thenReturn(flag);
    return autoQC;
  }

  private LocalDateTime makeTime(int minute) {
    return LocalDateTime.of(2020, 12, 3, 0, minute, 0);
  }

  private boolean checkSearchResultIds(List<SensorValue> searchResult,
    long[] ids) {
    boolean matches = true;

    if (searchResult.size() != ids.length) {
      matches = false;
    } else {
      for (int i = 0; i < searchResult.size(); i++) {
        if (searchResult.get(i).getId() != ids[i]) {
          matches = false;
          break;
        }
      }
    }

    return matches;
  }

  @Test
  public void emptyConstructorTest() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1);
    assertEquals(0, list.size());
  }

  @Test
  public void listConstructorTest() {
    SearchableSensorValuesList list = SearchableSensorValuesList
      .newFromSensorValueCollection(rangeTestList());
    assertEquals(rangeTestList().size(), list.size());
  }

  @Test
  public void rangeSearchAllBeforeTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(0),
      makeTime(10));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchAllAfterTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(50),
      makeTime(55));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchFullRangeTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(10),
      makeTime(55));
    assertTrue(
      checkSearchResultIds(searchResult, new long[] { 1, 2, 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchMiddleMissTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(31),
      makeTime(34));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchInexactStartTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(27),
      makeTime(50));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchExactStartTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(30),
      makeTime(50));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchInexactEndTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(30),
      makeTime(42));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5 }));
  }

  @Test
  public void rangeSearchExactEndTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(30),
      makeTime(40));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4 }));
  }

  @Test
  public void rangeSearchStartBeforeTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(10),
      makeTime(40));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 1, 2, 3, 4 }));
  }

  @Test
  public void rangeSearchEndAfterTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(25),
      makeTime(55));
    assertTrue(
      checkSearchResultIds(searchResult, new long[] { 2, 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchLastEntryTest() throws MissingParamException {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(45),
      makeTime(55));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 6 }));
  }

  @Test
  public void rangeSearchBadArgumentOrderTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      rangeTestList().rangeSearch(makeTime(55), makeTime(25));
    });

  }

  @Test
  public void rangeSearchBadEqualArgumentTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      rangeTestList().rangeSearch(makeTime(30), makeTime(30));
    });
  }

  private Measurement mockMeasurement(int minute) {
    Measurement mock = Mockito.mock(Measurement.class);
    Mockito.when(mock.getTime()).thenReturn(makeTime(minute));
    return mock;
  }

  @Test
  public void popMeasValInvalidColumnIdTest() {

    SearchableSensorValuesList list = new SearchableSensorValuesList(1);
    MeasurementValue value = new MeasurementValue(mockMeasurement(5),
      Mockito.mock(SensorType.class), 2);

    assertThrows(IllegalArgumentException.class, () -> {
      list.populateMeasurementValue(value, true);
    });
  }

  private MeasurementValue makeMeasurementValue(int minute) {
    return new MeasurementValue(mockMeasurement(minute),
      Mockito.mock(SensorType.class), 1);
  }

  /**
   * Make a list containing all {@link Flag#GOOD} values
   *
   * @return A list containing only {@link Flag#GOOD} values.
   */
  private SearchableSensorValuesList goodList() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1);

    list.add(makeSensorValue(1, 5));
    list.add(makeSensorValue(2, 10));
    list.add(makeSensorValue(3, 15));
    list.add(makeSensorValue(4, 20));
    list.add(makeSensorValue(5, 25));

    return list;
  }

  @ParameterizedTest
  @CsvSource({ "2, , 1, 2", "10, 2, , 2", "17, 3, 4, 2", "25, 5, , 2",
    "30, 5, , 2" })
  public void popMeasValAllGoodTest(int minute, Long expectedPrior,
    Long expectedPost, int expectedFlagValue)
    throws MissingParamException, RoutineException {

    MeasurementValue value = makeMeasurementValue(minute);
    goodList().populateMeasurementValue(value, true);

    assertEquals(expectedPrior, value.getPrior(), "Wrong prior");
    assertEquals(expectedPost, value.getPost(), "Wrong post");

    // Handle ASSUMED_GOOD
    assertEquals(expectedFlagValue, Math.abs(value.getQcFlag().getFlagValue()),
      "Wrong QC Flag");
  }

  /**
   * Make a list where one of the limits of the list is {@link Flag#GOOD}.
   *
   * <p>
   * Some flags are {@link Flag#ASSUMED_GOOD} and some are {@link Flag#NEEDED}
   * to test detection of correct flags.
   * </p>
   *
   * @return A list where one of the limits of the list is {@link Flag#GOOD}.
   */
  private SearchableSensorValuesList goodLimitList() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1);

    list.add(makeSensorValue(1, 5, Flag.GOOD, false));
    list.add(makeSensorValue(2, 10, Flag.GOOD, false));
    list.add(makeSensorValue(3, 15, Flag.QUESTIONABLE, true));
    list.add(makeSensorValue(4, 20, Flag.BAD, false));
    list.add(makeSensorValue(5, 25, Flag.QUESTIONABLE, false));
    list.add(makeSensorValue(6, 30, Flag.ASSUMED_GOOD, false));
    list.add(makeSensorValue(7, 35, Flag.BAD, false));
    list.add(makeSensorValue(8, 40, Flag.BAD, true));
    list.add(makeSensorValue(9, 45, Flag.QUESTIONABLE, false));
    list.add(makeSensorValue(10, 50, Flag.ASSUMED_GOOD, false));
    list.add(makeSensorValue(11, 55, Flag.BAD, false));

    return list;
  }

  // TODO EXPECTED VALUES WILL CHANGE WHEN PROPER INTERPOLATION IS IMPLEMENTED
  // #1558
  @ParameterizedTest
  @CsvSource({ "15, 3, , 3", "26, 5, 6, 3", "43, 8, 9, 4", "50, 10, , 2",
    "59, 11, , 4" })
  public void popMeasValGoodLimitNotGoodOnlyTest(int minute, Long expectedPrior,
    Long expectedPost, int expectedFlagValue)
    throws MissingParamException, RoutineException {

    MeasurementValue value = makeMeasurementValue(minute);
    goodLimitList().populateMeasurementValue(value, false);

    assertEquals(expectedPrior, value.getPrior(), "Wrong prior");
    assertEquals(expectedPost, value.getPost(), "Wrong post");

    // Handle ASSUMED_GOOD
    assertEquals(expectedFlagValue, Math.abs(value.getQcFlag().getFlagValue()),
      "Wrong QC Flag");
  }

  // TODO EXPECTED VALUES WILL CHANGE WHEN PROPER INTERPOLATION IS IMPLEMENTED
  // #1558
  @ParameterizedTest
  @CsvSource({ "15, 2, 6, 2", "26, 2, 6, 2", "43, 6, 10, 2", "50, 10, , 2",
    "59, 10, , 2" })
  public void popMeasValGoodLimitGoodOnlyTest(int minute, Long expectedPrior,
    Long expectedPost, int expectedFlagValue)
    throws MissingParamException, RoutineException {

    MeasurementValue value = makeMeasurementValue(minute);
    goodLimitList().populateMeasurementValue(value, true);

    assertEquals(expectedPrior, value.getPrior(), "Wrong prior");
    assertEquals(expectedPost, value.getPost(), "Wrong post");

    // Handle ASSUMED_GOOD
    assertEquals(expectedFlagValue, Math.abs(value.getQcFlag().getFlagValue()),
      "Wrong QC Flag");
  }

  /**
   * Make a list where neither of the limits of the list is {@link Flag#GOOD}.
   *
   * <p>
   * Some flags are {@link Flag#ASSUMED_GOOD} and some are {@link Flag#NEEDED}
   * to test detection of correct flags.
   * </p>
   *
   * @return A list where neither of the limits of the list is
   *         {@link Flag#GOOD}.
   */
  private SearchableSensorValuesList noGoodLimitList() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1);

    list.add(makeSensorValue(1, 5, Flag.BAD, true));
    list.add(makeSensorValue(2, 10, Flag.BAD, false));
    list.add(makeSensorValue(3, 15, Flag.GOOD, false));
    list.add(makeSensorValue(4, 20, Flag.QUESTIONABLE, false));
    list.add(makeSensorValue(5, 25, Flag.BAD, false));
    list.add(makeSensorValue(6, 30, Flag.QUESTIONABLE, false));

    return list;
  }

  // TODO EXPECTED VALUES WILL CHANGE WHEN PROPER INTERPOLATION IS IMPLEMENTED
  // #1558
  @ParameterizedTest
  @CsvSource({ "7, 1, 2, 4", "12, 2, 3, 4", "15, 3, , 2", "23, 4, 5, 4",
    "27, 5, 6, 4", "30, 6, , 3" })
  public void popMeasValNoGoodLimitNotGoodOnlyTest(int minute,
    Long expectedPrior, Long expectedPost, int expectedFlagValue)
    throws MissingParamException, RoutineException {

    MeasurementValue value = makeMeasurementValue(minute);
    noGoodLimitList().populateMeasurementValue(value, false);

    assertEquals(expectedPrior, value.getPrior(), "Wrong prior");
    assertEquals(expectedPost, value.getPost(), "Wrong post");

    // Handle ASSUMED_GOOD
    assertEquals(expectedFlagValue, Math.abs(value.getQcFlag().getFlagValue()),
      "Wrong QC Flag");
  }

  // TODO EXPECTED VALUES WILL CHANGE WHEN PROPER INTERPOLATION IS IMPLEMENTED
  // #1558
  @ParameterizedTest
  @CsvSource({ "7, , 3, 2", "12, , 3, 2", "15, 3, , 2", "23, 3, , 2",
    "27, 3, , 2", "30, 3, , 2" })
  public void popMeasValNoGoodLimitGoodOnlyTest(int minute, Long expectedPrior,
    Long expectedPost, int expectedFlagValue)
    throws MissingParamException, RoutineException {

    MeasurementValue value = makeMeasurementValue(minute);
    noGoodLimitList().populateMeasurementValue(value, true);

    assertEquals(expectedPrior, value.getPrior(), "Wrong prior");
    assertEquals(expectedPost, value.getPost(), "Wrong post");

    // Handle ASSUMED_GOOD
    assertEquals(expectedFlagValue, Math.abs(value.getQcFlag().getFlagValue()),
      "Wrong QC Flag");
  }

  /**
   * Make sure we can have more than one column ID
   */
  @Test
  public void multipleColumnIdsTest() {
    List<Long> columnIds = new ArrayList<Long>();
    columnIds.add(4L);
    columnIds.add(8L);

    SearchableSensorValuesList list = new SearchableSensorValuesList(columnIds);
    list.add(mockSensorValueWithColumnId(8));
    list.add(mockSensorValueWithColumnId(4));
  }

  /**
   * Test creating a list from sensor values with multiple column IDs
   */
  @Test
  public void newFromListMultipleColumnIdsTest() {

    List<SensorValue> values = new ArrayList<SensorValue>();
    values.add(mockSensorValueWithColumnId(4));
    values.add(mockSensorValueWithColumnId(8));

    SearchableSensorValuesList.newFromSensorValueCollection(values);

  }

  private SensorValue mockSensorValueWithColumnId(long columnId) {
    SensorValue value = Mockito.mock(SensorValue.class);
    Mockito.when(value.getColumnId()).thenReturn(4L);
    return value;
  }
}
