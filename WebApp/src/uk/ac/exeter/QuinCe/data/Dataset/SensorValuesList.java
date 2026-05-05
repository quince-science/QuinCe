package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * A list of SensorValue objects with various search capabilities.
 *
 * <p>
 * The list is maintained in {@link Coordinate} order of its members. Attempting
 * to add two {@link SensorValue}s with the same {@link Coordinate} will result
 * in an {@link IllegalArgumentException}. Attempting to add values with
 * different {@link Coordinate} types will also result in an
 * {@link IllegalArgumentException}.
 * </p>
 *
 * <p>
 * {@code get} methods for the list allow access in two ways:
 * </p>
 *
 * <ul>
 * <li><b>Values:</b> Get the values as they should be used for performing data
 * reduction.</li>
 * <li><b>Raw:</b> Get the raw {@link SensorValue} objects.</li>
 * </ul>
 *
 * <p>
 * Methods are named accordingly, e.g. {@code valuesSize} or {@code rawSize}.
 * Typically, QC activities will access the {@code raw} methods and data
 * reduction will access the {@code values} methods.
 * </p>
 */
public abstract class SensorValuesList {

  /**
   * Pre-defined exception thrown when an attempt is made to a
   * {@link SensorValue} with the same {@link Coordinate} as an existing
   * {@link SensorValue}.
   *
   * <p>
   * Note that this is a {@link RuntimeException} and as such must be explicitly
   * caught if required.
   * </p>
   */
  private static final IllegalArgumentException SAME_COORDINATE_EXCEPTION = new IllegalArgumentException(
    "Cannot add two SensorValues with the same coordinate");

  /**
   * Pre-defined exception thrown when an attempt is made to a
   * {@link SensorValue} with a different {@link Coordinate} type to the
   * existing entries in the list.
   *
   * <p>
   * Note that this is a {@link RuntimeException} and as such must be explicitly
   * caught if required.
   * </p>
   *
   * @see Coordinate#getType()
   */
  private static final IllegalArgumentException INVALID_COORDINATE_TYPE_EXCEPTION = new IllegalArgumentException(
    "Cannot add SensorValues with different coordinate types");

  /**
   * An instance of the comparator used to compare two {@link SensorValue}s by
   * their {@link Coordinate}.
   */
  protected static final SensorValueCoordinateComparator COORDINATE_COMPARATOR = new SensorValueCoordinateComparator();

  /**
   * The complete set of sensor values for the current dataset.
   */
  protected final DatasetSensorValues allSensorValues;

  /**
   * The list of values.
   */
  protected final ArrayList<SensorValue> list = new ArrayList<SensorValue>();

  /**
   * A cached copy of the {@link Coordinate}s from the {@link SensorValues}
   * stored in {@link #list}.
   */
  protected List<Coordinate> rawCoordinates = null;

  /**
   * A cached copy of the {@link Coordinate}s from the {@link #outputValues}.
   */
  private List<Coordinate> outputCoordinates = null;

  /**
   * The list of {@link FileColumn} database IDs whose {@link SensorValue}s are
   * allowed to be members of this list.
   */
  private final TreeSet<Long> columnIds;

  /**
   * The {@link SensorType} of values in this list.
   */
  protected final SensorType sensorType;

  /**
   * Indicate whether or not all values in the list should be forced as String
   * values.
   *
   * If this is set to {@code false}, the list will automatically decide whether
   * to use String or Double values.
   */
  private final boolean forceString;

  /**
   * Create a list for a single file column.
   *
   * @param sensorAssignments
   *          The instrument's sensor assignments, for checking
   *          {@link SensorType}s.
   * @param columnId
   *          The column's database ID.
   * @throws RecordNotFoundException
   *           If the {@link SensorType} for the column cannot be established.
   */
  public SensorValuesList(long columnId, DatasetSensorValues allSensorValues,
    boolean forceString) throws RecordNotFoundException {
    columnIds = new TreeSet<Long>();
    columnIds.add(columnId);
    this.allSensorValues = allSensorValues;
    this.sensorType = allSensorValues.getInstrument().getSensorAssignments()
      .getSensorTypeForDBColumn(columnId);
    this.forceString = forceString;
  }

