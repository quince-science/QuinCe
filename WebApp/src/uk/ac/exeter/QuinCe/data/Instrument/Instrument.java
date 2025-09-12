package uk.ac.exeter.QuinCe.data.Instrument;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroups;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorGroupsSerializer;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.VariableNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Holds all the details of an instrument.
 */
public class Instrument {

  /**
   * Name for the Sensor Groups entry in the JSON representation of the
   * instrument's properties.
   */
  private static final String SENSOR_GROUPS_JSON_NAME = "sensorGroups";

  /**
   * Name for the Diagnostic QC Configuration entry in the JSON representation
   * of the instrument's properties.
   */
  private static final String DIAGNOSTIC_QC_JSON_NAME = "diagnosticQC";

  /**
   * Property name for the pre-flushing time.
   */
  public static final String PROP_PRE_FLUSHING_TIME = "preFlushingTime";

  /**
   * Property name for the post-flushing time.
   */
  public static final String PROP_POST_FLUSHING_TIME = "postFlushingTime";

  /**
   * Property name for the depth.
   */
  public static final String PROP_DEPTH = "depth";

  /**
   * Property name for a fixed longitude. Used if the instrument's data does not
   * contain position information from a GPS.
   */
  public static final String PROP_LONGITUDE = "longitude";

  /**
   * Property name for the fixed latitude. Used if the instrument's data does
   * not contain position information from a GPS.
   */
  public static final String PROP_LATITUDE = "latitude";

  /**
   * The instrument's database ID.
   */
  private long id = DatabaseUtils.NO_DATABASE_RECORD;

  /**
   * The owner of the instrument.
   */
  private User owner;

  /**
   * The database IDs of the users with which the owner has shared this
   * instrument.
   */
  private List<Long> sharedWith;

  /**
   * The name of the instrument.
   */
  private final String name;

  /**
   * The instrument properties.
   */
  private Properties properties = null;

  /**
   * The instrument's file format definitions.
   */
  private InstrumentFileSet fileDefinitions = null;

  /**
   * The variables measured by this instrument.
   */
  private List<Variable> variables = null;

  /**
   * The properties set for the variables measured by this instrument.
   */
  private Map<Variable, Properties> variableProperties = null;

  /**
   * The assignment of columns in data files to sensors.
   */
  private SensorAssignments sensorAssignments = null;

  /**
   * The sensor groups.
   */
  private SensorGroups sensorGroups = null;

  /**
   * The name of the platform on which the instrument is deployed.
   */
  private final String platformName;

  /**
   * The code (usually an ICES code) for the platform on which the instrument is
   * deployed.
   */
  private final String platformCode;

  /**
   * Indicates whether or not this instrument supplies near-real-time data.
   *
   * At the time of writing, the NRT flag can only be set manually on the
   * database after the instrument is created. All calls within QuinCe set this
   * to false.
   */
  private boolean nrt = false;

  /**
   * The time at which the most recent automatic export of an NRT dataset was
   * performed for this Instrument.
   */
  private LocalDateTime lastNrtExport = null;

  /**
   * The configuration for the behaviour of diagnostic sensors QC.
   */
  private DiagnosticQCConfig diagnosticQC;

  /**
   * The time when this instrument was created.
   */
  private final LocalDateTime created;

