package uk.ac.exeter.QuinCe.web.Instrument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunType;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 *
 */
public class NewInstrumentBean extends FileUploadBean implements Serializable {

	private static final long serialVersionUID = 3590765853235157932L;

	//////////////// *** CONSTANTS *** ///////////////////////
	
	/**
	 * Navigation result when the new instrument process is cancelled
	 */
	private static final String PAGE_CANCEL = "cancel";

	/**
	 * The navigation to the sensor names page
	 */
	private static final String PAGE_NAMES = "names";
	
	/**
	 * The navigation to the file specification page
	 */
	private static final String PAGE_FILE_SPEC = "filespec";
	
	/**
	 * The navigation to the sample file upload page
	 */
	private static final String PAGE_SAMPLE_FILE = "sample_file";
	
	/**
	 * The navigation to the column selection page
	 */
	private static final String PAGE_COLUMN_SELECTION = "column_selection";

	/**
	 * The navigation to the run type selection page
	 */
	private static final String PAGE_RUN_TYPES = "run_types";
	
	/**
	 * The navigation to the instrument list
	 */
	private static final String PAGE_INSTRUMENT_LIST = "instrument_list";
	
	/**
	 * Indicates a comma separator
	 */
	public static final String SEPARATOR_COMMA = "comma";
	
	/**
	 * Indicates a tab separator
	 */
	public static final String SEPARATOR_TAB = "tab";
	
	/**
	 * Indicates a space separator
	 */
	public static final String SEPARATOR_SPACE = "space";
	
	/**
	 * Indicates a separator other than comma or tab
	 */
	public static final String SEPARATOR_OTHER = "other";
	
	/**
	 * The Component ID of the form input for the alternative separator character
	 */
	private static final String OTHER_SEPARATOR_CHAR_COMPONENT = "otherSeparatorChar";
	
	/**
	 * Indicates that the sample file extraction completed successfully
	 */
	public static final int EXTRACTION_OK = 0;
	
	/**
	 * Indicates that the sample file extraction failed
	 */
	public static final int EXTRACTION_ERROR = 1;
	
	/////////////////// *** FIELDS *** ///////////////////
	
	/**
	 * The type of separator used in the data file
	 */
	private String separator = SEPARATOR_COMMA;
	
	/**
	 * The character used as a separator if it is not comma or tab
	 */
	private String otherSeparatorChar = null;
	
	/**
	 * The contents of the uploaded sample file, as a list of String arrays.
	 * Each list entry is a line, and each line is an array of Strings.
	 */
	private List<Map<Integer, String>> sampleFileContents = null;
	
	/**
	 * The result of the sample file extraction
	 */
	private int sampleFileExtractionResult = EXTRACTION_OK;
	
	/**
	 * The message from the sample file extraction
	 */
	private String sampleFileExtractionMessage = "The extraction has not been run";
	
	/**
	 * The instrument details for storage in the database
	 */
	private Instrument instrumentDetails = null;
	
	/**
	 * The set of run type classifications, as reported from the HTML form
	 */
	private String runTypeClassifications = null;
	
	/**
	 * The list of columns selected on the column selection page
	 */
	private String columnSelection = null;
		
	////////// *** MAIN METHODS *** ///////////////
	/**
	 * Required basic constructor. All the actual construction
	 * is done in init().
	 */
	public NewInstrumentBean() {
		// Do nothing
	}
	
	/**
	 * Initialise the sub-components of the bean
	 */
	@PostConstruct
	public void init() {
		instrumentDetails = new Instrument(getUser().getDatabaseID());
	}
	
	/**
	 * Begin the process of adding a new instrument.
	 * Clear any existing data and go to the sensor names page
	 * @return The navigation result
	 */
	public String start() {
		clearData();
		return PAGE_NAMES;
	}
	
	/**
	 * Abort the collection of data about this new instrument.
	 * Returns the user to the instrument list
	 * @return The navigation result
	 */
	public String cancelInstrument() {
		clearData();
		return PAGE_CANCEL;
	}
	
	/**
	 * Navigate to the file specification page
	 * @return The navigation result
	 * @throws ResourceException 
	 * @throws DatabaseException 
	 * @throws MissingParamException 
	 */
	public String goToFileSpec() throws MissingParamException, DatabaseException, ResourceException {
		
		String result = PAGE_FILE_SPEC;
		
		if (InstrumentDB.instrumentExists(ServletUtils.getDBDataSource(), getUser(), instrumentDetails.getName())) {
			setMessage(getComponentID("instrumentName"), "An instrument with that name already exists");
			result = PAGE_NAMES;
		}

		return result;
	}
	
