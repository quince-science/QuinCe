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
import java.util.Set;

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
      + " (measurement_id, file_column_id, value) VALUES"
      + " (?, ?, ?)";

  /**
   * Statement to get diagnostic sensor names for a data set
   */
  private static final String GET_DIAGNOSTIC_SENSORS_QUERY = "SELECT "
      + "id, sensor_type, sensor_name FROM file_column "
      + "WHERE sensor_type LIKE 'Diagnostic: %' AND "
      + "file_definition_id IN "
      + "(SELECT file_definition_id FROM file_definition WHERE instrument_id = ?) "
      + "ORDER BY sensor_type, sensor_name";

  /**
   * Store a set of diagnostic values
   * @param conn A database connection
   * @param measurementId The database ID of the measurement to which the values belong
   * @param diagnosticValues The diagnostic values
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any required parameters are missing
   */
  protected static void storeDiagnosticValues(Connection conn, long measurementId, Map<Long, Double> diagnosticValues) throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(measurementId, "measurementId");
    MissingParam.checkMissing(diagnosticValues, "diagnosticValues", true);

    PreparedStatement diagnosticStatement = null;

    try {
      diagnosticStatement = conn.prepareStatement(STORE_DIAGNOSTIC_VALUE_STATEMENT);

      for (Map.Entry<Long, Double> entry : diagnosticValues.entrySet()) {
        diagnosticStatement.setLong(1, measurementId);
        diagnosticStatement.setLong(2, entry.getKey());

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
        String group = records.getString(2);
        if (!variables.containsGroup(group)) {
          variables.add(new VariableGroup(group, true));
        }

        String name = records.getString(3);
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
   * map of sensor key -> sensor value.
   *
   * @param dataSource A data source
   * @param measurementIds The IDs of the measurements whose diagnostic data is to be retrieved
   * @param sensors The sensors whose data is to be retrieved
   * @return The diagnostic data values
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static Map<Long, Map<String, Double>> getDiagnosticValues(DataSource dataSource, long instrumentId, List<Long> measurementIds, List<String> sensorNames) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(measurementIds, "measurementIds", true);
    MissingParam.checkMissing(sensorNames, "sensorNames", true);

    Connection conn = null;
    Map<Long, Map<String, Double>> result = null;

    try {
      conn = dataSource.getConnection();
      Map<Long, String> idMap = getDiagnosticSensorIdMap(conn, instrumentId);

      // Remove the sensor names we don't need
      for (Map.Entry<Long, String> entry : idMap.entrySet()) {
        if (!sensorNames.contains(entry.getValue())) {
          idMap.remove(entry.getKey());
        }
      }

      result = getDiagnosticValues(conn, measurementIds, idMap);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving diagnostic data", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Retrieve a set of diagnostic data values for a set of measurements.
   * The values are returned in a nested HashMap. The outer level
   * is identified by the measurement id. Within each ID is a
   * map of sensor key -> sensor value.
   *
   * @param dataSource A database connection
   * @param measurementIds The IDs of the measurements whose diagnostic data is to be retrieved
   * @param sensors The sensors whose data is to be retrieved
   * @return The diagnostic data values
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static Map<Long, Map<String, Double>> getDiagnosticValues(Connection conn, long instrumentId, List<Long> measurementIds, List<String> sensorNames) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurementIds, "measurementIds", true);
    MissingParam.checkMissing(sensorNames, "sensorNames", true);

    Map<Long, String> allSensors = getDiagnosticSensorIdMap(conn, instrumentId);
    Map<Long, String> requestedSensors = new HashMap<Long, String>();

    // Remove the sensor names we don't need
    for (Map.Entry<Long, String> entry : allSensors.entrySet()) {
      if (sensorNames.contains(entry.getValue())) {
        requestedSensors.put(entry.getKey(), entry.getValue());
      }
    }

    return getDiagnosticValues(conn, measurementIds, requestedSensors);
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
  private static Map<Long, Map<String, Double>> getDiagnosticValues(Connection conn, List<Long> measurementIds, Map<Long, String> idMap) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(measurementIds, "measurementIds", true);
    MissingParam.checkMissing(idMap, "sensorIds", true);

    Map<Long, Map<String, Double>> result = new HashMap<Long, Map<String, Double>>();

    if (idMap.size() > 0) {

      Set<Long> sensorIds = idMap.keySet();

      StringBuilder sql = new StringBuilder();
      sql.append("SELECT measurement_id, file_column_id, value "
          + "FROM diagnostic_data WHERE measurement_id IN (");

      sql.append(StringUtils.listToDelimited(measurementIds, ","));
      sql.append(") AND file_column_id IN (");
      sql.append(StringUtils.listToDelimited(sensorIds, ",", "'"));
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

          currentValues.put(idMap.get(records.getLong(2)), records.getDouble(3));
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

    List<String> sensors = getDiagnosticSensorNames(conn, instrumentId);

    for (String field : fields) {
      if (sensors.contains(field)) {
        matchedSensors.add(field);
      }
    }

    return matchedSensors;
  }

  /**
   * Determine whether a given field relates to a diagnostic sensor
   * @param conn A database connection
   * @param instrumentId The database ID of the instrument concerned
   * @param field The field name
   * @return {@code true} if the field is for a diagnostic sensor; {@code false} if it is not
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static boolean isDiagnosticField(Connection conn, long instrumentId, String field) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(field, "field");

    List<String> sensors = getDiagnosticSensorNames(conn, instrumentId);

    return sensors.contains(field);
  }

  public static long getSensorId(Connection conn, long instrumentId, String sensorName) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");
    MissingParam.checkMissing(sensorName, "sensorName");

    long result = -1;

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_DIAGNOSTIC_SENSORS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {
        String foundSensor = records.getString(3);
        if (foundSensor.equals(sensorName)) {
          result = records.getLong(1);
          break;
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting diagnostic sensor names", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Get the list of diagnostic sensors for a given instrument
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The list of diagnostic sensors
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<String> getDiagnosticSensorNames(DataSource dataSource, long instrumentId) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(instrumentId, "instrumentId");

    Connection conn = null;
    List<String> result = null;

    try {
      conn = dataSource.getConnection();
      result = getDiagnosticSensorNames(conn, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving diagnostic data", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Get the list of diagnostic sensors for a given instrument
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The list of diagnostic sensors
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static List<String> getDiagnosticSensorNames(Connection conn, long instrumentId) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    List<String> result = new ArrayList<String>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    // TODO Can we split this out into a sub-function? It's also used by getSensorId
    try {
      stmt = conn.prepareStatement(GET_DIAGNOSTIC_SENSORS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {
        result.add(records.getString(3));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting diagnostic sensor names", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Get the list of diagnostic sensors, with their database ID's for a given instrument
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The map of diagnostic sensors indexed by database ID
   * @throws MissingParamException If any required parameters are missing
   * @throws DatabaseException If a database error occurs
   */
  public static Map<Long, String> getDiagnosticSensorIdMap(Connection conn, long instrumentId) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    Map<Long, String> result = new HashMap<Long, String>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_DIAGNOSTIC_SENSORS_QUERY);
      stmt.setLong(1, instrumentId);

      records = stmt.executeQuery();

      while (records.next()) {
        result.put(records.getLong(1), records.getString(3));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while getting diagnostic sensor names", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }
}
