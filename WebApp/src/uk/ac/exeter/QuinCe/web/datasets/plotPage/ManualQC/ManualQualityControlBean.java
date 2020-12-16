package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.SQLException;
import java.util.Properties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.sql.DataSource;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageBean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;

@ManagedBean
@SessionScoped
public class ManualQualityControlBean extends PlotPageBean {

  /**
   * The data for the page
   */
  protected ManualQCData data;

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "user_qc";

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  public void initDataObject(DataSource dataSource) throws SQLException {
    data = new ManualQCData(dataSource, getCurrentInstrument(), dataset);
  }

  @Override
  public PlotPageData getData() {
    return data;
  }

  @Override
  protected void processDirtyData() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId,
        DataSet.STATUS_AUTO_QC);
      Properties jobProperties = new Properties();
      jobProperties.put(DataReductionJob.ID_PARAM, String.valueOf(datasetId));
      JobManager.addJob(getDataSource(), getUser(),
        AutoQCJob.class.getCanonicalName(), jobProperties);
    } catch (Exception e) {
      e.printStackTrace();
    }
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

  /**
   * Get the QC comments generated from the current selection.
   *
   * @return The QC comments
   */
  public String getUserCommentList() {
    return data.getUserCommentsList();
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

  /**
   * Get the worst QC flag from the current selection.
   *
   * @return The QC flag.
   */
  public int getWorstSelectedFlag() {
    return data.getWorstSelectedFlag().getFlagValue();
  }

  /**
   * Dummy setter for the worst selected flag. Needed because the
   * generateUserCommentList remoteCommand insists on trying to set this even
   * though I've told it not to.
   *
   * @param userCommentList
   */
  public void setWorstSelectedFlag(int worstSelectedFlag) {
    // TODO Work out how to not need this.
    // NOOP
  }

  /**
   * Generate the QC comments list and find the worst QC flag from the currently
   * selected values.
   */
  public void generateUserCommentsList() {
    data.generateUserCommentsList();
  }

  public int getUserFlag() {
    return data.getUserFlag();
  }

  public void setUserFlag(int userFlag) {
    data.setUserFlag(userFlag);
  }

  public String getUserComment() {
    return data.getUserComment();
  }

  public void setUserComment(String userComment) {
    data.setUserComment(userComment);
  }

  public void applyManualFlag() {
    data.applyManualFlag();
    dirty = true;
  }

  public String getNeededFlagCounts() {
    return new Gson().toJson(data.getNeedsFlagCounts());
  }
}
