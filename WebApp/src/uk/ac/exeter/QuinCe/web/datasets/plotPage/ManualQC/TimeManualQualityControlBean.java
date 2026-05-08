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
import uk.ac.exeter.QuinCe.jobs.files.DataSetJob;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageBean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;

@ManagedBean
@SessionScoped
public class TimeManualQualityControlBean extends PlotPageBean {

  /**
   * The data for the page
   */
  protected ManualQCData data;

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "time_manual_qc";

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
        DataSet.STATUS_WAITING);
      Properties jobProperties = new Properties();
      jobProperties.put(DataSetJob.ID_PARAM, String.valueOf(datasetId));
      JobManager.addJob(getDataSource(), getUser(),
        AutoQCJob.class.getCanonicalName(), jobProperties);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
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

  public void applyManualFlag() {
    data.applyManualFlag();
    dirty = true;
  }

  public String getNeededFlagCounts() {
    return new Gson().toJson(data.getNeedsFlagCounts());
  }

  @Override
  public boolean dualYAxes() {
    return true;
  }

  @Override
  public boolean allowMaps() {
    return !dataset.fixedPosition();
  }
}