	/**
	 * Process the column selection
	 * @return The navigation result
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 */
	public String processColumnSelection() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		List<Integer> selectedColumns = StringUtils.delimitedToIntegerList(columnSelection);
		List<Integer> requiredColumns = getColumnList();
		
		for (int i = 0; i < requiredColumns.size(); i++) {
			instrumentDetails.setColumnAssignment(requiredColumns.get(i), selectedColumns.get(i));
		}
		return PAGE_RUN_TYPES;
	}
	
	/**
	 * Navigate to the sample file upload page
	 * @return The navigation result
	 */
	public String goToSampleUploadFromFileSpec() {
		
		// Record the separator character in the instrument details
		instrumentDetails.setSeparatorChar(getSeparatorCharacter());

		
		String result = PAGE_SAMPLE_FILE;
		
		if (!validateFileSpec()) {
			result = PAGE_FILE_SPEC;
		}
		
		return result;
	}
	
	/**
	 * Check the result of the sample file extraction and navigate to the appropriate page 
	 * @return The navigation result
	 */
	public String postSampleFileUploadNavigation() {
		String result = PAGE_COLUMN_SELECTION;
		
		if (sampleFileExtractionResult == EXTRACTION_ERROR) {
			result = PAGE_SAMPLE_FILE;
			setMessage(null, sampleFileExtractionMessage);
		}
		
		return result;
	}
	
	/**
	 * Custom validation of entries on the file_spec page
	 * @return {@code true} if the validation passes; {@code false} if it doesn't.
	 */
	private boolean validateFileSpec() {
		boolean ok = true;
		
		if (separator.equals(SEPARATOR_OTHER)) {
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
		separator = SEPARATOR_COMMA;
		otherSeparatorChar = null;
		file = null;
		sampleFileContents = null;
		sampleFileExtractionResult = EXTRACTION_OK;
		sampleFileExtractionMessage = "The extraction has not been run";
		runTypeClassifications = null;
		instrumentDetails = new Instrument(getUser().getDatabaseID());
	}
	
	/**
	 * Extract the contents of the uploaded sample file.
	 */
	@Override
	public void processUploadedFile() {
		
		clearSampleFileExtractionError();
		
		sampleFileContents = new ArrayList<Map<Integer, String>>();

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(getFile().getInputstream(), StandardCharsets.UTF_8));		
			String line;
			int lineCount = 0;
			while ((line = in.readLine()) != null) {
				String[] splitLine = line.split(instrumentDetails.getColumnSplitString());
				lineCount++;
				if (lineCount == 1) {
					if (splitLine.length == 1) {
						setSampleFileExtractionError("The source file has only one column. Please check that you have specified the correct column separator.");
						break;
					} else {
						instrumentDetails.setRawFileColumnCount(splitLine.length);
					}
				}
				
				Map<Integer, String> lineMap = new HashMap<Integer, String>();
				for (int i = 0; i < splitLine.length; i++) {
					lineMap.put(i, splitLine[i]);
				}
				
				synchronized(sampleFileContents) {
					sampleFileContents.add(lineMap);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			setSampleFileExtractionError("An unexpected error occurred. Please try again.");
		}
	}
	
	/**
	 * Get the separator character as a String literal
	 * @return The separator character
	 */
	public char getSeparatorCharacter() {
		char result;
		
		switch(separator) {
		case SEPARATOR_COMMA: {
			result = ',';
			break;
		}
		case SEPARATOR_TAB: {
			result = '\t';
			break;
		}
		case SEPARATOR_SPACE: {
			result = ' ';
			break;
		}
		case SEPARATOR_OTHER: {
			result = otherSeparatorChar.toCharArray()[0];
			break;
		}
		default: {
			result = ',';
		}
		}
		
		return result;
	}

	/**
	 * Process the output of the Run Types page.
	 * Extracts the run types, and saves all the instrument
	 * details to the database
	 * @return The navigation result.
	 */
	public String processRunTypes() {
		extractRunTypes();
		
		String result = PAGE_INSTRUMENT_LIST;
		
		try {
			InstrumentDB.addInstrument(ServletUtils.getDBDataSource(), instrumentDetails);
		} catch (Exception e) {
			result = internalError(e);
		}
			
		return result;
	}
	
	/**
	 * Returns the column name for the specified column
	 * @param column The column
	 * @return The column name
	 */
	private String getColumnName(int column) {
		String result;
		
		switch (column) {
		case Instrument.COL_RUN_TYPE: {
			result = "Run type";
			break;
		}
		case Instrument.COL_YEAR: {
			result = "Year";
			break;
		}
		case Instrument.COL_MONTH: {
			result = "Month";
			break;
		}
		case Instrument.COL_DAY: {
			result = "Day";
			break;
		}
		case Instrument.COL_DATE: {
			result = "Date";
			break;
		}
		case Instrument.COL_HOUR: {
			result = "Hour";
			break;
		}
		case Instrument.COL_MINUTE: {
			result = "Minute";
			break;
		}
		case Instrument.COL_SECOND: {
			result = "Second";
			break;
		}
		case Instrument.COL_TIME: {
			result = "Time";
			break;
		}
		case Instrument.COL_CUSTOM_DATETIME_FORMAT: {
			result = "Date/Time";
			break;
		}
		case Instrument.COL_LONGITUDE: {
			result = "Longitude";
			break;
		}
		case Instrument.COL_EAST_WEST: {
			result = "East/West";
			break;
		}
		case Instrument.COL_LATITUDE: {
			result = "Latitude";
			break;
		}
		case Instrument.COL_NORTH_SOUTH: {
			result = "North/South";
			break;
		}
		case Instrument.COL_INTAKE_TEMP_1: {
			result = instrumentDetails.getLongIntakeTempName1();
			break;
		}
		case Instrument.COL_INTAKE_TEMP_2: {
			result = instrumentDetails.getLongIntakeTempName2();
			break;
		}
		case Instrument.COL_INTAKE_TEMP_3: {
			result = instrumentDetails.getLongIntakeTempName3();
			break;
		}
		case Instrument.COL_SALINITY_1: {
			result = instrumentDetails.getLongSalinityName1();
			break;
		}
		case Instrument.COL_SALINITY_2: {
			result = instrumentDetails.getLongSalinityName2();
			break;
		}
		case Instrument.COL_SALINITY_3: {
			result = instrumentDetails.getLongSalinityName3();
			break;
		}
		case Instrument.COL_EQT_1: {
			result = instrumentDetails.getLongEqtName1();
			break;
		}
		case Instrument.COL_EQT_2: {
			result = instrumentDetails.getLongEqtName2();
			break;
		}
		case Instrument.COL_EQT_3: {
			result = instrumentDetails.getLongEqtName3();
			break;
		}
		case Instrument.COL_EQP_1: {
			result = instrumentDetails.getLongEqpName1();
			break;
		}
		case Instrument.COL_EQP_2: {
			result = instrumentDetails.getLongEqpName2();
			break;
		}
		case Instrument.COL_EQP_3: {
			result = instrumentDetails.getLongEqpName3();
			break;
		}
		case Instrument.COL_AIR_FLOW_1: {
			result = instrumentDetails.getLongAirFlowName1();
			break;
		}
		case Instrument.COL_AIR_FLOW_2: {
			result = instrumentDetails.getLongAirFlowName2();
			break;
		}
		case Instrument.COL_AIR_FLOW_3: {
			result = instrumentDetails.getLongAirFlowName3();
			break;
		}
		case Instrument.COL_WATER_FLOW_1: {
			result = instrumentDetails.getLongWaterFlowName1();
			break;
		}
		case Instrument.COL_WATER_FLOW_2: {
			result = instrumentDetails.getLongWaterFlowName2();
			break;
		}
		case Instrument.COL_WATER_FLOW_3: {
			result = instrumentDetails.getLongWaterFlowName3();
			break;
		}
		case Instrument.COL_ATMOSPHERIC_PRESSURE: {
			result = "Atmospheric Pressure";
			break;
		}
		case Instrument.COL_XH2O: {
			result = "xH2O";
			break;
		}
		case Instrument.COL_CO2: {
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
	 * Returns a basic list of columns to be used
	 * when building the view of the sample file.
	 * 
	 * Each column is named col_&lt;index&gt;.
	 * 
	 * These column names are never actually displayed,
	 * but they are required as placeholders for the view.
	 * 
	 * @return A list of columns
	 */
	public List<SampleFileColumn> getColumns() {
		
		List<SampleFileColumn> columns = new ArrayList<SampleFileColumn>();
		
		for (int i = 0; i < instrumentDetails.getRawFileColumnCount(); i++) {
			columns.add(new SampleFileColumn(i));
		}
		
		return columns;
	}
	
	/**
	 * Returns an ordered set of unique values from the Run Types column
	 * in the sample data file
	 * @return The run type values
	 */
	public TreeSet<String> getRunTypesList() {
		TreeSet<String> result = new TreeSet<String>();
		
		int runTypesCol = instrumentDetails.getColumnAssignment(Instrument.COL_RUN_TYPE);
		
		for (int i = instrumentDetails.getHeaderLines(); i < sampleFileContents.size(); i++) {
			result.add(sampleFileContents.get(i).get(runTypesCol));
		}
		
		return result;
	}

	////////////// *** GETTERS AND SETTERS *** /////////////////
	/**
	 * Get the separator for the file. One of {@link #SEPARATOR_COMMA},
	 * {@link #SEPARATOR_TAB}, {@link #SEPARATOR_SPACE}, or {@link #SEPARATOR_OTHER}.
	 * @return The separator for the file
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * 
	 * Set the separator for the file. Must be one of 
	 * {@link #SEPARATOR_COMMA}, {@link #SEPARATOR_TAB}, {@link #SEPARATOR_SPACE}, or {@link #SEPARATOR_OTHER}.
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
	 * Returns the result of the sample file extraction. One of
	 * {@link EXTRACTION_OK} or {@link EXTRACTION_FAILED}.
	 * @return The result of the sample file extraction
	 * @see #processUploadedFile()
	 */
	public int getSampleFileExtractionResult() {
		return sampleFileExtractionResult;
	}
	
	/**
	 * Set the result of the sample file extraction
	 * @param sampleFileExtractionResult The result of the sample file extraction
	 */
	public void setSampleFileExtractionResult(int sampleFileExtractionResult) {
		this.sampleFileExtractionResult = sampleFileExtractionResult;
	}
	
	/**
	 * Get the message generated during the sample file extraction
	 * @return The message generated during the sample file extraction
	 */
	public String getSampleFileExtractionMessage() {
		return sampleFileExtractionMessage;
	}
	
	/**
	 * Shortcut method for recording a failure of the sample file extraction
	 * @param errorMessage The error message
	 */
	public void setSampleFileExtractionError(String errorMessage) {
		sampleFileExtractionResult = EXTRACTION_ERROR;
		sampleFileExtractionMessage = errorMessage;
		clearFile();
	}
	
	/**
	 * Reset the sample file extraction result and message
	 */
	public void clearSampleFileExtractionError() {
		sampleFileExtractionResult = EXTRACTION_OK;
		sampleFileExtractionMessage = "The extraction has not been run";
	}
	
	/**
	 * Returns the InstrumentDetails object for the current instrument
	 * @return The InstrumentDetails object
	 */
	public Instrument getInstrumentDetails() {
		return instrumentDetails;
	}
	
	/**
	 * Returns the first 50 lines of the sample file
	 * @return The first 50 lines of the sample file
	 */
	public List<Map<Integer, String>> getTruncatedSample() {
		return sampleFileContents.subList(0, 49);
	}
	
	
	/**
	 * Get the list of run type classifications as returned from the HTML form
	 * @return The list of run type classifications
	 */
	public String getRunTypeClassifications() {
		return runTypeClassifications;
	}
	
	/**
	 * Get the list of run type classifications as returned from the HTML form
	 * @param runTypeClassifications The list of run type classifications
	 */
	public void setRunTypeClassifications(String runTypeClassifications) {
		this.runTypeClassifications = runTypeClassifications;
	}
	
	/**
	 * Extract the run types as provided by the form
	 * into a format that can be used to save values into the database
	 */
	private void extractRunTypes() {
		List<Integer> classifications = StringUtils.delimitedToIntegerList(runTypeClassifications);
		TreeSet<String> runTypeNames = getRunTypesList();
		
		TreeSet<RunType> runTypes = new TreeSet<RunType>();
		
		int count = 0;
		for (String name : runTypeNames) {
			runTypes.add(new RunType(name, classifications.get(count)));
			count++;
		}
		
		instrumentDetails.setRunTypes(runTypes);
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
		
		columnList.add(Instrument.COL_RUN_TYPE);

		if (instrumentDetails.getCustomDateTimeFormat()) {
			columnList.add(Instrument.COL_CUSTOM_DATETIME_FORMAT);
		} else {
		
			if (instrumentDetails.getDateFormat() == Instrument.SEPARATE_FIELDS) {
				columnList.add(Instrument.COL_YEAR);
				columnList.add(Instrument.COL_MONTH);
				columnList.add(Instrument.COL_DAY);
			} else {
				columnList.add(Instrument.COL_DATE);
			}
			
			if (instrumentDetails.getTimeFormat() == Instrument.SEPARATE_FIELDS) {
				columnList.add(Instrument.COL_HOUR);
				columnList.add(Instrument.COL_MINUTE);
				columnList.add(Instrument.COL_SECOND);
			} else {
				columnList.add(Instrument.COL_TIME);
			}
		}
		
		columnList.add(Instrument.COL_LONGITUDE);
		if (instrumentDetails.getLonFormat() == Instrument.LON_FORMAT_0_180) {
			columnList.add(Instrument.COL_EAST_WEST);
		}
		
		columnList.add(Instrument.COL_LATITUDE);
		if (instrumentDetails.getLatFormat() == Instrument.LAT_FORMAT_0_90) {
			columnList.add(Instrument.COL_NORTH_SOUTH);
		}
		
		columnList.add(Instrument.COL_INTAKE_TEMP_1);
		
		if (instrumentDetails.hasIntakeTemp2()) {
			columnList.add(Instrument.COL_INTAKE_TEMP_2);
		}
		
		if (instrumentDetails.hasIntakeTemp3()) {
			columnList.add(Instrument.COL_INTAKE_TEMP_3);
		}
		
		columnList.add(Instrument.COL_SALINITY_1);
		
		if (instrumentDetails.hasSalinity2()) {
			columnList.add(Instrument.COL_SALINITY_2);
		}
		
		if (instrumentDetails.hasSalinity3()) {
			columnList.add(Instrument.COL_SALINITY_3);
		}
		
		columnList.add(Instrument.COL_EQT_1);
		
		if (instrumentDetails.hasEqt2()) {
			columnList.add(Instrument.COL_EQT_2);
		}
		
		if (instrumentDetails.hasEqt3()) {
			columnList.add(Instrument.COL_EQT_3);
		}
		
		columnList.add(Instrument.COL_EQP_1);
		
		if (instrumentDetails.hasEqp2()) {
			columnList.add(Instrument.COL_EQP_2);
		}
		
		if (instrumentDetails.hasEqp3()) {
			columnList.add(Instrument.COL_EQP_3);
		}
		
		if (instrumentDetails.hasAirFlow1()) {
			columnList.add(Instrument.COL_AIR_FLOW_1);
		}
		
		if (instrumentDetails.hasAirFlow2()) {
			columnList.add(Instrument.COL_AIR_FLOW_2);
		}
		
		if (instrumentDetails.hasAirFlow3()) {
			columnList.add(Instrument.COL_AIR_FLOW_3);
		}
		
		if (instrumentDetails.hasWaterFlow1()) {
			columnList.add(Instrument.COL_WATER_FLOW_1);
		}
		
		if (instrumentDetails.hasWaterFlow2()) {
			columnList.add(Instrument.COL_WATER_FLOW_2);
		}
		
		if (instrumentDetails.hasWaterFlow3()) {
			columnList.add(Instrument.COL_WATER_FLOW_3);
		}
		
		if (instrumentDetails.getHasAtmosphericPressure()) {
			columnList.add(Instrument.COL_ATMOSPHERIC_PRESSURE);
		}
		
		if (!instrumentDetails.getSamplesDried()) {
			columnList.add(Instrument.COL_XH2O);
		}
	
		columnList.add(Instrument.COL_CO2);
		
		return columnList;
	}
	
	/**
	 * Returns the list of columns selected on the column selection page
	 * @return The list of columns selected on the column selection page
	 */
	public String getColumnSelection() {
		return columnSelection;
	}
	
	/**
	 * Sets the list of columns selected on the column selection page
	 * @param columnSelection The list of columns
	 */
	public void setColumnSelection(String columnSelection) {
		this.columnSelection = columnSelection;
	}

	@Override
	protected String getFormName() {
		return "instrumentForm";
	}
}
