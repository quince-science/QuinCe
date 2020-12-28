package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Object to hold all the details of an instrument
 *
 * @author Steve Jones
 *
 */
public class Instrument {

  /**
   * Property name for the pre-flushing time
   */
  public static final String PROP_PRE_FLUSHING_TIME = "preFlushingTime";

  /**
   * Property name for the post-flushing time
   */
  public static final String PROP_POST_FLUSHING_TIME = "postFlushingTime";

  /**
   * Property name for the depth
   */
  public static final String PROP_DEPTH = "depth";

  /**
   * Property name for the fixed longitude
   */
  public static final String PROP_LONGITUDE = "longitude";

  /**
   * Property name for the fixed latitude
   */
  public static final String PROP_LATITUDE = "latitude";

  /**
   * The instrument's ID in the database
   */
  private long databaseID = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The ID of the owner of the instrument
   */
  private long ownerId;

  /**
   * The name of the instrument
   */
  private String name = null;

  /**
   * The instrument properties
   */
  private Properties properties = null;

  /**
   * The instrument's file format definitions
   */
  private InstrumentFileSet fileDefinitions = null;

  /**
   * The variables measured by this instrument
   */
  private List<Variable> variables = null;

  /**
   * The properties set for the variables measured by this instrument.
   */
  private Map<Variable, Properties> variableProperties = null;

  /**
   * The assignment of columns in data files to sensors
   */
  private SensorAssignments sensorAssignments = null;

  /**
   * Platform code
   */
  private String platformCode = null;

  /**
   * Indicates whether or not this instrument supplies near-real-time data
   *
   * At the time of writing, the NRT flag can only be set manually on the
   * database after the instrument is created. All calls within QuinCe set this
   * to false.
   */
  private boolean nrt = false;

  /**
   * Create an instrument from an existing database record.
   *
   * @param databaseId
   *          The instrument's database ID.
   * @param ownerId
   *          The ID of the instrument owner.
   * @param name
   *          The instrument's name.
   * @param fileDefinitions
   *          The data file definitions for the instrument.
   * @param variables
   *          The variables that the instrument measures.
   * @param sensorAssignments
   *          The sensors assigned to the instrument.
   * @param platformCode
   *          The instrument's identifier code.
   * @param nrt
   *          Indicates whether or not the instrument provides data in near real
   *          time.
   * @param properties
   *          The instrument's properties.
   */
  public Instrument(long databaseId, long ownerId, String name,
    InstrumentFileSet fileDefinitions, List<Variable> variables,
    Map<Variable, Properties> variableProperties,
    SensorAssignments sensorAssignments, String platformCode, boolean nrt,
    Properties properties) {

    this.databaseID = databaseId;
    this.ownerId = ownerId;
    this.name = name;
    this.fileDefinitions = fileDefinitions;
    this.variables = variables;
    this.variableProperties = variableProperties;
    this.sensorAssignments = sensorAssignments;
    this.setPlatformCode(platformCode);
    this.nrt = nrt;
    this.properties = properties;
  }

  /**
   * Create a new instrument with defined properties.
   *
   * @param owner
   *          The instrument owner.
   * @param name
   *          The instrument's name.
   * @param fileDefinitions
   *          The data file definitions for the instrument.
   * @param variables
   *          The variables that the instrument measures.
   * @param sensorAssignments
   *          The sensors assigned to the instrument.
   * @param platformCode
   *          The instrument's identifier code.
   * @param nrt
   *          Indicates whether or not the instrument provides data in near real
   *          time.
   * @param properties
   *          The instrument's properties.
   */
  public Instrument(User owner, String name, InstrumentFileSet fileDefinitions,
    List<Variable> variables, Map<Variable, Properties> variableProperties,
    SensorAssignments sensorAssignments, String platformCode, boolean nrt,
    Properties properties) {

    this.ownerId = owner.getDatabaseID();
    this.name = name;
    this.fileDefinitions = fileDefinitions;
    this.variables = variables;
    this.variableProperties = variableProperties;
    this.sensorAssignments = sensorAssignments;
    this.platformCode = platformCode;
    this.nrt = nrt;
    this.properties = properties;
  }

