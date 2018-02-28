package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;

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
  private double longitude;

  /**
   * The latitude
   */
  private double latitude;

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
  private Map<String, Double> diagnosticValues;

  /**
   * Basic constructor for required information
   * @param dataSet The data set to which the record belongs
   * @param date The date of the record
   * @param longitude The longitude
   * @param latitude The latitude
   * @param runType The Run Type
   * @param runTypeCategory The Run Type Category
   */
  public DataSetRawDataRecord(DataSet dataSet, LocalDateTime date, double longitude, double latitude, String runType, RunTypeCategory runTypeCategory) {
    this.id = DatabaseUtils.NO_DATABASE_RECORD;
    this.dataSet = dataSet;
    this.date = date;
    this.longitude = longitude;
    this.latitude = latitude;
    this.runType = runType;
    this.runTypeCategory = runTypeCategory;

    sensorValues = new LinkedHashMap<String, Double>();
    diagnosticValues = new HashMap<String, Double>();
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
  public DataSetRawDataRecord(DataSet dataSet, long id, LocalDateTime date, double longitude, double latitude, String runType, RunTypeCategory runTypeCategory) {
    this.id = id;
    this.dataSet = dataSet;
    this.date = date;
    this.longitude = longitude;
    this.latitude = latitude;
    this.runType = runType;
    this.runTypeCategory = runTypeCategory;

    sensorValues = new LinkedHashMap<String, Double>();
    diagnosticValues = new HashMap<String, Double>();
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
   * @param sensorName The sensor name
   * @param value The value
   */
  public void setDiagnosticValue(String sensorName, Double value) {
    diagnosticValues.put(sensorName, value);
  }

  /**
   * Set the diagnostic values contained in a String.
   * The String should be formatted as per the output of {@link #getDiagnosticValuesString()}.
   * @param valuesString The values string
   * @throws DataSetException If the values string is invalid
   */
  public void setDiagnosticValues(String valuesString) throws DataSetException {

    if (null != valuesString && valuesString.length() > 0) {
      String[] entries = valuesString.split(";");
      for (String entry : entries) {
        String[] fields = entry.split(":");
        if (fields.length != 2) {
          throw new DataSetException("Invalid diagnostic values string");
        } else {
          try {
            setSensorValue(fields[0], Double.parseDouble(fields[1]));
          } catch (NumberFormatException e) {
            throw new DataSetException("Invalid diagnostic values string");
          }
        }
      }
    }


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
  public double getLongitude() {
    return longitude;
  }

  /**
   * Get the latitude
   * @return The latitude
   */
  public double getLatitude() {
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
   * Get the diagnostic values as a String
   * @return The diagnostic values String
   */
  public String getDiagnosticValuesString() {

    StringBuilder result = new StringBuilder();

    for (Map.Entry<String, Double> entry : diagnosticValues.entrySet()) {
      result.append(entry.getKey());
      result.append(':');
      result.append(entry.getValue());
      result.append(';');
    }

    return result.toString();
  }

  /**
   * Get a sensor value
   * @param sensorName The sensor name
   * @return The sensor value
   */
  public Double getSensorValue(String sensorName) {
    return sensorValues.get(sensorName);
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
}
