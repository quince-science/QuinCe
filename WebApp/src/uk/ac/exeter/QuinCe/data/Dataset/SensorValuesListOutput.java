package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Represents a value extracted from a {@link SensorValuesList}.
 * 
 * <p>
 * This is a wrapper around {@link SensorValuesListValue}, providing all the
 * same functions. However, those functions now take account of the possibility
 * of extract values being interpolated (either around flagged data or missing
 * values at the requested point).
 * </p>
 */
public class SensorValuesListOutput extends SensorValuesListValue {

  /**
   * Indicates whether or not this value is constructed by interpolating around
   * already-flagged {@link SensorValue}s.
   */
  private boolean interpolatesAroundFlag = false;

  /**
   * Determine whether or not this SensorValuesListValue has been constructed by
   * interpolating around already flagged {@link SensorValue}s.
   * 
   * @return {@code true} if the value interpolates around flagged values;
   *         {@code false} if it does not.
   */
  public boolean interpolatesAroundFlag() {
    return interpolatesAroundFlag;
  }

  /**
   * Set the flag indicating that this value is constructed by interpolating
   * around already-flagged {@link SensorValue}s.
   */
  protected void setInterpolatesAroundFlag() {
    interpolatesAroundFlag = true;
  }

  /**
   * Check multiple SensorValuesListOutput objects to see if any of them have
   * their {@link #interpolatesAroundFlag} flag set.
   * 
   * @param values
   *          The values to check.
   * @return {@code true} if any value has the {@link #interpolatesAroundFlag}
   *         set; {@code false} otherwise.
   */
  public static boolean interpolatesAroundFlag(
    SensorValuesListOutput... values) {

    boolean result = false;

    for (SensorValuesListOutput value : values) {
      if (null != value && value.interpolatesAroundFlag) {
        result = true;
        break;
      }
    }

    return result;
  }
}
