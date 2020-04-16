package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

public abstract class PlotPage2Bean extends BaseManagedBean {

  /**
   * Navigation to the dataset list
   */
  private static final String NAV_DATASET_LIST = "dataset_list";

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
   * The data for the page
   */
  protected PlotPage2Data data;

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
  public PlotPage2Data getData() {
    return data;
  }

  /**
   * Get the navigation to the plot screen
   *
   * @return The navigation to the plot screen
   */
  protected abstract String getScreenNavigation();

  /**
   * Clear all cached data
   */
  private void reset() {
    dataset = null;
    dirty = false;
  }

  /**
   * Get the navigation string for when the plot page is finished with.
   *
   * @return The navigation string.
   */
  protected String getFinishNavigation() {
    return NAV_DATASET_LIST;
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

      dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
      initDataObject();

    } catch (Exception e) {
      return internalError(e);
    }

    /*
     * try { reset();
     *
     * dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
     *
     * initData(); selectableRows = buildSelectableRows();
     *
     * plot1 = new Plot(this, dataset.getBounds(), getDefaultPlot1XAxis(),
     * getDefaultPlot1YAxis(), getDefaultMap1Variable()); plot2 = new Plot(this,
     * dataset.getBounds(), getDefaultPlot2XAxis(), getDefaultPlot2YAxis(),
     * getDefaultMap2Variable());
     *
     * init(); dirty = false; } catch (Exception e) { e.printStackTrace(); }
     */

    return getScreenNavigation();
  }

  /**
   * Load the page data.
   * <p>
   * On completion of this method the bean will assume that {@link #data} is
   * populated and ready for use.
   * </p>
   */
  public void loadData() {
    data.loadData(getDataSource());
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
  protected abstract void initDataObject();

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
    return data.getErrorMessage();
  }

}
