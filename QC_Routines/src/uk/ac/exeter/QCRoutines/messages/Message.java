package uk.ac.exeter.QCRoutines.messages;

public abstract class Message {
	
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
	
	/**
	 * Token identifying the placeholder for the column name
	 */
	public static final String COLUMN_NAME_IDENTIFIER = "%c%";
	
	/**
	 * Token identifying the placeholder for a field value
	 */
	public static final String FIELD_VALUE_IDENTIFIER = "%f%";
	
	/**
	 * Token identifying the placeholder for a valid value
	 */
	public static final String VALID_VALUE_IDENTIFIER = "%v%";
	
	/**
	 * String to be used if tokens are in the message string, but
	 * no values are supplied to fill them.
	 */
	private static final String MISSING_VALUE_STRING = "MISSING_VALUE";

	protected int columnIndex;
	
	private String columnName;
	
	private int severity;
	
	protected int lineNumber;
	
	protected String fieldValue;
	
	protected String validValue;
	
	public Message(int columnIndex, String columnName, int severity, int lineNumber, String fieldValue, String validValue) {
		this.columnIndex = columnIndex;
		this.columnName = columnName;
		this.severity = severity;
		this.lineNumber = lineNumber;
		this.fieldValue = fieldValue;
		this.validValue = validValue;
	}
	
	/**
	 * Returns the line number for which this message was raised.
	 * @return The line number for which this message was raised.
	 */
	public int getLineNumber() {
		return lineNumber;
	}
	
	/**
	 * Returns the column index for which this message was raised.
	 * @return The column index for which this message was raised.
	 */
	public int getColumnIndex() {
		return columnIndex;
	}

	/**
	 * @return the name of the column for which this message was raised.
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Returns the severity of the message
	 * @return The severity of the message
	 */
	public int getSeverity() {
		return severity;
	}
	
	/**
	 * Determines whether or not this message represents an error
	 * @return {@code true} if the message is an error; {@code false} otherwise.
	 */
	public boolean isError() {
		return severity == ERROR;
	}
	
	/**
	 * Determines whether or not this message represents an warning
	 * @return {@code true} if the message is a warning; {@code false} otherwise.
	 */
	public boolean isWarning() {
		return severity == WARNING;
	}
	
	/**
	 * Create the {@link MessageKey} object for this message,
	 * to be used in storing the message.
	 * 
	 * @return The {@link MessageKey} object for this message 
	 */
	public MessageKey generateMessageKey() {
		return new MessageKey(columnIndex, getClass());
	}
	
	public String getMessageString() {
		
		StringBuffer result = new StringBuffer();
		result.append(getMessageSeverityString());
		result.append(": LINE ");
		result.append(lineNumber);
		result.append(": ");
		result.append(getRecordMessage(columnName, fieldValue, validValue));
		return result.toString();
	}
	
	private String getMessageSeverityString() {
		String result;
		
		switch (severity) {
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
	
	protected abstract String getFullMessage();
	
	protected abstract String getSummaryMessage();
	
	/**
	 * Generate the long form error message for this error type, substituting in
	 * the supplied field and valid values.
	 * 
	 * If the message requires these values but they are not provided, the message will still
	 * be generated but the values will be replaced with {@link #MISSING_VALUE_STRING}.
	 * 
	 * @param fieldValue The field value from the data file
	 * @param validValue The expected valid value(s)
	 * @return The message string
	 */
	public String getRecordMessage(String columnName, String fieldValue, String validValue) {
		
		String columnReplaceValue = MISSING_VALUE_STRING;
		if (null != columnName && columnName.trim().length() > 0) {
			columnReplaceValue = columnName.trim();
		}

		String fieldReplaceValue = MISSING_VALUE_STRING;
		if (null != fieldValue && fieldValue.trim().length() > 0) {
			fieldReplaceValue = fieldValue.trim();
		}
		
		String validReplaceValue = MISSING_VALUE_STRING;
		if (null != validValue && validValue.trim().length() > 0) {
			validReplaceValue = validValue.trim();
		}

		String result = getFullMessage().replaceAll(COLUMN_NAME_IDENTIFIER, columnReplaceValue);
		result = result.replaceAll(FIELD_VALUE_IDENTIFIER, fieldReplaceValue);
		result = result.replaceAll(VALID_VALUE_IDENTIFIER, validReplaceValue);

		return result;
	}
	
	public RebuildCode getRebuildCode() throws MessageException {
		return new RebuildCode(this);
	}
	
	public String getFieldValue() {
		return fieldValue;
	}
	
	public String getValidValue() {
		return validValue;
	}
}
