package uk.ac.exeter.QuinCe.utils;

import org.primefaces.json.JSONObject;

/**
 * @author jhe052
 *
 */
public class Message {

  public static final String MESSAGE_KEY = "message";

  public static final String DETAILS_KEY = "details";

  private String message = "";

  private String details = "";

  /**
   * Default, empty constructor
   */
  public Message() {
  }

  /**
   * Constructor initialised with message and message details
   */
  public Message(String message, String details) {
    this.message = message;
    this.details = details;
  }

  public String getMessage() {
    return message;
  }

  public String getDetails() {
    return details;
  }

  /**
   * @param messageDetails
   *          the message details to set
   */
  public void setDetails(String details) {
    this.details = details == null ? "" : details;
  }

  /**
   * @param message
   *          the message to set
   */
  public void setMessage(String message) {
    this.message = message == null ? "" : message;
  }

  public JSONObject getAsJSON() {
    JSONObject json = null;
    if (getMessage() != "") {
      json = new JSONObject();
      json.put(Message.MESSAGE_KEY, getMessage());
      json.put(Message.DETAILS_KEY, getDetails());
    }
    return json;
  }
}
