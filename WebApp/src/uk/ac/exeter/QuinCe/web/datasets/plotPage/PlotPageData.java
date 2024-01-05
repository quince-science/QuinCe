package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sql.DataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDataDB;
import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.data.Dataset.RunTypePeriods;
import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public abstract class PlotPageData {

  /**
   * Indicates a select action
   */
  protected static final int SELECT = 1;

  /**
   * Indicates a deselect action
   */
  protected static final int DESELECT = -1;

  /**
   * The database connection for retrieving data
   */
  protected DataSource dataSource = null;

  /**
   * The dataset whose data is represented.
   */
  protected final DataSet dataset;

  /**
   * The instrument that the dataset belongs to.
   */
  protected final Instrument instrument;

  /**
   * The Run Type periods
   */
  protected RunTypePeriods runTypePeriods;

  /**
   * Json serialization type for lists of longs
   */
  Type longList = new TypeToken<List<Long>>() {
  }.getType();

  /**
   * Gson instance for serializing table data
   */
  private Gson tableDataGson;

  /**
   * An error string to display to the user if something goes wrong.
   */
  private String errorMessage = null;

  /**
   * The column headers for the data
   */
  protected LinkedHashMap<String, List<PlotPageColumnHeading>> columnHeadings = null;

  /**
   * The extended column headers for the data
   */
  protected LinkedHashMap<String, List<PlotPageColumnHeading>> extendedColumnHeadings = null;

  /**
   * Indicates whether or not data has been loaded.
   */
  protected boolean loaded = false;

  /**
   * The column for which values have been selected
   */
  protected long selectedColumn = -1L;

  /**
   * The IDs of the selected rows
   */
  protected List<Long> selectedRows = new ArrayList<Long>();

  /**
   * The ID of the row that was just selected/deselected
   */
  protected long clickedRow = -1L;

  /**
   * The ID of the last row that was selected/deselected
   */
  protected long prevClickedRow = -1L;

  /**
   * Indicates whether the last selection action was a select or a deselect
   */
  protected int lastSelectionAction = SELECT;

  /**
   * Details for the first plot
   */
  private Plot plot1 = null;

  /**
   * Details for the second plot
   */
  private Plot plot2 = null;

  /**
   * Details for the first map
   */
  private QCMap map1 = null;

  /**
   * Details for the second map
   */
  private QCMap map2 = null;

  /**
   * Cache of data structured for maps
   */
  private Map<PlotPageColumnHeading, MapRecords> mapCache = new HashMap<PlotPageColumnHeading, MapRecords>();

  /**
   * The indicator of the root field group.
   *
   * <p>
   * This group is used as the basis for the page table - the columns are always
   * visible as the reference for the rest of the table. These columns are also
   * given a special status in plots and maps.
   * </p>
   */
  public static final String ROOT_FIELD_GROUP = "Base";

  public static final String SENSORS_FIELD_GROUP = "Sensors";

  public static final String DIAGNOSTICS_FIELD_GROUP = "Diagnostics";

  public static final String MEASUREMENTVALUES_FIELD_GROUP = "Measurement Values";

  protected PlotPageData(DataSource dataSource, Instrument instrument,
    DataSet dataset) throws SQLException {
    this.dataSource = dataSource;
    this.instrument = instrument;
    this.dataset = dataset;
  }

  /**
   * Load all page data.
   *
   * <p>
   * This calls {@link #loadDataAction(DataSource)} to do the actual loading
   * work.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @see #loadDataAction(DataSource)
   */
  public void loadData() {
    try {
      loadDataAction();

      if (instrument.hasRunTypes()) {
        DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
        try (Connection conn = dataSource.getConnection()) {
          runTypePeriods = DataSetDataDB.getRunTypePeriods(conn, instrument,
            dataset.getId());
        } catch (SQLException e) {
          error("Error loading data", e);
        }
      } else {
        runTypePeriods = new RunTypePeriods();
      }

      // Initialise Gson builder
      tableDataGson = new GsonBuilder()
        .registerTypeAdapter(PlotPageTableRecord.class,
          new PlotPageTableRecordSerializer(getAllSensorValues()))
        .create();

      // Initialise the plots
      plot1 = new Plot(this, getDefaultXAxis1(), getDefaultYAxis1(),
        !dataset.isNrt());
      plot2 = new Plot(this, getDefaultXAxis2(), getDefaultYAxis2(),
        !dataset.isNrt());
      map1 = new QCMap(this, getDefaultMap1Column(), !dataset.isNrt());
      map2 = new QCMap(this, getDefaultMap2Column(), !dataset.isNrt());

      loaded = true;
    } catch (Exception e) {
      error("Error while loading dataset data", e);
    }
  }

  /**
   * The actual method for loading the data.
   *
   * @param dataSource
   *          A data source.
   */
  protected abstract void loadDataAction() throws Exception;

  /**
   * Get the complete set of {@link SensorValue}s for the dataset.
   *
   * @return The dataset's SensorValues.
   */
  protected abstract DatasetSensorValues getAllSensorValues();

  /**
   * Get the standard column headings for the table in groups, without QC
   * columns.
   *
   * <p>
   * This set of column headings will contain the standard list for the data
   * table, with combined columns for position and similar combinations.
   * </p>
   *
   * <p>
   * Although all data values require accompanying QC Flag and QC Message
   * columns, they must not be included in the output of this method. The
   * application will ensure that they are included in the necessary places.
   * </p>
   *
   * <p>
   * The column headings will be returned as a map of
   * {@code <group>, <header list>} so the headings can be grouped. The map is a
   * {@link LinkedHashMap} so iterating over the map keys will always give the
   * same group (and therefore column) order.
   * </p>
   *
   * <p>
   * The first group should be named by {@link #ROOT_FIELD_GROUP}. This group
   * will be 'locked' in the display table so its columns are always visible.
   * </p>
   *
   * @return The column headings
   * @throws Exception
   */
  public LinkedHashMap<String, List<PlotPageColumnHeading>> getColumnHeadings()
    throws Exception {
    if (null == columnHeadings) {
      buildColumnHeadings();
    }

    return columnHeadings;
  }

  /**
   * Get the extended column headings for the table in groups, without QC
   * columns.
   *
   * <p>
   * This set of column headings will contain the complete list of column
   * headings with no combinations (e.g. separate latitude and longitude
   * columns).
   * </p>
   *
   * <p>
   * Although all data values require accompanying QC Flag and QC Message
   * columns, they must not be included in the output of this method. The
   * application will ensure that they are included in the necessary places.
   * </p>
   *
   * <p>
   * The column headings will be returned as a map of
   * {@code <group>, <header list>} so the headings can be grouped. The map is a
   * {@link LinkedHashMap} so iterating over the map keys will always give the
   * same group (and therefore column) order.
   * </p>
   *
   * <p>
   * The first group should be named by {@link #ROOT_FIELD_GROUP}. This group
   * will be 'locked' in the display table so its columns are always visible.
   * </p>
   *
   * @return The column headings
   * @throws Exception
   */
  public LinkedHashMap<String, List<PlotPageColumnHeading>> getExtendedColumnHeadings()
    throws Exception {
    if (null == extendedColumnHeadings) {
      buildColumnHeadings();
    }

    return extendedColumnHeadings;
  }

  /**
   * Build the column headings
   */
  protected abstract void buildColumnHeadings() throws Exception;

  /**
   * Get the standard column headings in JSON format.
   *
   * <pre>
   * The JSON will be an array of objects, one for each group. Each object will
   * contain the group name and an array of the column headings within that
   * group.
   * </p>
   *
   * <pre>
   * {[ { group: 'Group 1', headings: ['Heading 1', 'Heading 2'] }, { group:
   * 'Group 2', headings: ['Heading 3', 'Heading 4'] } ]}
   * </pre>
   *
   *
   * @return The table headings JSON.
   * @throws Exception
   */
  public String getColumnHeadingsJson() throws Exception {
    return buildColumnHeadingsJson(getColumnHeadings());
  }

  /**
   * Get the extended column headings in JSON format.
   *
   * <pre>
   * The JSON will be an array of objects, one for each group. Each object will
   * contain the group name and an array of the column headings within that
   * group.
   * </p>
   *
   * <pre>
   * {[ { group: 'Group 1', headings: ['Heading 1', 'Heading 2'] }, { group:
   * 'Group 2', headings: ['Heading 3', 'Heading 4'] } ]}
   * </pre>
   *
   *
   * @return The table headings JSON.
   * @throws Exception
   */
  public String getExtendedColumnHeadingsJson() throws Exception {
    return buildColumnHeadingsJson(getExtendedColumnHeadings());
  }

  private String buildColumnHeadingsJson(
    LinkedHashMap<String, List<PlotPageColumnHeading>> headings) {

    String result = null;

    if (loaded) {
      // Reorganise the column headings into a structure that can be used to
      // build the JSON
      List<JsonColumnGroup> jsonGroups = new ArrayList<JsonColumnGroup>(
        headings.size());

      if (validateColumnHeadings(headings)) {
        for (Map.Entry<String, List<PlotPageColumnHeading>> group : headings
          .entrySet()) {
          jsonGroups.add(new JsonColumnGroup(group));
        }
      }

      // Convert the reorganised data to JSON
      result = new GsonBuilder().serializeNulls().create().toJson(jsonGroups);
    }

    return result;
  }

  /**
   * Get the columns indices at which each column group is positioned in the
   * full list of headings.
   *
   * <p>
   * The result is a map of {@code groupName -> first column index}.
   * </p>
   *
   * <p>
   * The root column group is not included because that group's columns are
   * always visible in the displayed table.
   * </p>
   *
   * @return The column indices for each group.
   * @throws Exception
   */
  public LinkedHashMap<String, Integer> getColumnGroupOffsets()
    throws Exception {

    LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();

    int nextColumn = 0;
    for (Map.Entry<String, List<PlotPageColumnHeading>> groupEntry : getColumnHeadings()
      .entrySet()) {

      if (!groupEntry.getKey().equals(ROOT_FIELD_GROUP)) {
        result.put(groupEntry.getKey(), nextColumn);
        nextColumn += groupEntry.getValue().size();
      }
    }

    return result;
  }

  /**
   * Validate the column headings supplied by {@link #getColumnHeadings()}.
   *
   * @param headings
   *          The column headings
   * @return {@code true} if the column headings are valid; {@code false} if not
   */
  private boolean validateColumnHeadings(
    LinkedHashMap<String, List<PlotPageColumnHeading>> headings) {

    boolean valid = true;

    // There must be at least one column group
    if (null == headings || headings.size() == 0) {
      error("No column headings available");
      valid = false;
    } else {
      for (Map.Entry<String, List<PlotPageColumnHeading>> group : headings
        .entrySet()) {

        // A column group name cannot be null or empty
        if (StringUtils.isBlank(group.getKey())) {
          error("Blank column group detected");
          valid = false;
        }

        List<PlotPageColumnHeading> groupColumns = group.getValue();

        // Each column group must contain at least one column
        if (null == groupColumns || groupColumns.size() == 0) {
          // Variables that have no calculations will have an empty column
          // group, so we'll ignore this issue for now. Maybe we'll have to fix
          // it in the future though.

          // error("Empty column group detected",
          // new Exception("Empty column group '" + group.getKey() + "'"));
        } else {

          for (int i = 0; i < groupColumns.size(); i++) {

            String columnName = groupColumns.get(i).getShortName();

            // Blank column is not allowed
            if (StringUtils.isBlank(columnName)) {
              error("Blank column heading detected",
                new Exception("Blank column heading in group " + group.getKey()
                  + ", index " + i));
            } else if (columnName
              .equals(FileDefinition.LONGITUDE_COLUMN_NAME)) {

              // Longitude must be followed by Latitude
              if (i + 1 == groupColumns.size() || !groupColumns.get(i + 1)
                .getShortName().equals(FileDefinition.LATITUDE_COLUMN_NAME)) {
                error("Invalid position columns",
                  new Exception("Longitude must be followed by Latitude"));
              }
            } else if (columnName.equals(FileDefinition.LATITUDE_COLUMN_NAME)) {

              // Latitude must be preceded by Longitude
              if (i == 0 || !groupColumns.get(i - 1).getShortName()
                .equals(FileDefinition.LONGITUDE_COLUMN_NAME)) {

                error("Invalid position columns",
                  new Exception("Latitude must be preceded by Longitude"));
              }
            }
          }
        }
      }
    }

    return valid;
  }

  protected boolean isColumnEditable(long columnId) throws Exception {

    boolean editable = false;

    Map<String, List<PlotPageColumnHeading>> columnHeadings = getColumnHeadings();

    mainLoop: for (List<PlotPageColumnHeading> groupHeadings : columnHeadings
      .values()) {
      for (PlotPageColumnHeading heading : groupHeadings) {
        if (heading.getId() == columnId) {
          editable = heading.canEdit();
          break mainLoop;
        }
      }
    }

    return editable;
  }

  /**
   * Get the last set error message.
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Register an exception encountered during data processing.
   *
   * @param e
   *          The exception.
   */
  protected void error(Exception e) {
    if (e instanceof NullPointerException) {
      error("NullPointerException", e);
    } else {
      error(e.getMessage(), e);
    }
  }

  /**
   * Register a simple error encountered during data processing.
   *
   * <p>
   * The error message will be displayed to the user. A dummy exception will be
   * created and logged.
   * </p>
   *
   * @param message
   *          The error message.
   */
  protected void error(String message) {
    error(message, new Exception(message));
  }

  /**
   * Register an error encountered during data processing.
   *
   * <p>
   * The error message will be displayed to the user. The cause will be logged
   * to the system.
   * </p>
   *
   * @param message
   *          The error message.
   * @param cause
   *          The underlying cause.
   */
  protected void error(String message, Throwable cause) {

    long millis = DateTimeUtils.dateToLong(LocalDateTime.now());
    this.errorMessage = millis + ": " + message;
    ExceptionUtils.printStackTrace(cause);
  }

  /**
   * Get the number of records in the dataset.
   *
   * <p>
   * This is equivalent to the number of records that will be shown in the QC
   * table.
   * </p>
   *
   * @return The dataset size.
   */
  public abstract int size();

  /**
   * Generate the data for the table as a JSON String.
   *
   * <p>
   * The format for the JSON string is specified by
   * {@link PlotPageBean#generateTableData()}.
   * </p>
   *
   * @param start
   *          The first row to generate.
   * @param length
   *          The number of rows to generate.
   * @return The JSON string.
   *
   * @see PlotPageBean#generateTableData()
   */
  public String generateTableData(int start, int length) {

    String result = null;

    if (loaded) {
      List<PlotPageTableRecord> records = generateTableDataRecords(start,
        length);
      result = tableDataGson.toJson(records);
    }

    return result;
  }

  /**
   * Get the complete list of row IDs.
   *
   * @return The row IDs.
   */
  public abstract List<Long> getRowIDs();

  /**
   * Return the list of row IDs as a JSON string array.
   *
   * <p>
   * We can't use the standard JSF mechanism to directly return a
   * {@code List<String>} to the front end, because JSF 'helpfully' converts
   * numeric values to numbers instead of keeping them as strings which screws
   * up the Javascript processing.
   * </p>
   *
   * @return The row IDs as a JSON string.
   */
  public String getRowIDsJson() {
    return new Gson().toJson(getRowIDs());
  }

  /**
   * Generate the table data records for {@link #generateTableData(int, int)}.
   *
   * @param start
   *          The first row to generate.
   * @param length
   *          The number of rows to generate.
   * @return The table records.
   */
  protected abstract List<PlotPageTableRecord> generateTableDataRecords(
    int start, int length);

  /**
   * Utility class to represent a column header group as an independent Java
   * object.
   *
   * <p>
   * This is used by {@link PlotPageData#getColumnHeadingsJson()} to construct
   * the JSON object.
   * </p>
   *
   * @author Steve Jones
   *
   */
  class JsonColumnGroup {

    protected String group;

    protected List<PlotPageColumnHeading> headings;

    private JsonColumnGroup(Entry<String, List<PlotPageColumnHeading>> group2) {
      this.group = group2.getKey();
      this.headings = group2.getValue();
    }
  }

  /**
   * Get the currently selected column index.
   *
   * @return The selected column index.
   */
  public long getSelectedColumn() {
    return selectedColumn;
  }

  /**
   * Set the currently selected column index.
   *
   * @param selectedColumn
   *          The selected column index.
   */
  public void setSelectedColumn(long selectedColumn) {
    this.selectedColumn = selectedColumn;
  }

  /**
   * Get the current list of selected rows.
   *
   * @return The selected rows.
   */
  public String getSelectedRows() {
    return new Gson().toJson(selectedRows);
  }

  /**
   * Set the list of currently selected rows.
   *
   * <p>
   * Converts all supplied values to Strings. The method assumes that the
   * strings are already sorted.
   * </p>
   *
   * @param selectedRows
   *          The selected rows.
   */
  public void setSelectedRows(String selectedRows) {
    this.selectedRows = new Gson().fromJson(selectedRows, longList);
  }

  public void clearSelection() {
    this.selectedRows = new ArrayList<Long>();
  }

  /**
   * Get the ID of the row that was just clicked.
   *
   * @return The clicked row
   */
  public long getClickedRow() {
    return clickedRow;
  }

  /**
   * Set the ID of the row that was just clicked.
   *
   * @param clickedRow
   *          The clicked row.
   */
  public void setClickedRow(long clickedRow) {
    this.clickedRow = clickedRow;
  }

  /**
   * Get the ID of the previously clicked row.
   *
   * @return The previously clicked row
   */
  public long getPrevClickedRow() {
    return prevClickedRow;
  }

  /**
   * Set the ID of the previously clicked row.
   *
   * @param prevClickedRow
   *          The previously clicked row.
   */
  public void setPrevClickedRow(long prevClickedRow) {
    this.prevClickedRow = prevClickedRow;
  }

  /**
   * Get the last selection action
   *
   * @return The last selection action
   * @see #SELECT
   * @see #DESELECT
   */
  public int getLastSelectionAction() {
    return lastSelectionAction;
  }

  /**
   * Set the last selection action
   *
   * @return The last selection action
   * @see #SELECT
   * @see #DESELECT
   */
  public void setLastSelectionAction(int lastSelectionAction) {
    this.lastSelectionAction = lastSelectionAction;
  }

  /**
   * Select (or deselect) the range of rows specified by {@link #prevClickedRow}
   * and {@link #clickedRow}.
   *
   * @throws Exception
   */
  public void selectRange() throws Exception {

    TreeSet<Long> newSelectedRows = new TreeSet<Long>(selectedRows);

    List<Long> allRows = getRowIDs();

    int rangeStart = allRows.indexOf(prevClickedRow);
    int rangeEnd = allRows.indexOf(clickedRow);

    if (rangeEnd < rangeStart) {
      int temp = rangeStart;
      rangeStart = rangeEnd;
      rangeEnd = temp;
    }

    if (rangeStart < 0) {
      rangeStart = 0;
    }

    if (rangeEnd >= allRows.size()) {
      rangeEnd = allRows.size() - 1;
    }

    for (int i = rangeStart; i <= rangeEnd; i++) {
      if (lastSelectionAction == DESELECT) {
        newSelectedRows.remove(allRows.get(i));
      } else {
        if (canSelectCell(allRows.get(i), selectedColumn)) {
          newSelectedRows.add(allRows.get(i));
        }
      }
    }

    selectedRows = new ArrayList<Long>(newSelectedRows);
    prevClickedRow = clickedRow;
  }

  /**
   * Determine whether or not a cell can be selected by the user.
   *
   * @param row
   *          The row ID.
   * @param column
   *          The column.
   * @return {@code true} if the cell can be selected; {@code false} if not.
   * @throws Exception
   */
  protected boolean canSelectCell(long row, long column) throws Exception {
    return isColumnEditable(column);
  }

  /**
   * Get the details of the first plot
   *
   * @return
   */
  public Plot getPlot1() {
    return plot1;
  }

  /**
   * Get the details of the second plot
   */
  public Plot getPlot2() {
    return plot2;
  }

  protected void initPlots() {
    plot1.init();
    plot2.init();
  }

  /**
   * Get all the values for a given column.
   *
   * @param column
   *          The column.
   * @return The column values.
   */
  protected abstract TreeMap<LocalDateTime, PlotPageTableValue> getColumnValues(
    PlotPageColumnHeading column) throws Exception;

  /**
   * Get the {@link ColumnHeading} for the specified column ID.
   *
   * @param columnId
   *          The column ID.
   * @return The column heading.
   * @throws Exception
   */
  public PlotPageColumnHeading getColumnHeading(long columnId)
    throws Exception {

    PlotPageColumnHeading result = null;

    LinkedHashMap<String, List<PlotPageColumnHeading>> headings = getExtendedColumnHeadings();

    outer: for (String group : headings.keySet()) {
      for (PlotPageColumnHeading heading : headings.get(group)) {
        if (heading.getId() == columnId) {
          result = heading;
          break outer;
        }
      }
    }

    return result;
  }

  protected PlotPageColumnHeading getDefaultXAxis1() throws Exception {
    return getColumnHeadings().get(ROOT_FIELD_GROUP).get(0);
  }

  protected PlotPageColumnHeading getDefaultXAxis2() throws Exception {
    return getDefaultXAxis1();
  }

  protected abstract PlotPageColumnHeading getDefaultYAxis1() throws Exception;

  protected abstract PlotPageColumnHeading getDefaultYAxis2() throws Exception;

  protected PlotPageColumnHeading getDefaultMap1Column() throws Exception {
    return getDefaultYAxis1();
  }

  protected PlotPageColumnHeading getDefaultMap2Column() throws Exception {
    return getDefaultYAxis2();
  }

  /**
   * Get all the column headings as an unstructured list, i.e. not in their
   * groups.
   *
   * @return The column headings.
   * @throws Exception
   */
  protected List<PlotPageColumnHeading> getColumnHeadingsList()
    throws Exception {
    List<PlotPageColumnHeading> headings = new ArrayList<PlotPageColumnHeading>();
    getColumnHeadings().values().forEach(x -> headings.addAll(x));
    return headings;
  }

  /**
   * Clean up the data
   */
  public void destroy() {
  }

  public DataSet getDataset() {
    return dataset;
  }

  public QCMap getMap1() {
    return map1;
  }

  public QCMap getMap2() {
    return map2;
  }

  public Double[] getValueRange(PlotPageColumnHeading column) throws Exception {

    if (!mapCache.containsKey(column)) {
      buildMapCache(column);
    }

    return mapCache.get(column).getValueRange();
  }

  protected abstract List<LocalDateTime> getDataTimes();

  public String getMapData(PlotPageColumnHeading column, GeoBounds bounds,
    boolean useNeededFlags, boolean hideNonGoodFlags) throws Exception {

    if (!mapCache.containsKey(column)) {
      buildMapCache(column);
    }

    return mapCache.get(column).getDisplayJson(bounds, selectedRows,
      useNeededFlags, hideNonGoodFlags);
  }

  private void buildMapCache(PlotPageColumnHeading column) throws Exception {

    MapRecords records = new MapRecords(size());

    if (column.getId() == FileDefinition.TIME_COLUMN_ID) {
      List<LocalDateTime> times = getDataTimes();
      for (LocalDateTime time : times) {
        LatLng position = getAllSensorValues().getClosestPosition(time);
        records.add(new TimeMapRecord(position, time));
      }
    } else {
      TreeMap<LocalDateTime, PlotPageTableValue> values = getColumnValues(
        column);

      for (Map.Entry<LocalDateTime, PlotPageTableValue> entry : values
        .entrySet()) {
        LatLng position = getMapPosition(entry.getKey());
        if (null != position) {
          records.add(new PlotPageValueMapRecord(position, entry.getKey(),
            entry.getValue()));
        }
      }
    }

    mapCache.put(column, records);
  }

  protected abstract DataLatLng getMapPosition(LocalDateTime time)
    throws Exception;

  public boolean getPlot1HideFlags() {
    return null == plot1 ? false : plot1.getHideFlags();
  }

  public void setPlot1HideFlags(boolean hide) {
    plot1.setHideFlags(hide);
    map1.setHideFlags(hide);
  }

  public boolean getPlot2HideFlags() {
    return null == plot2 ? false : plot1.getHideFlags();
  }

  public void setPlot2HideFlags(boolean hide) {
    plot2.setHideFlags(hide);
    map2.setHideFlags(hide);
  }
}
