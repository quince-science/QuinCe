package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.util.Properties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
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

  /**
   * Indicates whether or not the selected calibrations should be used
   */
  private boolean useCalibrations = true;

  /**
   * The message attached to calibrations that should not be used
   */
  private String useCalibrationsMessage = null;

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
        DataSet.STATUS_SENSOR_QC);
      Properties jobProperties = new Properties();
      jobProperties.setProperty(DataReductionJob.ID_PARAM,
        String.valueOf(datasetId));
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
   * Get the flag indicating whether the selected calibrations are to be used
   *
   * @return The use calibrations flag
   */
  public boolean getUseCalibrations() {
    return useCalibrations;
  }

  /**
   * Set the flag indicating whether the selected calibrations are to be used
   *
   * @param useCalibrations
   *          The use calibrations flag
   */
  public void setUseCalibrations(boolean useCalibrations) {
    this.useCalibrations = useCalibrations;
  }

  /**
   * Get the message that will be attached to calibrations which aren't being
   * used
   *
   * @return The message for unused calibrations
   */
  public String getUseCalibrationsMessage() {
    return useCalibrationsMessage;
  }

  /**
   * Set the message that will be attached to calibrations which aren't being
   * used
   *
   * @param useCalibrationsMessage
   *          The message for unused calibrations
   */
  public void setUseCalibrationsMessage(String useCalibrationsMessage) {
    this.useCalibrationsMessage = useCalibrationsMessage;
  }

  /**
   * Set the usage status of the selected rows
   *
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws InvalidFlagException
   */
  public void setCalibrationUse()
    throws MissingParamException, DatabaseException, InvalidFlagException {

    Flag newFlag = Flag.GOOD;
    if (!useCalibrations) {
      newFlag = Flag.BAD;
    }

    data.applyFlag(newFlag, useCalibrationsMessage);
    dirty = true;
  }

  /**
   * Accept the automatic QC flags for the selected values.
   */
  public void acceptAutoQC() {
    data.acceptAutoQC();
    dirty = true;
  }

  public int getNeedsFlagCount() {
    return data.getNeedsFlagCount();
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
   * Get the QC comments generated from the current selection.
   *
   * @return The QC comments
   */
  public String getUserCommentList() {
    return data.getUserCommentsList();
  }

  /**
   * Generate the QC comments list and find the worst QC flag from the currently
   * selected values.
   */
  public void generateUserCommentsList() {
    data.generateUserCommentsList();
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
}
