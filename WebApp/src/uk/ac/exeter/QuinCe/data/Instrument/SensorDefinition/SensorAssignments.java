package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
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
public class SensorAssignments {

  /**
   * The assignments
   */
  private TreeMap<SensorType, Set<SensorAssignment>> sensorAssignments;

  /**
   * Indicators for required sensor types
   */
  private Map<SensorType, Boolean> requiredSensors;

  /**
   * Simple constructor - take in a map of Sensor Types and flags
   * indicating whether or not they are required
   * @param sensorTypes The
   */
  protected SensorAssignments(Map<SensorType, Boolean> sensorTypes) {
    this.requiredSensors = sensorTypes;

    sensorAssignments = new TreeMap<SensorType, Set<SensorAssignment>>();
    for (SensorType type : sensorTypes.keySet()) {
      sensorAssignments.put(type, new HashSet<SensorAssignment>());
    }
  }

  /**
   * Determines whether or not a given sensor type must have a column assigned.
   *
   * <p>
   *   This method checks the current sensor assignments, and determines whether
   *   or not a given sensor still needs to have an assignment made. This
   *   requires a number of checks:
   * </p>
   *
   * <ul>
   *   <li>
   *     If a primary sensor has been assigned, then no further assignment is needed
   *   </li>
   *   <li>
   *     If the sensor is part of a required group, and none of the sensors
   *     in that group have had a sensor assigned, then an assignment is needed.
   *     If any of the sensors in the group have been assigned, then no
   *     assignment is needed for this sensor.
   *   </li>
   *   <li>
   *     If the sensor is not part of a required group, but its required
   *     flag is set, then a sensor assignment is required.
   *   </li>
   *   <li>
   *     If another sensor type depends on this sensor type, then a
   *     sensor assignment is required. This determination may be based on
   *     the answer to {@link SensorType#getDependsQuestion()}.
   *   </li>
   * </ul>
   * @param sensorType The sensor type to be checked
   * @return {@code true} if a column must be assigned to the sensor; {@code false}
   * if no assignment is needed.
   * @throws SensorAssignmentException If the specified sensor type does not exist
   */
  public boolean isAssignmentRequired(SensorType sensorType) throws SensorAssignmentException {
    boolean required = false;

    if (!requiredSensors.containsKey(sensorType)) {
      throw new SensorAssignmentException("The specified sensor was not found");
    }

    if (!sensorAssigned(sensorType, true)) {
      if (isRequired(sensorType)) {
        if (!isSiblingAssigned(sensorType, true)) {
          required = true;
        }
      } else if (hasDependent(sensorType)) {
        required = true;
      }
    }

    return required;
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
   * @see SensorType#getDependsQuestion()
   */
  private boolean hasDependent(SensorType sensorType) {
    boolean result = false;

    for (SensorType testType : requiredSensors.keySet()) {
      if (result) {
        break;
      // A sensor can't depend on itself
      } else if (!testType.equals(sensorType)) {

        // See if the test sensor *may* depend on this sensor
        boolean potentiallyDependent = false;

        long dependsOn = testType.getDependsOn();

        // If the dependsOn is set...
        if (dependsOn != SensorType.NO_DEPENDS_ON) {

          // ...and it matches this sensor...
          if (dependsOn == sensorType.getId()) {

            // If the sensor is assigned then it might be dependent
            if (sensorAssigned(testType, false)) {
              potentiallyDependent = true;

            // If the sensor is required, and IS NOT part of a required group, then it
              // might be dependent
            } else if (isRequired(testType) && !testType.hasParent()) {
              potentiallyDependent = true;
            }
          }
        }

        if (potentiallyDependent) {

          // If there is no Depends Question, then we have a dependency
          if (!testType.hasDependsQuestion()) {
            result = true;
          } else {
            // Check the assignments and the answers to the Depends Question
            Set<SensorAssignment> testAssignments = sensorAssignments.get(testType);
            if (null != testAssignments) {
              for (SensorAssignment assignment : testAssignments) {
                if (assignment.getDependsQuestionAnswer()) {
                  result = true;
                  break;
                }
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Add a sensor assignment using the name of a sensor
   * @param sensorType The sensor type
   * @param assignment The assignment details
   * @throws SensorTypeNotFoundException If the named sensor does not exist
   */
  public void addAssignment(long sensorType, SensorAssignment assignment) throws SensorTypeNotFoundException {
    Set<SensorAssignment> assignments = sensorAssignments.get(getSensorType(sensorType));
    assignments.add(assignment);
  }

  /**
   * Get the {@link SensorType} object for a given sensor name
   * @param sensorId The sensor's database ID
   * @return The SensorType object
   * @throws SensorTypeNotFoundException If the sensor type does not exist
   */
  public SensorType getSensorType(long sensorId) throws SensorTypeNotFoundException {

    SensorType result = null;

    for (SensorType sensorType : requiredSensors.keySet()) {
      if (sensorType.getId() == sensorId) {
        result = sensorType;
        break;
      }
    }

    if (null == result) {
      throw new SensorTypeNotFoundException(sensorId);
    }

    return result;
  }

  /**
   * Determines whether or not a sensor type has been assigned to a column in
   * a file. Setting the {@code primary} flag will restrict the checks to only
   * looking at primary assignments; if the sensor has only been assigned as a fallback,
   * then it won't count. Setting {@code primary} to {@code false} will accept any
   * assignment.
   *
   * @param sensorType The sensor type
   * @param primaryOnly If the sensor must have been assigned as a primary sensor.
   * @return {@code true} if the sensor has been assigned; {@code false} if it has not
   */
  private boolean sensorAssigned(SensorType sensorType, boolean primaryOnly) {

    boolean assigned = false;

    Set<SensorAssignment> assignments = sensorAssignments.get(sensorType);
    if (null != assignments) {
      if (!primaryOnly) {
        assigned = (assignments.size() > 0);
      } else {
        for (SensorAssignment assignment : assignments) {
          if (assignment.isPrimary()) {
            assigned = true;
            break;
          }
        }
      }

    }

    return assigned;
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

    for (Map.Entry<SensorType, Set<SensorAssignment>> entry : sensorAssignments.entrySet()) {

      Set<SensorAssignment> assignments = entry.getValue();
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
    for (Map.Entry<SensorType, Set<SensorAssignment>> entry : sensorAssignments.entrySet()) {

      Set<SensorAssignment> assignments = entry.getValue();
      for (SensorAssignment assignment : assignments) {
        if (assignment.getDataFile().equalsIgnoreCase(fileDescription)) {
          assignments.remove(assignment);
        }
      }
    }
  }

  /**
   * Determines whether or not a Core Sensor has been assigned within a given file
   * @param file The file to be checked
   * @return {@code true} if the file has had a core sensor assigned; {@code false} if it has not
   * @throws DatabaseException If a database error occurs
   */
  public boolean coreSensorAssigned(String file) throws DatabaseException {
    boolean result = false;

    for (SensorType sensorType : requiredSensors.keySet()) {
      if (getSensorsConfiguration().isCoreSensor(ResourceManager.getInstance().getDBDataSource(), sensorType)) {
        for (SensorAssignment assignment : sensorAssignments.get(sensorType)) {
          if (assignment.getDataFile().equals(file)) {
            result = true;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Determine whether or not a sensor type is required.
   * If the sensor type has a parent, that is checked too
   *
   * @param sensorType The sensor type
   * @return {@code true} if the sensor type is required; {@code false} if it is not
   */
  private boolean isRequired(SensorType sensorType) {

    boolean required = requiredSensors.containsKey(sensorType);

    // See if the parent is required
    if (!required && sensorType.hasParent()) {
      required = isRequired(getSensorsConfiguration().getParent(sensorType));
    }

    return required;
  }

  /**
   * Determine whether or not the any siblings of the supplied SensorType
   * have been assigned. A sibling is defined as a SensorType with the same
   * parent type.
   *
   * Returns {@code false} if there are no siblings.
   *
   * @param sensorType The sensor whose siblings are to be checked
   * @param primaryOnly If {@code true}, only return {@code true} if a sibling has
   *                    had a primary sensor assigned
   * @return {@code true} if a sibling has been assigned; {@code false} if not
   */
  private boolean isSiblingAssigned(SensorType sensorType, boolean primaryOnly) {
    boolean siblingAssigned = false;

    List<SensorType> siblings = getSensorsConfiguration().getSiblings(sensorType);
    for (SensorType sibling : siblings) {
      if (sensorAssigned(sibling, primaryOnly)) {
        siblingAssigned = true;
        break;
      }
    }

    return siblingAssigned;
  }

  /**
   * Get the sensors configuration for the application
   * @return The sensors configuration
   */
  private SensorsConfiguration getSensorsConfiguration() {
    return ResourceManager.getInstance().getSensorsConfiguration();
  }
}
