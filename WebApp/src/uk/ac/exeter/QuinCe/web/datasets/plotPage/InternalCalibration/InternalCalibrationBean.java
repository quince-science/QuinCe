package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.util.Properties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.sql.DataSource;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataSetJob;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageBean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;

@ManagedBean
@SessionScoped
public class InternalCalibrationBean extends PlotPageBean {

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "internal_calibration_plot";

  /**
   * The data for the page
   */
  protected InternalCalibrationData data;

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  public PlotPageData getData() {
    return data;
  }

  @Override
  protected void processDirtyData() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId,
        DataSet.STATUS_WAITING);
      Properties jobProperties = new Properties();
      jobProperties.setProperty(DataSetJob.ID_PARAM, String.valueOf(datasetId));
      JobManager.addJob(getDataSource(), getUser(),
        AutoQCJob.class.getCanonicalName(), jobProperties);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  @Override
  protected void initDataObject(DataSource dataSource) throws Exception {
    data = new InternalCalibrationData(dataSource, getCurrentInstrument(),
      dataset);
  }

  @Override
  public void reset() {

    if (null != data) {
      data.destroy();
      data = null;
    }

    super.reset();
  }

  /**
   * Accept the automatic QC flags for the selected values.
   */
  public void acceptAutoQC() {
    data.acceptAutoQC();
    dirty = true;
  }

  public void setCalibrationUse() {
    try {
      data.applyManualFlag();
      dirty = true;
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
  }

  public String getNeededFlagCounts() {
    return new Gson().toJson(data.getNeedsFlagCount());
  }

  /**
   * Dummy setter for the user comment list. Needed because the
   * generateUserCommentList remoteCommand insists on trying to set this even
   * though I've told it not to.
   *
   * @param userCommentList
   */
  public void setUserCommentList(String userCommentList) {
    // TODO Work out how to not need this.
    // NOOP
  }

  @Override
  public boolean allowMaps() {
    return false;
  }
}
