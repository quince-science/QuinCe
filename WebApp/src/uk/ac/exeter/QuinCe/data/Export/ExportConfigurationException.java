package uk.ac.exeter.QuinCe.data.Export;

import com.google.gson.JsonElement;

@SuppressWarnings("serial")
public class ExportConfigurationException extends ExportException {

  /**
   * Constructor for a basic message
   *
   * @param name
   *          The name of the export option
   * @param message
   *          The message
   */
  public ExportConfigurationException(String name, String message) {
    super("Error in export config entry '" + name + "': " + message);
  }

  /**
   * Constructor for an underlying error
   *
   * @param name
   *          The name of the export option
   * @param cause
   *          The underlying cause
   */
  public ExportConfigurationException(String name, Throwable cause) {
    super("Error in export config entry '" + name + "': ", cause);
  }

  /**
   * Constructor for an underlying error with a message
   *
   * @param name
   *          The name of the export option
   * @param cause
   *          The underlying cause
   */
  public ExportConfigurationException(String name, String message,
    Throwable cause) {
    super("Error in export config entry '" + name + "': " + message + ": ",
      cause);
  }

  public ExportConfigurationException(JsonElement json, String message) {
    super("Invalid JSON object: " + message + "\n\n" + json.toString());
  }
}
