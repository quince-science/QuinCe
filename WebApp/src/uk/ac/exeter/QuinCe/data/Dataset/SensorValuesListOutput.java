package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Contains a value extracted from a {@link SensorValuesList} via one of the
 * {@code getValue} methods.
 *
 * <p>
 * This may contain an unaltered value or an interpolated value, depending on
 * what's requested, how the list is constructed, and the setup of the
 * underlying instrument.
 * </p>
 *
 * <p>
 * This class provides all the same methods as a {@link SensorValuesListValue},
 * plus other information that the caller may need to know about how the value
 * was constructed.
 * </p>
 */
public interface SensorValuesListOutput extends SensorValuesListValue {

  /**
   * Determine whether or not this SensorValuesListValue has been constructed by
   * interpolating around already flagged {@link SensorValue}s.
   *
   * @return {@code true} if the value interpolates around flagged values;
   *         {@code false} if it does not.
   */
  public boolean interpolatesAroundFlags();

  /**
   * Set the flag indicating that this value is constructed by interpolating
   * around already-flagged {@link SensorValue}s.
   */
  public void setInterpolatesAroundFlags();

  /**
   * Check multiple SensorValuesListOutput objects to see if any of them have
   * their {@link #interpolatesAroundFlag} flag set.
   *
   * @param values
   *          The values to check.
   * @return {@code true} if any value has the {@link #interpolatesAroundFlag}
   *         set; {@code false} otherwise.
   */
  public static boolean interpolatesAroundFlags(
    SensorValuesListOutput... values) {

    boolean result = false;

    for (SensorValuesListOutput value : values) {
      if (null != value && value.interpolatesAroundFlags()) {
        result = true;
        break;
      }
    }

    return result;
  }

}
