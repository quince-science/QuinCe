package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * Migration to add instrument_properties to the instruments table
 *
 * @author Steve Jones
 *
 */
public class V11__instrument_properties_1273 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Connection conn = context.getConnection();

    // Create the new properties field
    PreparedStatement newFieldStmt = conn.prepareStatement(
      "ALTER TABLE instrument ADD COLUMN properties MEDIUMTEXT NULL AFTER nrt");

    newFieldStmt.execute();
    newFieldStmt.close();

    // Transfer the existing properties
    PreparedStatement getPropertiesQuery = conn.prepareStatement(
      "SELECT id, pre_flushing_time, post_flushing_time, depth FROM instrument");

    PreparedStatement storeNewPropertiesStmt = conn
      .prepareStatement("UPDATE instrument SET properties = ? WHERE id = ?");

    Gson gson = new Gson();

    ResultSet propsRecords = getPropertiesQuery.executeQuery();
    while (propsRecords.next()) {
      long id = propsRecords.getLong(1);
      int preFlushingTime = propsRecords.getInt(2);
      int postFlushingTime = propsRecords.getInt(3);
      int depth = propsRecords.getInt(4);

      Properties properties = new Properties();
      properties.put(Instrument.PROP_PRE_FLUSHING_TIME,
        String.valueOf(preFlushingTime));
      properties.put(Instrument.PROP_POST_FLUSHING_TIME,
        String.valueOf(postFlushingTime));
      properties.put(Instrument.PROP_DEPTH, String.valueOf(depth));

      storeNewPropertiesStmt.setString(1, gson.toJson(properties));
      storeNewPropertiesStmt.setLong(2, id);
      storeNewPropertiesStmt.execute();
    }

    storeNewPropertiesStmt.close();
    getPropertiesQuery.close();

    // Remove the old fields
    PreparedStatement dropPreFlushingTimeStmt = conn
      .prepareStatement("ALTER TABLE instrument DROP COLUMN pre_flushing_time");
    dropPreFlushingTimeStmt.execute();
    dropPreFlushingTimeStmt.close();

    PreparedStatement dropPostFlushingTimeStmt = conn.prepareStatement(
      "ALTER TABLE instrument DROP COLUMN post_flushing_time");
    dropPostFlushingTimeStmt.execute();
    dropPostFlushingTimeStmt.close();

    PreparedStatement dropDepthStmt = conn
      .prepareStatement("ALTER TABLE instrument DROP COLUMN depth");
    dropDepthStmt.execute();
    dropDepthStmt.close();
  }
}
