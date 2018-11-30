package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QCRoutines.data.DataColumn;
import uk.ac.exeter.QCRoutines.data.InvalidDataException;
import uk.ac.exeter.QCRoutines.data.NoSuchColumnException;
import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableList;

/**
 * Instance of {@link CalculationDB} for underway pCO2
 * @author Steve Jones
 *
 */
public class EquilibratorPco2DB extends CalculationDB {

  /**
   * The name of the database table
   */
  private static final String TABLE_NAME = "equilibrator_pco2";

  /**
   * The statement to store calculation values
   */
  private static final String STORE_CALCULATION_VALUES_STATEMENT = "UPDATE "
      + TABLE_NAME + " SET "
      + "delta_temperature = ?, true_moisture = ?, ph2o = ?, " // 3
      + "dried_co2 = ?, calibrated_co2 = ?, " // 5
      + "pco2_te_wet = ?, pco2_sst = ?, fco2 = ? " // 8
      + "WHERE measurement_id = ?";

  /**
   * The statement to clear calculation values
   */
  private static final String CLEAR_CALCULATION_VALUES_STATEMENT = "UPDATE "
      + TABLE_NAME + " SET "
      + "delta_temperature = NULL, true_moisture = NULL, ph2o = NULL, " // 3
      + "dried_co2 = NULL, calibrated_co2 = NULL, " // 5
      + "pco2_te_wet = NULL, pco2_sst = NULL, fco2 = NULL " // 8
      + "WHERE measurement_id = ?";

  /**
   * The query to retrieve calculation values
   */
  private static final String GET_CALCULATION_VALUES_QUERY = "SELECT "
      + "measurement_id, delta_temperature, true_moisture, ph2o, " // 4
      + "dried_co2, calibrated_co2, " // 6
      + "pco2_te_wet, pco2_sst, fco2, " // 10
      + "auto_flag, auto_message, user_flag, user_message " // 13
      + "FROM " + TABLE_NAME
      + " WHERE measurement_id = ?";

  /**
   * The query to retrieve calculation values
   */
  private static final String GET_DATASET_CALCULATION_VALUES_QUERY = "SELECT "
      + "measurement_id, delta_temperature, true_moisture, ph2o, " // 4
      + "dried_co2, calibrated_co2, " // 6
      + "pco2_te_wet, pco2_sst, fco2, " // 10
      + "auto_flag, auto_message, user_flag, user_message " // 13
      + "FROM " + TABLE_NAME
      + " WHERE measurement_id IN "
      + "(SELECT id FROM dataset_data WHERE dataset_id = ?)";

  @Override
  public String getCalculationTable() {
    return TABLE_NAME;
  }

  @Override
  public void storeCalculationValues(Connection conn, long measurementId, Map<String, Double> values) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");
    MissingParam.checkMissing(values, "values");

    PreparedStatement stmt = null;

