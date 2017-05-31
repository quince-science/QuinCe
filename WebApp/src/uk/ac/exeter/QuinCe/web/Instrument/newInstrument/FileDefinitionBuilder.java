package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;

/**
 * This is a specialised instance of the InstrumentFile class
 * that provides special functions used only when an instrument
 * is being defined.
 * 
 * @author Steve Jones
 *
 */
public class FileDefinitionBuilder extends FileDefinition {

	/**
	 * The maximum number of lines from an uploaded file to be stored
	 */
	protected static final int FILE_DATA_MAX_LINES = 250;
	
	/**
	 * The number of lines to search in order to determine
	 * the file's column separator
	 */
	private static final int SEPARATOR_SEARCH_LINES = 10;
	
	/**
	 * The default description for new files
	 */
	private static final String DEFAULT_DESCRIPTION = "Data File";
	
	/**
	 * The first {@link #FILE_DATA_MAX_LINES} lines of the uploaded sample file, formatted as a JSON string
	 */
	private String fileData = null;
	
	/**
	 * The first {@link #FILE_DATA_MAX_LINES} lines of the uploaded sample file, as an array of Strings
	 */
	private List<String> fileDataArray = null; 
	
	/**
	 * Create a new file definition with the default description
	 */
	protected FileDefinitionBuilder(InstrumentFileSet fileSet) {
		super(DEFAULT_DESCRIPTION);

		int counter = 1;
		while (fileSet.containsFileDescription(getFileDescription())) {
			counter++;
			setFileDescription(DEFAULT_DESCRIPTION + " " + counter);
		}
	}
	
	/**
	 * Create a new file definition with a specified description
	 * @param fileDescription The file description
	 */
	public FileDefinitionBuilder(String fileDescription) {
		super(fileDescription);
	}
	
	/**
	 * Determines whether or not file data has been uploaded for this instrument file
	 * @return {@code true} if file data has been uploaded; {@code false} if it has not.
	 */
	public boolean getHasFileData() {
		return (null != fileData);
	}
	
	/**
	 * Store the uploaded data for this file in JSON format
	 * @param fileData The file data
	 */
	public void setFileData(String fileData) {
		this.fileData = fileData;
	}
	
	/**
	 * Get the contents of the uploaded sample data file as a JSON string
	 * @return The file contents 
	 */
	public String getFileData() {
		return fileData;
	}
	
	/**
	 * Guess the layout of the file from its contents
	 * @see #calculateColumnCount()
	 */
	public void guessFileLayout() {
		try {
			// Look at the last few lines. Find the most common separator character,
			// and the maximum number of columns in each line
			setSeparator(getMostCommonSeparator());
			
			// Work out the likely number of columns in the file from the last few lines
			super.setColumnCount(calculateColumnCount());
			
			
			// Now start at the beginning of the file, looking for rows containing the
			// maximum column count. Start rows that don't contain this are considered
			// to be the header.
			int currentRow = 0;
			boolean columnsRowFound = false;
			while (!columnsRowFound && currentRow < fileDataArray.size()) {
				if (countSeparatorInstances(getSeparator(), fileDataArray.get(currentRow)) == getColumnCount()) {
					columnsRowFound = true;
				} else {
					currentRow++;
				}
			}
			
			setHeaderLines(currentRow);
			if (currentRow > 0) {
				setHeaderString(fileDataArray.get(currentRow - 1));
			}
			
			// Finally, find the line row that's mostly numeric. This is the first proper data line.
			// Any lines between the header and this are column header rows
			boolean dataFound = false;
			while (!dataFound && currentRow < fileDataArray.size()) {
				String numbers = fileDataArray.get(currentRow).replaceAll("[^0-9]", "");
				if (numbers.length() > (fileDataArray.get(currentRow).length() / 2)) {
					dataFound = true;
				} else {
					currentRow++;
				}
			}
			
			setColumnHeaderRows(currentRow - getHeaderLines());
			
		} catch (Exception e) {
			// Any exceptions mean that the guessing failed. We don't need to take
			// any action because it will be left up to the user to specify the format
			// manually.
		}
	}
	
