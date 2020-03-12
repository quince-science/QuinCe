package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.Objects;

import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

/**
 * Root object for a single measurement in a dataset
 *
 * @author Steve Jones
 *
 */
public class Measurement implements Comparable<Measurement> {

  /**
   * The measurement's database ID
   */
  private long id;

  /**
   * The ID of the dataset to which this measurement belongs
   */
  private final long datasetId;

  /**
   * The timestamp of the measurement
   */
  private final LocalDateTime time;

  /**
   * The run type of the measurement (optional)
   */
  private final String runType;

  /**
   * Constructor for a brand new measurement that is not yet in the database
   *
   * @param datasetId
   *          The ID of the dataset to which the measurement belongs
   * @param time
   *          The timestamp of the measurement
   * @param longitude
   *          The longitude of the measurement
   * @param latitude
   *          The latitude of the measurement
   * @param runType
   *          The run type of the measurement
   */
  public Measurement(long datasetId, LocalDateTime time, String runType) {

    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = datasetId;
    this.time = time;
    this.runType = runType;
  }

  /**
   * Constructor for a measurement from the database
   *
   * @param id
   *          The measurement's database ID
   * @param datasetId
   *          The ID of the dataset to which the measurement belongs
   * @param time
   *          The timestamp of the measurement
   * @param longitude
   *          The longitude of the measurement
   * @param latitude
   *          The latitude of the measurement
   * @param runType
   *          The run type of the measurement
   */
  public Measurement(long id, long datasetId, LocalDateTime time,
    String runType) {

    this.id = id;
    this.datasetId = datasetId;
    this.time = time;
    this.runType = runType;
  }

  /**
   * Set the database ID for this measurement
   *
   * @param id
   *          The database ID
   */
  protected void setDatabaseId(long id) {
    this.id = id;
  }

  /**
   * Get the database ID of this measurement
   *
   * @return The measurement ID
   */
  public long getId() {
    return id;
  }

  /**
   * Get the database ID of the dataset to which this measurement belongs
   *
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Get the time of the measurement
   *
   * @return The measurement time
   */
  public LocalDateTime getTime() {
    return time;
  }

  /**
   * Get the Run Type of this measurement
   *
   * @return The run type
   */
  public String getRunType() {
    return runType;
  }

  /*
   * We will only need to compare measurements from the same dataset. Therefore
   * we can get away with just comparing the times.
   */
  @Override
  public int compareTo(Measurement o) {
    int result = 0;

    if (o.id != id) {
      result = time.compareTo(o.time);
    }

    return result;
  }

  /*
   * Equals & hashCode use the dataset ID and time
   */

  @Override
  public int hashCode() {
    return Objects.hash(datasetId, time);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Measurement))
      return false;
    Measurement other = (Measurement) obj;
    return datasetId == other.datasetId && Objects.equals(time, other.time);
  }
}
