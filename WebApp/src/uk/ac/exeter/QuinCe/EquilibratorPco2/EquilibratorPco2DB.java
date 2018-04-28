package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private static final String GET_CALCULATION_VALUES_STATEMENT = "SELECT "
      + "delta_temperature, true_moisture, ph2o, " // 3
      + "dried_co2, calibrated_co2, " // 5
      + "pco2_te_wet, pco2_sst, fco2, " // 9
      + "auto_flag, auto_message, user_flag, user_message " // 12
      + "FROM " + TABLE_NAME
      + " WHERE measurement_id = ?";


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

      stmt.setDouble(1, values.get("delta_temperature"));

      Double trueMoisture = values.get("true_moisture");
      if (null == trueMoisture) {
        stmt.setNull(2, Types.DOUBLE);
      } else {
        stmt.setDouble(2, trueMoisture);
      }

      stmt.setDouble(3, values.get("ph2o"));
      stmt.setDouble(4, values.get("dried_co2"));
      stmt.setDouble(5, values.get("calibrated_co2"));
      stmt.setDouble(6, values.get("pco2_te_wet"));
      stmt.setDouble(7, values.get("pco2_sst"));
      stmt.setDouble(8, values.get("fco2"));
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
  public Map<String, Double> getCalculationValues(Connection conn, CalculationRecord record) throws MissingParamException, DatabaseException, RecordNotFoundException, NoSuchColumnException, MessageException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(record, "record");

    PreparedStatement stmt = null;
    ResultSet dbRecord = null;
    Map<String, Double> values = new HashMap<String, Double>();

    try {
      stmt = conn.prepareStatement(GET_CALCULATION_VALUES_STATEMENT);
      stmt.setLong(1, record.getLineNumber());

      dbRecord = stmt.executeQuery();

      if (!dbRecord.next()) {
        throw new RecordNotFoundException("Calculation data record not found", TABLE_NAME, record.getLineNumber());
      } else {

        values.put("delta_temperature", dbRecord.getDouble(1));
        values.put("true_moisture", dbRecord.getDouble(2));
        values.put("ph2o", dbRecord.getDouble(3));
        values.put("dried_co2", dbRecord.getDouble(4));
        values.put("calibrated_co2", dbRecord.getDouble(5));
        values.put("pco2_te_wet", dbRecord.getDouble(6));
        values.put("pco2_sst", dbRecord.getDouble(7));
        values.put("fco2", dbRecord.getDouble(8));

        for (int i = 1; i < record.getData().size(); i++) {
          DataColumn column = record.getData().get(i);
          String fieldName = DatabaseUtils.getDatabaseFieldName(column.getName());
          Double value = values.get(fieldName);
          if (null != value) {
            column.setValue(String.valueOf(value));
          }
        }

        record.setAutoFlag(new Flag(dbRecord.getInt(9)));
        record.setMessages(RebuildCode.getMessagesFromRebuildCodes(dbRecord.getString(10)));
        record.setUserFlag(new Flag(dbRecord.getInt(11)));
        record.setUserMessage(dbRecord.getString(12));
      }
    } catch (SQLException|InvalidDataException|InvalidFlagException e) {
      throw new DatabaseException("Error retrieving calculations" , e);
    } finally {
      DatabaseUtils.closeResultSets(dbRecord);
      DatabaseUtils.closeStatements(stmt);
    }

    return values;
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
}
