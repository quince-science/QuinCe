package uk.ac.exeter.QuinCe.web.files;

import java.nio.charset.StandardCharsets;
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
	private UploadedFile uploadedFile;
	private int rowCount = 0;
	private boolean store = true;
	private DataFile dataFile = null;
	private ArrayList<FacesMessage> messages = new ArrayList<>();

	public UploadedDataFile(UploadedFile file) {
		setUploadedFile(file);
	}
	/**
	 * @return the uploadedFile
	 */
	public UploadedFile getUploadedFile() {
		return uploadedFile;
	}
	/**
	 * @param uploadedFile the uploadedFile to set
	 */
	public void setUploadedFile(UploadedFile uploadedFile) {
		this.uploadedFile = uploadedFile;
		String[] fileLines = getLines();
		rowCount = fileLines.length;
		while ("".equals(fileLines[rowCount - 1])) {
			rowCount--;
		}
	}
	public String[] getLines() {
		String fileContent = new String(uploadedFile.getContents(), StandardCharsets.UTF_8);
		return fileContent.split("[\\r\\n]+");
	}
	/**
	 * Get number of rows in the file
	 * @return the rowCount
	 */
	public int getRowCount() {
		return rowCount;
	}

	/**
	 * @param rowCount the rowCount to set
	 */
	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}

	/**
	 * @return the base file name
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
		return null == dataFile ? null : Date.from(dataFile.getStartDate().atZone(ZoneId.of("UTC")).toInstant());
	}

	/**
	 * @return the endDate
	 * @throws DataFileException
	 */
	public Date getEndDate() throws DataFileException {
		return null == dataFile ? null : Date.from(dataFile.getEndDate().atZone(ZoneId.of("UTC")).toInstant());
	}

	/**
	 * @return the messageClass
	 */
	public String getMessageClass() {
		String messageClass = "";
		Severity level = null;
		for (FacesMessage message: messages) {
			if (level == null || message.getSeverity().compareTo(level) > 0) {
				level = message.getSeverity();
			}
		}
		messageClass = getSeverityLabel(level);
		if ("".equals(messageClass) && dataFile != null && dataFile.getMessages().size() > 0) {
			messageClass = getSeverityLabel(FacesMessage.SEVERITY_INFO);
		}
		return messageClass;
	}

	public String getMissingRunTypeClass() {
		if (null != dataFile && dataFile.getMissingRunTypes().size() > 0) {
			return "shown";
		}
		return "hidden";
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
}
