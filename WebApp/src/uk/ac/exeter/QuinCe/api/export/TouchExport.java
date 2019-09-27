package uk.ac.exeter.QuinCe.api.export;

import javax.ws.rs.Path;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;

/**
 * API call to 'touch' a dataset that's being exported to ensure that it doesn't
 * time out.
 *
 * <p>
 * If a dataset is left in the status for too long, QuinCe will eventually
 * assume that the exporting process has failed, and set its status back to
 * {@link DataSet#STATUS_READY_FOR_EXPORT} so it will be reprocessed when the
 * exporter runs in future. This will make sure that datasets intended for
 * export are not missed if the export processor fails for any reason.
 * </p>
 *
 * <p>
 * If the export processor is taking a long time to do its job, it can make this
 * API call to 'touch' the dataset, signalling to QuinCe that the export is
 * still in progress and the exporter is still active.
 * </p>
 *
 * <p>
 * In reality, the call simply resets the dataset's status to
 * {@link DataSet#STATUS_EXPORTING}, which in turn updates the time at which the
 * dataset was last altered (it is this time that QuinCe watches to determine
 * whether or not the export has timed out).
 * </p>
 *
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
