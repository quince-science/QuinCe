package uk.ac.exeter.QuinCe.web.Instrument;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean to hold details of a data file's column specification.
 * Used as an injected bean to {@link NewInstrumentBean}.
 * @author Steve Jones
 *
 */
public class ColumnSpecBean extends BaseManagedBean {

	static {
		FORM_NAME = "instrumentForm";
	}

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
	 * The name of the first sea level pressure sensor
	 */
	private String slpName1 = null;
	
	/**
	 * The name of the second sea level pressure sensor
	 */
	private String slpName2 = null;
	
	/**
	 * The name of the third sea level pressure sensor
	 */
	private String slpName3 = null;
	
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
	

	
	protected void clearData() {
		intakeTempName1 = null;
		intakeTempName2 = null;
		intakeTempName3 = null;
		salinityName1 = null;
		salinityName2 = null;
		salinityName3 = null;
		slpName1 = null;
		slpName2 = null;
		slpName3 = null;
		eqtName1 = null;
		eqtName2 = null;
		eqtName3 = null;
		eqpName1 = null;
		eqpName2 = null;
		eqpName3 = null;
		dateFormat = SEPARATE_FIELDS;
		timeFormat = SEPARATE_FIELDS;
		lonFormat = LON_FORMAT_0_360;
		latFormat = LAT_FORMAT_MINUS90_90;
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
	 * Get the name of the first sea level pressure sensor
	 * @return The name of the first sea level pressure sensor
	 */
	public String getSlpName1() {
		return slpName1;
	}
	
	/**
	 * Set the name of the first sea level pressure sensor
	 * @param name The name of the first sea level pressure sensor
	 */
	public void setSlpName1(String slpName1) {
		this.slpName1 = slpName1;
	}

	/**
	 * Get the name of the second sea level pressure sensor
	 * @return The name of the second sea level pressure sensor
	 */
	public String getSlpName2() {
		return slpName2;
	}
	
	/**
	 * Set the name of the second sea level pressure sensor
	 * @param name The name of the second sea level pressure sensor
	 */
	public void setSlpName2(String slpName2) {
		this.slpName2 = slpName2;
	}

	/**
	 * Get the name of the third sea level pressure sensor
	 * @return The name of the third sea level pressure sensor
	 */
	public String getSlpName3() {
		return slpName3;
	}
	
	/**
	 * Set the name of the third sea level pressure sensor
	 * @param name The name of the third sea level pressure sensor
	 */
	public void setSlpName3(String slpName3) {
		this.slpName3 = slpName3;
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
}
