package uk.ac.exeter.QuinCe.web.Instrument;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Bean to hold details of a data file's column specification.
 * Used as an injected bean to {@link NewInstrumentBean}.
 * @author Steve Jones
 *
 */
public class ColumnSpec implements Serializable {

	private static final long serialVersionUID = 7883906827413013732L;

	/**
	 * Indicates that date or time components are stored in separate fields
	 */
	private static final String SEPARATE_FIELDS = "separate";
	
	/**
	 * Indicates YYYYMMDD date format
	 */
	public static final String DATE_FORMAT_YYYYMMDD = "YYYYMMDD";
	
	/**
	 * Indicates DD/MM/YY date format
	 */
	public static final String DATE_FORMAT_DDMMYY = "DDMMYY";
	
	/**
	 * Indicates DD/MM/YYYY date format
	 */
	public static final String DATE_FORMAT_DDMMYYYY = "DDMMYYYY";
	
	/**
	 * Indicates MM/DD/YY date format
	 */
	public static final String DATE_FORMAT_MMDDYY = "MMDDYY";
	
	/**
	 * Indicates MM/DD/YYYY date format
	 */
	public static final String DATE_FORMAT_MMDDYYYY = "MMDDYYYY";
	
	/**
	 * Indicates HHMMSS time format
	 */
	public static final String TIME_FORMAT_NO_COLON = "no_colon_time";
	
	/**
	 * Indicates HH:MM:SS time format
	 */
	public static final String TIME_FORMAT_COLON = "colon_time";
	
	/**
	 * Indicates 0:360 longitude format
	 */
	public static final String LON_FORMAT_0_360 = "0_360_lon";
	
	/**
	 * Indicates -180:180 longitude format
	 */
	public static final String LON_FORMAT_MINUS180_180 = "minus180_180_lon";
	
	/**
	 * Indicates 0:180 longitude format (N/S marker will be in a separate column)
	 */
	public static final String LON_FORMAT_0_180 = "0_180_lon";
	
	/**
	 * Indicates -90:90 latitude format
	 */
	public static final String LAT_FORMAT_MINUS90_90 = "minus90_90_lat";
	
	/**
	 * Indicates 0:90 latitude format (E/W marker will be in a separate column)
	 */
	public static final String LAT_FORMAT_0_90 = "0_90_lat";
	
	/**
	 * Run type column code
	 */
	private static final int COL_RUN_TYPE = 0;
	
	/**
	 * Year column code
	 */
	private static final int COL_YEAR = 1;
	
	/**
	 * Month column code
	 */
	private static final int COL_MONTH = 2;
	
	/**
	 * Day column code
	 */
	private static final int COL_DAY = 3;
	
	/**
	 * Date column code
	 */
	private static final int COL_DATE = 4;
	
	/**
	 * Hour column code
	 */
	private static final int COL_HOUR = 5;
	
	/**
	 * Minute column code
	 */
	private static final int COL_MINUTE = 6;
	
	/**
	 * Second column code
	 */
	private static final int COL_SECOND = 7;
	
	/**
	 * Time column code
	 */
	private static final int COL_TIME = 8;
	
	/**
	 * Longitude column code
	 */
	private static final int COL_LONGITUDE = 9;
	
	/**
	 * East/West column code
	 */
	private static final int COL_EAST_WEST = 10;
	
	/**
	 * Latitude column code
	 */
	private static final int COL_LATITUDE = 11;
	
	/**
	 * North/South column code
	 */
	private static final int COL_NORTH_SOUTH = 12;
	
	/**
	 * Intake temperature 1 column code
	 */
	private static final int COL_INTAKE_TEMP_1 = 13;
	
	/**
	 * Intake temperature 2 column code
	 */
	private static final int COL_INTAKE_TEMP_2 = 14;
	
	/**
	 * Intake temperature 3 column code
	 */
	private static final int COL_INTAKE_TEMP_3 = 15;
	
