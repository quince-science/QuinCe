package uk.ac.exeter.QuinCe.web.files;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.sql.DataSource;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileMessage;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.web.Instrument.newInstrument.FileDefinitionBuilder;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * @author Jonas F. Henriksen
 *
 */
public abstract class UploadedDataFile {

  /**
   * The contents of the file split into lines
   */
  private String[] fileLines = null;

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
   * The database ID of the existing file that this file will replace
   * -1 indicates that this is a completely new file
   */
  private long replaceFile = -1;

  /**
   * Extract the file contents as individual lines
   * @return The file lines
   */
  public String[] getLines() {
    if (null == fileLines) {
      String fileContent = getFileContents();
      if (null == fileContent || fileContent.trim().length() == 0) {
        fileLines = null;
      } else {
        fileLines = fileContent.split("[\\r\\n]+");
      }
    }

    return fileLines;
  }

  /**
   * Get the filename of the file
   * @return The filename
   */
  public abstract String getName();

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

  /**
   * Determine whether or not this file will replace an existing file
   * @return {@code true} if this is a replacement file; {@code false} if it is not
   */
  public boolean isReplacement() {
    return (replaceFile != -1);
  }

  /**
   * Set the ID of the data file that this file will replace
   * @param oldId
   */
  public void setReplacementFile(long oldId) {
    this.replaceFile = oldId;
  }

  /**
   * Get the ID of the file that this file will replace
   * @return
   */
  public long getReplacementFile() {
    return replaceFile;
  }

  protected void extractFile(Instrument instrument, Properties appConfig, boolean allowExactDuplicate) {
    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

      FileDefinitionBuilder guessedFileLayout = new FileDefinitionBuilder(instrument.getFileDefinitions());
      String[] lines = getLines();
      if (null == lines) {
        throw new DataFileException("File contains no data");
      }
      guessedFileLayout.setFileContents(Arrays.asList(lines));
      guessedFileLayout.guessFileLayout();
      FileDefinition fileDefinition = instrument.getFileDefinitions()
          .getMatchingFileDefinition(guessedFileLayout).iterator().next();
      // TODO Handle multiple matched definitions

      setDataFile(new DataFile(
          appConfig.getProperty("filestore"),
          fileDefinition,
          getName(),
          Arrays.asList(lines)
      ));
      if (getDataFile().getFirstDataLine() >= getDataFile()
          .getContentLineCount()) {
        throw new DataFileException("File contains headers but no data");
      }

      if (null == getDataFile().getStartDate()
          || null == getDataFile().getEndDate()) {
        putMessage(getName()
            + " has date issues, see messages below. Please fix these problems and upload the file again.",
            FacesMessage.SEVERITY_ERROR);
      } else if (getDataFile().getMessageCount() > 0) {
        putMessage(getName()
            + " could not be processed (see messages below). Please fix these problems and upload the file again.",
            FacesMessage.SEVERITY_ERROR);
      } else {
        List<DataFile> overlappingFiles = DataFileDB.getFilesWithinDates(dataSource, fileDefinition,
            getDataFile().getStartDate(), getDataFile().getEndDate());

        boolean fileOK = true;
        String fileMessage = null;

        if (overlappingFiles.size() > 0 && overlappingFiles.size() > 1) {
          fileOK = false;
          fileMessage = "This file overlaps one or more existing files";
        } else if (overlappingFiles.size() == 1) {
          DataFile existingFile = overlappingFiles.get(0);
          DataFile newFile = getDataFile();

          if (!existingFile.getFilename().equals(newFile.getFilename())) {
            fileOK = false;
            fileMessage = "This file overlaps an existing file with a different name";
          } else {
            String oldContents = existingFile.getContents();
            String newContents = newFile.getContents();

            if (newContents.length() < oldContents.length()) {
              fileOK = false;
              fileMessage = "This file would replace an existing file with fewer records";
            } else if (!allowExactDuplicate && newContents.length() == oldContents.length()) {
              fileOK = false;
              fileMessage = "This is an exact copy of an existing file";
            }

            if (fileOK) {
              String oldPartOfNewContents = newContents.substring(0, oldContents.length());
              if (!oldPartOfNewContents.equals(oldContents)) {
                fileOK = false;
                fileMessage = "This file would update an existing file but change existing data";
              } else {
                setReplacementFile(existingFile.getDatabaseId());
              }
            }
          }
        } else if (DataFileDB.hasFileWithName(dataSource, instrument.getDatabaseId(),
            getName())) {

          // We don't allow duplicate filenames
          fileOK = false;
          fileMessage = "A file with that name already exists";
        }

        if (!fileOK) {
          fileDefinition = null;
          setDataFile(null);
          putMessage(fileMessage, FacesMessage.SEVERITY_ERROR);
        }
      }
    } catch (NoSuchElementException nose) {
      setDataFile(null);
      putMessage("The format of " + getName() + " was not recognised. Please upload a different file.", FacesMessage.SEVERITY_ERROR);
    } catch (Exception e) {
      e.printStackTrace();
      setDataFile(null);
      putMessage("The file could not be processed: " + e.getMessage(), FacesMessage.SEVERITY_ERROR);
    }

    setProcessed(true);
  }

  /**
   * Get the contents of the file
   * @return The file contents
   */
  protected abstract String getFileContents();
}
