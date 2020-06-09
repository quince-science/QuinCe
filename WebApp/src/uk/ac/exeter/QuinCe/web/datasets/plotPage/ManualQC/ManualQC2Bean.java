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
  }

  public int getNeededFlagCount() {
    return data.getNeedsFlagCount();
  }
}
