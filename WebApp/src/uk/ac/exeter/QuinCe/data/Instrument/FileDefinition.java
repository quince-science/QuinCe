package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LatitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.LongitudeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorAssignments;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Holds a description of a sample data file uploaded during the
 * creation of a new instrument
 * @author Steve Jones
 *
 */
public class FileDefinition implements Comparable<FileDefinition> {

	/**
	 * The name of the Run Type column
	 */
	public static final String RUN_TYPE_COL_NAME = "Run Type";
	
	/**
	 * Inidicates that the file header is defined by a number of lines
	 */
	public static final int HEADER_TYPE_LINE_COUNT = 0;
	
	/**
	 * Indicates that the file header is defined by a specific string
	 */
	public static final int HEADER_TYPE_STRING = 1;
	
	/**
	 * The mapping of separator names to the separator characters
	 */
	private static Map<String, String> SEPARATOR_LOOKUP = null;

	/**
	 * The available separator characters
	 */
	protected static final String[] VALID_SEPARATORS = {"\t", ",", ";", " "};
	
	/**
	 * Menu index for the tab separator
	 */
	public static final int SEPARATOR_INDEX_TAB = 0;
	
	/**
	 * Menu index for the comma separator
	 */
	public static final int SEPARATOR_INDEX_COMMA = 1;
	
	/**
	 * Menu index for the semicolon separator
	 */
	public static final int SEPARATOR_INDEX_SEMICOLON = 2;
	
	/**
	 * Menu index for the space separator
	 */
	public static final int SEPARATOR_INDEX_SPACE = 3;
	
	/**
	 * The name used to identify files of this type
	 */
	private String fileDescription;
	
	/**
	 * The method by which the header is defined.
	 * One of {@link #HEADER_TYPE_LINE_COUNT} or {@link #HEADER_TYPE_STRING}
	 * 
	 * <p>
	 *   For an empty header, use {@link #HEADER_TYPE_LINE_COUNT} and set
	 *   {@link #headerLines} to zero.
	 * </p>
	 */
	private int headerType = HEADER_TYPE_LINE_COUNT;
	
	/**
	 * The number of lines in the header (if the header is defined by a number of lines)
	 */
	private int headerLines = 0;
	
	/**
	 * The string that indicates the end of the header
	 */
	private String headerEndString = null;
	
	/**
	 * The number of rows containing column headers
	 */
	private int columnHeaderRows = 0;
	
	/**
	 * The column separator
	 */
	private String separator = ",";
	
	/**
	 * The number of columns in the file
	 */
	private int columnCount = 0;
	
	/**
	 * The longitude specification 
	 */
	private LongitudeSpecification longitudeSpecification;
	
	/**
	 * The latitude specification
	 */
	private LatitudeSpecification latitudeSpecification;
	
	/**
	 * The Date/Time specification
	 */
	private DateTimeSpecification dateTimeSpecification;
	
	/**
	 * The column containing the run type. Only required if this file
	 * contains a Core Sensor
	 * @see SensorType#isCoreSensor()
	 */
	private int runTypeColumn = -1;
	
	/**
	 * The list of run type values for the instrument
	 */
	protected TreeMap<String, RunTypeCategory> runTypes = null;
	
	/**
	 * The file set of which this definition is a member
	 */
	private InstrumentFileSet fileSet;
	
	static {
		SEPARATOR_LOOKUP = new HashMap<String, String>(4);
		SEPARATOR_LOOKUP.put("TAB", "\t");
		SEPARATOR_LOOKUP.put("COMMA", ",");
		SEPARATOR_LOOKUP.put("SEMICOLON", ";");
		SEPARATOR_LOOKUP.put("SPACE", " ");
	}
	
	/**
	 * Create a new file with the given description.
	 *
	 * @param fileDescription The file description
	 * @param fileSet The file set of which this definition is a member
	 */
	public FileDefinition(String fileDescription, InstrumentFileSet fileSet) {
		this.fileDescription = fileDescription;
		this.longitudeSpecification = new LongitudeSpecification();
		this.latitudeSpecification = new LatitudeSpecification();
		this.dateTimeSpecification = new DateTimeSpecification(this);
		this.fileSet = fileSet;
	}
	
	/**
	 * Get the description for this file
	 * @return The file description
	 */
	public String getFileDescription() {
		return fileDescription;
	}
	
	/**
	 * Set the description for this file
	 * @param fileDescription The file description
	 */
	public void setFileDescription(String fileDescription) {
		this.fileDescription = fileDescription;
	}
	
	/**
	 * Comparison is based on the file description. The comparison is case insensitive.
	 */
	@Override
	public int compareTo(FileDefinition o) {
		return fileDescription.toLowerCase().compareTo(o.fileDescription.toLowerCase());
	}
	
