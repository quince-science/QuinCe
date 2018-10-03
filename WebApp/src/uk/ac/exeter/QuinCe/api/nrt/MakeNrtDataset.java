package uk.ac.exeter.QuinCe.api.nrt;

import java.sql.Connection;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API Method to create NRT datasets
 * @author zuj007
 *
 */
@Path("/nrt/MakeNrtDataset")
public class MakeNrtDataset {

  /**
   * Main API method. Performs checks then tries to
   * create the NRT dataset.
   * @param instrument The instrument ID ({@code instrument} parameter)
   * @return The response
   */
  @POST
  public Response makeNrtDataset(@FormParam("instrument") long instrument) {

    Response response;

    Connection conn = null;
    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
      conn = dataSource.getConnection();

      if (!InstrumentDB.instrumentExists(conn, instrument)) {
        response = Response.status(Status.NOT_FOUND).build();
      } else {
        if (!InstrumentDB.isNrtInstrument(conn, instrument)) {
          response = Response.status(Status.FORBIDDEN).build();
        } else {
          if (createNrtDataset(conn, instrument)) {
            response = Response.status(Status.OK).build();
          } else {
            response = Response.status(Status.NO_CONTENT).build();
          }
        }
      }
    } catch (Exception e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).build();
      e.printStackTrace();
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return response;
  }

  /**
   * Attempt to create a NRT dataset for an instrument
   * @param conn A database connection
   * @param instrument The instrument ID
   * @return {@code true} if a new NRT dataset is created;
   *         {@code false} if no dataset is created.
   */
  private boolean createNrtDataset(Connection conn, long instrument) {
    return false;
  }
}
