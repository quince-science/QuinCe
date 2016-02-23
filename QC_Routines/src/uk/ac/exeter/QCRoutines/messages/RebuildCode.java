package uk.ac.exeter.QCRoutines.messages;

import java.lang.reflect.Constructor;

public class RebuildCode {
	
	private static final int CODE_INDEX_CLASS_NAME = 0;
	
	private static final int CODE_INDEX_COL_INDEX = 1;
	
	private static final int CODE_INDEX_SEVERITY = 2;

	private Class<?> messageClass;
	
	private int columnIndex;
	
	private String columnName;
	
	private int severity;
	
	public RebuildCode(Message message) throws MessageException {
		messageClass = message.getClass();
		columnIndex = message.getColumnIndex();
		columnName = message.getColumnName();
		severity = message.getSeverity();
		validateMessageClass();
	}
	
	public RebuildCode(String code) throws RebuildCodeException {
			
		String[] codeComponents = code.split("_");
		if (codeComponents.length != 3) {
			throw new RebuildCodeException("Incorrect number of elements");
		} else {
			
			try {
				messageClass = Class.forName(codeComponents[CODE_INDEX_CLASS_NAME]);
			} catch (ClassNotFoundException e) {
				throw new RebuildCodeException("Cannot find message class '" + codeComponents[0] + "'");
			}
			
			try {
				columnIndex = Integer.parseInt(codeComponents[CODE_INDEX_COL_INDEX]);
				if (columnIndex < 0) {
					throw new RebuildCodeException("Invalid column index value");
				}
			} catch (NumberFormatException e) {
				throw new RebuildCodeException("Unparseable column index value");
			}
			
			try {
				severity = Integer.parseInt(codeComponents[CODE_INDEX_SEVERITY]);
				if (severity != Message.ERROR && severity != Message.WARNING) {
					throw new RebuildCodeException("Invalid severity value");
				}
			} catch (NumberFormatException e) {
				throw new RebuildCodeException("Unparseable severity value");
			}
		}
	}
	
	private void validateMessageClass() throws MessageException {
		// Message classes must have a constructor that takes
		// int columnIndex, String columnName, int severity, int lineNumber, String fieldValue, String validValue
		
		try {
			messageClass.getConstructor(int.class, String.class, int.class, int.class, String.class, String.class);
		} catch (Exception e) {
			throw new RebuildCodeException("Message class does not have the required constructor"); 
		}
	}
	
	public String getCode() {
		StringBuffer result = new StringBuffer();
		result.append(messageClass.getName());
		result.append('_');
		result.append(columnIndex);
		result.append('_');
		result.append(columnName);
		result.append('_');
		result.append(severity);
		result.append(';');
		
		return result.toString();
	}
	
	public String toString() {
		return getCode();
	}
	
	public Message getMessage(int lineNumber, String fieldValue, String validValue) throws MessageException {
		try {
			Constructor<?> messageConstructor = messageClass.getConstructor(int.class, String.class, int.class, int.class, String.class, String.class);
			return (Message) messageConstructor.newInstance(columnIndex, columnName, severity, lineNumber, fieldValue, validValue);
		} catch (Exception e) {
			throw new MessageException("Error while constructing message object from rebuild code", e);
		}
	}
}
