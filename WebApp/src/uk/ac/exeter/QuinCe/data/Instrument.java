package uk.ac.exeter.QuinCe.data;

import java.io.Serializable;
import java.util.Map;

/**
 * Object to hold all the details of an instrument
 * @author Steve Jones
 *
 */
public class Instrument implements Serializable {
	
	private static final long serialVersionUID = -8470431717231132753L;

	////////////// *** CONSTANTS *** ///////////////
	
	/**
	 * Indicates that date or time components are stored in separate fields
	 */
	public static final int SEPARATE_FIELDS = 0;
		
	/**
	 * Indicates YYYYMMDD date format
	 */
	public static final int DATE_FORMAT_YYYYMMDD = 1;
	
	/**
	 * Indicates DD/MM/YY date format
	 */
	public static final int DATE_FORMAT_DDMMYY = 2;
	
	/**
	 * Indicates DD/MM/YYYY date format
	 */
	public static final int DATE_FORMAT_DDMMYYYY = 3;
	
	/**
	 * Indicates MM/DD/YY date format
	 */
	public static final int DATE_FORMAT_MMDDYY = 4;
	
	/**
	 * Indicates MM/DD/YYYY date format
	 */
	public static final int DATE_FORMAT_MMDDYYYY = 5;

	/**
	 * Indicates HHMMSS time format
	 */
	public static final int TIME_FORMAT_NO_COLON = 1;
	
	/**
	 * Indicates HH:MM:SS time format
	 */
	public static final int TIME_FORMAT_COLON = 2;
	
	/**
	 * Indicates 0:360 longitude format
	 */
	public static final int LON_FORMAT_0_360 = 0;
	
	/**
	 * Indicates -180:180 longitude format
	 */
	public static final int LON_FORMAT_MINUS180_180 = 1;
	
	/**
	 * Indicates 0:180 longitude format (N/S marker will be in a separate column)
	 */
	public static final int LON_FORMAT_0_180 = 2;
	
	/**
	 * Indicates -90:90 latitude format
	 */
	public static final int LAT_FORMAT_MINUS90_90 = 0;
	
	/**
	 * Indicates 0:90 latitude format (E/W marker will be in a separate column)
	 */
	public static final int LAT_FORMAT_0_90 = 1;
	
	/**
	 * Indicates that a column is not used
	 */
	public static final int COL_NOT_USED = -1;
	
	/**
	 * Run type column code
	 */
	public static final int COL_RUN_TYPE = 0;
	
	/**
	 * Year column code
	 */
	public static final int COL_YEAR = 1;
	
	/**
	 * Month column code
	 */
	public static final int COL_MONTH = 2;
	
	/**
	 * Day column code
	 */
	public static final int COL_DAY = 3;
	
	/**
	 * Date column code
	 */
	public static final int COL_DATE = 4;
	
	/**
	 * Hour column code
	 */
	public static final int COL_HOUR = 5;
	
	/**
	 * Minute column code
	 */
	public static final int COL_MINUTE = 6;
	
	/**
	 * Second column code
	 */
	public static final int COL_SECOND = 7;
	
	/**
	 * Time column code
	 */
	public static final int COL_TIME = 8;
	
	/**
	 * Longitude column code
	 */
	public static final int COL_LONGITUDE = 9;
	
	/**
	 * East/West column code
	 */
	public static final int COL_EAST_WEST = 10;
	
	/**
	 * Latitude column code
	 */
	public static final int COL_LATITUDE = 11;
	
	/**
	 * North/South column code
	 */
	public static final int COL_NORTH_SOUTH = 12;
	
	/**
	 * Intake temperature 1 column code
	 */
	public static final int COL_INTAKE_TEMP_1 = 13;
	
	/**
	 * Intake temperature 2 column code
	 */
	public static final int COL_INTAKE_TEMP_2 = 14;
	
	/**
	 * Intake temperature 3 column code
	 */
	public static final int COL_INTAKE_TEMP_3 = 15;
	
	/**
	 * Salinity 1 column code
	 */
	public static final int COL_SALINITY_1 = 16;
	
	/**
	 * Salinity 2 column code
	 */
	public static final int COL_SALINITY_2 = 17;
	
	/**
	 * Salinity 3 column code
	 */
	public static final int COL_SALINITY_3 = 18;
	
	/**
	 * Equilibrator temperature 1 column code
	 */
	public static final int COL_EQT_1 = 19;
	
	/**
	 * Equilibrator temperature 2 column code
	 */
	public static final int COL_EQT_2 = 20;
	
	/**
	 * Equilibrator temperature 3 column code
	 */
	public static final int COL_EQT_3 = 21;
	
	/**
	 * Equilibrator pressure 1 column code
	 */
	public static final int COL_EQP_1 = 22;
	
	/**
	 * Equilibrator pressure 2 column code
	 */
	public static final int COL_EQP_2 = 23;
	
	/**
	 * Equilibrator pressure 3 column code
	 */
	public static final int COL_EQP_3 = 24;
	
	/**
	 * Atmospheric pressure column code
	 */
	public static final int COL_ATMOSPHERIC_PRESSURE = 25;
	
	/**
	 * Moisture column code
	 */
	public static final int COL_MOISTURE = 26;
	
	/**
	 * CO2 column code
	 */
	public static final int COL_CO2 = 27;

	/**
	 * The total number of columns that could be defined for an instrument
	 */
	private static final int COL_COUNT = 28;
	
	////////////// *** FIELDS *** ///////////////
	
	/**
	 * The name of the instrument
	 */
	private String name = null;
	
	/**
	 * The character used as a separator in data files from
	 * the instrument
	 */
	private char separatorChar = ',';
	
