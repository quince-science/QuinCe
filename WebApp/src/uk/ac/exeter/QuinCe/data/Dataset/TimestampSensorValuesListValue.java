package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Represents a time-based value retrieved from a {@link SensorValuesList}.
 *
 * <p>
 * Values of this type are returned from {@link TimestampSensorValuesList}. Read
 * the documentation for that class for more information about how this class
 * handles values constructed from multiple {@link SensorValue}s.
 * </p>
 *
 * <p>
 * The value contains three times. Start and End times indicate the time range
 * of the source {@link SensorValue}s from which the value has been
 * constructed/calculated, and is useful for PERIODIC measurement modes and
 * interpolated values. The Nominal time is a single reference timestamp for the
 * value. This may be the centre point of a group of values, or the target time
 * of an interpolated value.
 * </p>
 *
 * <p>
 * Note that if the source {@link SensorValuesList} has a PERIODIC measurement
 * mode the calculated value is the mean of multiple source
 * {@link SensorValue}s. We also store the first and last timestamp of those
 * values, which can be used to obtain values from other columns later on.
 * Asking for the 'default' timestamp will give the midpoint between the start
 * and end times.
 * </p>
 *
 * <p>
 * This class can hold either a {@link String} or a {@link Double} value, but
 * not both. Any attempt to access the value type not held will result in an
 * </p>
 *
 * <p>
 * Instances of this class are intended to be atomic and immutable. Objects can
 * be constructed and their properties read, but no changes are permitted.
 * </p>
 *
 * @see TimestampSensorValuesList
 */
public class TimestampSensorValuesListValue implements SensorValuesListValue {
  /**
   * The beginning of the time period covered by this value.
   */
  private final TimeCoordinate startTime;

  /**
   * The end of the time period covered by this value.
   */
  private final TimeCoordinate endTime;

  /**
   * The nominal single timestamp for this value, as a {@link Coordinate}
   * object.
   */
  private final TimeCoordinate nominalTime;

  /**
   * The {@link SensorType} of the value.
   */
  private final SensorType sensorType;

  /**
   * QC Flag for this value.
   */
  private final Flag qcFlag;

  /**
   * QC message for this value.
   */
  private final String qcMessage;

  /**
   * The {@link String} value.
   */
  private final String stringValue;

  /**
   * The {@link Double} value.
   */
  private final Double doubleValue;

  /**
   * The {@link SensorValue}s used to calculate the value.
   *
   * <p>
   * The {@link Collection} is unmodifiable.
   * </p>
   *
   * @see Collections#unmodifiableCollection(Collection)
   */
  private final Collection<SensorValue> sourceSensorValues;

  /**
   * Constructor for a value based on a single {@link SensorValue}. The type and
   * QC flag are determined automatically.
   *
   * @throws RoutineException
   */
  protected TimestampSensorValuesListValue(SensorValue sourceSensorValue,
    SensorType sensorType, DatasetSensorValues allSensorValues)
    throws SensorValuesListException, RoutineException {

    if (!(sourceSensorValue.getCoordinate() instanceof TimeCoordinate)) {
      throw new IllegalArgumentException("Cannot use non-time coordinates");
    }

    this.startTime = (TimeCoordinate) sourceSensorValue.getCoordinate();
    this.endTime = (TimeCoordinate) sourceSensorValue.getCoordinate();
    this.nominalTime = (TimeCoordinate) sourceSensorValue.getCoordinate();
    sourceSensorValues = Collections.singleton(sourceSensorValue);
    this.sensorType = sensorType;

    if (sourceSensorValue.isNumeric()) {
      doubleValue = sourceSensorValue.getDoubleValue();
      qcFlag = sourceSensorValue.getDisplayFlag(allSensorValues);
      qcMessage = sourceSensorValue.getDisplayQCMessage(allSensorValues);
      stringValue = null;
    } else if (null == sourceSensorValue.getValue()) {
      throw new NullPointerException("NULL SensorValues are not permitted");
    } else {
      stringValue = sourceSensorValue.getValue();
      qcFlag = sourceSensorValue.getDisplayFlag(allSensorValues);
      qcMessage = sourceSensorValue.getDisplayQCMessage(allSensorValues);
      doubleValue = null;
    }
  }

  /**
   * Constructor for a value based on multiple {@link SensorValue}s.
   *
   * @param time
   *          The value's timestamp.
   * @param sourceSensorValues
   *          The source {@link SensorValue}s.
   * @param value
   *          The value.
   * @param qcFlag
   *          The value's QC flag.
   * @param qcMessage
   *          The value's QC message.
   */
  protected TimestampSensorValuesListValue(TimeCoordinate startTime,
    TimeCoordinate endTime, TimeCoordinate nominalTime,
    Collection<SensorValue> sourceSensorValues, SensorType sensorType,
    String value, Flag qcFlag, String qcMessage) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.nominalTime = nominalTime;
    this.sourceSensorValues = Collections
      .unmodifiableCollection(sourceSensorValues);
    this.sensorType = sensorType;
    this.stringValue = value;

    // Ensure that all the source sensor values contain the specified string
    // value
    if (sourceSensorValues.stream()
      .anyMatch(v -> !v.getValue().equals(value))) {

      throw new IllegalArgumentException(
        "All SensorValues must equal the passed value");
    }

