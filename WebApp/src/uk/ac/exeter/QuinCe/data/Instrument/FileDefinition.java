package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.Arrays;

/**
 * Holds a description of a sample data file uploaded during the
 * creation of a new instrument
 * @author Steve Jones
 *
 */
public class FileDefinition implements Comparable<FileDefinition> {

	/**
	 * Inidicates that the file header is defined by a number of lines
	 */
	public static final int HEADER_TYPE_LINE_COUNT = 0;
	
	/**
	 * Indicates that the file header is defined by a specific string
	 */
	public static final int HEADER_TYPE_STRING = 1;
	
	/**
	 * The supported separators
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
	 */
	private int headerType = HEADER_TYPE_LINE_COUNT;
	
	/**
	 * The number of lines in the header (if the header is defined by a number of lines)
	 */
	private int headerLines = 0;
	
	/**
	 * The string that indicates the end of the header
	 */
	private String headerString = null;
	
	/**
	 * The number of rows containing column headers
	 */
	private int columnHeaderRows = 0;
	
	/**
	 * The column separator
	 */
	private String separator = ",";
	
	/**
	 * Create a new file with the given description
	 * @param fileDescription The file description
	 */
	public FileDefinition(String fileDescription) {
		this.fileDescription = fileDescription;
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
	public String getHeaderString() {
		return headerString;
	}

	/**
	 * Set the string that defines the last line of the header.
	 * @param headerString The string that defines the last line of the header.
	 */
	public void setHeaderString(String headerString) {
		this.headerString = headerString;
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
	 * Get the column separator
	 * @return The separator
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * Get the numerical index of the current separator
	 * @return The separator index
	 */
	public int getSeparatorIndex() {
		return Arrays.asList(VALID_SEPARATORS).indexOf(separator);
	}

	/**
	 * Set the column separator
	 * @param separator The separator
	 * @throws InvalidSeparatorException If the supplied separator is not supported
	 */
	public void setSeparator(String separator) throws InvalidSeparatorException {
		validateSeparator(separator);
		this.separator = separator;
	}
	
	/**
	 * Set the column separator using a numeric index.
	 * @param index The separator index
	 * @throws InvalidSeparatorException If the index is invalid
	 */
	public void setSeparatorIndex(int index) throws InvalidSeparatorException {
		switch (index) {
		case 0: {
			separator = "\t";
			break;
		}
		case 1: {
			separator = ",";
			break;
		}
		case 2: {
			separator = ";";
			break;
		}
		case 3: {
			separator = " ";
			break;
		}
		default: {
			throw new InvalidSeparatorException(index);
		}
		}
	}
	
	/**
	 * Ensure that a separator is one of the supported options
	 * @param separator The separator to be checked
	 * @throws InvalidSeparatorException If the separator is invalid
	 */
	private void validateSeparator(String separator) throws InvalidSeparatorException {
		
		boolean separatorValid = false;
		
		for (int i = 0; i < VALID_SEPARATORS.length; i++) {
			if (VALID_SEPARATORS[i].equals(separator)) {
				separatorValid = true;
				break;
			}
		}

		if (!separatorValid) {
			throw new InvalidSeparatorException(separator);
		}
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
		this.headerString = headerString;
	}
}
