package uk.ac.exeter.QuinCe.data.Dataset.QC.Routines;

public class QCRoutinesConfigurationException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 10002001L;

  /**
   * The name of the configuration file in which the error was encountered.
   */
  private String configFile;

  /**
   * The name of the configuration item where the error was found
   */
  private String itemName;

  /**
   * The line of the config file where the error occurred
   */
  private int lineNumber;

  /**
   * Creates an exception that doesn't relate to a specific configuration item
   * @param configFile The path of the file being worked on when the exception occurred.
   * @param message The error message.
   */
  public QCRoutinesConfigurationException(String configFile, String message) {
    super(message);
    this.configFile = configFile;
  }

  /**
   * Creates an exception relating to a specific line
   * @param configFile The path of the file being worked on when the exception occurred.
   * @param lineNumber The line number of the file on which the error occurred
   * @param message The error message.
   */
  public QCRoutinesConfigurationException(String configFile, int lineNumber, String message) {
    super(message);
    this.configFile = configFile;
    this.lineNumber = lineNumber;
  }

  /**
   * Creates an exception relating to a specific line with a root cause
   * @param configFile The path of the file being worked on when the exception occurred.
   * @param lineNumber The line number of the file on which the error occurred
   * @param message The error message.
   * @param cause The underlying cause of the exception
   */
  public QCRoutinesConfigurationException(String configFile, int lineNumber,
    String message, Throwable cause) {

    super(message, cause);
    this.configFile = configFile;
    this.lineNumber = lineNumber;
  }

  /**
   * Creates an exception for an error pertaining to a specific configuration item
   * @param configFile The path of the file being worked on when the exception occurred.
   * @param itemName The name of the item where the error was encountered
   * @param lineNumber The line number of the file on which the error occurred
   * @param message The error message
   */
  public QCRoutinesConfigurationException(String configFile, String itemName,
    int lineNumber, String message) {

    super(message);
    this.configFile = configFile;
    this.itemName = itemName;
    this.lineNumber = lineNumber;
  }

  /**
   * Creates an exception relating to a specific line with a root cause
   * @param configFile The path of the file being worked on when the exception occurred.
   * @param itemName The name of the item where the error was encountered
   * @param lineNumber The line number of the file on which the error occurred
   * @param message The error message.
   * @param cause The underlying cause of the exception
   */
  public QCRoutinesConfigurationException(String configFile, String itemName,
    int lineNumber, String message, Throwable cause) {

    super(message, cause);
    this.configFile = configFile;
    this.itemName = itemName;
    this.lineNumber = lineNumber;
  }

  /**
   * Create a configuration exception that has an underlying cause
   * @param configFile The path of the file being worked on when the exception occurred.
   * @param message The error message.
   * @param cause The exception that caused the error.
   */
  public QCRoutinesConfigurationException(String configFile, String message,
    Throwable cause) {

    super(message, cause);
    this.configFile = configFile;
  }

  /**
   * Returns the message of the exception, file name, item name and line number.
   * @return The exception message
   */
  @Override
  public String getMessage() {
    StringBuffer message = new StringBuffer();

    message.append("FILE ");
    message.append(configFile);

    if (null != itemName) {
      message.append(", ITEM ");
      message.append(itemName);
    }

    if (-1 != lineNumber) {
      message.append(", LINE ");
      message.append(lineNumber);
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
    return configFile;
  }

  /**
   * Returns the name of the configuration item that was being processed
   * when the error occurred.
   * @return The name of the configuration item
   */
  public String getItemName() {
    return itemName;
  }

  /**
   * Returns the line number on which the error occurred.
   * @return The line number on which the error occurred.
   */
  public int getLine() {
    return lineNumber;
  }

  /**
   * Returns just the error message from the exception without the filename, item
   * name or line number, as per {@link Exception#getMessage}.
   * @return The error message of the exception without the filename.
   */
  public String getMessageOnly() {
    return super.getMessage();
  }

}
