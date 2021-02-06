package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * The set of measurements for a given {@link Dataset}.
 * 
 * <p>
 * The main object is a map grouping the measurements by run type, but a second
 * view of all measurements in time order is accessible through
 * {@link #getTimeOrderedMeasurements()}.
 * </p>
 * 
 * @author stevej
 *
 */
public class DatasetMeasurements {

  /**
   * The measurements mapped by run type.
   */
  private HashMap<String, ArrayList<Measurement>> measurements;

  /**
   * A view of all the measurements in time order.
   */
  private List<Measurement> timeOrderedMeasurements = null;

  /**
   * The list of all measurement times.
   */
  private List<LocalDateTime> measurementTimes = null;;

  /**
   * Basic constructor.
   */
  public DatasetMeasurements() {
    measurements = new HashMap<String, ArrayList<Measurement>>();
  }

  /**
   * Get the Run Types in this set of measurements.
   * 
   * @return The Run Types.
   */
  public Set<String> getRunTypes() {
    return measurements.keySet();
  }

  /**
   * Get the measurements for a specified run type.
   * 
   * @param runType
   *          The run type.
   * @return The measurements.
   */
  public List<Measurement> getMeasurements(String runType) {
    return measurements.get(runType);
  }

  public void addMeasurement(String runType, Measurement measurement) {
    if (!measurements.containsKey(runType)) {
      measurements.put(runType, new ArrayList<Measurement>());
    }

    measurements.get(runType).add(measurement);
    timeOrderedMeasurements = null;
    measurementTimes = null;
  }

  /**
   * Get all measurements in time order.
   * 
   * @return The time-ordered measurements.
   */
  public List<Measurement> getTimeOrderedMeasurements() {
    if (null == timeOrderedMeasurements) {
      makeTimeOrderedMeasurements();
    }

    return timeOrderedMeasurements;
  }

  /**
   * Get the times of all measurements.
   * 
   * @return The measurement times.
   */
  public List<LocalDateTime> getMeasurementTimes() {
    if (null == timeOrderedMeasurements) {
      makeTimeOrderedMeasurements();
    }

    return measurementTimes;
  }

  /**
   * Construct the time-ordered view of the measurements.
   */
  private void makeTimeOrderedMeasurements() {

    List<Measurement> orderedMeasurements = new ArrayList<Measurement>();
    measurements.values().forEach(orderedMeasurements::addAll);

    Collections.sort(orderedMeasurements);
    timeOrderedMeasurements = Collections.unmodifiableList(orderedMeasurements);

    List<LocalDateTime> times = new ArrayList<LocalDateTime>(
      timeOrderedMeasurements.size());
    timeOrderedMeasurements.forEach(m -> times.add(m.getTime()));
    measurementTimes = Collections.unmodifiableList(times);
  }

}
