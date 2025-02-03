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
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

public class SensorsConfiguration {

  /**
   * Query to load all sensor types from the database
   */
  private static final String GET_SENSOR_TYPES_QUERY = "SELECT "
    + "id, name, vargroup, parent, depends_on, depends_question, "
    + "internal_calibration, use_zero_in_calibration, run_type_aware, "
    + "diagnostic, display_order, units, column_code, column_heading, source_columns "
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
    + "v.id, v.name, v.attributes, v.properties, "
    + "s.sensor_type, s.core, s.attribute_name, s.attribute_value, "
    + "s.questionable_cascade, s.bad_cascade, "
    + "COALESCE(s.export_column_short, t.name), "
    + "COALESCE(s.export_column_long, t.column_heading), "
    + "COALESCE(s.export_column_code, t.column_code), t.units "
    + "FROM variables v INNER JOIN variable_sensors s ON v.id = s.variable_id "
    + "INNER JOIN sensor_types t ON s.sensor_type = t.id " + "ORDER BY v.id";

  /**
   * The set complete set of sensor types
   */
  private Map<Long, SensorType> sensorTypes;

  /**
   * The set of variables an instrument can measure
   */
  private Map<Long, Variable> instrumentVariables;

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
      ExceptionUtils.printStackTrace(e);
      throw new SensorConfigurationException(
        "Error while loading sensor configuration", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Load the sensor type details from the database. Does not perform any checks
   * yet.
   *
   * @param conn
   *          A database connection.
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any internal calls have missing parameters
   * @throws SensorConfigurationException
   *           If the configuration is invalid
   */
  private void loadSensorTypes(Connection conn) throws DatabaseException,
    MissingParamException, SensorConfigurationException {
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
   *
   * @param conn
   *          A database connection
   * @throws DatabaseException
   *           If a database error occurs
   * @throws InvalidFlagException
   *           If any of the flag cascade values are invalid.
   * @throws SensorConfigurationException
   *           If the sensor configuration is invalid.
   * @throws SensorTypeNotFoundException
   *           If any specified {@link SensorType}s cannot be found.
   */
  private void loadInstrumentVariables(Connection conn)
    throws DatabaseException, SensorTypeNotFoundException,
    SensorConfigurationException, InvalidFlagException {

    instrumentVariables = new HashMap<Long, Variable>();

    PreparedStatement stmt = null;
    ResultSet records = null;

    try {
      stmt = conn.prepareStatement(GET_VARIABLES_SENSOR_TYPES_QUERY);
      records = stmt.executeQuery();

      long currentVariableId = -1;
      String name = null;
      VariableAttributes attributes = null;
      String propertiesJson = null;
      long coreSensorType = -1L;
      Map<Long, AttributeCondition> attributeConditions = new HashMap<Long, AttributeCondition>();
      List<Long> requiredSensorTypes = new ArrayList<Long>();
      List<Integer> questionableCascades = new ArrayList<Integer>();
      List<Integer> badCascades = new ArrayList<Integer>();
      Map<SensorType, ColumnHeading> columnHeadings = new HashMap<SensorType, ColumnHeading>();

      while (records.next()) {

        long newVariable = records.getLong(1);
        if (newVariable != currentVariableId) {
          if (currentVariableId > -1) {
            // Write the old variable
            instrumentVariables.put(currentVariableId,
              new Variable(this, currentVariableId, name, attributes,
                propertiesJson, coreSensorType, requiredSensorTypes,
                attributeConditions, questionableCascades, badCascades,
                columnHeadings));
          }

          // Set up the new variable
          currentVariableId = newVariable;
          name = records.getString(2);
          attributes = makeAttributesMap(records.getString(3));
          propertiesJson = records.getString(4);
          coreSensorType = -1L;
          attributeConditions = new HashMap<Long, AttributeCondition>();
          requiredSensorTypes = new ArrayList<Long>();
          questionableCascades = new ArrayList<Integer>();
          badCascades = new ArrayList<Integer>();
          columnHeadings = new HashMap<SensorType, ColumnHeading>();
        }

        long sensorTypeId = records.getLong(5);
        boolean core = records.getBoolean(6);
        if (core) {
          coreSensorType = sensorTypeId;
        } else {
          requiredSensorTypes.add(sensorTypeId);

          String attributeName = records.getString(7);
          String attributeValue = records.getString(8);
          if (null != attributeName) {
            attributeConditions.put(sensorTypeId,
              new AttributeCondition(attributeName, attributeValue));
          }

          questionableCascades.add(records.getInt(9));
          badCascades.add(records.getInt(10));
        }

        String shortName = records.getString(11);
        String longName = records.getString(12);
        String code = records.getString(13);
        String units = records.getString(14);

        SensorType sensorType = this.getSensorType(sensorTypeId);
        columnHeadings.put(sensorType, new ColumnHeading(sensorTypeId,
          shortName, longName, code, units, true, true));
      }

      // Write the last variable
      instrumentVariables.put(currentVariableId,
        new Variable(this, currentVariableId, name, attributes, propertiesJson,
          coreSensorType, requiredSensorTypes, attributeConditions,
          questionableCascades, badCascades, columnHeadings));
    } catch (SQLException e) {
      throw new DatabaseException("Error while loading instrument variables",
        e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }
  }

  private VariableAttributes makeAttributesMap(String attributesJson) {
    return null == attributesJson ? new VariableAttributes()
      : new GsonBuilder()
        .registerTypeAdapter(VariableAttributes.class,
          new VariableAttributesDeserializer())
        .create().fromJson(attributesJson, VariableAttributes.class);
  }

  /**
   * Get the list of sensor types in this configuration
   *
   * @return The sensor types
   */
  public List<SensorType> getSensorTypes() {
    List<SensorType> typesList = new ArrayList<SensorType>(
      sensorTypes.values());
    Collections.sort(typesList);
    return typesList;
  }

  /**
   * Check the sensor type parent and dependsOn references to make sure they
   * exist
   *
   * @throws SensorConfigurationException
   *           If any reference doesn't exist
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
    sensorTypes.put(SensorType.RUN_TYPE_SENSOR_TYPE.getId(),
      SensorType.RUN_TYPE_SENSOR_TYPE);
  }

  /**
   * Check a list of sensor names to ensure they are all present in the sensor
   * configuration.
   *
   * @param names
   *          The names to check
   * @throws SensorConfigurationException
   *           If any sensor names are not recognised
   */
  public void validateSensorNames(List<String> names)
    throws SensorConfigurationException {
    for (String name : names) {
      boolean found = false;

      for (SensorType sensorType : sensorTypes.values()) {
        if (sensorType.getShortName().equalsIgnoreCase(name)) {
          found = true;
          break;
        }
      }

      if (!found) {
        throw new SensorConfigurationException(
          "Unrecognised sensor type '" + name + "'");
      }
    }
  }

  /**
   * Get all the child types of a given sensor type. If there are no children,
   * the list will be empty.
   *
   * @param parent
   *          The parent sensor type
   * @return The child types
   */
  public Set<SensorType> getChildren(SensorType parent) {

    Set<SensorType> children = new HashSet<SensorType>();

    // Ignore position sensor types
    if (!parent.equals(SensorType.LONGITUDE_SENSOR_TYPE)
      && !parent.equals(SensorType.LATITUDE_SENSOR_TYPE)) {

      for (SensorType type : sensorTypes.values()) {
        if (type.getParent() == parent.getId()) {
          children.add(type);
        }
      }
    }

    return children;
  }

  /**
   * Get the parent SensorType of a given SensorType. Returns {@code null} if
   * there is no parent
   *
   * @param child
   *          The sensor type whose parent is required
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
   *
   * @param sensorType
   *          The SensorType
   * @return {@code true} if the SensorType has children; {@code false} if not
   */
  public boolean isParent(SensorType sensorType) {
    return getChildren(sensorType).size() > 0;
  }

  /**
   * Get the siblings of a given SensorType, i.e. the types that have the same
   * parent as the supplied type.
   *
   * If the type has no parents or no siblings, the returned list is empty.
   *
   * Note that the returned list does not contain the passed in type.
   *
   * @param type
   *          The type whose siblings are to be found
   * @return The siblings
   */
  public List<SensorType> getSiblings(SensorType type) {
    List<SensorType> siblings = new ArrayList<SensorType>();

    if (type.hasParent()) {
      Set<SensorType> children = getChildren(getParent(type));
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
   *
   * @param varIds
   *          The variable IDs
   * @return The core sensor types
   * @throws SensorConfigurationException
   *           If any variables cannot be found
   */
  public List<SensorType> getCoreSensors(List<Long> varIds)
    throws SensorConfigurationException {

    List<SensorType> result = new ArrayList<SensorType>(varIds.size());

    for (long varId : varIds) {
      Variable variable = instrumentVariables.get(varId);
      if (null == variable) {
        throw new SensorConfigurationException(
          "Cannot find variable with ID " + varId);
      }

      result.add(variable.getCoreSensorType());
    }

    return result;
  }

  /**
   * Get all sensor types that are not defined as core sensor types
   *
   * @param conn
   *          A database connection
   * @return The non-core sensor types
   * @throws DatabaseException
   *           If a database error occurs
   * @throws SensorTypeNotFoundException
   *           If any types do not exist in the configuration
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
      throw new DatabaseException(
        "Error while retrieving non-core sensor types", e);
    } finally {
      DatabaseUtils.closeResultSets(records);
      DatabaseUtils.closeStatements(stmt);
    }

    return result;
  }

  /**
   * Determine whether or not a given SensorType is a core type for any
   * {@link Variable}.
   *
   * @param sensorType
   *          The Sensor Type
   * @return {@code true} If at least one {@link Variable} has the Sensor Type
   *         as a core type; {@code false} otherwise.
   * @throws DatabaseException
   *           If a database error occurs
   */
  public boolean isCoreSensor(SensorType sensorType) throws DatabaseException {
    return instrumentVariables.values().stream()
      .anyMatch(v -> v.getCoreSensorType().equals(sensorType));
  }

  /**
   * Get the {@link SensorType} object for a given sensor ID
   *
   * @param sensorId
   *          The sensor's database ID
   * @return The SensorType object
   * @throws SensorTypeNotFoundException
   *           If the sensor type does not exist
   */
  public SensorType getSensorType(long sensorId)
    throws SensorTypeNotFoundException {

    SensorType result = null;

    if (sensorId == SensorType.LONGITUDE_ID) {
      result = SensorType.LONGITUDE_SENSOR_TYPE;
    } else if (sensorId == SensorType.LATITUDE_ID) {
      result = SensorType.LATITUDE_SENSOR_TYPE;
    } else {
      result = sensorTypes.get(sensorId);
    }

    if (null == result) {
      throw new SensorTypeNotFoundException(sensorId);
    }

    return result;
  }

  /**
   * Get the {@link SensorType} object with the given name
   *
   * @param typeName
   *          The sensor type's name
   * @return The SensorType object
   * @throws SensorTypeNotFoundException
   *           If the sensor type does not exist
   */
  public SensorType getSensorType(String typeName)
    throws SensorTypeNotFoundException {
    SensorType result = null;

    for (SensorType type : sensorTypes.values()) {
      if (type.getShortName().equals(typeName)) {
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
   * Get the list of {@link SensorType} objects corresponding to the supplied
   * list of names
   *
   * @param typeNames
   *          The sensor types' names
   * @return The SensorType objects
   * @throws SensorTypeNotFoundException
   *           If any sensor type does not exist
   */
  public List<SensorType> getSensorTypes(String[] typeNames)
    throws SensorTypeNotFoundException {
    List<SensorType> result = new ArrayList<SensorType>(typeNames.length);
    for (String typeName : typeNames) {
      result.add(getSensorType(typeName));
    }

    return result;
  }

  /**
   * See if the supplied SensorType is required by any of the listed variables
   *
   * @param sensorType
   *          The SensorType
   * @param variableIds
   *          The variables' database IDs
   * @return {@code true} if any variable requires the SensorType; {@code false}
   *         if not
   * @throws SensorConfigurationException
   *           If any variable ID is invalid
   */
  public boolean requiredForVariables(SensorType sensorType,
    List<Long> variableIds, Map<Long, VariableAttributes> varAttributes)
    throws SensorConfigurationException {

    boolean required = false;

    for (long varId : variableIds) {
      Variable variable = instrumentVariables.get(varId);
      if (null == variable) {
        throw new SensorConfigurationException("Unknown variable ID " + varId);
      } else {
        required = requiredForVariable(sensorType, variable,
          varAttributes.get(varId));
        if (required) {
          break;
        }
      }
    }

    return required;
  }

  /**
   * See if the supplied {@link SensorType} is required by specified
   * {@link Variable}.
   *
   * The method searches the dependents of the {@link SensorType}s directly
   * assigned to the {@link Variable}. Any that are dependent, either always or
   * via a Depends Question, will result in a {@code true} return value.
   *
   * @param sensorType
   *          The SensorType
   * @param variable
   *          The Variable
   * @return {@code true} if the Variable requires the SensorType; {@code false}
   *         if not
   * @throws SensorConfigurationException
   *           If the sensor configuration is internally inconsistent
   */
  public boolean requiredForVariable(SensorType sensorType, Variable variable,
    VariableAttributes attributes) throws SensorConfigurationException {

    boolean required = false;

    List<SensorType> variableSensorTypes = variable.getAllSensorTypes(false);
    for (SensorType varSensorType : variableSensorTypes) {
      if (varSensorType.equalsIncludingRelations(sensorType)) {

        // See if the requirement is based on a variable attribute
        AttributeCondition attrCondition = variable
          .getAttributeCondition(sensorType);
        if (null == attrCondition) {
          required = true;
        } else {
          required = attrCondition.matches(attributes);
        }

        if (required) {
          break;
        }
      } else if (varSensorType.dependsOnOtherType()) {
        try {
          SensorType dependsOnType = getSensorType(
            varSensorType.getDependsOn());

          // Does this type depend on us?
          if (dependsOnType.equalsIncludingRelations(sensorType)) {

            // Yes. Is there a Depends Question? If so, it's not specifically
            // required.
            // Other checks later in processing will see if the question has
            // been answered
            if (!varSensorType.hasDependsQuestion()) {
              required = true;
              break;
            }
          }
        } catch (SensorTypeNotFoundException e) {
          throw new SensorConfigurationException(
            "Cannot find sensor type that should exist", e);
        }
      }
    }

    return required;
  }

  /**
   * Get the set of SensorTypes required for the specified variables.
   *
   * @param variableIds
   *          The variables' database IDs
   * @return The SensorTypes required by the variables
   * @throws SensorConfigurationException
   *           If the sensor configuration is invalid.
   */

  /**
   * Get the {@link SensorType}s required for the specified {@link Variable}s.
   *
   * @param variableIds
   *          The {@link Variable}s' database IDs.
   * @param replaceParentsWithChildren
   *          Indicates whether parent {@link SensorType}s should be replaced
   *          with their children. For example, <i>Pressure in Equilibrator</i>
   *          may be replaced with <i>Absolute Pressure in Equilibrator</i> and
   *          <i>Relative Pressure in Equilibrator</i>.
   * @param includeDependents
   *          Indicates whether or not dependent {@link SensorType}s should be
   *          included.
   * @param includeRunType
   *          Indicates whether or not the special Run Type indicator should be
   *          included.
   * @return The required {@link SensorType}s.
   * @throws SensorConfigurationException
   *           If the sensor configuration is invaid.
   */
  public Set<SensorType> getSensorTypes(List<Long> variableIds,
    boolean replaceParentsWithChildren, boolean includeDependents,
    boolean includeRunType) throws SensorConfigurationException {

    Set<SensorType> sensorTypes = new TreeSet<SensorType>();

    for (long varId : variableIds) {
      Variable variable = instrumentVariables.get(varId);
      if (null == variable) {
        throw new SensorConfigurationException("Unknown variable ID " + varId);
      } else {
        try {
          if (includeRunType && variable.requiresRunType()) {
            sensorTypes.add(SensorType.RUN_TYPE_SENSOR_TYPE);
          }

          for (SensorType sensorType : variable.getAllSensorTypes(false)) {
            if (sensorType.hasInternalCalibration() && includeRunType) {
              sensorTypes.add(SensorType.RUN_TYPE_SENSOR_TYPE);
            }

            if (!isParent(sensorType) || !replaceParentsWithChildren) {
              sensorTypes.add(sensorType);
              if (includeDependents && sensorType.dependsOnOtherType()) {
                sensorTypes.add(getSensorType(sensorType.getDependsOn()));
              }
            } else {
              for (SensorType childType : getChildren(sensorType)) {
                sensorTypes.add(childType);
                if (includeDependents && childType.dependsOnOtherType()) {
                  sensorTypes.add(getSensorType(childType.getDependsOn()));
                }
              }
            }
          }
        } catch (SensorTypeNotFoundException e) {
          throw new SensorConfigurationException(
            "Unrecognised SensorType encountered", e);
        }
      }
    }

    return sensorTypes;
  }

  /**
   * Get the {@link SensorType}s required for the specified {@link Variable}.
   *
   * @param variableId
   *          The {@link Variable}'s database ID.
   * @param replaceParentsWithChildren
   *          Indicates whether parent {@link SensorType}s should be replaced
   *          with their children. For example, <i>Pressure in Equilibrator</i>
   *          may be replaced with <i>Absolute Pressure in Equilibrator</i> and
   *          <i>Relative Pressure in Equilibrator</i>.
   * @param includeDependents
   *          Indicates whether or not dependent {@link SensorType}s should be
   *          included.
   * @param includeRunType
   *          Indicates whether or not the special Run Type indicator should be
   *          included.
   * @return The required {@link SensorType}s.
   * @throws SensorConfigurationException
   *           If the sensor configuration is invaid.
   */
  public Set<SensorType> getSensorTypes(long variableId,
    boolean replaceParentsWithChildren, boolean includeDependents,
    boolean includeRunType) throws SensorConfigurationException {

    List<Long> varList = new ArrayList<Long>(1);
    varList.add(variableId);
    return getSensorTypes(varList, replaceParentsWithChildren,
      includeDependents, includeRunType);
  }

  /**
   * Convenience method to see if a SensorType has a parent. Simply calls
   * {@link SensorType#hasParent()}.
   *
   * @param sensorType
   *          The SensorType to check
   * @return {@code true} if the SensorType has a parent; {@code false} if not
   */
  public boolean hasParent(SensorType sensorType) {
    return sensorType.hasParent();
  }

  /**
   * Make sure parents and children are configured correctly
   *
   * @throws SensorConfigurationException
   *           If any configuration is invalid
   */
  private void checkParentsAndChildren() throws SensorConfigurationException {

    for (SensorType sensorType : getSensorTypes()) {
      // Parents must have more than one child
      if (isParent(sensorType)) {
        Set<SensorType> children = getChildren(sensorType);
        if (children.size() <= 1) {
          throw new SensorConfigurationException("SensorType "
            + sensorType.getId() + " must have more than one child");
        }

        if (hasParent(sensorType)) {
          throw new SensorConfigurationException("SensorType "
            + sensorType.getId() + " cannot be both a parent and a child");
        }

        if (sensorType.dependsOnOtherType()) {
          throw new SensorConfigurationException(
            "SensorType " + sensorType.getId()
              + " is a parent and cannot depend on another sensor type");
        }
      }
    }
  }

  /**
   * Get an InstrumentVariable by its ID
   *
   * @param variableId
   *          The variable ID
   * @return The InstrumentVariable object
   * @throws VariableNotFoundException
   *           If the ID isn't found
   */
  public Variable getInstrumentVariable(long variableId)
    throws VariableNotFoundException {
    Variable result = instrumentVariables.get(variableId);
    if (null == result) {
      throw new VariableNotFoundException(variableId);
    }
    return result;
  }

  public Variable getInstrumentVariable(String name)
    throws VariableNotFoundException {
    Variable result = null;

    for (Variable variable : instrumentVariables.values()) {
      if (variable.getName().equals(name)) {
        result = variable;
        break;
      }
    }
    if (null == result) {
      throw new VariableNotFoundException(name);
    }
    return result;
  }

  /**
   * Get a list of {@link Variable}s using their IDs.
   *
   * @param variableIds
   *          The variable IDs.
   * @return The Variable objects.
   * @throws VariableNotFoundException
   *           If any IDs aren't found.
   */
  public List<Variable> getInstrumentVariables(List<Long> variableIds)
    throws VariableNotFoundException {

    List<Variable> variables = new ArrayList<Variable>(variableIds.size());
    for (long id : variableIds) {
      Variable variable = instrumentVariables.get(id);
      if (null == variable) {
        throw new VariableNotFoundException(id);
      }
      variables.add(variable);
    }

    return variables;

  }

  public List<SensorType> getDiagnosticSensorTypes() {
    return getSensorTypes().stream().filter(t -> t.isDiagnostic())
      .collect(Collectors.toList());
  }
}
