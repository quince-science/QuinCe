package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

/**
 * Add "item" key to MeasurementValues JSON.
 *
 * <p>
 * This is a naive method that assumes 1 sensor value ID = Measured, and >1 =
 * Interpolated. It will be wrong in some cases, but reprocessing the dataset
 * will fix it.
 * </p>
 *
 * @author stevej
 *
 */
public class V28__update_measurement_values_json_268 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Gson gson = new Gson();
    Connection conn = context.getConnection();

    List<Long> ids = new ArrayList<Long>();

    PreparedStatement idStmt = conn.prepareStatement(
      "SELECT id FROM measurements " + "WHERE measurement_values IS NOT NULL");

    ResultSet idRecords = idStmt.executeQuery();
    while (idRecords.next()) {
      ids.add(idRecords.getLong(1));
    }

    idRecords.close();
    idStmt.close();

    PreparedStatement getMVJson = conn.prepareStatement(
      "SELECT measurement_values FROM measurements WHERE id = ?");

    PreparedStatement setMVJson = conn.prepareStatement(
      "UPDATE measurements SET measurement_values = ? WHERE id = ?");

    for (Long id : ids) {

      getMVJson.setLong(1, id);
      ResultSet record = getMVJson.executeQuery();
      record.next();

      JsonObject jsonObj = JsonParser.parseString(record.getString(1))
        .getAsJsonObject();

      for (String key : jsonObj.keySet()) {
        JsonObject entry = jsonObj.get(key).getAsJsonObject();

        JsonArray sensorValueIds = entry.get("svids").getAsJsonArray();

        char type;

        switch (sensorValueIds.size()) {
        case 0: {
          type = PlotPageTableValue.NAN_TYPE;
          break;
        }
        case 1: {
          type = PlotPageTableValue.MEASURED_TYPE;
          break;
        }
        default: {
          type = PlotPageTableValue.INTERPOLATED_TYPE;
        }
        }

        entry.addProperty("type", type);

      }
      setMVJson.setString(1, gson.toJson(jsonObj));
      setMVJson.setLong(2, id);
      setMVJson.execute();

      record.close();
    }

    setMVJson.close();
    getMVJson.close();
  }
}
