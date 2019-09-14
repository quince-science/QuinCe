package uk.ac.exeter.QuinCe.api.export;

import javax.ws.rs.Path;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;

/**
 * API call to report to QuinCe that an export has been completed.
 * 
 * @author Steve Jones
 *
 */
@Path("/export/completeExport")
public class CompleteExport extends SetExportStatus {

  @Override
  protected int getNewStatus() {
    return DataSet.STATUS_EXPORT_COMPLETE;
  }

}
