package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
@SuppressWarnings("serial")
public class SimplePlotPageDataStructure extends
  TreeMap<LocalDateTime, LinkedHashMap<ColumnHeading, PlotPageTableColumn>> {

  /**
   * Cached list of row IDs, i.e. the times as longs.
   */
  private List<Long> rowIds = null;

  private final List<ColumnHeading> columnHeadings;

  private final ColumnHeading timeHeading;

  public SimplePlotPageDataStructure(List<ColumnHeading> columnHeadings) {
    super();
    this.columnHeadings = Collections.unmodifiableList(columnHeadings);
    timeHeading = getTimeHeading();
  }

  private void addTime(LocalDateTime time) {

    // Create an empty map of all the columns
    LinkedHashMap<ColumnHeading, PlotPageTableColumn> columns = new LinkedHashMap<ColumnHeading, PlotPageTableColumn>();
    columnHeadings.forEach(x -> columns.put(x, null));

    if (null != timeHeading) {
      columns.put(timeHeading, new PlotPageTableColumn(time, false));
    }

    put(time, columns);
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
      rowIds = keySet().stream().map(x -> DateTimeUtils.dateToLong(x))
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
      record.addAll(get(time).values());
      result.add(record);
    }

    return result;
  }

  public TreeMap<LocalDateTime, PlotPageTableColumn> getColumnValues(
    ColumnHeading column) throws Exception {

    TreeMap<LocalDateTime, PlotPageTableColumn> result = new TreeMap<LocalDateTime, PlotPageTableColumn>();

    for (LocalDateTime time : keySet()) {
      for (ColumnHeading heading : get(time).keySet()) {
        if (heading.equals(column)) {
          result.put(time, get(time).get(column));
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

    if (!containsKey(time)) {
      // Create the time entry
      addTime(time);

      // Clear the row IDs cache
      rowIds = null;
    }

    get(time).put(heading, value);
  }

  private ColumnHeading getTimeHeading() {

    ColumnHeading result = null;

    for (ColumnHeading heading : columnHeadings) {
      if (heading.getId() == FileDefinition.TIME_COLUMN_ID) {
        result = heading;
        break;
      }
    }

    return result;
  }
}
