package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.NoSuchCategoryException;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;

/**
 * Class to hold data for a single record extracted from raw data
 * @author Steve Jones
 *
 */
public class DataSetRawDataRecord {

	/**
	 * The data set to which the record belongs
	 */
	private DataSet dataSet;
		
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
	private Map<String, Double> sensorValue;
	
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
		this.dataSet = dataSet;
		this.date = date;
		this.longitude = longitude;
		this.latitude = latitude;
		this.runType = runType;
		this.runTypeCategory = runTypeCategory;
		
		sensorValue = new TreeMap<String, Double>();
		diagnosticValues = new HashMap<String, Double>();
	}
	
	/**
	 * Set a field value
	 * @param sensorName The sensor name
	 * @param value The value
	 */
	public void setSensorValue(String sensorName, Double value) {
		sensorValue.put(sensorName, value);
	}
	
	/**
	 * Set a diagnostic sensor value
	 * @param sensorName The sensor name
	 * @param value The value
	 */
	public void setDiagnosticValue(String sensorName, Double value) {
		sensorValue.put(sensorName, value);
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
		return sensorValue.get(sensorName);
	}
}
