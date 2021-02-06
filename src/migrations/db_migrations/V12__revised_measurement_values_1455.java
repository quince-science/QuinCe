package db_migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routines.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Replaces the {@code measurement_types} table with
 * {@code measurement_sensor_types}, which performs the same function but stores
 * more useful info in fewer records.
 * 
 * @author Steve Jones
 *
 */
public class V12__revised_measurement_values_1455 extends BaseJavaMigration {

  private PreparedStatement getSensorValueStmt = null;

  @Override
  public void migrate(Context context) throws Exception {

    // Set up things we need
    Connection conn = context.getConnection();

    PreparedStatement addMeasValColStmt = conn
      .prepareStatement("ALTER TABLE measurements "
        + "ADD COLUMN measurement_values MEDIUMTEXT NULL");

    addMeasValColStmt.execute();
    addMeasValColStmt.close();

    /*
     * 
     * // Lookup table of file column id -> sensor type id Map<Long, Long>
     * sensorTypes = buildSensorTypesLookup(conn);
     * 
     * 
     * 
     * // Set up SensorValue record statement String sensorValueSql = "SELECT "
     * + "id, file_column, date, value, auto_qc, " +
     * "user_qc_flag, user_qc_message FROM sensor_values WHERE id = ?";
     * getSensorValueStmt = conn.prepareStatement(sensorValueSql);
     * 
     * // Gson serializer Gson gson = new Gson();
     * 
     * // Create the new measurement_sensor_types table String
     * measurementSensorTypesTableSql = "CREATE TABLE " +
     * "measurement_sensor_types (measurement_id BIGINT(20) NOT NULL," +
     * "sensor_type_id INT(11) NOT NULL," +
     * "used_sensor_values MEDIUMTEXT NOT NULL," +
     * "calculated_value VARCHAR(100) NULL, qc_flag SMALLINT(2) NULL," +
     * "PRIMARY KEY (measurement_id, sensor_type_id)," +
     * "INDEX meassenstype_sensortype_idx (sensor_type_id ASC)," +
     * "CONSTRAINT meassenstype_measurement FOREIGN KEY (measurement_id) " +
     * "REFERENCES measurements (id) ON DELETE NO ACTION " +
     * "ON UPDATE NO ACTION)";
     * 
     * PreparedStatement newTableSqlStmt = conn
     * .prepareStatement(measurementSensorTypesTableSql);
     * newTableSqlStmt.execute(); newTableSqlStmt.close();
     * 
     * // We have to process the measurement values in batches to // avoid
     * running out of memory. The most convenient way is // to do it per dataset
     * List<Long> datasets = new ArrayList<Long>(); PreparedStatement
     * datasetsStmt = conn
     * .prepareStatement("SELECT DISTINCT dataset_id FROM measurements");
     * ResultSet datasetRecords = datasetsStmt.executeQuery(); while
     * (datasetRecords.next()) { datasets.add(datasetRecords.getLong(1)); }
     * 
     * datasetRecords.close(); datasetsStmt.close();
     * 
     * // Statement to get measurement values String measurementValuesSql =
     * "SELECT " +
     * "mv.measurement_id, mv.file_column_id, mv.prior, mv.post, m.date " +
     * "FROM measurement_values mv INNER JOIN measurements m ON " +
     * "mv.measurement_id = m.id WHERE m.dataset_id = ?"; PreparedStatement
     * measValuesStmt = conn .prepareStatement(measurementValuesSql);
     * 
     * // Statement to add new measurement_sensor_types record String
     * measurementSensorTypesRecordSql = "INSERT INTO measurement_sensor_types "
     * +
     * "(measurement_id, sensor_type_id, used_sensor_values, calculated_value, qc_flag) "
     * + "VALUES (?, ?, ?, ?, ?)"; PreparedStatement addRecordStmt = conn
     * .prepareStatement(measurementSensorTypesRecordSql);
     * 
     * // Now we process each dataset in turn int datasetCount = 0; for (long
     * datasetId : datasets) { datasetCount++; System.out.println(
     * "Processing dataset " + datasetCount + " of " + datasets.size());
     * 
     * // Now we get all the records from the measurement_values table // And
     * migrate them to the measurement_sensor_types table
     * 
     * measValuesStmt.setLong(1, datasetId); ResultSet measurementValues =
     * measValuesStmt.executeQuery();
     * 
     * while (measurementValues.next()) {
     * 
     * long measurementId = measurementValues.getLong(1); long fileColumnId =
     * measurementValues.getLong(2); SensorValue priorValue =
     * getSensorValue(measurementValues.getLong(3)); SensorValue postValue =
     * getSensorValue(measurementValues.getLong(4)); long measurementTime =
     * measurementValues.getLong(5);
     * 
     * // Build the data for the new measurement_sensor_types record List<Long>
     * sensorValueIds = new ArrayList<Long>(); Flag finalFlag = null; Double
     * finalValue = Double.NaN;
     * 
     * if (null == postValue) {
     * 
     * // Prior is the only value. Just copy across.
     * sensorValueIds.add(priorValue.getId()); finalFlag = getFlag(priorValue);
     * finalValue = StringUtils.doubleFromString(priorValue.getValue()); } else
     * {
     * 
     * // We have to do the interpolation
     * sensorValueIds.add(priorValue.getId());
     * sensorValueIds.add(postValue.getId());
     * 
     * Flag priorFlag = getFlag(priorValue); Flag postFlag = getFlag(postValue);
     * finalFlag = priorFlag.moreSignificantThan(postFlag) ? priorFlag :
     * postFlag;
     * 
     * long priorTime = DateTimeUtils.dateToLong(priorValue.getTime()); Double
     * priorNumber = StringUtils .doubleFromString(priorValue.getValue());
     * 
     * long postTime = DateTimeUtils.dateToLong(postValue.getTime()); Double
     * postNumber = StringUtils .doubleFromString(postValue.getValue());
     * 
     * finalValue = (priorNumber * (postTime - measurementTime) + postNumber *
     * (measurementTime - priorTime)) / (postTime - priorTime);
     * 
     * }
     * 
     * // Now store the new record addRecordStmt.setLong(1, measurementId);
     * addRecordStmt.setLong(2, sensorTypes.get(fileColumnId));
     * addRecordStmt.setString(3, gson.toJson(sensorValueIds));
     * addRecordStmt.setString(4, String.valueOf(finalValue));
     * addRecordStmt.setInt(5, finalFlag.getFlagValue());
     * addRecordStmt.execute(); }
     * 
     * measurementValues.close(); }
     * 
     * addRecordStmt.close(); measValuesStmt.close();
     * getSensorValueStmt.close();
     */
  }

