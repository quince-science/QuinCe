package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.InvalidFlagException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues.AutoQCResult;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;

/**
 * Represents a single value from a sensor.
 */
public class SensorValue implements Comparable<SensorValue>, Cloneable {

  /**
   * Special value indicating a matched SensorValue that has no value.
   */
  public static final String NO_VALUE = String.valueOf(Long.MIN_VALUE);

  /**
   * The QC comment used for missing values
   */
  public static final String MISSING_QC_COMMENT = "Missing";

  /**
   * The {@link FlagScheme} being used for this value.
   */
  private FlagScheme flagScheme;

  /**
   * The database ID of this value
   */
  private long id;

  /**
   * The ID of the dataset that the sensor value is in
   */
  private final long datasetId;

  /**
   * The ID of the column that the value is in. Either the ID of a row in the
   * {@code file_column} table, or a special value (e.g. for lon/lat)
   */
  private final long columnId;

  /**
   * The coordinate for this value.
   */
  private final Coordinate coordinate;

  /**
   * The automatic QC result
   */
  private AutoQCResult autoQC = null;

  /**
   * The user QC flag
   */
  private Flag userQCFlag = null;

  /**
   * The user QC message
   */
  private String userQCMessage = null;

  /**
   * The value (can be null)
   */
  private String value;

  /**
   * Cache of the value as a {@link Double}.
   */
  private Double doubleValue = null;

  /**
   * Indicates whether the value needs to be saved to the database
   */
  private boolean dirty;

  /**
   * Indicates whether or not this value can be saved to the database.
   */
  private final boolean canBeSaved;

  /**
   * Build a sensor value with default QC flags
   *
   * @param datasetId
   * @param columnId
   * @param time
   * @param value
   */
  public SensorValue(long datasetId, FlagScheme flagScheme, long columnId,
    Coordinate coordinate, String value) {

    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = datasetId;
    this.flagScheme = flagScheme;
    this.columnId = columnId;
    this.coordinate = coordinate;
    this.value = value;
    this.autoQC = new AutoQCResult(flagScheme);
    this.dirty = true;
    this.canBeSaved = true;

    if (null == value) {
      this.userQCFlag = flagScheme.getBadFlag();
      this.userQCMessage = MISSING_QC_COMMENT;
    } else {
      this.userQCFlag = flagScheme.getAssumedGoodFlag();
    }
  }

  /**
   * Build a sensor value with default QC flags
   *
   * @param datasetId
   * @param columnId
   * @param time
   * @param value
   */
  public SensorValue(long databaseId, long datasetId, FlagScheme flagScheme,
    long columnId, Coordinate coordinate, String value, AutoQCResult autoQc,
    Flag userQcFlag, String userQcMessage) {

    this.id = databaseId;
    this.flagScheme = flagScheme;
    this.datasetId = datasetId;
    this.columnId = columnId;
    this.coordinate = coordinate;
    this.value = value;

    if (null == autoQc) {
      this.autoQC = new AutoQCResult(flagScheme);
    } else {
      this.autoQC = autoQc;
    }

    this.userQCFlag = userQcFlag;
    this.userQCMessage = userQcMessage;
    this.dirty = false;
    this.canBeSaved = true;
  }

  /**
   * Create a copy of an existing {@code SensorValue} but with a new timestamp.
   *
   * {@code SensorValue}s created with this constructor cannot be saved to the
   * database.
   *
   * @param source
   *          The source {@code SensorValue}.
   * @param newTime
   *          The new timestamp.
   */
  public SensorValue(SensorValue source, Coordinate newCoordinate) {
    this.id = source.id;
    this.datasetId = source.datasetId;
    this.flagScheme = source.flagScheme;
    this.columnId = source.columnId;
    this.autoQC = source.autoQC;
    this.userQCFlag = source.userQCFlag;
    this.userQCMessage = source.userQCMessage;
    this.value = source.value;
    this.dirty = false;

    this.coordinate = newCoordinate;
    this.canBeSaved = false;
  }