	/**
	 * Salinity 1 column code
	 */
	private static final int COL_SALINITY_1 = 16;
	
	/**
	 * Salinity 2 column code
	 */
	private static final int COL_SALINITY_2 = 17;
	
	/**
	 * Salinity 3 column code
	 */
	private static final int COL_SALINITY_3 = 18;
	
	/**
	 * Equilibrator temperature 1 column code
	 */
	private static final int COL_EQT_1 = 19;
	
	/**
	 * Equilibrator temperature 2 column code
	 */
	private static final int COL_EQT_2 = 20;
	
	/**
	 * Equilibrator temperature 3 column code
	 */
	private static final int COL_EQT_3 = 21;
	
	/**
	 * Equilibrator pressure 1 column code
	 */
	private static final int COL_EQP_1 = 22;
	
	/**
	 * Equilibrator pressure 2 column code
	 */
	private static final int COL_EQP_2 = 23;
	
	/**
	 * Equilibrator pressure 3 column code
	 */
	private static final int COL_EQP_3 = 24;
	
	/**
	 * Atmospheric pressure column code
	 */
	private static final int COL_ATMOSPHERIC_PRESSURE = 25;
	
	/**
	 * Moisture column code
	 */
	private static final int COL_MOISTURE = 26;
	
	/**
	 * CO2 column code
	 */
	private static final int COL_CO2 = 27;
	
	/**
	 * Indicates that a column is not used
	 */
	private static final int COL_NOT_USED = -1;
	
	/**
	 * The name of the first intake temperature sensor
	 */
	private String intakeTempName1 = null;
	
	/**
	 * The name of the second intake temperature sensor
	 */
	private String intakeTempName2 = null;
	
	/**
	 * The name of the third intake temperature sensor
	 */
	private String intakeTempName3 = null;
	
	/**
	 * The name of the first salinity sensor
	 */
	private String salinityName1 = null;
	
	/**
	 * The name of the second salinity sensor
	 */
	private String salinityName2 = null;
	
	/**
	 * The name of the third salinity sensor
	 */
	private String salinityName3 = null;
	
	/**
	 * The name of the first equilibrator temperature sensor
	 */
	private String eqtName1 = null;
	
	/**
	 * The name of the second equilibrator temperature sensor
	 */
	private String eqtName2 = null;
	
	/**
	 * The name of the third equilibrator temperature sensor
	 */
	private String eqtName3 = null;
	
	/**
	 * The name of the first equilibrator pressure sensor
	 */
	private String eqpName1 = null;
	
	/**
	 * The name of the second equilibrator pressure sensor
	 */
	private String eqpName2 = null;
	
	/**
	 * The name of the third equilibrator pressure sensor
	 */
	private String eqpName3 = null;
	
	/**
	 * The date format
	 */
	private String dateFormat = SEPARATE_FIELDS;
	
	/**
	 * The time format
	 */
	private String timeFormat = SEPARATE_FIELDS;
	
	/**
	 * The longitude format
	 */
	private String lonFormat = LON_FORMAT_0_360;
	
	/**
	 * The latitude format
	 */
	private String latFormat = LAT_FORMAT_MINUS90_90;
	
	/**
	 * Indicates whether or not the instrument records barometric pressure
	 */
	private boolean hasAtmosphericPressure = false;
	
	/**
	 * The list of columns selected on the column selection page
	 */
	private String columnSelection = null;
	
	/**
	 * The parent object that spawned this specification
	 */
	private NewInstrumentBean parent = null;
	
	/**
	 * Column containing the run type
	 */
	private int runTypeCol = COL_NOT_USED;
	
	/**
	 * Column containing the year
	 */
	private int yearCol = COL_NOT_USED;
	
	/**
	 * Column containing the month
	 */
	private int monthCol = COL_NOT_USED;
	
	/**
	 * Column containing the day
	 */
	private int dayCol = COL_NOT_USED;
	
	/**
	 * Column containing the date
	 */
	private int dateCol = COL_NOT_USED;
	