    this.doubleValue = null;
    this.qcFlag = qcFlag;
    this.qcMessage = qcMessage;
  }

  /**
   * Create a copy of an existing {@link TimestampSensorValuesListValue} with a
   * new nominal time.
   *
   * @param original
   *          The original value.
   * @param nominalTime
   *          The new nominal time.
   */
  protected TimestampSensorValuesListValue(
    TimestampSensorValuesListValue original, TimeCoordinate nominalTime) {

    this.sensorType = original.sensorType;
    this.doubleValue = original.doubleValue;
    this.stringValue = original.stringValue;
    this.startTime = original.startTime;
    this.endTime = original.endTime;
    this.nominalTime = nominalTime;
    this.qcFlag = original.qcFlag;
    this.qcMessage = original.qcMessage;
    this.sourceSensorValues = new ArrayList<SensorValue>(
      original.sourceSensorValues);
  }

  /**
   * Constructor for a value based on multiple {@link SensorValue}s.
   *
   * @param time
   *          The value's timestamp.
   * @param sourceSensorValues
   *          The source {@link SensorValue}s.
   * @param value
   *          The value.
   * @param qcFlag
   *          The value's QC flag.
   * @param qcMessage
   *          The value's QC message.
   */
  protected TimestampSensorValuesListValue(TimeCoordinate startTime,
    TimeCoordinate endTime, TimeCoordinate nominalTime,
    Collection<SensorValue> sourceSensorValues, SensorType sensorType,
    Double value, Flag qcFlag, String qcMessage) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.nominalTime = nominalTime;
    this.sourceSensorValues = Collections
      .unmodifiableCollection(sourceSensorValues);
    this.sensorType = sensorType;
    this.doubleValue = value;
    this.stringValue = null;
    this.qcFlag = qcFlag;
    this.qcMessage = qcMessage;
  }

  /**
   * Copy constructor. Performs a shallow copy on internal values.
   *
   * @param original
   *          The original value.
   */
  protected TimestampSensorValuesListValue(
    TimestampSensorValuesListValue original) {

    this.sensorType = original.sensorType;
    this.doubleValue = original.doubleValue;
    this.stringValue = original.stringValue;
    this.startTime = original.startTime;
    this.endTime = original.endTime;
    this.nominalTime = original.nominalTime;
    this.qcFlag = original.qcFlag;
    this.qcMessage = original.qcMessage;
    this.sourceSensorValues = new ArrayList<SensorValue>(
      original.sourceSensorValues);
  }

  /**
   * Get the value's timestamp. This is the midpoint of the {@link #startTime}
   * and {@link #endTime}.
   *
   * @return The timestamp.
   */
  public LocalDateTime getTime() {
    return DateTimeUtils.midPoint(startTime.getTime(), endTime.getTime());
  }

  /**
   * Get the start time of the period covered by this value.
   *
   * @return The start time.
   */
  public TimeCoordinate getStartTime() {
    return startTime;
  }

  /**
   * Get the end time of the period covered by this value.
   *
   * @return The end time.
   */
  public TimeCoordinate getEndTime() {
    return endTime;
  }

  /**
   * Get the nominal timestamp for this value.
   *
   * @return The nominal timestamp.
   */
  public TimeCoordinate getNominalTime() {
    return nominalTime;
  }

  @Override
  public Collection<SensorValue> getSourceSensorValues() {
    return sourceSensorValues;
  }

  /**
   * Get the {@link String} value.
   *
   * @return The {@link String) value.
   */
  public String getStringValue() {
    if (null == stringValue) {
      throw new IncorrectValueTypeException(STRING_TYPE);
    }
    return stringValue;
  }

  /**
   * Get the {@link Double} value.
   *
   * @return The {@link Double) value.
   */
  public Double getDoubleValue() {
    if (null == doubleValue) {
      throw new IncorrectValueTypeException(DOUBLE_TYPE);
    }

    return doubleValue;
  }

  /**
   * Determine whether or not this is an interpolated value.
   *
   * <p>
   * The value is deemed to be interpolated if (a) there is more than one source
   * {@link SensorValue} or (b) the single source {@link SensorValue} has a
   * different timestamp to that assigned to this value (i.e. the result of
   * calling {@link #getTime()}}.
   * </p>
   *
   * @return {@code true} if this value is interpolated; {@code false} if it is
   *         not.
   */
  @Override
  public boolean isInterpolated() {
    boolean result;

    if (sourceSensorValues.size() > 1) {
      result = true;
    } else if (!sourceSensorValues.stream().findFirst().get().getCoordinate()
      .getTime().equals(getTime())) {
      result = true;
    } else {
      result = false;
    }

    return result;
  }

  /**
   * Determine whether this value holds a {@link String} or a {@link Double}.
   *
   * @return The value's type.
   * @see #STRING_TYPE
   * @see #DOUBLE_TYPE
   */
  public int getType() {
    return null == doubleValue ? STRING_TYPE : DOUBLE_TYPE;
  }

  @Override
  public SensorType getSensorType() {
    return sensorType;
  }

  @Override
  public Flag getQCFlag() {
    return qcFlag;
  }

  @Override
  public String getQCMessage() {
    return qcMessage;
  }

  @Override
  public Coordinate getCoordinate() {
    return nominalTime;
  }

  public boolean encompasses(TimeCoordinate time) {
    return DateTimeUtils.isBetween(time.getTime(), startTime.getTime(),
      endTime.getTime());
  }
}
