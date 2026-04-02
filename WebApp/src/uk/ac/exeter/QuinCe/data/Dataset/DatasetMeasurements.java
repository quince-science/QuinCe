package uk.ac.exeter.QuinCe.data.Dataset;

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
   * A view of all the measurements in {@link Coordinate} order.
   */
  private List<Measurement> orderedMeasurements = null;

  /**
   * The list of all measurement {@link Coordinate}s.
   */
  private List<Coordinate> measurementCoordinates = null;;

  /**
   * Lookup table for the measurements in {@link #timeOrderedMeasurements}.
   */
  private HashMap<Measurement, Integer> measurementIndices = null;

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
      orderedMeasurements = null;
      measurementCoordinates = null;
    }
  }

  /**
   * Get all measurements in time order.
   *
   * @return The time-ordered measurements.
   */
  public List<Measurement> getOrderedMeasurements() {
    if (null == orderedMeasurements) {
      makeOrderedMeasurements();
    }

    return orderedMeasurements;
  }

  /**
   * Get the {@link Coordinate}s of all measurements.
   *
   * @return The measurement coordinates.
   */
  public List<Coordinate> getMeasurementTimes() {
    if (null == orderedMeasurements) {
      makeOrderedMeasurements();
    }

    return measurementCoordinates;
  }

  /**
   * Construct the {@link Coordinate}-ordered view of the measurements.
   */
  private void makeOrderedMeasurements() {

    // Make a unique list of all the measurements
    // Measurements have a natural sort order by time
    TreeSet<Measurement> construction = new TreeSet<Measurement>();
    measurements.values().forEach(construction::addAll);

    orderedMeasurements = new ArrayList<Measurement>(construction);

    measurementIndices = new HashMap<Measurement, Integer>();
    for (int i = 0; i < orderedMeasurements.size(); i++) {
      measurementIndices.put(orderedMeasurements.get(i), i);
    }

    List<Coordinate> coordinates = new ArrayList<Coordinate>(
      construction.size());
    orderedMeasurements.forEach(m -> coordinates.add(m.getCoordinate()));
    measurementCoordinates = Collections.unmodifiableList(coordinates);
  }

  public TreeSet<Measurement> getMeasurementsInSameRun(Variable variable,
    Measurement start) {
    return getMeasurementsInSameRun(variable.getId(), start);
  }

  public TreeSet<Measurement> getMeasurementsInSameRun(long variableId,
    Measurement start) {

    TreeSet<Measurement> result = new TreeSet<Measurement>(
      Measurement.COORDINATE_COMPARATOR);
    result.add(start);

    if (null == orderedMeasurements) {
      makeOrderedMeasurements();
    }

    int startPos = measurementIndices.get(start);

    // Search backwards until will find a measurement that has a different run
    // type
    boolean sameRunType = true;
    int searchPos = startPos;
    while (sameRunType) {
      searchPos--;
      if (searchPos < 0) {
        sameRunType = false;
      } else {
        String searchRunType = getOrderedMeasurements().get(searchPos)
          .getRunType(variableId);
        if (searchPos < 0 || null == searchRunType
          || !searchRunType.equals(start.getRunType(variableId))) {
          sameRunType = false;
        } else {
          result.add(getOrderedMeasurements().get(searchPos));
        }
      }
    }

    // Search forwards until will find a measurement that has a different run
    // type
    sameRunType = true;
    searchPos = startPos;
    while (sameRunType) {
      searchPos++;
      if (searchPos >= getOrderedMeasurements().size()) {
        sameRunType = false;
      } else {
        String searchRunType = getOrderedMeasurements().get(searchPos)
          .getRunType(variableId);
        if (searchPos >= getOrderedMeasurements().size()
          || null == searchRunType
          || !searchRunType.equals(start.getRunType(variableId))) {
          sameRunType = false;
        } else {
          result.add(getOrderedMeasurements().get(searchPos));
        }
      }
    }

    return result;
  }

  public TreeSet<Measurement> getRunBefore(long variableId, String runType,
    Coordinate coordinate) {

    TreeSet<Measurement> result;

    List<Measurement> measurements = getMeasurements(variableId, runType);

    if (null == measurements) {
      result = new TreeSet<Measurement>();
    } else {

      Optional<Measurement> lastBefore = getMeasurements(variableId, runType)
        .stream().filter(m -> m.getCoordinate().isBefore(coordinate))
        .reduce((first, second) -> second);

      result = lastBefore.isEmpty() ? new TreeSet<Measurement>()
        : getMeasurementsInSameRun(variableId, lastBefore.get());
    }

    return result;
  }

  public TreeSet<Measurement> getRunAfter(long variableId, String runType,
    Coordinate coordinate) {

    TreeSet<Measurement> result;

    List<Measurement> measurements = getMeasurements(variableId, runType);

    if (null == measurements) {
      result = new TreeSet<Measurement>();
    } else {
      Optional<Measurement> firstAfter = getMeasurements(variableId, runType)
        .stream().filter(m -> m.getCoordinate().isAfter(coordinate))
        .findFirst();

      result = firstAfter.isEmpty() ? new TreeSet<Measurement>()
        : getMeasurementsInSameRun(variableId, firstAfter.get());
    }

    return result;

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
