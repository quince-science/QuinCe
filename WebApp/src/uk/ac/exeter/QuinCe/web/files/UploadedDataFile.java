package uk.ac.exeter.QuinCe.web.files;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;
import org.primefaces.model.UploadedFile;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileMessage;

/**
 * @author Jonas F. Henriksen
 *
 */
public class UploadedDataFile {

  /**
   * The uploaded file
   */
  private UploadedFile uploadedFile;

  /**
   * Indicates whether or not the file should be stored
   */
  private boolean store = true;

  /**
   * The processed file
   */
  private DataFile dataFile = null;

  /**
   * Error messages for the file
   */
  private ArrayList<FacesMessage> messages = new ArrayList<>();

  /**
   * Indicates whether or not the file has been
   * extracted and processed
   */
  private boolean processed = false;

  /**
   * Basic constructor - reads the file contents ready for processing
   * @param file The uploaded file
   */
  public UploadedDataFile(UploadedFile file) {
    setUploadedFile(file);
  }

  /**
   * Get the uploaded file
   * @return The uploaded file
   */
  public UploadedFile getUploadedFile() {
    return uploadedFile;
  }

  /**
   * Assign the uploaded file and extract its contents
   * @param uploadedFile the uploadedFile to set
   */
  public void setUploadedFile(UploadedFile uploadedFile) {
    this.uploadedFile = uploadedFile;
    getLines();
  }

  /**
   * Extract the file contents as individual lines
   * @return The file lines
   */
  public String[] getLines() {
    String fileContent = new String(uploadedFile.getContents(), StandardCharsets.UTF_8);
    if (null == fileContent || fileContent.trim().length() == 0) {
      return null;
    }
    return fileContent.split("[\\r\\n]+");
  }

  /**
   * Get the filename of the file
   * @return The filename
   */
  public String getName() {
    return uploadedFile.getFileName();
  }

  /**
   * @return indication whether this file should be stored in the database
   */
  public boolean isStore() {
    return store;
  }

  /**
   * @param store says whether this file should be stored to the database
   */
  public void setStore(boolean store) {
    this.store = store;
  }

  /**
   * @return the dataFile
   */
  public DataFile getDataFile() {
    return dataFile;
  }

  /**
   * @param dataFile the dataFile to set
   */
  public void setDataFile(DataFile dataFile) {
    this.dataFile = dataFile;
  }

  /**
   * @return the startDate
   */
  public Date getStartDate() {
    Date date = null;
    if (null != dataFile) {
      LocalDateTime localDate = dataFile.getStartDate();
      if (null != localDate) {
        date = Date.from(localDate.atZone(ZoneId.of("UTC")).toInstant());
      }
    }
    return date;
  }

  /**
   * @return the endDate
   * @throws DataFileException
   */
  public Date getEndDate() {
    Date date = null;
    if (null != dataFile) {
      LocalDateTime localDate = dataFile.getEndDate();
      if (null != localDate) {
        date = Date.from(localDate.atZone(ZoneId.of("UTC")).toInstant());
      }
    }
    return date;
  }

  /**
   * @param summary
   * @param severityError
   */
  public void putMessage(String summary, Severity severityError) {
    messages.add(new FacesMessage(severityError, summary, ""));
    setStore(false);
  }

  /**
   * Get info and error-messages as a JSON-structure.
   * @return a json-array with messages
   */
  public String getMessages() {
    JSONArray jsonArray = new JSONArray();
    for (FacesMessage message: messages) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("summary", message.getSummary());
      jsonObject.put("severity", getSeverityLabel(message.getSeverity()));
      jsonObject.put("type", "file");
      jsonArray.put(jsonObject);
    }
    if (null != dataFile) {
      for (DataFileMessage message: dataFile.getMessages()) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("summary", message.toString());
        jsonObject.put("severity", getSeverityLabel(FacesMessage.SEVERITY_INFO));
        jsonObject.put("type", "row");
        jsonArray.put(jsonObject);
      }
    }
    return jsonArray.toString();
  }

  /**
   * Get a label indicating the severity of the error. This can be used eg. as a css-class in the
   * front-end.
   *
   * @param severity Severity-level of the message
   * @return the string label.
   */
  public String getSeverityLabel(Severity severity) {
    String severityClass = "";
    if (FacesMessage.SEVERITY_WARN.equals(severity)) {
      severityClass = "warning";
    } else if (FacesMessage.SEVERITY_ERROR.equals(severity)) {
      severityClass = "error";
    } else if (FacesMessage.SEVERITY_FATAL.equals(severity)) {
      severityClass = "fatal";
    } else if (FacesMessage.SEVERITY_INFO.equals(severity)) {
      severityClass = "info";
    }
    return severityClass;
  }

  /**
   * Determine whether or not this file has been extraced
   * and processed.
   * @return {@code true} if the file has been processed; {@code false} if it has not
   */
  public boolean isProcessed() {
    return processed;
  }

  /**
   * Set the flag indicating whether or not the file has been processed
   * @param processed The processed flag
   */
  public void setProcessed(boolean processed) {
    this.processed = processed;
  }

  /**
   * Determine whether or not error messages have been
   * generated for this file
   * @return {@code true} if there are messages; {@code false} if there are none
   */
  public boolean getHasMessages() {
    return null != messages && messages.size() > 0;
  }

  /**
   * Determine whether or not unrecognised run types have been detected
   * in the file
   * @return {@code true} if unrecognised run types have been found; {@code false} otherwise
   */
  public boolean getHasUnrecognisedRunTypes() {
    return null != dataFile && dataFile.getMissingRunTypes().size() > 0;
  }
}
