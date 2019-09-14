package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;

import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Defines an individual sensor type for an instrument
 * 
 * @author Steve Jones
 */
public class SensorType implements Comparable<SensorType> {

  /**
   * Value to use when a SensorType has no parent
   */
  public static final long NO_PARENT = -1000;

  /**
   * Value to use when a SensorType does not depend on another sensor
   */
  public static final long NO_DEPENDS_ON = -1000;

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
   * The database ID of this sensor type
   */
  private long id;

  /**
   * The name of the sensor type
   */
  private String name;

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
   * Indicates whether this is a special sensor type created internally by the
   * application
   */
  private boolean systemType = false;

  /**
   * The column header
   */
  private ColumnHeader columnHeader;

  /**
   * Determines where this sensor type will be displayed in lists of sensor
   * types
   */
  private int displayOrder = 0;

  static {
    RUN_TYPE_SENSOR_TYPE = new SensorType(RUN_TYPE_ID, "Run Type", "Run Type",
      RUN_TYPE_ORDER, null, "RUNTYPE");
    LONGITUDE_SENSOR_TYPE = new SensorType(LONGITUDE_ID, "Longitude",
      "Longitude", LONGITUDE_ORDER, "degrees_east", "ALONGP01");
    LATITUDE_SENSOR_TYPE = new SensorType(LATITUDE_ID, "Latitude", "Latitude",
      LATITUDE_ORDER, "degrees_north", "ALATGP01");
  }

  /**
   * Create a SensorType object. Only minimal checking is performed
   * 
   * @param id
   * @param name
   * @param parent
   * @param dependsOn2
   * @param dependsQuestion2
   * @param internalCalibration
   * @param diagnostic2
   * @throws MissingParamException
   *           If the ID is not a positive number
   */
  public SensorType(long id, String name, String group, Long parent,
    Long dependsOn, String dependsQuestion, boolean internalCalibration,
    boolean diagnostic, int displayOrder, String units, String columnCode,
    String columnHeading)
    throws MissingParamException, SensorConfigurationException {

    MissingParam.checkPositive(id, "id");
    MissingParam.checkMissing(name, "name", false);
    MissingParam.checkMissing(group, "group", false);
    MissingParam.checkNullPositive(parent, "parent");
    MissingParam.checkNullPositive(dependsOn, "dependsOn");

    this.id = id;
    this.name = name;
    this.group = group;

    if (null == parent) {
      this.parent = NO_PARENT;
    } else if (parent == id) {
      throw new SensorConfigurationException(id,
        "A sensor type cannot be its own parent");
    } else {
      this.parent = parent;
    }

    if (null == dependsOn) {
      this.dependsOn = NO_DEPENDS_ON;
    } else if (dependsOn == id) {
      throw new SensorConfigurationException(id,
        "A sensor type cannot depend on itself");
    } else {
      this.dependsOn = dependsOn;
    }

    if (null == dependsQuestion || dependsQuestion.trim().length() == 0) {
      this.dependsQuestion = null;
    } else if (!this.dependsOnOtherType()) {
      throw new SensorConfigurationException(id,
        "Cannot have a Depends Question without depending on another sensor type");
    } else {
      this.dependsQuestion = dependsQuestion.trim();
    }

    this.internalCalibration = internalCalibration;
    this.diagnostic = diagnostic;
    this.systemType = false;
    this.displayOrder = displayOrder;
  }

  /**
   * Internal constructor for special sensor types
   * 
   * @param id
   *          The sensor ID
   * @param name
   *          The sensor name
   */
  private SensorType(long id, String name, String group, int displayOrder,
    String units, String columnCode) {
    this.id = id;
    this.name = name;
    this.group = group;
    this.parent = NO_PARENT;
    this.dependsOn = NO_DEPENDS_ON;
    this.dependsQuestion = null;
    this.internalCalibration = false;
    this.diagnostic = false;
    this.systemType = true;
    this.displayOrder = displayOrder;
    this.columnHeader = new ColumnHeader(name, columnCode, units);
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

    this.id = record.getLong(1);
    this.name = record.getString(2);
    this.group = record.getString(3);

    Long parent = DatabaseUtils.getNullableLong(record, 4);
    if (null == parent) {
      this.parent = NO_PARENT;
    } else if (parent == id) {
      throw new SensorConfigurationException(id,
        "A sensor type cannot be its own parent");
    } else {
      this.parent = parent;
    }

    Long dependsOn = DatabaseUtils.getNullableLong(record, 5);
    if (null == dependsOn) {
      this.dependsOn = NO_DEPENDS_ON;
    } else if (dependsOn == id) {
      throw new SensorConfigurationException(id,
        "A sensor type cannot depend on itself");
    } else {
      this.dependsOn = dependsOn;
    }

    String dependsQuestion = record.getString(6);
    if (null == dependsQuestion || dependsQuestion.trim().length() == 0) {
      this.dependsQuestion = null;
    } else if (!this.dependsOnOtherType()) {
      throw new SensorConfigurationException(id,
        "Cannot have a Depends Question without depending on another sensor type");
    } else {
      this.dependsQuestion = dependsQuestion.trim();
    }

    this.internalCalibration = record.getBoolean(7);
    this.diagnostic = record.getBoolean(8);
    this.displayOrder = record.getInt(9);
    this.columnHeader = new ColumnHeader(record.getString(12),
      record.getString(11), record.getString(10));
  }

  /**
   * Get the database ID of this type
   * 
   * @return The database ID
   */
  public long getId() {
    return id;
  }

  /**
   * Get the name of this sensor type
   * 
   * @return The name of the sensor type
   */
  public String getName() {
    return name;
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
    if (name.equals("CO₂ in gas")) {
      result = "co2";
    } else if (name.equals("xH₂O in gas")) {
      result = "xh2o";
    } else {
      result = DatabaseUtils.getDatabaseFieldName(name);
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

  /**
   * Equality is based on the sensor type ID
   */
  @Override
  public boolean equals(Object o) {
    boolean equal = false;

    if (null != o && o instanceof SensorType) {
      equal = ((SensorType) o).id == this.id;
    }

    return equal;
  }

  @Override
  public int compareTo(SensorType o) {

    // Compare display order, then sensor type name
    int result = this.displayOrder - o.displayOrder;

    if (result == 0) {
      result = name.compareTo(o.name);
    }

    return result;
  }

  @Override
  public String toString() {
    return "Sensor Type: " + getName();
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

  public ColumnHeader getColumnHeader() {
    return columnHeader;
  }
}
