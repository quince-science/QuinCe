package uk.ac.exeter.QuinCe.web.datasets.plotPage.Position;

import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Instrument.MissingRunTypeException;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.TimeManualQualityControlBean;

@ManagedBean
@SessionScoped
public class PositionQualityControlBean extends TimeManualQualityControlBean {

  DatasetSensorValues positionData;

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "position_qc";

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  public void initDataObject(DataSource dataSource)
    throws SQLException, MissingRunTypeException {
    super.data = new PositionQCData(dataSource, getCurrentInstrument(),
      dataset);
  }

  @Override
  public boolean getCanFilter() {
    return false;
  }
}