  /**
   * Create an Instrument object with an existing database record.
   *
   * @param owner
   *          The instrument's owner.
   * @param databaseId
   *          The instrument's database ID.
   * @param name
   *          The name of the instrument.
   * @param sharedWith
   *          The database IDs of the users with which the instrument is shared.
   * @param fileDefinitions
   *          The data file definitions for the instrument.
   * @param variables
   *          The variables measured by the instrument.
   * @param variableProperties
   *          The configuration properties for the measured variables.
   * @param sensorAssignments
   *          The assignments of input data columns to specific sensors.
   * @param platformName
   *          The name of the platform on which the instrument is deployed.
   * @param platformCode
   *          The code for the platform on which the instrument is deployed.
   * @param nrt
   *          Indicates whether or not the instrument provides data in near real
   *          time.
   * @param lastNrtExport
   *          The time at which an NRT dataset was last exported.
   * @param propertiesJson
   *          The main instrument properties as a JSON string.
   * @param created
   *          The time when the instrument was created.
   * @throws SensorGroupsException
   *           If the sensor groupings configuration is invalid.
   */
  public Instrument(User owner, long databaseId, String name,
    List<Long> sharedWith, InstrumentFileSet fileDefinitions,
    List<Variable> variables, Map<Variable, Properties> variableProperties,
    SensorAssignments sensorAssignments, String platformName,
    String platformCode, boolean nrt, LocalDateTime lastNrtExport,
    String propertiesJson, LocalDateTime created) throws SensorGroupsException {

    this.owner = owner;
    this.id = databaseId;
    this.name = name;
    this.sharedWith = null != sharedWith ? sharedWith : new ArrayList<Long>();
    this.fileDefinitions = fileDefinitions;
    this.variables = variables;
    this.variableProperties = variableProperties;
    this.sensorAssignments = sensorAssignments;
    this.platformName = platformName;
    this.platformCode = platformCode;
    this.nrt = nrt;
    this.lastNrtExport = lastNrtExport;
    parsePropertiesJson(propertiesJson);
    this.created = created;
  }

  /**
   * Copy constructor. Performs a shallow copy on members.
   *
   * @param source
   *          The source object.
   */
  public Instrument(Instrument source) {
    this.owner = source.owner;
    this.id = source.id;
    this.name = source.name;
    this.sharedWith = source.sharedWith;
    this.fileDefinitions = source.fileDefinitions;
    this.variables = source.variables;
    this.variableProperties = source.variableProperties;
    this.sensorAssignments = source.sensorAssignments;
    this.platformName = source.platformName;
    this.platformCode = source.platformCode;
    this.nrt = source.nrt;
    this.lastNrtExport = source.lastNrtExport;
    this.properties = source.properties;
    this.created = source.created;
  }

  /**
   * Create a new instrument that is not yet fully configured and not stored in
   * the database.
   *
   * @param owner
   *          The instrument's owner.
   * @param name
   *          The name of the instrument.
   * @param sharedWith
   *          The database IDs of the users with which the instrument is shared.
   * @param fileDefinitions
   *          The data file definitions for the instrument.
   * @param variables
   *          The variables measured by the instrument.
   * @param variableProperties
   *          The configuration properties for the measured variables.
   * @param sensorAssignments
   *          The assignments of input data columns to specific sensors.
   * @param sensorGroups
   *          The logical groupings of the defined sensors.
   * @param platformName
   *          The name of the platform on which the instrument is deployed.
   * @param platformCode
   *          The code for the platform on which the instrument is deployed.
   * @param nrt
   *          Indicates whether or not the instrument provides data in near real
   *          time.
   * @param lastNrtExport
   *          The time at which an NRT dataset was last exported.
   * @param created
   *          The time when the instrument was created.
   */
  public Instrument(User owner, String name, List<Long> sharedWith,
    InstrumentFileSet fileDefinitions, List<Variable> variables,
    Map<Variable, Properties> variableProperties,
    SensorAssignments sensorAssignments, SensorGroups sensorGroups,
    String platformName, String platformCode, boolean nrt,
    LocalDateTime lastNrtExport, LocalDateTime created) {

    this.owner = owner;
    this.name = name;
    this.sharedWith = null != sharedWith ? sharedWith : new ArrayList<Long>();
    this.fileDefinitions = fileDefinitions;
    this.variables = variables;
    this.variableProperties = variableProperties;
    this.sensorAssignments = sensorAssignments;
    this.sensorGroups = sensorGroups;
    this.platformName = platformName;
    this.platformCode = platformCode;
    this.nrt = nrt;
    this.lastNrtExport = lastNrtExport;
    this.properties = new Properties();
    this.created = created;
  }

