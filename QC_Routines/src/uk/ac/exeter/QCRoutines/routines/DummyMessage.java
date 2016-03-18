package uk.ac.exeter.QCRoutines.routines;

import uk.ac.exeter.QCRoutines.messages.Flag;
import uk.ac.exeter.QCRoutines.messages.Message;

public class DummyMessage extends Message {

	public DummyMessage(int columnIndex, String columnName, Flag flag, int lineNumber, String fieldValue, String validValue) {
		super(columnIndex, columnName, flag, lineNumber, fieldValue, validValue);
	}

	public DummyMessage(int columnIndex, String columnName, Flag flag, int lineNumber, String fieldValue) {
		super(columnIndex, columnName, flag, lineNumber, fieldValue, null);
	}

	@Override
	protected String getFullMessage() {
		return "This is a dummy message for column '" + COLUMN_NAME_IDENTIFIER + "' on line " + lineNumber;
	}

	@Override
	public String getShortMessage() {
		return "Dummy message";
	}
}
