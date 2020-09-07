package db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;

/**
 * Migration to add instrument_properties to the instruments table
 *
 * @author Steve Jones
 *
 */
public class V11__instrument_dataset_properties_1273 extends BaseJavaMigration {

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

    // Rename instrument_variables attributes to properties for consistency
    PreparedStatement instrVarPropsStmt = conn.prepareStatement(
      "ALTER TABLE instrument_variables CHANGE attributes properties MEDIUMTEXT");
    instrVarPropsStmt.execute();
    instrVarPropsStmt.close();

    // Redefine dataset properties
    PreparedStatement datasetPropsStmt = conn.prepareStatement(
      "ALTER TABLE dataset CHANGE properties properties MEDIUMTEXT");
    datasetPropsStmt.execute();
    datasetPropsStmt.close();

    // Populate the dataset properties
    buildDatasetProperties(conn);
  }

  private void buildDatasetProperties(Connection conn) throws SQLException {

    // This is not written efficiently, but it does very little so it doesn't
    // matter.

    Map<Long, Long> datasets = new HashMap<Long, Long>();

    PreparedStatement getDatasetsQuery = conn
      .prepareStatement("SELECT id, instrument_id FROM dataset");
    ResultSet datasetRecords = getDatasetsQuery.executeQuery();

    while (datasetRecords.next()) {
      datasets.put(datasetRecords.getLong(1), datasetRecords.getLong(2));
    }

    datasetRecords.close();
    getDatasetsQuery.close();

    for (Map.Entry<Long, Long> entry : datasets.entrySet()) {
      long datasetId = entry.getKey();
      long instrumentId = entry.getValue();

      Map<String, Properties> datasetProperties = new HashMap<String, Properties>();

      // Get the instrument properties
      PreparedStatement getInstrPropsQuery = conn
        .prepareStatement("SELECT properties FROM instrument WHERE id = ?");
      getInstrPropsQuery.setLong(1, instrumentId);

      ResultSet instrPropsRecord = getInstrPropsQuery.executeQuery();
      instrPropsRecord.next();
      datasetProperties.put(DataSet.INSTRUMENT_PROPERTIES_KEY,
        new Gson().fromJson(instrPropsRecord.getString(1), Properties.class));

      instrPropsRecord.close();
      getInstrPropsQuery.close();

      // Now get the variable properties for the variables
      // (Note that by the time we get here, instrument_variables.attributes has
      // been renamed to instrument_variables.properties)
      PreparedStatement varPropsStmt = conn.prepareStatement(
        "SELECT variable_id, properties FROM instrument_variables WHERE instrument_id = ?");
      varPropsStmt.setLong(1, instrumentId);

      Map<Long, Properties> varProps = new HashMap<Long, Properties>();
      ResultSet varPropsRecords = varPropsStmt.executeQuery();
      while (varPropsRecords.next()) {
        varProps.put(varPropsRecords.getLong(1),
          new Gson().fromJson(varPropsRecords.getString(2), Properties.class));
      }

      varPropsRecords.close();
      varPropsStmt.close();

      for (Map.Entry<Long, Properties> entry2 : varProps.entrySet()) {
        long variableId = entry2.getKey();
        Properties props = entry2.getValue();

        // Get the variable name
        PreparedStatement varNameStmt = conn
          .prepareStatement("SELECT name FROM variables WHERE id = ?");
        varNameStmt.setLong(1, variableId);
        ResultSet varRecord = varNameStmt.executeQuery();
        varRecord.next();
        String varName = varRecord.getString(1);

        varRecord.close();
        varNameStmt.close();

        datasetProperties.put(varName, props);
      }

      // Now write the dataset properties to the database
      PreparedStatement writePropsStmt = conn
        .prepareStatement("UPDATE dataset SET properties = ? WHERE id = ?");
      writePropsStmt.setString(1, new Gson().toJson(datasetProperties));
      writePropsStmt.setLong(2, datasetId);

      writePropsStmt.execute();
      writePropsStmt.close();
    }
  }
}
