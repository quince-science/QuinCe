package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Represents a value retrieved from a {@link SensorValuesList}.
 */
public interface SensorValuesListValue {

  /**
   * Indicates that this value holds a {@link String}.
   */
  public static final int STRING_TYPE = 0;

  /**
   * Indicates that this value holds a {@link Double}.
   */
  public static final int DOUBLE_TYPE = 1;

  /**
   * Get the {@link SensorType} for the value.
   *
   * @return The {@link SensorType}.
   */
  public SensorType getSensorType();

  /**
   * Get the QC flag for the value.
   *
   * @return The QC flag.
   */
  public Flag getQCFlag();

  /**
   * Get the QC message for the value.
   *
   * @return The QC message.
   */
  public String getQCMessage();

  /**
   * Get the value as a {@link Double}.
   *
   * <p>
   * This will only give a result if the underlying data contains a numeric
   * value. Otherwise an {@link IncorrectValueTypeException} is thrown.
   * </p>
   *
   * @return The {@link Double) value.
   *
   */
  public Double getDoubleValue();

  /**
   * Get the {@link String} value.
   *
   * <p>
   * This will only give a result if the underlying data <i>does not</i> contain
   * a numeric value. Otherwise an {@link IncorrectValueTypeException} is
   * thrown.
   * </p>
   *
   * @return The {@link String) value.
   */
  public String getStringValue();

  /**
   * Get the source {@link SensorValue}s for this value.
   *
   * <p>
   * The returned {@link Collection} should be unmodifiable. The order of the
   * values is not guaranteed.
   * </p>
   *
   * @return The source {@link SensorValue}s.
   */
  public Collection<SensorValue> getSourceSensorValues();

  /**
   * Determine whether or not this value has been interpolated.
   *
   * @return
   */
  public boolean isInterpolated();

  /**
   * Get the {@link Coordinate} for this value.
   *
   * @return The coordinate.
   */
  public Coordinate getCoordinate();
}
