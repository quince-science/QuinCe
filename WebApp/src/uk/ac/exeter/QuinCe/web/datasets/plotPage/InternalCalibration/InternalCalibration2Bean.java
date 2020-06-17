package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.util.HashMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Bean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Data;

@ManagedBean
@SessionScoped
public class InternalCalibration2Bean extends PlotPage2Bean {

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "internal_calibration_plot2";

  /**
   * The data for the page
   */
  protected InternalCalibration2Data data;

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
  public PlotPage2Data getData() {
    return data;
  }

  @Override
  protected void processDirtyData() {
    try {
      DataSetDB.setDatasetStatus(getDataSource(), datasetId,
        DataSet.STATUS_AUTO_QC);
      Map<String, String> jobParams = new HashMap<String, String>();
      jobParams.put(DataReductionJob.ID_PARAM, String.valueOf(datasetId));
      JobManager.addJob(getDataSource(), getUser(),
        AutoQCJob.class.getCanonicalName(), jobParams);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void initDataObject() throws Exception {
    data = new InternalCalibration2Data(getDataSource(), getCurrentInstrument(),
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
   */
  public void setCalibrationUse()
    throws MissingParamException, DatabaseException {

    Flag newFlag = Flag.GOOD;
    if (!useCalibrations) {
      newFlag = Flag.BAD;
    }

    data.applyFlag(newFlag, useCalibrationsMessage);
    // dirty = true;
  }
}
