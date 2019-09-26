package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.BeanException;
import uk.ac.exeter.QuinCe.web.datasets.data.DatasetMeasurementData;
import uk.ac.exeter.QuinCe.web.datasets.data.Field;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldSets;
import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Bean for pages containing plots and tables
 *
 * @author Steve Jones
 *
 */
public abstract class PlotPageBean extends BaseManagedBean {

  /**
   * The ID of the data set being processed
   */
  protected long datasetId;

  /**
   * The data set being processed
   */
  protected DataSet dataset;

  /**
   * The field sets for the current dataset
   */
  protected FieldSets fieldSets;

  /**
   * The table content for the current field set
   */
  protected DatasetMeasurementData pageData;

  /**
   * The data for the current view of the data table.
   *
   * <p>
   * The table data is loaded on demand as the user scrolls around, to eliminate
   * the delay and memory requirements of loading a complete voyage's data in
   * one go. When the user scrolls to a particular row, the data for that row
   * and a set of rows before and after it is loaded.
   * </p>
   *
   * @see #generateTableData()
   * @see <a href=
   *      "https://datatables.net/examples/data_sources/server_side.html">DataTables
   *      Server-Side Processing</a>
   */
  private String tableJsonData = null;

  /**
   * An internal value for the DataTables library, used when drawing retrieving
   * table data from the server
   *
   * @see <a href=
   *      "https://datatables.net/examples/data_sources/server_side.html">DataTables
   *      Server-Side Processing</a>
   */
  private int tableDataDraw;

  /**
   * The first row of the table data view to be loaded for DataTables
   *
   * @see <a href=
   *      "https://datatables.net/examples/data_sources/server_side.html">DataTables
   *      Server-Side Processing</a>
   */
  private int tableDataStart;

  /**
   * The number of rows to be loaded for DataTables
   *
   * @see <a href=
   *      "https://datatables.net/examples/data_sources/server_side.html">DataTables
   *      Server-Side Processing</a>
   */
  private int tableDataLength;

  /**
   * The currently selected field set
   */
  private long currentFieldSet = 0;

  /**
   * A Javascript array string containing the list of all row numbers in the
   * current data file that can be selected in the data table. Selectable rows
   * can have their WOCE flag set by the user. Unselectable rows are typically
   * rows that have their QC flag set to FATAL, which means they cannot be
   * processed at all.
   *
   * <p>
   * The rows are loaded during {@link #start} via
   * {@link #buildSelectableRows()}.
   * </p>
   */
  protected String selectableRows = null;

  /**
   * The index of the selected column. Can be converted to a field using the
   * fieldSets
   */
  protected int selectedColumn = -1;

  /**
   * The row numbers that have been selected by the user. Stored as a
   * comma-separated list.
   */
  protected String selectedRows = null;

  /**
   * The data for the first plot as a JSON string
   */
  protected Plot plot1;

  /**
   * The data for the second plot as a JSON string
   */
  protected Plot plot2;

  /**
   * Dirty data indicator
   */
  protected boolean dirty = false;

  /**
   * Get the dataset ID
   *
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Set the dataset ID
   *
   * @param datasetId
   *          The dataset ID
   */
  public void setDatasetId(long datasetId) {
    this.datasetId = datasetId;
  }

  /**
   * Get the current DataSet object
   *
   * @return The data set
   */
  public DataSet getDataset() {
    return dataset;
  }

  /**
   * Get the data for the current view in the data table
   *
   * @return The table data
   * @see #tableJsonData
   */
  public String getTableJsonData() {
    return tableJsonData;
  }

  /**
   * Set the data for the current view in the data table
   *
   * @param tableJsonData
   *          The table data
   * @see #tableJsonData
   */
  public void setTableJsonData(String tableJsonData) {
    this.tableJsonData = tableJsonData;
  }

  /**
   * Dummy set for the variable set JSON
   *
   * @param variableSetJson
   *          Ignored.
   */
  public void setVariableSetJson(String variableSetJson) {
    // Do nothing
  }

