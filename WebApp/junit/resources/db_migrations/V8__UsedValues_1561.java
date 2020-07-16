package resources.db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Migration to rearrange measurement recording in the database.
 *
 * <p>
 * The {@code measurement} table will now only contain one record per timestamp
 * in a given dataset. The {@code measurement_values} table will be restructured
 * to record interpolated values from sensors.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class V8__UsedValues_1561 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    Connection conn = context.getConnection();
    conn.setAutoCommit(true);

    System.out.println("Creating new measurement_values table...");
    createNewMeasurementValuesTable(conn);

    // Process measurements and update data_reduction records
    System.out.println("Processing measurements...");
    List<Long> measurementsToDelete = processMeasurements(conn);

    // Delete measurement_values table
    System.out.println("Deleting old measurement_values table...");
    try (PreparedStatement dropMeasValStmt = conn
      .prepareStatement("DROP TABLE measurement_values_old")) {

      dropMeasValStmt.execute();
    }

    System.out.println("Deleting unused measurement records...");
    int recordNumber = 0;
    for (Long deadMeasurement : measurementsToDelete) {
      try (PreparedStatement deleteMeasStmt = conn
        .prepareStatement("DELETE FROM measurements WHERE id = ?")) {

        deleteMeasStmt.setLong(1, deadMeasurement);
        deleteMeasStmt.execute();

      }

      recordNumber++;
      if (recordNumber % 1000 == 0) {
        System.out.println(
          "Deleted " + recordNumber + " of " + measurementsToDelete.size());
      }

    }

    System.out.println("Deleting unused columns...");
    dropMeasurementsColumn(conn, "variable_id");
    dropMeasurementsColumn(conn, "longitude");
    dropMeasurementsColumn(conn, "latitude");

    conn.setAutoCommit(false);
  }

  private void createNewMeasurementValuesTable(Connection conn)
    throws SQLException {

    // Rename the old table
    try (PreparedStatement renameStmt = conn.prepareStatement(
      "ALTER TABLE measurement_values RENAME TO measurement_values_old")) {
      renameStmt.execute();
    }

    // Create the new table
    String createSql = "CREATE TABLE measurement_values ("
      + "measurement_id BIGINT(20) NOT NULL,"
      + "file_column_id INT(11) NOT NULL," + "prior BIGINT(20) NOT NULL,"
      + "post BIGINT(20) NULL,"
      + "PRIMARY KEY (measurement_id, file_column_id),"
      + "CONSTRAINT MEAS_VAL_MEASUREMENT FOREIGN KEY (measurement_id)"
      + "  REFERENCES measurements (id) ON DELETE NO ACTION ON UPDATE NO ACTION, "
      + "CONSTRAINT MEAS_VAL_PRIOR_SENSORVAL FOREIGN KEY (prior) "
      + "REFERENCES sensor_values (id) ON DELETE NO ACTION ON UPDATE NO ACTION,"
      + "CONSTRAINT MEAS_VAL_POST_SENSORVAL FOREIGN KEY (post) "
      + "REFERENCES sensor_values (id) ON DELETE NO ACTION ON UPDATE NO ACTION)";

    try (PreparedStatement createStmt = conn.prepareStatement(createSql)) {
      createStmt.execute();
    }

  }

  private List<Long> processMeasurements(Connection conn)
    throws SQLException, InvalidFlagException, DatabaseException,
    MissingParamException, DataReductionException {

    // Get the record count
    int recordCount = 0;

    try (PreparedStatement countStmt = conn
      .prepareStatement("SELECT COUNT(*) FROM measurements")) {

      try (ResultSet countRecord = countStmt.executeQuery()) {
        countRecord.next();
        recordCount = countRecord.getInt(1);
      }
    }

    int measurementCount = 0;

    List<Long> measurementsToDelete = new ArrayList<Long>(measurementCount / 2);

    try (PreparedStatement measStmt = conn
      .prepareStatement("SELECT id, dataset_id, date, longitude, latitude "
        + "FROM measurements")) {

      try (ResultSet measurements = measStmt.executeQuery()) {

        long currentId = -1;
        long currentDataset = -1;
        long currentTime = -1;

        while (measurements.next()) {

          long recordId = measurements.getLong(1);
          long datasetId = measurements.getLong(2);
          long time = measurements.getLong(3);
          double longitude = measurements.getDouble(4);
          double latitude = measurements.getDouble(5);

          // We used to have multiple measurement records for each variable.
          // These are now reduced to one measurement record.
          boolean newMeasurement = !(datasetId == currentDataset
            && time == currentTime);

          if (newMeasurement) {
            currentId = recordId;
            currentDataset = datasetId;
            currentTime = time;
          }

          makeNewSensorValues(conn, recordId, currentId, datasetId, time,
            longitude, latitude);

          if (!newMeasurement) {
            updateDataReduction(conn, recordId, currentId);
            measurementsToDelete.add(recordId);
          }

          measurementCount++;
          if (measurementCount % 1000 == 0) {
            System.out
              .println("Processed " + measurementCount + " of " + recordCount);
          }
        }
      }
    }

    return measurementsToDelete;
  }

  private void makeNewSensorValues(Connection conn, long originalMeasurementId,
    long trueMeasurementId, long datasetId, long time, double longitude,
    double latitude) throws SQLException, InvalidFlagException {

    try (PreparedStatement migrateSensorValuesStmt = conn
      .prepareStatement("INSERT IGNORE INTO "
        + "measurement_values (measurement_id, file_column_id, prior) "
        + "SELECT ?, sv.file_column, mv.sensor_value_id "
        + "FROM measurement_values_old mv "
        + "INNER JOIN sensor_values sv ON mv.sensor_value_id = sv.id "
        + "INNER JOIN file_column fc ON sv.file_column = fc.id "
        + "WHERE mv.measurement_id = ?")) {

      migrateSensorValuesStmt.setLong(1, trueMeasurementId);
      migrateSensorValuesStmt.setLong(2, originalMeasurementId);
      migrateSensorValuesStmt.execute();

    }

    // Add the position values
    if (!newTableContains(conn, trueMeasurementId,
      FileDefinition.LONGITUDE_COLUMN_ID)) {

      long[] posValueIds = getPositionValueIds(conn, datasetId, time);
      addMeasurementValue(conn, trueMeasurementId,
        FileDefinition.LONGITUDE_COLUMN_ID, posValueIds[0]);
      addMeasurementValue(conn, trueMeasurementId,
        FileDefinition.LATITUDE_COLUMN_ID, posValueIds[1]);
    }
  }

  private boolean newTableContains(Connection conn, long measurementId,
    long fileColumnId) throws SQLException {
    boolean result;

    try (PreparedStatement containsQuery = conn.prepareStatement(
      "SELECT COUNT(*) FROM measurement_values WHERE measurement_id = ? "
        + "AND file_column_id = ?")) {

      containsQuery.setLong(1, measurementId);
      containsQuery.setLong(2, fileColumnId);

      try (ResultSet containsCount = containsQuery.executeQuery()) {
        containsCount.next();
        result = containsCount.getInt(1) > 0;
      }
    }

    return result;
  }

  private void addMeasurementValue(Connection conn, long measurementId,
    long fileColumnId, long sensorValueId) throws SQLException {

    try (PreparedStatement addSensorValueStmt = conn
      .prepareStatement("INSERT INTO measurement_values (measurement_id, "
        + "file_column_id, prior) VALUES (?, ?, ?)")) {

      addSensorValueStmt.setLong(1, measurementId);
      addSensorValueStmt.setLong(2, fileColumnId);
      addSensorValueStmt.setLong(3, sensorValueId);

      addSensorValueStmt.execute();
    }

  }

  private void updateDataReduction(Connection conn, long measurementId,
    long trueMeasurementId) throws SQLException {

    try (PreparedStatement newMeasIdStmt = conn.prepareStatement("UPDATE "
      + "data_reduction SET measurement_id = ? WHERE measurement_id = ?")) {

      newMeasIdStmt.setLong(1, trueMeasurementId);
      newMeasIdStmt.setLong(2, measurementId);

      newMeasIdStmt.execute();
    }
  }

  private void dropMeasurementsColumn(Connection conn, String columnName)
    throws SQLException {
    try (PreparedStatement delColStmt = conn
      .prepareStatement("ALTER TABLE measurements DROP COLUMN " + columnName)) {

      delColStmt.execute();
    }
  }

  @Override
  public boolean canExecuteInTransaction() {
    return false;
  }

  private long[] getPositionValueIds(Connection conn, long datasetId, long time)
    throws SQLException, InvalidFlagException {

    long[] result = null;

    // Transfer the longitude and latitude
    try (PreparedStatement posStmt = conn.prepareStatement(
      "SELECT id FROM sensor_values WHERE dataset_id = ? AND date = ? "
        + "AND file_column IN (-1000, -1001) ORDER BY file_column DESC")) {

      posStmt.setLong(1, datasetId);
      posStmt.setLong(2, time);

      try (ResultSet posRecord = posStmt.executeQuery()) {
        posRecord.next();
        long lon = posRecord.getLong(1);
        posRecord.next();
        long lat = posRecord.getLong(1);

        result = new long[] { lon, lat };
      }
    }

    return result;
  }
}
