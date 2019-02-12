package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 *
 * @author Steve Jones
 *
 */
public class SensorsConfiguration {

  /**
   * Query to load all sensor types from the database
   */
  private static final String GET_SENSOR_TYPES_QUERY = "SELECT "
      + "id, name, parent, depends_on, depends_question, " // 5
      + "internal_calibration, diagnostic " // 7
      + "FROM sensor_types";

  /**
   * Query to get non-core sensor types
   */
  private static final String GET_NON_CORE_TYPES_QUERY = "SELECT "
    + "id FROM sensor_types WHERE id NOT IN "
    + "(SELECT DISTINCT sensor_type FROM variable_sensors WHERE core = 1)";

  /**
   * Query to get core sensor types
   */
  private static final String GET_CORE_TYPES_QUERY = "SELECT "
    + "sensor_type FROM variable_sensors WHERE core = 1";

  private static final String GET_INSTRUMENT_SENSOR_TYPES_QUERY = "SELECT DISTINCT "
    + "sensor_type FROM variable_sensors "
    + "WHERE variable_id IN "
    + "(SELECT variable_id FROM instrument_variables WHERE instrument_id = ?)";

  private static final String GET_VARIABLES_SENSOR_TYPES_QUERY = "SELECT DISTINCT "
    + "sensor_type FROM variable_sensors "
    + "WHERE variable_id IN " + DatabaseUtils.IN_PARAMS_TOKEN;

  /**
   * The set of sensors defined for the instrument with
   * the data file columns assigned to them
   */
  private Map<Long, SensorType> sensorTypes;