  /**
   * Get the total number of rows in the data file. If the number of rows is not
   * known, the result will be negative.
   *
   * <p>
   * Note that this is the number of atmospheric or ocean records, depending on
   * what is being displayed.
   * </p>
   *
   * @return The number of records
   */
  public int getRecordCount() {
    int result = -1;
    if (null != pageData) {
      result = pageData.size();
    }

    return result;
  }

  /**
   * Set the record count (dummy method)
   *
   * @param recordCount
   *          Ignored.
   */
  public void setRecordCount(int recordCount) {
    // Do nothing
  }

  /**
   * Get the current value for the DataTables internal {@code draw} parameter
   *
   * @return The DataTables {@code draw} parameter
   * @see #tableDataDraw
   */
  public int getTableDataDraw() {
    return tableDataDraw;
  }

  /**
   * Set the value for the DataTables internal {@code draw} parameter
   *
   * @param tableDataDraw
   *          The DataTables {@code draw} parameter
   * @see #tableDataDraw
   */
  public void setTableDataDraw(int tableDataDraw) {
    this.tableDataDraw = tableDataDraw;
  }

  /**
   * Get the first row of the current view in the data table
   *
   * @return The first row of the view
   * @see #tableDataStart
   */
  public int getTableDataStart() {
    return tableDataStart;
  }

  /**
   * Set the first row of the view in the data table
   *
   * @param tableDataStart
   *          The first row of the view
   * @see #tableDataStart
   */
  public void setTableDataStart(int tableDataStart) {
    this.tableDataStart = tableDataStart;
  }

  /**
   * Get the number of rows in the current view in the data table
   *
   * @return The number of rows in the view
   * @see #tableDataLength
   */
  public int getTableDataLength() {
    return tableDataLength;
  }

  /**
   * Set the number of rows in the current view in the data file
   *
   * @param tableDataLength
   *          The number of rows in the view
   * @see #tableDataLength
   */
  public void setTableDataLength(int tableDataLength) {
    this.tableDataLength = tableDataLength;
  }

  /**
   * Get the list of selectable row IDs
   *
   * @return The selectable rows
   */
  public String getSelectableRows() {
    return selectableRows;
  }

  /**
   * Dummy method to set selectable rows (required by bean)
   *
   * @param dummy
   *          Ignored
   */
  public void setSelectableRows(String dummy) {
    // Do nothing
  }

  /**
   * Get the set of rows that have been selected by the user. The rows are
   * returned as an unsorted comma-separated list.
   *
   * @return The selected rows
   */
  public String getSelectedRows() {
    return selectedRows;
  }

  /**
   * Get the selected rows as a list of numbers
   *
   * @return The selected rows
   */
  public List<LocalDateTime> getSelectedRowsList() {
    List<Long> rowLongs = StringUtils.delimitedToLongList(selectedRows);
    return rowLongs.stream().map(DateTimeUtils::longToDate)
      .collect(Collectors.toList());
  }

  public int getSelectedColumn() {
    return selectedColumn;
  }

  /**
   * Set the selected column index
   *
   * @param selectedRows
   *          The selected column index
   * @see #selectedColumn
   */
  public void setSelectedColumn(int selectedColumn) {
    this.selectedColumn = selectedColumn;
  }

  /**
   * Set the selected table rows.
   *
   * @param selectedRows
   *          The selected rows
   * @see #selectedRows
   */
  public void setSelectedRows(String selectedRows) {
    this.selectedRows = selectedRows;
  }

