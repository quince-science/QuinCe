package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Replaces the {@code measurement_types} table with
 * {@code measurement_sensor_types}, which performs the same function but stores
 * more useful info in fewer records.
 * 
 * @author Steve Jones
 *
 */
public class V12__revised_measurement_values_1455 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    // Add measurement_values column to measurements
    Connection conn = context.getConnection();

    PreparedStatement addMeasValColStmt = conn
      .prepareStatement("ALTER TABLE measurements "
        + "ADD COLUMN measurement_values MEDIUMTEXT NULL");

    addMeasValColStmt.execute();
    addMeasValColStmt.close();

    // Create customisable columns for sensor types in different variables
    PreparedStatement addShortColumnStmt = conn
      .prepareStatement("ALTER TABLE variable_sensors "
        + "ADD COLUMN export_column_short VARCHAR(100) NULL AFTER bad_cascade");

    addShortColumnStmt.execute();
    addShortColumnStmt.close();

    PreparedStatement addLongColumnStmt = conn
      .prepareStatement("ALTER TABLE variable_sensors "
        + "ADD COLUMN export_column_long VARCHAR(100) NULL AFTER export_column_short");

    addLongColumnStmt.execute();
    addLongColumnStmt.close();

    PreparedStatement addCodeColumnStmt = conn
      .prepareStatement("ALTER TABLE variable_sensors "
        + "ADD COLUMN export_column_code VARCHAR(50) NULL AFTER export_column_long");

    addCodeColumnStmt.execute();
    addCodeColumnStmt.close();

    // Update column info
    PreparedStatement editSensorTypeXco2Code = conn.prepareStatement(
      "UPDATE sensor_types SET column_code = 'XCO2WBDY' WHERE id=9");

    editSensorTypeXco2Code.execute();
    editSensorTypeXco2Code.close();

    // Add new column info for variable_sensors
    PreparedStatement newColumnInfoStmt = conn.prepareStatement(
      "UPDATE variable_sensors " + "SET export_column_short = ?, "
        + "export_column_long = ?, " + "export_column_code = ?"
        + "WHERE variable_id = ? AND sensor_type = ?;");

    newColumnInfoStmt.setString(1, "xCO₂ in Water");
    newColumnInfoStmt.setString(2, "xCO₂ in Water - Calibrated");
    newColumnInfoStmt.setString(3, "XCO2DCEQ");
    newColumnInfoStmt.setLong(4, 1);
    newColumnInfoStmt.setLong(5, 9);

    newColumnInfoStmt.execute();

    newColumnInfoStmt.setString(1, "xCO₂ in Atmosphere");
    newColumnInfoStmt.setString(2, "xCO₂ in Atmosphere - Calibrated");
    newColumnInfoStmt.setString(3, "XCO2DCMA");
    newColumnInfoStmt.setLong(4, 2);
    newColumnInfoStmt.setLong(5, 9);

    newColumnInfoStmt.execute();

    newColumnInfoStmt.close();
  }
}
