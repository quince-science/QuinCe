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
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * A basic data structure to be used by classes implementing
 * {@link PlotPage2Data}.
 *
 * <p>
 * This is a nested map of {@link LocalDateTime} -> {@link ColumnHeading} ->
 * {@link PlotPageTableColumn}.
 * </p>
 *
 * <p>
 * The class contains analogous methods for the following data retrieval methods
 * specified in {@link PlotPage2Data}:
 * <ul>
 * <li>{@link PlotPage2Data#size()} (from the {@link Map} interface)</li>
 * <li>{@link PlotPage2Data#getRowIDs()}</li>
 * <li>{@link PlotPage2Data#generateTableDataRecords(int, int)}</li>
 * <li>{@link PlotPage2Data#getColumnValues(ColumnHeading)}</li>
 * </p>
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

  private final List<ColumnHeading> columnHeadings;

  private final ColumnHeading timeHeading;

  /**
   * The main data structure, with columns organised by date and column
   */
  private TreeMap<LocalDateTime, LinkedHashMap<ColumnHeading, PlotPageTableColumn>> pageData;

  public SimplePlotPageDataStructure(List<ColumnHeading> columnHeadings) {
    pageData = new TreeMap<LocalDateTime, LinkedHashMap<ColumnHeading, PlotPageTableColumn>>();
    this.columnHeadings = Collections.unmodifiableList(columnHeadings);
    timeHeading = getTimeHeading();
  }

  private void addTime(LocalDateTime time) {

    // Create an empty map of all the columns
    LinkedHashMap<ColumnHeading, PlotPageTableColumn> columns = new LinkedHashMap<ColumnHeading, PlotPageTableColumn>();
    columnHeadings.forEach(x -> columns.put(x, null));

    if (null != timeHeading) {
      columns.put(timeHeading, new SimplePlotPageTableColumn(time, false));
    }

    pageData.put(time, columns);
  }

  /**
   * Get the record times as a list of longs.
   *
   * <p>
   * Analogous to {@link PlotPage2Data#getRowIDs()}
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

  public TreeMap<LocalDateTime, PlotPageTableColumn> getColumnValues(
    ColumnHeading column) throws Exception {

    TreeMap<LocalDateTime, PlotPageTableColumn> result = new TreeMap<LocalDateTime, PlotPageTableColumn>();

    for (LocalDateTime time : pageData.keySet()) {
      for (ColumnHeading heading : pageData.get(time).keySet()) {
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
  public void add(LocalDateTime time, ColumnHeading heading,
    PlotPageTableColumn value) {

    if (!pageData.containsKey(time)) {
      // Create the time entry
      addTime(time);

      // Clear the row IDs cache
      rowIds = null;
    }

    pageData.get(time).put(heading, value);
  }

  public void add(LocalDateTime time, ColumnHeading heading, SensorValue value,
    boolean used) throws RoutineException {

    if (!pageData.containsKey(time)) {
      // Create the time entry
      addTime(time);

      // Clear the row IDs cache
      rowIds = null;
    }

    pageData.get(time).put(heading,
      new SensorValuePlotPageTableColumn(value, used));
  }

  private ColumnHeading getTimeHeading() {
    return getHeading(FileDefinition.TIME_COLUMN_ID);
  }

  private ColumnHeading getHeading(long columnId) {
    ColumnHeading result = null;

    for (ColumnHeading heading : columnHeadings) {
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

    ColumnHeading heading = getHeading(columnId);

    for (LocalDateTime time : times) {

      Map<ColumnHeading, PlotPageTableColumn> timeEntry = pageData.get(time);
      if (null != timeEntry) {
        PlotPageTableColumn column = timeEntry.get(heading);
        if (null != column) {
          if (column instanceof SensorValuePlotPageTableColumn) {
            result
              .add(((SensorValuePlotPageTableColumn) column).getSensorValue());
          }
        }
      }
    }

    return result;
  }
}
