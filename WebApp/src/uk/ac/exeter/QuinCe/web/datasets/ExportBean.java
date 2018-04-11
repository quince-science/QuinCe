package uk.ac.exeter.QuinCe.web.datasets;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

@ManagedBean
@SessionScoped
public class ExportBean extends BaseManagedBean {

  /**
   * Navigation to the export page
   */
  private static final String NAV_EXPORT_PAGE = "export";

  /**
   * The database ID of the dataset to be exported
   */
  private DataSet dataset = null;

  /**
   * Initialise the bean
   */
  public String start() {
    return NAV_EXPORT_PAGE;
  }

  /**
   * Get the dataset ID
   * @return The dataset ID
   */
  public long getDatasetId() {
    long result = -1;
    if (dataset != null) {
      result = dataset.getId();
    }

    return result;
  }

  /**
   * Set the dataset using its ID
   * @param datasetId The dataset ID
   */
  public void setDatasetId(long datasetId) {
    try {
      this.dataset = DataSetDB.getDataSet(getDataSource(), datasetId);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the dataset
   * @return The dataset
   */
  public DataSet getDataset() {
    return dataset;
  }

  /**
   * Set the dataset
   * @param dataset The dataset
   */
  public void setDataset(DataSet dataset) {
    this.dataset = dataset;
  }
}
