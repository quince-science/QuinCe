package uk.ac.exeter.QCRoutines.messages;

/**
 * Key class for organising messages. Consists of the column index
 * and message type
 */
public class MessageKey implements Comparable<MessageKey> {

	/**
	 * The index of the column that messages under this key
	 * refer to
	 */
	private int columnIndex;
	
	/**
	 * The type of the messages referred to by this key
	 */
	private Class<?> messageClass;
	
	/**
	 * Construct a MessageKey object
	 * @param columnIndex The column index
	 * @param messageType The message type
	 */
	public MessageKey(int columnIndex, Class<?> messageClass) {
		this.columnIndex = columnIndex;
		this.messageClass = messageClass;
	}
	
	/**
	 * Return the column index
	 * @return The column index
	 */
	public int getColumnIndex() {
		return columnIndex;
	}
	
	/**
	 * Return the message type
	 * @return The message type
	 */
	public Class<?> getMessageClass() {
		return messageClass;
	}
	
	@Override
	public int compareTo(MessageKey compare) {
		
		// Compare the column index first
		int result = this.columnIndex - compare.columnIndex;
		
		if (result == 0) {
			result = messageClass.getName().compareTo(compare.messageClass.getName());
		}
		
		return result;
	}
}
