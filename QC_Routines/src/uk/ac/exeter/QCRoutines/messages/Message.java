package uk.ac.exeter.QCRoutines.messages;

public class Message {
	
	public static final int ERROR = 1;
	
	public static final String ERROR_STRING = "ERROR";
	
	public static final int WARNING = 2;
	
	public static final String WARNING_STRING = "WARNING";
	
	public static final String UNKNOWN_SEVERITY_STRING = "*** Severity Unknown ***";
	
	public static final int DATE_TIME_COLUMN_INDEX = -1;
	
	public static final String DATE_TIME_COLUMN_NAME = "Date/Time";
	
	public static final int SHIP_SPEED_COLUMN_INDEX = -2;
	
	public static final String SHIP_SPEED_COLUMN_NAME = "Lon/Lat/Date/Time";
	
	public static final int NO_COLUMN_INDEX = -999;
	
	public static final int NO_LINE_NUMBER = -999;

	private int itsColumnIndex;
	
	private String itsColumnName;
	
	private MessageType itsType;
	
	private int itsSeverity;
	
	private int itsLineNumber;
	
	private String itsFieldValue;
	
	private String itsValidValue;
	
	public Message(int columnIndex, String columnName, MessageType type, int severity, int lineNumber, String fieldValue, String validValue) {
		itsColumnIndex = columnIndex;
		itsColumnName = columnName;
		itsType = type;
		itsSeverity = severity;
		itsLineNumber = lineNumber;
		itsFieldValue = fieldValue;
		itsValidValue = validValue;
	}
	
	/**
	 * Returns the line number for which this message was raised.
	 * @return The line number for which this message was raised.
	 */
	public int getLineNumber() {
		return itsLineNumber;
	}
	
	/**
	 * Returns the column index for which this message was raised.
	 * @return The column index for which this message was raised.
	 */
	public int getColumnIndex() {
		return itsColumnIndex;
	}

	/**
	 * @return the name of the column for which this message was raised.
	 */
	public String getColumnName() {
		return itsColumnName;
	}

	/**
	 * Returns the severity of the message
	 * @return The severity of the message
	 */
	public int getSeverity() {
		return itsSeverity;
	}
	
	public MessageType getMessageType() {
		return itsType;
	}
	
	/**
	 * Determines whether or not this message represents an error
	 * @return {@code true} if the message is an error; {@code false} otherwise.
	 */
	public boolean isError() {
		return itsSeverity == ERROR;
	}
	
	/**
	 * Determines whether or not this message represents an warning
	 * @return {@code true} if the message is a warning; {@code false} otherwise.
	 */
	public boolean isWarning() {
		return itsSeverity == WARNING;
	}
	
	/**
	 * Create the {@link MessageKey} object for this message,
	 * to be used in storing the message.
	 * 
	 * @return The {@link MessageKey} object for this message 
	 */
	public MessageKey generateMessageKey() {
		return new MessageKey(itsColumnIndex, itsType);
	}
	
	public String getMessageString() {
		
		StringBuffer result = new StringBuffer();
		result.append(getMessageSeverityString());
		result.append(": LINE ");
		result.append(itsLineNumber);
		result.append(": ");
		result.append(itsType.getRecordMessage(itsColumnName, itsFieldValue, itsValidValue));
		return result.toString();
	}
	
	private String getMessageSeverityString() {
		String result;
		
		switch (itsSeverity) {
		case ERROR:
		{
			result = ERROR_STRING;
			break;
		}
		case WARNING:
		{
			result = WARNING_STRING;
			break;
		}
		default:
			result = UNKNOWN_SEVERITY_STRING; 
		}
		
		return result;
	}
}
