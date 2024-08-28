package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

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
   * Dirty data indicator
   */
  protected boolean dirty = false;

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
   * Get the current DataSet object.
   *
   * @return The data set.
   */
  public DataSet getDataset() {
    return dataset;
  }

  /**
   * Get the page data.
   *
   * @return The page data.
   */
  public abstract PlotPageData getData();

  /**
   * Get the navigation to the plot screen
   *
   * @return The navigation to the plot screen
   */
  protected abstract String getScreenNavigation();

  /**
   * Clear all cached data
   */
  protected void reset() {
    dataset = null;
    dirty = false;
  }

  /**
   * Get the navigation string for when the plot page is finished with.
   *
   * @return The navigation string.
   */
  protected String getFinishNavigation() {
    return getDatasetListNavigation();
  }

  /**
   * Performs actions required if any data has been changed.
   */
  protected abstract void processDirtyData();

  /**
   * Start a new bean instance
   *
   * @return Navigation to the plot page
   */
  public String start() {
    try {
      reset();
      getProgress().setName("Initialising");
      getProgress().setValue(0F);
      getProgress().setMax(100F);
      dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
      setCurrentInstrumentId(dataset.getInstrumentId());
      initDataObject(getDataSource());
    } catch (Exception e) {
      return internalError(e);
    }

    return getScreenNavigation();
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
    if (null != getData()) {
      result = getData().size();
    }

    return result;
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
   * Get the data for the current view of the data table.
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
  public String getTableJsonData() {
    return tableJsonData;
  }

  /**
   * Retrieve the data for the table from the database as a JSON string. The
   * data is stored in {@link #tableJsonData}.
   *
   * <p>
   * The JSON string is an array of records. Each record is an object consisting
   * of the following:
   * </p>
   * <ul>
   * <li>DT_RowId: The row ID for looking up data</li>
   * <li>0 - ???: The column contents, identified by its column index according
   * to {@link #getColumnHeadings()}</li>
   * </ul>
   *
   * <p>
   * Each of the columns is an array containing:
   * </p>
   *
   * <ul>
   * <li>The value</li>
   * <li>A flag indicating whether or not the value is used for calculating a
   * variable</li>
   * <li>The QC flag value</li>
   * <li>The QC message</li>
   * </ul>
   *
   * @see PlotPageData#generateTableData(int, int)
   */
  public void generateTableData() {
    tableJsonData = getData().generateTableData(tableDataStart,
      tableDataLength);
  }

  /**
   * Load the page data.
   * <p>
   * On completion of this method the bean will assume that {@link #data} is
   * populated and ready for use.
   * </p>
   */
  public void loadData() {
    getData().loadData(getProgress());
  }

  /**
   * Initialise the data object for the plot page.
   *
   * <p>
   * This should create the data object, but not load any data. Data loading is
   * delayed until later ajax calls to allow site responsiveness.
   * </p>
   *
   * @see #loadData()
   */
  protected abstract void initDataObject(DataSource dataSource)
    throws Exception;

  /**
   * Finish with this bean instance, tidying up as necessary.
   *
   * @return Navigation to the destination page after tidying up.
   */
  public String finish() {
    if (dirty) {
      processDirtyData();
    }

    reset();

    return getFinishNavigation();
  }

  /**
   * Abort this bean, cleaning it up without performing any further data
   * processing.
   *
   * @return Navigation to the destination page.
   */
  public String abort() {
    reset();
    return getFinishNavigation();
  }

  /**
   * Get the latest error message from data processing.
   *
   * @return The error message.
   */
  public String getError() {
    PlotPageData data = getData();
    return null == data ? null : data.getErrorMessage();
  }

  /**
   * Indicates whether or not this dataset can be edited.
   *
   * <p>
   * By default, a dataset can be edited unless it is an NRT dataset.
   * </p>
   *
   * @return {@code true} if the dataset can be edited; {@code false} if not.
   *
   * @see DataSet#isNrt()
   */
  public boolean getCanEdit() {
    return null == dataset ? false : !dataset.isNrt();
  }

  public String getUserComments() {
    return dataset.getUserMessages().getDisplayString();
  }

  public void setUserComments(String userComments) {
    dataset.setUserMessages(userComments);
  }

  public void saveUserComments()
    throws MissingParamException, DatabaseException {
    DataSetDB.storeUserMessages(getDataSource(), dataset);
  }

  /**
   * Get the bounds of the data as a JSON string
   *
   * @return The data bounds
   */
  public String getDataBounds() {
    return dataset.getBounds().toJson();
  }

  public boolean allowMaps() {
    return true;
  }

  public boolean dualYAxes() {
    return false;
  }
}
