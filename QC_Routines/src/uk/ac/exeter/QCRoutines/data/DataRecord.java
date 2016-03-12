package uk.ac.exeter.QCRoutines.data;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;

public abstract class DataRecord {

	/**
	 * The output messages generated for this line, if any
	 */
	private List<Message> messages;
	
	/**
	 * Flag to indicate the presence of warnings raised during processing
	 */
	private boolean hasWarnings = false;
	
	/**
	 * Flag to indicate the presence of errors raised during processing
	 */
	private boolean hasErrors = false;
	
	/**
	 * The line of the input file that this record came from
	 */
	private int lineNumber;
	
	/**
	 * The quality control flag for the record.
	 * All records are assumed to be good unless otherwise stated
	 */
	private int flag = FLAG_GOOD;

	/**
	 * Builds a complete record object
	 * @param dataFields The set of data values for the record, in the order specified by the column specification
	 * @param lineNumber The line number of the record
	 */
	public DataRecord(List<String> dataFields, int lineNumber) throws DataRecordException {
		this.messages = new ArrayList<Message>();
		this.lineNumber = lineNumber;

		// Populate all the basic data columns
		setDataValues(dataFields);
	}
	
	/**
	 * Returns the date/time of this record as a single object.
	 * @return The date/time of this record.
	 */
	public abstract DateTime getTime();

	/**
	 * Returns the longitude of this record
	 * @return The longitude of this record
	 */
	public abstract double getLongitude();
	
	/**
	 * Returns the latitude of this record
	 * @return The latitude of this record
	 */
	public abstract double getLatitude();
	
	/**
	 * Populate all fields whose values are taken directly from the input data, converting where necessary
	 * @param lineNumber The current line number of the input file
	 * @param colSpec The column specification
	 * @param dataFields The input data fields
	 * @throws SocatDataException If an error occurs during processing
	 */
	protected abstract void setDataValues(List<String> dataFields) throws DataRecordException;
	
	/**
	 * Returns the value of a named column
	 * @param columnName The name of the column
	 * @return The value of that column
	 * @throws DataRecordException If the named column does not exist
	 */
	public abstract String getValue(String columnName) throws DataRecordException;
	
	/**
	 * Returns the value held in the specified column
	 * @param columnIndex The 1-based column index
	 * @return The value of that column
	 * @throws DataRecordException If the column does not exist
	 */
	public String getValue(int columnIndex) throws DataRecordException {
		return getValue(getColumnName(columnIndex));
	}
	
	/**
	 * Returns the name of the column corresponding to the specified column index
	 * @param columnIndex The 1-based column index
	 * @return The column name
	 * @throws DataRecordException If the column does not exist
	 */
	public abstract String getColumnName(int columnIndex) throws DataRecordException;
	
	/**
	 * Returns the index of the named column
	 * @param columnIndex The column name
	 * @return The 1-based column index
	 * @throws DataRecordException If the column does not exist
	 */
	public abstract int getColumnIndex(String columnName) throws DataRecordException;
	
	/**
	 * Indicates whether or not errors were raised during the processing of this
	 * data record
	 * @return {@code true} if errors were raised; {@code false} otherwise.
	 */
	public boolean hasErrors() {
		return hasErrors;
	}
	
	/**
	 * Indicates whether or not warnings were raised during the processing of this
	 * data record
	 * @return {@code true} if warnings were raised; {@code false} otherwise.
	 */
	public boolean hasWarnings() {
		return hasWarnings;
	}
	
	/**
	 * Returns the list of all messages created during processing of this record
	 * @return The list of messages
	 */
	public List<Message> getMessages() {
		return messages;
	}
	
	/**
	 * Returns the line number in the original data file that this record came from
	 * @return The line number
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Adds a message to the set of messages assigned to this record
	 * @param message The message
	 */
	public void addMessage(Message message) {
		messages.add(message);
		
		switch(message.getSeverity()) {
		case Message.ERROR: {
			hasErrors = true;
			break;
		}
		case Message.WARNING: {
			hasWarnings = true;
			break;
		}
		default: {
			// Noop
		}
		}
	}
	
	public int getFlag() {
		return flag;
	}
	
	public void setFlag(int flag) {
		this.flag = flag;
	}
	
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}
	
	public void setMessages(String codes) throws MessageException {
		this.messages = RebuildCode.getMessagesFromRebuildCodes(codes);
	}
}
