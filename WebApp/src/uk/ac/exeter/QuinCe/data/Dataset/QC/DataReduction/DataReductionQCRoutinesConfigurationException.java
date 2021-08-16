package uk.ac.exeter.QuinCe.data.Dataset.QC.DataReduction;

/**
 * Exception class for errors encountered in the configuration of data reduction
 * QC routines.
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class DataReductionQCRoutinesConfigurationException extends Exception {

  /**
   * The name of the configuration file in which the error was encountered.
   */
  private String configFile;

  /**
   * The name of the configuration item where the error was found
   */
  private String itemName;

  /**
   * Creates an exception that doesn't relate to a specific configuration item
   *
   * @param configFile
   *          The path of the file being worked on when the exception occurred.
   * @param message
   *          The error message.
   */
  public DataReductionQCRoutinesConfigurationException(String configFile,
    String message) {
    super(message);
    this.configFile = configFile;
  }

  /**
   * Creates an exception for an error pertaining to a specific configuration
   * item
   *
   * @param configFile
   *          The path of the file being worked on when the exception occurred.
   * @param itemName
   *          The name of the item where the error was encountered
   * @param lineNumber
   *          The line number of the file on which the error occurred
   * @param message
   *          The error message
   */
  public DataReductionQCRoutinesConfigurationException(String configFile,
    String itemName, String message) {

    super(message);
    this.configFile = configFile;
    this.itemName = itemName;
  }

  /**
   * Creates an exception relating to a specific line with a root cause
   *
   * @param configFile
   *          The path of the file being worked on when the exception occurred.
   * @param itemName
   *          The name of the item where the error was encountered
   * @param lineNumber
   *          The line number of the file on which the error occurred
   * @param message
   *          The error message.
   * @param cause
   *          The underlying cause of the exception
   */
  public DataReductionQCRoutinesConfigurationException(String configFile,
    String itemName, String message, Throwable cause) {

    super(message, cause);
    this.configFile = configFile;
    this.itemName = itemName;
  }

  /**
   * Create a configuration exception that has an underlying cause
   *
   * @param configFile
   *          The path of the file being worked on when the exception occurred.
   * @param message
   *          The error message.
   * @param cause
   *          The exception that caused the error.
   */
  public DataReductionQCRoutinesConfigurationException(String configFile,
    String message, Throwable cause) {

    super(message, cause);
    this.configFile = configFile;
  }

  /**
   * Create a configuration exception that has an underlying cause
   *
   * @param configFile
   *          The path of the file being worked on when the exception occurred.
   * @param cause
   *          The exception that caused the error.
   */
  public DataReductionQCRoutinesConfigurationException(String configFile,
    Throwable cause) {

    super(cause);
    this.configFile = configFile;
  }

  /**
   * Returns the message of the exception, file name, item name and line number.
   *
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

    message.append(": ");
    message.append(super.getMessage());

    if (null != getCause()) {
      message.append(" ->\n" + getCause().getMessage());
    }

    return message.toString();
  }

  /**
   * Returns the path of the file being worked on when the exception occurred.
   *
   * @return The path of the file being worked on when the exception occurred.
   */
  public String getFile() {
    return configFile;
  }

  /**
   * Returns the name of the configuration item that was being processed when
   * the error occurred.
   *
   * @return The name of the configuration item
   */
  public String getItemName() {
    return itemName;
  }

  /**
   * Returns just the error message from the exception without the filename,
   * item name or line number, as per {@link Exception#getMessage}.
   *
   * @return The error message of the exception without the filename.
   */
  public String getMessageOnly() {
    return super.getMessage();
  }

}
