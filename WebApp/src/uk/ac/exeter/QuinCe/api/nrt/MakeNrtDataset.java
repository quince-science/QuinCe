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

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
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
  private boolean createNrtDataset(Connection conn, Instrument instrument) throws Exception {

    Properties appConfig = ResourceManager.getInstance().getConfig();

    DataSet lastDataset = DataSetDB.getLastDataSet(conn, instrument.getDatabaseId());
    DataSet nrtDataset = DataSetDB.getNrtDataSet(conn, instrument.getDatabaseId());

    // If there's a NRT dataset, it should be the last one. If it isn't, delete it.
    if (null != lastDataset && null != nrtDataset && !nrtDataset.equals(lastDataset)) {
      DataSetDB.deleteNrtDataSet(conn, instrument.getDatabaseId());
      nrtDataset = null;
    }

    boolean createDataset = true;

    // If the last dataset is the NRT dataset, see if there's new
    // data after it. If there isn't, we don't need to do anything
    if (null != nrtDataset) {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrument.getDatabaseId(), nrtDataset.getEnd())) {
        createDataset = false;
      } else {
        DataSetDB.deleteNrtDataSet(conn, instrument.getDatabaseId());
        lastDataset = DataSetDB.getLastDataSet(conn, instrument.getDatabaseId());
      }
    } else if (null != lastDataset) {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrument.getDatabaseId(), lastDataset.getEnd())) {
        createDataset = false;
      }
    } else {
      if (!DataFileDB.completeFilesAfter(conn, appConfig, instrument.getDatabaseId(), null)) {
        createDataset = false;
      }
    }

    if (createDataset) {
      // Default to 1st Jan 1900. The real dataset date will be adjusted
      // when the records are extracted
      LocalDateTime nrtStartDate = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
      if (null != lastDataset) {
        nrtStartDate = lastDataset.getEnd().plusSeconds(1);
      }

      LocalDateTime endDate = DataFileDB.getLastFileDate(conn, instrument.getDatabaseId());
      String nrtDatasetName = buildNrtDatasetName(instrument);
      DataSet newDataset = new DataSet(instrument.getDatabaseId(), nrtDatasetName, nrtStartDate, endDate, true);
      DataSetDB.addDataSet(conn, newDataset);

      // TODO This is a copy of the code in DataSetsBean.addDataSet. Does it need collapsing?
      Map<String, String> params = new HashMap<String, String>();
      params.put(ExtractDataSetJob.ID_PARAM, String.valueOf(newDataset.getId()));

      JobManager.addJob(conn, UserDB.getUser(conn, instrument.getOwnerId()),
          ExtractDataSetJob.class.getCanonicalName(), params);
    }

    return createDataset;
  }

  private String buildNrtDatasetName(Instrument instrument) {
    StringBuilder result = new StringBuilder("NRT");
    result.append(instrument.getPlatformCode());
    result.append(System.currentTimeMillis());
    return result.toString();
  }

}
