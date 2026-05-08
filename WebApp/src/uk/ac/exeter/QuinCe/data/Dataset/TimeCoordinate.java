package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * Coordinate for time-based measurements.
 *
 * @see uk.ac.exeter.QuinCe.data.Instrument.Instrument#BASIS_TIME
 */
public class TimeCoordinate extends Coordinate {

  /**
   * The formatter for the time.
   *
   * <p>
   * This is used by {@link #toString()} to present the time in the desired
   * format. The default format (including when the formatter is {@code null})
   * is ISO, but can be overridden by {@link #setFormatter(DateTimeFormatter)}.
   * </p>
   *
   * @see DateTimeUtils#toIsoDate(LocalDateTime)
   * @see #setFormatter(DateTimeFormatter)
   */
  private DateTimeFormatter formatter = null;

  /**
   * A special instance of a {@link Coordinate} that is larger than all other
   * {@link Coordinate}s.
   */
  public static TimeCoordinate MAX;

  static {
    try {
      MAX = new TimeCoordinate(DatabaseUtils.NO_DATABASE_RECORD,
        DatabaseUtils.NO_DATABASE_RECORD, LocalDateTime.MAX);
    } catch (CoordinateException e) {
      // This won't happen. Honest.
    }
  }

  /**
   * Constructor. Time is required.
   *
   * @param id
   *          The coordinate's database ID
   * @param datasetId
   *          The database ID of the {@link DataSet} that the coordinate belongs
   *          to.
   * @param time
   *          The timestamp.
   * @throws CoordinateException
   *           If the timestamp is null.
   */
  public TimeCoordinate(long id, long datasetId, LocalDateTime time)
    throws CoordinateException {
    super(id, datasetId, time);
    if (null == time) {
      throw new CoordinateException("Time cannot be null");
    }
  }

  /**
   * Construct a TimeCoordinate for a DataSet with a specified time.
   *
   * <p>
   * The coordinate will not exist in the database, so will be given an ID of
   * {@link DatabaseUtils#NO_DATABASE_RECORD}.
   * </p>
   *
   * @param datasetId
   *          The DataSet ID.
   * @param time
   *          The time.
   */
  public TimeCoordinate(long datasetId, LocalDateTime time) {
    super(DatabaseUtils.NO_DATABASE_RECORD, datasetId, time);
  }

  /**
   * Constructor for a coordinate attached to a new, unsaved {@link DataSet}.
   *
   * @param time
   *          The time.
   */
  public TimeCoordinate(LocalDateTime time) {
    super(DatabaseUtils.NO_DATABASE_RECORD, DatabaseUtils.NO_DATABASE_RECORD,
      time);
  }

  @Override
  public int getType() {
    return Instrument.BASIS_TIME;
  }

  @Override
  protected int compareToWorker(Coordinate o) {
    if (!(o instanceof TimeCoordinate)) {
      throw new IllegalArgumentException(
        "Cannot compare Coordinates of different types.");
    }

    return getTime().compareTo(o.getTime());
  }

  @Override
  protected boolean equalsWorker(Coordinate other) {
    return this.getTime().equals(other.getTime());
  }

  @Override
  public String toString() {
    return null != formatter ? formatter.format(getTime())
      : DateTimeUtils.toIsoDate(getTime());
  }

  /**
   * Generate a dummy TimeCoordinate with the specified time.
   *
   * <p>
   * The result of calling {@link #getId()} and {@link #getDatasetId()} on the
   * returned object will be {@link DatabaseUtils#NO_DATABASE_RECORD}.
   * </p>
   *
   * @param time
   *          The time for the coordinate.
   * @return The coordinate.
   * @throws CoordinateException
   */
  public static TimeCoordinate dummyCoordinate(LocalDateTime time)
    throws CoordinateException {
    return new TimeCoordinate(DatabaseUtils.NO_DATABASE_RECORD,
      DatabaseUtils.NO_DATABASE_RECORD, time);
  }

  /**
   * Get a TimeCoordinate for a specified timestamp.
   *
   * <p>
   * The {@link Collection} of {@link Coordinate}s to search is supplied in
   * {@code existingCoordinates}. If a coordinate exists with the specified
   * time, that coordinate is returned. Otherwise a new {@link TimeCoordinate}
   * object is created with the specified time. Calling {@link #isInDatabase()}
   * on this new coordinate will return {@code false}.
   * </p>
   *
   * @param time
   *          The desired coordinate time.
   * @param existingCoordinates
   *          The existing coordinates in the DataSet.
   * @return The found or created coordinate.
   * @throws CoordinateException
   *           If a new coordinate is required but cannot be created.
   */
  public static TimeCoordinate getCoordinate(LocalDateTime time, long datasetId,
    Collection<Coordinate> existingCoordinates) throws CoordinateException {

    return (TimeCoordinate) existingCoordinates.stream()
      .filter(c -> c.getTime().equals(time)).findAny()
      .orElse(new TimeCoordinate(datasetId, time));
  }

  /**
   * Determines whether or not this {@code TimeCoordinate} is before the
   * specified time.
   *
   * @param time
   *          The time.
   * @return {@code true} if this TimeCoordinate is before the time;
   *         {@code false} otherwise.
   */
  public boolean isBefore(LocalDateTime time) {
    return getTime().isBefore(time);
  }

  /**
   * Determines whether or not this {@code TimeCoordinate} is after the
   * specified time.
   *
   * @param time
   *          The time.
   * @return {@code true} if this TimeCoordinate is after the time;
   *         {@code false} otherwise.
   */
  public boolean isAfter(LocalDateTime time) {
    return getTime().isAfter(time);
  }

  /**
   * Set the formatter for the timestamp.
   *
   * @param formatter
   *          The formatter.
   * @see #formatter
   */
  public void setFormatter(DateTimeFormatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public String getValue(SensorType sensorType)
    throws SensorTypeNotFoundException {
    throw new SensorTypeNotFoundException(sensorType);
  }

}
