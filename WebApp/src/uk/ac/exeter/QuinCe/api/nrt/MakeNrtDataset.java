package uk.ac.exeter.QuinCe.api.nrt;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.ExtractDataSetJob;
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
   * @param instrumentId The instrument ID
   * @return {@code true} if a new NRT dataset is created;
   *         {@code false} if no dataset is created.
   * @throws Exception Any errors are propagated upward
   */
  private boolean createNrtDataset(Connection conn, long instrumentId) throws Exception {

    Properties appConfig = ResourceManager.getInstance().getConfig();

    DataSet lastDataset = DataSetDB.getLastDataSet(conn, instrumentId);
    DataSet nrtDataset = DataSetDB.getNrtDataSet(conn, instrumentId);

    // If there's a NRT dataset, it should be the last one. If it isn't, delete it.
    if (null != lastDataset && null != nrtDataset && !nrtDataset.equals(lastDataset)) {
      DataSetDB.deleteNrtDataSet(conn, instrumentId);
      nrtDataset = null;
    }

    boolean createDataset = true;

    // If the last dataset is the NRT dataset, see if there's new
    // data after it. If there isn't, we don't need to do anything
    if (null != nrtDataset) {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrumentId, nrtDataset.getEnd())) {
        createDataset = false;
      } else {
        DataSetDB.deleteNrtDataSet(conn, instrumentId);
        lastDataset = DataSetDB.getLastDataSet(conn, instrumentId);
      }
    } else if (null != lastDataset) {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrumentId, lastDataset.getEnd())) {
        createDataset = false;
      }
    } else {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrumentId, null)) {
        createDataset = false;
      }
    }

    if (createDataset) {
      LocalDateTime nrtStartDate = null;
      if (null != lastDataset) {
        nrtStartDate = lastDataset.getEnd().plusSeconds(1);
      }

      LocalDateTime endDate = DataFileDB.getLastFileDate(conn, instrumentId);
      DataSet newDataset = new DataSet(instrumentId, DataSet.NRT_DATASET_NAME, nrtStartDate, endDate, true);
      DataSetDB.addDataSet(conn, newDataset);

      // TODO This is a copy of the code in DataSetsBean.addDataSet. Does it need collapsing?
      Map<String, String> params = new HashMap<String, String>();
      params.put(ExtractDataSetJob.ID_PARAM, String.valueOf(newDataset.getId()));

      JobManager.addJob(conn, InstrumentDB.getInstrumentOwner(conn, instrumentId), ExtractDataSetJob.class.getCanonicalName(), params);
    }

    return createDataset;
  }
}
