package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Bean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Data;

@ManagedBean
@SessionScoped
public class ManualQC2Bean extends PlotPage2Bean {

  /**
   * The data for the page
   */
  protected ManualQC2Data data;

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "user_qc2";

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  public void initDataObject() throws SQLException {
    data = new ManualQC2Data(getDataSource(), getCurrentInstrument(), dataset);
  }

  @Override
  public PlotPage2Data getData() {
    return data;
  }

  @Override
  protected void processDirtyData() {
    System.out.println("Dirty!");
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
  }
}
