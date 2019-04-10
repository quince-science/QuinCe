package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;

import uk.ac.exeter.QCRoutines.messages.InvalidFlagException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

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
      + "id, name, vargroup, parent, depends_on, depends_question, " // 6
      + "internal_calibration, diagnostic " // 8
      + "FROM sensor_types";

  /**
   * Query to get non-core sensor types
   */
  private static final String GET_NON_CORE_TYPES_QUERY = "SELECT "
    + "id FROM sensor_types WHERE id NOT IN "
    + "(SELECT DISTINCT sensor_type FROM variable_sensors WHERE core = 1)";

  /**
   * Query to get the sensor types required by all variables
   */
  private static final String GET_VARIABLES_SENSOR_TYPES_QUERY = "SELECT "
    + "v.id, v.name, "
    + "s.sensor_type, s.core, s.questionable_cascade, s.bad_cascade "
    + "FROM variables v INNER JOIN variable_sensors s ON v.id = s.variable_id "
    + "ORDER BY v.id";

  /**
   * The set complete set of sensor types
   */
  private Map<Long, SensorType> sensorTypes;

  /**
   * The set of variables an instrument can measure
   */
  private Map<Long, InstrumentVariable> instrumentVariables;

  public SensorsConfiguration(DataSource dataSource)
    throws SensorConfigurationException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");

    Connection conn = null;
    try {
      conn = dataSource.getConnection();
      loadSensorTypes(conn);
      loadInstrumentVariables(conn);
      checkReferences();
      checkParentsAndChildren();
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
   * Load the sensor types used by the variables defined in the database
   * @param conn A database connection
   * @throws DatabaseException If a database error occurs
   * @throws InvalidFlagException
   * @throws SensorConfigurationException
   * @throws SensorTypeNotFoundException
   */
  private void loadInstrumentVariables(Connection conn) throws DatabaseException, SensorTypeNotFoundException, SensorConfigurationException, InvalidFlagException {

    instrumentVariables = new HashMap<Long, InstrumentVariable>();

    PreparedStatement stmt = null;
    ResultSet records = null;


    try {
      stmt = conn.prepareStatement(GET_VARIABLES_SENSOR_TYPES_QUERY);
      records = stmt.executeQuery();

      long currentVariable = -1;
      String name = null;
      long coreSensorType = -1;
      List<Long> requiredSensorTypes = new ArrayList<Long>();;
      List<Integer> questionableCascades = new ArrayList<Integer>();
      List<Integer> badCascades = new ArrayList<Integer>();

      while (records.next()) {

        long newVariable = records.getLong(1);
        if (newVariable != currentVariable) {
          if (currentVariable > -1) {
            // Write the old variable
            instrumentVariables.put(currentVariable,
              new InstrumentVariable(this, currentVariable, name, coreSensorType,
                requiredSensorTypes, questionableCascades, badCascades));
          }

          // Set up the new variable
          currentVariable = newVariable;
          name = records.getString(2);
          requiredSensorTypes = new ArrayList<Long>();
          questionableCascades = new ArrayList<Integer>();
          badCascades = new ArrayList<Integer>();
        }


        long sensorTypeId = records.getLong(3);
        boolean core = records.getBoolean(4);
        if (core) {
          coreSensorType = sensorTypeId;
        } else {
          requiredSensorTypes.add(sensorTypeId);
          questionableCascades.add(records.getInt(5));
          badCascades.add(records.getInt(6));
        }

      }

      // Write the last variable
      instrumentVariables.put(currentVariable,
        new InstrumentVariable(this, currentVariable, name, coreSensorType,
          requiredSensorTypes, questionableCascades, badCascades));
    } catch(SQLException e) {
      throw new DatabaseException("Error while loading instrument variables", e);
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
    sensorTypes.put(SensorType.RUN_TYPE_SENSOR_TYPE.getId(), SensorType.RUN_TYPE_SENSOR_TYPE);
  }

  /**
   * Check a list of sensor names to ensure they are all present in the sensor configuration.
   * @param names The names to check
   * @throws SensorConfigurationException If any sensor names are not recognised
   */
  public void validateSensorNames(List<String> names) throws SensorConfigurationException {
    for (String name : names) {
      boolean found = false;

      for (SensorType sensorType : sensorTypes.values()) {
        if (sensorType.getName().equalsIgnoreCase(name)) {
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
   * Get the core sensor type for a set of variables identified by ID
   * @param varId The variable IDs
   * @return The core sensor types
   * @throws SensorConfigurationException If any variables cannot be found
   */
  public List<SensorType> getCoreSensors(List<Long> varIds)
    throws SensorConfigurationException {

    List<SensorType> result = new ArrayList<SensorType>(varIds.size());

    for (long varId : varIds) {
      InstrumentVariable variable = instrumentVariables.get(varId);
      if (null == variable) {
        throw new SensorConfigurationException("Cannot find variable with ID " + varId);
      }

      result.add(variable.getCoreSensorType());
    }

    return result;
  }

  /**
   * Get all sensor types that are not defined as core sensor types
   * @param conn A database connection
   * @return The non-core sensor types
   * @throws DatabaseException If a database error occurs
   * @throws SensorTypeNotFoundException If any types do not exist in the configuration
   */
  public Set<SensorType> getNonCoreSensors(Connection conn)
    throws DatabaseException, SensorTypeNotFoundException {

    Set<SensorType> result = new TreeSet<SensorType>();
    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_NON_CORE_TYPES_QUERY);
      records = stmt.executeQuery();
      while (records.next()) {
        long typeId = records.getLong(1);
        SensorType type = getSensorType(typeId);

        // If this is a parent type, add the children
        if (isParent(type)) {
          for (SensorType child : getChildren(type)) {
            result.add(child);
          }
        } else if (getParent(type) != null) {
          // If this is a child, add it and all its siblings
          for (SensorType child : getChildren(getParent(type))) {
            result.add(child);
          }
        } else {
          result.add(sensorTypes.get(records.getLong(1)));
        }
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
  public boolean isCoreSensor(SensorType sensorType)
    throws DatabaseException {

    boolean core = false;

    for (InstrumentVariable variable : instrumentVariables.values()) {
      if (variable.getCoreSensorType().equals(sensorType)) {
        core = true;
        break;
      }
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
   * See if the supplied SensorType is required by any of the listed variables
   * @param sensorType The SensorType
   * @param variableIds The variables' database IDs
   * @return {@code true} if any variable requires the SensorType; {@code false} if not
   * @throws SensorConfigurationException If any variable ID is invalid
   */
  public boolean requiredForVariables(SensorType sensorType, List<Long> variableIds) throws SensorConfigurationException {

    boolean required = false;

    for (long varId : variableIds) {
      InstrumentVariable variable = instrumentVariables.get(varId);
      if (null == variable) {
        throw new SensorConfigurationException("Unknown variable ID " + varId);
      } else {
        required = requiredForVariable(sensorType, variable);
        if (required) {
          break;
        }
      }
    }

    return required;
  }

  /**
   * See if the supplied SensorType is required by any of the listed variables
   * @param sensorType The SensorType
   * @param variableIds The variables' database IDs
   * @return {@code true} if any variable requires the SensorType; {@code false} if not
   */
  public boolean requiredForVariable(SensorType sensorType, InstrumentVariable variable) {
    boolean required = false;

    List<SensorType> variableSensorTypes = variable.getAllSensorTypes();
    for (SensorType varSensorType : variableSensorTypes) {
      if (varSensorType.equalsIncludingRelations(sensorType)) {
        required = true;
        break;
      }
    }

    return required;
  }

  /**
   * Get the set of SensorTypes required for the specified variables.
   * @param variableIds The variables' database IDs
   * @return The SensorTypes required by the variables
   * @throws SensorConfigurationException If any variable IDs do not exist
   */
  public Set<SensorType> getSensorTypes(List<Long> variableIds,
    boolean replaceParentsWithChildren) throws SensorConfigurationException {

    Set<SensorType> sensorTypes = new HashSet<SensorType>();

    for (long varId : variableIds) {
      InstrumentVariable variable = instrumentVariables.get(varId);
      if (null == variable) {
        throw new SensorConfigurationException("Unknown variable ID " + varId);
      } else {
        if (!replaceParentsWithChildren) {
          sensorTypes.addAll(variable.getAllSensorTypes());
        } else {
          for (SensorType tempType : variable.getAllSensorTypes()) {
            if (!isParent(tempType)) {
              sensorTypes.add(tempType);
            } else {
              sensorTypes.addAll(getChildren(tempType));
            }
          }
        }
      }
    }

    return sensorTypes;
  }

  /**
   * Convenience method to see if a SensorType has a parent. Simply calls
   * {@link SensorType#hasParent()}.
   * @param sensorType The SensorType to check
   * @return {@code true} if the SensorType has a parent; {@code false} if not
   */
  public boolean hasParent(SensorType sensorType) {
    return sensorType.hasParent();
  }

  /**
   * Make sure parents and children are configured correctly
   * @throws SensorConfigurationException If any configuration is invalid
   */
  private void checkParentsAndChildren() throws SensorConfigurationException {

    for (SensorType sensorType : getSensorTypes()) {
      // Parents must have more than one child
      if (isParent(sensorType)) {
        List<SensorType> children = getChildren(sensorType);
        if (children.size() <= 1) {
          throw new SensorConfigurationException(
            "SensorType " + sensorType.getId() + " must have more than one child"
          );
        }

        if (hasParent(sensorType)) {
          throw new SensorConfigurationException(
            "SensorType " + sensorType.getId() + " cannot be both a parent and a child"
          );
        }

        if (sensorType.dependsOnOtherType()) {
          throw new SensorConfigurationException(
            "SensorType " + sensorType.getId() + " is a parent and cannot depend on another sensor type"
          );
        }
      }
    }
  }

  /**
   * Get an InstrumentVariable by its ID
   * @param variableId The variable ID
   * @return The InstrumentVariable object
   * @throws VariableNotFoundException If the ID isn't found
   */
  public InstrumentVariable getInstrumentVariable(long variableId) throws VariableNotFoundException {
    InstrumentVariable result = instrumentVariables.get(variableId);
    if (null == result) {
      throw new VariableNotFoundException(variableId);
    }
    return result;
  }

  /**
   * Get a list of InstrumentVaraiables using their IDs
   * @param variableId The variable IDs
   * @return The InstrumentVariable objects
   * @throws VariableNotFoundException If any IDs aren't found
   */
  public List<InstrumentVariable> getInstrumentVariables(List<Long> variableIds) throws VariableNotFoundException {

    List<InstrumentVariable> variables = new ArrayList<InstrumentVariable>(variableIds.size());
    for (long id : variableIds) {
      InstrumentVariable variable = instrumentVariables.get(id);
      if (null == variable) {
        throw new VariableNotFoundException(id);
      }
      variables.add(variable);
    }

    return variables;

  }
}
