package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Holds information about the QC behaviour of a diagnostic sensor, including
 * its valid range of values and which other sensors are affected by it.
 *
 */
public class DiagnosticSensorQCConfig {

  /**
   * The minimum allowed value for the sensor.
   */
  private double rangeMin = Double.MIN_VALUE;

  /**
   * The maximum allowed value for the sensor.
   */
  private double rangeMax = Double.MAX_VALUE;

  /**
   * The measurement sensors whose QC flag will be set when this diagnostic
   * sensor's flag is set. Different sensors can be triggered for different Run
   * Types.
   */
  private TreeMap<SensorAssignment, List<String>> affectedRunTypes;

  /**
   * Basic constructor.
   */
  public DiagnosticSensorQCConfig() {
    affectedRunTypes = new TreeMap<SensorAssignment, List<String>>();
  }

  /**
   * Set the minimum allowed value for the sensor.
   *
   * @param min
   *          The minimum value.
   */
  public void setRangeMin(double min) {
    this.rangeMin = min;
    validateRange();
  }

  /**
   * Set the maximum allowed value for the sensor.
   *
   * @param max
   *          The maximum value.
   */
  public void setRangeMax(double max) {
    this.rangeMax = max;
    validateRange();
  }

  /**
   * Ensure that the min and max are the right way round.
   */
  private void validateRange() {
    if (rangeMin < rangeMax) {
      double temp = rangeMax;
      rangeMax = rangeMin;
      rangeMin = temp;
    }
  }

  /**
   * Check a value to see if it is within the allowable range of the sensor.
   *
   * @param value
   *          The value to be checked.
   * @return {@code true} if the value is within the sensor's allowable range;
   *         {@code false} if it is not.
   */
  public boolean rangeOK(double value) {
    return !Double.isNaN(value) && value <= rangeMax && value >= rangeMin;
  }

  /**
   * Set the {@link Variable}s for which the given measurement sensor will be
   * affected by flags on the parent diagnostic sensor.
   *
   * @param measurementSensor
   *          The measurement sensor.
   * @param variables
   *          The Variables for which the effects of the parent diagnostic
   *          sensor will occur.
   */
  protected void setMeasurementSensorRunTypes(
    SensorAssignment measurementSensor, Collection<String> runTypes) {

    if (null == runTypes || runTypes.size() == 0) {
      affectedRunTypes.remove(measurementSensor);
    } else {
      affectedRunTypes.put(measurementSensor, new ArrayList<String>(runTypes));
    }
  }

  protected List<String> getMeasurementSensorRunTypes(
    SensorAssignment measurementSensor) {
    return affectedRunTypes.containsKey(measurementSensor)
      ? affectedRunTypes.get(measurementSensor)
      : new ArrayList<String>(0);
  }

  protected boolean anyRunTypeAssigned() {
    return affectedRunTypes.size() > 0;
  }
}
