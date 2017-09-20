package uk.ac.exeter.QuinCe.data.Files;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeColumnAssignment;
import uk.ac.exeter.QuinCe.data.Instrument.DataFormats.DateTimeSpecification;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeCategory;
import uk.ac.exeter.QuinCe.utils.HighlightedString;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Class representing a data file
 * @author Steve Jones
 *
 */
public class DataFile {

	/**
	 * The file format definition
	 */
	private FileDefinition fileDefinition;
	
	/**
	 * The file name
	 */
	private String filename;
	
	/**
	 * The file contents
	 */
	private List<String> contents;
	
	/**
	 * Messages generated regarding the file
	 */
	private TreeSet<DataFileMessage> messages;
	
	/**
	 * The date in the file header
	 */
	private LocalDateTime headerDate = null;

	/**
	 * Create a DataFile with the specified definition and contents
	 * @param fileDefinition The file format definition
	 * @param filename The file name
	 * @param contents The file contents
	 * @throws MissingParamException If any fields are null
	 */
	public DataFile(FileDefinition fileDefinition, String filename, List<String> contents) throws MissingParamException {
		MissingParam.checkMissing(fileDefinition, "fileDefinition");
		MissingParam.checkMissing(filename, "fileName");
		MissingParam.checkMissing(contents, "contents");
		
		this.fileDefinition = fileDefinition;
		this.filename = filename;
		this.contents = contents;
		
		messages = new TreeSet<DataFileMessage>();
		
		boolean fileOK = extractHeaderDate();

		if (fileOK) {
			validate();
		}
	}
	
	/**
	 * Get the file format description
	 * @return The file format description
	 */
	public String getFileDescription() {
		return fileDefinition.getFileDescription();
	}
	
	/**
	 * Get the file name
	 * @return The file name
	 */
	public String getFilename() {
		return filename;
	}
	
	/**
	 * Set a new file definition for the file. The new definition must match
	 * the layout of the previous definition, since the file's format must
	 * conform to both the new and old definitions.
	 * 
	 * @param newDefinition The new file definition
	 * @throws FileDefinitionException The the definition does not match the file layout
	 */
	public void setFileDefinition(FileDefinition newDefinition) throws FileDefinitionException {
		if (!fileDefinition.matchesLayout(newDefinition)) {
			throw new FileDefinitionException("File Definition does not match file contents");
		} else {
			this.fileDefinition = newDefinition;
		}
	}
	
	/**
	 * Get the zero-based row number of the first data row
	 * in a file
	 * @return The first data row number
	 * @throws DataFileException If the end of the file is reached without finding the end of the header
	 */
	public int getFirstDataLine() throws DataFileException {
		// The lines are zero-based, so just returning the header length will be correct
		return fileDefinition.getHeaderLength(contents);
	}
	
	/**
	 * Get the number of rows in the file
	 * @return The row count
	 */
	public int getLineCount() {
		return contents.size();
	}

	/**
	 * Get the data from a specified row in the file as a list of string fields.
	 * This is the row position in the whole file, including headers.
	 * @param row The row to be retrieved
	 * @return The row fields
	 * @throws DataFileException If the requested row is outside the bounds of the file
	 */
	public List<String> getRowFields(int row) throws DataFileException {
		List<String> result;
		
		if (row < getFirstDataLine()) {
			throw new DataFileException("Requested row " + row + " is in the file header");
		} else if (row > (contents.size() - 1)) {
			throw new DataFileException("Requested row " + row + " is in the file header");
		} else {
			result = StringUtils.trimList(Arrays.asList(contents.get(row).split(fileDefinition.getSeparator())));
		}
		
		return result;
	}
	
	/**
	 * Validate the file contents.
	 * Creates a set of {@code DataFileMessage}
	 * objects, which can be retrieved using {@code getMessages()}.
	 */
	public void validate() {
		
		// Check that there is actually data in the file
		int firstDataLine = -1;
		try {
			firstDataLine = getFirstDataLine();
		} catch (DataFileException e) {
			addMessage("File does not contain any data");
		}
		
		if (firstDataLine > -1) {
			
			// For each line in the file, check that:
			// (a) The date/time are present and monotonic in the file
			// (b) Has the correct number of columns (for Run Types that aren't IGNORED)
			// (c) The Run Type is recognised
			
			LocalDateTime lastDateTime = null;
			for (int lineNumber = firstDataLine; lineNumber < contents.size(); lineNumber++) {
				String line = contents.get(lineNumber);
				
				try {
					LocalDateTime dateTime = fileDefinition.getDateTimeSpecification().getDateTime(null, fileDefinition.extractFields(line));
					if (null != lastDateTime) {
						if (dateTime.compareTo(lastDateTime) <= 0) {
							addMessage(lineNumber, "Date/Time is not monotonic");
						}
					}
				} catch (DataFileException e) {
					addMessage(lineNumber, e.getMessage());
				}
				
				boolean checkColumnCount = true;
				
				if (fileDefinition.hasRunTypes()) {
					try {
						RunTypeCategory runType = fileDefinition.getRunType(line);
						if (runType.equals(RunTypeCategory.IGNORED_CATEGORY)) {
							checkColumnCount = false;
						}
					} catch (FileDefinitionException e) {
						addMessage(lineNumber, e.getMessage());
					}
				}
				
				if (checkColumnCount && fileDefinition.extractFields(line).size() != fileDefinition.getColumnCount()) {
					addMessage(lineNumber, "Incorrect number of columns");
				}
			}
		}
	}
	
