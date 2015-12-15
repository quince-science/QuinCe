package uk.ac.exeter.QuinCe.data;

/**
 * Handles the generation of sensor codes, converting the various
 * available sensors into string codes that can be stored and processed
 * more flexibly.
 * 
 * Each code is represented as a string of the form:
 * &lt;sensor_type&gt;_&lt;sensor_number&gt;
 * 
 * Where the sensor type is a number representing the intake temperature,
 * salinity etc.
 * 
 * The sensor_number is simply the nth sensor of that particular type.
 * 
 * So the second intake temperature sensor will have the code 01_02.
 * 
 * @author Steve Jones
 *
 */
public class SensorCode {

	/**
	 * Indicates an intake temperature sensor
	 */
	public static final int TYPE_INTAKE_TEMP = 0;
	
	/**
	 * Indicates a salinity sensor
	 */
	public static final int TYPE_SALINITY = 1;
	
	/**
	 * Indicates an equilibrator temperature sensor
	 */
	public static final int TYPE_EQT = 2;
	
	/**
	 * Indicates an equilibrator pressure sensor
	 */
	public static final int TYPE_EQP = 3;
	
	/**
	 * The sensor type
	 */
	private int sensorType;
	
	/**
	 * The sensor number
	 */
	private int sensorNumber;
	
	/**
	 * Create a sensor code object from a sensor code string
	 * @param sensorCode The code string
	 */
	public SensorCode(String sensorCode) {
		sensorType = Integer.parseInt(sensorCode.substring(0, 1));
		sensorNumber = Integer.parseInt(sensorCode.substring(3, 4));
	}
	
	/**
	 * Create a sensor code using specific sensor type and number
	 * @param sensorType The numeric code for the sensor type
	 * @param sensorNumber The sensor number
	 */
	public SensorCode(int sensorType, int sensorNumber) {
		this.sensorType = sensorType;
		this.sensorNumber = sensorNumber;
	}
	
	/**
	 * The toString method will simply return the sensor code
	 */
	public String toString() {
		return pad(sensorType) + "_" + pad(sensorNumber);
	}
	
	/**
	 * Zero-pad a number to two digits
	 * @param number The number to be padded
	 * @return The padded number
	 */
	private String pad(int number) {
		StringBuffer result = new StringBuffer();
		
		if (number < 10) {
			result.append("0");
		}
		
		result.append(number);
		
		return result.toString();
	}
}

