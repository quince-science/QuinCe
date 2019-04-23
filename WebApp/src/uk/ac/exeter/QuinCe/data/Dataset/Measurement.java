package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
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
   * The measured variable
   */
  private final InstrumentVariable variable;

  /**
   * The timestamp of the measurement
   */
  private final LocalDateTime time;

  /**
   * The longitude of the measurement
   */
  private final double longitude;

  /**
   * The latitude of the measurement
   */
  private final double latitude;

  /**
   * The run type of the measurement (optional)
   */
  private final String runType;

  /**
   * Constructor for a brand new measurement that is not yet in the database
   * @param datasetId The ID of the dataset to which the measurement belongs
   * @param variable The variable that is measured
   * @param time The timestamp of the measurement
   * @param longitude The longitude of the measurement
   * @param latitude The latitude of the measurement
   * @param runType The run type of the measurement
   */
  public Measurement(long datasetId, InstrumentVariable variable,
    LocalDateTime time, double longitude, double latitude, String runType) {

    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = datasetId;
    this.variable = variable;
    this.time = time;
    this.longitude = longitude;
    this.latitude = latitude;
    this.runType = runType;
  }

  /**
   * Constructor for a measurement from the database
   * @param id The measurement's database ID
   * @param datasetId The ID of the dataset to which the measurement belongs
   * @param variable The variable that is measured
   * @param time The timestamp of the measurement
   * @param longitude The longitude of the measurement
   * @param latitude The latitude of the measurement
@param runType The run type of the measurement
   */
  public Measurement(long id, long datasetId, InstrumentVariable variable,
    LocalDateTime time, double longitude, double latitude, String runType) {

    this.id = id;
    this.datasetId = datasetId;
    this.variable = variable;
    this.time = time;
    this.longitude = longitude;
    this.latitude = latitude;
    this.runType = runType;
  }

  /**
   * Set the database ID for this measurement
   * @param id The database ID
   */
  protected void setDatabaseId(long id) {
    this.id = id;
  }

  /**
   * Get the database ID of this measurement
   * @return The measurement ID
   */
  public long getId() {
    return id;
  }

  /**
   * Get the database ID of the dataset to which this measurement belongs
   * @return The dataset ID
   */
  public long getDatasetId() {
    return datasetId;
  }

  /**
   * Get the variable measured in this measurement
   * @return The measured variable
   */
  public InstrumentVariable getVariable() {
    return variable;
  }

  /**
   * Get the time of the measurement
   * @return The measurement time
   */
  public LocalDateTime getTime() {
    return time;
  }

  /**
   * Get the longitude of the measurement
   * @return The measurement longitude
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * Get the latitude of the measurement
   * @return The measurement latitude
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * Get the Run Type of this measurement
   * @return The run type
   */
  public String getRunType() {
    return runType;
  }

  /*
   * In theory, equals and compareTo should check the same fields.
   * However, we will only compare Measurements from the same dataset,
   * and the id field is unique (the primary key in the database).
   *
   * Therefore checking the id is sufficient to know that two objects
   * are equal. Comparison is done on the id first, and if they aren't equal
   * then the time is used. This ensures that "compareTo() == 0" is consistent
   * with "equals() == true" in all cases.
   *
   * If the IDs aren't equal, compareTo uses time to establish ordering.
   */

  @Override
  public boolean equals(Object o) {
    // Compare by ID
    boolean equals = false;

    if (o instanceof Measurement) {
      equals = ((Measurement) o).id == id;
    }

    return equals;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  @Override
  public int compareTo(Measurement o) {
    int result = 0;

    if (o.id != id) {
      result = time.compareTo(o.time);
    }

    return result;
  }
}