  /**
   * Get the database ID of the dataset to which this value belongs
   *
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Get the database ID of the file column from which this value was extracted
   *
   * @return The column ID
   */
  public long getColumnId() {
    return columnId;
  }

  /**
   * Get the {@link Coordinate} for this value.
   *
   * @return The coordinate
   */
  public Coordinate getCoordinate() {
    return coordinate;
  }

  /**
   * Get the measured value in its original string format
   *
   * @return The value
   */
  public String getValue() {
    return value;
  }

  /**
   * Get the value as a Double. No error checking is performed. Returns
   * {@code null} if the value is {@code null}. All commas are removed from
   * number before parsing
   *
   * @return The value as a Double
   */
  public Double getDoubleValue() {
    if (null == doubleValue) {
      doubleValue = StringUtils.doubleFromString(value);
    }

    return doubleValue;
  }

  /**
   * Indicates whether or not this value is {@code null}.
   *
   * @return {@code true} if the value is null; {@code false} otherwise.
   */
  public boolean isNaN() {
    return getDoubleValue().isNaN();
  }

  /**
   * Get the overall QC flag resulting from the automatic QC. This will be the
   * most significant flag generated by all QC routines run on the value.
   *
   * @return The automatic QC flag
   */
  public Flag getAutoQcFlag() {
    return autoQC.getOverallFlag();
  }

  /**
   * Get the complete automatic QC result
   *
   * @return The automatic QC result
   */
  public AutoQCResult getAutoQcResult() {
    return autoQC;
  }

  /**
   * Get the QC flag set by the user
   *
   * @return The user QC flag
   */
  public Flag getUserQCFlag() {
    return getUserQCFlag(false);
  }

  public Flag getUserQCFlag(boolean ignoreNeeded) {

    Flag result = userQCFlag;

    if (StringUtils.isEmpty(value)) {
      /*
       * Empty values are bad by definition
       */
      result = flagScheme.getBadFlag();
    } else if ((null == userQCFlag || userQCFlag.equals(FlagScheme.NEEDED_FLAG))
      && ignoreNeeded) {

      result = getAutoQcFlag();
    }

    return result;
  }

  /**
   * Get the QC message entered by the user
   *
   * @return The user QC message
   */
  public String getUserQCMessage() {
    return null == userQCMessage ? "" : userQCMessage;
  }

  /**
   * Reset the automatic QC result
   *
   * @throws RecordNotFoundException
   *           If the value has not yet been stored in the database
   */
  public void clearAutomaticQC() throws RecordNotFoundException {

    if (!isInDatabase()) {
      throw new RecordNotFoundException(
        "SensorValue has not been stored in the database");
    }
    autoQC = new AutoQCResult(flagScheme);

    // Reset the user QC if it hasn't been set by the user
    if (userQCFlag.equals(flagScheme.getAssumedGoodFlag())
      || userQCFlag.equals(FlagScheme.NEEDED_FLAG)) {
      userQCFlag = flagScheme.getAssumedGoodFlag();
      userQCMessage = null;
    }

    dirty = true;
  }

  /**
   * Add a flag from an automatic QC routine to the automatic QC result
   *
   * @param flag
   *          The flag
   *
   * @throws RecordNotFoundException
   *           If the value has not yet been stored in the database
   */
  public void addAutoQCFlag(RoutineFlag flag)
    throws RecordNotFoundException, RoutineException {

    if (!isInDatabase()) {
      throw new RecordNotFoundException(
        "SensorValue has not been stored in the database");
    }
    autoQC.add(flag);

    // Update the user QC if it hasn't been set by the user
    if (userQCFlag.equals(flagScheme.getAssumedGoodFlag())
      || userQCFlag.equals(FlagScheme.NEEDED_FLAG)) {
      userQCFlag = FlagScheme.NEEDED_FLAG;
      userQCMessage = autoQC.getAllMessages();
    }

    dirty = true;
  }

