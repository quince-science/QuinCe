package uk.ac.exeter.QuinCe.api.export;

import java.sql.Connection;

import javax.ws.rs.Path;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;

/**
 * API call to report to QuinCe that an export has been completed. This will
 * return the dataset's status to {@link DataSet#STATUS_EXPORT_COMPLETE}.
 */
@Path("/export/completeExport")
public class CompleteExport extends SetExportStatus {

  @Override
  protected int getNewStatus() {
    return DataSet.STATUS_EXPORT_COMPLETE;
  }

  @Override
  protected void additionalAction(Connection conn, long id) throws Exception {
    DataSetDB.setDatasetExported(conn, id);
  }
}
