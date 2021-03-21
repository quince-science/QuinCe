package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * A basic data structure to be used by classes implementing
 * {@link PlotPageData}.
 *
 * <p>
 * This is a nested map of {@link LocalDateTime} -&gt;
 * {@link PlotPageColumnHeading} -&gt; {@link PlotPageTableValue}.
 * </p>
 *
 * <p>
 * The class contains analogous methods for the following data retrieval methods
 * specified in {@link PlotPageData}:
 * </p>
 * <ul>
 * <li>{@link PlotPageData#size()} (from the {@link Map} interface)</li>
 * <li>{@link PlotPageData#getRowIDs()}</li>
 * <li>{@link PlotPageData#generateTableDataRecords(int, int)}</li>
 * <li>{@link PlotPageData#getColumnValues(PlotPageColumnHeading)}</li>
 * </ul>
 *
 * <p>
 * The data structure can be built using the {@link #add} method.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class SimplePlotPageDataStructure {

  /**
   * Cached list of row IDs, i.e. the times as longs.
   */
  private List<Long> rowIds = null;

  private final List<PlotPageColumnHeading> columnHeadings;

  private final PlotPageColumnHeading timeHeading;

  /**
   * The main data structure, with columns organised by date and column
   */
  private TreeMap<LocalDateTime, LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue>> pageData;

  public SimplePlotPageDataStructure(List<PlotPageColumnHeading> list) {
    pageData = new TreeMap<LocalDateTime, LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue>>();
    this.columnHeadings = Collections.unmodifiableList(list);
    timeHeading = getTimeHeading();
  }

  private void addTime(LocalDateTime time) {

    // Create an empty map of all the columns
    LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue> columns = new LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue>();
    columnHeadings.forEach(x -> columns.put(x, null));

    if (null != timeHeading) {
      columns.put(timeHeading, new SimplePlotPageTableValue(time, false));
    }

    pageData.put(time, columns);
  }

  /**
   * Get the record times as a list of longs.
   *
   * <p>
   * Analogous to {@link PlotPageData#getRowIDs()}
   * </p>
   *
   * @return The row IDs.
   */
  public List<Long> getRowIds() {

    if (null == rowIds) {
      rowIds = pageData.keySet().stream().map(x -> DateTimeUtils.dateToLong(x))
        .collect(Collectors.toList());
    }

    return rowIds;
  }

  public List<PlotPageTableRecord> generateTableDataRecords(int start,
    int length) {

    List<PlotPageTableRecord> result = new ArrayList<PlotPageTableRecord>(
      length);

    List<Long> ids = getRowIds();
    int end = start + length - 1;
    if (end >= ids.size()) {
      end = ids.size() - 1;
    }

    for (int i = start; i <= end; i++) {
      LocalDateTime time = DateTimeUtils.longToDate(ids.get(i));
      PlotPageTableRecord record = new PlotPageTableRecord(time);
      record.addAll(pageData.get(time).values());
      result.add(record);
    }

    return result;
  }

  public TreeMap<LocalDateTime, PlotPageTableValue> getColumnValues(
    PlotPageColumnHeading column) throws Exception {

    TreeMap<LocalDateTime, PlotPageTableValue> result = new TreeMap<LocalDateTime, PlotPageTableValue>();

    for (LocalDateTime time : pageData.keySet()) {
      for (PlotPageColumnHeading heading : pageData.get(time).keySet()) {
        if (heading.equals(column)) {
          result.put(time, pageData.get(time).get(column));
          break;
        }
      }
    }

    return result;
  }

  /**
   * Add a value to the data structure.
   *
   * @param time
   *          The value's timestamp
   * @param heading
   *          The column
   * @param value
   *          The value
   */
  public void add(LocalDateTime time, PlotPageColumnHeading heading,
    PlotPageTableValue value) {

    if (!pageData.containsKey(time)) {
      // Create the time entry
      addTime(time);

      // Clear the row IDs cache
      rowIds = null;
    }

    pageData.get(time).put(heading, value);
  }

  public void add(LocalDateTime time, PlotPageColumnHeading heading,
    SensorValue value) throws RoutineException {

    if (!pageData.containsKey(time)) {
      // Create the time entry
      addTime(time);

      // Clear the row IDs cache
      rowIds = null;
    }

    pageData.get(time).put(heading, new SensorValuePlotPageTableValue(value));
  }

  private PlotPageColumnHeading getTimeHeading() {
    return getHeading(FileDefinition.TIME_COLUMN_ID);
  }

  private PlotPageColumnHeading getHeading(long columnId) {
    PlotPageColumnHeading result = null;

    for (PlotPageColumnHeading heading : columnHeadings) {
      if (heading.getId() == columnId) {
        result = heading;
        break;
      }
    }

    return result;
  }

  public int size() {
    return pageData.size();
  }

  public List<SensorValue> getSensorValues(long columnId,
    List<LocalDateTime> times) {

    List<SensorValue> result = new ArrayList<SensorValue>(times.size());

    PlotPageColumnHeading heading = getHeading(columnId);

    for (LocalDateTime time : times) {

      Map<PlotPageColumnHeading, PlotPageTableValue> timeEntry = pageData
        .get(time);
      if (null != timeEntry) {
        PlotPageTableValue column = timeEntry.get(heading);
        if (null != column) {
          if (column instanceof SensorValuePlotPageTableValue) {
            result
              .add(((SensorValuePlotPageTableValue) column).getSensorValue());
          }
        }
      }
    }

    return result;
  }
}
