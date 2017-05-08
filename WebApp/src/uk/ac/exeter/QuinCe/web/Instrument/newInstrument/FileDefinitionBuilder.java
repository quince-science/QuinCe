package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.util.Arrays;
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
	 * The number of lines to search in order to determine
	 * the file's column separator
	 */
	private static final int SEPARATOR_SEARCH_LINES = 10;
	
	/**
	 * The default description for new files
	 */
	private static final String DEFAULT_DESCRIPTION = "Data File";
	
	/**
	 * The contents of the uploaded data file
	 */
	private String fileData = null;
	
	/**
	 * Create a new file with the default description
	 */
	public FileDefinitionBuilder() {
		super(DEFAULT_DESCRIPTION);
	}
	
	/**
	 * Determines whether or not this instrument file has been fully defined.
	 * This includes the existence of file data, specification of headers etc.
	 * @return {@code true} if this file has been fully defined; {@code false} otherwise.
	 */
	public boolean fileDefined() {
		return false;
	}
	
	/**
	 * Determines whether or not file data has been uploaded for this instrument file
	 * @return {@code true} if file data has been uploaded; {@code false} if it has not.
	 */
	public boolean getHasFileData() {
		return (null != fileData);
	}
	
	/**
	 * Store the uploaded data for this file
	 * @param fileData The file data
	 */
	public void setFileData(String fileData) {
		this.fileData = fileData;
	}
	
	/**
	 * Get the contents of the uploaded sample data file
	 * @return The file contents 
	 */
	public String getFileData() {
		return fileData;
	}
	
	/**
	 * Guess the layout of the file from its contents
	 */
	public void guessFileLayout() {
		
		try {
			// Split the file data into lines
			String[] lines = fileData.split("\n");
			
			// Look at the last few lines. Find the most common separator character,
			// and the maximum number of columns in each line
			String separatorSearchString = String.join("\n", Arrays.copyOfRange(lines, lines.length - SEPARATOR_SEARCH_LINES, lines.length));
			setSeparator(getMostCommonSeparator(separatorSearchString));
			
			// Work out the likely number of columns in the file from the last few lines
			int maxColumnCount = 0;
			for (int i = lines.length - SEPARATOR_SEARCH_LINES; i < lines.length; i++) {
				int separatorCount = countSeparatorInstances(getSeparator(), lines[i]);
				if (separatorCount > maxColumnCount) {
					maxColumnCount = separatorCount;
				}
			}
			
			
			// Now start at the beginning of the file, looking for rows containing the
			// maximum column count. Start rows that don't contain this are considered
			// to be the header.
			int currentRow = 0;
			boolean columnsRowFound = false;
			while (!columnsRowFound && currentRow < lines.length) {
				if (countSeparatorInstances(getSeparator(), lines[currentRow]) == maxColumnCount) {
					columnsRowFound = true;
				} else {
					currentRow++;
				}
			}
			
			setHeaderLines(currentRow);
			if (currentRow > 0) {
				setHeaderString(lines[currentRow - 1]);
			}
			
			// Finally, find the line row that's mostly numeric. This is the first proper data line.
			// Any lines between the header and this are column header rows
			boolean dataFound = false;
			while (!dataFound && currentRow < lines.length) {
				String numbers = lines[currentRow].replaceAll("[^0-9]", "");
				if (numbers.length() > (lines[currentRow].length() / 2)) {
					dataFound = true;
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
	 * @param searchString The string to search
	 * @return The most common separator in the string
	 */
	private String getMostCommonSeparator(String searchString) {
		String mostCommonSeparator = null;
		int mostCommonSeparatorCount = 0;
		
		for (String separator : VALID_SEPARATORS) {
			
			int matchCount = countSeparatorInstances(separator, searchString);
			
			if (matchCount > mostCommonSeparatorCount) {
				mostCommonSeparator = separator;
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
}
