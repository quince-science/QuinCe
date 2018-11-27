package uk.ac.exeter.QuinCe.api.export;

import java.sql.Connection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetDB;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * API call to get a list of all datasets ready for export
 *
 * @author zuj007
 *
 */
@Path("/export/exportList")
public class ExportList {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public String getExportList() throws Exception {

    String result = null;
    Connection conn = null;

    try {
      ResourceManager resourceManager = ResourceManager.getInstance();
      SensorsConfiguration sensorConfig = resourceManager.getSensorsConfiguration();
      RunTypeCategoryConfiguration runTypeConfig = resourceManager.getRunTypeCategoryConfiguration();

      conn = resourceManager.getDBDataSource().getConnection();
      List<DataSet> datasets = DataSetDB.getDatasetsWithStatus(conn, DataSet.STATUS_READY_FOR_EXPORT);

      JSONArray json = new JSONArray();

      for (DataSet dataset : datasets) {
        JSONObject datasetJson = new JSONObject();

        datasetJson.put("id", dataset.getId());
        datasetJson.put("name", dataset.getName());

        Instrument instrument = InstrumentDB.getInstrument(conn, dataset.getInstrumentId(), sensorConfig,
            runTypeConfig);
        JSONObject instrumentJson = new JSONObject();
        instrumentJson.put("name", instrument.getName());
        instrumentJson.put("user", UserDB.getUser(conn, instrument.getOwnerId()).getFullName());
        datasetJson.put("instrument", instrumentJson);

        json.put(datasetJson);
      }
      result = json.toString();
    } catch (Exception e) {
      throw e;
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }
}