	/**
	 * Column containing the hour
	 */
	private int hourCol = COL_NOT_USED;
	
	/**
	 * Column containing the minute
	 */
	private int minuteCol = COL_NOT_USED;
	
	/**
	 * Column containing the second
	 */
	private int secondCol = COL_NOT_USED;
	
	/**
	 * Column containing the time
	 */
	private int timeCol = COL_NOT_USED;
	
	/**
	 * Column containing the longitude
	 */
	private int longitudeCol = COL_NOT_USED;
	
	/**
	 * Column containing the east/west identifier
	 */
	private int eastWestCol = COL_NOT_USED;
	
	/**
	 * Column containing the latitude
	 */
	private int latitudeCol = COL_NOT_USED;
	
	/**
	 * Column containing the north/south identifier
	 */
	private int northSouthCol = COL_NOT_USED;
	
	/**
	 * Column containing the first intake temperature
	 */
	private int intakeTemp1Col = COL_NOT_USED;
	
	/**
	 * Column containing the second intake temperature
	 */
	private int intakeTemp2Col = COL_NOT_USED;
	
	/**
	 * Column containing the third intake temperature
	 */
	private int intakeTemp3Col = COL_NOT_USED;
	
	/**
	 * Column containing the first salinity
	 */
	private int salinity1Col = COL_NOT_USED;
	
	/**
	 * Column containing the second salinity
	 */
	private int salinity2Col = COL_NOT_USED;
	
	/**
	 * Column containing the third salinity
	 */
	private int salinity3Col = COL_NOT_USED;
	
	/**
	 * Column containing the first equilibrator temperature
	 */
	private int eqt1Col = COL_NOT_USED;
	
	/**
	 * Column containing the second equilibrator temperature
	 */
	private int eqt2Col = COL_NOT_USED;
	
	/**
	 * Column containing the third equilibrator temperature
	 */
	private int eqt3Col = COL_NOT_USED;
	
	/**
	 * Column containing the first equilibrator pressure
	 */
	private int eqp1Col = COL_NOT_USED;
	
	/**
	 * Column containing the second equilibrator pressure
	 */
	private int eqp2Col = COL_NOT_USED;
	
	/**
	 * Column containing the third equilibrator pressure
	 */
	private int eqp3Col = COL_NOT_USED;
	
	/**
	 * Column containing the atmospheric pressure
	 */
	private int atmosphericPressureCol = COL_NOT_USED;
	
	/**
	 * Column containing the moisture
	 */
	private int moistureCol = COL_NOT_USED;
	
	/**
	 * Column containing the CO2
	 */
	private int co2Col = COL_NOT_USED;
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Basic constructor
	 * @param parent
	 */
	public ColumnSpec(NewInstrumentBean parent) {
		this.parent = parent;
	}
	