  /**
   * Validate that all required information for the Instrument is present and
   * consistent.
   *
   * @param checkDatabaseColumns
   *          Specifies whether or not database columns have been assigned and
   *          should be checked.
   * @throws InstrumentException
   *           If the instrument is not valid.
   */
  public void validate(boolean checkDatabaseColumns)
    throws InstrumentException {
    // TODO Write it!
    // Compare sensor groups with sensor assignments
  }

  /**
   * Get the instrument's database ID.
   *
   * @return The database ID.
   */
  public long getId() {
    return id;
  }

  /**
   * Set the instrument's database ID
   *
   * @param databaseID
   *          The database ID.
   */
  public void setDatabaseId(long databaseID) {
    this.id = databaseID;
  }

  /**
   * Get the {@link User} object representing the owner of the instrument.
   *
   * @return The instrument's owner.
   */
  public User getOwner() {
    return owner;
  }

  /**
   * Set the instrument's owner.
   *
   * @param owner
   *          The new owner.
   */
  public void setOwner(User owner) {
    this.owner = owner;
  }

  /**
   * Get the instrument's name.
   *
   * @return The instrument name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the instrument's file format definitions.
   *
   * @return The file definitions.
   */
  public InstrumentFileSet getFileDefinitions() {
    return fileDefinitions;
  }

  /**
   * Get the specification of assignments of data columns to sensors.
   *
   * @return The sensor assignments.
   */
  public SensorAssignments getSensorAssignments() {
    return sensorAssignments;
  }

  /**
   * Get the code of the platform on which the instrument is deployed.
   *
   * @return The platform code.
   */
  public String getPlatformCode() {
    return platformCode;
  }

  /**
   * Get the name of the platform on which the instrument is deployed.
   *
   * @return The platform name.
   */
  public String getPlatformName() {
    return platformName;
  }

  /**
   * Determine whether or not this instrument provides near-real-time data.
   *
   * @return {@code true} if NRT data is provided; {@code false} if it is not
   */
  public boolean getNrt() {
    return nrt;
  }

  /**
   * Set the NRT flag for the instrument.
   *
   * @param nrt
   *          The NRT flag.
   */
  public void setNrt(boolean nrt) {
    this.nrt = nrt;
  }

  /**
   * Get the {@link Variable}s measured by this instrument.
   *
   * @return The measured variables.
   */
  public List<Variable> getVariables() {
    return variables;
  }

  /**
   * Wrapper method to {@link #getRunTypeCategory(long, String)} that takes a
   * {@link Map.Entry}.
   *
   * @param runTypeEntry
   *          The map entry.
   * @return The Run Type category
   * @throws RunTypeCategoryException
   *           If the category cannot be determined.
   */
  public RunTypeCategory getRunTypeCategory(
    Map.Entry<Long, String> runTypeEntry) throws RunTypeCategoryException {
    return getRunTypeCategory(runTypeEntry.getKey(), runTypeEntry.getValue());
  }

  /**
   * Get the {@link RunTypeCategory} to which a specified Run Type value is
   * assigned in the context of a specified {@link Variable}'s requirements.
   *
   * @param variableId
   *          The {@link Variable}'s database ID.
   * @param runTypeValue
   *          The Run Type value
   * @return The Run Type category
   * @throws RunTypeCategoryException
   *           If the category cannot be determined.
   */
  public RunTypeCategory getRunTypeCategory(long variableId,
    String runTypeValue) throws RunTypeCategoryException {

    RunTypeCategory result = null;

    // Fixed run types are those defined in code (see Measurement.java), and
    // determined programmatically.
    // Non-fixed ones are provided as a column in the data

    // Start by testing the fixed run types
    switch (runTypeValue) {
    case Measurement.IGNORED_RUN_TYPE: {
      result = RunTypeCategory.IGNORED;
      break;
    }
    case Measurement.INTERNAL_CALIBRATION_RUN_TYPE: {
      result = RunTypeCategory.INTERNAL_CALIBRATION;
      break;
    }
    case Measurement.MEASUREMENT_RUN_TYPE: {
      try {
        Variable variable = ResourceManager.getInstance()
          .getSensorsConfiguration().getInstrumentVariable(variableId);
        result = new RunTypeCategory(variableId, variable.getName());
      } catch (VariableNotFoundException e) {
        throw new RunTypeCategoryException(
          "Variable not found for variable ID " + variableId);
      }
      break;
    }
    default: {
      // We didn't see anything for the fixed run types. See if there are custom
      // ones defined.
      TreeSet<SensorAssignment> runTypeAssignments = getSensorAssignments()
        .get(SensorType.RUN_TYPE_SENSOR_TYPE);
      if (null == runTypeAssignments || runTypeAssignments.size() == 0) {
        throw new RunTypeCategoryException(
          "No custom run types defined for variable ID " + variableId);
      } else {
        FileDefinition fileDef = getFileDefinitions()
          .get(runTypeAssignments.first().getDataFile());
        result = fileDef.getRunTypes().getRunTypeCategory(runTypeValue);
        if (null == result) {
          throw new RunTypeCategoryException(
            "Unrecognised run type " + runTypeValue);
        }
      }
    }
    }

    return result;
  }

