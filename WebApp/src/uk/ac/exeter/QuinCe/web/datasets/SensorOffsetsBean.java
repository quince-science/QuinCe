package uk.ac.exeter.QuinCe.web.datasets;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

@ManagedBean
@SessionScoped
public class SensorOffsetsBean extends BaseManagedBean {

  private static final String NAV_DATASET_LIST = "dataset_list";

  private static final String NAV_OFFSETS = "sensor_offsets";

  /**
   * The ID of the data set being processed
   */
  protected long datasetId;

  public String start() {
    return NAV_OFFSETS;
  }

  public String finish() {
    return NAV_DATASET_LIST;
  }

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
}