  /**
   * Create a new instrument with no properties defined.
   *
   * @param owner
   *          The instrument owner.
   * @param name
   *          The instrument's name.
   * @param fileDefinitions
   *          The data file definitions for the instrument.
   * @param variables
   *          The variables that the instrument measures.
   * @param sensorAssignments
   *          The sensors assigned to the instrument.
   * @param platformCode
   *          The instrument's identifier code.
   * @param nrt
   *          Indicates whether or not the instrument provides data in near real
   *          time.
   */
  public Instrument(User owner, String name, InstrumentFileSet fileDefinitions,
    List<Variable> variables, Map<Variable, Properties> variableProperties,
    SensorAssignments sensorAssignments, String platformCode, boolean nrt) {

    this.ownerId = owner.getDatabaseID();
    this.name = name;
    this.fileDefinitions = fileDefinitions;
    this.variables = variables;
    this.variableProperties = variableProperties;
    this.sensorAssignments = sensorAssignments;
    this.platformCode = platformCode;
    this.nrt = nrt;
    this.properties = new Properties();
  }

  /**
   * Validate that all required information for the Instrument is present
   *
   * @param checkDatabaseColumns
   *          Specifies whether or not database columns have been assigned and
   *          should be checked
   * @throws InstrumentException
   *           If the instrument is not valid
   */
  public void validate(boolean checkDatabaseColumns)
    throws InstrumentException {
    // TODO Write it!
  }

  /**
   * Returns the ID of the instrument in the database
   *
   * @return The ID of the instrument in the database
   */
  public long getDatabaseId() {
    return databaseID;
  }

  /**
   * Sets the ID of the instrument in the database
   *
   * @param databaseID
   *          The database ID
   */
  public void setDatabaseId(long databaseID) {
    this.databaseID = databaseID;
  }

  /**
   * Returns the database ID of the owner of the instrument
   *
   * @return The ID of the owner of the instrument
   */
  public long getOwnerId() {
    return ownerId;
  }

  /**
   * Get the instrument's name
   *
   * @return The name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the instrument's file definitions
   *
   * @return The file definitions
   */
  public InstrumentFileSet getFileDefinitions() {
    return fileDefinitions;
  }

  /**
   * Get the sensor assignments
   *
   * @return The sensor assignments
   */
  public SensorAssignments getSensorAssignments() {
    return sensorAssignments;
  }

  /**
   * @return the platformCode
   */
  public String getPlatformCode() {
    return platformCode;
  }

  /**
   * @param platformCode
   *          the platformCode to set
   */
  public void setPlatformCode(String platformCode) {
    this.platformCode = platformCode;
  }

  /**
   * Determine whether or not this instrument provides near-real-time data
   *
   * @return {@code true} if NRT data is provided; {@code false} if it is not
   */
  public boolean getNrt() {
    return nrt;
  }

  /**
   * Set the NRT flag
   *
   * @param nrt
   *          NRT flag
   */
  public void setNrt(boolean nrt) {
    this.nrt = nrt;
  }

  /**
   * Get the variables measured by this instrument
   *
   * @return
   */
  public List<Variable> getVariables() {
    return variables;
  }

  /**
   * Get the Run Type category for a given Run Type value
   *
   * @param runTypeValue
   *          The Run Type value
   * @return The Run Type category
   */
  public RunTypeCategory getRunTypeCategory(String runTypeValue) {
    // TODO Maybe we can build a lookup table for this, since the values
    // are fixed once the instrument's loaded from the database
    List<SensorAssignment> runTypeAssignments = getSensorAssignments()
      .get(SensorType.RUN_TYPE_SENSOR_TYPE);
    FileDefinition fileDef = getFileDefinitions()
      .get(runTypeAssignments.get(0).getDataFile());
    return fileDef.getRunTypes().getRunTypeCategory(runTypeValue);
  }