  /**
   * Determine whether a given Run Type values corresponds to a state where the
   * instrument is taking measurements (as opposed to calibration, flushing
   * etc.).
   *
   * @param runType
   *          The Run Type value.
   * @return {@code true} if the Run Type corresponds to the instrument's
   *         measuring mode; {@code false} if it does not.
   * @throws RunTypeCategoryException
   *           If the Run Type category cannot be determined.
   */
  public boolean isMeasurementRunType(String runType)
    throws RunTypeCategoryException {

    boolean result = false;

    for (Variable variable : variables) {
      if (getRunTypeCategory(variable.getId(), runType).isMeasurementType()) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Get the {@link Variable} object for a variable using its database ID.
   *
   * <p>
   * Only {@link Variable}s measured by this instrument will be located and
   * returned. A request for a {@link Variable} not measured by the instrument
   * will result in an exception, even if it exists in the database.
   * </p>
   *
   * @param variableId
   *          The variable ID
   * @return The {@link Variable} object.
   * @throws InstrumentException
   *           If the variable is not measured by this instrument.
   */
  public Variable getVariable(long variableId) throws InstrumentException {
    Variable result = null;

    result = variables.stream().filter(v -> v.getId() == variableId).findAny()
      .orElse(null);

    if (null == result) {
      throw new InstrumentException(
        "Variable with ID " + variableId + " is not part of this instrument");
    }
    return result;
  }

  /**
   * Get a {@link Variable} based on its name.
   *
   * <p>
   * This method will only return a result if the {@link Variable} exists,
   * <i>and</i> it is measured by this instrument.
   * </p>
   *
   * @param name
   *          The variable name
   * @return The InstrumentVariable, or {@code null} if the instrument does not
   *         have the variable defined.
   */
  public Variable getVariable(String name) {
    return variables.stream().filter(v -> v.getName().equals(name)).findAny()
      .orElse(null);
  }

  /**
   * Get the Run Types types that correspond to measurements for a given
   * {@link Variable}.
   *
   * <p>
   * The output of this method is a {@link Map} of {@link RunTypeCategory}
   * numeric codes (including variable measurements, calibration modes, etc.) to
   * the Run Type values that correspond to them. This includes all aliased Run
   * Type values.
   * </p>
   *
   * @param variable
   *          The {@link Variable} whose Run Type values are required.
   * @return The Run Type values, grouped by category.
   * @see RunTypeCategory
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
   * Get the list of {@link Variable}s that require the specified
   * {@link SensorType} for its data reduction.
   *
   * <p>
   * If the {@link SensorType} is not required by any {@link Variable}s, the
   * returned list will be empty.
   * </p>
   *
   * @param sensorType
   *          The {@link SensorType}.
   * @return The {@link Variable}s that require the {@link SensorType}.
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

  /**
   * Get the Run Type values associated with a given {@link RunTypeCategory}
   * using its numeric code. Aliases are not included.
   *
   * @param categoryCode
   *          The numeric {@link RunTypeCategory} code.
   * @return The Run Type values associated with the category.
   * @see RunTypeCategory
   */
  public List<String> getRunTypes(long categoryCode) {
    List<String> result = new ArrayList<String>();

    for (FileDefinition fileDef : fileDefinitions) {
      if (null != fileDef.getRunTypes()) {
        for (Map.Entry<String, RunTypeAssignment> entry : fileDef.getRunTypes()
          .entrySet()) {
          if (entry.getValue().getCategoryCode() == categoryCode) {
            result.add(entry.getKey());
          }
        }
      }
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Get the Run Type values associated with measurements for any
   * {@link Variable} measured by the instrument.
   *
   * @return The Run Types associated with measurements.
   */
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

  /**
   * Get the Run Type values associated with internal calibrations.
   *
   * @return The Run Types associated with internal calibrations.
   */
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

  /**
   * Get all the Run Type values associated with this instrument.
   *
   * <p>
   * The output of this method is a {@link Map} of {@link RunTypeCategory}
   * objects (corresponding to measurements, calibration modes, etc.) to the Run
   * Type values that correspond to them. This includes all aliased Run Type
   * values.
   * </p>
   *
   * @return The Run Types for the instrument.
   * @see RunTypeCategory
   */
  public Map<RunTypeCategory, TreeSet<RunTypeAssignment>> getAllRunTypes() {
    Map<RunTypeCategory, TreeSet<RunTypeAssignment>> runTypes = new TreeMap<RunTypeCategory, TreeSet<RunTypeAssignment>>();

    for (FileDefinition fileDef : fileDefinitions) {
      RunTypeAssignments assignments = fileDef.getRunTypes();
      if (null != assignments) {
        for (RunTypeAssignment assignment : assignments.values()) {
          if (!assignment.isAlias()) {
            if (!runTypes.containsKey(assignment.getCategory())) {
              runTypes.put(assignment.getCategory(),
                new TreeSet<RunTypeAssignment>());
            }

            runTypes.get(assignment.getCategory()).add(assignment);
          }
        }
      }
    }

    return runTypes;
  }

  /**
   * Get all the Run Type values defined for the instrument, including aliases.
   *
   * @return The instrument's Run Type values.
   */
  public List<String> getAllRunTypeNames() {
    return getAllRunTypes().values().stream().flatMap(v -> v.stream())
      .map(r -> r.getRunName()).toList();
  }

  /**
   * Determine whether or not the instrument has internal calibrations defined.
   *
   * <p>
   * This is determined by whether or not any run types of
   * {@link RunTypeCategory#INTERNAL_CALIBRATION_TYPE} have been assigned. Even
   * though some sensor types provide options for Internal Calibration run
   * types, these are optional - if they aren't provided, QuinCe won't attempt
   * to perform any calibration.
   * </p>
   *
   * @return {@code true} if internal calibrations are required by the
   *         instrument; {@code false} if not.
   */
  public boolean hasInternalCalibrations() {
    return getRunTypes(RunTypeCategory.INTERNAL_CALIBRATION_TYPE).size() > 0;
  }

  /**
   * Determines whether or not any {@link CalculationCoefficient}s have been
   * defined for any of the {@link Variable}s that are measured by the
   * instrument.
   *
   * @return {@code true} if any {@link CalculationCoefficient}s are defined;
   *         {@code false}.
   */
  public boolean hasCalculationCoefficients() {
    boolean result = false;

    for (Variable variable : variables) {
      if (variable.hasCoefficients()) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determines whether or not any Run Type values have been assigned for the
   * instrument.
   *
   * @return {@code true} if at least one Run Type value is assigned;
   *         {@code false} otherwise.
   */
  public boolean hasRunTypes() {
    return sensorAssignments.getRunTypeColumnIDs().size() > 0;
  }

  /**
   * Determine whether a Run Type indicates that the instrument is taking a
   * measurement for the specified {@link Variable} in the instrument.
   *
   * @param variable
   *          The variable.
   * @param runType
   *          The Run Type value.
   * @return {@code true} if the Run Type value indicates the {@link Variable}
   *         is being measured.
   *
   * @throws RunTypeCategoryException
   *           If the Run Type's category cannot be established.
   */
  public boolean isRunTypeForVariable(Variable variable, String runType)
    throws RunTypeCategoryException {

    boolean result = false;

    if (null != runType) {
      if (null != variable.getRunType()
        && variable.getRunType().equals(runType)) {
        result = true;
      } else if (getRunTypeCategory(variable.getId(), runType)
        .getType() == variable.getId()) {
        result = true;
      }
    }

    return result;
  }

  /**
   * Set a property on the instrument.
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
   * Get a named instrument property.
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

  /**
   * Get the specific properties of the instrument for a specified
   * {@link Variable}.
   *
   * @param variable
   *          The {@link Variable}
   * @return The properties for the {@link Variable}.
   */
  public Properties getVariableProperties(Variable variable) {
    return variableProperties.get(variable);
  }

  /**
   * Get the {@link Variable}-specific properties for all {@link Variable}s
   * measured by the instrument.
   *
   * @return The {@link Variable}-specific properties.
   */
  public Map<Variable, Properties> getAllVariableProperties() {
    return variableProperties;
  }

  /**
   * Get the column headings that are associated with the specified Run Type
   * value.
   *
   * <p>
   * This method determines whether or not the Run Type value indicates that a
   * measurement is being taken for any of the {@link Variable}s measured by
   * this instrument. If they are, all the {@link ColumnHeading}s associated
   * with that {@link Variable} are added to the result.
   * </p>
   *
   * @param runType
   *          The Run Type value.
   * @return The column headings associated with the Run Type value.
   * @throws RunTypeCategoryException
   *           If the Run Type value's category cannot be determined.
   * @see #isRunTypeForVariable(Variable, String)
   * @see ColumnHeading
   */
  public Set<ColumnHeading> getAllVariableColumnHeadings(String runType)
    throws RunTypeCategoryException {
    Set<ColumnHeading> result = new HashSet<ColumnHeading>();

    for (Variable variable : variables) {
      if (isRunTypeForVariable(variable, runType)) {
        result.addAll(variable.getAllColumnHeadings());
      }
    }

    return result;
  }

  /**
   * Determine whether or not this instrument measures the specified
   * {@link Variable}.
   *
   * @param variable
   *          The {@link Variable}
   * @return {@code true} if this instrument measures the {@link Variable};
   *         {@code false} if it does not.
   */
  public boolean hasVariable(Variable variable) {
    return variables.contains(variable);
  }

  /**
   * Convenience method to determine whether or not this instrument has a fixed
   * position.
   *
   * @return {@code true} if the instrument has a fixed position; {@code false}
   *         otherwise.
   */
  public boolean fixedPosition() {
    return null != getProperty("longitude");
  }

  /**
   * Determines whether or not the specified column ID value is valid for this
   * instrument.
   *
   * <p>
   * The specified column ID corresponds to a {@link FileColumn} object. The
   * method determines whether a column with the ID has been assigned as part of
   * the instrument's configuration.
   * </p>
   *
   * @param columnId
   *          The column ID
   * @return {@code true} if the column is part of this instrument's
   *         configuration; {@code false} if it is not.
   * @see FileColumn
   */
  public boolean columnValid(long columnId) {

    boolean result = true;

    try {
      if (columnId == FileDefinition.LONGITUDE_COLUMN_ID
        || columnId == FileDefinition.LATITUDE_COLUMN_ID) {
        if (fixedPosition()) {
          result = false;
        }
      } else if (null == getSensorAssignments()
        .getSensorTypeForDBColumn(columnId)) {
        result = false;
      } else {
        result = ((hasRunTypes()
          && getSensorAssignments().getSensorTypeForDBColumn(columnId)
            .equals(SensorType.RUN_TYPE_SENSOR_TYPE))
          || getSensorAssignments().getSensorColumnIds().contains(columnId)
          || getSensorAssignments().getDiagnosticColumnIds()
            .contains(columnId));
      }
    } catch (RecordNotFoundException e) {
      result = false;
    }

    return result;
  }

  /**
   * Get the {@link SensorGroups} defined for the instrument.
   *
   * @return The {@link SensorGroups}.
   */
  public SensorGroups getSensorGroups() {
    return sensorGroups;
  }

  /**
   * Get the instrument's properties as a JSON string.
   *
   * <p>
   * The JSON represents both {@link #properties} and {@link #sensorGroups}.
   * They are stored in the database as a single entity.
   * </p>
   *
   * @return The properties JSON.
   */
  public String getPropertiesJson() {

    Gson gson = new GsonBuilder()
      .registerTypeAdapter(SensorGroups.class, new SensorGroupsSerializer())
      .registerTypeAdapter(DiagnosticQCConfig.class,
        new DiagnosticQCConfigSerializer())
      .create();

    // Get the basic properties JSON
    JsonObject result = gson.toJsonTree(properties, Properties.class)
      .getAsJsonObject();

    // Add the Sensor Groups
    result.add(SENSOR_GROUPS_JSON_NAME, gson.toJsonTree(sensorGroups));

    // Add diagnostic QC setup
    result.add(DIAGNOSTIC_QC_JSON_NAME,
      gson.toJsonTree(getDiagnosticQCConfig()));

    return result.toString();
  }

  /**
   * Parse a JSON string from the {@code properties} field in the database and
   * add its contents to this object.
   *
   * @param jsonString
   *          The JSON string.
   * @throws SensorGroupsException
   *           If the Sensor Groups portion of the string is invalid.
   */
  private void parsePropertiesJson(String jsonString)
    throws SensorGroupsException {

    if (null == jsonString || jsonString.trim().length() == 0) {
      properties = new Properties();
      sensorGroups = new SensorGroups(sensorAssignments);
    } else {
      JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

      // First extract the SensorGroups element and parse it
      if (json.has(SENSOR_GROUPS_JSON_NAME)) {
        JsonElement sensorGroupsElement = json.get(SENSOR_GROUPS_JSON_NAME);
        sensorGroups = new SensorGroups(sensorGroupsElement, sensorAssignments);

        // Remove the elements and parse the remainder into Properties
        json.remove(SENSOR_GROUPS_JSON_NAME);
      } else {
        sensorGroups = new SensorGroups(sensorAssignments);
      }

      if (json.has(DIAGNOSTIC_QC_JSON_NAME)) {
        JsonElement diagnosticQCElement = json.get(DIAGNOSTIC_QC_JSON_NAME);
        diagnosticQC = new DiagnosticQCConfig(diagnosticQCElement,
          sensorAssignments);

        // Remove the elements and parse the remainder into Properties
        json.remove(DIAGNOSTIC_QC_JSON_NAME);
      }

      properties = new Gson().fromJson(json, Properties.class);
    }
  }

  /**
   * Determine whether or not this instrument has multiple sensor groups.
   *
   * <p>
   * If there is only one sensor group, or the instrument has no
   * {@link SensorGroups} definition, this method will return {@code false}. If
   * there is more than one sensor group then this method returns {@code true}.
   * </p>
   *
   * @return {@code true} if there are sensor groups; {@code false} otherwise.
   */
  public boolean hasSensorGroups() {
    return null != sensorGroups && sensorGroups.size() > 1;
  }

  /**
   * Get the display name of this instrument.
   *
   * <p>
   * The display name is {@code <platformName>;<name>}.
   *
   * @return The instrument's display name.
   * @see #platformName
   * @see #name
   */
  public String getDisplayName() {
    return platformName + ";" + name;
  }

  /**
   * Determine whether or not any diagnostic sensors have been assigned to this
   * instrument.
   *
   * <p>
   * This is just a pass-through to the {@link SensorAssignments} class, because
   * PrimeFaces can't interact with it directly. (It's confused by the fact that
   * it's a {@link Map} and tries to do its own thing.)
   * </p>
   *
   * @return {@code true} if at least one diagnostic sensor is assigned;
   *         {@code false} otherwise.
   */
  public boolean hasDiagnosticSensors() {
    return sensorAssignments.hasDiagnosticSensors();
  }

  /**
   * Get the Diagnostic Sensor QC configuration for the instrument.
   *
   * @return The diagnostic sensor QC configuration.
   */
  public DiagnosticQCConfig getDiagnosticQCConfig() {
    if (null == diagnosticQC) {
      diagnosticQC = new DiagnosticQCConfig();
    }

    return diagnosticQC;
  }

  /**
   * Get a list of {@link Variable} objects for the specified variable IDs.
   *
   * <p>
   * Only {@link Variable}s registered to this instrument will be matched. If a
   * Variable with the specified ID is not present, an exception is thrown.
   * </p>
   *
   * <p>
   * The returned {@link List} will be in the iteration order of the supplied
   * {@link Collection}.
   * </p>
   *
   * @param ids
   *          The variable IDs.
   * @return The Variable objects.
   * @throws VariableNotFoundException
   *           If one of the IDs does not correspond to a Variable registered
   *           with this Instrument.
   */
  public List<Variable> getVariables(Collection<Long> ids)
    throws VariableNotFoundException {
    List<Variable> result = new ArrayList<Variable>(
      null == ids ? 0 : ids.size());

    if (null != ids) {
      for (Long id : ids) {
        Optional<Variable> foundVariable = variables.stream()
          .filter(v -> v.getId() == id).findAny();

        if (foundVariable.isEmpty()) {
          throw new VariableNotFoundException(id);
        } else {
          result.add(foundVariable.get());
        }
      }
    }

    return result;
  }

  /**
   * Get the last time at which an NRT dataset was exported for this instrument.
   *
   * <p>
   * Returns {@code null} if the instrument does not provide NRT data or NRT
   * data has never been exported.
   *
   * @return The time of the last NRT export.
   */
  public LocalDateTime getLastNrtExport() {
    return lastNrtExport;
  }

  /**
   * Get the users with which this instrument has been shared.
   *
   * <p>
   * Only the database IDs of the users are returned.
   * </p>
   *
   * @return The users that the instrument is shared with.
   */
  public List<Long> getSharedWith() {
    return sharedWith;
  }

  /**
   * Determine whether or not this instrument has been shared with the specified
   * {@link User}.
   *
   * @param user
   *          The {@link User} to be checked.
   * @return {@code true} if this instrument is shared with the {@link User};
   *         {@code false} if it is not.
   */
  public boolean isSharedWith(User user) {
    return sharedWith.contains(user.getDatabaseID());
  }

  /**
   * Share this instrument with the specified {@link User}.
   *
   * @param user
   *          The user.
   */
  public void addShare(User user) {
    if (!isSharedWith(user)) {
      sharedWith.add(user.getDatabaseID());
    }
  }

  /**
   * Remove the specified {@link User}'s shared access to this instrument.
   *
   * @param user
   *          The user to be removed.
   */
  public void removeShare(User user) {
    sharedWith.remove(user.getDatabaseID());
  }

  /**
   * Get the time when this instrument was created in QuinCe.
   *
   * @return The instrument's creation time.
   */
  public LocalDateTime getCreated() {
    return created;
  }

  /**
   * Filter a {@link Collection} of {@link Instrument}s to those matching the
   * specified {@link #platformName} and {@link #platformCode}, and sort them
   * with the most recently created first.
   *
   * <p>
   * Since this method is often used to find instruments that match a given
   * instrument, an exclusion ID can be provided to ensure that instrument is
   * not included in the output. Setting this to a negative value will ensure
   * that all matching instruments are returned.
   * </p>
   *
   * @param instruments
   *          The instruments to be filtered
   * @param platformName
   *          The desired platform name
   * @param platformCode
   *          The desired platform code
   * @param exclude
   *          The ID of an instrument to be excluded from the final list
   * @return The filtered instruments.
   */
  public static List<Instrument> filterByPlatform(
    Collection<Instrument> instruments, String platformName,
    String platformCode, long exclude) {

    return instruments.stream()
      .filter(
        i -> i.getId() != exclude && i.getPlatformName().equals(platformName)
          && i.getPlatformCode().equals(platformCode))
      .sorted(new InstrumentCreationDateComparator(true)).toList();
  }

  @Override
  public String toString() {
    return platformName + ":" + name;
  }
}
