package uk.ac.exeter.QuinCe.web.Instrument;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.FileUploadBean;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 *
 */
public class NewInstrumentBean extends FileUploadBean implements Serializable {

	private static final long serialVersionUID = 6578490211991423884L;

	static {
		FORM_NAME = "instrumentForm";
	}

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
	 * The navigation to the instrment list
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
	
	/**
	 * The name of the instrument
	 */
	private String name = null;
	
	/**
	 * The type of separator used in the data file
	 */
	private String separator = SEPARATOR_COMMA;
	
	/**
	 * The character used as a separator if it is not comma or tab
	 */
	private String otherSeparatorChar = null;
	
	/**
	 * Indicates whether or not samples are dried before being measured
	 */
	private boolean samplesDried = false;
	
	/**
	 * The contents of the uploaded sample file, as a list of String arrays.
	 * Each list entry is a line, and each line is an array of Strings.
	 */
	private List<Map<Integer, String>> sampleFileContents = null;
	
	/**
	 * The number of files in the sample file
	 */
	private int sampleFileColumnCount = -1;
	
	/**
	 * The number of header lines in the data file
	 */
	private int headerLines = 0;
	
	/**
	 * The result of the sample file extraction
	 */
	private int sampleFileExtractionResult = EXTRACTION_OK;
	
	/**
	 * The message from the sample file extraction
	 */
	private String sampleFileExtractionMessage = "The extraction has not been run";
	
	/**
	 * The column specification for the instrument's data file
	 */
	private ColumnSpec columnSpec = null;
	
	/**
	 * The set of run type classifications, as reported from the HTML form
	 */
	private String runTypeClassifications = null;
	
	/**
	 * The run types in recorded by the instrument and their classification
	 */
	private Map<String, Integer> runTypes = null;
	
	/**
	 * Required basic constructor
	 */
	public NewInstrumentBean() {
		// Do nothing
	}
	