  /**
   * Get an InstrumentVaraible based on its ID
   *
   * @param variableId
   *          The variable ID
   * @return The InstrumentVariable
   * @throws InstrumentException
   *           If the variable is not found
   */
  public Variable getVariable(long variableId) throws InstrumentException {
    Variable result = null;

    for (Variable variable : variables) {
      if (variable.getId() == variableId) {
        result = variable;
        break;
      }
    }

    if (null == result) {
      throw new InstrumentException(
        "Variable with ID " + variableId + " is not part of this instrument");
    }
    return result;
  }

  /**
   * Get an InstrumentVaraible based on its name
   *
   * @param variableId
   *          The variable name
   * @return The InstrumentVariable, or null if the instrument does not have the
   *         variable defined.
   */
  public Variable getVariable(String name) {
    Variable result = null;

    for (Variable variable : variables) {
      if (variable.getName().equals(name)) {
        result = variable;
        break;
      }
    }

    return result;
  }

  /**
   * Get the run types that correspond to measurements for a given variable
   * Returns a map of Column ID to the run types, including all aliases
   *
   * @param variable
   * @return
   */
  public Map<Long, List<String>> getVariableRunTypes(Variable variable) {
    Map<Long, List<String>> result = new HashMap<Long, List<String>>();

    for (FileDefinition fileDefinition : fileDefinitions) {
      RunTypeAssignments runTypeAssignments = fileDefinition.getRunTypes();
      if (null != runTypeAssignments) {
        for (String runType : runTypeAssignments.keySet()) {
          RunTypeAssignment assignment = runTypeAssignments.get(runType);

          while (assignment.isAlias()) {
            assignment = runTypeAssignments.get(assignment.getAliasTo());
          }

          if (assignment.getCategory().isVariable()) {
            if (!result.containsKey(assignment.getCategoryCode())) {
              result.put(assignment.getCategoryCode(), new ArrayList<String>());
            }

            result.get(assignment.getCategoryCode()).add(runType);
          }
        }
      }
    }

    return result;
  }

  /**
   * Get the list of variables that require the specified sensor type. If no
   * variables require it, the list is empty.
   *
   * @param sensorType
   * @return
   */
  public List<Variable> getSensorVariables(SensorType sensorType) {
    List<Variable> result = new ArrayList<Variable>(variables.size());

    for (Variable variable : variables) {
      List<SensorType> variableSensorTypes = variable.getAllSensorTypes(false);
      for (SensorType type : variableSensorTypes) {
        if (type.equals(sensorType)) {
          result.add(variable);
          break;
        } else if (type.getDependsOn() == sensorType.getId()) {
          result.add(variable);
          break;
        }
      }
    }

    return result;
  }

  public List<String> getRunTypes(long assignmentType) {

    List<String> result = new ArrayList<String>();

    for (FileDefinition fileDef : fileDefinitions) {
      if (null != fileDef.getRunTypes()) {
        for (Map.Entry<String, RunTypeAssignment> entry : fileDef.getRunTypes()
          .entrySet()) {
          if (entry.getValue().getCategoryCode() == assignmentType) {
            result.add(entry.getKey());
          }
        }
      }
    }

    Collections.sort(result);
    return result;
  }

  public List<String> getMeasurementRunTypes() {
    List<String> result = new ArrayList<String>();

    for (Variable variable : variables) {
      Map<Long, List<String>> variableRunTypes = getVariableRunTypes(variable);
      if (variableRunTypes.size() > 0
        && variableRunTypes.containsKey(variable.getId())) {
        result.addAll(variableRunTypes.get(variable.getId()));
      }
    }

    return result;
  }