	/**
	 * Equals compares on the unique file description (case insensitive)
	 */
	@Override
	public boolean equals(Object o) {
		boolean result = true;
		
		if (null == o) {
			result = false;
		} else if (!(o instanceof FileDefinition)) {
			result = false;
		} else {
			FileDefinition oFile = (FileDefinition) o;
			result = oFile.fileDescription.toLowerCase() == fileDescription.toLowerCase();
		}
		
		return result;
	}

	/**
	 * Get the number of lines that make up the header.
	 * This is only valid if {@link #headerType} is set to {@link #HEADER_TYPE_LINE_COUNT}.
	 * @return The number of lines in the header
	 */
	public int getHeaderLines() {
		return headerLines;
	}

	/**
	 * Set the number of lines that make up the header
	 * @param headerLines The number of lines in the header
	 */
	public void setHeaderLines(int headerLines) {
		this.headerLines = headerLines;
	}

	/**
	 * Get the string that defines the last line of the header.
	 * This is only valid if {@link #headerType} is set to {@link #HEADER_TYPE_STRING}.
	 * @return The string that defines the last line of the header.
	 */
	public String getHeaderEndString() {
		return headerEndString;
	}

	/**
	 * Set the string that defines the last line of the header.
	 * @param headerEndString The string that defines the last line of the header.
	 */
	public void setHeaderEndString(String headerEndString) {
		this.headerEndString = headerEndString;
	}

	/**
	 * Get the number of column header rows
	 * @return The number of column header rows
	 */
	public int getColumnHeaderRows() {
		return columnHeaderRows;
	}

	/**
	 * Set the number of column header rows
	 * @param columnHeaderRows The number of column header rows
	 */
	public void setColumnHeaderRows(int columnHeaderRows) {
		this.columnHeaderRows = columnHeaderRows;
	}

