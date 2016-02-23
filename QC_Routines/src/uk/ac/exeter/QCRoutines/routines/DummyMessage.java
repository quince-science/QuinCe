package uk.ac.exeter.QCRoutines.routines;

import uk.ac.exeter.QCRoutines.messages.Message;

public class DummyMessage extends Message {

	public DummyMessage(int columnIndex, String columnName, int severity, int lineNumber, String fieldValue) {
		super(columnIndex, columnName, severity, lineNumber, fieldValue, null);
	}

	@Override
	protected String getFullMessage() {
		return "This is a dummy message for column '" + COLUMN_NAME_IDENTIFIER + "' on line " + lineNumber;
	}

	@Override
	protected String getSummaryMessage() {
		return "Dummy message";
	}
}
