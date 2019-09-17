package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Object to hold all the details of an instrument
 *
 * @author Steve Jones
 *
 */
public class Instrument {

  ////////////// *** FIELDS *** ///////////////

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
   * The instrument's file format definitions
   */
  private InstrumentFileSet fileDefinitions = null;

  /**
   * The variables measured by this instrument
   */
  private List<InstrumentVariable> variables = null;

  /**
   * The assignment of columns in data files to sensors
   */
  private SensorAssignments sensorAssignments = null;

  /**
   * The flushing time at the start of each run
   */
  private int preFlushingTime = 0;

  /**
   * The flushing time at the end of each run
   */
  private int postFlushingTime = 0;

  /**
   * The depth of the instrument or its intake
   */
  private int depth = 0;

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
   * Constructor for a complete instrument that's already in the database
   *
   * @param databaseId
   *          The instrument's database ID
   * @param ownerId
   *          The instrument owner's database ID
   * @param name
   *          The instrument name
   * @param fileDefinitions
   *          The file format definitions
   * @param variables
   *          The variables measured by this instrument
   * @param sensorAssignments
   *          The sensor assignments
   * @param preFlushingTime
   *          The pre-flushing time
   * @param postFlushingTime
   *          The post-flushing time
   * @param minimumWaterFlow
   *          The minimum water flow
   * @param averagingMode
   *          The averaging mode
   * @param platformCode
   *          The platform code
   * @param nrt
   *          Near real time flag
   */
  public Instrument(long databaseId, long ownerId, String name,
    InstrumentFileSet fileDefinitions, List<InstrumentVariable> variables,
    SensorAssignments sensorAssignments, int preFlushingTime,
    int postFlushingTime, int depth, String platformCode, boolean nrt) {

    this.databaseID = databaseId;
    this.ownerId = ownerId;
    this.name = name;
    this.fileDefinitions = fileDefinitions;
    this.variables = variables;
    this.sensorAssignments = sensorAssignments;
    this.preFlushingTime = preFlushingTime;
    this.postFlushingTime = postFlushingTime;
    this.depth = depth;
    this.setPlatformCode(platformCode);
    this.nrt = nrt;

    // TODO Validate averaging mode
  }

  /**
   * Constructor for a complete instrument with no database ID
   *
   * @param owner
   *          The instrument's owner
   * @param name
   *          The instrument name
   * @param fileDefinitions
   *          The file format definitions
   * @param variables
   *          The variables measured by this instrument
   * @param sensorAssignments
   *          The sensor assignments
   * @param preFlushingTime
   *          The pre-flushing time
   * @param postFlushingTime
   *          The post-flushing time
   * @param minimumWaterFlow
   *          The minimum water flow
   * @param averagingMode
   *          The averaging mode
   * @param platformCode
   *          The platform code
   */
  public Instrument(User owner, String name, InstrumentFileSet fileDefinitions,
    List<InstrumentVariable> variables, SensorAssignments sensorAssignments,
    int preFlushingTime, int postFlushingTime, int depth, String platformCode,
    boolean nrt) {

    this.ownerId = owner.getDatabaseID();
    this.name = name;
    this.fileDefinitions = fileDefinitions;
    this.variables = variables;
    this.sensorAssignments = sensorAssignments;
    this.preFlushingTime = preFlushingTime;
    this.postFlushingTime = postFlushingTime;
    this.depth = depth;
    this.platformCode = platformCode;
    this.nrt = nrt;

    // TODO Validate averaging mode
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
   * Returns the pre-flushing time
   *
   * @return The pre-flushing time
   */
  public int getPreFlushingTime() {
    return preFlushingTime;
  }

  /**
   * Sets the pre-flushing time
   *
   * @param preFlushingTime
   *          The pre-flushing time
   */
  public void setPreFlushingTime(int preFlushingTime) {
    this.preFlushingTime = preFlushingTime;
  }

  /**
   * Returns the post-flushing time
   *
   * @return The post-flushing time
   */
  public int getPostFlushingTime() {
    return postFlushingTime;
  }

  /**
   * Sets the post-flushing time
   *
   * @param postFlushingTime
   *          The post-flushing time
   */
  public void setPostFlushingTime(int postFlushingTime) {
    this.postFlushingTime = postFlushingTime;
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
  public List<InstrumentVariable> getVariables() {
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
  public InstrumentVariable getVariable(long variableId)
    throws InstrumentException {
    InstrumentVariable result = null;

    for (InstrumentVariable variable : variables) {
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
   * @return The InstrumentVariable
   * @throws InstrumentException
   *           If the variable is not found
   */
  public InstrumentVariable getVariable(String name)
    throws InstrumentException {
    InstrumentVariable result = null;

    for (InstrumentVariable variable : variables) {
      if (variable.getName().equals(name)) {
        result = variable;
        break;
      }
    }

    if (null == result) {
      throw new InstrumentException(
        "Variable with name " + name + " is not part of this instrument");
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
  public Map<Long, List<String>> getVariableRunTypes(
    InstrumentVariable variable) {
    Map<Long, List<String>> result = new HashMap<Long, List<String>>();

    for (FileDefinition fileDefinition : fileDefinitions) {
      RunTypeAssignments runTypeAssignments = fileDefinition.getRunTypes();
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

    return result;
  }

  /**
   * Get the list of variables that require the specified sensor type. If no
   * variables require it, the list is empty.
   *
   * @param sensorType
   * @return
   */
  public List<InstrumentVariable> getSensorVariables(SensorType sensorType) {
    List<InstrumentVariable> result = new ArrayList<InstrumentVariable>(
      variables.size());

    for (InstrumentVariable variable : variables) {
      List<SensorType> variableSensorTypes = variable.getAllSensorTypes();
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
      for (Map.Entry<String, RunTypeAssignment> entry : fileDef.getRunTypes()
        .entrySet()) {
        if (entry.getValue().getCategoryCode() == assignmentType) {
          result.add(entry.getKey());
        }
      }
    }

    Collections.sort(result);
    return result;
  }

  public List<String> getMeasurementRunTypes() {
    List<String> result = new ArrayList<String>();

    for (InstrumentVariable variable : variables) {
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

      for (Map.Entry<String, RunTypeAssignment> assignment : assignments
        .entrySet()) {
        if (assignment.getValue().getCategory()
          .equals(RunTypeCategory.INTERNAL_CALIBRATION)) {
          result.add(assignment.getKey());
        }
      }
    }

    return result;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }
}
