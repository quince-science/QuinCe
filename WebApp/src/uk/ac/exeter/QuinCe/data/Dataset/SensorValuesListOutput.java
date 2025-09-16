package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

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
public class SensorValuesListOutput extends SensorValuesListValue {

  /**
   * Indicates whether or not this value is constructed by interpolating around
   * already-flagged {@link SensorValue}s.
   */
  private boolean interpolatesAroundFlags = false;

  public SensorValuesListOutput(LocalDateTime startTime, LocalDateTime endTime,
    LocalDateTime nominalTime, Collection<SensorValue> usedValues,
    SensorType sensorType, Double mean, Flag chosenFlag,
    String collectionToDelimited, boolean interpolatesAroundFlags) {

    super(startTime, endTime, nominalTime, usedValues, sensorType, mean,
      chosenFlag, collectionToDelimited);

    this.interpolatesAroundFlags = interpolatesAroundFlags;
  }

  protected SensorValuesListOutput(LocalDateTime startTime,
    LocalDateTime endTime, LocalDateTime nominalTime,
    Collection<SensorValue> sourceSensorValues, SensorType sensorType,
    String value, Flag qcFlag, String qcMessage,
    boolean interpolatesAroundFlags) {

    super(startTime, endTime, nominalTime, sourceSensorValues, sensorType,
      value, qcFlag, qcMessage);

    this.interpolatesAroundFlags = interpolatesAroundFlags;
  }

  public SensorValuesListOutput(SensorValuesListValue sourceValue,
    boolean interpolatesAroundFlags) {
    super(sourceValue);
    this.interpolatesAroundFlags = interpolatesAroundFlags;
  }

  public SensorValuesListOutput(SensorValuesListValue sourceValue,
    LocalDateTime nominalTime, boolean interpolatesAroundFlags) {
    super(sourceValue, nominalTime);
    this.interpolatesAroundFlags = interpolatesAroundFlags;
  }

  /**
   * Determine whether or not this SensorValuesListValue has been constructed by
   * interpolating around already flagged {@link SensorValue}s.
   *
   * @return {@code true} if the value interpolates around flagged values;
   *         {@code false} if it does not.
   */
  public boolean interpolatesAroundFlags() {
    return interpolatesAroundFlags;
  }

  /**
   * Set the flag indicating that this value is constructed by interpolating
   * around already-flagged {@link SensorValue}s.
   */
  protected void setInterpolatesAroundFlags() {
    interpolatesAroundFlags = true;
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
  protected static boolean interpolatesAroundFlags(
    SensorValuesListOutput... values) {

    boolean result = false;

    for (SensorValuesListOutput value : values) {
      if (null != value && value.interpolatesAroundFlags) {
        result = true;
        break;
      }
    }

    return result;
  }
}
