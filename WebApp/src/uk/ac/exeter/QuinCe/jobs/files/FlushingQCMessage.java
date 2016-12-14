package uk.ac.exeter.QuinCe.jobs.files;

import java.util.TreeSet;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;
import uk.ac.exeter.QCRoutines.messages.MessageException;

public class FlushingQCMessage extends Message {

	public static final String MESSAGE_TEXT = "Flushing";
	
	public FlushingQCMessage(int lineNumber, TreeSet<Integer> columnIndices, TreeSet<String> columnNames, Flag flag, String fieldValue, String validValue) {
		super(lineNumber, columnIndices, columnNames, flag, fieldValue, validValue);
	}

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
