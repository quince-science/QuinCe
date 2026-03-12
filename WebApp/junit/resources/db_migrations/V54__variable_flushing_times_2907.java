package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
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

    /*
     * For instruments with custom run types, move the flushing time to the run
     * types table.
     */
    PreparedStatement addFlushingTimeStmt = conn.prepareStatement(
      "ALTER TABLE run_type ADD COLUMN flushing_time INT DEFAULT 0 AFTER alias_to");

    addFlushingTimeStmt.execute();
    addFlushingTimeStmt.close();

    // Get the instruments which have run types with their properties
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

    for (Map.Entry<Long, String> instrument : instruments.entrySet()) {
      JsonObject properties = gson.fromJson(instrument.getValue(),
        JsonObject.class);
      int flushingTime = properties.get("preFlushingTime").getAsInt();
      setFlushingTimeStmt.setLong(1, flushingTime);
      setFlushingTimeStmt.setLong(2, instrument.getKey());
      setFlushingTimeStmt.execute();
    }

    setFlushingTimeStmt.close();

    /*
     * For Pro Oceanus, we move the flushing time to a Variable Attribute
     */

    // Create the attribute
    PreparedStatement proOceanusVariablePropStmt = conn
      .prepareStatement("UPDATE variables SET attributes = "
        + "'{\"flushing_time\": {\"name\": \"Flushing Time\", \"type\": \"NUMBER\"}}' WHERE id = 8");
    proOceanusVariablePropStmt.execute();
    proOceanusVariablePropStmt.close();

    // Find the relevant instruments
    Map<Long, JsonObject> proOceanusInstruments = new HashMap<Long, JsonObject>();

    PreparedStatement proOceanusInstrumentsQuery = conn
      .prepareStatement("SELECT i.id, i.properties "
        + "FROM instrument i INNER JOIN instrument_variables iv ON i.id = iv.instrument_id "
        + "WHERE iv.variable_id = 8");

    ResultSet proOceanusRecords = proOceanusInstrumentsQuery.executeQuery();
    while (proOceanusRecords.next()) {
      proOceanusInstruments.put(proOceanusRecords.getLong(1),
        gson.fromJson(proOceanusRecords.getString(2), JsonObject.class));
    }

    proOceanusRecords.close();
    proOceanusInstrumentsQuery.close();

    // Create the instruments' variable attribute
    PreparedStatement proOceanusAttributeStmt = conn
      .prepareStatement("UPDATE instrument_variables "
        + "SET properties = ? WHERE instrument_id = ? AND variable_id = 8");

    for (Map.Entry<Long, JsonObject> instrument : proOceanusInstruments
      .entrySet()) {

      int flushingTime = instrument.getValue().get("preFlushingTime")
        .getAsInt();

      String attrString = "{\"flushing_time\":\"" + flushingTime + "\"}";

      proOceanusAttributeStmt.setString(1, attrString);
      proOceanusAttributeStmt.setLong(2, instrument.getKey());

      proOceanusAttributeStmt.execute();
    }

    proOceanusAttributeStmt.close();

    /*
     * Remove the flushing time property from all instruments.
     *
     * Anything we haven't explicitly moved will be lost, but that's fine
     * because they aren't used.
     */
    Map<Long, JsonObject> allInstrumentProperties = new HashMap<Long, JsonObject>();

    PreparedStatement allInstrumentsQuery = conn
      .prepareStatement("select id, properties from instrument");

    ResultSet allInstrumentsRecords = allInstrumentsQuery.executeQuery();

    while (allInstrumentsRecords.next()) {
      allInstrumentProperties.put(allInstrumentsRecords.getLong(1),
        gson.fromJson(allInstrumentsRecords.getString(2), JsonObject.class));
    }

    instrumentsRecords.close();
    instrumentsQuery.close();

    PreparedStatement updatePropertiesStmt = conn.prepareStatement(
      "UPDATE instrument " + "SET properties = ? WHERE id = ?");

    for (Map.Entry<Long, JsonObject> instrument : allInstrumentProperties
      .entrySet()) {

      instrument.getValue().remove("preFlushingTime");
      instrument.getValue().remove("postFlushingTime");

      updatePropertiesStmt.setString(1, gson.toJson(instrument.getValue()));
      updatePropertiesStmt.setLong(2, instrument.getKey());

      updatePropertiesStmt.execute();
    }

    updatePropertiesStmt.close();

    conn.commit();
  }
}
