package uk.ac.exeter.QuinCe.data.Instrument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
public class SensorCode implements Comparable<SensorCode> {

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
	 * The parent instrument
	 */
	private Instrument instrument;
	
	/**
	 * Create a sensor code object from a sensor code string
	 * @param sensorCode The code string
	 * @param instrument The parent instrument
	 */
	public SensorCode(String sensorCode, Instrument instrument) {
		sensorType = Integer.parseInt(sensorCode.substring(0, 2));
		sensorNumber = Integer.parseInt(sensorCode.substring(3, 5));
		this.instrument = instrument;
	}
	
	/**
	 * Create a sensor code using specific sensor type and number
	 * @param sensorType The numeric code for the sensor type
	 * @param sensorNumber The sensor number
	 * @param instrument The parent instrument
	 */
	public SensorCode(int sensorType, int sensorNumber, Instrument instrument) {
		this.sensorType = sensorType;
		this.sensorNumber = sensorNumber;
		this.instrument = instrument;
	}
	
	public String getSensorName() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return getSensorNameFromInstrument(false);
	}
	
	public String getLongSensorName() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return getSensorNameFromInstrument(true);
	}
	
	private String getSensorNameFromInstrument(boolean longName) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		StringBuffer methodName = new StringBuffer("get");
		if (longName) {
			methodName.append("Long");
		}
		
		switch(sensorType) {
		case TYPE_INTAKE_TEMP: {
			methodName.append("IntakeTemp");
			break;
		}
		case TYPE_SALINITY: {
			methodName.append("Salinity");
			break;
		}
		case TYPE_EQT: {
			methodName.append("Eqt");
			break;
		}
		case TYPE_EQP: {
			methodName.append("Eqp");
			break;
		}
		}
		
		methodName.append("Name");
		methodName.append(sensorNumber);
		
		Method nameMethod = Instrument.class.getMethod(methodName.toString(), (Class<?>[]) null);
		return (String) nameMethod.invoke(instrument, (Object[]) null);
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

	@Override
	public int compareTo(SensorCode o) {
		int result = 0;
		
		result = this.sensorType - o.sensorType;
		
		if (result == 0) {
			result = this.sensorNumber - o.sensorNumber;
		}
		
		
		return result;
	}
}