	/**
	 * Retrieve a list of all the columns expected from the
	 * data file for the instrument.
	 * 
	 * The list contains identifier codes. Use getShortColumnName
	 * or getFullColumnName to get the text column name.
	 * 
	 * @return A list of expected columns
	 */
	public List<Integer> getColumnList() {
		
		List<Integer> columnList = new ArrayList<Integer>();
		
		columnList.add(COL_RUN_TYPE);

		if (dateFormat.equals(SEPARATE_FIELDS)) {
			columnList.add(COL_YEAR);
			columnList.add(COL_MONTH);
			columnList.add(COL_DAY);
		} else {
			columnList.add(COL_DATE);
		}
		
		if (timeFormat.equals(SEPARATE_FIELDS)) {
			columnList.add(COL_HOUR);
			columnList.add(COL_MINUTE);
			columnList.add(COL_SECOND);
		} else {
			columnList.add(COL_TIME);
		}
		
		columnList.add(COL_LONGITUDE);
		if (lonFormat.equals(LON_FORMAT_0_180)) {
			columnList.add(COL_EAST_WEST);
		}
		
		columnList.add(COL_LATITUDE);
		if (latFormat.equals(LAT_FORMAT_0_90)) {
			columnList.add(COL_NORTH_SOUTH);
		}
		
		columnList.add(COL_INTAKE_TEMP_1);
		
		if (null != intakeTempName2 && intakeTempName2.length() > 0) {
			columnList.add(COL_INTAKE_TEMP_2);
		}
		
		if (null != intakeTempName3 && intakeTempName3.length() > 0) {
			columnList.add(COL_INTAKE_TEMP_3);
		}
		
		columnList.add(COL_SALINITY_1);
		
		if (null != salinityName2 && salinityName2.length() > 0) {
			columnList.add(COL_SALINITY_2);
		}
		
		if (null != salinityName3 && salinityName3.length() > 0) {
			columnList.add(COL_SALINITY_3);
		}
		
		columnList.add(COL_EQT_1);
		
		if (null != eqtName2 && eqtName2.length() > 0) {
			columnList.add(COL_EQT_2);
		}
		
		if (null != eqtName3 && eqtName3.length() > 0) {
			columnList.add(COL_EQT_3);
		}
		
		columnList.add(COL_EQP_1);
		
		if (null != eqpName2 && eqpName2.length() > 0) {
			columnList.add(COL_EQP_2);
		}
		
		if (null != eqpName3 && eqpName3.length() > 0) {
			columnList.add(COL_EQP_3);
		}
		
		if (hasAtmosphericPressure) {
			columnList.add(COL_ATMOSPHERIC_PRESSURE);
		}
		
		if (!getSamplesDried()) {
			columnList.add(COL_MOISTURE);
		}
	
		columnList.add(COL_CO2);
		
		return columnList;
	}
	
	/**
	 * Returns the column name for the specified column
	 * @param column The column
	 * @return The column name
	 */
	private String getColumnName(int column) {
		String result;
		
		switch (column) {
		case COL_RUN_TYPE: {
			result = "Run type";
			break;
		}
		case COL_YEAR: {
			result = "Year";
			break;
		}
		case COL_MONTH: {
			result = "Month";
			break;
		}
		case COL_DAY: {
			result = "Day";
			break;
		}
		case COL_DATE: {
			result = "Date";
			break;
		}
		case COL_HOUR: {
			result = "Hour";
			break;
		}
		case COL_MINUTE: {
			result = "Minute";
			break;
		}
		case COL_SECOND: {
			result = "Second";
			break;
		}
		case COL_TIME: {
			result = "Time";
			break;
		}
		case COL_LONGITUDE: {
			result = "Longitude";
			break;
		}
		case COL_EAST_WEST: {
			result = "East/West";
			break;
		}
		case COL_LATITUDE: {
			result = "Latitude";
			break;
		}
		case COL_NORTH_SOUTH: {
			result = "North/South";
			break;
		}
		case COL_INTAKE_TEMP_1: {
			result = "Intake Temperature: " + intakeTempName1;
			break;
		}
		case COL_INTAKE_TEMP_2: {
			result = "Intake Temperature: " + intakeTempName2;
			break;
		}
		case COL_INTAKE_TEMP_3: {
			result = "Intake Temperature: " + intakeTempName3;
			break;
		}
		case COL_SALINITY_1: {
			result = "Salinity: " + salinityName1;
			break;
		}
		case COL_SALINITY_2: {
			result = "Salinity: " + salinityName2;
			break;
		}
		case COL_SALINITY_3: {
			result = "Salinity: " + salinityName3;
			break;
		}
		case COL_EQT_1: {
			result = "Equilibrator Temperature: " + eqtName1;
			break;
		}
		case COL_EQT_2: {
			result = "Equilibrator Temperature: " + eqtName2;
			break;
		}
		case COL_EQT_3: {
			result = "Equilibrator Temperature: " + eqtName3;
			break;
		}
		case COL_EQP_1: {
			result = "Equilibrator Pressure: " + eqpName1;
			break;
		}
		case COL_EQP_2: {
			result = "Equilibrator Pressure: " + eqpName2;
			break;
		}
		case COL_EQP_3: {
			result = "Equilibrator Pressure: " + eqpName3;
			break;
		}
		case COL_ATMOSPHERIC_PRESSURE: {
			result = "Atmospheric Pressure";
			break;
		}
		case COL_MOISTURE: {
			result = "Moisture";
			break;
		}
		case COL_CO2: {
			result = "CO2";
			break;
		}
		default: {
			result = "***UNRECOGNISED COLUMN " + column + "***";
		}
		}
		
		return result;
	}
	