  /**
   * Remove the automatic QC information related to a specified {@Link Routine}.
   *
   * @param routine
   *          The routine whose information is to be removed.
   */
  public boolean removeAutoQCFlag(Routine routine) {
    boolean result = autoQC.remove(routine);

    if (result) {
      if (autoQC.size() == 0 && userQCFlag.equals(FlagScheme.NEEDED_FLAG)) {
        userQCFlag = flagScheme.getAssumedGoodFlag();
        userQCMessage = null;
      }
      dirty = true;
    }

    return result;
  }

  /**
   * Set the User QC information. If this will override an existing position QC,
   * only set it if the flag is worse than the position flag.
   *
   * @param flag
   *          The user QC flag
   * @param message
   *          The user QC message
   * @throws InvalidFlagException
   *           If an invalid flag is set.
   */
  public void setUserQC(Flag flag, String message) throws InvalidFlagException {

    boolean setQC = true;

    // We always simplify user QC flags to the basic flag.
    Flag simpleFlag = flag.getSimpleFlag();

    if (simpleFlag.equals(FlagScheme.LOOKUP_FLAG)) {
      throw new InvalidFlagException(
        "Cannot manually set " + flag.toString() + " flag");
    }

    if (null != userQCFlag) {
      // Never override a flushing or Auto QC flag
      if (userQCFlag.equals(FlagScheme.FLUSHING_FLAG)
        || userQCFlag.equals(FlagScheme.LOOKUP_FLAG)) {
        setQC = false;
      }
    }

    if (setQC && isNumeric() && !isNaN()) {
      setUserQCAction(simpleFlag, message);
    }
  }

  /**
   * Remove any user QC flags, so the {@link #autoQC} becomes the basis for the
   * value's QC result.
   *
   * <p>
   * {@link Flag#LOOKUP} flags will not be replaced. {@link Flag#LOOKUP} flags
   * will only be replaced if {@code force} is {@code true}.
   * </p>
   *
   * @param force
   *          Force override of {@link Flag#FLUSHING} flags.
   */
  public void removeUserQC(boolean force) {

    boolean remove = true;

    if (userQCFlag.equals(FlagScheme.LOOKUP_FLAG)
      || (!force && userQCFlag.equals(FlagScheme.FLUSHING_FLAG))) {
      remove = false;
    }

    if (remove) {
      userQCFlag = autoQC.size() > 0 ? FlagScheme.NEEDED_FLAG
        : flagScheme.getAssumedGoodFlag();
      userQCMessage = "";
      dirty = true;
    }
  }

  /**
   * Copy the user QC info from the specified SensorValue.
   *
   * @param source
   *          The source SensorValue.
   */
  public void setUserQC(SensorValue source) {
    userQCFlag = source.userQCFlag;
    userQCMessage = source.userQCMessage;
    dirty = true;
  }

  private void setUserQCAction(Flag flag, String message) {
    userQCFlag = flag;
    userQCMessage = message;
    dirty = true;
  }

  /**
   * Determine whether or not this value needs to be saved to the database
   *
   * @return {@code true} if the value needs to be saved; {@code false}
   *         otherwise
   */
  public boolean isDirty() {
    return dirty;
  }

  /**
   * Determine whether or not this value is stored in the database
   *
   * @return {@code true} if the value is in the database; {@code false} if it
   *         is a new record and is yet to be saved
   */
  public boolean isInDatabase() {
    return (id != DatabaseUtils.NO_DATABASE_RECORD);
  }

  /**
   * Clear the {@code dirty} flag on a collection of SensorValues
   *
   * @param sensorValues
   *          The values to be cleared
   */
  public static void clearDirtyFlag(Collection<SensorValue> sensorValues) {
    for (SensorValue value : sensorValues) {
      value.dirty = false;
    }
  }

  /**
   * Get the database ID of this sensor value
   *
   * @return The database ID
   */
  public long getId() {
    return id;
  }

  public void setId(long id) throws IllegalAccessException {
    if (isInDatabase()) {
      throw new IllegalAccessException("Cannot reassign SensorValue ID");
    }

    this.id = id;
  }

