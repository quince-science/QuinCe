package uk.ac.exeter.QuinCe.data.Dataset;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.Variable;
import uk.ac.exeter.QuinCe.web.VariableGroup;
import uk.ac.exeter.QuinCe.web.VariableList;

/**
 * Methods for storing and retrieving diagnostic data
 * @author Steve Jones
 *
 */
public class DiagnosticDataDB {

  /**
   * Statement to store a diagnostic value
   */
  private static final String STORE_DIAGNOSTIC_VALUE_STATEMENT = "INSERT INTO diagnostic_data"
      + " (measurement_id, sensor_name, value) VALUES"
      + " (?, ?, ?)";

  /**
   * Statement to get diagnostic sensor names for a data set
   */
  private static final String GET_DIAGNOSTIC_SENSORS_QUERY = "SELECT "
      + "sensor_type, sensor_name FROM file_column "
      + "WHERE sensor_type LIKE 'Diagnostic: %' AND "
      + "file_definition_id IN "
      + "(SELECT file_definition_id FROM file_definition WHERE instrument_id = ?) "
      + "ORDER BY sensor_name";

  /**
   * Store a set of diagnostic values
   * @param conn A database connection
   * @param measurementId The database ID of the measurement to which the values belong
   * @param diagnosticValues The diagnostic values
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  protected static void storeDiagnosticValues(Connection conn, long measurementId, Map<String, Double> diagnosticValues) throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");
    MissingParam.checkMissing(diagnosticValues, "diagnosticValues", true);

    PreparedStatement diagnosticStatement = null;

    try {
      diagnosticStatement = conn.prepareStatement(STORE_DIAGNOSTIC_VALUE_STATEMENT);

      for (Map.Entry<String, Double> entry : diagnosticValues.entrySet()) {
        diagnosticStatement.setLong(1, measurementId);
        diagnosticStatement.setString(2, entry.getKey());

        if (null == entry.getValue()) {
          diagnosticStatement.setNull(3, Types.DOUBLE);
        } else {
          diagnosticStatement.setDouble(3, entry.getValue());
        }

        diagnosticStatement.execute();
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while storing diagnostic values", e);
    } finally {
      DatabaseUtils.closeStatements(diagnosticStatement);
    }
  }

  /**
   * Add the diagnostic variables to the list of variables
   * for a data set
   * @param dataSource A data source
   * @param dataSet The data set for which the variables should be retrieved
   * @param variables The set of variables to be populated
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static void populateVariableList(DataSource dataSource, DataSet dataSet, VariableList variables) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(dataSet, "dataSet");
    MissingParam.checkMissing(variables, "variables", true);

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();

      stmt = conn.prepareStatement(GET_DIAGNOSTIC_SENSORS_QUERY);
      stmt.setLong(1, dataSet.getInstrumentId());

      records = stmt.executeQuery();

      while (records.next()) {
        String group = records.getString(1);
        if (!variables.containsGroup(group)) {
          variables.add(new VariableGroup(group, true));
        }

        String name = records.getString(2);
        variables.addVariable(group, new Variable(Variable.TYPE_DIAGNOSTIC, name, name, false, true, true));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving diagnostic sensor names", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Retrieve a set of diagnostic data values for a set of measurements.
   * The values are returned in a nested HashMap. The outer level
   * is identified by the measurement id. Within each ID is a
   * map of sensor name -> sensor value.
   *
   * @param conn A database connection
   * @param measurementIds The IDs of the measurements whose diagnostic data is to be retrieved
   * @param sensors The sensors whose data is to be retrieved
   * @return The diagnostic data values
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static Map<Long, Map<String, Double>> getDiagnosticValues(Connection conn, List<Long> measurementIds, List<String> sensors) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurementIds, "measurementIds", true);
    MissingParam.checkMissing(sensors, "sensors", true);

    Map<Long, Map<String, Double>> result = new HashMap<Long, Map<String, Double>>();

    if (sensors.size() > 0) {

      StringBuilder sql = new StringBuilder();
      sql.append("SELECT measurement_id, sensor_name, value "
          + "FROM diagnostic_data WHERE measurement_id IN (");

      sql.append(StringUtils.listToDelimited(measurementIds, ","));
      sql.append(") AND sensor_name IN (");
      sql.append(StringUtils.listToDelimited(sensors, ",", "'"));
      sql.append(") ORDER BY measurement_id");

      PreparedStatement stmt = null;
      ResultSet records = null;

      try {
        stmt = conn.prepareStatement(sql.toString());
        records = stmt.executeQuery();

        long currentId = -1;
        Map<String, Double> currentValues = null;

        while (records.next()) {

          long id = records.getLong(1);
          if (id != currentId) {
            if (currentId != -1) {
              result.put(currentId, currentValues);
            }

            currentId = id;
            currentValues = new HashMap<String, Double>();
          }

          currentValues.put(records.getString(2), records.getDouble(3));
        }

        if (currentId != -1) {
          result.put(currentId, currentValues);
        }
      } catch (SQLException e) {
        throw new DatabaseException("Error while retrieving diagnostic data", e);
      } finally {
        DatabaseUtils.closeResultSets(records);
        DatabaseUtils.closeStatements(stmt);
      }
    }

    return result;
  }

  /**
   * From a list of field names, select those fields
   * which are diagnostic sensor names
   * @param conn A database connection
   * @param instrumentId The database ID of the instrument to which the fields belong
   * @param fields The fields to be checked
   * @return The fields from the original list that are sensor names
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<String> extractDiagnosticFields(Connection conn, long instrumentId, List<String> fields) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(fields, "fields", true);

    List<String> matchedSensors = new ArrayList<String>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_DIAGNOSTIC_SENSORS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {
        String sensor = records.getString(2);
        if (fields.contains(sensor)) {
          matchedSensors.add(sensor);
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving diagnostic sensor names", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return matchedSensors;
  }
}
