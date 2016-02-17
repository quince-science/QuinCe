package uk.ac.exeter.QCRoutines.messages;

/**
 * Stores the details of a particular message type.
 * Message types are typically associated with a specific error,
 * e.g. value range or missing value.
 * 
 * The ID must be unique for each message type. Long and short message strings
 * are displayed or logged according to the needs of the system. The message
 * strings can contain two optional parameters:
 * 
 * {@code %c%} The column name
 * {@code %f%} The field value from the file data
 * {@code %v%} The valid value(s) for the field
 * 
 * These must be passed to the {@link Messages} when a message is created,
 * and they will be substituted into the message string when it is generated. If
 * the message requires these values but they are not provided, the message will still
 * be generated but the values will be replaced with {@code MISSING_VALUE}.
 * 
 */
public class MessageType implements Comparable<MessageType> {
	
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

	/**
	 * The unique identifier for this message type
	 */
	private String itsID;
	
	/**
	 * The full. record-specific message string
	 */
	private String itsFullMessage;
	
	/**
	 * The summary message string
	 */
	private String itsSummaryMessage;
	
	
	/**
	 * Simple constructor
	 */
	public MessageType(String id, String fullMessage, String summaryMessage) {
		itsID = id;
		itsFullMessage = fullMessage;
		itsSummaryMessage = summaryMessage;
	}
	
	/**
	 * Returns the id of this message type
	 * @return The id of this message type
	 */
	public String getID() {
		return itsID;
	}
	
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

		String result = itsFullMessage.replaceAll(COLUMN_NAME_IDENTIFIER, columnReplaceValue);
		result = result.replaceAll(FIELD_VALUE_IDENTIFIER, fieldReplaceValue);
		result = result.replaceAll(VALID_VALUE_IDENTIFIER, validReplaceValue);

		return result;
	}

	/**
	 * Generate the short form error message for this error type, substituting in
	 * the supplied field and valid values.
	 * 
	 * If the message requires these values but they are not provided, the message will still
	 * be generated but the values will be replaced with {@link #MISSING_VALUE_STRING}.
	 * 
	 * @param fieldValue The field value from the data file
	 * @param validValue The expected valid value(s)
	 * @return The message string
	 */
	public String getSummaryMessage(String columnName) {

		String columnReplaceValue = MISSING_VALUE_STRING;
		if (null != columnName && columnName.trim().length() > 0) {
			columnReplaceValue = columnName.trim();
		}

		return itsSummaryMessage.replaceAll(COLUMN_NAME_IDENTIFIER, columnReplaceValue);
	}
	
	@Override
	public int compareTo(MessageType compare) {
		return itsID.compareTo(compare.itsID);
	}
}
