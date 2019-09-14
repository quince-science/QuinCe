package uk.ac.exeter.QuinCe.data.Export;

import org.primefaces.json.JSONObject;

public class ExportConfigurationException extends ExportException {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -6755072895338252798L;

  /**
   * Constructor for a basic message
   * 
   * @param index
   *          The index of the entry in the configuration that failed
   * @param message
   *          The message
   */
  public ExportConfigurationException(String name, String message) {
    super("Error in export config entry '" + name + "': " + message);
  }

  /**
   * Constructor for an underlying error
   * 
   * @param index
   *          The index of the entry in the configuration that failed
   * @param message
   *          The error message
   * @param cause
   *          The underlying cause
   */
  public ExportConfigurationException(String name, Throwable cause) {
    super("Error in export config entry '" + name + "': ", cause);
  }

  public ExportConfigurationException(JSONObject json, String message) {
    super("Invalid JSON object: " + message + "\n\n" + json.toString());
  }
}