	/**
	 * Shortcut method for adding a message to the message list
	 * @param lineNumber The line number
	 * @param message The message text
	 */
	private void addMessage(int lineNumber, String message) {
		messages.add(new DataFileMessage(lineNumber, message));
	}
	
	/**
	 * Shortcut method for adding a message to the message list
	 * @param message The message text
	 */
	private void addMessage(String message) {
		messages.add(new DataFileMessage(message));
	}
	
	/**
	 * Get the messages generated for this file
	 * @return The messages
	 */
	public TreeSet<DataFileMessage> getMessages() {
		return messages;
	}
	
	/**
	 * Get the number of messages that have been generated
	 * for this file
	 * @return The message count
	 */
	public int getMessageCount() {
		return messages.size();
	}
	
	/**
	 * Get the start date from the file header. This is only applicable
	 * if the date format is {@link DateTimeSpecification#HOURS_FROM_START}.
	 * @return The start date from the file header
	 */
	private boolean extractHeaderDate() {
		boolean result = true;
		
		DateTimeSpecification dateTimeSpec = fileDefinition.getDateTimeSpecification();
		if (dateTimeSpec.isAssigned(DateTimeSpecification.HOURS_FROM_START)) {
			try {
				DateTimeColumnAssignment assignment = dateTimeSpec.getAssignment(DateTimeSpecification.HOURS_FROM_START);
				HighlightedString matchedLine = fileDefinition.getHeaderLine(contents, assignment.getPrefix(), assignment.getSuffix());
				headerDate = LocalDateTime.parse(matchedLine.getHighlightedPortion(), assignment.getFormatter());
			} catch (Exception e) {
				e.printStackTrace();
				addMessage("Could not extract file start date from header: " + e.getMessage());
				result = false;
			}
		}
		
		return result;
	}
	
	/**
	 * Get the date of the first record in the file
	 * @return The date
	 * @throws DataFileException If the first record cannot be accessed, or any 
	 */
	public LocalDateTime getStartDate() throws DataFileException {
		return getDate(getFirstDataLine());
	}
	
	/**
	 * Get the date of the last record in the file
	 * @return The date
	 * @throws DataFileException If any date/time fields are empty
	 */
	public LocalDateTime getEndDate() throws DataFileException {
		return getDate(contents.size() - 1);
	}
	
	/**
	 * Get the date of a line in the file
	 * @param line The line
	 * @return The date
	 * @throws DataFileException If any date/time fields are empty
	 */
	public LocalDateTime getDate(int line) throws DataFileException {
		return fileDefinition.getDateTimeSpecification().getDateTime(headerDate, fileDefinition.extractFields(contents.get(line)));
	}
	
	/**
	 * Get a {@link Double} value from a field.
	 * <p>
	 *   Returns {@code null} if the field string is empty, or the field
	 *   equals the supplied {@code missingValue} (if it is supplied).
	 * </p>
	 * 
	 * @param field The field
	 * @param missingValue The 'missing' value for the field
	 * @return The numeric field value
	 * @throws DataFileException If the field value is not numeric
	 */
	public static Double extractDoubleFieldValue(String field, String missingValue) throws DataFileException {
		Double result = null;
		
		if (null != field && field.trim().length() > 0) {
			if (null != missingValue && !field.equals(missingValue)) {
				try {
					result = Double.parseDouble(field);
				} catch (NumberFormatException e) {
					throw new DataFileException("Value is not numeric");
				}
			}
		}
		
		return result;
	}

	/**
	 * Get a {@link Integer} value from a field.
	 * <p>
	 *   Returns {@code null} if the field string is empty, or the field
	 *   equals the supplied {@code missingValue} (if it is supplied).
	 * </p>
	 * 
	 * @param field The field
	 * @param missingValue The 'missing' value for the field
	 * @return The numeric field value
	 * @throws DataFileException If the field value is not numeric
	 */
	public static Integer extractIntFieldValue(String field, String missingValue) throws DataFileException {
		Integer result = null;
		
		if (null != field && field.trim().length() > 0) {
			if (null != missingValue && !field.equals(missingValue)) {
				try {
					result = Integer.parseInt(field);
				} catch (NumberFormatException e) {
					throw new DataFileException("Value is not numeric");
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Get a {@link String} value from a field.
	 * <p>
	 *   If the field is empty, or equals the supplied {@code missingValue},
	 *   {@code null} is returned.
	 * </p>
	 * 
	 * @param field The field
	 * @param missingValue The 'missing' value for the field
	 * @return The field value
	 */
	public static String extractStringFieldValue(String field, String missingValue) {
		String result = field;
		
		if (null != field) {
			result = field.trim();

			if (null != missingValue && !field.equals(missingValue)) {
				result = null;
			}
		}
		
		return result;
	}
}