	/**
	 * Returns the list of column names that need
	 * to be extracted from an instrument's data files for processing
	 * @return The list of column names.
	 */
	public List<String> getColumnNames() {
		List<String> result = new ArrayList<String>();
		
		for (int colIndex: getColumnList()) {
			result.add(getColumnName(colIndex));
		}
		
		return result;
	}
	
	/**
	 * Interrogates the New Instrument bean to determine whether or
	 * not samples are dried before being analysed
	 * @return A flag indicating whether or not samples are dried before being analysed
	 */
	private boolean getSamplesDried() {
		return Boolean.parseBoolean(parent.getSamplesDried());
	}
	
	/**
	 * Take the list of selected columns and set the column variables
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	protected void processColumnSelection() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		List<Integer> selectedColumns = StringUtils.delimitedToIntegerList(columnSelection);
		List<Integer> requiredColumns = getColumnList();
		
		for (int i = 0; i < requiredColumns.size(); i++) {
			Field columnVar = getColumnVarField(requiredColumns.get(i));
			columnVar.setInt(this, selectedColumns.get(i));
		}
	}
	
	/**
	 * Get the name of the column variable for a given column code
	 * @param column The column code
	 * @return The name of the column variable
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	private Field getColumnVarField(int column) throws NoSuchFieldException, SecurityException {
		String varName = null;
		
		switch(column) {
		case COL_RUN_TYPE: {
			varName = "runTypeCol";
			break;
		}
		case COL_YEAR: {
			varName = "yearCol";
			break;
		}
		case COL_MONTH: {
			varName = "monthCol";
			break;
		}
		case COL_DAY: {
			varName = "dayCol";
			break;
		}
		case COL_DATE: {
			varName = "dateCol";
			break;
		}
		case COL_HOUR: {
			varName = "hourCol";
			break;
		}
		case COL_MINUTE: {
			varName = "minuteCol";
			break;
		}
		case COL_SECOND: {
			varName = "secondCol";
			break;
		}
		case COL_TIME: {
			varName = "timeCol";
			break;
		}
		case COL_LONGITUDE: {
			varName = "longitudeCol";
			break;
		}
		case COL_EAST_WEST: {
			varName = "eastWestCol";
			break;
		}
		case COL_LATITUDE: {
			varName = "latitudeCol";
			break;
		}
		case COL_NORTH_SOUTH: {
			varName = "northSouthCol";
			break;
		}
		case COL_INTAKE_TEMP_1: {
			varName = "intakeTemp1Col";
			break;
		}
		case COL_INTAKE_TEMP_2: {
			varName = "intakeTemp2Col";
			break;
		}
		case COL_INTAKE_TEMP_3: {
			varName = "intakeTemp3Col";
			break;
		}
		case COL_SALINITY_1: {
			varName = "salinity1Col";
			break;
		}
		case COL_SALINITY_2: {
			varName = "salinity2Col";
			break;
		}
		case COL_SALINITY_3: {
			varName = "salinity3Col";
			break;
		}
		case COL_EQT_1: {
			varName = "eqt1Col";
			break;
		}
		case COL_EQT_2: {
			varName = "eqt2Col";
			break;
		}
		case COL_EQT_3: {
			varName = "eqt3Col";
			break;
		}
		case COL_EQP_1: {
			varName = "eqp1Col";
			break;
		}
		case COL_EQP_2: {
			varName = "eqp2Col";
			break;
		}
		case COL_EQP_3: {
			varName = "eqp3Col";
			break;
		}
		case COL_ATMOSPHERIC_PRESSURE: {
			varName = "atmosphericPressureCol";
			break;
		}
		case COL_MOISTURE: {
			varName = "moistureCol";
			break;
		}
		case COL_CO2: {
			varName = "co2Col";
			break;
		}
		}
		
		Field result = null;
		if (null != varName) {
			result = getClass().getDeclaredField(varName);
		}
		
		return result; 
	}
	
	/////////////////// GETTERS AND SETTERS //////////////////////////////////
	
	/**
	 * Get the name of the first intake temperature sensor
	 * @return The name of the first intake temperature sensor
	 */
	public String getIntakeTempName1() {
		return intakeTempName1;
	}
	