  /**
   * Create a list for a set of file columns.
   *
   * <p>
   * Note that all columns must be of the same {@link SensorType}, otherwise an
   * {@link IllegalArgumentException} will be thrown.
   * </p>
   *
   * @param sensorAssignments
   *          The instrument's sensor assignments, for checking
   *          {@link SensorType}s.
   * @param columnIds
   *          The columns' database IDs.
   * @throws RecordNotFoundException
   *           If the {@link SensorType} for any column cannot be established.
   */
  public SensorValuesList(Collection<Long> columnIds,
    DatasetSensorValues allSensorValues, boolean forceString)
    throws RecordNotFoundException {

    SensorType testingSensorType = null;
    SensorAssignments sensorAssignments = allSensorValues.getInstrument()
      .getSensorAssignments();

    for (long columnId : columnIds) {
      if (null == testingSensorType) {
        testingSensorType = sensorAssignments
          .getSensorTypeForDBColumn(columnId);
      } else {
        if (!sensorAssignments.getSensorTypeForDBColumn(columnId)
          .equals(testingSensorType)) {
          throw new IllegalArgumentException(
            "All column IDs must be for the same SensorType");
        }
      }
    }

    this.columnIds = new TreeSet<Long>(columnIds);
    this.sensorType = testingSensorType;
    this.allSensorValues = allSensorValues;
    this.forceString = forceString;
  }

  /**
   * Factory method to build a list directly from a collection of
   * {@link SensorValue}s.
   *
   * <p>
   * All the values in the passed in {@link Collection} must be of the same
   * {@link SensorType}, and have unique {@link Coordinate}s. Otherwise an
   * {@link IllegalArgumentException} will be thrown.
   * </p>
   *
   * @param values
   *          The sensor values.
   * @return The constructed list.
   * @throws RecordNotFoundException
   *           If the {@link SensorType}s for any of the values cannot be
   *           established.
   */
  public static SensorValuesList newFromSensorValueCollection(
    Collection<SensorValue> values, DatasetSensorValues allSensorValues,
    boolean forceString) throws RecordNotFoundException {

    TreeSet<Long> columnIds = values.stream().map(SensorValue::getColumnId)
      .collect(Collectors.toCollection(TreeSet::new));

    SensorValuesList list = SensorValuesListFactory
      .makeSensorValuesList(columnIds, allSensorValues, forceString);
    list.addAll(values);

    return list;
  }

  /**
   * Add a {@link SensorValue} to the list.
   *
   * <p>
   * Attempting to add a value from a column other than those listed in
   * {@link #columnIds} will result in an {@link IllegalArgumentException}.
   * Attempting to add a value with a {@link Coordinate} identical to a value
   * already in the list will also cause an {@link IllegalArgumentException}.
   * </p>
   *
   * @param value
   *          The value to add.
   */
  public void add(SensorValue value) {

    // Null values are not allowed
    if (null == value) {
      throw new IllegalArgumentException("null values are not permitted");
    }

    // The value must have a columnId that matches one of the specified
    // columnIDs
    if (!columnIds.contains(value.getColumnId())) {
      throw new IllegalArgumentException("Invalid column ID");
    }

    if (list.size() == 0) {
      list.add(value);
    } else if (list.get(0).getCoordinate().getType() != value.getCoordinate()
      .getType()) {
      // Values with different coordinate types are not allowed
      throw INVALID_COORDINATE_TYPE_EXCEPTION;
    } else {
      int lastComparison = COORDINATE_COMPARATOR.compare(value, last());

      if (lastComparison == 0) {
        // Values with identical Coordinates are not allowed
        throw SAME_COORDINATE_EXCEPTION;
      } else if (lastComparison > 0) {
        // The value being added is after the last value, so we just add it to
        // the end.
        list.add(value);
      } else {

        int binarySearchResult = Collections.binarySearch(list, value,
          COORDINATE_COMPARATOR);

        // Values with the identical Coordinates are not allowed
        if (binarySearchResult >= 0) {
          throw SAME_COORDINATE_EXCEPTION;
        } else {
          list.add((binarySearchResult * -1) - 1, value);
        }
      }
    }

    resetOutput();
  }

  /**
   * Add all the supplied {@link SensorValue}s to the list.
   *
   * <p>
   * All the restrictions for adding values enforced in the
   * {@link #add(SensorValue)} method apply.
   * </p>
   *
   * @param values
   *          The values to add.
   */
  private void addAll(Collection<? extends SensorValue> values) {
    values.forEach(this::add);
  }

  /**
   * Add the contents of another {@link SensorValuesList} to this list.
   *
   * <p>
   * All the restrictions for adding values enforced in the
   * {@link #add(SensorValue)} method apply.
   * </p>
   *
   * @param values
   *          The list of values to add.
   */
  public void addAll(SensorValuesList values) {
    if (null != values) {
      values.list.forEach(this::add);
    }
  }

  /**
   * Remove a value from the list.
   *
   * @param sensorValue
   *          The value to remove.
   * @return If the list was changed.
   */
  public boolean remove(SensorValue sensorValue) {
    return list.remove(sensorValue);
  }

  /**
   * Determine whether or not the list is empty.
   *
   * @return {@code true} if the list is empty; {@code false} if it contains any
   *         values.
   */
  public boolean isEmpty() {
    return list.isEmpty();
  }

