package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V13__upgrade_autoqc_and_status_1160 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Connection conn = context.getConnection();

    // All current Auto QC results are from SensorValue QC routines,
    // so we prefix all then names, eg GradientTest becomes
    // SensorValue.GradientTest

    PreparedStatement getAutoQC = conn.prepareStatement(
      "SELECT id, auto_qc FROM sensor_values WHERE auto_qc IS NOT NULL");

    PreparedStatement setAutoQC = conn
      .prepareStatement("UPDATE sensor_values SET auto_qc = ? WHERE id = ?");

    ResultSet records = getAutoQC.executeQuery();
    while (records.next()) {
      long id = records.getLong(1);
      String oldAutoQC = records.getString(2);

      String newAutoQC = oldAutoQC.replaceAll("\"routineName\":\"([^\"]*)\"",
        "\"routineName\":\"SensorValue.$1\"");

      setAutoQC.setString(1, newAutoQC);
      setAutoQC.setLong(2, id);
      setAutoQC.execute();
    }

    records.close();
    setAutoQC.close();
    getAutoQC.close();

    // Now we update the dataset status numbers to give more flexibility
    PreparedStatement fieldChange = conn.prepareStatement("ALTER TABLE dataset "
      + "CHANGE COLUMN status status SMALLINT(4) NOT NULL");
    fieldChange.execute();

    // Export Complete
    updateStatusNumber(conn, 9, 140);

    // Automatic Export In Progress
    updateStatusNumber(conn, 8, 130);

    // Waiting for Automatic Export
    updateStatusNumber(conn, 7, 120);

    // Waiting for Approval
    updateStatusNumber(conn, 6, 110);

    // Ready for Submission
    updateStatusNumber(conn, 5, 100);

    // Ready for Manual QC
    updateStatusNumber(conn, 4, 50);

    // Data Reduction QC
    updateStatusNumber(conn, 10, 40);

    // Data Reduction
    updateStatusNumber(conn, 2, 30);

    // Automatic QC (renamed to Sensor QC)
    updateStatusNumber(conn, 3, 20);

    // Data Extraction
    updateStatusNumber(conn, 1, 10);
  }

  private void updateStatusNumber(Connection conn, int from, int to)
    throws SQLException {

    PreparedStatement statusNumberStmt = conn
      .prepareStatement("UPDATE dataset SET status = ? WHERE status = ?");
    statusNumberStmt.setInt(1, to);
    statusNumberStmt.setInt(2, from);
    statusNumberStmt.execute();
    statusNumberStmt.close();
  }
}