	/**
	 * Set the name of the first intake temperature sensor
	 * @param name The name of the first intake temperature sensor
	 */
	public void setIntakeTempName1(String intakeTempName1) {
		this.intakeTempName1 = intakeTempName1;
	}

	/**
	 * Get the name of the second intake temperature sensor
	 * @return The name of the second intake temperature sensor
	 */
	public String getIntakeTempName2() {
		return intakeTempName2;
	}
	
	/**
	 * Set the name of the second intake temperature sensor
	 * @param name The name of the second intake temperature sensor
	 */
	public void setIntakeTempName2(String intakeTempName2) {
		this.intakeTempName2 = intakeTempName2;
	}

	/**
	 * Get the name of the third intake temperature sensor
	 * @return The name of the third intake temperature sensor
	 */
	public String getIntakeTempName3() {
		return intakeTempName3;
	}
	
	/**
	 * Set the name of the third intake temperature sensor
	 * @param name The name of the third intake temperature sensor
	 */
	public void setIntakeTempName3(String intakeTempName3) {
		this.intakeTempName3 = intakeTempName3;
	}

	/**
	 * Get the name of the first salinity sensor
	 * @return The name of the first salinity sensor
	 */
	public String getSalinityName1() {
		return salinityName1;
	}
	
	/**
	 * Set the name of the first salinity sensor
	 * @param name The name of the first salinity sensor
	 */
	public void setSalinityName1(String salinityName1) {
		this.salinityName1 = salinityName1;
	}

	/**
	 * Get the name of the second salinity sensor
	 * @return The name of the second salinity sensor
	 */
	public String getSalinityName2() {
		return salinityName2;
	}
	
	/**
	 * Set the name of the second salinity sensor
	 * @param name The name of the second salinity sensor
	 */
	public void setSalinityName2(String salinityName2) {
		this.salinityName2 = salinityName2;
	}

	/**
	 * Get the name of the third salinity sensor
	 * @return The name of the third salinity sensor
	 */
	public String getSalinityName3() {
		return salinityName3;
	}
	
	/**
	 * Set the name of the third salinity sensor
	 * @param name The name of the third salinity sensor
	 */
	public void setSalinityName3(String salinityName3) {
		this.salinityName3 = salinityName3;
	}

	/**
	 * Get the name of the first equilibrator temperature sensor
	 * @return The name of the first equilibrator temperature sensor
	 */
	public String getEqtName1() {
		return eqtName1;
	}
	
	/**
	 * Set the name of the first equilibrator temperature sensor
	 * @param name The name of the first equilibrator temperature sensor
	 */
	public void setEqtName1(String eqtName1) {
		this.eqtName1 = eqtName1;
	}

	/**
	 * Get the name of the second equilibrator temperature sensor
	 * @return The name of the second equilibrator temperature sensor
	 */
	public String getEqtName2() {
		return eqtName2;
	}
	
	/**
	 * Set the name of the second equilibrator temperature sensor
	 * @param name The name of the second equilibrator temperature sensor
	 */
	public void setEqtName2(String eqtName2) {
		this.eqtName2 = eqtName2;
	}

