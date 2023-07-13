package uk.ac.exeter.QuinCe.jobs.files;

import java.sql.Connection;
import java.util.Map;

import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.BackgroundTask;
import uk.ac.exeter.QuinCe.utils.BackgroundTaskException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@WebListener
public class DeleteDatasetsJob extends BackgroundTask {

  @Override
  protected void doTask() throws BackgroundTaskException {
    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

      try (Connection conn = dataSource.getConnection()) {
        Map<Long, DataSet> datasets = DataSetDB
          .getDatasetsWithStatus(dataSource, DataSet.STATUS_DELETE);

        for (DataSet dataset : datasets.values()) {
          DataSetDB.deleteDataSet(conn, dataset);
        }
      }
    } catch (Exception e) {
      throw new BackgroundTaskException(e);
    }
  }

  @Override
  protected long getRunInterval() {
    return 5;
  }

}
