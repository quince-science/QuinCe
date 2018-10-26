package uk.ac.exeter.QuinCe.api.export;

import javax.ws.rs.Path;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;

/**
 * API call to report to QuinCe that an
 * export has been abandoned.
 * @author zuj007
 *
 */
@Path("/export/abandonExport")
public class AbandonExport extends SetExportStatus {

  @Override
  protected int getNewStatus() {
    return DataSet.STATUS_READY_FOR_EXPORT;
  }

}
