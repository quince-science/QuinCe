package uk.ac.exeter.QuinCe.api.nrt;

import java.sql.Connection;
import java.time.LocalDateTime;
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
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.files.CreateNrtDataset;
import uk.ac.exeter.QuinCe.jobs.files.DataSetJob;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API Method to create an NRT {@link DataSet}.
 *
 * *
 * <table>
 * <caption>Possible response codes</caption>
 * <tr>
 * <th>Response Code</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>200</td>
 * <td>If the state of the {@link Instrument}'s {@link DataSet}s is such that an
 * NRT {@link DataSet} can be created. This does not necessarily mean that an
 * NRT {@link DataSet} will definitely be created: just that the
 * {@link Instrument}'s state does not prevent one from being created if
 * appropriate data exists.</td>
 * </tr>
 * <tr>
 * <td>204</td>
 * <td>The {@link Instrument} is not currently in a state where an NRT
 * {@link DataSet} can be created.</td>
 * </tr>
 * <tr>
 * <td>403</td>
 * <td>Authentication failed, or the requested {@link Instrument} is not
 * configured for NRT data.</td>
 * </tr>
 * <tr>
 * <td>404</td>
 * <td>The specified {@link Instrument} does not exist.</td>
 * </tr>
 * <tr>
 * <td>500</td>
 * <td>An error occurred while trying to create the NRT {@link DataSet}.</td>
 * </tr>
 * </table>
 */
@Path("/nrt/MakeNrtDataset")
public class MakeNrtDataset {

  /**
   * Main API method. Performs checks then tries to create the NRT dataset.
   *
   * @param instrumentId
   *          The instrument ID ({@code instrument} parameter)
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
        Instrument instrument = InstrumentDB.getInstrument(conn, instrumentId);

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
      ExceptionUtils.printStackTrace(e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return response;
  }

  /**
   * Attempt to create a NRT dataset for an instrument.
   *
   * @param conn
   *          A database connection.
   * @param instrument
   *          The instrument.
   * @return {@code true} if a new NRT dataset is created; {@code false} if no
   *         dataset is created.
   * @throws Exception
   *           Any errors are propagated upward.
   */
  public static boolean createNrtDataset(Connection conn, Instrument instrument)
    throws Exception {

    boolean createDataset = false;

    // Only try to create a new NRT dataset if either (a) there are no existing
    // NRT datasets or (b) there is an NRT dataset and its status is either
    // WAITING FOR EXPORT or EXPORT COMPLETE - any other time the NRT is being
    // processed, so leave it alone.
    DataSet existingDataset = DataSetDB.getNrtDataSet(conn, instrument.getId());

    // If there is no NRT dataset, create one
    if (null == existingDataset) {
      createDataset = true;
    } else {
      if (existingDataset.getStatus() == DataSet.STATUS_REPROCESS) {
        createDataset = true;
      } else if (existingDataset.getStatus() == DataSet.STATUS_READY_FOR_EXPORT
        || existingDataset.getStatus() == DataSet.STATUS_EXPORT_COMPLETE) {

        // If the NRT was created less than 24 hours ago, don't create a new one
        if (DateTimeUtils.secondsBetween(existingDataset.getCreatedDate(),
          LocalDateTime.now()) > 86400) {

          // See if any data files have been uploaded/updated since the NRT
          // dataset was created. If so, recreate it.
          LocalDateTime lastFileModification = DataFileDB
            .getLastFileModification(conn, instrument.getId());

          if (null != lastFileModification
            && lastFileModification.isAfter(existingDataset.getCreatedDate())) {

            createDataset = true;
          }
        }
      }
    }

    if (createDataset) {
      Properties jobProperties = new Properties();
      jobProperties.setProperty(DataSetJob.ID_PARAM,
        String.valueOf(instrument.getId()));

      JobManager.addJob(conn, instrument.getOwner(),
        CreateNrtDataset.class.getCanonicalName(), jobProperties);
    }

    return createDataset;
  }
}
