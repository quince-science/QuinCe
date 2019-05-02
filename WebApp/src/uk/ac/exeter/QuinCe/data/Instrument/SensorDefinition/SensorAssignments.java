package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Map of sensors to their assignments in an instrument's data files.
 * <p>
 *   Although this class extends the Map interface, you should use the
 *   convenience methods defined here to change the contents of the Map.
 *   You can do it all yourself if you like, but caveat emptor.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class SensorAssignments extends TreeMap<SensorType, List<SensorAssignment>> {

  /**
   * The database IDs of the variables that this set of assignments is targeting
   */
  private List<Long> variableIDs;

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 7750520470025596480L;

  /**
   * Build the list of assignments based on the supplied list of variable IDs
   * @throws DatabaseException If any database lookups fail
   * @throws SensorConfigurationException If any variables can't be found
   * @throws SensorTypeNotFoundException If any internally listed SensorTypes
   *                                     don't exist
   */
  public SensorAssignments(Connection conn, List<Long> variableIDs)
    throws DatabaseException, SensorConfigurationException, SensorTypeNotFoundException {

    super();

    this.variableIDs = variableIDs;

    populateAssignments(conn);
  }

  /**
   * Make a SensorAssignments using a list of InstrumentVariable objects instead
   * of their IDs. This can't be a constructor because of type erasure.
   * @param conn A database connection
   * @param variables The instrument variables
   * @return The SensorAssignments object
   */
  public static SensorAssignments makeSensorAssignmentsFromVariables(
    Connection conn, List<InstrumentVariable> variables)
      throws DatabaseException, SensorConfigurationException, SensorTypeNotFoundException {

    List<Long> ids = new ArrayList<Long>(variables.size());
    for (InstrumentVariable variable : variables) {
      ids.add(variable.getId());
    }

    return new SensorAssignments(conn, ids);
  }

  /**
   * Initialise the data structure
   * @param conn A database connection
   * @throws DatabaseException If any database lookups fail
   * @throws SensorConfigurationException If any variables can't be found
   * @throws SensorTypeNotFoundException If any internally listed SensorTypes
   *                                     don't exist
   */
  private void populateAssignments(Connection conn)
    throws DatabaseException, SensorTypeNotFoundException, SensorConfigurationException {

    SensorsConfiguration sensorConfig =
      ResourceManager.getInstance().getSensorsConfiguration();

    for (SensorType type: sensorConfig.getNonCoreSensors(conn)) {
      put(type, new ArrayList<SensorAssignment>());

      // Add the Run Type if required
      if (type.hasInternalCalibration()) {
        put(SensorType.RUN_TYPE_SENSOR_TYPE, new ArrayList<SensorAssignment>());
      }
    }

    for (SensorType coreType : sensorConfig.getCoreSensors(variableIDs)) {
      put(coreType, new ArrayList<SensorAssignment>());

      // Add the Run Type if required
      if (coreType.hasInternalCalibration()) {
        put(SensorType.RUN_TYPE_SENSOR_TYPE, new ArrayList<SensorAssignment>());
      }
    }
  }

  /**
   * Determines whether or not a given SensorType must have a column assigned.
   * If a column has already been assigned then assignment is not required.
   *
   * @param sensorType The sensor type to be checked
   * @return {@code true} if a column must be assigned to the sensor; {@code false}
   * if no assignment is needed.
   * @throws SensorAssignmentException If the specified sensor type does not exist
   * @throws SensorConfigurationException If the internal configuration is invalid
   */
  public boolean isAssignmentRequired(SensorType sensorType) throws SensorAssignmentException, SensorConfigurationException {

    // Is the sensor required for the variables being measured?
    boolean required = getSensorConfig().requiredForVariables(sensorType, variableIDs);

    // Do other required sensors depend on this SensorType?
    if (hasAssignedDependents(sensorType)) {
      required = true;
    }

    // If it's required and primary not assigned, return true
    return (required && !isAssigned(sensorType, true, true));
  }

  /**
   * See if a given SensorType has any dependents, and whether those dependents
   * have been assigned
   * @param sensorType The SensorType
   * @param variableIDs The variables' database IDs
   * @return {@code true} if there are required dependents; {@code false} if not
   * @throws SensorConfigurationException
   */
  private boolean hasAssignedDependents(SensorType sensorType) throws SensorConfigurationException {

    boolean result = false;

    Set<SensorType> dependents = getDependents(sensorType);
    for (SensorType dependent : dependents) {
      if (isAssigned(dependent, false, false)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * See if a SensorType has been assigned
   * @param sensorType The sensor type
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */
  public boolean isAssigned(SensorType sensorType) {
    return isAssigned(sensorType, false, false);
  }
  
  /**
   * See if a SensorType has been assigned. Optionally only check for
   * primary assignments. Check children or siblings as appropriate.
   *
   * @param sensorType The SensorType
   * @param primaryOnly If only primary assignments are to be checked
   * @param includeRelations Indicates whether to look at relations when checking for assignment
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */
  private boolean isAssigned(SensorType sensorType, boolean primaryOnly, boolean includeRelations) {
    return isAssigned(sensorType, null, primaryOnly, includeRelations);
  }

  /**
   * See if a SensorType has been assigned. Optionally only check for
   * primary assignments. Check children or siblings as appropriate.
   *
   * @param sensorType The SensorType
   * @param primaryOnly If only primary assignments are to be checked
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */
  private boolean isAssigned(SensorType sensorType, String dataFileName, boolean primaryOnly, boolean includeRelations) {
    boolean assigned = false;

    SensorsConfiguration sensorConfig = getSensorConfig();

    if (includeRelations) {
      if (sensorConfig.isParent(sensorType)) {
        for (SensorType child : sensorConfig.getChildren(sensorType)) {
          if (checkAssignment(child, dataFileName, primaryOnly)) {
            assigned = true;
            break;
          }
        }
      } else if (sensorType.hasParent()) {
        for (SensorType child : sensorConfig.getChildren(
          sensorConfig.getParent(sensorType))) {

          if (checkAssignment(child, dataFileName, primaryOnly)) {
            assigned = true;
            break;
          }
        }
      } else {
        assigned = checkAssignment(sensorType, dataFileName, primaryOnly);
      }
    } else {
      assigned = checkAssignment(sensorType, dataFileName, primaryOnly);
    }


    return assigned;
  }

  /**
   * Determine whether or not a given column in a given file has already
   * been assigned
   * @param file The file
   * @param column The column
   * @return {@code true} if the file and column have been assigned;
   *         {@code false} if not
   */
  private boolean isAssigned(String file, int column) {
    boolean assigned = false;

    for (List<SensorAssignment> set : values()) {
      for (SensorAssignment assignment : set) {
        if (assignment.getDataFile().equals(file) &&
          assignment.getColumn() == column) {

          assigned = true;
          break;
        }
      }
    }

    return assigned;
  }

  /**
   * Check the internal data structure to see if a SensorType has been assigned.
   * Used by {{@link #isAssigned(SensorType, boolean)}
   *
   * @param sensorType The SensorType
   * @param primaryOnly If only primary assignments are to be checked
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */
  private boolean checkAssignment(SensorType sensorType, String dataFileName, boolean primaryOnly) {
    boolean assigned = false;

    List<SensorAssignment> assignments = get(sensorType);
    if (null != assignments) {
      for (SensorAssignment assignment : get(sensorType)) {
        if (null == dataFileName || assignment.getDataFile().equals(dataFileName)) {
          if (primaryOnly) {
            if (assignment.isPrimary()) {
              assigned = true;
              break;
            }
          } else {
            assigned = true;
            break;
          }
        }
      }
    }

    return assigned;
  }

  /**
   * Determines whether or not any of the sensor types in the
   * collection depends on the supplied sensor type.
   *
   * If the sensor type has a Depends Question, this is taken into account.
   *
   * The logic of this is quite nasty. Follow the code comments.
   *
   * @param sensorType The sensor type that other sensors may depend on
   * @return {@code true} if any other sensor types depend on the supplied sensor type; {@code false} if there are no dependents
   * @throws SensorConfigurationException If the internal configuration is invalid
   */
  public Set<SensorType> getDependents(SensorType sensorType)
    throws SensorConfigurationException {

    Set<SensorType> dependents = new HashSet<SensorType>();

    for (SensorType testType : getSensorConfig().getSensorTypes()) {
      // A sensor can't depend on itself
      if (!testType.equals(sensorType)) {

        if (testType.getDependsOn() == sensorType.getId()) {
          if (testType.hasDependsQuestion()) {

            // See if the Depends Question has been answered true
            for (SensorAssignment assignment : get(testType)) {
              if (assignment.getDependsQuestionAnswer()) {
                dependents.add(testType);
                break;
              }
            }
          } else {
            dependents.add(testType);
          }
        }
      }
    }

    return dependents;
  }
  
  /**
   * Get the Sensor Type that the given type depends on, if there is such a type.
   * If there's a Depends Question, one of the assignments must have answered
   * {@code true}. Returns {@code null} if the supplied Sensor Type does
   * not depend on another Sensor Type.
   * 
   * @param baseType The originating Sensor Type
   * @return The type that the baseType depends on
   * @throws SensorTypeNotFoundException If the sensor configuration is inconsistent
   */
  public SensorType getDependsOn(SensorType baseType) throws SensorTypeNotFoundException {
    SensorType result = null;
    
    if (baseType.dependsOnOtherType()) {

      SensorType dependsOnType = getSensorConfig().getSensorType(baseType.getDependsOn());
      
      if (!baseType.hasDependsQuestion()) {
        result = dependsOnType;
      } else {
        // See if the Depends On Question has been answered True in any
        // assignments
        for (SensorAssignment assignment : get(baseType)) {
          if (assignment.getDependsQuestionAnswer()) {
            result = dependsOnType;
            break;
          }
        }
      }
      
      
    }
    
    return result;
  }


  /**
   * Get the Sensors Configuration
   * @return The Sensors Configuration
   */
  private SensorsConfiguration getSensorConfig() {
    return ResourceManager.getInstance().getSensorsConfiguration();
  }

  /**
   * Add a sensor assignment using the name of a sensor type
   * @param sensorType The sensor type
   * @param assignment The assignment details
   * @throws SensorTypeNotFoundException If the named sensor does not exist
   * @throws SensorAssignmentException If the file column has already been assigned
   */
  public void addAssignment(String typeName, SensorAssignment assignment)
    throws SensorTypeNotFoundException, SensorAssignmentException {

    SensorType sensorType = getSensorConfig().getSensorType(typeName);
    addAssignment(sensorType.getId(), assignment);
  }

  /**
   * Add a sensor assignment using the ID of a sensor type
   * @param sensorTypeId The sensor type
   * @param assignment The assignment details
   * @throws SensorTypeNotFoundException If the named sensor does not exist
   * @throws SensorAssignmentException If the file column has already been assigned
   */
  public void addAssignment(long sensorTypeId, SensorAssignment assignment)
    throws SensorTypeNotFoundException, SensorAssignmentException {

    SensorType sensorType = getSensorConfig().getSensorType(sensorTypeId);

    if (getSensorConfig().isParent(sensorType)) {
      throw new SensorAssignmentException("Cannot assign parent sensor types");
    }

    if (isAssigned(assignment.getDataFile(), assignment.getColumn())) {
      throw new SensorAssignmentException("File '" + assignment.getDataFile() +
        "', column " + assignment.getColumn() + " has already been assigned");
    }

    List<SensorAssignment> assignments = get(sensorType);
    if (null == assignments) {
      // The sensor is not valid for this instrument, so it has not
      // been added to the assignments list
      throw new SensorAssignmentException(sensorType + " is not valid for this instrument");
    }
    assignments.add(assignment);
  }

  /**
   * De-assign a file/column from this set of assignments. The assignment doesn't have
   * to exist; the method will return a {@code boolean} indicating whether or not
   * a matching assignment was found and removed.
   * @param fileDescription The file description
   * @param columnIndex The column index
   * @return {@code true} if an assignment was removed; {@code false} if not
   */
  public boolean removeAssignment(String fileDescription, int columnIndex) {

    boolean assignmentRemoved = false;

    for (Map.Entry<SensorType, List<SensorAssignment>> entry : entrySet()) {

      List<SensorAssignment> assignments = entry.getValue();
      for (SensorAssignment assignment : assignments) {
        if (assignment.getDataFile().equalsIgnoreCase(fileDescription) && assignment.getColumn() == columnIndex) {
          assignments.remove(assignment);
          assignmentRemoved = true;
          break;
        }
      }

    }

    return assignmentRemoved;
  }

  /**
   * Remove all assignments from a given file
   * @param fileDescription The file description
   */
  public void removeFileAssignments(String fileDescription) {
    for (Map.Entry<SensorType, List<SensorAssignment>> entry : entrySet()) {

      Set<SensorAssignment> assignmentsToRemove = new HashSet<SensorAssignment>();

      List<SensorAssignment> assignments = entry.getValue();
      for (SensorAssignment assignment : assignments) {
        if (assignment.getDataFile().equalsIgnoreCase(fileDescription)) {
          assignmentsToRemove.add(assignment);
        }
      }

      assignments.removeAll(assignmentsToRemove);
    }
  }

  /**
   * Determines whether or not a Core Sensor has been assigned within any file
   * @param dataFileName The file to be checked
   * @param primaryOnly If {@code true}, only primary assignments are considered;
   *        secondary assignments will return {@code false}.
   * @return {@code true} if the file has had a core sensor assigned; {@code false} if it has not
   * @throws SensorConfigurationException If the sensor configuration is invalid
   */
  public boolean coreSensorAssigned(String dataFileName, boolean primaryOnly)
    throws SensorConfigurationException {

    boolean result = false;

    List<SensorType> coreSensors = getSensorConfig().getCoreSensors(variableIDs);
    for (SensorType coreType : coreSensors) {
      if (isAssigned(coreType, dataFileName, primaryOnly, true)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether or not the Run Type is required in a given file, and has
   * not yet been assigned.
   * @param dataFileName The data file to be checked
   * @return {@code true} if the run type is required; {@code false} if not.
   */
  public boolean runTypeRequired(String dataFileName) {
    boolean required = false;

    for (SensorType type : keySet()) {
      if (type.hasInternalCalibration()) {
        for (SensorAssignment assignment : get(type)) {
          if (assignment.getDataFile().equals(dataFileName)) {
            required = true;
            break;
          }
        }
      }
    }

    return required;
  }

  /**
   * Get the number of columns assigned to a given SensorType.
   * If the SensorType is not found, returns 0
   * @param sensorType The SensorType
   * @return The number of assigned columns
   */
  public int getAssignmentCount(SensorType sensorType) {
    int count = 0;

    if (containsKey(sensorType)) {
      count = get(sensorType).size();
    }

    return count;
  }

  /**
   * Get the sensor assignment with a given File Column database ID
   * @param columnId The file column database ID
   * @return The sensor assignment
   * @throws RecordNotFoundException If there is no assignment with the specified ID
   */
  public SensorType getSensorTypeForDBColumn(long columnId) throws RecordNotFoundException {
    SensorType result = null;

    if (columnId == FileDefinition.LONGITUDE_COLUMN_ID) {
      result = SensorType.LONGITUDE_SENSOR_TYPE;
    } else if (columnId == FileDefinition.LATITUDE_COLUMN_ID) {
      result = SensorType.LATITUDE_SENSOR_TYPE;
    } else {
      for (SensorType sensorType : keySet()) {
        for (SensorAssignment assignment : get(sensorType)) {
          if (assignment.getDatabaseId() == columnId) {
            result = sensorType;
            break;
          }
        }

        if (null != result) {
          break;
        }
      }
    }

    return result;
  }
}
