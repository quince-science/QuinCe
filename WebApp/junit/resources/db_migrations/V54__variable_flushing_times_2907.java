package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class V54__variable_flushing_times_2907 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Gson gson = new Gson();

    Connection conn = context.getConnection();

    // Add flushing_time to the run type table
    PreparedStatement addFlushingTimeStmt = conn.prepareStatement(
      "ALTER TABLE run_type ADD COLUMN flushing_time INT DEFAULT 0 AFTER alias_to");

    addFlushingTimeStmt.execute();
    addFlushingTimeStmt.close();

    // Get the instruments with run types with their properties
    Map<Long, String> instruments = new LinkedHashMap<Long, String>();

    PreparedStatement instrumentsQuery = conn
      .prepareStatement("select distinct i.id, i.properties from instrument i "
        + "inner join file_definition f on i.id = f.instrument_id "
        + "where f.id in (select distinct file_definition_id from run_type);");

    ResultSet instrumentsRecords = instrumentsQuery.executeQuery();
    while (instrumentsRecords.next()) {
      instruments.put(instrumentsRecords.getLong(1),
        instrumentsRecords.getString(2));
    }

    instrumentsRecords.close();
    instrumentsQuery.close();

    PreparedStatement setFlushingTimeStmt = conn
      .prepareStatement("UPDATE run_type SET flushing_time = ? "
        + "WHERE file_definition_id IN (SELECT id FROM file_definition WHERE instrument_id = ?)");

    PreparedStatement updatePropertiesStmt = conn.prepareStatement(
      "UPDATE instrument " + "SET properties = ? WHERE id = ?");

    for (Map.Entry<Long, String> instrument : instruments.entrySet()) {
      // Copy flushing time to the run_type records
      JsonObject properties = gson.fromJson(instrument.getValue(),
        JsonObject.class);
      int flushingTime = properties.get("preFlushingTime").getAsInt();
      setFlushingTimeStmt.setLong(1, flushingTime);
      setFlushingTimeStmt.setLong(2, instrument.getKey());
      setFlushingTimeStmt.execute();

      // Remove the flushing info from the instrument properties
      properties.remove("preFlushingTime");
      properties.remove("postFlushingTime");

      updatePropertiesStmt.setString(1, gson.toJson(properties));
      updatePropertiesStmt.setLong(2, instrument.getKey());

      updatePropertiesStmt.execute();
    }

    setFlushingTimeStmt.close();
    updatePropertiesStmt.close();
    conn.commit();
  }
}