	/**
	 * Get the file's column separator
	 * @return The separator
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * Get the name for the file's column separator
	 * @return The separator name
	 */
	public String getSeparatorName() {
		
		String result = null;
		
		for (Map.Entry<String, String> entry : SEPARATOR_LOOKUP.entrySet()) {
			if (entry.getValue().equals(separator)) {
				result = entry.getKey();
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Set the column separator. This can use either a separator name
	 * or the separator character itself.
	 * 
	 * @param separator The separator
	 * @throws InvalidSeparatorException If the supplied separator is not supported
	 * @see #SEPARATOR_LOOKUP
	 * @see #VALID_SEPARATORS
	 */
	public void setSeparator(String separator) throws InvalidSeparatorException {
		if (!validateSeparator(separator)) {
			throw new InvalidSeparatorException(separator);
		}
		this.separator = separator;
	}
	
	/**
	 * Set the file's column separator using the separator name
	 * @param separatorName The separator name
	 * @throws InvalidSeparatorException If the separator name is not recognised
	 */
	public void setSeparatorName(String separatorName) throws InvalidSeparatorException {
		if (!SEPARATOR_LOOKUP.containsKey(separatorName)) {
			throw new InvalidSeparatorException(separatorName);
		} else {
			this.separator = SEPARATOR_LOOKUP.get(separatorName);
		}
	}
	
	/**
	 * Ensure that a separator is one of the supported options
	 * @param separator The separator to be checked
	 * @return {@code true} if the separator is supported; {@code false} if it is not
	 */
	public static boolean validateSeparator(String separator) {
		
		boolean separatorValid = true;
		
		if (!SEPARATOR_LOOKUP.containsKey(separator) && !SEPARATOR_LOOKUP.containsValue(separator)) {
			separatorValid = false;
		}

		return separatorValid;
	}
	
	/**
	 * Get the header type of the file. Will be either {@link #HEADER_TYPE_LINE_COUNT} or {@link #HEADER_TYPE_STRING}.
	 * @return The header type.
	 */
	public int getHeaderType() {
		return headerType;
	}
	
	/**
	 * Set the header type of the file. Must be either {@link #HEADER_TYPE_LINE_COUNT} or {@link #HEADER_TYPE_STRING}.
	 * @param headerType The header type
	 * @throws InvalidHeaderTypeException If an invalid header type is specified
	 */
	public void setHeaderType(int headerType) throws InvalidHeaderTypeException {
		if (headerType != HEADER_TYPE_LINE_COUNT && headerType != HEADER_TYPE_STRING) {
			throw new InvalidHeaderTypeException();
		}
		this.headerType = headerType;
	}
	
	/**
	 * Set the header to be defined by a number of lines
	 * @param headerLines The number of lines in the header
	 */
	public void setLineCountHeaderType(int headerLines) {
		this.headerType = HEADER_TYPE_LINE_COUNT;
		this.headerLines = headerLines;
	}
	
	/**
	 * Set the header to be defined by a string that marks the end of the header
	 * @param headerString The string denoting the end of the header
	 */
	public void setStringHeaderType(String headerString) {
		this.headerType = HEADER_TYPE_STRING;
		this.headerEndString = headerString;
	}
	
	/**
	 * Set the column count
	 * @param columnCount The column count
	 */
	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}
	
	/**
	 * Get the column count
	 * @return The column count
	 */
	public int getColumnCount() {
		return columnCount;
	}
	
	/**
	 * Convert a string from a data file into a list of column values
	 * @param dataLine The data line
	 * @return The column values
	 */
	public List<String> makeColumnValues(String dataLine) {
		List<String> values = new ArrayList<String>();
		
		if (separator.equals(" ")) {
			dataLine = dataLine.trim().replaceAll("  *", " ");
		}
		
		values = Arrays.asList(dataLine.split(separator));
		return StringUtils.trimList(values);
	}

	/**
	 * Get the longitude specification
	 * @return The longitude specification
	 */
	public LongitudeSpecification getLongitudeSpecification() {
		return longitudeSpecification;
	}

	/**
	 * Get the latitude specification
	 * @return The latitude specification
	 */
	public LatitudeSpecification getLatitudeSpecification() {
		return latitudeSpecification;
	}

	/**
	 * Get the date/time specification
	 * @return The date/time specification
	 */
	public DateTimeSpecification getDateTimeSpecification() {
		return dateTimeSpecification;
	}
	
	/**
	 * Determine whether or not the file has a header.
	 * 
	 * The header has either with a specified number of header lines or
	 * a header end string.
	 * @return {@code true} if the file has a header; {@code false} if it does not.
	 */
	public boolean hasHeader() {
		return (headerLines > 0 || null != headerEndString);
	}
	
	/**
	 * Get the file set of which this definition is a member
	 * @return The parent file set
	 */
	public InstrumentFileSet getFileSet() {
		return fileSet;
	}
	
	/**
	 * Remove a column assignment from the date/time or position
	 * specification. If there is no assignment for the column,
	 * no action is taken.
	 * @param column The column to be unassigned
	 * @return {@code true} if an assignment was found and removed; {@code false} if not.
	 */
	public boolean removeAssignment(int column) {
	
		boolean unassigned = false;
		
		if (latitudeSpecification.getValueColumn() == column) {
			latitudeSpecification.clearValueColumn();
			unassigned = true;
		} else if (latitudeSpecification.getHemisphereColumn() == column) {
			latitudeSpecification.clearHemisphereColumn();
			unassigned= true;
		}
	
		if (!unassigned) {
			if (longitudeSpecification.getValueColumn() == column) {
				longitudeSpecification.clearValueColumn();
				unassigned = true;
			} else if (longitudeSpecification.getHemisphereColumn() == column) {
				longitudeSpecification.clearHemisphereColumn();
				unassigned= true;
			}
		}
		
		if (!unassigned) {
			DateTimeSpecification dateTime = getDateTimeSpecification();
			unassigned = dateTime.removeAssignment(column);
		}

		return unassigned;
	}
	
	/**
	 * Get the column containing the Run Type
	 * @return The Run Type column
	 */
	public int getRunTypeColumn() {
		return runTypeColumn;
	}
	
	/**
	 * Get the assigned run types for this file. If the
	 * {@link #runTypeColumn} is {@code -1}, this will return
	 * {@code null}.
	 * @return The run types
	 */
	public TreeMap<String, RunTypeCategory> getRunTypes() {
		return runTypes;
	}
	
	/**
	 * Set the index of the column containing the run type
	 * @param runTypeColumn The Run Type column
	 */
	public void setRunTypeColumn(int runTypeColumn) {
		this.runTypeColumn = runTypeColumn;
		initialiseRunTypes();
	}
	
	/**
	 * Initialise the run types data structure
	 */
	public void initialiseRunTypes() {
		if (runTypeColumn == -1) {
			runTypes = null;
		} else {
			runTypes = new TreeMap<String, RunTypeCategory>();
		}
	}
	
	/**
	 * Determines whether or not this file requires a Run Type column.
	 * 
	 * <p>
	 *   If this file has had a Core Sensor assigned to it, then a Run Type
	 *   column is also required. Otherwise, the column is not required.
	 * </p>
	 * @param sensorAssignments The sensor assignments for the current instrument
	 * @return {@code true} if a Run Type column is required; {@code false} otherwise.
	 */
	public boolean requiresRunTypeColumn(SensorAssignments sensorAssignments) {
		return sensorAssignments.coreSensorAssigned(fileDescription);
	}
	
	/**
	 * Set the run type category for a given run type
	 * @param runType The run type
	 * @param category The run type category
	 */
	public void setRunTypeCategory(String runType, RunTypeCategory category) {
		runTypes.put(runType, category);
	}
}