	/**
	 * Get the name of the third equilibrator temperature sensor
	 * @return The name of the third equilibrator temperature sensor
	 */
	public String getEqtName3() {
		return eqtName3;
	}
	
	/**
	 * Set the name of the third equilibrator temperature sensor
	 * @param name The name of the third equilibrator temperature sensor
	 */
	public void setEqtName3(String eqtName3) {
		this.eqtName3 = eqtName3;
	}

	/**
	 * Get the name of the first equilibrator pressure sensor
	 * @return The name of the first equilibrator pressure sensor
	 */
	public String getEqpName1() {
		return eqpName1;
	}
	
	/**
	 * Set the name of the first equilibrator pressure sensor
	 * @param name The name of the first equilibrator pressure sensor
	 */
	public void setEqpName1(String eqpName1) {
		this.eqpName1 = eqpName1;
	}

	/**
	 * Get the name of the second equilibrator pressure sensor
	 * @return The name of the second equilibrator pressure sensor
	 */
	public String getEqpName2() {
		return eqpName2;
	}
	
	/**
	 * Set the name of the second equilibrator pressure sensor
	 * @param name The name of the second equilibrator pressure sensor
	 */
	public void setEqpName2(String eqpName2) {
		this.eqpName2 = eqpName2;
	}

	/**
	 * Get the name of the third equilibrator pressure sensor
	 * @return The name of the third equilibrator pressure sensor
	 */
	public String getEqpName3() {
		return eqpName3;
	}
	
	/**
	 * Set the name of the third equilibrator pressure sensor
	 * @param name The name of the third equilibrator pressure sensor
	 */
	public void setEqpName3(String eqpName3) {
		this.eqpName3 = eqpName3;
	}

	/**
	 * Get the date columns format
	 * @return The date columns format
	 */
	public String getDateFormat() {
		return dateFormat;
	}
	
	/**
	 * Set the date columns format
	 * @param dateFormat The date columns format
	 */
	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
	
	/**
	 * Get the time columns format
	 * @return The time columns format
	 */
	public String getTimeFormat() {
		return timeFormat;
	}
	
	/**
	 * Set the time columns format
	 * @param timeFormat The time columns format
	 */
	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}
	
	/**
	 * Get the longitude format
	 * @return The longitude format
	 */
	public String getLonFormat() {
		return lonFormat;
	}
	
	/**
	 * Set the longitude format
	 * @param lonFormat The longitude format
	 */
	public void setLonFormat(String lonFormat) {
		this.lonFormat = lonFormat;
	}
	
	/**
	 * Get the latitue format
	 * @return The latitue format
	 */
	public String getLatFormat() {
		return latFormat;
	}
	
	/**
	 * Set the latitue format
	 * @param latFormat The latitue format
	 */
	public void setLatFormat(String latFormat) {
		this.latFormat = latFormat;
	}
	
	/**
	 * Returns the flag that indicates whether the instrument
	 * records barometric pressure
	 * @return The flag value
	 */
	public String getHasAtmosphericPressure() {
		return String.valueOf(hasAtmosphericPressure);
	}
	
	/**
	 * Sets the flag that indicates whether the instrument
	 * records barometric pressure
	 * @param hasAtmosphericPressure The flag value
	 */
	public void setHasAtmosphericPressure(String hasAtmosphericPressure) {
		this.hasAtmosphericPressure = Boolean.parseBoolean(hasAtmosphericPressure);
	}
	
	/**
	 * Returns the list of columns selected on the column selection page
	 * @return The list of columns selected on the column selection page
	 */
	public String getColumnSelection() {
		return columnSelection;
	}
	
	/**
	 * Set the list of columns selected on the column selection page
	 * @param columnSelection The list of columns selected on the column selection page
	 */
	public void setColumnSelection(String columnSelection) {
		this.columnSelection = columnSelection;
	}
}
