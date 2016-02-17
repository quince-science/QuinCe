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
	private int itsColumnIndex;
	
	/**
	 * The type of the messages referred to by this key
	 */
	private MessageType itsMessageType;
	
	/**
	 * Construct a MessageKey object
	 * @param columnIndex The column index
	 * @param messageType The message type
	 */
	public MessageKey(int columnIndex, MessageType messageType) {
		itsColumnIndex = columnIndex;
		itsMessageType = messageType;
	}
	
	/**
	 * Return the column index
	 * @return The column index
	 */
	public int getColumnIndex() {
		return itsColumnIndex;
	}
	
	/**
	 * Return the message type
	 * @return The message type
	 */
	public MessageType getMessageType() {
		return itsMessageType;
	}
	
	@Override
	public int compareTo(MessageKey compare) {
		
		// Compare the column index first
		int result = this.itsColumnIndex - compare.itsColumnIndex;
		
		if (result == 0) {
			result = itsMessageType.compareTo(compare.itsMessageType);
		}
		
		return result;
	}
}
