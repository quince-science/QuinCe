package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Dataset.ColumnHeading;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.ManualQC.MeasurementValueSensorType;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Defines an individual sensor type for an instrument
 */
public class SensorType extends ColumnHeading
  implements Comparable<SensorType> {

  /**
   * Value to use when a SensorType has no parent
   *
   * Just a random number - only used in this class.
   */
  public static final long NO_PARENT = -677L;

  /**
   * Value to use when a SensorType does not depend on another sensor.
   *
   * Just a random number - only used in this class.
   */
  public static final long NO_DEPENDS_ON = -817L;

  /**
   * Special ID for the Run Type sensor
   */
  public static final long RUN_TYPE_ID = -1;

  /**
   * The display order for the Run Type
   */
  private static final int RUN_TYPE_ORDER = -1000;

  /**
   * Special ID for dummy longitude sensor type
   */
  public static final long LONGITUDE_ID = FileDefinition.LONGITUDE_COLUMN_ID;

  /**
   * The display order for the longitude
   */
  private static final int LONGITUDE_ORDER = -2;

  /**
   * Special ID for dummy latitude sensor type
   */
  public static final long LATITUDE_ID = FileDefinition.LATITUDE_COLUMN_ID;

  /**
   * The display order for the latitude
   */
  private static final int LATITUDE_ORDER = -1;

  /**
   * The special Run Type sensor type
   */
  public static SensorType RUN_TYPE_SENSOR_TYPE;

  /**
   * Dummy longitude sensor type
   */
  public static SensorType LONGITUDE_SENSOR_TYPE;

  /**
   * Dummy latitude sensor type
   */
  public static SensorType LATITUDE_SENSOR_TYPE;

  /**
   * The variable group to which this SensorType belongs
   */
  private String group;

  /**
   * The parent sensor type, if applicable
   */
  private long parent;

  /**
   * Specifies the name of another sensor that must also be present if this
   * sensor is present.
   *
   * <p>
   * Some sensor values depend on the value of another sensor in the instrument
   * for the necessary calculations to be performed. For example, a differential
   * pressure sensor requires an absolute atmospheric pressure sensor in order
   * for the true pressure to be calculated.
   * </p>
   */
  private long dependsOn;

  /**
   * Specifies a question to ask that determines whether the {@link #dependsOn}
   * criterion should be honoured. This question will yield a {@code boolean}
   * result. If the result is {@code true}, then the {@link #dependsOn}
   * criterion will be enforced. If false, it will not. If the question is empty
   * or null, then the criterion will always be enforced.
   */
  private String dependsQuestion;

  /**
   * Indicates whether or not this is a diagnostic sensor
   */
  private boolean diagnostic = false;

  /**
   * Indicates whether or not this sensor has calibration data collected
   * internally by the instrument
   */
  private boolean internalCalibration = false;

  /**
   * Indicates whether the zero standard should be used in calibration.
   */
  private boolean includeZeroInCalibration = true;

  /**
   * Indicates whether this is a special sensor type created internally by the
   * application
   */
  private boolean systemType = false;

  /**
   * Determines where this sensor type will be displayed in lists of sensor
   * types
   */
  private int displayOrder = 0;

  /**
   * Indicates whether values are likely to be significantly different in
   * different run types. QC routines etc. need to know this.
   */
  private boolean runTypeAware = false;

  /**
   * The list of column names known to relate to this SensorType.
   */
  private List<String> sourceColumns = Collections
    .unmodifiableList(new ArrayList<String>());

  static {
    RUN_TYPE_SENSOR_TYPE = new SensorType(RUN_TYPE_ID, "Run Type", "Run Type",
      RUN_TYPE_ORDER, null, "RUNTYPE",
      new String[] { "Type", "Measurement Type" });
    LONGITUDE_SENSOR_TYPE = new SensorType(LONGITUDE_ID, "Longitude",
      "Longitude", LONGITUDE_ORDER, "degrees_east", "ALONGP01", null);
    LATITUDE_SENSOR_TYPE = new SensorType(LATITUDE_ID, "Latitude", "Latitude",
      LATITUDE_ORDER, "degrees_north", "ALATGP01", null);
  }

  /**
   * Create a Sensor Type with the specified values.
   *
   * @param id
   *          The database ID.
   * @param name
   *          The type name.
   * @param group
   *          The group to which the Sensor Type belongs.
   * @param displayOrder
   *          The display order.
   * @param units
   *          The units measured.
   * @param columnCode
   *          The vocabulary code for this Sensor Type.
   */
  private SensorType(long id, String name, String group, int displayOrder,
    String units, String columnCode, String[] sourceColumns) {

    super(id, name, name, columnCode, units, false, true);

    this.group = group;
    this.parent = NO_PARENT;
    this.dependsOn = NO_DEPENDS_ON;
    this.dependsQuestion = null;
    this.internalCalibration = false;
    this.includeZeroInCalibration = true;
    this.diagnostic = false;
    this.systemType = true;
    this.displayOrder = displayOrder;
    this.runTypeAware = false;

    if (null != sourceColumns) {
      this.sourceColumns = Collections.unmodifiableList(Arrays
        .asList(sourceColumns).stream().map(sc -> sc.toLowerCase()).toList());
    }
  }

  /**
   * Copy constructor.
   *
   * @param sensorType
   *          The source object.
   */
  protected SensorType(SensorType source) {
    super(source.getId(), source.getShortName(), source.getLongName(),
      source.getCodeName(), source.getUnits(), false, true);

    this.dependsOn = source.dependsOn;
    this.dependsQuestion = source.dependsQuestion;
    this.diagnostic = source.diagnostic;
    this.displayOrder = source.displayOrder;
    this.group = source.group;
    this.includeZeroInCalibration = source.includeZeroInCalibration;
    this.internalCalibration = source.internalCalibration;
    this.parent = source.parent;
    this.runTypeAware = source.runTypeAware;
    this.systemType = source.systemType;
    this.sourceColumns = source.sourceColumns;
  }

  /**
   * Build a new SensorType object from a database record
   *
   * @param record
   *          The database record
   * @throws SQLException
   *           If the record cannot be read
   * @throws SensorConfigurationException
   *           If the record is invalid
   */
  protected SensorType(ResultSet record)
    throws SQLException, SensorConfigurationException {

    super(record.getLong(1), record.getString(2), record.getString(14),
      record.getString(13), record.getString(12), true, false);

    this.group = record.getString(3);

    Long parent = DatabaseUtils.getNullableLong(record, 4);
    if (null == parent) {
      this.parent = NO_PARENT;
    } else if (parent == getId()) {
      throw new SensorConfigurationException(getId(),
        "A sensor type cannot be its own parent");
    } else {
      this.parent = parent;
    }

    Long dependsOn = DatabaseUtils.getNullableLong(record, 5);
    if (null == dependsOn) {
      this.dependsOn = NO_DEPENDS_ON;
    } else if (dependsOn == getId()) {
      throw new SensorConfigurationException(getId(),
        "A sensor type cannot depend on itself");
    } else {
      this.dependsOn = dependsOn;
    }

    String dependsQuestion = record.getString(6);
    if (null == dependsQuestion || dependsQuestion.trim().length() == 0) {
      this.dependsQuestion = null;
    } else if (!this.dependsOnOtherType()) {
      throw new SensorConfigurationException(getId(),
        "Cannot have a Depends Question without depending on another sensor type");
    } else {
      this.dependsQuestion = dependsQuestion.trim();
    }

    this.internalCalibration = record.getBoolean(7);
    this.includeZeroInCalibration = record.getBoolean(8);
    this.runTypeAware = record.getBoolean(9);
    this.diagnostic = record.getBoolean(10);
    this.displayOrder = record.getInt(11);

    String sourceColumnString = record.getString(15);
    if (null != sourceColumnString) {
      this.sourceColumns = Collections.unmodifiableList(
        StringUtils.delimitedToList(sourceColumnString.toLowerCase(), ";"));
    }
  }

  /**
   * Get the group to which this SensorType belongs
   *
   * @return The group
   */
  public String getGroup() {
    return group;
  }

  /**
   * Get the ID of this type's parent
   *
   * @return The parent ID
   */
  public long getParent() {
    return parent;
  }

  /**
   * Determine whether or not this type has a parent
   *
   * @return {@code true} if the type has a parent; {@code false} if not.
   */
  public boolean hasParent() {
    return parent != NO_PARENT;
  }

  /**
   * Get the ID of the type that this type depends on
   *
   * @return The ID of the type that this type depends on
   */
  public long getDependsOn() {
    return dependsOn;
  }

  /**
   * Determine whether or not this type depends on another type
   *
   * @return {@code true} if this type depends on another type; {@code false} if
   *         not
   */
  public boolean dependsOnOtherType() {
    return dependsOn != NO_DEPENDS_ON;
  }

  /**
   * Gets the question that determines whether the {@link #dependsOn} criterion
   * will be honoured.
   *
   * @return The question
   * @see #dependsQuestion
   */
  public String getDependsQuestion() {
    return dependsQuestion;
  }

  /**
   * Determines whether or not this sensor type has a Depends Question
   *
   * @return {@code true} if there is a Depends Question; {@code false} if there
   *         is not
   */
  public boolean hasDependsQuestion() {
    return (dependsQuestion != null);
  }

  /**
   * Determine whether or not this is a diagnostic sensor
   *
   * @return {@code true} if this is a diagnostic sensor; {@code false} if it is
   *         not.
   */
  public boolean isDiagnostic() {
    return diagnostic;
  }

  /**
   * Determine whether or not this sensor type is internally calibrated
   *
   * @return {@code true} if the type is internall calibrated; {@code false} if
   *         not
   */
  public boolean hasInternalCalibration() {
    return internalCalibration;
  }

  public boolean includeZeroInCalibration() {
    return includeZeroInCalibration;
  }

  /**
   * Get the database field name for this sensor type
   *
   * <p>
   * The database field name is the sensor type's name converted to lower case
   * and with spaces replaced by underscores. Brackets and other odd characters
   * that upset MySQL are removed.
   * </p>
   *
   * <p>
   * Sensors that are not used in calculations are not stored in conventional
   * database fields. For those sensors, this method returns {@code null}.
   * </p>
   *
   * @return The database field name
   */
  public String getDatabaseFieldName() {
    String result = null;

    // TODO These are temporary until the sensor values are moved to
    // a different table in future updates.
    if (getShortName().equals("CO₂ in gas")) {
      result = "co2";
    } else if (getShortName().equals("xH₂O in gas")) {
      result = "xh2o";
    } else {
      result = DatabaseUtils.getDatabaseFieldName(getShortName());
    }

    return result;
  }

  /**
   * See if this SensorType matches the passed in Sensor Type, checking parents
   * and children as appropriate
   *
   * @param o
   *          The SensorType to compare
   * @return {@code true} if this SensorType equals the passed in SensorType, or
   *         its parent/children match. {@code false} if no matches are found
   */
  public boolean equalsIncludingRelations(SensorType o) {
    boolean result = false;

    if (equals(o)) {
      result = true;
    } else {
      SensorsConfiguration config = ResourceManager.getInstance()
        .getSensorsConfiguration();

      if (config.hasParent(this)) {
        result = config.getParent(this).equals(o);
      } else if (config.isParent(this)) {
        for (SensorType child : config.getChildren(this)) {
          if (child.equals(o)) {
            result = true;
            break;
          }
        }

      }
    }

    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (getId() ^ (getId() >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SensorType other = (SensorType) obj;
    if (getId() != other.getId())
      return false;
    return true;
  }

  @Override
  public int compareTo(SensorType o) {

    // Compare display order, then sensor type name
    int result = this.displayOrder - o.displayOrder;

    if (result == 0) {
      result = getShortName().compareTo(o.getShortName());
    }

    return result;
  }

  @Override
  public String toString() {
    return getShortName();
  }

  /**
   * Determine whether or not this sensor type is an internal type generated by
   * the system
   *
   * @return {@code true} if this is a system type; {@code false} otherwise
   */
  public boolean isSystemType() {
    return systemType;
  }

  public static boolean isPosition(long id) {
    return (id == LONGITUDE_ID || id == LATITUDE_ID);
  }

  /**
   * Determines whether or not this {@code SensorType} is for a normal sensor,
   * i.e. not a diagnostic, system or position sensor.
   *
   * @return {@code true} if this is a normal sensor type; {@code false}
   *         otherwise.
   */
  public boolean isSensor() {
    return !isDiagnostic() && !isSystemType() && !isPosition(getId());
  }

  @Override
  public boolean includeType() {
    return true;
  }

  public boolean isPosition() {
    return equals(LONGITUDE_SENSOR_TYPE) || equals(LATITUDE_SENSOR_TYPE);
  }

  public boolean isRunTypeAware() {
    return runTypeAware;
  }

  /**
   * Get the 'true' database ID of a given {@link SensorType}, converting proxy
   * {@link SensorType}s to their originals first.
   *
   * @param sensorType
   *          The SensorType.
   * @return The SensorType's ID.
   */
  public static long getTrueSensorTypeId(SensorType sensorType) {
    return sensorType instanceof MeasurementValueSensorType
      ? ((MeasurementValueSensorType) sensorType).getOriginalId()
      : sensorType.getId();
  }

  public boolean questionableFlagAllowed() {
    return !isDiagnostic() && !isPosition();
  }

  public List<String> getSourceColumns() {
    return sourceColumns;
  }
}
