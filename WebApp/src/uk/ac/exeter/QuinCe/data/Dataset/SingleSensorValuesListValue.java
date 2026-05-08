package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.Collection;
import java.util.Collections;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Represents a value retrieved from a {@link SensorValuesList}.
 *
 * <p>
 * This default implementation is essentially a wrapper around a
 * {@link SensorValue}. However, there can be more complex implementations that
 * build values from multiple source {@link SensorValues} and provide extra
 * functionality such as interpolation and grouping.
 * </p>
 */
public class SingleSensorValuesListValue implements SensorValuesListValue {

  /**
   * The {@link SensorType} of the value.
   */
  private final SensorType sensorType;

  /**
   * The {@link SensorValue} that this object represents.
   */
  private final SensorValue sensorValue;

  /**
   * QC Flag for this value.
   */
  protected final Flag qcFlag;

  /**
   * QC message for this value.
   */
  private final String qcMessage;

  /**
   * Simple constructor for a single {@link SensorValue}.
   *
   * <p>
   * This constructor extracts all required information, so no processing is
   * performed after this: the getter methods simply return the extracted
   * values.
   * </p>
   *
   * @param sourceSensorValue
   *          The {@link SensorValue}.
   * @param sensorType
   *          The {@link SensorType} for the {@link SensorValue}.
   * @param allSensorValues
   *          All the sensor values from the {@link DataSet} currently being
   *          processed.
   * @throws RoutineException
   *           If the QC information cannot be extracted from the
   *           {@link SensorValue}.
   */
  protected SingleSensorValuesListValue(SensorValue sourceSensorValue,
    SensorType sensorType, DatasetSensorValues allSensorValues)
    throws RoutineException {

    this.sensorType = sensorType;
    this.sensorValue = sourceSensorValue;

    qcFlag = sensorValue.getDisplayFlag(allSensorValues);
    qcMessage = sensorValue.getDisplayQCMessage(allSensorValues);
  }

  protected SingleSensorValuesListValue(SingleSensorValuesListValue source) {
    this.sensorType = source.sensorType;
    this.sensorValue = source.sensorValue;
    this.qcFlag = source.qcFlag;
    this.qcMessage = source.qcMessage;
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

  /**
   * <p>
   * This will only give a result if the underlying {@link SensorValue} contains
   * a numeric value. Otherwise an {@link IncorrectValueTypeException} is
   * thrown.
   * </p>
   *
   * @see SensorValue#isNumeric()
   */
  @Override
  public Double getDoubleValue() {
    if (!sensorValue.isNumeric()) {
      throw new IncorrectValueTypeException(DOUBLE_TYPE);
    }

    return sensorValue.getDoubleValue();
  }

  /**
   * <p>
   * This will only give a result if the underlying {@link SensorValue} <i>does
   * not</i> contain a numeric value. Otherwise an
   * {@link IncorrectValueTypeException} is thrown.
   * </p>
   *
   * @see SensorValue#isNumeric()
   */
  @Override
  public String getStringValue() {
    if (sensorValue.isNumeric()) {
      throw new IncorrectValueTypeException(STRING_TYPE);
    }

    return sensorValue.getValue();
  }

  @Override
  public Collection<SensorValue> getSourceSensorValues() {
    return Collections.singleton(sensorValue);
  }

  @Override
  public boolean isInterpolated() {
    return false;
  }

  @Override
  public Coordinate getCoordinate() {
    return sensorValue.getCoordinate();
  }
}
