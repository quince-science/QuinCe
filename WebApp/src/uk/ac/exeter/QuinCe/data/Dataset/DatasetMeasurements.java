package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

  public void addMeasurement(Measurement measurement) {
    if (!measurements.containsKey(measurement.getRunType())) {
      measurements.put(measurement.getRunType(), new ArrayList<Measurement>());
    }

    measurements.get(measurement.getRunType()).add(measurement);
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

  public TreeSet<Measurement> getMeasurementsInSameRun(Measurement start) {

    TreeSet<Measurement> result = new TreeSet<Measurement>(
      Measurement.TIME_COMPARATOR);
    result.add(start);

    int startPos = getTimeOrderedMeasurements().indexOf(start);

    // Search backwards until will find a measurement that has a different run
    // type
    boolean sameRunType = true;
    int searchPos = startPos;
    while (sameRunType) {
      searchPos--;
      if (searchPos < 0 || !getTimeOrderedMeasurements().get(searchPos)
        .getRunType().equals(start.getRunType())) {
        sameRunType = false;
      } else {
        result.add(getTimeOrderedMeasurements().get(searchPos));
      }
    }

    // Search forwards until will find a measurement that has a different run
    // type
    sameRunType = true;
    searchPos = startPos;
    while (sameRunType) {
      searchPos++;
      if (searchPos >= getTimeOrderedMeasurements().size()
        || !getTimeOrderedMeasurements().get(searchPos).getRunType()
          .equals(start.getRunType())) {
        sameRunType = false;
      } else {
        result.add(getTimeOrderedMeasurements().get(searchPos));
      }
    }

    return result;
  }
}
