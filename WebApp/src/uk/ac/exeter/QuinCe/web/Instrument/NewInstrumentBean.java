package uk.ac.exeter.QuinCe.web.Instrument;

import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 *
 */
public class NewInstrumentBean extends BaseManagedBean {

	static {
		FORM_NAME = "instrumentForm";
	}

	/**
	 * Navigation result when the new instrument process is cancelled
	 */
	private static final String CANCEL_PAGE = "cancel";

	/**
	 * The navigation to the sensor names page
	 */
	private static final String NAMES_PAGE = "names";
	
	/**
	 * The navigation to the file specification page
	 */
	private static final String FILE_SPEC_PAGE = "filespec";
	
	/**
	 * Indicates a comma separator
	 */
	public static final String COMMA_SEPARATOR = "comma";
	
	/**
	 * Indicates a tab separator
	 */
	public static final String TAB_SEPARATOR = "tab";
	
	/**
	 * Indicates a separator other than comma or tab
	 */
	public static final String OTHER_SEPARATOR = "other";
	
	/**
	 * The Component ID of the form input for the alternative separator character
	 */
	private static final String OTHER_SEPARATOR_CHAR_COMPONENT = "otherSeparatorChar";
	
	/**
	 * Indicates that date or time components are stored in separate fields
	 */
	private static final String SEPARATE_FIELDS = "separate";
	
	/**
	 * Indicates YYYYMMDD date format
	 */
	public static final String YYYYMMDD_DATE_FORMAT = "YYYYMMDD";
	
	/**
	 * Indicates DD/MM/YY date format
	 */
	public static final String DDMMYY_DATE_FORMAT = "DDMMYY";
	
	/**
	 * Indicates DD/MM/YYYY date format
	 */
	public static final String DDMMYYYY_DATE_FORMAT = "DDMMYYYY";
	
	/**
	 * Indicates MM/DD/YY date format
	 */
	public static final String MMDDYY_DATE_FORMAT = "MMDDYY";
	
	/**
	 * Indicates MM/DD/YYYY date format
	 */
	public static final String MMDDYYYY_DATE_FORMAT = "MMDDYYYY";
	
	/**
	 * The name of the instrument
	 */
	private String name = null;
	
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
	 * The type of separator used in the data file
	 */
	private String separator = COMMA_SEPARATOR;
	
	/**
	 * The character used as a separator if it is not comma or tab
	 */
	private String otherSeparatorChar = null;
	
	private String dateFormat = SEPARATE_FIELDS;
	
	/**
	 * Begin the process of adding a new instrument.
	 * Clear any existing data and go to the sensor names page
	 * @return The navigation result
	 */
	public String start() {
		clearData();
		return NAMES_PAGE;
	}
	
	/**
	 * Abort the collection of data about this new instrument.
	 * Returns the user to the instrument list
	 * @return The navigation result
	 */
	public String cancelInstrument() {
		clearData();
		return CANCEL_PAGE;
	}
	
	/**
	 * Navigate to the file specification page
	 * @return The navigation result
	 */
	public String goToFileSpec() {
		return FILE_SPEC_PAGE;
	}
	
	/**
	 * Navigate to the file specification page
	 * @return The navigation result
	 */
	public String goToNamesFromFileSpec() {
		String result = NAMES_PAGE;
		
		if (!validateFileSpec()) {
			result = FILE_SPEC_PAGE;
		}
		
		return result;
	}
	
	/**
	 * Custom validation of entries on the file_spec page
	 * @return {@code true} if the validation passes; {@code false} if it doesn't.
	 */
	private boolean validateFileSpec() {
		boolean ok = true;
		
		if (separator.equals(OTHER_SEPARATOR)) {
			if (null == otherSeparatorChar || otherSeparatorChar.length() == 0) {
				setMessage(getComponentID(OTHER_SEPARATOR_CHAR_COMPONENT), "You must specify the separator character");
				ok = false;
			}
		}
		
		return ok;
	}
		
	/**
	 * Clear all the data from the bean
	 */
	private void clearData() {
		name = null;
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
		separator = COMMA_SEPARATOR;
		otherSeparatorChar = null;
		dateFormat = SEPARATE_FIELDS;
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
	 * Get the separator for the file. One of {@link COMMA_SEPARATOR},
	 * {@link TAB_SEPARATOR}, or {@link OTHER_SEPARATOR}.
	 * @return The separator for the file
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * 
	 * Set the separator for the file. Must be one of 
	 * {@link COMMA_SEPARATOR}, {@link TAB_SEPARATOR}, or {@link OTHER_SEPARATOR}.
	 * @param separator The separator
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	/**
	 * Get the character to be used as a separator, when it isn't a
	 * comma or tab.
	 * @return The character to be used as a separator
	 */
	public String getOtherSeparatorChar() {
		return otherSeparatorChar;
	}
	
	/**
	 * Set the character to be used as a separator, when it isn't a
	 * comma or tab.
	 * @param otherSeparatorChar The separator character
	 */
	public void setOtherSeparatorChar(String otherSeparatorChar) {
		this.otherSeparatorChar = otherSeparatorChar;
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
}
