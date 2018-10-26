package uk.ac.exeter.QuinCe.api.export;

import java.sql.Connection;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Abstract API call to set a dataset's status
 * @author zuj007
 *
 */
public abstract class SetExportStatus {

  @POST
  public Response setExportStatus(@FormParam("id") long id) throws Exception {

    Status responseCode = Status.OK;

    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
      Connection conn = dataSource.getConnection();
      DataSet dataset = DataSetDB.getDataSet(conn, id);
      if (dataset.getStatus() != DataSet.STATUS_EXPORTING) {
        responseCode = Status.FORBIDDEN;
      } else {
        DataSetDB.setDatasetStatus(conn, id, getNewStatus());
      }
    } catch (RecordNotFoundException e) {
      responseCode = Status.NOT_FOUND;
    }

    return Response.status(responseCode).build();
  }

  protected abstract int getNewStatus();
}
