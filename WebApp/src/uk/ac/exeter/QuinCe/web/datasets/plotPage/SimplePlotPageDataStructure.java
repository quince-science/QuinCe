package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;

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
 */
public class SimplePlotPageDataStructure {

  private final List<PlotPageColumnHeading> columnHeadings;

  private final PlotPageColumnHeading timeHeading;

  /**
   * The main data structure, with columns organised by {@link Coordinate} and
   * column
   */
  private TreeMap<Coordinate, LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue>> pageData;

  /**
   * Cache of the {@link Coordinates} added to the data.
   */
  private TreeMap<Long, Coordinate> coordinates;

  private final FlagScheme flagScheme;

  public SimplePlotPageDataStructure(List<PlotPageColumnHeading> list,
    FlagScheme flagScheme) {
    pageData = new TreeMap<Coordinate, LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue>>();
    coordinates = new TreeMap<Long, Coordinate>();
    this.columnHeadings = Collections.unmodifiableList(list);
    timeHeading = getTimeHeading();
    this.flagScheme = flagScheme;
  }

  private void addCoordinate(Coordinate coordinate) {

    // Create an empty map of all the columns
    LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue> columns = new LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue>();
    columnHeadings.forEach(x -> columns.put(x, null));

    if (null != timeHeading) {
      columns.put(timeHeading,
        new SimplePlotPageTableValue(coordinate, flagScheme));
    }

    pageData.put(coordinate, columns);
    coordinates.putIfAbsent(coordinate.getId(), coordinate);
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
    return new ArrayList<Long>(coordinates.keySet());
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
      PlotPageTableRecord record = new PlotPageTableRecord(ids.get(i),
        flagScheme);
      record.addAll(pageData.get(coordinates.get(ids.get(i))).values());
      result.add(record);
    }

    return result;
  }

  public TreeMap<Coordinate, PlotPageTableValue> getColumnValues(
    PlotPageColumnHeading column) throws Exception {

    TreeMap<Coordinate, PlotPageTableValue> result = new TreeMap<Coordinate, PlotPageTableValue>();

    for (Coordinate time : pageData.keySet()) {
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
  public void add(Coordinate coordinate, PlotPageColumnHeading heading,
    PlotPageTableValue value) {

    if (!pageData.containsKey(coordinate)) {
      // Create the time entry
      addCoordinate(coordinate);
    }

    pageData.get(coordinate).put(heading, value);
  }

  public void add(Coordinate coordinate, PlotPageColumnHeading heading,
    SensorValue value) throws RoutineException {

    if (!pageData.containsKey(coordinate)) {
      // Create the time entry
      addCoordinate(coordinate);
    }

    pageData.get(coordinate).put(heading,
      new SensorValuePlotPageTableValue(value));
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
    List<Long> coordinateIds) {

    List<SensorValue> result = new ArrayList<SensorValue>(coordinates.size());

    PlotPageColumnHeading heading = getHeading(columnId);

    for (Long coordinateId : coordinateIds) {

      Map<PlotPageColumnHeading, PlotPageTableValue> timeEntry = pageData
        .get(coordinates.get(coordinateId));
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

  public int getNeedsFlagCount() {
    int result = 0;

    for (LinkedHashMap<PlotPageColumnHeading, PlotPageTableValue> entry : pageData
      .values()) {
      for (PlotPageTableValue value : entry.values()) {
        if (null != value && value.getFlagNeeded()) {
          result++;
        }
      }
    }

    return result;
  }

  public List<Coordinate> getCoordinates() {
    return new ArrayList<Coordinate>(coordinates.values());
  }
}
