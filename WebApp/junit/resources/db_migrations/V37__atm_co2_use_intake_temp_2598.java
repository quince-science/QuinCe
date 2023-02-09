package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.jobs.files.LocateMeasurementsJob;

public class V37__atm_co2_use_intake_temp_2598 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Connection conn = context.getConnection();

    User user = UserDB.getUser(conn, "api_user@bcdc.no");

    // Switch Underway Atmospheric CO2 to use Intake Temperature
    // instead of Equilibrator Temperature
    PreparedStatement intakeTempStmt = conn.prepareStatement(
      "UPDATE variable_sensors SET sensor_type = 1 WHERE variable_id = 2 AND sensor_type = 3");

    intakeTempStmt.execute();
    intakeTempStmt.close();

    // Get all datasets affected by the change (datasets for instruments that
    // measure Underway Atmospheric CO2
    List<Long> datasetIds = new ArrayList<Long>();

    PreparedStatement getDatasetsStmt = conn
      .prepareStatement("SELECT id FROM dataset WHERE nrt = 0 AND status != "
        + DataSet.STATUS_ERROR + " AND instrument_id IN "
        + "(SELECT instrument_id FROM instrument_variables WHERE variable_id = "
        + "(SELECT id FROM variables WHERE name = 'Underway Atmospheric pCO₂'))");

    ResultSet datasetRecords = getDatasetsStmt.executeQuery();
    while (datasetRecords.next()) {
      datasetIds.add(datasetRecords.getLong(1));
    }

    PreparedStatement statusStmt = conn
      .prepareStatement("UPDATE dataset SET status = "
        + DataSet.STATUS_DATA_REDUCTION + " WHERE id = ?");

    PreparedStatement jobStmt = conn.prepareStatement(
      "INSERT INTO job (owner, class, properties) VALUES (?, '"
        + LocateMeasurementsJob.class.getCanonicalName() + "', ?)");

    for (long id : datasetIds) {
      statusStmt.setLong(1, id);
      statusStmt.addBatch();

      jobStmt.setLong(1, user.getDatabaseID());
      jobStmt.setString(2, "{\"id\":\"" + id + "\"}");
      jobStmt.addBatch();
    }

    statusStmt.executeBatch();
    jobStmt.executeBatch();

    jobStmt.close();
    statusStmt.close();
  }
}
