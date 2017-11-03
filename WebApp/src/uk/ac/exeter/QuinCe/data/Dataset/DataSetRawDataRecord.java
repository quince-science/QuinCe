package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
	private RunTypeCategory runType;
	
	/**
	 * Map holding the field values used in calculations
	 */
	private Map<String, Double> fieldValues;
	
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
	 */
	public DataSetRawDataRecord(DataSet dataSet, LocalDateTime date, double longitude, double latitude, RunTypeCategory runType) {
		this.dataSet = dataSet;
		this.date = date;
		this.longitude = longitude;
		this.latitude = latitude;
		this.runType = runType;
		
		fieldValues = new HashMap<String, Double>();
		diagnosticValues = new HashMap<String, Double>();
	}
	
	/**
	 * Set a field value
	 * @param sensorName The sensor name
	 * @param value The value
	 */
	public void setValue(String sensorName, Double value) {
		fieldValues.put(sensorName, value);
	}
	
	/**
	 * Set a diagnostic sensor value
	 * @param sensorName The sensor name
	 * @param value The value
	 */
	public void setDiagnosticValue(String sensorName, Double value) {
		fieldValues.put(sensorName, value);
	}
}
