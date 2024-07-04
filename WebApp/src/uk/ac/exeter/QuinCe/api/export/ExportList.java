package uk.ac.exeter.QuinCe.api.export;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API call to get a list of all datasets ready for export. These are any
 * datasets whose status is set to {@link DataSet#STATUS_READY_FOR_EXPORT}.
 *
 * <p>
 * This call returns a JSON array of dataset IDs and names, along with
 * instrument details to aid display to users. The JSON is formatted as follows:
 * </p>
 *
 * <pre>
 * [
 *   {
 *     "id": 99
 *     "name": "BSBS20150807",
 *     "instrument": {
 *       "name":"BS",
 *       "user":"Steve Jones"
 *     }
 *   }
 * ]
 * </pre>
 */
@Path("/export/exportList")
public class ExportList {

  private static final long MIN_NRT_EXPORT_INTERVAL = 86400L;

  /**
   * The main processing method for the API call.
   *
   * @return The JSON output (see above).
   * @throws Exception
   *           If any errors occur while retrieving the dataset details. Results
   *           in a
   *           {@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR}
   *           being sent back to the client.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public String getExportList() throws Exception {

    String result = null;
    Connection conn = null;

    try {
      ResourceManager resourceManager = ResourceManager.getInstance();

      conn = resourceManager.getDBDataSource().getConnection();
      Collection<DataSet> datasets = DataSetDB
        .getDatasetsWithStatus(conn, DataSet.STATUS_READY_FOR_EXPORT).values();

      JsonArray json = new JsonArray();

      for (DataSet dataset : datasets) {
        Instrument instrument = InstrumentDB.getInstrument(conn,
          dataset.getInstrumentId());

        boolean canExport = true;

        if (dataset.isNrt()) {
          if (null != instrument.getLastNrtExport()) {
            long timeSinceLastNrtExport = DateTimeUtils.secondsBetween(
              instrument.getLastNrtExport(), LocalDateTime.now());
            if (timeSinceLastNrtExport < MIN_NRT_EXPORT_INTERVAL) {
              canExport = false;
            }
          }
        }

        if (canExport) {
          JsonObject datasetJson = new JsonObject();

          datasetJson.addProperty("id", dataset.getId());
          datasetJson.addProperty("name", dataset.getName());

          JsonObject instrumentJson = new JsonObject();
          instrumentJson.addProperty("name", instrument.getName());
          instrumentJson.addProperty("user",
            instrument.getOwner().getFullName());
          datasetJson.add("instrument", instrumentJson);

          json.add(datasetJson);
        }
      }
      result = json.toString();
    } catch (

    Exception e) {
      ExceptionUtils.printStackTrace(e);
      throw e;
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }
}
