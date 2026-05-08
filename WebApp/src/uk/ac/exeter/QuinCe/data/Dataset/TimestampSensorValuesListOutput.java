package uk.ac.exeter.QuinCe.data.Dataset;

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
public class TimestampSensorValuesListOutput
  extends TimestampSensorValuesListValue implements SensorValuesListOutput {

  /**
   * Indicates whether or not this value is constructed by interpolating around
   * already-flagged {@link SensorValue}s.
   */
  private boolean interpolatesAroundFlags = false;

  public TimestampSensorValuesListOutput(TimeCoordinate startTime,
    TimeCoordinate endTime, TimeCoordinate nominalTime,
    Collection<SensorValue> usedValues, SensorType sensorType, Double mean,
    Flag chosenFlag, String collectionToDelimited,
    boolean interpolatesAroundFlags) {

    super(startTime, endTime, nominalTime, usedValues, sensorType, mean,
      chosenFlag, collectionToDelimited);

    this.interpolatesAroundFlags = interpolatesAroundFlags;
  }

  protected TimestampSensorValuesListOutput(TimeCoordinate startTime,
    TimeCoordinate endTime, TimeCoordinate nominalTime,
    Collection<SensorValue> sourceSensorValues, SensorType sensorType,
    String value, Flag qcFlag, String qcMessage,
    boolean interpolatesAroundFlags) {

    super(startTime, endTime, nominalTime, sourceSensorValues, sensorType,
      value, qcFlag, qcMessage);

    this.interpolatesAroundFlags = interpolatesAroundFlags;
  }

  public TimestampSensorValuesListOutput(
    TimestampSensorValuesListValue sourceValue,
    boolean interpolatesAroundFlags) {
    super(sourceValue);
    this.interpolatesAroundFlags = interpolatesAroundFlags;
  }

  public TimestampSensorValuesListOutput(
    TimestampSensorValuesListValue sourceValue, TimeCoordinate nominalTime,
    boolean interpolatesAroundFlags) {
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
  @Override
  public boolean interpolatesAroundFlags() {
    return interpolatesAroundFlags;
  }

  /**
   * Set the flag indicating that this value is constructed by interpolating
   * around already-flagged {@link SensorValue}s.
   */
  @Override
  public void setInterpolatesAroundFlags() {
    interpolatesAroundFlags = true;
  }
}
