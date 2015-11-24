package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedProperty;

import uk.ac.exeter.QuinCe.web.FileUploadBean;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 *
 */
public class NewInstrumentBean extends FileUploadBean {

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
	private List<String[]> sampleFileContents = null;
	
	/**
	 * The number of files in the sample file
	 */
	private int sampleFileColumnCount = -1;
	
	/**
	 * The utility for extracting the contents of the sample file.
	 */
	private SampleFileExtractor sampleFileExtractor = null;
	
	/**
	 * The result of the sample file extraction
	 */
	private int sampleFileExtractionResult = EXTRACTION_ERROR;
	
	/**
	 * The message from the sample file extraction
	 */
	private String sampleFileExtractionMessage = "The extraction has not been run";
	
	/**
	 * The column specification for the instrument's data file
	 */
	@ManagedProperty("#{columnSpecBean}")
	private ColumnSpecBean columnSpecBean;
	
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
	
	public String postSampleFileNavigation() {
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
		file = null;
		sampleFileContents = null;
		
		if (null != sampleFileExtractor) {
			sampleFileExtractor.terminate();
			sampleFileExtractor = null;
		}
		
		if (null != columnSpecBean) {
			columnSpecBean.clearData();
		}
	}
	
	/**
	 * Kick off the background thread that will extract the
	 * contents of the uploaded sample file.
	 */
	@Override
	public void processUploadedFile() {
		
		sampleFileContents = new ArrayList<String[]>();
		
		if (null != sampleFileExtractor) {
			sampleFileExtractor.terminate();
		}
		
		sampleFileExtractor = new SampleFileExtractor(this);
		Thread extractorThread = new Thread(sampleFileExtractor);
		extractorThread.start();
	}
	
	public void setColumnSpecBean(ColumnSpecBean columnSpecBean) {
		this.columnSpecBean = columnSpecBean;
	}
	
	/**
	 * Add a line of fields to the extracted sample file contents
	 * @param line The line to be added
	 */
	protected void addSampleFileLine(String[] line) {
		synchronized(sampleFileContents) {
			sampleFileContents.add(line);
		}
	}
	
	/**
	 * Retrieve the progress of the sample file extractor.
	 * If it is not active, the progress will be 1.
	 * @return The progress of the sample file extractor.
	 */
	public int getExtractionProgress() {
		int progress = 1;
		
		if (null != sampleFileExtractor) {
			progress = sampleFileExtractor.getProgress();
		}
		
		return progress;
	}
	
	/**
	 * Signalled from the front end to state that it has
	 * noticed sample file extraction progress reaching 100%.
	 */
	public void finishExtraction() {
		// Reset the progress to zero ready for the next extraction
		sampleFileExtractor.resetProgress();
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
}
