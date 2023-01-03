package junit.uk.ac.exeter.QuinCe.data.Dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import junit.uk.ac.exeter.QuinCe.TestBase.BaseTest;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;

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

  private SensorValue mockSensorValueWithColumnId(long columnId) {
    SensorValue value = Mockito.mock(SensorValue.class);
    Mockito.when(value.getColumnId()).thenReturn(4L);
    return value;
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
  public void rangeSearchAllBeforeTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(0),
      makeTime(10));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchAllAfterTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(50),
      makeTime(55));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchFullRangeTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(10),
      makeTime(55));
    assertTrue(
      checkSearchResultIds(searchResult, new long[] { 1, 2, 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchMiddleMissTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(31),
      makeTime(34));
    assertEquals(0, searchResult.size());
  }

  @Test
  public void rangeSearchInexactStartTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(27),
      makeTime(50));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchExactStartTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(30),
      makeTime(50));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchInexactEndTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(30),
      makeTime(42));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4, 5 }));
  }

  @Test
  public void rangeSearchExactEndTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(30),
      makeTime(40));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 3, 4 }));
  }

  @Test
  public void rangeSearchStartBeforeTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(10),
      makeTime(40));
    assertTrue(checkSearchResultIds(searchResult, new long[] { 1, 2, 3, 4 }));
  }

  @Test
  public void rangeSearchEndAfterTest() throws Exception {
    List<SensorValue> searchResult = rangeTestList().rangeSearch(makeTime(25),
      makeTime(55));
    assertTrue(
      checkSearchResultIds(searchResult, new long[] { 2, 3, 4, 5, 6 }));
  }

  @Test
  public void rangeSearchLastEntryTest() throws Exception {
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

  @Test
  public void getClosestExactTest() {
    SearchableSensorValuesList list = rangeTestList();
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
    SearchableSensorValuesList list = rangeTestList();
    List<SensorValue> found = list.getClosest(makeTime(32));
    assertEquals(2, found.size());
    assertEquals(3L, found.get(0).getId());
    assertEquals(4L, found.get(1).getId());
  }

  @Test
  public void getClosestStartTest() {
    SearchableSensorValuesList list = rangeTestList();
    List<SensorValue> found = list.getClosest(makeTime(15));
    assertEquals(1, found.size());
    assertEquals(1L, found.get(0).getId());
  }

  @Test
  public void getClosestEndTest() {
    SearchableSensorValuesList list = rangeTestList();
    List<SensorValue> found = list.getClosest(makeTime(50));
    assertEquals(1, found.size());
    assertEquals(6L, found.get(0).getId());
  }
}
