
package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Root object for a single measurement in a dataset
 *
 * @author Steve Jones
 *
 */
public class Measurement implements Comparable<Measurement> {

  public static final MeasurementTimeComparator TIME_COMPARATOR = new MeasurementTimeComparator();

  protected static Gson gson;

  protected static Type MEASUREMENT_VALUES_TYPE;

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
   * The values used to perform data reduction for this measurement.
   */
  private HashMap<Long, MeasurementValue> measurementValues;

  static {
    gson = new GsonBuilder()
      .registerTypeAdapter(new HashMap<Long, MeasurementValue>().getClass(),
        new MeasurementValuesSerializer())
      .create();

    MEASUREMENT_VALUES_TYPE = new TypeToken<HashMap<Long, MeasurementValue>>() {
    }.getType();
  }

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
    this.measurementValues = new HashMap<Long, MeasurementValue>();
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
    this.measurementValues = new HashMap<Long, MeasurementValue>();
  }

  public Measurement(long id, long datasetId, LocalDateTime time,
    String runType, HashMap<Long, MeasurementValue> measurementValues) {

    this.id = id;
    this.datasetId = datasetId;
    this.time = time;
    this.runType = runType;

    if (null == measurementValues) {
      this.measurementValues = new HashMap<Long, MeasurementValue>();
    } else {
      this.measurementValues = measurementValues;
    }
  }

  /**
   * Constructor for a dummy Measurement with just a time.
   * 
   * <p>
   * Used by {@link #dummyTimeMeasurement(LocalDateTime)} for the
   * {@link #TIME_COMPARATOR}.
   * </p>
   * 
   * @param time
   *          The measurement time.
   */
  private Measurement(LocalDateTime time) {
    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = DatabaseUtils.NO_DATABASE_RECORD;
    this.time = time;
    this.runType = null;
    this.measurementValues = null;
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

  public void setMeasurementValue(MeasurementValue measurementValue) {
    measurementValues.put(measurementValue.getSensorType().getId(),
      measurementValue);
  }

  public MeasurementValue getMeasurementValue(SensorType sensorType) {
    return measurementValues.get(sensorType.getId());
  }

  public MeasurementValue getMeasurementValue(String sensorType)
    throws SensorTypeNotFoundException {
    return getMeasurementValue(ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType(sensorType));
  }

  public boolean hasMeasurementValue(SensorType sensorType) {
    return measurementValues.containsKey(sensorType.getId());
  }

  public String getMeasurementValuesJson() {
    return gson.toJson(measurementValues);
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

  @Override
  public String toString() {
    return "#" + id + " " + runType;
  }

  public static Measurement dummyTimeMeasurement(LocalDateTime time) {
    return new Measurement(time);
  }
}

class MeasurementTimeComparator implements Comparator<Measurement> {
  @Override
  public int compare(Measurement o1, Measurement o2) {
    return o1.getTime().compareTo(o2.getTime());
  }
}