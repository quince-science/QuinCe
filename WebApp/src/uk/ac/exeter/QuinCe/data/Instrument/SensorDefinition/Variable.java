package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;

/**
 * Class to hold the sensors required for a given variable.
 *
 * The sensors are split into:
 *
 * CORE: The sensor that measures the actual variable REQUIRED: The additional
 * sensors needed to perform data reduction
 *
 * Any sensors that may be required because CORE or REQUIRED depend on them are
 * not included; these are calculated on the fly elsewhere.
 */
public class Variable implements Comparable<Variable> {

  /**
   * The variable's database ID
   */
  private long id;

  /**
   * The variable name
   */
  private String name;

  /**
   * Bit mask indicating which measurement bases are allowed for this Variable.
   */
  private int allowedBasisMask;

  /**
   * IDs and Labels for this variable's attributes, which must be defined by the
   * user when they create an instrument.
   */
  private VariableAttributes attributes;

  /**
   * The variable's properties
   */
  private final VariableProperties properties;

  /**
   * The core SensorType
   */
  private SensorType coreSensorType;

  /**
   * The other sensors required for data reduction
   */
  private List<SensorType> requiredSensorTypes;

  /**
   * The set of attribute conditions that determine whether or not some sensor
   * types are required. These sensor types are in {@link #requiredSensorTypes},
   * but the enforcement will depend on the attribute values specified here.
   */
  private Map<Long, AttributeCondition> attributeConditions;

  /**
   * The cascades from Questionable flags for each required SensorType to the
   * Core SensorType flag
   */
  private VariableCascades cascades;

  /**
   * Column headings to use in export files. These override the settings in the
   * parent {@link SensorType} object.
   */
  private Map<SensorType, ColumnHeading> columnHeadings;

  /**
   * Main constructor using SensorType ids
   *
   * @param id
   *          The variable's database ID
   * @param name
   *          The variable name
   * @param coreSensorTypeId
   *          The core SensorType's ID
   * @param requiredSensorTypeIds
   *          The other required SensorTypes' IDs
   * @throws SensorTypeNotFoundException
   *           If any SensorTypes are not found
   * @throws SensorConfigurationException
   *           If the parameters are not internally consistent
   * @throws InvalidFlagException
   *           If any cascade flags are invalid
   */
  protected Variable(SensorsConfiguration sensorConfig, long id, String name,
    int allowedBasis, VariableAttributes attributes, String propertiesJson,
    long coreSensorTypeId, List<Long> requiredSensorTypeIds,
    Map<Long, AttributeCondition> attrConditions, VariableCascades cascades,
    Map<SensorType, ColumnHeading> columnHeadings)
    throws SensorTypeNotFoundException, SensorConfigurationException,
    InvalidFlagException {

    this.id = id;
    this.name = name;
    this.allowedBasisMask = allowedBasis;
    this.attributes = attributes;

    if (null == propertiesJson || propertiesJson.length() == 0) {
      this.properties = new VariableProperties();
    } else {
      Gson gson = new GsonBuilder()
        .registerTypeAdapter(VariableProperties.class,
          new VariablePropertiesDeserializer(id, name))
        .create();

      this.properties = gson.fromJson(propertiesJson, VariableProperties.class);
    }

    if (coreSensorTypeId < 0) {
      throw new SensorConfigurationException(
        "Variable " + id + "does not have a core SensorType");
    }

    coreSensorType = sensorConfig.getSensorType(coreSensorTypeId);
    if (coreSensorType.hasParent()) {
      throw new SensorConfigurationException(
        "Core sensor type cannot be a child (ID " + coreSensorType.getId()
          + ")");
    }

    this.requiredSensorTypes = new ArrayList<SensorType>(
      requiredSensorTypeIds.size());
    this.attributeConditions = attrConditions;

    this.cascades = cascades;

    for (int i = 0; i < requiredSensorTypeIds.size(); i++) {
      SensorType sensorType = sensorConfig
        .getSensorType(requiredSensorTypeIds.get(i));
      if (sensorType.hasParent()) {
        throw new SensorConfigurationException(
          "Required sensor type cannot be a child (ID " + sensorType.getId()
            + ")");
      }
      requiredSensorTypes.add(sensorType);
    }

    this.columnHeadings = columnHeadings;
  }

  /**
   * Get the database ID of this variable
   *
   * @return
   */
  public long getId() {
    return id;
  }

  /**
   * Get the variable name
   *
   * @return The variable name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the core SensorType
   *
   * @return The core SensorType
   */
  public SensorType getCoreSensorType() {
    return coreSensorType;
  }

