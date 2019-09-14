package uk.ac.exeter.QuinCe.data.Instrument.RunTypes;

/**
 * Defines a Run Type Category
 * 
 * @author Steve Jones
 *
 */
public class RunTypeCategory implements Comparable<RunTypeCategory> {

  /**
   * Code to indicate that a run type has not been assigned. Has no associated
   * RunTypeCategory object.
   */
  public static final long NOT_ASSIGNED = -999L;

  /**
   * Category value for ignored run types
   */
  public static final long IGNORED_TYPE = -1L;

  /**
   * Category value for ignored run types
   */
  public static final long ALIAS_TYPE = -2L;

  /**
   * Category value for internal calibration run types
   */
  public static final long INTERNAL_CALIBRATION_TYPE = -3L;

  /**
   * The special IGNORED run type category
   */
  public static RunTypeCategory IGNORED = null;

  /**
   * The special External Standard run type category
   */
  public static RunTypeCategory INTERNAL_CALIBRATION = null;

  /**
   * The special ALIAS run type category
   */
  public static RunTypeCategory ALIAS = null;

  /**
   * The category type
   */
  private final long type;

  /**
   * The description of this category. Usually a variable name, expect for the
   * special types
   */
  private final String description;

  // Set up the special alias types
  static {
    IGNORED = new RunTypeCategory(IGNORED_TYPE, "Ignored");
    ALIAS = new RunTypeCategory(ALIAS_TYPE, "Alias");
    INTERNAL_CALIBRATION = new RunTypeCategory(INTERNAL_CALIBRATION_TYPE,
      "Internal Calibration");
  }

  /**
   * Basic constructor
   * 
   * @param code
   *          The category code - either a variable ID, or a special code as
   *          above
   * @param name
   *          The category description - usually the variable name
   * @throws InvalidCategoryTypeException
   *           If the type is invalid
   */
  protected RunTypeCategory(long type, String description) {
    this.type = type;
    this.description = description;
  }

  /**
   * Get the category type
   * 
   * @return The category type
   */
  public long getType() {
    return type;
  }

  /**
   * Get the category description
   * 
   * @return The category description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Determine whether or not this run type is for measurements
   * 
   * @return {@code true} if this is a measurement type; {@code false} otherwise
   */
  public boolean isMeasurementType() {
    return type > 0;
  }

  @Override
  public boolean equals(Object o) {
    boolean equals = false;

    if (o instanceof RunTypeCategory) {
      RunTypeCategory oRunTypeCategory = (RunTypeCategory) o;
      equals = oRunTypeCategory.type == type;
    }

    return equals;
  }

  @Override
  public String toString() {
    return type + ":" + description;
  }

  @Override
  public int compareTo(RunTypeCategory o) {
    return description.compareTo(o.description);
  }

  /**
   * Determine whether or not this category references a variable
   * 
   * @return
   */
  public boolean isVariable() {
    return type > 0;
  }
}
