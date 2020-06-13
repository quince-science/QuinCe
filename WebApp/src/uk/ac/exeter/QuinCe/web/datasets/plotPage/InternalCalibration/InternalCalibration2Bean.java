package uk.ac.exeter.QuinCe.web.datasets.plotPage.InternalCalibration;

import java.util.HashMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataReductionJob;
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
}