	/**
	 * Initialise the sub-components of the bean
	 */
	@PostConstruct
	public void init() {
		columnSpec = new ColumnSpec(this);
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
	 */
	public String goToFileSpec() {
		return PAGE_FILE_SPEC;
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
		columnSpec.processColumnSelection();
		return PAGE_RUN_TYPES;
	}
	
	/**
	 * Navigate to the sensor names page
	 * @return The navigation result
	 */
	public String goToNamesFromFileSpec() {
		String result = PAGE_NAMES;
		
		if (!validateFileSpec()) {
			result = PAGE_FILE_SPEC;
		}
		
		return result;
	}
	
	/**
	 * Navigate to the sample file upload page
	 * @return The navigation result
	 */
	public String goToSampleUploadFromFileSpec() {
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
		name = null;
		separator = SEPARATOR_COMMA;
		otherSeparatorChar = null;
		samplesDried = false;
		file = null;
		sampleFileContents = null;
		sampleFileColumnCount = -1;
		headerLines = 0;
		sampleFileExtractionResult = EXTRACTION_OK;
		sampleFileExtractionMessage = "The extraction has not been run";
		runTypeClassifications = null;
		runTypes = null;
		
		columnSpec = new ColumnSpec(this);
	}
	
	/**
	 * Extract the contents of the uploaded sample file.
	 */
	@Override
	public void processUploadedFile() {
		
		sampleFileContents = new ArrayList<Map<Integer, String>>();

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(getFile().getInputstream()));		
			String line;
			int lineCount = 0;
			while ((line = in.readLine()) != null) {
				String[] splitLine = line.split(getSeparatorCharacter());
				lineCount++;
				if (lineCount == 1) {
					if (splitLine.length == 1) {
						setSampleFileExtractionError("The source file has only one column. Please check that you have specified the correct column separator.");
						break;
					} else {
						setSampleFileColumnCount(splitLine.length);
					}
				} else {
					if (getSampleFileColumnCount() != splitLine.length) {
						setSampleFileExtractionError("The file does not contain a consistent number of columns (line " + lineCount + ").");
						break;
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
	 * Get the separator character as a String literal
	 * @return The separator character
	 */
	public String getSeparatorCharacter() {
		String result;
		
		switch(separator) {
		case SEPARATOR_COMMA: {
			result = ",";
			break;
		}
		case SEPARATOR_TAB: {
			result = "\t";
			break;
		}
		case SEPARATOR_SPACE: {
			result = "  *";
			break;
		}
		case SEPARATOR_OTHER: {
			result = otherSeparatorChar;
			break;
		}
		default: {
			result = ",";
		}
		}
		
		return result;
	}
	
	/**
	 * Retrieve the number of columns in the sample file
	 * @return The number of columns in the sample file
	 */
	public int getSampleFileColumnCount() {
		return sampleFileColumnCount;
	}
	
	/**
	 * Set the number of columns in the sample file
	 * @param sampleFileColumnCount The number of columns in the sample file
	 */
	public void setSampleFileColumnCount(int sampleFileColumnCount) {
		this.sampleFileColumnCount = sampleFileColumnCount;
	}
	
	/**
	 * Returns the result of the sample file extraction. One of
	 * {@link EXTRACTION_OK} or {@link EXTRACTION_FAILED}.
	 * @return The result of the sample file extraction
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
	}
	
	/**
	 * Returns the flag indicating whether or not samples are dried before being analysed
	 * @return The flag indicating whether or not samples are dried before being analysed
	 */
	public String getSamplesDried() {
		return String.valueOf(samplesDried);
	}
	
	/**
	 * Sets the flag indicating whether or not samples are dried before being analysed
	 * @param samplesDried The flag value
	 */
	public void setSamplesDried(String samplesDried) {
		this.samplesDried = Boolean.parseBoolean(samplesDried);
	}
	
	/**
	 * Returns the number of header lines in the sample file
	 * @return The number of header lines in the sample file
	 */
	public int getHeaderLines() {
		return headerLines;
	}
	
	/**
	 * Sets the number of header lines in the sample file
	 * @param headerLines The number of header lines in the sample file
	 */
	public void setHeaderLines(int headerLines) {
		this.headerLines = headerLines;
	}

	/**
	 * Returns the ColumnSpec object for the current instrement
	 * @return The ColumnSpec object
	 */
	public ColumnSpec getColumnSpec() {
		return columnSpec;
	}
	
	/**
	 * Returns the first 50 lines of the sample file
	 * @return The first 50 lines of the sample file
	 */
	public List<Map<Integer, String>> getTruncatedSample() {
		return sampleFileContents.subList(0, 49);
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
		
		for (int i = 0; i < sampleFileContents.get(0).size(); i++) {
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
		
		int runTypesCol = columnSpec.getRunTypeCol();
		
		for (int i = headerLines; i < sampleFileContents.size(); i++) {
			result.add(sampleFileContents.get(i).get(runTypesCol));
		}
		
		return result;
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
	 * Process the output of the Run Types page.
	 * Extracts the run types, and saves all the instrument
	 * details to the database
	 * @return The navigation result.
	 */
	public String processRunTypes() {
		extractRunTypes();
		saveInstrument();
		return PAGE_INSTRUMENT_LIST;
	}
	
	/**
	 * Extract the run types as provided by the form
	 * into a format that can be used to save values into the database
	 */
	private void extractRunTypes() {
		List<Integer> classifications = StringUtils.delimitedToIntegerList(runTypeClassifications);
		TreeSet<String> runTypeNames = getRunTypesList();
		
		runTypes = new HashMap<String, Integer>(runTypeNames.size());
		
		int count = 0;
		for (String name : runTypeNames) {
			runTypes.put(name, classifications.get(count));
			count++;
		}
	}
	
	/**
	 * Saves all the details of the instrument to the database
	 */
	private void saveInstrument() {
		// TODO This should all be done in a single transaction
	}
}
