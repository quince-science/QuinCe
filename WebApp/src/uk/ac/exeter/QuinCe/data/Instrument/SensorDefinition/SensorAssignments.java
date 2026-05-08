package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.PositionSpecification;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.NewInstrumentBean;
import uk.ac.exeter.QuinCe.web.Instrument.NewInstrument.NewInstrumentFileSet;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Map of sensors to their assignments in an instrument's data files.
 *
 * <p>
 * Although this class extends the Map interface, you should use the convenience
 * methods defined here to change the contents of the Map. You can do it all
 * yourself if you like, but you'll need to perform all validity checks yourself
 * too. Good luck...
 * </p>
 */
@SuppressWarnings("serial")
public class SensorAssignments
  extends TreeMap<SensorType, TreeSet<SensorAssignment>> {

  private static final int FULLY_ASSIGNED = 1;

  private static final int ASSIGNED_WITHOUT_DEPENDENT = -1;

  private static final int NOT_ASSIGNED = 0;

  /**
   * We can override whether a {@link SensorType} must be assigned by putting an
   * entry in this map.
   *
   * @see #isAssignmentRequired(SensorType, Map)
   */
  protected HashMap<SensorType, Boolean> forcedAssignmentRequired = new HashMap<SensorType, Boolean>();

  /**
   * The database IDs of the variables that this set of assignments is targeting
   */
  private List<Long> variableIDs;

  private HashMap<Long, SensorType> dbColumnSensorTypeCache = new HashMap<Long, SensorType>();

  /**
   * Initialise the assignments for the specified list of {@link Variable}s,
   * using a database {@link Connection}.
   *
   * @param conn
   *          A database connection
   * @param variableIDs
   *          The variable IDs
   * @throws DatabaseException
   *           If any database lookups fail
   * @throws SensorConfigurationException
   *           If any variables can't be found
   * @throws SensorTypeNotFoundException
   *           If any internally listed SensorTypes don't exist
   */
  protected SensorAssignments(Connection conn, List<Long> variableIDs)
    throws DatabaseException, SensorConfigurationException,
    SensorTypeNotFoundException {

    super();
    this.variableIDs = variableIDs;
    populateAssignments(conn);
  }

  /**
   * Initialise the assignments for the specified list of {@link Variable}s
   * using a {@link DataSource}.
   *
   * @param dataSource
   *          A data source
   * @param variableIDs
   *          The variable IDs
   * @throws DatabaseException
   *           If any database lookups fail
   * @throws SensorConfigurationException
   *           If any variables can't be found
   * @throws SensorTypeNotFoundException
   *           If any internally listed SensorTypes don't exist
   */
  protected SensorAssignments(DataSource dataSource, List<Long> variableIDs)
    throws SensorTypeNotFoundException, SensorConfigurationException,
    DatabaseException {
    super();
    this.variableIDs = variableIDs;
    populateAssignments(dataSource);
  }

  /**
   * Make a SensorAssignments using a list of InstrumentVariable objects instead
   * of their IDs. This can't be a constructor because of type erasure.
   *
   * @param conn
   *          A database connection
   * @param variables
   *          The instrument variables
   * @return The SensorAssignments object
   * @throws DatabaseException
   *           If any database lookups fail
   * @throws SensorConfigurationException
   *           If any variables can't be found
   * @throws SensorTypeNotFoundException
   *           If any internally listed SensorTypes don't exist
   */
  public static SensorAssignments makeSensorAssignmentsFromVariables(
    Connection conn, List<Variable> variables) throws DatabaseException,
    SensorConfigurationException, SensorTypeNotFoundException {

    List<Long> ids = new ArrayList<Long>(variables.size());
    for (Variable variable : variables) {
      ids.add(variable.getId());
    }

    return new SensorAssignments(conn, ids);
  }

  /**
   * Initialise the data structure. Passes through to
   * {@link #populateAssignments(Connection)}.
   *
   * @param dataSource
   *          A data source
   * @throws DatabaseException
   *           If any database lookups fail
   * @throws SensorConfigurationException
   *           If any variables can't be found
   * @throws SensorTypeNotFoundException
   *           If any internally listed SensorTypes don't exist
   */
  private void populateAssignments(DataSource dataSource)
    throws SensorTypeNotFoundException, SensorConfigurationException,
    DatabaseException {

    try (Connection conn = dataSource.getConnection()) {
      populateAssignments(conn);
    } catch (SQLException e) {
      throw new DatabaseException("Error setting up sensor assignments", e);
    }
  }

  /**
   * Initialise the data structure
   *
   * @param conn
   *          A database connection
   * @throws DatabaseException
   *           If any database lookups fail
   * @throws SensorConfigurationException
   *           If any variables can't be found
   * @throws SensorTypeNotFoundException
   *           If any internally listed SensorTypes don't exist
   */
  private void populateAssignments(Connection conn) throws DatabaseException,
    SensorTypeNotFoundException, SensorConfigurationException {

    for (SensorType type : getSensorConfig().getNonCoreSensors(conn)) {
      put(type, new TreeSet<SensorAssignment>());

      // Add the Run Type if required
      if (type.hasInternalCalibration()) {
        put(SensorType.RUN_TYPE_SENSOR_TYPE, new TreeSet<SensorAssignment>());
      }
    }

    for (SensorType coreType : getSensorConfig().getCoreSensors(variableIDs)) {
      if (null != coreType) {
        put(coreType, new TreeSet<SensorAssignment>());

        // Add the Run Type if required
        if (coreType.hasInternalCalibration()) {
          put(SensorType.RUN_TYPE_SENSOR_TYPE, new TreeSet<SensorAssignment>());
        }
      }
    }
  }

  /**
   * Determines whether or not a given SensorType must have a column assigned.
   * If a column has already been assigned then assignment is not required.
   *
   * @param sensorType
   *          The sensor type to be checked
   * @return {@code true} if a column must be assigned to the sensor;
   *         {@code false} if no assignment is needed.
   * @throws SensorAssignmentException
   *           If the specified sensor type does not exist
   * @throws SensorConfigurationException
   *           If the internal configuration is invalid
   */
  public boolean isAssignmentRequired(SensorType sensorType,
    Map<Long, VariableAttributes> varAttributes)
    throws SensorAssignmentException, SensorConfigurationException {

    boolean result;

    if (forcedAssignmentRequired.containsKey(sensorType)) {
      result = forcedAssignmentRequired.get(sensorType)
        && !isAssigned(sensorType, true, true);
    } else {

      if (sensorType.equals(SensorType.RUN_TYPE_SENSOR_TYPE)) {
        result = isRunTypeSensorTypeRequired();
      } else {
        // Is the sensor required for the variables being measured?
        boolean required = getSensorConfig().requiredForVariables(sensorType,
          variableIDs, varAttributes);

        // Do other required sensors depend on this SensorType?
        if (hasAssignedDependents(sensorType)) {
          required = true;
        }

        // If it's required and primary not assigned, return true
        result = (required && !isAssigned(sensorType, true, true));
      }
    }

    return result;
  }

  /**
   * Determine whether or not the special
   * {@link SensorType#RUN_TYPE_SENSOR_TYPE} is required.
   *
   * @return {@code true} if the {@link SensorType#RUN_TYPE_SENSOR_TYPE} is
   *         required; {@code false} otherwise.
   * @throws SensorConfigurationException
   * @throws VariableNotFoundException
   */
  private boolean isRunTypeSensorTypeRequired()
    throws SensorConfigurationException {
    boolean result = false;

    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    if (!isAssigned(SensorType.RUN_TYPE_SENSOR_TYPE)) {

      try {
        for (Long id : variableIDs) {
          if (sensorConfig.getInstrumentVariable(id).requiresRunType()) {
            result = true;
            break;
          }
        }
      } catch (VariableNotFoundException e) {
        throw new SensorConfigurationException("Error getting run type info",
          e);
      }

      if (!result) {
        for (SensorType sensorType : keySet()) {
          if (sensorType.hasInternalCalibration()) {
            result = true;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * See if a given SensorType has any dependents, and whether those dependents
   * have been assigned
   *
   * @param sensorType
   *          The SensorType
   * @return {@code true} if there are required dependents; {@code false} if not
   * @throws SensorConfigurationException
   *           If the sensorType's dependents cannot be established.
   */
  private boolean hasAssignedDependents(SensorType sensorType)
    throws SensorConfigurationException {

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
   *
   * @param sensorType
   *          The sensor type
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */
  public boolean isAssigned(SensorType sensorType) {
    return isAssigned(sensorType, false, false);
  }

  public boolean isAssigned(String... sensorTypeNames)
    throws SensorTypeNotFoundException {
    boolean assigned = true;

    for (String sensorTypeName : sensorTypeNames) {
      if (!isAssigned(ResourceManager.getInstance().getSensorsConfiguration()
        .getSensorType(sensorTypeName))) {
        assigned = false;
        break;
      }
    }

    return assigned;
  }

  /**
   * See if a SensorType has been assigned. Optionally only check for primary
   * assignments. Check children or siblings as appropriate.
   *
   * @param sensorType
   *          The SensorType
   * @param primaryOnly
   *          If only primary assignments are to be checked
   * @param includeRelations
   *          Indicates whether to look at relations when checking for
   *          assignment
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */
  public boolean isAssigned(SensorType sensorType, boolean primaryOnly,
    boolean includeRelations) {
    return isAssigned(sensorType, null, primaryOnly, includeRelations);
  }

  /**
   * See if a SensorType has been assigned. Optionally only check for primary
   * assignments. Check children or siblings as appropriate.
   *
   * @param sensorType
   *          The SensorType
   * @param primaryOnly
   *          If only primary assignments are to be checked
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */

  /**
   * Determine whether a {@link SensorType} has been assigned. Checking can
   * optionally include siblings/child sensor types. Optionally only check
   * whether a primary sensor have been assigned.
   *
   * @param sensorType
   *          The {@link SensorType} to check.
   * @param dataFileName
   *          The file whose assignments are to be checked.
   * @param primaryOnly
   *          Indicates whether or not only primary assignments should be
   *          included.
   * @param includeRelations
   *          Indicates whether child or sibling {@link SensorType}s should be
   *          included.
   * @return {@code true} if the {@link SensorType} is assigned; {@code false}
   *         if not.
   */
  private boolean isAssigned(SensorType sensorType, String dataFileName,
    boolean primaryOnly, boolean includeRelations) {
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
        for (SensorType child : sensorConfig
          .getChildren(sensorConfig.getParent(sensorType))) {

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
   * Determine whether or not a given column in a given file has already been
   * assigned
   *
   * @param file
   *          The file
   * @param column
   *          The column
   * @return {@code true} if the file and column have been assigned;
   *         {@code false} if not
   */
  private boolean isAssigned(SensorType sensorType, String file, int column) {
    boolean assigned = false;

    if (null != get(sensorType)) {
      for (SensorAssignment assignment : get(sensorType)) {
        if (assignment.getDataFile().equals(file)
          && assignment.getColumn() == column) {

          assigned = true;
          break;
        }
      }
    }

    return assigned;
  }

  /**
   * Check the internal data structure to see if a SensorType has been assigned.
   * Used by the {@code isAssigned} methods.
   *
   * @param sensorType
   *          The {@link SensorType} to be checked.
   * @param dataFileName
   *          The data file whose assignments are to be checked.
   * @param primaryOnly
   *          If only primary assignments are to be checked.
   * @return {@code true} if the sensor has been assigned; {@code false} if not
   */
  private boolean checkAssignment(SensorType sensorType, String dataFileName,
    boolean primaryOnly) {
    boolean assigned = false;

    TreeSet<SensorAssignment> assignments = get(sensorType);
    if (null != assignments) {
      for (SensorAssignment assignment : get(sensorType)) {
        if (null == dataFileName
          || assignment.getDataFile().equals(dataFileName)) {
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
   * Determines whether or not any of the sensor types in the collection depends
   * on the supplied sensor type. If the sensor type has a Depends Question,
   * this is taken into account. The logic of this is quite nasty. Follow the
   * code comments.
   *
   * @param sensorType
   *          The sensor type that other sensors may depend on
   * @return {@code true} if any other sensor types depend on the supplied
   *         sensor type; {@code false} if there are no dependents
   * @throws SensorConfigurationException
   *           If the internal configuration is invalid
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
            if (null != get(testType)) {
              for (SensorAssignment assignment : get(testType)) {
                if (assignment.getDependsQuestionAnswer()) {
                  dependents.add(testType);
                  break;
                }
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
   * Get the Sensor Type that the given type depends on, if there is such a
   * type. If there's a Depends Question, one of the assignments must have
   * answered {@code true}. Returns {@code null} if the supplied Sensor Type
   * does not depend on another Sensor Type.
   *
   * @param baseType
   *          The originating Sensor Type
   * @return The type that the baseType depends on
   * @throws SensorTypeNotFoundException
   *           If the sensor configuration is inconsistent
   */
  public SensorType getDependsOn(SensorType baseType)
    throws SensorTypeNotFoundException {
    SensorType result = null;

    if (baseType.dependsOnOtherType()) {

      SensorType dependsOnType = getSensorConfig()
        .getSensorType(baseType.getDependsOn());

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
   *
   * @return The Sensors Configuration
   */
  private SensorsConfiguration getSensorConfig() {
    return ResourceManager.getInstance().getSensorsConfiguration();
  }

  public void addAssignment(SensorAssignment assignment)
    throws SensorTypeNotFoundException, SensorAssignmentException {
    addAssignment(assignment, false);
  }

  /**
   * Add a sensor assignment using the ID of a sensor type
   *
   * @param assignment
   *          The assignment details
   * @param allowDuplicates
   *          Some historical instruments have duplicate sensor names which
   *          can't be changed, so we have an option to allow these through.
   *          Otherwise this should be set to {@code false}.
   * @throws SensorTypeNotFoundException
   *           If the named sensor does not exist
   * @throws SensorAssignmentException
   *           If the file column has already been assigned
   */
  public void addAssignment(SensorAssignment assignment,
    boolean allowDuplicates)
    throws SensorTypeNotFoundException, SensorAssignmentException {

    if (getSensorConfig().isParent(assignment.getSensorType())) {
      throw new SensorAssignmentException("Cannot assign parent sensor types");
    }

    if (isAssigned(assignment.getSensorType(), assignment.getDataFile(),
      assignment.getColumn())) {
      throw new SensorAssignmentException("File '" + assignment.getDataFile()
        + "', column " + assignment.getColumn() + " has already been assigned");
    }

    if (!allowDuplicates && getAllSensorNames()
      .contains(assignment.getSensorName().toLowerCase())) {
      throw new SensorAssignmentException("Sensor name "
        + assignment.getSensorName() + " has already been assigned");
    }

    TreeSet<SensorAssignment> assignments = get(assignment.getSensorType());
    if (null == assignments) {
      // The sensor is not valid for this instrument, so it has not
      // been added to the assignments list
      throw new SensorAssignmentException(
        assignment.getSensorType().getShortName()
          + " is not valid for this instrument");
    }
    assignments.add(assignment);
  }

  /**
   * Remove the assignment of a {@link SensorType} from a specified file/column.
   * The assignment doesn't have to exist; the method will return the remove
   * {@link SensorAssignment} object.
   *
   * @param sensorType
   *          The {@link} SensorType to be unassigned.
   * @param fileDescription
   *          The file from which the assignment is to be removed.
   * @param columnIndex
   *          The column index.
   * @return The removed assignment, or {@code null} if there was no assignment
   *         to remove.
   */
  public SensorAssignment removeAssignment(SensorType sensorType,
    String fileDescription, int columnIndex) {

    SensorAssignment removed = null;

    for (SensorAssignment assignment : get(sensorType)) {
      if (assignment.getDataFile().equalsIgnoreCase(fileDescription)
        && assignment.getColumn() == columnIndex) {
        get(sensorType).remove(assignment);
        removed = assignment;
        break;
      }
    }

    return removed;
  }

  /**
   * Remove all assignments from a given file
   *
   * @param fileDescription
   *          The file description
   */
  public void removeFileAssignments(String fileDescription) {
    for (Map.Entry<SensorType, TreeSet<SensorAssignment>> entry : entrySet()) {

      Set<SensorAssignment> assignmentsToRemove = new HashSet<SensorAssignment>();

      TreeSet<SensorAssignment> assignments = entry.getValue();
      for (SensorAssignment assignment : assignments) {
        if (assignment.getDataFile().equalsIgnoreCase(fileDescription)) {
          assignmentsToRemove.add(assignment);
        }
      }

      assignments.removeAll(assignmentsToRemove);
    }
  }

  public Set<SensorAssignment> getFileAssignments(String fileDescription) {

    Set<SensorAssignment> result = new HashSet<SensorAssignment>();

    for (Map.Entry<SensorType, TreeSet<SensorAssignment>> entry : entrySet()) {

      Set<SensorAssignment> foundAssignments = new HashSet<SensorAssignment>();

      TreeSet<SensorAssignment> assignments = entry.getValue();
      for (SensorAssignment assignment : assignments) {
        if (assignment.getDataFile().equalsIgnoreCase(fileDescription)) {
          foundAssignments.add(assignment);
        }
      }

      result.addAll(foundAssignments);
    }
    return result;
  }

  /**
   * Determine whether or not the specified file has had any columns assigned in
   * this set of assignments.
   *
   * <p>
   * Date/time columns are not considered to be assignments for the purposes of
   * this check.
   * </p>
   *
   * @param fileDescription
   *          The file description.
   * @return {@code true} if at least one column from the file has been
   *         assigned; {@code false} otherwise.
   */
  public boolean isFileAssigned(String fileDescription) {
    boolean result = false;

    search: for (Map.Entry<SensorType, TreeSet<SensorAssignment>> entry : entrySet()) {
      TreeSet<SensorAssignment> assignments = entry.getValue();
      for (SensorAssignment assignment : assignments) {
        if (assignment.getDataFile().equalsIgnoreCase(fileDescription)) {
          result = true;
          break search;
        }
      }
    }

    return result;
  }

  /**
   * Determines whether or not a Core Sensor has been assigned within any file
   *
   * @param dataFileName
   *          The file to be checked
   * @param primaryOnly
   *          If {@code true}, only primary assignments are considered;
   *          secondary assignments will return {@code false}.
   * @return {@code true} if the file has had a core sensor assigned;
   *         {@code false} if it has not
   * @throws SensorConfigurationException
   *           If the sensor configuration is invalid
   */
  public boolean coreSensorAssigned(String dataFileName, boolean primaryOnly)
    throws SensorConfigurationException {

    boolean result = false;

    List<SensorType> coreSensors = getSensorConfig()
      .getCoreSensors(variableIDs);
    for (SensorType coreType : coreSensors) {
      if (isAssigned(coreType, dataFileName, primaryOnly, true)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Get the number of columns assigned to a given SensorType. If the SensorType
   * is not found, returns 0
   *
   * @param sensorType
   *          The SensorType
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
   *
   * @param columnId
   *          The file column database ID
   * @return The sensor assignment
   * @throws RecordNotFoundException
   *           If there is no assignment with the specified ID
   */
  public SensorType getSensorTypeForDBColumn(long columnId)
    throws RecordNotFoundException {
    SensorType result = null;

    if (columnId == FileDefinition.LONGITUDE_COLUMN_ID) {
      result = SensorType.LONGITUDE_SENSOR_TYPE;
    } else if (columnId == FileDefinition.LATITUDE_COLUMN_ID) {
      result = SensorType.LATITUDE_SENSOR_TYPE;
    } else {
      // Try to get the SensorType from the cache
      result = dbColumnSensorTypeCache.get(columnId);

      if (null == result) {
        for (SensorType sensorType : keySet()) {
          for (SensorAssignment assignment : get(sensorType)) {
            if (assignment.getDatabaseId() == columnId) {
              result = sensorType;
              break;
            }
          }

          if (null != result) {
            dbColumnSensorTypeCache.put(columnId, result);
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Get the database IDs for the columns assigned as Run Type.
   *
   * @return The Run Type assignment IDs.
   */
  public List<Long> getRunTypeColumnIDs() {
    List<Long> result = new ArrayList<Long>();
    for (SensorAssignment assignment : get(SensorType.RUN_TYPE_SENSOR_TYPE)) {
      result.add(assignment.getDatabaseId());
    }

    return result;
  }

  public List<Long> getFileColumnIDs() {
    List<Long> ids = new ArrayList<Long>();

    for (TreeSet<SensorAssignment> typeAssignments : values()) {
      for (SensorAssignment assignment : typeAssignments) {
        ids.add(assignment.getDatabaseId());
      }
    }

    return ids;
  }

  /**
   * Get the column IDs of all sensor columns
   *
   * @return The sensor column IDs
   */
  public List<Long> getSensorColumnIds() {
    return getColumnIds(SensorType::isSensor);
  }

  /**
   * Get the column IDs of all sensor columns in a specified group.
   *
   * @param group
   *          The group.
   * @return The matching column IDs.
   */
  public List<Long> getGroupColumnIds(String group) {
    return getColumnIds(s -> s.getGroup().equals(group));
  }

  /**
   * Get the column IDs of all diagnostic columns
   *
   * @return The sensor column IDs
   */
  public List<Long> getDiagnosticColumnIds() {
    return getColumnIds(SensorType::isDiagnostic);
  }

  private List<Long> getColumnIds(Predicate<SensorType> filterMethod) {
    return keySet().stream().filter(filterMethod)
      .map(t -> get(t).stream().map(a -> a.getDatabaseId())
        .collect(Collectors.toList()))
      .flatMap(l -> l.stream()).collect(Collectors.toList());
  }

  /**
   * Determine whether or not any of the assigned sensors require internal
   * calibrations.
   *
   * @return {@code true} if any internal calibrations are required;
   *         {@code false} otherwise.
   */
  public boolean hasInternalCalibrations() {
    boolean result = false;

    for (SensorType sensorType : keySet()) {
      if (sensorType.hasInternalCalibration() && get(sensorType).size() > 0) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether a Run Type column has been assigned.
   *
   * @return {@code true} if a Run Type column has been assigned; {@code false}
   *         otherwise.
   */
  public boolean hasRunType() {
    return isAssigned(SensorType.RUN_TYPE_SENSOR_TYPE);
  }

  /**
   * Get the IDs of the columns that will be internally calibrated during data
   * reduction.
   *
   * @return The column IDs
   */
  public List<Long> getInternalCalibrationSensors() {

    List<Long> result = new ArrayList<Long>();

    for (SensorType sensorType : keySet()) {
      if (sensorType.hasInternalCalibration()) {
        for (SensorAssignment assignment : get(sensorType)) {
          result.add(assignment.getDatabaseId());
        }
      }
    }

    return result;
  }

  public boolean isOfSensorType(long columnId, SensorType sensorType) {
    return getColumnIds(sensorType).contains(columnId);
  }

  public List<Long> getColumnIds(String sensorTypeName)
    throws SensorTypeNotFoundException {

    SensorType sensorType = ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType(sensorTypeName);

    return getColumnIds(sensorType);
  }

  public List<Long> getColumnIds(SensorType sensorType) {
    List<SensorType> sensorTypes = new ArrayList<SensorType>(1);
    sensorTypes.add(sensorType);
    return getColumnIds(sensorTypes);
  }

  public List<Long> getColumnIds(Collection<SensorType> sensorTypes) {

    List<Long> result = null;

    for (SensorType sensorType : sensorTypes) {

      if (sensorType.equals(SensorType.LONGITUDE_SENSOR_TYPE)) {
        result = new ArrayList<Long>(1);
        result.add(FileDefinition.LONGITUDE_COLUMN_ID);
      } else if (sensorType.equals(SensorType.LATITUDE_SENSOR_TYPE)) {
        result = new ArrayList<Long>(1);
        result.add(FileDefinition.LATITUDE_COLUMN_ID);
      } else {

        SensorsConfiguration sensorConfig = ResourceManager.getInstance()
          .getSensorsConfiguration();

        Set<SensorType> localSensorTypes;

        if (sensorConfig.isParent(sensorType)) {
          localSensorTypes = sensorConfig.getChildren(sensorType);
        } else {
          localSensorTypes = new HashSet<SensorType>();
          localSensorTypes.add(sensorType);
        }

        for (SensorType type : localSensorTypes) {
          TreeSet<SensorAssignment> assignments = get(type);
          if (null != assignments) {

            result = new ArrayList<Long>(assignments.size());

            for (SensorAssignment assignment : assignments) {
              result.add(assignment.getDatabaseId());
            }
          }
        }
      }
    }

    if (null == result) {
      result = new ArrayList<Long>(0);
    }

    return result;
  }

  /**
   * Determines whether or not all sensor types required for a given variable
   * have been assigned.
   *
   * @param variable
   *          The variable to be checked.
   * @return {@code true} if all required sensor types are assigned;
   *         {@code false} otherwise.
   * @throws SensorConfigurationException
   *           If the sensor types for the variable cannot be retrieved.
   * @throws SensorTypeNotFoundException
   *           If a referenced {@link SensorType} does not exist in the
   *           database.
   */
  public boolean isVariableComplete(Variable variable)
    throws SensorConfigurationException, SensorTypeNotFoundException {

    boolean complete = true;

    Set<SensorType> requiredSensorTypes = getSensorConfig()
      .getSensorTypes(variable.getId(), false, false, true);

    for (SensorType sensorType : requiredSensorTypes) {

      // For a Parent sensor type, check its children
      if (getSensorConfig().isParent(sensorType)) {

        boolean anyChildAssigned = false;
        boolean anyChildDependentsRequired = false;

        for (SensorType childType : getSensorConfig().getChildren(sensorType)) {

          int assigned = variableCompleteSensorTypeCheck(childType, variable);

          if (assigned == FULLY_ASSIGNED) {
            anyChildAssigned = true;
          } else if (assigned == ASSIGNED_WITHOUT_DEPENDENT) {
            anyChildDependentsRequired = true;
          }
        }
        if (!anyChildAssigned || anyChildDependentsRequired) {
          complete = false;
          break;
        }
      } else {
        if (variableCompleteSensorTypeCheck(sensorType,
          variable) != FULLY_ASSIGNED) {
          complete = false;
          break;
        }
      }
    }

    return complete;
  }

  private int variableCompleteSensorTypeCheck(SensorType sensorType,
    Variable variable) throws SensorTypeNotFoundException {

    int result = FULLY_ASSIGNED;

    AttributeCondition attributeCondition = variable
      .getAttributeCondition(sensorType);

    // Only check the assignment if the variable attribute settings require it
    if (null == attributeCondition
      || attributeCondition.matches(variable.getAttributes())) {
      if (!isAssigned(sensorType)) {
        // If the sensor type isn't assigned, we can stop
        result = NOT_ASSIGNED;
      } else if (sensorType.dependsOnOtherType()) {

        boolean checkDependsOn = false;

        // If there's no depends question, we can directly check the dependsOn
        if (!sensorType.hasDependsQuestion()) {
          checkDependsOn = true;
        } else {

          // We only need to check dependsOn if the depends question has been
          // answered True for any assignment
          for (SensorAssignment assignment : get(sensorType)) {
            if (assignment.getDependsQuestionAnswer()) {
              checkDependsOn = true;
            }
          }
        }

        if (checkDependsOn) {
          SensorType dependsOn = getSensorConfig()
            .getSensorType(sensorType.getDependsOn());
          if (!isAssigned(dependsOn)) {
            result = ASSIGNED_WITHOUT_DEPENDENT;
          }
        }
      }
    }

    return result;
  }

  public void renameFile(String oldName, String newName) {
    for (TreeSet<SensorAssignment> assignments : values()) {
      for (SensorAssignment assignment : assignments) {
        if (assignment.getDataFile().equals(oldName)) {
          assignment.setDataFile(newName);
        }
      }
    }
  }

  /**
   * Get the assignment for the sensor with the specified name.
   *
   * @param name
   *          The sensor name.
   * @return The assignment.
   * @throws SensorAssignmentException
   *           If there is no assignment with the specified sensor name.
   */
  public SensorAssignment getBySensorName(String name)
    throws SensorAssignmentException {

    Optional<SensorAssignment> found = values().stream().flatMap(Set::stream)
      .filter(a -> a.getSensorName().equals(name)).findAny();

    if (found.isEmpty()) {
      throw new SensorAssignmentException(
        "Cannot find assignment for sensor named '" + name + "'");
    }

    return found.get();
  }

  /**
   * Get all the assignments as a {@link Stream}.
   *
   * @return The assignments.
   */
  protected Stream<SensorAssignment> getAllAssignments() {
    return values().stream().flatMap(Set::stream);
  }

  /**
   * Get the {@link SensorType}s that are assigned.
   *
   * <p>
   * The {@link SensorType}s are in display order according to
   * {@link SensorType#compareTo(SensorType)}.
   * </p>
   *
   * @return
   */
  public Set<SensorType> getAssignedSensorTypes() {
    return keySet().stream().filter(k -> get(k).size() > 0)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * Get all assigned sensor names in lower case.
   *
   * @return
   */
  public List<String> getAllSensorNames() {
    return values().stream().flatMap(Set::stream)
      .map(a -> a.getSensorName().toLowerCase()).collect(Collectors.toList());
  }

  /**
   * Determines whether or not there are any diagnostic sensors assigned.
   *
   * @return {@code true} if at least one diagnostic sensor is assigned;
   *         {@code false} otherwise.
   */
  public boolean hasDiagnosticSensors() {
    return keySet().stream().filter(t -> t.isDiagnostic()).map(t -> get(t))
      .filter(a -> a.size() > 0).findAny().isPresent();
  }

  /**
   * Get the list of assigned diagnostic sensors.
   *
   * @return The diagnostic sensors.
   */
  public List<SensorAssignment> getDiagnosticSensors() {
    return keySet().stream().filter(t -> t.isDiagnostic())
      .flatMap(t -> get(t).stream()).collect(Collectors.toList());
  }

  /**
   * Get the list of assigned non-diagnostic sensors.
   *
   * @return The diagnostic sensors.
   */
  public List<SensorAssignment> getNonDiagnosticSensors(
    boolean includeSystemTypes) {
    return keySet().stream().filter(
      t -> !t.isDiagnostic() && (includeSystemTypes ? true : !t.isSystemType()))
      .flatMap(t -> get(t).stream()).collect(Collectors.toList());
  }

  /**
   * Get the {@link SensorAssignment} with the specified database ID.
   *
   * <p>
   * Returns {@code null} if there is no assignment with that ID.
   * </p>
   *
   * @param assignmentId
   *          The database ID
   * @return The SensorAssignment.
   */
  public SensorAssignment getById(long assignmentId) {
    Optional<SensorAssignment> found = values().stream().flatMap(Set::stream)
      .filter(a -> a.getDatabaseId() == assignmentId).findFirst();
    return found.isPresent() ? found.get() : null;
  }

  /**
   * Search for a {@link SensorAssignment} from the specified sensor (column)
   * name.
   *
   * <p>
   * All {@link SensorAssignment}s are searched to see if the assigned sensor
   * name matches the passed in name. If one, and only one, matching assignment
   * is found, it is returned.
   * </p>
   * <p>
   * If zero or multiple matching {@link SensorAssignments} are found,
   * {@code null} is returned.
   * </p>
   *
   * @param sensorName
   *          The sensor name to search for.
   * @return The matching {@link SensorAssignment} if exactly one match is
   *         found; {@code null} otherwise.
   */
  public SensorAssignment getSingleAssignment(String sensorName) {

    SensorAssignment foundAssignment = null;

    mainloop: for (TreeSet<SensorAssignment> assignmentsSet : values()) {
      for (SensorAssignment assignment : assignmentsSet) {
        if (assignment.getSensorName().equalsIgnoreCase(sensorName)) {

          // If we already found an assignment, clear it and abort
          if (null != foundAssignment) {
            foundAssignment = null;
            break mainloop;
          } else {
            foundAssignment = assignment;
          }
        }
      }
    }

    return foundAssignment;
  }

  /**
   * Get the database column IDs from a collection of {@link SensorAssignment}s.
   *
   * @param assignments
   *          The sensor assignments.
   * @return The assigned column IDs.
   */
  public static Collection<Long> getColumnIdsForAssignments(
    Collection<SensorAssignment> assignments) {
    return assignments.stream().map(a -> a.getDatabaseId()).toList();
  }

  public static SensorAssignments create(int basis, DataSource dataSource,
    List<Long> instrumentVariables) throws SensorTypeNotFoundException,
    SensorConfigurationException, DatabaseException {

    switch (basis) {
    case Instrument.BASIS_TIME: {
      return new SensorAssignments(dataSource, instrumentVariables);
    }
    case Instrument.BASIS_ARGO: {
      return new ArgoSensorAssignments(dataSource, instrumentVariables);
    }
    default: {
      throw new IllegalArgumentException("Unrecognised basis " + basis);
    }
    }
  }

  public int getFixedLongitudeFormat() {
    return PositionSpecification.NO_FORMAT;
  }

  public int getFixedLatitudeFormat() {
    return PositionSpecification.NO_FORMAT;
  }

  /**
   * Custom assignment of file columns to sensor types. The default
   * implementation does nothing.
   *
   * <p>
   * The {@link NewInstrumentBean} will attempt to automatically assign columns
   * to sensor types. Some instrument types can automatically specify their own
   * assignments over and above what can be done generically. This can include:
   * </p>
   * <ul>
   * <li>Date/Time and Position assignments, which also need a format assigned.
   * QuinCe can't guess this, but some applications (e.g. Argo) will have fixed
   * formats.</li>
   * <li>QuinCe will refuse to guess some columns (e.g. TEMP) because they are
   * too generic and may refer to different things in different circumstances.
   * If we absolutely know what they refer to, we can add them here.</li>
   * </ul>
   *
   * @param files
   *          The instrument's files.
   */
  public void customAssignColumns(NewInstrumentFileSet files)
    throws SensorAssignmentException {
    // NOOP
  }
}
