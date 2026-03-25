package db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Files.TimeDataFile;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Migration for support for non-time-based data.
 * 
 * <p>
 * This assumes that the data integrity is sound, and doesn't check anything. An
 * error will be thrown if anything is amiss.
 * </p>
 */
public class V55__non_time_measurements_2931 extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection conn = context.getConnection();
    createCoordinates(conn);
    makeDatasetFiles(conn);
    migrateVariableCascades(conn);
  }

  private void createCoordinates(Connection conn) throws SQLException {

    // Create the coordinates table
    PreparedStatement createCoordsTableStatement = conn.prepareStatement(
      "CREATE TABLE coordinates (id BIGINT(20) NOT NULL AUTO_INCREMENT, "
        + "dataset_id INT(11) NOT NULL, date BIGINT(20) NULL, "
        + "depth MEDIUMINT NULL, station VARCHAR(10) NULL, "
        + "cast VARCHAR(10) NULL, bottle VARCHAR(10) NULL, "
        + "replicate VARCHAR(10) NULL, cycle VARCHAR(10) NULL, "
        + "PRIMARY KEY(id), INDEX coord_datasetid (dataset_id), "
        + "CONSTRAINT coord_dataset FOREIGN KEY (dataset_id) REFERENCES dataset(id))"
        + "ENGINE = InnoDB");

    createCoordsTableStatement.execute();
    createCoordsTableStatement.close();

    // Create the coordinates records by copying distinct dataset_id/date from
    // the sensor_values table
    PreparedStatement createCoordsStmt = conn.prepareStatement(
      "INSERT INTO coordinates (dataset_id, date) SELECT dataset_id, date FROM sensor_values GROUP BY dataset_id, date");
    createCoordsStmt.execute();
    conn.commit();
    createCoordsStmt.close();

    long maxCoordId;
    PreparedStatement maxCoordIdStmt = conn
      .prepareStatement("SELECT MAX(id) FROM coordinates");
    ResultSet maxCoordRecord = maxCoordIdStmt.executeQuery();
    maxCoordRecord.next();
    maxCoordId = maxCoordRecord.getLong(1);
    maxCoordRecord.close();
    maxCoordIdStmt.close();

    // Add coordinate field to sensor_values table with foreign key constraint
    PreparedStatement addSensorValuesCoordFieldStmt = conn.prepareStatement(
      "ALTER TABLE sensor_values ADD coordinate_id BIGINT(20) DEFAULT 0 NOT NULL AFTER id");
    addSensorValuesCoordFieldStmt.execute();
    addSensorValuesCoordFieldStmt.close();

    // Add coordinate field to measurements table with foreign key constraint
    PreparedStatement addMeasurementsCoordFieldStmt = conn.prepareStatement(
      "ALTER TABLE measurements ADD coordinate_id BIGINT(20) DEFAULT 0 NOT NULL AFTER id");
    addMeasurementsCoordFieldStmt.execute();
    addMeasurementsCoordFieldStmt.close();

    // Load all coordinate records and copy IDs back to sensor_values
    int batchSize = 20000;
    PreparedStatement getCoordsQuery = conn.prepareStatement(
      "SELECT id, dataset_id, date FROM coordinates WHERE id > ? ORDER BY id LIMIT "
        + batchSize);

    PreparedStatement writeSensorValuesCoordStmt = conn.prepareStatement(
      "UPDATE sensor_values SET coordinate_id = ? WHERE dataset_id = ? AND date = ?");

    PreparedStatement writeMeasurementsCoordStmt = conn.prepareStatement(
      "UPDATE measurements SET coordinate_id = ? WHERE dataset_id = ? AND date = ?");

    long lastCoordId = 0L;
    while (lastCoordId < maxCoordId) {
      getCoordsQuery.setLong(1, lastCoordId);

      // Copy coordinate IDs back to sensor values
      ResultSet coords = getCoordsQuery.executeQuery();
      while (coords.next()) {
        long coordId = coords.getLong(1);
        writeSensorValuesCoordStmt.setLong(1, coordId);
        writeSensorValuesCoordStmt.setLong(2, coords.getLong(2));
        writeSensorValuesCoordStmt.setLong(3, coords.getLong(3));
        writeSensorValuesCoordStmt.addBatch();

        writeMeasurementsCoordStmt.setLong(1, coordId);
        writeMeasurementsCoordStmt.setLong(2, coords.getLong(2));
        writeMeasurementsCoordStmt.setLong(3, coords.getLong(3));
        writeMeasurementsCoordStmt.addBatch();

        lastCoordId = coordId;
      }

      System.out.println(lastCoordId);
      writeSensorValuesCoordStmt.executeBatch();
      writeMeasurementsCoordStmt.executeBatch();
      conn.commit();
      coords.close();
    }

    getCoordsQuery.close();
    writeSensorValuesCoordStmt.close();
    writeMeasurementsCoordStmt.close();
  }

  private void makeDatasetFiles(Connection conn) throws SQLException {

    // Create the coordinates table
    PreparedStatement createDatasetFilesTableStatement = conn.prepareStatement(
      "CREATE TABLE dataset_files (dataset_id INT(11) NOT NULL, "
        + "datafile_id INT(11) NOT NULL, "
        + "INDEX datasetfiles_datasetid (dataset_id), "
        + "CONSTRAINT datasetfiles_datasetid FOREIGN KEY (dataset_id) REFERENCES dataset(id), "
        + "CONSTRAINT datasetfiles_datafileid FOREIGN KEY (datafile_id) REFERENCES data_file(id)) "
        + "ENGINE = InnoDB");

    createDatasetFilesTableStatement.execute();
    createDatasetFilesTableStatement.close();

    // Get all existing DataSets
    List<DatasetInfo> datasets = new ArrayList<DatasetInfo>();

    PreparedStatement getDatasetIDsQuery = conn.prepareStatement(
      "SELECT id, instrument_id, start, end FROM dataset ORDER BY instrument_id");

    ResultSet datasetRecords = getDatasetIDsQuery.executeQuery();
    while (datasetRecords.next()) {
      datasets.add(new DatasetInfo(datasetRecords));
    }
    datasetRecords.close();
    getDatasetIDsQuery.close();

    // Get used data files for each dataset and add them to the table
    PreparedStatement getFilesQuery = conn
      .prepareStatement("SELECT id, start_date, end_date, properties "
        + "FROM data_file WHERE file_definition_id IN "
        + "(SELECT id FROM file_definition WHERE instrument_id = ?) "
        + "ORDER BY start_date ASC");

    PreparedStatement addDatasetFileStmt = conn
      .prepareStatement("INSERT INTO dataset_files VALUES (?, ?)");

    long currentInstrument = -1L;
    List<FileInfo> currentDataFiles = null;

    for (DatasetInfo dataset : datasets) {
      // If we hit a new instrument, load its data file info
      if (dataset.instrumentId != currentInstrument) {
        currentInstrument = dataset.instrumentId;
        currentDataFiles = new ArrayList<FileInfo>();

        getFilesQuery.setLong(1, currentInstrument);
        ResultSet fileRecords = getFilesQuery.executeQuery();
        while (fileRecords.next()) {
          currentDataFiles.add(new FileInfo(fileRecords));
        }
      }

      for (FileInfo file : currentDataFiles) {
        if (file.coveredBy(dataset.start, dataset.end)) {
          addDatasetFileStmt.setLong(1, dataset.id);
          addDatasetFileStmt.setLong(2, file.id);
          addDatasetFileStmt.execute();
        }
      }

    }

    addDatasetFileStmt.close();
    getFilesQuery.close();
    conn.commit();
  }

  private void migrateVariableCascades(Connection conn) throws SQLException {

    // Create the new cascades field
    PreparedStatement createCascadesFieldStmt = conn.prepareStatement(
      "ALTER TABLE variable_sensors ADD COLUMN cascades MEDIUMTEXT AFTER attribute_value");
    createCascadesFieldStmt.execute();
    createCascadesFieldStmt.close();

    // Transfer the cascade data
    PreparedStatement addCascadeStmt = conn.prepareStatement(
      "UPDATE variable_sensors SET cascades = ? WHERE variable_id = ? AND sensor_type = ?");

    PreparedStatement getCascadesQuery = conn.prepareStatement(
      "SELECT variable_id, sensor_type, questionable_cascade, bad_cascade FROM variable_sensors");

    ResultSet cascadeRecords = getCascadesQuery.executeQuery();
    while (cascadeRecords.next()) {

      addCascadeStmt.setLong(1, cascadeRecords.getLong(1));
      addCascadeStmt.setLong(2, cascadeRecords.getLong(2));

      int questionableCascade = cascadeRecords.getInt(3);
      int badCascade = cascadeRecords.getInt(4);

      addCascadeStmt.setString(3, "{\"Time\":[[3," + questionableCascade
        + "]],[[4, " + badCascade + "]]}");

      addCascadeStmt.execute();
    }

    cascadeRecords.close();
    getCascadesQuery.close();
    addCascadeStmt.close();

    // Drop the old cascade columns
    PreparedStatement dropQuestionableCascadeStmt = conn.prepareStatement(
      "ALTER TABLE variable_sensors DROP COLUMN questionable_cascade");
    dropQuestionableCascadeStmt.execute();
    dropQuestionableCascadeStmt.close();

    PreparedStatement dropBadCascadeStmt = conn
      .prepareStatement("ALTER TABLE variable_sensors DROP COLUMN bad_cascade");
    dropBadCascadeStmt.execute();
    dropBadCascadeStmt.close();
  }

  class DatasetInfo {
    protected long id;
    protected long instrumentId;
    protected LocalDateTime start;
    protected LocalDateTime end;

    protected DatasetInfo(ResultSet record) throws SQLException {
      id = record.getLong(1);
      instrumentId = record.getLong(2);
      start = DateTimeUtils.longToDate(record.getLong(3));
      end = DateTimeUtils.longToDate(record.getLong(4));
    }
  }

  class FileInfo {
    protected long id;
    private LocalDateTime rawStart;
    private LocalDateTime rawEnd;
    private Properties properties;

    protected FileInfo(ResultSet record) throws SQLException {
      id = record.getLong(1);
      rawStart = DateTimeUtils.longToDate(record.getLong(2));
      rawEnd = DateTimeUtils.longToDate(record.getLong(3));
      properties = new Gson().fromJson(record.getString(4), Properties.class);
    }

    protected boolean coveredBy(LocalDateTime start, LocalDateTime end) {
      int offset = Integer
        .parseInt(properties.getProperty(TimeDataFile.TIME_OFFSET_PROP));

      return rawEnd.plusSeconds(offset).isAfter(start)
        && rawStart.plusSeconds(offset).isBefore(end);
    }
  }
}
