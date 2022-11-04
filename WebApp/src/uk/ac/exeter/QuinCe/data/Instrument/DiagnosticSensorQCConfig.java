package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

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
   * sensor's flag is set. Different sensors can be triggered for different
   * {@link Variable}s.
   */
  private TreeMap<SensorAssignment, TreeSet<Variable>> flagCascades;

  /**
   * Basic constructor.
   */
  public DiagnosticSensorQCConfig() {
    flagCascades = new TreeMap<SensorAssignment, TreeSet<Variable>>();
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
  protected void setMeasurementSensorVariables(
    SensorAssignment measurementSensor, Collection<Variable> variables) {

    if (null == variables || variables.size() == 0) {
      flagCascades.remove(measurementSensor);
    } else {
      flagCascades.put(measurementSensor, new TreeSet<Variable>(variables));
    }
  }

  protected TreeSet<Variable> getMeasurementSensorVariables(
    SensorAssignment measurementSensor) {
    return flagCascades.containsKey(measurementSensor)
      ? flagCascades.get(measurementSensor)
      : new TreeSet<Variable>();
  }
}