    try {
      stmt = conn.prepareStatement(STORE_CALCULATION_VALUES_STATEMENT);

      DatabaseUtils.setNullableValue(stmt, 1, values.get("delta_temperature"));
      DatabaseUtils.setNullableValue(stmt, 2, values.get("true_moisture"));
      DatabaseUtils.setNullableValue(stmt, 3, values.get("ph2o"));
      DatabaseUtils.setNullableValue(stmt, 4, values.get("dried_co2"));
      DatabaseUtils.setNullableValue(stmt, 5, values.get("calibrated_co2"));
      DatabaseUtils.setNullableValue(stmt, 6, values.get("pco2_te_wet"));
      DatabaseUtils.setNullableValue(stmt, 7, values.get("pco2_sst"));
      DatabaseUtils.setNullableValue(stmt, 8, values.get("fco2"));

      stmt.setLong(9, measurementId);

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing calculations" , e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  // TODO In the long run a lot of this can be factored out. Or it may become obsolete with per-field QC flags.
  @Override
  public void loadCalculationValues(Connection conn, CalculationRecord record) throws MissingParamException, DatabaseException, RecordNotFoundException, NoSuchColumnException, MessageException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");

    PreparedStatement stmt = null;
    ResultSet dbRecord = null;

    try {
      stmt = conn.prepareStatement(GET_CALCULATION_VALUES_QUERY);
      stmt.setLong(1, record.getLineNumber());

      dbRecord = stmt.executeQuery();

      if (!dbRecord.next()) {
        throw new RecordNotFoundException("Calculation data record not found", TABLE_NAME, record.getLineNumber());
      } else {
        loadCalculationValuesFromResultSet(record, dbRecord);
      }
    } catch (SQLException|InvalidDataException|InvalidFlagException e) {
      throw new DatabaseException("Error retrieving calculations" , e);
    } finally {
      DatabaseUtils.closeResultSets(dbRecord);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  @Override
  public void clearCalculationValues(Connection conn, long measurementId) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");

    PreparedStatement stmt = null;

    try {
      stmt = conn.prepareStatement(CLEAR_CALCULATION_VALUES_STATEMENT);
      stmt.setLong(1, measurementId);
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing calculations" , e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  @Override
  public List<String> getCalculationColumnHeadings() {
    return EquilibratorPco2CalculationRecord.calculationColumns;
  }

  @Override
  public void populateVariableList(VariableList variables) throws MissingParamException {
    MissingParam.checkMissing(variables, "variables", true);

    variables.addVariable("Delta T", new Variable(Variable.TYPE_CALCULATION, "ΔT", "delta_temperature"));
    variables.addVariable("xH2O", new Variable(Variable.TYPE_CALCULATION, "True xH2O", "true_moisture"));
    variables.addVariable("pH2O", new Variable(Variable.TYPE_CALCULATION, "pH2O", "ph2o"));
    variables.addVariable("CO2", new Variable(Variable.TYPE_CALCULATION, "Dried CO2", "dried_co2"));
    variables.addVariable("CO2", new Variable(Variable.TYPE_CALCULATION, "Calibrated CO2", "calibrated_co2"));
    variables.addVariable("CO2", new Variable(Variable.TYPE_CALCULATION, "pCO2 TE Wet", "pco2_te_wet"));
    variables.addVariable("CO2", new Variable(Variable.TYPE_CALCULATION, "pCO2 SST", "pco2_sst"));
    variables.addVariable("CO2", new Variable(Variable.TYPE_CALCULATION, "Final fCO2", "fco2"));
  }

  @Override
  public void loadCalculationValues(Connection conn, long datasetId,
      TreeMap<Long, CalculationRecord> records) throws MissingParamException, DatabaseException,
      InvalidDataException, NoSuchColumnException, MessageException, InvalidFlagException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(datasetId, "datasetId");
    MissingParam.checkMissing(records, "records");

    PreparedStatement stmt = null;
    ResultSet dbRecords = null;

    try {
      stmt = conn.prepareStatement(GET_DATASET_CALCULATION_VALUES_QUERY);
      stmt.setLong(1, datasetId);

      dbRecords = stmt.executeQuery();
      while (dbRecords.next()) {
        long recordId = dbRecords.getLong(1);
        CalculationRecord record = records.get(recordId);
        if (null != record) {
          loadCalculationValuesFromResultSet(record, dbRecords);
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while loading calculation data", e);
    } finally {
      DatabaseUtils.closeResultSets(dbRecords);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Load calculation values from a ResultSet into a CalculationRecord
   * @param record The CalculationRecord
   * @param dbRecord The ResultSet
   * @return The loaded values
   * @throws SQLException
   * @throws InvalidDataException
   * @throws NoSuchColumnException
   * @throws MessageException
   * @throws InvalidFlagException
   */
  private void loadCalculationValuesFromResultSet(CalculationRecord record,
      ResultSet dbRecord) throws SQLException, InvalidDataException, NoSuchColumnException,
      MessageException, InvalidFlagException {

    Map<String, Double> values = new HashMap<String, Double>();

    values.put("delta_temperature", DatabaseUtils.getNullableDouble(dbRecord, 2));
    values.put("true_moisture", DatabaseUtils.getNullableDouble(dbRecord, 3));
    values.put("ph2o", DatabaseUtils.getNullableDouble(dbRecord, 4));
    values.put("dried_co2", DatabaseUtils.getNullableDouble(dbRecord, 5));
    values.put("calibrated_co2", DatabaseUtils.getNullableDouble(dbRecord, 6));
    values.put("pco2_te_wet", DatabaseUtils.getNullableDouble(dbRecord, 7));
    values.put("pco2_sst", DatabaseUtils.getNullableDouble(dbRecord, 8));
    values.put("fco2", DatabaseUtils.getNullableDouble(dbRecord, 9));

    for (int i = 1; i < record.getData().size(); i++) {
      DataColumn column = record.getData().get(i);
      String fieldName = DatabaseUtils.getDatabaseFieldName(column.getName());
      Double value = values.get(fieldName);
      if (null != value) {
        column.setValue(String.valueOf(value));
      }
    }

    record.setAutoFlag(new Flag(dbRecord.getInt(10)));
    record.setMessages(RebuildCode.getMessagesFromRebuildCodes(dbRecord.getString(11)));
    record.setUserFlag(new Flag(dbRecord.getInt(12)));
    record.setUserMessage(dbRecord.getString(13));
  }
}