  private Map<Long, Long> buildSensorTypesLookup(Connection conn)
    throws SQLException {

    Map<Long, Long> result = new HashMap<Long, Long>();

    PreparedStatement sensorTypeStmt = conn
      .prepareStatement("SELECT id, sensor_type FROM file_column");
    ResultSet records = sensorTypeStmt.executeQuery();

    while (records.next()) {
      result.put(records.getLong(1), records.getLong(2));
    }

    records.close();
    sensorTypeStmt.close();

    // Add the special types
    result.put(FileDefinition.LONGITUDE_COLUMN_ID, SensorType.LONGITUDE_ID);
    result.put(FileDefinition.LATITUDE_COLUMN_ID, SensorType.LATITUDE_ID);

    return result;
  }

  private SensorValue getSensorValue(long id) throws Exception {

    SensorValue result = null;

    if (id > 0) {
      getSensorValueStmt.setLong(1, id);
      ResultSet record = getSensorValueStmt.executeQuery();
      record.next();

      long valueId = record.getLong(1);
      long fileColumnId = record.getLong(2);
      LocalDateTime time = DateTimeUtils.longToDate(record.getLong(3));
      String value = record.getString(4);
      AutoQCResult autoQC = AutoQCResult.buildFromJson(record.getString(5));
      Flag userQCFlag = new Flag(record.getInt(6));
      String userQCMessage = record.getString(7);

      // We don't care about the dataset ID here
      result = new SensorValue(valueId, -1L, fileColumnId, time, value, autoQC,
        userQCFlag, userQCMessage);
      record.close();
    }

    return result;
  }

  private Flag getFlag(SensorValue sensorValue) {
    return sensorValue.getUserQCFlag().equals(Flag.NEEDED)
      ? sensorValue.getAutoQcFlag()
      : sensorValue.getUserQCFlag();
  }
}
