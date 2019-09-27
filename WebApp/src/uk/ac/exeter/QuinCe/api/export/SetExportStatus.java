package uk.ac.exeter.QuinCe.api.export;

import java.sql.Connection;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * <p>
 * A generic call to set the export status on a dataset. This is an abstract
 * class: sub-classes must implement the {@link #getNewStatus} method to specify
 * what the dataset's status will be set to.
 * </p>
 *
 * <p>
 * <b>Parameters:</b>
 * <ul>
 * <li>{@code id}: The dataset's database ID</li>
 * </ul>
 * </p>
 *
 * <p>
 * If the dataset is not found in the database, the API call will return
 * {@link javax.ws.rs.core.Response.Status#NOT_FOUND}. These API calls are only
 * valid if the dataset is currently being exported (and thus its export status
 * can be changed). If the dataset is not currently being exported, the API call
 * will return {@link javax.ws.rs.core.Response.Status#FORBIDDEN}.
 * </p>
 *
 * @author Steve Jones
 *
 */
public abstract class SetExportStatus {

  /**
   * The main action method of the API call.
   *
   * @param id
   *          The ID of the dataset whose status will be set.
   * @return The HTTP response indicating whether or not the call succeeded.
   * @throws Exception
   *           If any errors occur while updating the dataset's status. Will
   *           result in a
   *           {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR}
   *           being returned to the client.
   */
  @POST
  public Response setExportStatus(@FormParam("id") long id) throws Exception {

    Status responseCode = Status.OK;
    Connection conn = null;

    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
      conn = dataSource.getConnection();
      DataSet dataset = DataSetDB.getDataSet(conn, id);
      if (dataset.getStatus() != DataSet.STATUS_EXPORTING) {
        responseCode = Status.FORBIDDEN;
      } else {
        DataSetDB.setDatasetStatus(conn, id, getNewStatus());
      }
    } catch (RecordNotFoundException e) {
      responseCode = Status.NOT_FOUND;
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return Response.status(responseCode).build();
  }

  /**
   * Specifies the new status for the dataset.
   *
   * @return The new dataset status.
   */
  protected abstract int getNewStatus();
}
