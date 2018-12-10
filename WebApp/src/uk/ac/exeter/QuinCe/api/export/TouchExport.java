package uk.ac.exeter.QuinCe.api.export;

import javax.ws.rs.Path;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;

/**
 * API call to 'touch' a dataset that's
 * being exported to ensure that it doesn't time out
 * @author Steve Jones
 *
 */
@Path("/export/touchExport")
public class TouchExport extends SetExportStatus {

  @Override
  protected int getNewStatus() {
    return DataSet.STATUS_EXPORTING;
  }

}
