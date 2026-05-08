package uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.sql.DataSource;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.jobs.files.DataSetJob;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageBean;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageData;

@ManagedBean
@SessionScoped
public class ArgoManualQualityControlBean extends PlotPageBean {

  /**
   * The data for the page
   */
  protected ArgoManualQCData data;

  /**
   * The currently select profile
   */
  protected int selectedProfile = 0;

  private List<SelectItem> variableMenuItems = null;

  /**
   * Navigation to the calibration data plot page
   */
  private static final String NAV_PLOT = "argo_manual_qc";

  @Override
  protected String getScreenNavigation() {
    return NAV_PLOT;
  }

  @Override
  public void initDataObject(DataSource dataSource) throws Exception {
    data = new ArgoManualQCData(dataSource, getCurrentInstrument(), dataset);
  }

  @Override
  public PlotPageData getData() {
    return data;
  }

  public ArgoManualQCData getProfileData() {
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
    return false;
  }

  @Override
  public boolean allowMaps() {
    return false;
  }

  @Override
  public void loadData() {
    super.loadData();
    selectedProfile = 0;

    try {
      buildVariableMenuItems();
    } catch (Exception e) {
      internalError(e);
    }
  }

  public int getSelectedProfile() {
    return selectedProfile;
  }

  public void setSelectedProfile(int index) {
    selectedProfile = index;
  }

  @Override
  public void generateTableData() {
    tableJsonData = data.generateTableData();
  }

  public void selectProfile() {
    data.setSelectedProfile(selectedProfile);
    generateTableData();
  }

  public List<SelectItem> getPlotMenuEntries() {

    if (null == variableMenuItems) {
      try {
        buildVariableMenuItems();
      } catch (Exception e) {
        internalError(e);
      }
    }

    return variableMenuItems;
  }

  private void buildVariableMenuItems() throws Exception {
    variableMenuItems = new ArrayList<SelectItem>();

    for (Map.Entry<String, List<PlotPageColumnHeading>> entry : data
      .getExtendedColumnHeadings().entrySet()) {

      if (!entry.getKey().equals(PlotPageData.ROOT_FIELD_GROUP)) {
        SelectItemGroup group = new SelectItemGroup(entry.getKey());
        SelectItem[] groupItems = new SelectItem[entry.getValue().size()];

        int i = 0;
        for (PlotPageColumnHeading heading : entry.getValue()) {
          groupItems[i] = new SelectItem(String.valueOf(heading.getId()),
            heading.getShortName(true));
          i++;
        }

        group.setSelectItems(groupItems);
        variableMenuItems.add(group);
      }
    }
  }
}