  public SensorsConfiguration(DataSource dataSource) throws SensorConfigurationException {

    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      loadSensorTypes(conn);
      checkReferences();
      buildSpecialSensors();
    } catch (Exception e) {
      throw new SensorConfigurationException("Error while loading sensor configuration", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Load the sensor type details from the database. Does not perform any checks yet.
   * @throws DatabaseException If a database error occurs
   * @throws MissingParamException If any internal calls have missing parameters
   * @throws SensorConfigurationException If the configuration is invalid
   */
  private void loadSensorTypes(Connection conn)
      throws DatabaseException, MissingParamException, SensorConfigurationException {
    sensorTypes = new HashMap<Long, SensorType>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_SENSOR_TYPES_QUERY);
      records = stmt.executeQuery();

      while (records.next()) {
        SensorType type = new SensorType(records);
        sensorTypes.put(type.getId(), type);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while loading sensor types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Get the list of sensor types in this configuration
   * @return The sensor types
   */
  public List<SensorType> getSensorTypes() {
    List<SensorType> typesList = new ArrayList<SensorType>(sensorTypes.values());
    Collections.sort(typesList);
    return typesList;
  }

  /**
   * Check the sensor type parent and dependsOn references to make sure they exist
   * @throws SensorConfigurationException If any reference doesn't exist
   */
  private void checkReferences() throws SensorConfigurationException {
    for (SensorType type : sensorTypes.values()) {
      if (type.hasParent()) {
        if (!sensorTypes.containsKey(type.getParent())) {
          throw new SensorConfigurationException(type.getId(),
              "Parent ID " + type.getParent() + " does not exist");
        }
      }

      if (type.dependsOnOtherType()) {
        if (!sensorTypes.containsKey(type.getDependsOn())) {
          throw new SensorConfigurationException(type.getId(),
              "Type depends on non-existent other type");
        }
      }
    }
  }

  /**
   * Build the special sensor types used internally by the application
   */
  private void buildSpecialSensors() {
    SensorType runTypeSensor = SensorType.createRunTypeSensor();
    sensorTypes.put(runTypeSensor.getId(), runTypeSensor);
  }

  /**
   * Check a list of sensor names to ensure they are all present in the sensor configuration.
   * Note that this checks both sensor names and Required Group names
   * @param names The names to check
   * @throws SensorConfigurationException If any sensor names are not recognised
   */
  public void validateSensorNames(List<String> names) throws SensorConfigurationException {
    for (String name : names) {
      boolean found = false;

      for (SensorType sensorType : sensorTypes.values()) {
        if (sensorType.getName().equalsIgnoreCase(name) ||
          (sensorType.hasParent() &&
            sensorTypes.get(sensorType.getParent()).getName().equalsIgnoreCase(name))) {
          found = true;
          break;
        }
      }

      if (!found) {
        throw new SensorConfigurationException("Unrecognised sensor type '" + name + "'");
      }
    }
  }

  /**
   * Get an empty map of sensor types ready to have columns assigned
   * @return An empty sensor types/assignments map
   * @throws MissingParamException
   * @throws DatabaseException
   * @throws SensorConfigurationException
   */
  public SensorAssignments getNewSensorAssigments(DataSource dataSource, long instrumentId)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      return getNewSensorAssigments(conn, instrumentId);
    } catch (SQLException e) {
      throw new DatabaseException("Error while building sensor assignments list", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

  }

  /**
   * Get an empty map of sensor types ready to have columns assigned
   * @return An empty sensor types/assignments map
   * @throws MissingParamException
   * @throws DatabaseException
   * @throws SensorConfigurationException
   */
  public SensorAssignments getNewSensorAssigments(Connection conn, long instrumentId)
    throws MissingParamException, DatabaseException {

    HashMap<SensorType, Boolean> requiredTypes = new HashMap<SensorType, Boolean>();

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkZeroPositive(instrumentId, "instrumentId");

    try {
      // All non-core sensors are not required
      // (base state - some will be made required later)
      for (SensorType type : getNonCoreSensors(conn)) {
        requiredTypes.put(type, false);
      }

      // Get all required sensors for the instrument's variables
      for (SensorType type : getRequiredSensors(conn, instrumentId)) {
        requiredTypes.put(type, true);
        for (SensorType child : getChildren(type)) {
          requiredTypes.put(child, true);
        }
      }

      return new SensorAssignments(requiredTypes);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

  }

  /**
   * Get an empty map of sensor types ready to have columns assigned
   * @return An empty sensor types/assignments map
   * @throws MissingParamException
   * @throws DatabaseException
   * @throws SensorConfigurationException
   */
  public SensorAssignments getNewSensorAssigments(DataSource dataSource, List<Long> variableIds)
    throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(variableIds, "variableIds");

    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      return getNewSensorAssigments(conn, variableIds);
    } catch (SQLException e) {
      throw new DatabaseException("Error while building sensor assignments list", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

  }

  /**
   * Get an empty map of sensor types ready to have columns assigned
   * @return An empty sensor types/assignments map
   * @throws MissingParamException
   * @throws DatabaseException
   * @throws SensorConfigurationException
   */
  public SensorAssignments getNewSensorAssigments(Connection conn, List<Long> variableIds)
    throws MissingParamException, DatabaseException {

    HashMap<SensorType, Boolean> requiredTypes = new HashMap<SensorType, Boolean>();

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(variableIds, "variableIds");

    try {
      // All non-core sensors are not required
      // (base state - some will be made required later)
      for (SensorType type : getNonCoreSensors(conn)) {
        requiredTypes.put(type, false);
      }

      // Get all required sensors for the instrument's variables
      for (SensorType type : getRequiredSensors(conn, variableIds)) {
        requiredTypes.put(type, true);
        for (SensorType child : getChildren(type)) {
          requiredTypes.put(child, true);
        }
      }

      return new SensorAssignments(requiredTypes);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

  }

  /**
   * Get all the child types of a given sensor type.
   * If there are no children, the list will be empty
   * @param parent The parent sensor type
   * @return The child types
   */
  public List<SensorType> getChildren(SensorType parent) {
    List<SensorType> children = new ArrayList<SensorType>();

    for (SensorType type : sensorTypes.values()) {
      if (type.getParent() == parent.getId()) {
        children.add(type);
      }
    }

    return children;
  }

  /**
   * Get the parent SensorType of a given SensorType.
   * Returns {@code null} if there is no parent
   * @param child The sensor type whose parent is required
   * @return The parent sensor type
   */
  public SensorType getParent(SensorType child) {
    SensorType result = null;
    if (child.getParent() != SensorType.NO_PARENT) {
      result = sensorTypes.get(child.getParent());
    }
    return result;
  }

  /**
   * Determine whether a given SensorType has children
   * @param sensorType The SensorType
   * @return {@code true} if the SensorType has children; {@code false} if not
   */
  public boolean isParent(SensorType sensorType) {
    return getChildren(sensorType).size() > 0;
  }

  /**
   * Get the siblings of a given SensorType, i.e. the types that
   * have the same parent as the supplied type.
   *
   * If the type has no parents or no siblings, the returned list is empty.
   *
   * Note that the returned list does not contain the passed in type.
   *
   * @param type The type whose siblings are to be found
   * @return The siblings
   */
  public List<SensorType> getSiblings(SensorType type) {
    List<SensorType> siblings = new ArrayList<SensorType>();

    if (type.hasParent()) {
      List<SensorType> children = getChildren(getParent(type));
      for (SensorType child : children) {
        if (child.getId() != type.getId()) {
          siblings.add(child);
        }
      }
    }

    return siblings;
  }

  /**
   * Get all sensor types that are not defined as core sensor types
   * @param conn A database connection
   * @return The non-core sensor types
   * @throws DatabaseException If a database error occurs
   */
  private List<SensorType> getNonCoreSensors(Connection conn) throws DatabaseException {

    List<SensorType> result = new ArrayList<SensorType>();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_NON_CORE_TYPES_QUERY);
      records = stmt.executeQuery();
      while (records.next()) {
        result.add(sensorTypes.get(records.getLong(1)));
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving non-core sensor types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Determine whether or not a given SensorType is a core type.
   *
   * @param conn A database connection
   * @return The non-core sensor types
   * @throws DatabaseException If a database error occurs
   */
  public boolean isCoreSensor(DataSource dataSource, SensorType sensorType)
    throws DatabaseException {

    boolean core = false;

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_CORE_TYPES_QUERY);
      records = stmt.executeQuery();
      while (records.next()) {
        if (records.getLong(1) == sensorType.getId()) {
          core = true;
          break;
        }
      }

    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving non-core sensor types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return core;
  }

  /**
   * Get the {@link SensorType} object for a given sensor ID
   * @param sensorId The sensor's database ID
   * @return The SensorType object
   * @throws SensorTypeNotFoundException If the sensor type does not exist
   */
  public SensorType getSensorType(long sensorId) throws SensorTypeNotFoundException {
    SensorType result = sensorTypes.get(sensorId);
    if (null == result) {
      throw new SensorTypeNotFoundException(sensorId);
    }

    return result;
  }

  /**
   * Get the {@link SensorType} object with the given name
   * @param sensorId The sensor type's name
   * @return The SensorType object
   * @throws SensorTypeNotFoundException If the sensor type does not exist
   */
  public SensorType getSensorType(String typeName) throws SensorTypeNotFoundException {
    SensorType result = null;

    for (SensorType type: sensorTypes.values()) {
      if (type.getName().equals(typeName)) {
        result = type;
        break;
      }
    }

    if (null == result) {
      throw new SensorTypeNotFoundException(typeName);
    }
    return result;
  }

  /**
   * Get a list of all the required sensor types for a given instrument,
   * based on the variables that it measures
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The required sensor types
   * @throws DatabaseException
   */
  private List<SensorType> getRequiredSensors(Connection conn, long instrumentId) throws DatabaseException {

    List<SensorType> result = new ArrayList<SensorType>();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_INSTRUMENT_SENSOR_TYPES_QUERY);
      stmt.setLong(1, instrumentId);
      records = stmt.executeQuery();
      while (records.next()) {
        result.add(sensorTypes.get(records.getLong(1)));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving instrument sensor types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Get a list of all the required sensor types for a given instrument,
   * based on the variables that it measures
   * @param conn A database connection
   * @param instrumentId The instrument's database ID
   * @return The required sensor types
   * @throws DatabaseException
   */
  private List<SensorType> getRequiredSensors(Connection conn, List<Long> variableIds) throws DatabaseException {

    List<SensorType> result = new ArrayList<SensorType>();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(DatabaseUtils.makeInStatementSql(GET_VARIABLES_SENSOR_TYPES_QUERY, variableIds.size()));
      for (int i = 0; i < variableIds.size(); i++) {
        stmt.setLong(i + 1, variableIds.get(i));
      }

      records = stmt.executeQuery();
      while (records.next()) {
        result.add(sensorTypes.get(records.getLong(1)));
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error while retrieving instrument sensor types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Determine whether or not a given sensor type is required for an instrument
   * @param conn A database connection
   * @param instrumentId The instrument for which sensor types are being checked
   * @param sensorType The sensor type
   * @return {@code true} if the sensor is required; {@code false} if not
   * @throws DatabaseException If a database error occurs
   */
  public boolean isRequired(Connection conn, long instrumentId, SensorType sensorType) throws DatabaseException {

    // TODO This is very inefficient and will result in many database queries
    //      during data extraction. It should be fixed when the migration is complete though
    boolean required = false;

    List<SensorType> sensorTypes = getRequiredSensors(conn, instrumentId);
    if (sensorTypes.contains(sensorType)) {
      required = true;
    } else if (sensorType.hasParent()) {
      SensorsConfiguration sensorsConfig = ResourceManager.getInstance().getSensorsConfiguration();
      SensorType parent = sensorsConfig.getParent(sensorType);
      if (sensorTypes.contains(parent)) {
        required = true;
      }
    }

    return required;
  }
}
