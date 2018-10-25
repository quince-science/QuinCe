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
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@Path("/export/exportList")
public class ExportList {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public String getExportList() throws Exception {

    ResourceManager resourceManager = ResourceManager.getInstance();
    SensorsConfiguration sensorConfig = resourceManager.getSensorsConfiguration();
    RunTypeCategoryConfiguration runTypeConfig = resourceManager.getRunTypeCategoryConfiguration();

    Connection conn = resourceManager.getDBDataSource().getConnection();
    List<DataSet> datasets = DataSetDB.getExportableDatasets(conn);

    JSONArray json = new JSONArray();

    for (DataSet dataset : datasets) {
      JSONObject datasetJson = new JSONObject();

      datasetJson.put("id", dataset.getId());
      datasetJson.put("name", dataset.getName());

      Instrument instrument = InstrumentDB.getInstrument(
          conn, dataset.getInstrumentId(), sensorConfig, runTypeConfig);
      JSONObject instrumentJson = new JSONObject();
      instrumentJson.put("name", instrument.getName());
      instrumentJson.put("user", UserDB.getUser(conn, instrument.getOwnerId()).getFullName());
      datasetJson.put("instrument", instrumentJson);

      json.put(datasetJson);
    }

    return json.toString();
  }
}
