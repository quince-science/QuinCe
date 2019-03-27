package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorsConfiguration;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Class to hold data for a single record extracted from raw data
 * @author Steve Jones
 *
 */
public class DataSetRawDataRecord implements Comparable<DataSetRawDataRecord> {

  /**
   * Latitude/Longitude value indicating no position
   */
  public static final double NO_POSITION = -999.9;

  /**
   * The data set to which the record belongs
   */
  private DataSet dataSet;

  /**
   * The database ID of the record
   */
  private long id;

  /**
   * The date of the record
   */
  private LocalDateTime date;

  /**
   * The longitude
   */
  private Double longitude;

  /**
   * The latitude
   */
  private Double latitude;

  /**
   * The Run Type of the record
   */
  private String runType;

  /**
   * The Run Type Category of the record
   */
  private RunTypeCategory runTypeCategory;

  /**
   * Map holding the field values used in calculations
   */
  private LinkedHashMap<String, Double> sensorValues;

  /**
   * Map holding values from diagnostic sensors
   */
  private Map<Long, Double> diagnosticValues;

  /**
   * Basic constructor for required information
   * @param dataSet The data set to which the record belongs
   * @param date The date of the record
   * @param longitude The longitude
   * @param latitude The latitude
   * @param runType The Run Type
   * @param runTypeCategory The Run Type Category
   */
  public DataSetRawDataRecord(DataSet dataSet, LocalDateTime date, Double longitude, Double latitude, String runType, RunTypeCategory runTypeCategory) {
    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.dataSet = dataSet;
    this.date = date;
    this.longitude = longitude;
    this.latitude = latitude;
    this.runType = runType;
    this.runTypeCategory = runTypeCategory;

    sensorValues = new LinkedHashMap<String, Double>();
    diagnosticValues = new HashMap<Long, Double>();
  }

  /**
   * Constructor for a record from the database
   * @param dataSet The data set to which the record belongs
   * @param id The database ID
   * @param date The date of the record
   * @param longitude The longitude
   * @param latitude The latitude
   * @param runType The Run Type
   * @param runTypeCategory The Run Type Category
   */
  public DataSetRawDataRecord(DataSet dataSet, long id, LocalDateTime date, Double longitude, Double latitude, String runType, RunTypeCategory runTypeCategory) {
    this.id = id;
    this.dataSet = dataSet;
    this.date = date;
    this.longitude = longitude;
    this.latitude = latitude;
    this.runType = runType;
    this.runTypeCategory = runTypeCategory;

    sensorValues = new LinkedHashMap<String, Double>();
    diagnosticValues = new HashMap<Long, Double>();
  }

  /**
   * Set a field value
   * @param sensorName The sensor name
   * @param value The value
   */
  public void setSensorValue(String sensorName, Double value) {
    sensorValues.put(sensorName, value);
  }

  /**
   * Set a diagnostic sensor value
   * @param sensorId The sensor's database ID
   * @param value The value
   */
  public void setDiagnosticValue(long sensorId, Double value) {
    diagnosticValues.put(sensorId, value);
  }

  /**
   * Determine whether or not this record is for a measurement
   * @return {@code true} if this is a measurement; {@code false} otherwise
   * @throws NoSuchCategoryException If the record's run type does not exist
   */
  public boolean isMeasurement() throws NoSuchCategoryException {
    return runTypeCategory.getType() == RunTypeCategory.TYPE_MEASUREMENT;
  }

  /**
   * Determine whether or not this is a calibration record
   * @return {@code true} if this is a calibration record; {@code false} otherwise
   * @throws NoSuchCategoryException If the record's run type does not exist
   */
  public boolean isCalibration() throws NoSuchCategoryException {
    return runTypeCategory.getType() == RunTypeCategory.TYPE_CALIBRATION;
  }

  /**
   * Get the database ID of the record
   * @return The record ID
   */
  public long getId() {
    return id;
  }

  /**
   * Get the database ID of the dataset to which this record
   * belongs
   * @return The dataset ID
   */
  public long getDatasetId() {
    return dataSet.getId();
  }

  /**
   * Get the date of the record
   * @return The record date
   */
  public LocalDateTime getDate() {
    return date;
  }

  /**
   * Get the longitude
   * @return The longitude
   */
  public Double getLongitude() {
    return longitude;
  }

  /**
   * Get the latitude
   * @return The latitude
   */
  public Double getLatitude() {
    return latitude;
  }

  /**
   * Get the Run Type
   * @return The Run Type
   */
  public String getRunType() {
    return runType;
  }

  /**
   * Get a sensor value, or a value from a sensor in a Required Group.
   *
   * <p>
   *   If the supplied name is the name of a sensor, then its value is
   *   retrieved directly. Otherwise, if it's the name of a Required
   *   Group, the method finds sensors in that group and returns
   *   the first value that it finds. Sensors are searched in the
   *   order that they appear in the sensor configuration file.
   * </p>
   *
   * @param name The sensor or Required Group name
   * @return The sensor value
   */
  public Double getSensorValue(String name) {
    Double result = null;

    if (sensorValues.containsKey(name)) {
      result = sensorValues.get(name);
    } else {
      SensorsConfiguration sensorConfig = ResourceManager.getInstance().getSensorsConfiguration();
      for (SensorType sensor : sensorConfig.getSensorTypes()) {
        if (null != sensor.getRequiredGroup() && sensor.getRequiredGroup().equalsIgnoreCase(name)) {
          Double value = sensorValues.get(sensor.getName());
          if (null != value) {
            result = value;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Get all the sensor values for this record
   * @return The sensor values
   */
  public Map<String, Double> getSensorValues() {
    return sensorValues;
  }

  @Override
  public int compareTo(DataSetRawDataRecord o) {
    return this.date.compareTo(o.date);
  }

  /**
   * Compare this record to a given date.
   *
   * Works the same as {@link Comparable#compareTo(Object)}.
   *
   * @param date The date to compare
   * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified date.
   */
  public int compareTo(LocalDateTime date) {
    return this.date.compareTo(date);
  }

  @Override
  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof LocalDateTime) {
      result = ((LocalDateTime) o).equals(this.date);
    } else if (o instanceof DataSetRawDataRecord) {
      result = ((DataSetRawDataRecord) o).date.equals(this.date);
    }

    return result;
  }

  /**
   * Get the set of diagnostic values
   * @return The diagnostic values
   */
  public Map<Long, Double> getDiagnosticValues() {
    return diagnosticValues;
  }
}
