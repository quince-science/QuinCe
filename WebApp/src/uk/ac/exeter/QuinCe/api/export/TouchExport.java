package uk.ac.exeter.QuinCe.api.export;

import javax.ws.rs.Path;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;

@Path("/export/touchExport")
public class TouchExport extends SetExportStatus {

  @Override
  protected int getNewStatus() {
    return DataSet.STATUS_EXPORTING;
  }

}
