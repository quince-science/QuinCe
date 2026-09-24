package uk.ac.exeter.QuinCe.web.datasets.export;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.Coordinate;
import uk.ac.exeter.QuinCe.data.Export.ExportOption;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageColumnHeading;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ArgoManualQCData;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.ManualQCData;

public class ArgoExportData extends ExportData {

  private ArgoManualQCData localSourceData;

  public ArgoExportData(ManualQCData sourceData, ExportOption exportOption)
    throws Exception {
    super(sourceData, exportOption);
    this.localSourceData = (ArgoManualQCData) sourceData;
    localInit();
  }

  /**
   * Additional initialisation steps.
   */
  protected void localInit() {
    // Add the profile headers to the root column group
    List<PlotPageColumnHeading> rootColumnGroup = headingsWithProperties
      .get(ManualQCData.ROOT_FIELD_GROUP);

    rootColumnGroup.addAll(0, localSourceData.getProfileDataHeadings());
  }

  public List<Coordinate> getCoordinates() {
    return localSourceData.getCoordinates();
  }
}
