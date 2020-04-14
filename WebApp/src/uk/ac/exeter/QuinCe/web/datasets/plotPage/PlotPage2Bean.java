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

}