  public List<String> getInternalCalibrationRunTypes() {
    // Get the list of run type values that indicate measurements
    List<String> result = new ArrayList<String>(0);

    for (FileDefinition fileDef : fileDefinitions) {
      RunTypeAssignments assignments = fileDef.getRunTypes();
      if (null != assignments) {

        for (Map.Entry<String, RunTypeAssignment> assignment : assignments
          .entrySet()) {

          // Follow aliases
          RunTypeAssignment checkAssignment = assignment.getValue();
          if (checkAssignment.isAlias()) {
            checkAssignment = fileDef.getRunTypes()
              .get(checkAssignment.getAliasTo());
          }

          if (checkAssignment.getCategory()
            .equals(RunTypeCategory.INTERNAL_CALIBRATION)) {
            result.add(assignment.getKey());
          }
        }
      }
    }

    return result;
  }

  public boolean hasInternalCalibrations() {
    boolean result = false;

    for (Variable variable : variables) {
      if (variable.hasInternalCalibrations()) {
        result = true;
        break;
      }
    }

    return result;
  }

  public boolean hasRunTypes() {
    return sensorAssignments.getRunTypeColumnIDs().size() > 0;
  }

  /**
   * Determine whether a Measurement object has the correct run type for data
   * reduction to be performed
   *
   * @param runType
   *          The run type
   * @return
   */
  public boolean isMeasurementRunType(String runType) {
    boolean result = false;

    if (!hasInternalCalibrations()) {
      result = true;
    } else {
      result = getRunTypeCategory(runType).isMeasurementType();
    }

    return result;
  }

  /**
   * Determine whether a Measurement object has the correct run type for data
   * reduction to be performed
   *
   * @param runType
   *          The run type
   * @return
   */
  public boolean isRunTypeForVariable(Variable variable, String runType) {
    boolean result = false;

    if (!hasInternalCalibrations()) {
      result = true;
    } else {
      result = getRunTypeCategory(runType).getType() == variable.getId();
    }

    return result;
  }

  /**
   * Set an property on the instrument.
   *
   * @param name
   *          The property name.
   * @param value
   *          The property value.
   */
  public void setProperty(String name, String value) {
    properties.setProperty(name, value);
  }

  /**
   * Set an integer property on the instrument.
   *
   * @param name
   *          The property name.
   * @param value
   *          The property value.
   */
  public void setProperty(String name, int value) {
    properties.setProperty(name, String.valueOf(value));
  }

  /**
   * Set a double property on the instrument.
   *
   * @param name
   *          The property name.
   * @param value
   *          The property value.
   */
  public void setProperty(String name, double value) {
    properties.setProperty(name, String.valueOf(value));
  }

  /**
   * Get all the instrument's properties.
   *
   * @return The properties.
   */
  public Properties getProperties() {
    return properties;
  }

  /**
   * Get an instrument property.
   *
   * <p>
   * Returns {@code null} if the property does not exist.
   * </p>
   *
   * @param name
   *          The property name.
   * @return The property value.
   */
  public String getProperty(String name) {
    return properties.getProperty(name);
  }

  /**
   * Get an instrument property as an {@link Integer}.
   *
   * <p>
   * Returns {@code null} if the property does not exist or its value cannot be
   * parsed.
   * </p>
   *
   * @param name
   *          The property name.
   * @return The property value.
   */
  public Integer getIntProperty(String name) {
    Integer result = null;

    if (properties.containsKey(name)) {
      try {
        result = Integer.parseInt(properties.getProperty(name));
      } catch (NumberFormatException e) {
        // Swallow the exception so we return null
      }
    }

    return result;
  }

  /**
   * Get an instrument property as an {@link Double}.
   *
   * <p>
   * Returns {@code null} if the property does not exist or its value cannot be
   * parsed.
   * </p>
   *
   * @param name
   *          The property name.
   * @return The property value.
   */
  public Double getDoubleProperty(String name) {
    Double result = null;

    if (properties.containsKey(name)) {
      try {
        result = Double.parseDouble(properties.getProperty(name));
      } catch (NumberFormatException e) {
        // Swallow the exception so we return null
      }
    }

    return result;
  }

  public Properties getVariableProperties(Variable variable) {
    return variableProperties.get(variable);
  }

  public Map<Variable, Properties> getAllVariableProperties() {
    return variableProperties;
  }
}
