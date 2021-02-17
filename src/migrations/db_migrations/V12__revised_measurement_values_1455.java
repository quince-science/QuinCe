package db_migrations;

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

    // Set up things we need
    Connection conn = context.getConnection();

    PreparedStatement addMeasValColStmt = conn
      .prepareStatement("ALTER TABLE measurements "
        + "ADD COLUMN measurement_values MEDIUMTEXT NULL");

    addMeasValColStmt.execute();
    addMeasValColStmt.close();
  }
}
