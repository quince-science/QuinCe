package uk.ac.exeter.QuinCe.jobs.files;

import java.util.TreeSet;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;

/**
 * A QC Routines message to indicate that a measurement
 * has been excluded from analysis because it is in the
 * instrument's flushing period.
 * 
 * @author Steve Jones
 * @see Message
 */
@Deprecated
public class FlushingQCMessage extends Message {

	/**
	 * The message text - simply the word 'Flushing'
	 */
	public static final String MESSAGE_TEXT = "Flushing";
	
	/**
	 * Required constructor for reconstruction of the Message object
	 * @param lineNumber The line to which this message applies
	 * @param columnIndices The index(es) of the column(s) to which this message applies
	 * @param columnNames The name(s) of the column(s) to which this message applies
	 * @param flag The flag for the message
	 * @param fieldValue The value from the line that caused the message to be generated
	 * @param validValue An example of a valid value indicating what the line should contain
	 * @see Message
	 */
	public FlushingQCMessage(int lineNumber, TreeSet<Integer> columnIndices, TreeSet<String> columnNames, Flag flag, String fieldValue, String validValue) {
		super(lineNumber, columnIndices, columnNames, flag, fieldValue, validValue);
	}

	/**
	 * Constructor to build a new Flushing message
	 * @param lineNumber The line number of the record
	 * @throws MessageException If any errors occur while constructing the message
	 */
	public FlushingQCMessage(int lineNumber) throws MessageException {
		super(lineNumber, null, null, Flag.IGNORED, null, null);
		
		columnIndices = new TreeSet<Integer>();
		columnIndices.add(NO_COLUMN_INDEX);
		
		columnNames = new TreeSet<String>();
		columnNames.add("");
	}

	@Override
	public String getFullMessage() {
		return MESSAGE_TEXT;
	}

	@Override
	public String getShortMessage() {
		return MESSAGE_TEXT;
	}
}
