package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * The set of measurements for a given {@link DataSet}.
 *
 * <p>
 * The main object is a map grouping the measurements by run type, but a second
 * view of all measurements in time order is accessible through
 * {@link #getTimeOrderedMeasurements()}.
 * </p>
 */
public class DatasetMeasurements {

  /**
   * The measurements mapped by variable and run type.
   *
   * <p>
   * Since a {@link Measurement} may apply to multiple {@link Variable}s, it may
   * appear multiple times in this map.
   * </p>
   */
  private HashMap<VariableRunType, ArrayList<Measurement>> measurements;

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
    measurements = new HashMap<VariableRunType, ArrayList<Measurement>>();
  }

  /**
   * Get the Run Types in this set of measurements.
   *
   * @return The Run Types.
   */
  public Set<String> getRunTypes(Variable variable) {
    return measurements.keySet().stream()
      .filter(k -> k.variableId == variable.getId()).map(k -> k.runType)
      .collect(Collectors.toSet());
  }

  /**
   * Get the measurements for a specified run type.
   *
   * @param runType
   *          The run type.
   * @return The measurements.
   */
  public List<Measurement> getMeasurements(Variable variable, String runType) {
    return measurements.get(new VariableRunType(variable, runType));
  }

  /**
   * Get all the measurements for the specified {@link Variable} with the
   * specified Run Type.
   *
   * @param variableId
   *          The variable's database ID
   * @param runType
   *          The run type
   * @return The matching measurments.
   */
  public List<Measurement> getMeasurements(long variableId, String runType) {
    return measurements.get(new VariableRunType(variableId, runType));
  }

  public void addMeasurement(Measurement measurement) {

    for (Map.Entry<Long, String> runTypeEntry : measurement.getRunTypes()
      .entrySet()) {
      VariableRunType varRunType = new VariableRunType(runTypeEntry);

      if (!measurements.containsKey(varRunType)) {
        measurements.put(varRunType, new ArrayList<Measurement>());
      }

      measurements.get(varRunType).add(measurement);
      timeOrderedMeasurements = null;
      measurementTimes = null;
    }
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

    // Make a unique list of all the measurements
    // Measurements have a natural sort order by time
    TreeSet<Measurement> orderedMeasurements = new TreeSet<Measurement>();
    measurements.values().forEach(orderedMeasurements::addAll);

    timeOrderedMeasurements = new ArrayList<Measurement>(orderedMeasurements);

    List<LocalDateTime> times = new ArrayList<LocalDateTime>(
      timeOrderedMeasurements.size());
    timeOrderedMeasurements.forEach(m -> times.add(m.getTime()));
    measurementTimes = Collections.unmodifiableList(times);
  }

  public TreeSet<Measurement> getMeasurementsInSameRun(Variable variable,
    Measurement start) {
    return getMeasurementsInSameRun(variable.getId(), start);
  }

  public TreeSet<Measurement> getMeasurementsInSameRun(long variableId,
    Measurement start) {

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
      if (searchPos < 0) {
        sameRunType = false;
      } else {
        String searchRunType = getTimeOrderedMeasurements().get(searchPos)
          .getRunType(variableId);
        if (searchPos < 0 || null == searchRunType
          || !searchRunType.equals(start.getRunType(variableId))) {
          sameRunType = false;
        } else {
          result.add(getTimeOrderedMeasurements().get(searchPos));
        }
      }
    }

    // Search forwards until will find a measurement that has a different run
    // type
    sameRunType = true;
    searchPos = startPos;
    while (sameRunType) {
      searchPos++;
      if (searchPos >= getTimeOrderedMeasurements().size()) {
        sameRunType = false;
      } else {
        String searchRunType = getTimeOrderedMeasurements().get(searchPos)
          .getRunType(variableId);
        if (searchPos >= getTimeOrderedMeasurements().size()
          || null == searchRunType
          || !searchRunType.equals(start.getRunType(variableId))) {
          sameRunType = false;
        } else {
          result.add(getTimeOrderedMeasurements().get(searchPos));
        }
      }
    }

    return result;
  }

  public TreeSet<Measurement> getRunBefore(long variableId, String runType,
    LocalDateTime time) {

    Optional<Measurement> lastBefore = getMeasurements(variableId, runType)
      .stream().filter(m -> m.getTime().isBefore(time))
      .reduce((first, second) -> second);

    return lastBefore.isEmpty() ? new TreeSet<Measurement>()
      : getMeasurementsInSameRun(variableId, lastBefore.get());

  }

  public TreeSet<Measurement> getRunAfter(long variableId, String runType,
    LocalDateTime time) {

    Optional<Measurement> firstAfter = getMeasurements(variableId, runType)
      .stream().filter(m -> m.getTime().isAfter(time)).findFirst();

    return firstAfter.isEmpty() ? new TreeSet<Measurement>()
      : getMeasurementsInSameRun(variableId, firstAfter.get());

  }

  private static class VariableRunType {
    private final long variableId;
    private final String runType;

    private VariableRunType(long variableId, String runType) {
      this.variableId = variableId;
      this.runType = runType;
    }

    private VariableRunType(Variable variable, String runType) {
      this.variableId = variable.getId();
      this.runType = runType;
    }

    private VariableRunType(Map.Entry<Long, String> mapEntry) {
      this.variableId = mapEntry.getKey();
      this.runType = mapEntry.getValue();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((runType == null) ? 0 : runType.hashCode());
      result = prime * result + (int) (variableId ^ (variableId >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      VariableRunType other = (VariableRunType) obj;
      if (runType == null) {
        if (other.runType != null)
          return false;
      } else if (!runType.equals(other.runType))
        return false;
      if (variableId != other.variableId)
        return false;
      return true;
    }
  }
}