	/**
	 * Search a string to find the most commonly occurring valid separator value
	 * @return The most common separator in the string
	 */
	private String getMostCommonSeparator() {
		String mostCommonSeparator = null;
		int mostCommonSeparatorCount = 0;
		
		for (String separator : VALID_SEPARATORS) {
			int matchCount = calculateColumnCount(separator);
			
			if (matchCount > mostCommonSeparatorCount) {
				mostCommonSeparator = separator;
				mostCommonSeparatorCount = matchCount;
			}
		}
		
		return mostCommonSeparator;
	}
	
	/**
	 * Count the number of instances of a given separator in a string
	 * @param separator The separator to search for
	 * @param searchString The string to be searched
	 * @return The number of separators found in the string
	 */
	private int countSeparatorInstances(String separator, String searchString) {
		
		Pattern searchPattern;
		
		if (separator.equals(" ")) {
			// Space separators come in groups, so count consecutive spaces as one instance
			searchPattern = Pattern.compile(" +");
		} else {
			searchPattern = Pattern.compile(separator);
		}
		
		Matcher matcher = searchPattern.matcher(searchString);
		
		int matchCount = 0;
		while (matcher.find()) {
			matchCount++;
		}
		
		return matchCount;
	}
	
	/**
	 * Store the file data as an array of Strings
	 * @param fileDataArray The file data
	 */
	protected void setFileDataArray(List<String> fileDataArray) {
		this.fileDataArray = fileDataArray;
	}
	
	/**
	 * Create a deep copy of a {@code FileDefinitionBuilder} object.
	 * @param source The source object
	 * @return The copied object
	 */
	public static FileDefinitionBuilder copy(FileDefinitionBuilder source) {
		
		FileDefinitionBuilder dest = new FileDefinitionBuilder(source.getFileDescription());

		try {
			dest.setHeaderType(source.getHeaderType());
			dest.setHeaderLines(source.getHeaderLines());
			dest.setHeaderString(source.getHeaderString());
			dest.setColumnHeaderRows(source.getColumnHeaderRows());
			dest.setSeparator(source.getSeparator());
			dest.fileData = source.fileData;
			
			dest.fileDataArray = new ArrayList<String>(source.fileDataArray.size());
			for (String sourceLine : source.fileDataArray) {
				dest.fileDataArray.add(sourceLine);
			}
			
		} catch (Exception e) {
			// Since we're copying an existing object that must already be valid,
			// we can safely swallow exceptions
		}

		return dest;
	}

	/**
	 * Get the number of columns in the file.
	 * 
	 * @return The number of columns in the file
	 * @see #calculateColumnCount(String)
	 */
	public int calculateColumnCount() {
		
		int result;
		
		if (null == getSeparator() || null == fileData)
			result = 0;
		else {
			result = calculateColumnCount(getSeparator());
		}
		
		return result;
	}
	
	/**
	 * Get the number of columns in a file using the specified separator.
	 * 
	 * This states the hypothetical number of columns found in the file
	 * if the specified separator is used.
	 *
	 * This counts is based on the number of separators found per line
	 * in the last few lines of the file. The largest number of columns
	 * is used, because some instruments report diagnostic information on shorter lines.
	 * 
	 * @param separator The separator
	 * @return The number of columns
	 * @see #SEPARATOR_SEARCH_LINES
	 * @see #getMostCommonSeparator()
	 */
	 public int calculateColumnCount(String separator) {
		int maxColumnCount = 0;
		
		int firstSearchLine = fileDataArray.size() - SEPARATOR_SEARCH_LINES;
		if (firstSearchLine < 0) {
			firstSearchLine = 0;
		}
		for (int i = firstSearchLine; i < fileDataArray.size(); i++) {
			int separatorCount = countSeparatorInstances(separator, fileDataArray.get(i));
			if (separatorCount > maxColumnCount) {
				maxColumnCount = separatorCount;
			}
		}
		
		return maxColumnCount;
	}