  @Override
  public int compareTo(SensorValue o) {
    int result = coordinate.compareTo(o.coordinate);

    if (result == 0) {
      result = Long.compare(datasetId, o.datasetId);
    }

    if (result == 0) {
      result = Long.compare(columnId, o.columnId);
    }

    return result;
  }

  /**
   * Clear the automatic QC information for a set of SensorValues
   *
   * @param values
   *          The values
   *
   * @throws RecordNotFoundException
   */
  public static void clearAutoQC(Collection<SensorValue> values)
    throws RecordNotFoundException {
    for (SensorValue value : values) {
      value.clearAutomaticQC();
    }
  }

  /**
   * Clear the automatic QC information for a set of SensorValues
   *
   * @param values
   *          The values
   *
   * @throws RecordNotFoundException
   */
  public static void clearAutoQC(SensorValuesList values)
    throws RecordNotFoundException {
    clearAutoQC(values.getRawValues());
  }

  public static boolean contains(List<SensorValue> values, String searchValue) {
    boolean result = false;

    for (SensorValue testValue : values) {
      if (testValue.getValue().equals(searchValue)) {
        result = true;
        break;
      }
    }

    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (columnId ^ (columnId >>> 32));
    result = prime * result + (int) (datasetId ^ (datasetId >>> 32));
    result = prime * result
      + ((coordinate == null) ? 0 : coordinate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof SensorValue))
      return false;
    SensorValue other = (SensorValue) obj;
    if (columnId != other.columnId)
      return false;
    if (datasetId != other.datasetId)
      return false;
    if (coordinate == null) {
      if (other.coordinate != null)
        return false;
    } else if (!coordinate.equals(other.coordinate))
      return false;
    return true;
  }

  public void calibrateValue(Calibration calibration) {
    if (!isNaN()) {
      setValue(calibration.calibrateValue(getDoubleValue()));
    }
  }

  public Flag getDisplayFlag(DatasetSensorValues allSensorValues) {

    Flag result;

    if (StringUtils.isEmpty(value)) {
      result = flagScheme.getBadFlag();
    } else if (userQCFlag.equals(FlagScheme.LOOKUP_FLAG)) {
      Set<Long> sourceValues = StringUtils.delimitedToLongSet(userQCMessage);
      result = SensorValue.getValueWithWorstFlag(
        allSensorValues.getById(sourceValues), allSensorValues).getUserQCFlag();
    } else {
      result = flagNeeded() ? autoQC.getOverallFlag() : getUserQCFlag();
    }

    return result;
  }

  public String getDisplayQCMessage(DatasetSensorValues allSensorValues)
    throws RoutineException {

    String result;

    if (userQCFlag.equals(FlagScheme.LOOKUP_FLAG)) {
      Set<Long> sourceValues = StringUtils.delimitedToLongSet(userQCMessage);

      Set<String> messages = new TreeSet<String>();

      for (SensorValue value : allSensorValues.getById(sourceValues)) {
        if (!(isPosition() && value.isPosition())) {
          String qcMessage = value.getDisplayQCMessage(allSensorValues);
          if (null != qcMessage) {
            messages.add(qcMessage);
          }
        }
      }

      result = StringUtils.collectionToDelimited(messages, ";");

    } else {
      result = flagNeeded() ? autoQC.getAllMessages() : getUserQCMessage();
    }

    return result;
  }

  public boolean flagNeeded() {
    return userQCFlag.equals(FlagScheme.NEEDED_FLAG);
  }

  @Override
  public String toString() {
    return coordinate.toString() + ": " + columnId + " = "
      + (value.equals(NO_VALUE) ? "No Value" : value);
  }

  public void setValue(String value) {
    this.value = value;
    doubleValue = null;
  }

  public void setValue(Double value) {
    this.doubleValue = value;
    this.value = String.valueOf(value);
  }

  public boolean noValue() {
    return null == value || value.equals(NO_VALUE);
  }

  @Override
  public Object clone() {
    SensorValue clone = new SensorValue(id, datasetId, flagScheme, columnId,
      coordinate, value, autoQC, userQCFlag, userQCMessage);
    clone.dirty = this.dirty;
    return clone;
  }

  /**
   * Calculate the mean value from a collection of SensorValues. NaN values are
   * ignored.
   *
   * @param values
   *          The values.
   *
   * @return The mean value.
   */
  public static Double getMeanValue(Collection<SensorValue> values) {

    Double total = 0D;
    int count = 0;

    for (SensorValue value : values) {
      if (!value.isNaN()) {
        total = total + value.getDoubleValue();
        count++;
      }
    }

    return total / count;

  }

  public static Flag getCombinedDisplayFlag(
    Collection<SensorValue> sensorValues, DatasetSensorValues allSensorValues) {

    Flag result = allSensorValues.getFlagScheme().getGoodFlag();

    for (SensorValue sensorValue : sensorValues) {
      if (null != sensorValue) {
        if (sensorValue.getDisplayFlag(allSensorValues)
          .moreSignificantThan(result)) {
          result = sensorValue.getDisplayFlag(allSensorValues);
        }
      }
    }

    return result;
  }

  public static String getCombinedQcComment(
    Collection<SensorValue> sensorValues, DatasetSensorValues allSensorValues)
    throws RoutineException {

    List<String> comments = new ArrayList<String>(sensorValues.size());

    for (SensorValue sensorValue : sensorValues) {
      if (null != sensorValue) {
        String comment = sensorValue.getDisplayQCMessage(allSensorValues);
        if (null != comment && comment.trim().length() > 0) {
          comments.add(comment.trim());
        }
      }
    }

    return StringUtils.collectionToDelimited(comments, ";");
  }

  public static boolean allUserQCNeeded(Collection<SensorValue> values,
    FlagScheme flagScheme) {

    boolean result = true;

    for (SensorValue value : values) {
      if (!value.getUserQCFlag().equals(FlagScheme.NEEDED_FLAG)
        && !value.getUserQCFlag().equals(flagScheme.getAssumedGoodFlag())) {
        result = false;
        break;
      }
    }

    return result;
  }

  public boolean canBeSaved() {
    return canBeSaved;
  }

  /**
   * Select the value with the worst {@link Flag} from the supplied collection
   * of values. If there's a tie, any one of those values may be returned.
   *
   * @param values
   *          The values to be compared.
   * @return One of the values with the worst Flag.
   */
  public static SensorValue getValueWithWorstFlag(
    Collection<SensorValue> values, DatasetSensorValues allSensorValues) {
    SensorValue result = null;

    for (SensorValue value : values) {
      if (null != value) {
        if (null == result || value.getDisplayFlag(allSensorValues)
          .moreSignificantThan(result.getDisplayFlag(allSensorValues))) {
          result = value;
        }
      }
    }

    return result;
  }

  /**
   * Get the smallest absolute delta per minute (excluding zero) between two
   * consecutive {@link SensorValue}s in a {@link List}.
   *
   * <p>
   * The list is assumed to be in chronological order.
   * </p>
   *
   * @param values
   *          The list of SensorValues
   * @return The smallest delta
   */
  public static double getMinimumChange(List<SensorValue> values) {

    double minDelta = Double.MAX_VALUE;

    if (values.size() > 1) {
      for (int i = 1; i < values.size(); i++) {
        SensorValue first = values.get(i - 1);
        SensorValue second = values.get(i);

        if (!first.isNaN() && !second.isNaN()) {

          double valueDelta = Math
            .abs(second.getDoubleValue() - first.getDoubleValue());

          if (valueDelta > 0 && valueDelta < minDelta) {
            minDelta = valueDelta;
          }
        }
      }
    }

    return minDelta;
  }

  /**
   * Set the QC for this SensorValue as a cascade from another SensorValue. Can
   * be called multiple times with different sources.
   *
   * @param source
   *          The source of the QC flag
   * @throws InvalidFlagException
   */
  public void setCascadingQC(SensorValue source) throws InvalidFlagException {
    SortedSet<Long> sources = userQCFlag.equals(FlagScheme.LOOKUP_FLAG)
      ? StringUtils.delimitedToLongSet(userQCMessage)
      : new TreeSet<Long>();

    sources.add(source.getId());

    userQCFlag = FlagScheme.LOOKUP_FLAG;
    userQCMessage = StringUtils.collectionToDelimited(sources, ",");
    dirty = true;
  }

  public void setCascadingQC(PlotPageTableValue source)
    throws InvalidFlagException {
    SortedSet<Long> sources = userQCFlag.equals(FlagScheme.LOOKUP_FLAG)
      ? StringUtils.delimitedToLongSet(userQCMessage)
      : new TreeSet<Long>();

    sources.addAll(source.getSources());

    if (sources.size() == 0) {
      throw new InvalidFlagException(
        "Attempted to set LOOKUP flag with no source");
    }

    userQCFlag = FlagScheme.LOOKUP_FLAG;
    userQCMessage = StringUtils.collectionToDelimited(sources, ",");
    dirty = true;
  }

  /**
   * Remove a cascading QC source from this value's QC.
   *
   * <p>
   * If this is the only QC source, the user QC will revert to either
   * {@link Flag#NEEDS_FLAG} or {@link Flag#ASSUMED_GOOD} according to the
   * {@link #autoQC} value.
   * </p>
   * <p>
   * If this value is not registered with any cascading QC, or the specified
   * source is not recorded as part of the value's cascading QC, no action will
   * be taken.
   * </p>
   *
   * @param source
   *          The cascading QC source to be removed.
   */
  public void removeCascadingQC(long sourceId) {

    // If there is no cascading QC already registered, do nothing.
    if (userQCFlag.equals(FlagScheme.LOOKUP_FLAG)) {

      SortedSet<Long> sources = StringUtils.delimitedToLongSet(userQCMessage);
      sources.remove(sourceId);

      if (sources.size() == 0) {
        // Reset the flag to either NEEDED or ASSUMED_GOOD
        if (!flagScheme.isGood(autoQC.getOverallFlag(), true)) {
          userQCFlag = FlagScheme.NEEDED_FLAG;
        } else {
          userQCFlag = flagScheme.getAssumedGoodFlag();
        }

        userQCMessage = null;
      } else {
        userQCMessage = StringUtils.collectionToDelimited(sources, ",");
      }

      dirty = true;
    }
  }

  public void removeCascadingQC(PlotPageTableValue source) {
    // If there is no cascading QC already registered, do nothing.
    if (userQCFlag.equals(FlagScheme.LOOKUP_FLAG)) {

      SortedSet<Long> sources = StringUtils.delimitedToLongSet(userQCMessage);

      source.getSources().forEach(sources::remove);

      if (sources.size() == 0) {
        // Reset the flag to either NEEDED or ASSUMED_GOOD
        if (!flagScheme.isGood(autoQC.getOverallFlag(), true)) {
          userQCFlag = FlagScheme.NEEDED_FLAG;
        } else {
          userQCFlag = flagScheme.getAssumedGoodFlag();
        }

        userQCMessage = null;
      } else {
        userQCMessage = StringUtils.collectionToDelimited(sources, ",");
      }

      dirty = true;
    }
  }

  public boolean isNumeric() {
    return StringUtils.isNumeric(value);
  }

  public boolean isPosition() {
    return SensorType.isPosition(columnId);
  }

  /**
   * Determine whether or not two {@link SensorValue} object have the same
   * value. All other attributes from the objects are ignored.
   *
   * <p>
   * If both values are {@code null}, the values are considered equal.
   * </p>
   *
   * @param value1
   *          The first value.
   * @param value2
   *          The second value.
   * @return {@code true} if the values are the same; {@code false} otherwise.
   */
  public static boolean valuesEqual(SensorValue value1, SensorValue value2) {

    boolean result;

    String val1 = value1.value;
    String val2 = value2.value;

    if (null == val1 && null == val2) {
      result = true;
    } else if (null == val1 || null == val2) {
      result = false;
    } else {
      result = val1.equals(val2);
    }

    return result;
  }

  public FlagScheme getFlagScheme() {
    return flagScheme;
  }
}
