package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPage2Bean;

@ManagedBean
@SessionScoped
public class ManualQC2Bean extends PlotPage2Bean {

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "user_qc2";

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  public void loadData() {
    try {

      data = new ManualQC2Data(getCurrentInstrument(), dataset,
        getDataSource());

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  protected void processDirtyData() {
    System.out.println("Dirty!");
  }
}