  /**
   * Get all SensorTypes required for this variable, including both Core and
   * Required types.
   *
   * @return All required SensorTypes
   */
  public List<SensorType> getAllSensorTypes(boolean includePosition) {
    List<SensorType> result = new ArrayList<SensorType>(requiredSensorTypes);
    result.add(coreSensorType);

    if (includePosition) {
      result.add(SensorType.LONGITUDE_SENSOR_TYPE);
      result.add(SensorType.LATITUDE_SENSOR_TYPE);
    }
    return result;
  }

  /**
   * Get the cascading flag value for a given SensorType. This indicates what
   * flag should be set on the final calculated value based on the flag of a
   * required SensorType.
   *
   * If the SensorType is not related to the final value in any way, return
   * {@code null}.
   *
   * @param sensorType
   *          The SensorType
   * @param flag
   *          The flag assigned to that SensorType
   * @param sensorAssignments
   *          The current set of sensor assignments for the instrument
   * @return The flag to apply to the final calculated value
   * @throws SensorConfigurationException
   *           If the internal configuration is invalid
   */
  public Flag getCascade(SensorType sensorType, FlagScheme flagScheme,
    Flag flag, SensorAssignments sensorAssignments)
    throws SensorConfigurationException {

    Flag result = null;

    if (coreSensorType.equals(sensorType)
      || sensorType.equals(SensorType.LONGITUDE_SENSOR_TYPE)
      || sensorType.equals(SensorType.LATITUDE_SENSOR_TYPE)) {
      result = flag;
    } else if (flagScheme.isGood(flag, true)) {
      result = flagScheme.getAssumedGoodFlag();
    } else if (flag.equals(FlagScheme.NEEDED_FLAG)) {
      result = FlagScheme.NEEDED_FLAG;
    } else {
      result = cascades.getCascadeFlag(flagScheme, sensorType, flag);

      /*
       * If result is null here, this means that the supplied SensorType is not
       * in the list of required sensors. Therefore it must be a sensor that one
       * of the core/required sensors depends on. Find that SensorType and get
       * its cascade.
       *
       * If there's more than one matched SensorType, return the worst flag we
       * find
       */
      if (null == result) {
        Set<SensorType> dependingTypes = sensorAssignments
          .getDependents(sensorType);
        for (SensorType dependingType : dependingTypes) {
          Flag dependingCascadeFlag = getCascade(dependingType, flagScheme,
            flag, sensorAssignments);
          if (null == result
            || dependingCascadeFlag.moreSignificantThan(result)) {
            result = dependingCascadeFlag;
          }
        }
      }

      /*
       * If the result is STILL null, then this sensor has no bearing on the
       * final calculated value. We return this as a null.
       */

    }

    return result;

  }

  public boolean hasAttributes() {
    return attributes.size() > 0;
  }

  public VariableAttributes getAttributes() {
    return attributes;
  }

  public boolean hasInternalCalibrations() {
    boolean result = false;

    for (SensorType sensorType : getAllSensorTypes(false)) {
      if (sensorType.hasInternalCalibration()) {
        result = true;
        break;
      }
    }

    return result;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Variable))
      return false;
    Variable other = (Variable) obj;
    return id == other.id;
  }

  @Override
  public String toString() {
    return name;
  }

  public ColumnHeading getColumnHeading(SensorType sensorType) {
    return columnHeadings.get(sensorType);
  }

  public Collection<ColumnHeading> getAllColumnHeadings() {
    return columnHeadings.values();
  }

  public boolean hasCoefficients() {
    return properties.getCoefficients().size() > 0;
  }

  public List<String> getCoefficients() {
    return properties.getCoefficients();
  }

  public boolean requiresRunType() {
    return properties.hasPresetRunTypes();
  }

  public String getRunType() {
    return properties.getRunType(id);
  }

  @Override
  public int compareTo(Variable o) {
    // Variables are ordered by name
    return name.compareTo(o.name);
  }

  protected AttributeCondition getAttributeCondition(SensorType sensorType) {
    return null == attributeConditions ? null
      : attributeConditions.get(sensorType.getId());
  }

  public boolean basisAllowed(int basis) {
    return (basis & allowedBasisMask) > 0;
  }

  public Map<Long, Boolean> getDependsQuestionAnswers() {
    return properties.getDependsQuestionAnswers();
  }

  public List<PresetRunType> getPresetRunTypes() {
    return properties.getPresetRunTypes();
  }
}