  /**
   * Get the value at the specified {@link Coordinate}.
   *
   * <p>
   * The {@code interpolate} flag will not be honoured if the coordinate type
   * does not support interpolation.
   * </p>
   *
   * <p>
   * Return {@code null} if no matching value can be found.
   *
   * @param coordinate
   *          The coordinate.
   * @param interpolate
   *          Indicates whether or not interpolation should be used to construct
   *          a value for the coordinate.
   * @return The matching value.
   * @throws SensorValuesListException
   *
   */
  public abstract SensorValuesListOutput getValue(Coordinate coordinate,
    boolean interpolate) throws SensorValuesListException;

  /**
   * Get the set of output values for the list.
   *
   * <p>
   * The output values are constructed according to the measurement mode of the
   * list and the QC flags of the member {@link SensorValue}s.
   * </p>
   *
   * @return The output values.
   * @throws SensorValuesListException
   *
   * @see #getOutputValues()
   */
  public List<SensorValuesListValue> getValues()
    throws SensorValuesListException {

    return Collections.unmodifiableList(getOutputValues());
  }

  /**
   * Get the {@link Coordinate}s of the output values for the list.
   *
   * @return The output coordinates.
   * @throws SensorValuesListException
   * @see #getOutputCoordinates()
   */
  public List<Coordinate> getValueCoordinates()
    throws SensorValuesListException {

    return Collections.unmodifiableList(getOutputCoordinates());
  }

  /**
   * Returns the number of values in the list taking into account any averaging
   * performed for PERIODIC mode.
   *
   * @return The number of values in the list.
   * @throws SensorValuesListException
   *           If the output values cannot be constructed.
   */
  public int valuesSize() throws SensorValuesListException {
    return getOutputValues().size();
  }

  /**
   * Get the last value in the list.
   *
   * @return The last value.
   */
  private SensorValue last() {
    return list.get(list.size() - 1);
  }

  public List<Coordinate> getRawCoordinates() {
    if (null == rawCoordinates) {
      buildRawCoordinates();
    }

    return rawCoordinates;
  }

  private void buildRawCoordinates() {
    rawCoordinates = list.stream().map(SensorValue::getCoordinate).toList();
  }

  /**
   * Get all the raw {@link SensorValue} objects in the list.
   *
   * <p>
   * This allows access to the individual {@link SensorValue} objects without
   * being able to modify the overall list.
   * </p>
   *
   * @return The {@link SensorValue}s in the list.
   */
  public List<SensorValue> getRawValues() {
    return Collections.unmodifiableList(list);
  }

  /**
   * Get a single {@link SensorValue} from the list referenced by
   * {@link Coordinate}.
   *
   * <p>
   * This method should be used for QC purposes. To get values for use in data
   * reduction, use {@link #getMeasurementValue(Coordinate)}.
   * </p>
   *
   * @param time
   *          The desired {@link Coordinate}.
   * @return The value with the specified {@link Coordinate}, or {@code null} if
   *         there is not one.
   */
  public SensorValue getRawSensorValue(Coordinate coordinate, long columnId) {
    int searchIndex = Collections.binarySearch(list,
      makeDummySensorValue(coordinate, columnId));
    return searchIndex < 0 ? null : list.get(searchIndex);
  }

  /**
   * Create a dummy value with the specified {@link Coordinate}.
   *
   * <p>
   * Used to search for values based on a {@link Coordinate} for which a value
   * may or may not exist.
   * </p>
   *
   * @param coordinate
   *          The required coordinate.
   * @return The dummy value.
   */
  private SensorValue makeDummySensorValue(Coordinate coordinate,
    long columnId) {
    return new SensorValue(allSensorValues.getDatasetId(),
      allSensorValues.getFlagScheme(), columnId, coordinate, null);
  }

  /**
   * Get the closest raw {@link SensorValue}(s) to the specified time.
   *
   * <p>
   * If there is a value at exactly the specified time, that value is returned.
   * If there is no such value, the values immediately before and after the time
   * are returned, if they exist.
   * </p>
   *
   * @param assignment
   *          The column to search.
   * @param time
   *          The time.
   * @return The closest values.
   */
  public List<SensorValue> getClosestSensorValues(Coordinate coordinate) {
    List<SensorValue> result = new ArrayList<SensorValue>(2);

    int searchIndex = Collections.binarySearch(getRawCoordinates(), coordinate);
    if (searchIndex >= 0) {
      result.add(list.get(searchIndex));
    } else {
      int priorIndex = Math.abs(searchIndex) - 2;
      int postIndex = Math.abs(searchIndex) - 1;

      if (priorIndex >= 0) {
        result.add(list.get(priorIndex));
      }

      if (postIndex < list.size()) {
        result.add(list.get(postIndex));
      }
    }

    return result;
  }

