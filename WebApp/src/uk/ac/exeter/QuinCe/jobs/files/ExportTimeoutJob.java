package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.annotation.WebListener;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.BackgroundTask;
import uk.ac.exeter.QuinCe.utils.BackgroundTaskException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Job to reset dataset export status if export is taking too long
 * (we assume that the exporter has died)
 * @author zuj007
 *
 */
@WebListener
public class ExportTimeoutJob extends BackgroundTask {

  @Override
  protected void doTask() throws BackgroundTaskException {

    Connection conn = null;
    LocalDateTime now = DateTimeUtils.longToDate(System.currentTimeMillis());

    try {
      conn = ResourceManager.getInstance().getDBDataSource().getConnection();
      List<DataSet> dataSets =
          DataSetDB.getDatasetsWithStatus(conn, DataSet.STATUS_EXPORTING);

      for (DataSet dataset : dataSets) {
        if (now.minusMinutes(30).isAfter(dataset.getStatusDate())) {
          DataSetDB.setDatasetStatus(conn,
              dataset.getId(), DataSet.STATUS_READY_FOR_EXPORT);
        }
      }
    } catch (Exception e) {
      throw new BackgroundTaskException(e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  @Override
  protected long getRunInterval() {
    return 30;
  }

}
