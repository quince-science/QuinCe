package uk.ac.exeter.QuinCe.data;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Object to hold all the details of an instrument
 * @author Steve Jones
 *
 */
public class Instrument implements Serializable {
	
	private static final long serialVersionUID = 7282491666003300432L;

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
	 * The instrument's ID in the database
	 */
	private long databaseID = DatabaseUtils.NO_DATABASE_RECORD;
	
	/**
	 * The name of the instrument
	 */
	private String name = null;
	
	/**
	 * The ID of the owner of the instrument
	 */
	private long ownerID;
	
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
	private TreeSet<RunType> runTypes = null;
	
	/**
	 * The number of columns in the raw data file
	 */
	private int rawFileColumnCount = -1;
	
	/**
	 * The set of column assignments
	 */
	private int[] columnAssignments;
	
	/**
	 * Formatter for dates
	 */
	private SimpleDateFormat dateFormatter = null;
	
	/**
	 * Formatter for times
	 */
	private SimpleDateFormat timeFormatter = null;
	
	////////// *** MAIN METHODS *** /////////////

	/**
	 * Basic constructor - does not take any parameters.
	 * All fields must be populated by the setter methods.
	 */
	public Instrument(long ownerID) {
		
		this.ownerID = ownerID;
		
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
	
	public void validate() throws MissingParamException {
		// TODO Write it!
	}
	
	/**
	 * Determines whether or not a given run type is for a measurement.
	 * If the run type does not exist, this will return {@code false}.
	 * 
	 * @param runType The run type
	 * @return {@code true} if the run type is for a measurement; {@code false} if it is not.
	 */
	public boolean isMeasurementRunType(String runType) {
		boolean result = false;
		
		for (RunType type : runTypes) {
			if (type.getName().equals(runType)) {
				result = type.isMeasurementRunType();
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Determines whether or not a given run type is for a gas standard.
	 * If the run type does not exist, this will return {@code false}.
	 * 
	 * @param runType The run type
	 * @return {@code true} if the run type is for a measurement; {@code false} if it is not.
	 */
	public boolean isStandardRunType(String runType) {
		boolean result = false;
		
		for (RunType type : runTypes) {
			if (type.getName().equals(runType)) {
				result = type.isStandardRunType();
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Determines whether or not a run type should be ignored. If the run type
	 * is unrecognised it will count as being ignored.
	 * 
	 * @param runType The run type to be checked
	 * @return {@code true} if the run type should be ignored; {@code false} if it should be used.
	 */
	public boolean isIgnoredRunType(String runType) {
		boolean result = true;
		
		for (RunType type : runTypes) {
			if (type.getName().equals(runType)) {
				result = type.isIgnoredRunType();
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the integer code for the run type matching the supplied
	 * run type name. If the run type is not found, it will default to
	 * the ignored/none run type.
	 * @param runType The run type
	 * @return The code
	 */
	public int getRunTypeCode(String runType) {
		int result = RunType.RUN_TYPE_NONE;
		
		for (RunType type : runTypes) {
			if (type.getName().equals(runType)) {
				result = type.getCode();
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the integer code for the run type matching the supplied
	 * run type name. If the run type is not found, it will default to
	 * the ignored/none run type.
	 * @param runType The run type
	 * @return The code
	 */
	public long getRunTypeId(String runType) {
		long result = -1;
		
		for (RunType type : runTypes) {
			if (type.getName().equals(runType)) {
				result = type.getDatabaseId();
			}
		}
		
		return result;
	}
	
	/**
	 * Extracts the date and time from a line in a raw data file
	 * @param line The line
	 * @return The date and time
	 * @throws DateTimeParseException If the date or time cannot be parsed
	 * @throws InstrumentException If the date/time format is unrecognised
	 */
	public Calendar getDateFromLine(List<String> line) throws DateTimeParseException, InstrumentException {
		
		Calendar result = Calendar.getInstance(new SimpleTimeZone(0, "UTC"), Locale.ENGLISH);

		// Need to do date and time separately.
		
		switch (getDateFormat()) {
		case Instrument.SEPARATE_FIELDS: {
			
			try {
				result.set(Calendar.YEAR, Integer.parseInt(line.get(getColumnAssignment(Instrument.COL_YEAR))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateTimeParseException("Invalid year value " + line.get(getColumnAssignment(Instrument.COL_YEAR)));
			}
			
			try {
				result.set(Calendar.MONTH, Integer.parseInt(line.get(getColumnAssignment(Instrument.COL_MONTH))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateTimeParseException("Invalid month value " + line.get(getColumnAssignment(Instrument.COL_MONTH)));
			}
			
			try {
				result.set(Calendar.DAY_OF_MONTH, Integer.parseInt(line.get(getColumnAssignment(Instrument.COL_DAY))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateTimeParseException("Invalid day value " + line.get(getColumnAssignment(Instrument.COL_DAY)));
			}
			break;
		}
		default: {
			try {
				if (null == dateFormatter) {
					makeDateFormatter();
				}
				
				Calendar parsedDate = Calendar.getInstance();
				parsedDate.setTime(dateFormatter.parse(line.get(getColumnAssignment(Instrument.COL_DATE))));
				result.set(Calendar.YEAR, parsedDate.get(Calendar.YEAR));
				result.set(Calendar.MONTH, parsedDate.get(Calendar.MONTH));
				result.set(Calendar.DAY_OF_MONTH, parsedDate.get(Calendar.DAY_OF_MONTH));
				
			} catch (ParseException e) {
				throw new DateTimeParseException("Invalid date value " + line.get(getColumnAssignment(Instrument.COL_DATE)));
			}
		}
		}
			

		// Now the time
		switch(getTimeFormat()) {
		case Instrument.SEPARATE_FIELDS: {
			try {
				result.set(Calendar.HOUR, Integer.parseInt(line.get(getColumnAssignment(Instrument.COL_HOUR))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateTimeParseException("Invalid hour value " + line.get(getColumnAssignment(Instrument.COL_HOUR)));
			}
			
			try {
				result.set(Calendar.MINUTE, Integer.parseInt(line.get(getColumnAssignment(Instrument.COL_MINUTE))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateTimeParseException("Invalid minute value " + line.get(getColumnAssignment(Instrument.COL_MINUTE)));
			}
			
			try {
				result.set(Calendar.SECOND, Integer.parseInt(line.get(getColumnAssignment(Instrument.COL_SECOND))));
			} catch (NumberFormatException|ArrayIndexOutOfBoundsException e) {
				throw new DateTimeParseException("Invalid second value " + line.get(getColumnAssignment(Instrument.COL_SECOND)));
			}
			break;
		}
		default: {
			try {
				if (null == timeFormatter) {
					makeTimeFormatter();
				}
	
				Calendar parsedTime = Calendar.getInstance();
				parsedTime.setTime(timeFormatter.parse(line.get(getColumnAssignment(Instrument.COL_TIME))));
				result.set(Calendar.HOUR, parsedTime.get(Calendar.HOUR));
				result.set(Calendar.MINUTE, parsedTime.get(Calendar.MINUTE));
				result.set(Calendar.SECOND, parsedTime.get(Calendar.SECOND));
			} catch (ParseException e) {
				throw new DateTimeParseException("Invalid time value " + line.get(getColumnAssignment(Instrument.COL_TIME)));
			}
		}
		}
		
		return result;
	}

	/**
	 * Create a date formatter for parsing dates from the file.
	 * @throws RawDataFileException If the date format is not recognised
	 * @throws InstrumentException 
	 */
	private void makeDateFormatter() throws InstrumentException {
		
		switch (getDateFormat()) {
		case Instrument.DATE_FORMAT_DDMMYY: {
			dateFormatter = new SimpleDateFormat("dd/MM/yy");
			break;
		}
		case Instrument.DATE_FORMAT_DDMMYYYY: {
			dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
			break;
		}
		case Instrument.DATE_FORMAT_MMDDYY: {
			dateFormatter = new SimpleDateFormat("MM/dd/yy");
			break;
		}
		case Instrument.DATE_FORMAT_MMDDYYYY: {
			dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
			break;
		}
		case Instrument.DATE_FORMAT_YYYYMMDD: {
			dateFormatter = new SimpleDateFormat("YYYYMMdd");
		}
		default: {
			throw new InstrumentException("Unrecognised date format code '" + getDateFormat() + "'");
		}
		}
	}
	
	/**
	 * Create a date formatter for parsing times from the file
	 * @throws RawDataFileException If the time format is not recognised
	 * @throws InstrumentException 
	 */
	private void makeTimeFormatter() throws InstrumentException {
	
		switch(getTimeFormat()) {
		case Instrument.TIME_FORMAT_COLON: {
			timeFormatter = new SimpleDateFormat("H:m:s");
			break;
		}
		case Instrument.TIME_FORMAT_NO_COLON: {
			timeFormatter = new SimpleDateFormat("Hms");
		}
		default: {
			throw new InstrumentException("Unrecognised time format code '" + getTimeFormat() + "'");
		}
		}
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
	public void setRunTypes(TreeSet<RunType> runTypes) {
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
	
	/**
	 * Returns the list of run type names and their allocated run type
	 * @return The list of run types
	 */
	public TreeSet<RunType> getRunTypes() {
		return runTypes;
	}
	
	public String getRunTypeName(long runTypeId) {
		
		String name = null;
		for (RunType type : runTypes) {
			if (type.getDatabaseId() == runTypeId) {
				name = type.getName();
				break;
			}
		}
		return name;
	}
	
	/**
	 * Returns the ID of the instrument in the database
	 * @return The ID of the instrument in the database
	 */
	public long getDatabaseId() {
		return databaseID;
	}
	
	/**
	 * Sets the ID of the instrument in the database
	 * @param databaseID The database ID
	 */
	public void setDatabaseId(long databaseID) {
		this.databaseID = databaseID;
	}
	
	/**
	 * Returns the ID of the owner of the instrument
	 * @return The ID of the owner of the instrument
	 */
	public long getOwnerId() {
		return ownerID;
	}
	
	/**
	 * Sets the ID of the owner of the instrument
	 * @param databaseID The owner ID
	 */
	public void setOwnerId(long ownerID) {
		this.ownerID = ownerID;
	}
	
	/**
	 * Indicates whether or not the instrument has Intake Temperature 1 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasIntakeTemp1() {
		return (null == intakeTempName1 || !intakeTempName1.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Intake Temperature 2 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasIntakeTemp2() {
		return (null == intakeTempName2 || !intakeTempName2.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Intake Temperature 3 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasIntakeTemp3() {
		return (null == intakeTempName3 || !intakeTempName3.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Salinity 1 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasSalinity1() {
		return (null == salinityName1 || !salinityName1.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Salinity 2 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasSalinity2() {
		return (null == salinityName2 || !salinityName2.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Salinity 3 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasSalinity3() {
		return (null == salinityName3 || !salinityName3.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Equilibrator Temperature 1 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasEqt1() {
		return (null == eqtName1 || !eqtName1.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Equilibrator Temperature 2 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasEqt2() {
		return (null == eqtName2 || !eqtName2.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Equilibrator Temperature 3 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasEqt3() {
		return (null == eqtName3 || !eqtName3.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Equilibrator Pressure 1 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasEqp1() {
		return (null == eqpName1 || !eqpName1.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Equilibrator Pressure 2 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasEqp2() {
		return (null == eqpName2 || !eqpName2.isEmpty());
	}
	
	/**
	 * Indicates whether or not the instrument has Equilibrator Pressure 3 defined
	 * @return {@code true} if the sensor is defined; {@code false} if it is not.
	 */
	public boolean hasEqp3() {
		return (null == eqpName3 || !eqpName3.isEmpty());
	}

	/**
	 * Get the long version of intake temperature 1's name. This includes the sensor type,
	 * e.g. 'Intake temperature: Aanderaa'.
	 * @return The long version of the sensor's name
	 */
	public String getLongIntakeTempName1() {
		return "Intake Temperature: " + intakeTempName1;
	}

	/**
	 * Get the long version of intake temperature 2's name. This includes the sensor type,
	 * e.g. 'Intake temperature: Aanderaa'.
	 * @return The long version of the sensor's name
	 */
	public String getLongIntakeTempName2() {
		return "Intake Temperature: " + intakeTempName2;
	}

	/**
	 * Get the long version of intake temperature 3's name. This includes the sensor type,
	 * e.g. 'Intake temperature: Aanderaa'.
	 * @return The long version of the sensor's name
	 */
	public String getLongIntakeTempName3() {
		return "Intake Temperature: " + intakeTempName3;
	}

	/**
	 * Get the long version of salinity 1's name. This includes the sensor type,
	 * e.g. 'Salinity: Seabird'.
	 * @return The long version of the sensor's name
	 */
	public String getLongSalinityName1() {
		return "Salinity: " + salinityName1;
	}

	/**
	 * Get the long version of salinity 2's name. This includes the sensor type,
	 * e.g. 'Salinity: Seabird'.
	 * @return The long version of the sensor's name
	 */
	public String getLongSalinityName2() {
		return "Salinity: " + salinityName2;
	}

	/**
	 * Get the long version of salinity 3's name. This includes the sensor type,
	 * e.g. 'Salinity: Seabird'.
	 * @return The long version of the sensor's name
	 */
	public String getLongSalinityName3() {
		return "Salinity: " + salinityName3;
	}

	/**
	 * Get the long version of equilibrator temperature 1's name. This includes the sensor type,
	 * e.g. 'Equilibrator Temperature: PT100'.
	 * @return The long version of the sensor's name
	 */
	public String getLongEqtName1() {
		return "Equilibrator Temperature: " + eqtName1;
	}

	/**
	 * Get the long version of equilibrator temperature 2's name. This includes the sensor type,
	 * e.g. 'Equilibrator Temperature: PT100'.
	 * @return The long version of the sensor's name
	 */
	public String getLongEqtName2() {
		return "Equilibrator Temperature: " + eqtName2;
	}

	/**
	 * Get the long version of equilibrator temperature 3's name. This includes the sensor type,
	 * e.g. 'Equilibrator Temperature: PT100'.
	 * @return The long version of the sensor's name
	 */
	public String getLongEqtName3() {
		return "Equilibrator Temperature: " + eqtName3;
	}

	/**
	 * Get the long version of equilibrator pressure 1's name. This includes the sensor type,
	 * e.g. 'Equilibrator Pressure: Omega'.
	 * @return The long version of the sensor's name
	 */
	public String getLongEqpName1() {
		return "Equilibrator Pressure: " + eqpName1;
	}

	/**
	 * Get the long version of equilibrator pressure 2's name. This includes the sensor type,
	 * e.g. 'Equilibrator Pressure: Omega'.
	 * @return The long version of the sensor's name
	 */
	public String getLongEqpName2() {
		return "Equilibrator Pressure: " + eqpName2;
	}

	/**
	 * Get the long version of equilibrator pressure 3's name. This includes the sensor type,
	 * e.g. 'Equilibrator Pressure: Omega'.
	 * @return The long version of the sensor's name
	 */
	public String getLongEqpName3() {
		return "Equilibrator Pressure: " + eqpName3;
	}
	
	/**
	 * Returns the number of columns in the instrument's raw data files
	 * @return The number of columns in the instrument's raw data files
	 */
	public int getRawFileColumnCount() {
		return rawFileColumnCount;
	}
	
	/**
	 * Sets the number of columns in the instrument's raw data files
	 * @param rawFileColumnCount The number of columns
	 */
	public void setRawFileColumnCount(int rawFileColumnCount) {
		this.rawFileColumnCount = rawFileColumnCount;
	}

}
