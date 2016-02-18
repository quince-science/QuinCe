package uk.ac.exeter.QCRoutines.config;

/**
 * An exception class for errors encountered in configuration files.
 *
 */
public class ConfigException extends Exception {
	
	/**
	 * Version string for the BaseConfig class. This should be changed if the
	 * class is ever updated and becomes incompatible with previous versions.
	 */
	private static final long serialVersionUID = 10002001L;

	/**
	 * The name of the configuration file in which the error was encountered.
	 */
	private String itsFile;
	
	/**
	 * The name of the configuration item where the error was found
	 */
	private String itsItemName;

	/**
	 * The line of the config file where the error occurred
	 */
	private int itsLineNumber;
	
	/**
	 * Creates an exception that doesn't relate to a specific configuration item
	 * @param file The path of the file being worked on when the exception occurred.
	 * @param message The error message.
	 */
	public ConfigException(String file, String message) {
		super(message);
		itsFile = file;
	}
	
	/**
	 * Creates an exception relating to a specific line
	 * @param file The path of the file being worked on when the exception occurred.
	 * @param lineNumber The line number of the file on which the error occurred
	 * @param message The error message.
	 */
	public ConfigException(String file, int lineNumber, String message) {
		super(message);
		itsFile = file;
		itsLineNumber = lineNumber;
	}
	
	/**
	 * Creates an exception relating to a specific line with a root cause
	 * @param file The path of the file being worked on when the exception occurred.
	 * @param lineNumber The line number of the file on which the error occurred
	 * @param message The error message.
	 * @param cause The underlying cause of the exception
	 */
	public ConfigException(String file, int lineNumber, String message, Throwable cause) {
		super(message, cause);
		itsFile = file;
		itsLineNumber = lineNumber;
	}
	
	/**
	 * Creates an exception for an error pertaining to a specific configuration item
	 * @param file The path of the file being worked on when the exception occurred.
	 * @param item The name of the item where the error was encountered
	 * @param lineNumber The line number of the file on which the error occurred
	 * @param message The error message
	 */
	public ConfigException(String file, String item, int lineNumber, String message) {
		super(message);
		itsFile = file;
		itsItemName = item;
		itsLineNumber = lineNumber;
	}

	/**
	 * Creates an exception relating to a specific line with a root cause
	 * @param file The path of the file being worked on when the exception occurred.
	 * @param item The name of the item where the error was encountered
	 * @param lineNumber The line number of the file on which the error occurred
	 * @param message The error message.
	 * @param cause The underlying cause of the exception
	 */
	public ConfigException(String file, String item, int lineNumber, String message, Throwable cause) {
		super(message, cause);
		itsFile = file;
		itsItemName = item;
		itsLineNumber = lineNumber;
	}
	
	/**
	 * Create a configuration exception that has an underlying cause
	 * @param file The path of the file being worked on when the exception occurred.
	 * @param message The error message.
	 * @param cause The exception that caused the error.
	 */
	public ConfigException(String file, String message, Throwable cause) {
		super(message, cause);
		itsFile = file;
	}
	
	/**
	 * Returns the message of the exception, including the name of the file.
	 * @return The message of the exception, including the name of the file.
	 */
	public String getMessage() {
		StringBuffer message = new StringBuffer();
		
		message.append("FILE ");
		message.append(itsFile);
		
		if (null != itsItemName) {
			message.append(", ITEM ");
			message.append(itsItemName);
		}
		
		if (-1 != itsLineNumber) {
			message.append(", LINE ");
			message.append(itsLineNumber);
		}
		
		message.append(": ");
		message.append(super.getMessage());
		
		if (null != getCause()) {
			message.append(" ->\n" + getCause().getMessage());
		}
		
		return message.toString();
	}
	
	/**
	 * Returns the path of the file being worked on when the exception occurred.
	 * @return The path of the file being worked on when the exception occurred.
	 */
	public String getFile() {
		return itsFile;
	}
	
	/**
	 * Returns the name of the configuration item that was being processed
	 * when the error occurred.
	 * @return The name of the configuration item
	 */
	public String getItemName() {
		return itsItemName;
	}
	
	/**
	 * Returns the line number on which the error occurred.
	 * @return The line number on which the error occurred.
	 */
	public int getLine() {
		return itsLineNumber;
	}
	
	/**
	 * Returns just the error message from the exception without the filename, as per
	 * {@code Exception.getMessage()}.
	 * @return The error message of the exception without the filename.
	 */
	public String getMessageOnly() {
		return super.getMessage();
	}

}
