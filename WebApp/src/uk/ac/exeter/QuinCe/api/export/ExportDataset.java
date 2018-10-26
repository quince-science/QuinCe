package uk.ac.exeter.QuinCe.api.export;

import java.sql.Connection;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.datasets.ExportBean;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API call to export a dataset as a ZIP file.
 *
 * The ZIP will contain:
 * <ul>
 *   <li>The dataset in all available export formats</li>
 *   <li>The raw data files used to build the datases</li>
 *   <li>A manifest containing file lists and metadata</li>
 * </ul>
 *
 * @author zuj007
 *
 */
@Path("/export/exportDataset")
public class ExportDataset {

  @POST
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getDatasetZip(@FormParam("id") long id) throws Exception {

    Response response;
    Status responseCode = Status.OK;
    byte[] zip = null;

    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
      Connection conn = dataSource.getConnection();
      DataSet dataset = DataSetDB.getDataSet(conn, id);
      if (dataset.getStatus() != DataSet.STATUS_READY_FOR_EXPORT) {
        responseCode = Status.FORBIDDEN;
      } else {
        zip = ExportBean.buildExportZip(conn, dataset, null);
        DataSetDB.setDatasetStatus(conn, id, DataSet.STATUS_EXPORTING);
      }
    } catch (RecordNotFoundException e) {
      responseCode = Status.NOT_FOUND;
    }

    if (!responseCode.equals(Status.OK)) {
      response = Response.status(responseCode).build();
    } else {
      response = Response.ok(zip, MediaType.APPLICATION_OCTET_STREAM_TYPE).build();
    }

    return response;
  }
}
