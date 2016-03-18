package uk.ac.exeter.QCRoutines.data;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;
import uk.ac.exeter.QCRoutines.messages.RebuildCode;

public abstract class DataRecord {

	/**
	 * The output messages generated for this line, if any
	 */
	protected List<Message> messages;
	
	/**
	 * Flag to indicate the presence of questionable flags raised during processing
	 */
	private boolean hasQuestionable = false;
	
	/**
	 * Flag to indicate the presence of bad flags raised during processing
	 */
	private boolean hasBad = false;
	
	/**
	 * The line of the input file that this record came from
	 */
	private int lineNumber;
	
	/**
	 * The quality control flag for the record.
	 * All records are assumed to be good unless otherwise stated
	 */
	private Flag flag = Flag.GOOD;

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
	 * Indicates whether or not questionable flags were raised during the processing of this
	 * data record
	 * @return {@code true} if questionable flags were raised; {@code false} otherwise.
	 */
	public boolean hasQuestionable() {
		return hasQuestionable;
	}
	
	/**
	 * Indicates whether or not bad flags were raised during the processing of this
	 * data record
	 * @return {@code true} if bad flags were raised; {@code false} otherwise.
	 */
	public boolean hasBad() {
		return hasBad;
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
	 * Adds a message to the set of messages assigned to this record,
	 * and automatically updates the record's flag to match
	 * @param message The message
	 */
	public void addMessage(Message message) {
		addMessage(message, true);
	}
	
	/**
	 * Adds a message to the set of messages assigned to this record,
	 * and optionally updates the record's flag to match
	 * @param message The message
	 * @param updateFlag {@code true} if the record's flag should be updated; {@code false} if it should not.
	 */
	public void addMessage(Message message, boolean updateFlag) {
		messages.add(message);
		
		Flag messageFlag = message.getFlag();
		if (flag.equals(Flag.BAD)) {
			hasBad = true;
		} else if (flag.equals(Flag.QUESTIONABLE)) {
			hasQuestionable = true;
		}

		if (updateFlag) {
			if (messageFlag.moreSignificantThan(flag)) {
				flag = messageFlag;
			}
		}
	}

	/**
	 * Get the most significant flag applied to this record
	 * @return The flag for the record
	 */
	public Flag getFlag() {
		return flag;
	}
	
	/**
	 * Replace all the messages for this record with the supplied list of messages.
	 * Optionally, the record's flags will also be reset according to the flags on the messages.
	 * @param messages The set of messages
	 * @param setFlag Indicates whether or not the record's flag is to be updated
	 */
	public void setMessages(List<Message> messages, boolean setFlag) {
		clearMessages();
		for (Message message : messages) {
			addMessage(message, setFlag);
		}
	}
	
	/**
	 * Replace all the messages for this record with the supplied list of messages.
	 * The record's flags will also be reset according to the flags on the messages.
	 * @param messages The set of messages
	 */
	public void setMessages(List<Message> messages) {
		setMessages(messages, true);
	}
	
	/**
	 * Replace all the messages for this record with the supplied message codes.
	 * The record's flags will also be reset according to the flags on the messages.
	 * @param messages The set of message codes
	 */
	public void setMessages(String codes) throws MessageException {
		setMessages(RebuildCode.getMessagesFromRebuildCodes(codes));
	}
	
	/**
	 * Clear all messages from the record, and reset the flags to the default
	 * 'good' state.
	 */
	public void clearMessages() {
		messages = new ArrayList<Message>(messages.size());
		hasBad = false;
		hasQuestionable = false;
		flag = Flag.GOOD;
	}

	/**
	 * Sets the flag for this record
	 * @param flag The flag
	 */
	public void setFlag(Flag flag) {
		this.flag = flag;
		if (flag.equals(Flag.BAD)) {
			hasBad = true;
		} else if (flag.equals(Flag.QUESTIONABLE)) {
			hasQuestionable = true;
		}
	}
	
	/**
	 * Return a string containing the summary for all messages in this record
	 * @return The messages summary string
	 */
	public String getMessageSummaries() {
		StringBuffer summaries = new StringBuffer();
		for (int i = 0; i < messages.size(); i++) {
			summaries.append(messages.get(i).getShortMessage());
			if (i < messages.size() - 1) {
				summaries.append("; ");
			}
		}
		
		return summaries.toString();
	}
}
