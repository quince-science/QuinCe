package uk.ac.exeter.QuinCe.data.Export;

public class ExportConfigurationException extends ExportException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -6755072895338252798L;

  /**
   * Constructor for a basic message
   * @param index The index of the entry in the configuration that failed
   * @param message The message
   */
  public ExportConfigurationException(int index, String message) {
    super("Error in export config entry " + index + ": " + message);
  }

  /**
   * Constructor for an underlying error
   * @param index The index of the entry in the configuration that failed
   * @param message The error message
   * @param cause The underlying cause
   */
  public ExportConfigurationException(int index, Throwable cause) {
    super("Error in export config entry " + index + ": ", cause);
  }
}