  /**
   * Retrieve the data for the table from the database as a JSON string. The
   * data is stored in {@link #tableJsonData}.
   */
  public void generateTableData() {
    try {
      tableJsonData = getTableData(tableDataStart, tableDataLength);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Clear existing table data. This will force calls to generateTableData to
   * reinitialise everything.
   */
  private void clearTableData() {
    tableJsonData = null;
  }

  /**
   * Get the list of field set IDs
   *
   * @return The field set IDs
   * @throws DatabaseException
   * @throws VariableNotFoundException
   * @throws MissingParamException
   */
  public FieldSets getFieldSets() {
    return fieldSets;
  }

  /**
   * Get the details for Plot 1
   *
   * @return The plot 1 details
   */
  public Plot getPlot1() {
    return plot1;
  }

  /**
   * Get the details for Plot 2
   *
   * @return The plot 2 details
   */
  public Plot getPlot2() {
    return plot2;
  }

  /**
   * Reload all the data on the page
   */
  public void reloadPageData() {
    reloadPlotData(1);
    reloadPlotData(2);
    clearTableData();
    generateTableData();
  }

  /**
   * Reload data for a given plot. Can be used if the plot is changed.
   *
   * @param plotIndex
   *          The plot that needs to be reloaded
   */
  public void reloadPlotData(int plotIndex) {
    try {
      switch (plotIndex) {
      case 1: {
        // plot1Labels = buildPlotLabels(1);
        // plot1Data = loadPlotData(1);
        break;
      }
      case 2: {
        // plot2Labels = buildPlotLabels(2);
        // plot2Data = buildPlotLabels(2);
        break;
      }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Start a new bean instance
   *
   * @return Navigation to the plot page
   */
  public String start() {
    try {
      reset();

      ResourceManager resourceManager = ResourceManager.getInstance();
      dataset = DataSetDB.getDataSet(getDataSource(), datasetId);

      initData();
      selectableRows = buildSelectableRows();

      plot1 = new Plot(this, dataset.getBounds(), getDefaultPlot1XAxis(),
        getDefaultPlot1YAxis(), getDefaultMap1Variable());
      plot2 = new Plot(this, dataset.getBounds(), getDefaultPlot2XAxis(),
        getDefaultPlot2YAxis(), getDefaultMap2Variable());

      init();
      dirty = false;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return getScreenNavigation();
  }

  /**
   * Clear all cached data
   */
  protected void reset() {
    dataset = null;
    pageData = null;
    fieldSets = null;
    plot1 = null;
    plot2 = null;
    selectableRows = null;
    selectedRows = null;
  }

  /**
   * Get the current table mode
   *
   * @return The table mode
   */
  public long getFieldSet() {
    return currentFieldSet;
  }

  /**
   * Set the table mode
   *
   * @param sensorsFieldset
   *          The table mode
   */
  public void setFieldSet(long fieldSet) {
    this.currentFieldSet = fieldSet;
  }

  /**
   * Perform bean-specific initialisation
   */
  protected abstract void init();

  /**
   * Get the navigation to the plot screen
   *
   * @return The navigation to the plot screen
   */
  protected abstract String getScreenNavigation();

  /**
   * Build the list of selectable record IDs. By default, this is all rows
   *
   * @throws Exception
   *           If the list cannot be created
   */
  protected String buildSelectableRows() throws Exception {
    String result = "";
    if (!dataset.isNrt()) {
      result = pageData.getRowIdsJson();
    }

    return result;
  }

  /**
   * Build the labels for the plot
   */
  protected abstract String buildPlotLabels(int plotIndex);

  /**
   * Get the page data object
   *
   * @return The page data object
   */
  public DatasetMeasurementData getTableData() {
    return pageData;
  }

  /**
   * Load the data for the specified portion of the table as a JSON string
   *
   * @param start
   *          The first record to retrieve
   * @param length
   *          The number of records to retrieve
   * @return The table data
   */
  protected String getTableData(int start, int length) throws Exception {

    pageData.loadRows(start, length);

    // TODO Convert to GSON - I don't have the API here

    JSONArray json = new JSONArray();

    for (int i = start; i < start + length; i++) {

      JSONObject obj = new JSONObject();

      obj.put("DT_RowId",
        DateTimeUtils.dateToLong(pageData.getRowIds().get(i)));

      int columnIndex = 0;
      obj.put(String.valueOf(columnIndex), pageData.getRowIds().get(i));

      LinkedHashMap<Field, FieldValue> row = pageData
        .get(pageData.getRowIds().get(i));

      if (null == row) {
        throw new BeanException("Page Data row not loaded");
      }

      for (FieldValue value : row.values()) {

        if (null == value) {
          columnIndex++;
          obj.put(String.valueOf(columnIndex), JSONObject.NULL);
        } else {
          JSONArray cellData = new JSONArray();

          if (value.getValue().isNaN()) {
            cellData.put(JSONObject.NULL);
          } else {
            cellData.put(value.getValue());
          }

          cellData.put(value.isUsed());
          cellData.put(value.getQcFlag().getFlagValue());
          cellData.put(value.needsFlag());
          cellData.put(value.getQcComment());

          columnIndex++;
          obj.put(String.valueOf(columnIndex), cellData);
        }

      }

      json.put(obj);
    }

    return json.toString();
  }

  /**
   * Get the default variable to use on the X axis of Plot 1
   *
   * @return The default variable
   */
  protected Field getDefaultPlot1XAxis() {
    return fieldSets.getRowIdField();
  }

  /**
   * Get the default variables to use on the Y axis of Plot 1
   *
   * @return The default variables
   */
  protected abstract Field getDefaultPlot1YAxis();

  /**
   * Get the default variable to use on the X axis of Plot 2
   *
   * @return The default variable
   */
  protected Field getDefaultPlot2XAxis() {
    return fieldSets.getRowIdField();
  }

  /**
   * Get the default variables to use on the Y axis of Plot 2
   *
   * @return The default variables
   */
  protected abstract Field getDefaultPlot2YAxis();

  /**
   * Get the default variable to use on Map 1
   *
   * @return The default variable
   */
  protected Field getDefaultMap1Variable() {
    return getDefaultPlot1YAxis();
  }

  /**
   * Get the default variable to use on Map 2
   *
   * @return The default variable
   */
  protected Field getDefaultMap2Variable() {
    return getDefaultPlot2YAxis();
  }

  /**
   * Get the variable group details
   *
   * @return The variable group details
   */
  public String getVariableGroups() throws Exception {
    return new Gson().toJson(getFieldSets());
  }

  /**
   * Get the variable group names as a JSON string
   *
   * @return The variable group names
   */
  public String getVariableGroupNames() throws Exception {
    return new Gson().toJson(getFieldSets(true).keySet());
  }

  /**
   * Get the bounds of the data as a JSON string
   *
   * @return The data bounds
   */
  public String getDataBounds() {
    return dataset.getBounds().toJson();
  }

  /**
   * Dummy
   *
   * @param dataBounds
   *          Ignored
   */
  public void setDataBounds(String dataBounds) {
    // Do nothing
  }

  /**
   * Indicates whether or not the plot page contains two plots
   *
   * @return {@code true} if two plots are required; {@code false} if only one
   *         plot.
   */
  public abstract boolean getHasTwoPlots();

  /**
   * Indicates whether or not changes can be made to the data
   *
   * @return {@code true} if data can be edited; {@code false} if not
   */
  public boolean getCanEdit() {
    // NRT data sets cannot be edited
    return !getDataset().isNrt();
  }

  /**
   * Initialise the data structure for displaying data. Set up columns and row
   * IDs
   *
   * @throws Exception
   */
  protected abstract void initData() throws Exception;

  /**
   * Get the page data object
   *
   * @return The page data
   */
  public DatasetMeasurementData getData() {
    return pageData;
  }

  /**
   * Get the indexes of the columns that can be selected in the data table
   *
   * @return
   */
  public abstract List<Integer> getSelectableColumns();

  /**
   * Get the field sets for the current dataset
   *
   * @param includeTimePos
   *          Include the base field set containing time and position
   * @return
   * @throws Exception
   */
  public abstract LinkedHashMap<String, Long> getFieldSets(
    boolean includeTimePos) throws Exception;

  public boolean isNrt() {
    return dataset.isNrt();
  }
}