  /**
   * Get the number of individual {@link SensorValue} objects in the list.
   *
   * <p>
   * Note that this may differ from the number of values returned by
   * {@link #size()} method, depending on the measurement mode.
   * </p>
   *
   * @return The total number of {@link SensorValue}s in the list.
   */
  public int rawSize() {
    return list.size();
  }

  /**
   * Determine whether or not the list contains a {@link SensorValue} with the
   * specified {@link Coordinate}.
   *
   * @param time
   *          The coordinate.
   * @return {@code true} if the list contains a {@link SensorValue} with the
   *         specified {@link Coordinate}; {@code false} otherwise.
   */
  public boolean containsCoordinate(Coordinate coordinate) {
    return Collections.binarySearch(getRawCoordinates(), coordinate) >= 0;
  }

  /**
   * Get a value from the list that has a {@link Coordinate} matching the
   * specified coordinate, or the value immediately before it.
   *
   * @param coordinate
   *          The desired coordinate.
   * @return The matched value.
   * @throws SensorValuesListException
   */
  public SensorValuesListValue getValueOnOrBefore(Coordinate coordinate)
    throws SensorValuesListException {

    SensorValuesListValue result = null;

    List<Coordinate> coordinates = getValueCoordinates();
    int searchIndex = Collections.binarySearch(coordinates, coordinate);

    // A >= 0 = an exact match
    if (searchIndex >= 0) {
      result = getOutputValues().get(searchIndex);
    } else {

      /*
       * If we get a -1 result, there is no value before the requested time. So
       * we don't return anything. Otherwise we return the value before the
       * returned insertion point.
       */
      if (searchIndex < -1) {
        int getIndex = Math.abs(searchIndex) - 2;
        if (getIndex >= 0) {
          result = getOutputValues().get(getIndex);
        }
      }
    }

    return result;
  }

  /**
   * Clear any already calculated output values, forcing them to be recalculated
   * at the next call to {@code getValue} methods.
   */
  public void resetOutput() {
    rawCoordinates = null;
    listContentsUpdated();
  }

  /**
   * Retrieve the constructed output values from the list.
   *
   * <p>
   * Most instances of this class do not simply return a list of values that
   * directly corresponds to the individual members supplied through the
   * {@code add} methods. This method must be implemented to ensure that the
   * outputs from the list have the necessary processing applied.
   * </p>
   *
   * @return The computed output values of the list.
   */
  protected abstract List<? extends SensorValuesListValue> getOutputValues()
    throws SensorValuesListException;

  /**
   * The {@link Coordinate}s of the constructed output values from the list.
   *
   * @return The {@link Coordinate}s of the computed output values of the list.
   * @throws SensorValuesListException
   * @see #getOutputValues()
   */
  protected List<Coordinate> getOutputCoordinates()
    throws SensorValuesListException {
    if (null == outputCoordinates) {
      outputCoordinates = getOutputValues().stream().map(v -> v.getCoordinate())
        .toList();
    }

    return outputCoordinates;
  };

  /**
   * Method to broadcast the fact that the base contents of the list have been
   * changed.
   *
   * <p>
   * When the contents of the list are changed, this method is called to signal
   * that any previously computed output values may now be invalid and should be
   * recalculated.
   * </p>
   */
  protected abstract void listContentsUpdated();

  public List<SensorValuesListValue> getValuesBetween(Coordinate coord1,
    Coordinate coord2) throws SensorValuesListException {

    List<SensorValuesListValue> result = new ArrayList<SensorValuesListValue>();

    // If the two time are equal, there are no values between
    if (!coord1.equals(coord2)) {

      int step = (coord1.isBefore(coord2) ? 1 : -1);

      List<Coordinate> coords = getOutputCoordinates();
      int index = coords.indexOf(coord1);

      boolean stop = false;

      /*
       * The test for stopping is
       * "Have we gone past time2 or fallen off the end of the list?". The test
       * is flipped depending on the direction of search.
       */
      IntPredicate stopTest = coord1.isBefore(coord2)
        ? (x) -> x >= coords.size() || coords.get(x).isAfter(coord2)
        : (x) -> x < 0 || coords.get(x).isBefore(coord2);

      while (!stop) {
        index += step;

        if (stopTest.test(index)) {
          stop = true;
        } else {
          result.add(getValue(coords.get(index), false));
        }
      }
    }

    return result;
  }
}

/**
 * Comparator class that compares two {@link SensorValue} objects by their
 * {@link Coordinate}.
 */
class SensorValueCoordinateComparator implements Comparator<SensorValue> {
  @Override
  public int compare(SensorValue o1, SensorValue o2) {
    return o1.getCoordinate().compareTo(o2.getCoordinate());
  }
}
