
package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagScheme;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Root object for a single measurement in a {@link DataSet}.
 */
public class Measurement implements Comparable<Measurement> {

  public static final MeasurementCoordinateComparator COORDINATE_COMPARATOR = new MeasurementCoordinateComparator();

  protected static Type MEASUREMENT_VALUES_TYPE;

  /**
   * Value stored in the database indicating that the measurement's Run Type
   * will be used to determine which {@link Variable} it applies to.
   */
  public static final long RUN_TYPE_DEFINES_VARIABLE = -1L;

  /**
   * Auto-generated run type name for ignored measurements identified by QuinCe
   * without a dedicated Run Type identification column.
   */
  public static final String IGNORED_RUN_TYPE = "__I";

  /**
   * Auto-generated run type name for internal calibration records identified by
   * QuinCe without a dedicated Run Type identification column.
   */
  public static final String INTERNAL_CALIBRATION_RUN_TYPE = "__C";

  /**
   * Auto-generated run type name for measurements identified by QuinCe without
   * a dedicated Run Type identification column.
   */
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
   * The {@link Coordinate} of the measurement
   */
  private Coordinate coordinate;

  /**
   * The run types of the measurement.
   *
   * <p>
   * Multiple run types may be applicable to different variables. Use
   * {@link #RUN_TYPE_DEFINES_VARIABLE} for the generic run type.
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

  /**
   * The Gson serializer for this measurement.
   */
  private Gson gson;

  static {
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
  public Measurement(long datasetId, FlagScheme flagScheme,
    Coordinate coordinate, Map<Long, String> runTypes) {

    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = datasetId;
    this.coordinate = coordinate;
    this.runTypes = runTypes;
    this.measurementValues = new HashMap<Long, MeasurementValue>();
    buildGson(flagScheme);
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
  public Measurement(long id, long datasetId, Coordinate coordinate,
    String runType, FlagScheme flagScheme) {

    this.id = id;
    this.datasetId = datasetId;
    this.coordinate = coordinate;
    this.runTypes = new HashMap<Long, String>();
    this.measurementValues = new HashMap<Long, MeasurementValue>();
    buildGson(flagScheme);
  }

  public Measurement(long id, long datasetId, Coordinate coordinate,
    Map<Long, String> runTypes,
    HashMap<Long, MeasurementValue> measurementValues, FlagScheme flagScheme) {

    this.id = id;
    this.datasetId = datasetId;
    this.coordinate = coordinate;
    this.runTypes = runTypes;

    if (null == measurementValues) {
      this.measurementValues = new HashMap<Long, MeasurementValue>();
    } else {
      this.measurementValues = measurementValues;
    }

    buildGson(flagScheme);
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
  private Measurement(Coordinate coordinate, FlagScheme flagScheme) {
    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.datasetId = DatabaseUtils.NO_DATABASE_RECORD;
    this.coordinate = coordinate;
    this.runTypes = new HashMap<Long, String>();
    this.measurementValues = null;
  }

  private void buildGson(FlagScheme flagScheme) {
    gson = new GsonBuilder()
      .registerTypeAdapter(new HashMap<Long, MeasurementValue>().getClass(),
        new MeasurementValuesSerializer(flagScheme))
      .create();
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
   * Get the {@link Coordinate} of the measurement
   *
   * @return The measurement time
   */
  public Coordinate getCoordinate() {
    return coordinate;
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
      result = coordinate.compareTo(o.coordinate);
    }

    return result;
  }

  public void setMeasurementValue(MeasurementValue measurementValue) {
    if (null != measurementValue) {
      measurementValues.put(measurementValue.getSensorType().getId(),
        measurementValue);
    }
  }

  public MeasurementValue getMeasurementValue(SensorType sensorType) {
    return measurementValues.get(SensorType.getTrueSensorTypeId(sensorType));
  }

  public MeasurementValue getMeasurementValue(String sensorType)
    throws SensorTypeNotFoundException {
    return getMeasurementValue(ResourceManager.getInstance()
      .getSensorsConfiguration().getSensorType(sensorType));
  }

  /**
   * See if this measurement has values for one or more of the specified sensor
   * types.
   *
   * @param sensorTypes
   * @return
   */
  public boolean hasMeasurementValue(Collection<SensorType> sensorTypes) {
    return sensorTypes.stream().anyMatch(this::hasMeasurementValue);
  }

  public boolean hasMeasurementValue(SensorType sensorType) {
    return measurementValues
      .containsKey(SensorType.getTrueSensorTypeId(sensorType));
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
    return Objects.hash(datasetId, coordinate);
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
    return datasetId == other.datasetId
      && Objects.equals(coordinate, other.coordinate);
  }

  @Override
  public String toString() {
    return "#" + id;
  }

  public static Measurement dummyTimeMeasurement(Coordinate coordinate,
    FlagScheme flagScheme) {
    return new Measurement(coordinate, flagScheme);
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

  public Collection<MeasurementValue> getMeasurementValues() {
    return measurementValues.values();
  }

  public void setCoordinate(Coordinate coordinate) {
    this.coordinate = coordinate;
  }

  /**
   * Get the measurement's QC flag.
   *
   * <p>
   * This is calculated as the worst QC flag from all the measurement's
   * {@link MeasurementValue}s.
   * </p>
   *
   * @return The QC flag for the measurement.
   */
  public Flag getQCFlag(DatasetSensorValues allSensorValues) {

    Flag result;

    if (measurementValues.size() == 0) {
      result = allSensorValues.getFlagScheme().getBadFlag();
    } else {
      result = allSensorValues.getFlagScheme().getAssumedGoodFlag();

      for (MeasurementValue measurementValue : measurementValues.values()) {
        if (measurementValue.getQcFlag(allSensorValues)
          .moreSignificantThan(result)) {
          result = measurementValue.getQcFlag(allSensorValues);
        }
      }
    }

    return result;

  }

  /**
   * Determines whether or not a given run type is <i>not</i> sourced from a
   * column in a data file.
   *
   * This is determined by checking the run type against those that can be
   * defined programmatically ({@link #MEASUREMENT_RUN_TYPE},
   * {@link #INTERNAL_CALIBRATION_RUN_TYPE} or {@link #IGNORED_RUN_TYPE}).
   *
   * @param runType
   *          The run type
   * @return {@code true} if the run type is defined outside a data file;
   *         {@code false} if it is defined in a file.
   */
  public static boolean isNonColumnRunType(String runType) {
    return runType.equals(MEASUREMENT_RUN_TYPE)
      || runType.equals(INTERNAL_CALIBRATION_RUN_TYPE)
      || runType.equals(IGNORED_RUN_TYPE);
  }
}

class MeasurementCoordinateComparator implements Comparator<Measurement> {
  @Override
  public int compare(Measurement o1, Measurement o2) {
    return o1.getCoordinate().compareTo(o2.getCoordinate());
  }
}
