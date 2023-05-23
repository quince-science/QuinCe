package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
  private Double rangeMin = null;

  /**
   * The maximum allowed value for the sensor.
   */
  private Double rangeMax = null;

  /**
   * The measurement sensors whose QC flag will be set when this diagnostic
   * sensor's flag is set. Different sensors can be triggered for different Run
   * Types.
   */
  private TreeMap<SensorAssignment, List<String>> affectedRunTypes;

  /**
   * Basic constructor.
   */
  protected DiagnosticSensorQCConfig() {
    affectedRunTypes = new TreeMap<SensorAssignment, List<String>>();
  }

  /**
   * Get the minimum allowed value for the sensor.
   *
   * @return The minimum allowed value.
   */
  protected Double getRangeMin() {
    return rangeMin;
  }

  /**
   * Set the minimum allowed value for the sensor.
   *
   * @param min
   *          The minimum value.
   */
  protected void setRangeMin(Double min) {
    this.rangeMin = min;
    validateRange();
  }

  /**
   * Get the maximum allowed value for the sensor.
   *
   * @return The maximum allowed value.
   */
  protected Double getRangeMax() {
    return rangeMax;
  }

  /**
   * Set the maximum allowed value for the sensor.
   *
   * @param max
   *          The maximum value.
   */
  protected void setRangeMax(Double max) {
    this.rangeMax = max;
    validateRange();
  }

  /**
   * Ensure that the min and max are the right way round.
   */
  private void validateRange() {
    if (null != rangeMin && null != rangeMax) {
      if (rangeMin > rangeMax) {
        double temp = rangeMax;
        rangeMax = rangeMin;
        rangeMin = temp;
      }
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
  protected boolean rangeOK(double value) {
    boolean result;

    if (Double.isNaN(value)) {
      result = true;
    } else {
      if (null == rangeMin && null == rangeMax) {
        result = true;
      } else if (null == rangeMin) {
        result = value <= rangeMax;
      } else if (null == rangeMax) {
        result = value >= rangeMin;
      } else {
        result = value <= rangeMax && value >= rangeMin;
      }
    }

    return result;
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

  protected Map<SensorAssignment, List<String>> getAllAffectedRunTypes() {
    return affectedRunTypes;
  }

  protected boolean anyRunTypeAssigned() {
    return affectedRunTypes.size() > 0;
  }

  public boolean isEmpty() {
    boolean result = true;

    if (null != rangeMin) {
      result = false;
    } else if (null != rangeMax) {
      result = false;
    } else {
      result = !anyRunTypeAssigned();
    }

    return result;
  }
}
