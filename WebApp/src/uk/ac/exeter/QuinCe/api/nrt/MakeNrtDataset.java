package uk.ac.exeter.QuinCe.api.nrt;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.CreateNrtDataset;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API Method to create NRT datasets
 * @author Steve Jones
 *
 */
@Path("/nrt/MakeNrtDataset")
public class MakeNrtDataset {

  /**
   * Main API method. Performs checks then tries to
   * create the NRT dataset.
   * @param instrumentId The instrument ID ({@code instrument} parameter)
   * @return The response
   */
  @POST
  public Response makeNrtDataset(@FormParam("instrument") long instrumentId) {

    Response response;

    Connection conn = null;
    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
      conn = dataSource.getConnection();

      if (!InstrumentDB.instrumentExists(conn, instrumentId)) {
        response = Response.status(Status.NOT_FOUND).build();
      } else {
        ResourceManager resourceManager = ResourceManager.getInstance();

        Instrument instrument = InstrumentDB.getInstrument(conn, instrumentId,
            resourceManager.getSensorsConfiguration(), resourceManager.getRunTypeCategoryConfiguration());

        if (!instrument.getNrt()) {
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
   * @param instrumentId The instrument ID
   * @return {@code true} if a new NRT dataset is created;
   *         {@code false} if no dataset is created.
   * @throws Exception Any errors are propagated upward
   */
  public static boolean createNrtDataset(Connection conn, Instrument instrument) throws Exception {

    Properties appConfig = ResourceManager.getInstance().getConfig();

    DataSet lastDataset = DataSetDB.getLastDataSet(conn, instrument.getDatabaseId());

    boolean createDataset = true;

    if (null == lastDataset) {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrument.getDatabaseId(), null)) {
        createDataset = false;
      }
    } else {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrument.getDatabaseId(), lastDataset.getEnd())) {
        createDataset = false;
      }
    }

    if (createDataset) {
      Map<String, String> params = new HashMap<String, String>();
      params.put(ExtractDataSetJob.ID_PARAM, String.valueOf(instrument.getDatabaseId()));

      JobManager.addJob(conn, UserDB.getUser(conn, instrument.getOwnerId()),
          CreateNrtDataset.class.getCanonicalName(), params);
    }

    return createDataset;
  }
}