	/**
	 * Dummy set column method for bean requirements.
	 * We don't allow external agencies to set this,
	 * but it's needed for bean compatibility
	 * @param columnCount The column count
	 */
	@Override
	public void setColumnCount(int columnCount) {
		// Do nothing
	}
	
	/**
	 * Get the set of column definitions for this file definition as a JSON array
	 * <p>
	 *   If the file has column header rows, the column names will
	 *   be taken from the first of those rows. Otherwise, they will be
	 *   Column 1, Column 2 etc.
	 * </p>
	 *   
	 * @return The file columns
	 */
	public String getFileColumns() {

		// TODO Only regenerate the columns when the file spec is changed. Don't do it
		//      on demand like this.
		List<String> columns = new ArrayList<String>();
		 
		if (getColumnHeaderRows() == 0) {
			for (int i = 0; i < getColumnCount(); i++) {
				columns.add("Column " + (i + 1));
			}
		} else {
			String columnHeaders = fileDataArray.get(getFirstColumnHeaderRow());
			columns = makeColumnValues(columnHeaders);
		}
		
		StringBuilder result = new StringBuilder();
		
		result.append('[');
		
		for (int i = 0; i < columns.size(); i++) {
			result.append('"');
			result.append(columns.get(i).replaceAll("'", "\\'"));
			result.append('"');
			
			if (i < columns.size() - 1) {
				result.append(',');
			}
		}
		
		result.append(']');
		
		return result.toString();
	}
	
	/**
	 * Return the row number of the first column header row in the file
	 * @return The first column header row, or -1 if the file does not have column headers
	 */
	public int getFirstColumnHeaderRow() {
		int result = -1;
		
		if (getColumnHeaderRows() > 0) {
			// The column headers are the first row
			// after the file header. If we get the file header length,
			// then the row index is the same because it's zero based.
			// It's useful sometimes...
			result = getHeaderLength();
		}
		
		return result;
	}
	
	/**
	 * Get the number of rows in the file header
	 * @return The number of rows in the file header
	 */
	public int getHeaderLength() {
		int result = 0;
		
		switch (getHeaderType()) {
		case HEADER_TYPE_LINE_COUNT: {
			result = getHeaderLines();
			break;
		}
		case HEADER_TYPE_STRING: {
			
			int row = 0;
			boolean foundHeaderEnd = false;
			while (!foundHeaderEnd && row < fileDataArray.size()) {
				if (fileDataArray.get(row).equalsIgnoreCase(getHeaderString())) {
					foundHeaderEnd = true;
				} else {
					row++;
				}
			}
			
			result = row;
			break;
		}
		}
		
		return result;
	}
	
	/**
	 * Get the data from the sample file as a JSON string
	 * @return The file data
	 */
	public String getJsonData() {
		
		StringBuilder result = new StringBuilder();
		
		result.append('[');
		
		int firstDataRow = getHeaderLength() + getColumnHeaderRows();
		for (int i = firstDataRow; i < fileDataArray.size(); i++) {
			List<String> columnValues = makeColumnValues(fileDataArray.get(i));
			
			result.append('[');

			for (int j = 0; j < getColumnCount(); j++) {
				
				result.append('"');
				
				// We can't guarantee that every column has data, so
				// fill in empty strings for unused columns
				if (j < columnValues.size()) {
					result.append(columnValues.get(j).replaceAll("'", "\\'"));
				}
				
				result.append('"');
				if (j < getColumnCount() - 1) {
					result.append(',');
				}
			}
			
			result.append(']');
			if (i < fileDataArray.size() - 1) {
				result.append(',');
			}
		}

		
		result.append(']');
		
		return result.toString();
	}
}
