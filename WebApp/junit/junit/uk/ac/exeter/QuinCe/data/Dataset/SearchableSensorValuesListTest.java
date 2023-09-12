package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class SearchableSensorValuesListTest extends BaseTest {

  private SearchableSensorValuesList makePopulatedList() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1L);

    list.add(makeSensorValue(1L, 20));
    list.add(makeSensorValue(2L, 25));
    list.add(makeSensorValue(3L, 30));
    list.add(makeSensorValue(4L, 35));
    list.add(makeSensorValue(5L, 40));
    list.add(makeSensorValue(6L, 45));

    return list;
  }

  private SensorValue makeSensorValue(long id, int minute) {
    return makeSensorValue(id, 1L, minute, Flag.GOOD, false);
  }

  private SensorValue makeSensorValue(long valueId, long columnId, int minute) {
    return makeSensorValue(valueId, columnId, minute, Flag.GOOD, false);
  }

  private SensorValue makeSensorValue(long id, long columnId, int minute,
    Flag flag, boolean needed) {

    AutoQCResult autoQC;
    if (!needed) {
      autoQC = mockAutoQC(Flag.GOOD);
    } else {
      autoQC = mockAutoQC(flag);
    }

    Flag userFlag = needed ? Flag.NEEDED : flag;

    return new SensorValue(id, 1L, columnId, makeTime(minute), "29.111", autoQC,
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
      .newFromSensorValueCollection(makePopulatedList());
    assertEquals(makePopulatedList().size(), list.size());
  }

  @Test
  public void rangeSearchAllBeforeTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(0), makeTime(10));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchAllAfterTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(50), makeTime(55));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchFullRangeTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(10), makeTime(55));
    assertTrue(
      checkSearchResultIds(searchResult, new long[] { 1, 2, 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchMiddleMissTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(31), makeTime(34));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchInexactStartTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(27), makeTime(50));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchExactStartTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(30), makeTime(50));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchInexactEndTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(30), makeTime(42));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5 }));
  }

  @Test
  public void rangeSearchExactEndTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(30), makeTime(40));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4 }));
  }

  @Test
  public void rangeSearchStartBeforeTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(10), makeTime(40));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 1, 2, 3, 4 }));
  }

  @Test
  public void rangeSearchEndAfterTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(25), makeTime(55));
    assertTrue(
      checkSearchResultIds(searchResult, new long[] { 2, 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchLastEntryTest() throws Exception {
    List<SensorValue> searchResult = makePopulatedList()
      .rangeSearch(makeTime(45), makeTime(55));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 6 }));
  }

  @Test
  public void rangeSearchBadArgumentOrderTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      makePopulatedList().rangeSearch(makeTime(55), makeTime(25));
    });

  }

  @Test
  public void rangeSearchBadEqualArgumentTest() {
    assertThrows(IllegalArgumentException.class, () -> {
      makePopulatedList().rangeSearch(makeTime(30), makeTime(30));
    });
  }

  @Test
  public void getClosestExactTest() {
    SearchableSensorValuesList list = makePopulatedList();
    List<SensorValue> found = list.getClosest(makeTime(35));
    assertEquals(1, found.size());
    assertEquals(4L, found.get(0).getId());
  }

  @Test
  public void getClosestEmptyTest() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(4L);
    List<SensorValue> found = list.getClosest(makeTime(35));
    assertTrue(found.isEmpty());
  }

  @Test
  public void getClosestBetweenTest() {
    SearchableSensorValuesList list = makePopulatedList();
    List<SensorValue> found = list.getClosest(makeTime(32));
    assertEquals(2, found.size());
    assertEquals(3L, found.get(0).getId());
    assertEquals(4L, found.get(1).getId());
  }

  @Test
  public void getClosestStartTest() {
    SearchableSensorValuesList list = makePopulatedList();
    List<SensorValue> found = list.getClosest(makeTime(15));
    assertEquals(1, found.size());
    assertEquals(1L, found.get(0).getId());
  }

  @Test
  public void getClosestEndTest() {
    SearchableSensorValuesList list = makePopulatedList();
    List<SensorValue> found = list.getClosest(makeTime(50));
    assertEquals(1, found.size());
    assertEquals(6L, found.get(0).getId());
  }

  /**
   * Test creating a list from sensor values with multiple column IDs
   */
  @Test
  public void newFromListMultipleColumnIdsTest() {
    List<SensorValue> values = new ArrayList<SensorValue>();
    values.add(makeSensorValue(1L, 4L, 30));
    values.add(makeSensorValue(2L, 8L, 31));
    SearchableSensorValuesList.newFromSensorValueCollection(values);
  }

  @Test
  public void addToEmptyListTest() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1L);
    list.add(makeSensorValue(1L, 30));
    assertEquals(1, list.size());
  }

  @Test
  public void addNullTest() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1L);
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(null);
    });
  }

  @Test
  public void singleColumnInvalidColumnTest() {
    SearchableSensorValuesList list = new SearchableSensorValuesList(1L);
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(1L, 2L, 30));
    });
  }

  @Test
  public void addMultipleColumnsTest() {
    List<Long> columns = Arrays.asList(1L, 2L);
    SearchableSensorValuesList list = new SearchableSensorValuesList(columns);
    list.add(makeSensorValue(1L, 10));
    list.add(makeSensorValue(2L, 20));
    assertEquals(2, list.size());
  }

  @Test
  public void invalidMultipleColumnsTest() {
    List<Long> columns = Arrays.asList(1L, 2L);
    SearchableSensorValuesList list = new SearchableSensorValuesList(columns);
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(1L, 3L, 30));
    });
  }

  @Test
  public void sameTimeAsLastTest() {
    SearchableSensorValuesList list = makePopulatedList();
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(10L, 45));
    });
  }

  @Test
  public void sameTimeAsFirstTest() {
    SearchableSensorValuesList list = makePopulatedList();
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(10L, 20));
    });
  }

  @Test
  public void sameTimeAsMidTest() {
    SearchableSensorValuesList list = makePopulatedList();
    assertThrows(IllegalArgumentException.class, () -> {
      list.add(makeSensorValue(10L, 30));
    });
  }

  private boolean listIsOrdered(SearchableSensorValuesList list) {
    boolean result = true;

    for (int i = 1; i < list.size(); i++) {
      LocalDateTime prev = list.get(i - 1).getTime();
      LocalDateTime current = list.get(i).getTime();
      if (!prev.isBefore(current)) {
        result = false;
        break;
      }
    }

    return result;
  }

  @Test
  public void addBeforeFirst() {
    SearchableSensorValuesList list = makePopulatedList();
    list.add(makeSensorValue(10L, 0));
    assertTrue(listIsOrdered(list));
  }

  @Test
  public void addAfterLast() {
    SearchableSensorValuesList list = makePopulatedList();
    list.add(makeSensorValue(10L, 59));
    assertTrue(listIsOrdered(list));
  }

  @Test
  public void addInMiddle() {
    SearchableSensorValuesList list = makePopulatedList();
    list.add(makeSensorValue(10L, 32));
    assertTrue(listIsOrdered(list));
  }

  @Test
  public void addInPositionForbidden() {
    SearchableSensorValuesList list = makePopulatedList();
    assertThrows(UnsupportedOperationException.class, () -> {
      list.add(1, makeSensorValue(10L, 50));
    });
  }

  @Test
  public void addMultipleInPositionForbidden() {
    SearchableSensorValuesList list = makePopulatedList();
    List<SensorValue> values = new ArrayList<SensorValue>();
    assertThrows(UnsupportedOperationException.class, () -> {
      list.addAll(1, values);
    });
  }

  @Test
  public void addMultiple() {
    SearchableSensorValuesList list = makePopulatedList();
    List<SensorValue> values = new ArrayList<SensorValue>();
    values.add(makeSensorValue(10L, 0));
    values.add(makeSensorValue(11L, 50));
    values.add(makeSensorValue(12L, 32));
    list.addAll(values);
    assertTrue(listIsOrdered(list));
  }

  private static Stream<Arguments> measurementModeParams() {
    return Stream.of(Arguments.of(1, SearchableSensorValuesList.MODE_PERIODIC),
      Arguments.of(2, SearchableSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(3, SearchableSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(4, SearchableSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(5, SearchableSensorValuesList.MODE_CONTINUOUS),
      Arguments.of(6, SearchableSensorValuesList.MODE_PERIODIC),
      Arguments.of(7, SearchableSensorValuesList.MODE_PERIODIC),
      Arguments.of(8, SearchableSensorValuesList.MODE_PERIODIC));
  }

  @ParameterizedTest
  @MethodSource("measurementModeParams")
  public void measurementModeTest(int fileNumber, int expectedMode)
    throws Exception {

    // Load dates
    File timesFile = context.getResource(
      "classpath:resources/testdata/data/DataSet/SearchableSensorValuesList/measurementMode"
        + fileNumber + ".csv")
      .getFile();

    SearchableSensorValuesList list = new SearchableSensorValuesList(1L);

    BufferedReader in = new BufferedReader(new FileReader(timesFile));
    String line;
    while ((line = in.readLine()) != null) {
      LocalDateTime timestamp = DateTimeUtils.parseISODateTime(line);
      list.add(new SensorValue(1L, 1L, timestamp, "1"));
    }
    in.close();

    assertEquals(expectedMode, list.getMeasurementMode());
  }
}