	/**
	 * The number of header lines in the data files
	 * produced by the instrument
	 */
	private int headerLines = 0;
	
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
	private int dateFormat = SEPARATE_FIELDS;

	/**
	 * The time format
	 */
	private int timeFormat = SEPARATE_FIELDS;

	/**
	 * The longitude format
	 */
	private int lonFormat = LON_FORMAT_0_360;
	
	/**
	 * The latitude format
	 */
	private int latFormat = LAT_FORMAT_MINUS90_90;
	
	/**
	 * Indicates whether or not the instrument records barometric pressure
	 */
	private boolean hasAtmosphericPressure = false;
	
	/**
	 * Indicates whether or not samples are dried before being measured
	 */
	private boolean samplesDried = false;
	
	/**
	 * The run types in recorded by the instrument and their classification
	 */
	private Map<String, Integer> runTypes = null;
	
	/**
	 * The set of column assignments
	 */
	private int[] columnAssignments;
	
	////////// *** MAIN METHODS *** /////////////

	/**
	 * Basic constructor - does not take any parameters.
	 * All fields must be populated by the setter methods.
	 */
	public Instrument() {
		// Initialise the columnAssignments array
		columnAssignments = new int[COL_COUNT];
		for (int i = 0; i < columnAssignments.length; i++) {
			columnAssignments[i] = COL_NOT_USED;
		}
	}

	/**
	 * Returns the regular expression to be used for splitting
	 * columns in data files. In most cases this is simply the separator
	 * character, but for spaces the expression will match one or more
	 * consecutive spaces ({@code '  *'}).
	 * @return The regexp string for splitting columns
	 */
	public String getColumnSplitString() {
		String result = String.valueOf(getSeparatorChar());

		if (getSeparatorChar() == ' ') {
			result = "  *";
		}
		
		return result;
	}

	///////// *** GETTERS AND SETTERS *** ///////////////
	
	/**
	 * Returns the number of header lines the data files produced
	 * by the instrument.
	 * @return The number of header lines
	 */
	public int getHeaderLines() {
		return headerLines;
	}
	
	/**
	 * Sets the number of header lines the data files produced
	 * by the instrument.
	 * @param headerLines The number of header lines
	 */
	public void setHeaderLines(int headerLines) {
		this.headerLines = headerLines;
	}

	/**
	 * Get the instrument name
	 * @return The instrument name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Set the instrument name
	 * @param name The instrument name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the separator character
	 * @return The separator character
	 */
	public char getSeparatorChar() {
		return separatorChar;
	}
	
	/**
	 * Set the separator character
	 * @param name The separator character
	 */
	public void setSeparatorChar(char separatorChar) {
		this.separatorChar = separatorChar;
	}
	
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
	public int getDateFormat() {
		return dateFormat;
	}
	
	/**
	 * Set the date columns format
	 * @param dateFormat The date columns format
	 */
	public void setDateFormat(int dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Get the time columns format
	 * @return The time columns format
	 */
	public int getTimeFormat() {
		return timeFormat;
	}
	
	/**
	 * Set the time columns format
	 * @param timeFormat The time columns format
	 */
	public void setTimeFormat(int timeFormat) {
		this.timeFormat = timeFormat;
	}
	
	/**
	 * Get the longitude format
	 * @return The longitude format
	 */
	public int getLonFormat() {
		return lonFormat;
	}
	
	/**
	 * Set the longitude format
	 * @param lonFormat The longitude format
	 */
	public void setLonFormat(int lonFormat) {
		this.lonFormat = lonFormat;
	}
	
	/**
	 * Get the latitue format
	 * @return The latitue format
	 */
	public int getLatFormat() {
		return latFormat;
	}
	
	/**
	 * Set the latitue format
	 * @param latFormat The latitue format
	 */
	public void setLatFormat(int latFormat) {
		this.latFormat = latFormat;
	}

	/**
	 * Returns the flag that indicates whether the instrument
	 * records barometric pressure
	 * @return The flag value
	 */
	public boolean getHasAtmosphericPressure() {
		return hasAtmosphericPressure;
	}
	
	/**
	 * Sets the flag that indicates whether the instrument
	 * records barometric pressure
	 * @param hasAtmosphericPressure The flag value
	 */
	public void setHasAtmosphericPressure(boolean hasAtmosphericPressure) {
		this.hasAtmosphericPressure = hasAtmosphericPressure;
	}

	/**
	 * Returns the flag indicating whether or not samples are dried before being analysed
	 * @return The flag indicating whether or not samples are dried before being analysed
	 */
	public boolean getSamplesDried() {
		return samplesDried;
	}
	
	/**
	 * Sets the flag indicating whether or not samples are dried before being analysed
	 * @param samplesDried The flag value
	 */
	public void setSamplesDried(boolean samplesDried) {
		this.samplesDried = samplesDried;
	}
	
	/**
	 * Store the run type classifications
	 * @param runTypes The run type classifications
	 */
	public void setRunTypes(Map<String, Integer> runTypes) {
		this.runTypes = runTypes;
	}
	
	/**
	 * Returns the column in the data file that contains the
	 * values of the specified data type.
	 * @param dataTypeIdentifier The data type identifier
	 * @return The column of the data file that contains the specified data
	 */
	public int getColumnAssignment(int dataTypeIdentifier) {
		return columnAssignments[dataTypeIdentifier];
	}
	
	/**
	 * Specify which column of the instrument's data file
	 * contains the specified data
	 * @param data The data type identifier
	 * @param column
	 */
	public void setColumnAssignment(int dataTypeIdentifier, int column) {
		columnAssignments[dataTypeIdentifier] = column;
	}
}
