
package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
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

  public static final long GENERIC_RUN_TYPE_VARIABLE = -1L;

  public static final String IGNORED_RUN_TYPE = "__I";

  public static final String INTERNAL_CALIBRATION_RUN_TYPE = "__C";

  public static final String MEASUREMENT_RUN_TYPE = "__M";

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
   * The run types of the measurement.
   *
   * <p>
   * Multiple run types may be applicable to different variables. Use
   * {@link #GENERIC_RUN_TYPE_VARIABLE} for the generic run type.
   */
  private final Map<Long, String> runTypes;

  /**
   * The values used to perform data reduction for this measurement.
   *
   * <p>
   * The {@link Long} is the database ID of a {@link SensorType}.
   * </p>
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
  public Measurement(long datasetId, LocalDateTime time,
    Map<Long, String> runTypes) {

    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = datasetId;
    this.time = time;
    this.runTypes = runTypes;
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
    this.runTypes = new HashMap<Long, String>();
    this.measurementValues = new HashMap<Long, MeasurementValue>();
  }

  public Measurement(long id, long datasetId, LocalDateTime time,
    Map<Long, String> runTypes,
    HashMap<Long, MeasurementValue> measurementValues) {

    this.id = id;
    this.datasetId = datasetId;
    this.time = time;
    this.runTypes = runTypes;

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
    this.runTypes = new HashMap<Long, String>();
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
  public String getRunType(long variableId) {
    return runTypes.get(variableId);
  }

  public String getRunType(Variable variable) {
    return runTypes.get(variable.getId());
  }

  public Map<Long, String> getRunTypes() {
    return runTypes;
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

  public boolean containsMeasurementValue(SensorType sensorType) {
    return measurementValues.containsKey(sensorType.getId());
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

  public Set<SensorType> getMeasurementValueSensorTypes()
    throws SensorTypeNotFoundException {
    SensorsConfiguration sensorConfig = ResourceManager.getInstance()
      .getSensorsConfiguration();

    Set<SensorType> sensorTypes = new TreeSet<SensorType>();
    for (long id : measurementValues.keySet()) {
      sensorTypes.add(sensorConfig.getSensorType(id));
    }
    return sensorTypes;
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
    return "#" + id;
  }

  public static Measurement dummyTimeMeasurement(LocalDateTime time) {
    return new Measurement(time);
  }

  /**
   * Add the run types from the supplied measurement to this one.
   *
   * <p>
   * Clashing run types are overwritten by the incoming run types.
   * </p>
   *
   * @param incoming
   *          The source of the new run types
   */
  public void addRunTypes(Measurement incoming) {
    this.runTypes.putAll(incoming.runTypes);
  }
}

class MeasurementTimeComparator implements Comparator<Measurement> {
  @Override
  public int compare(Measurement o1, Measurement o2) {
    return o1.getTime().compareTo(o2.getTime());
  }
}
