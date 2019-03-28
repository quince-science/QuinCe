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
import javax.ws.rs.core.Response.Status;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileMessage;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.web.Instrument.newInstrument.FileDefinitionBuilder;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * @author Jonas F. Henriksen
 *
 */
public abstract class UploadedDataFile {

  /**
   * HTTP Status Code to use for files that can't be processed
   * due to data issues (not defined in the {#Status} class).
   */
  private static final int UNPROCESSABLE_STATUS = 422;

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
   * Return status code for the uploaded file (for use with API calls)
   */
  private int statusCode = Status.OK.getStatusCode();

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
  public void putMessage(int statusCode, String summary, Severity severityError) {
    messages.add(new FacesMessage(severityError, summary, ""));
    this.statusCode = statusCode;
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

  /**
   * Extract the file contents and ensure that it doesn't clash with existing files
   * @param instrument The instrument to which the file belongs
   * @param appConfig The application configuration
   * @param allowExactDuplicate Indicates whether exact duplicate files are accepted
   */
  public void extractFile(Instrument instrument, Properties appConfig, boolean allowExactDuplicate, boolean allowEmpty) {
    boolean fileEmpty = false;

    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

      InstrumentFileSet fileDefinitions = instrument.getFileDefinitions();
      String[] lines = getLines();
      if (null == lines) {
        if (allowEmpty) {
          fileEmpty = true;
        } else {
          throw new DataFileException(DataFileException.NO_FILE_ID, DataFileException.NO_LINE_NUMBER, "File contains no data");
        }
      }

      if (!fileEmpty) {
        FileDefinitionBuilder layoutGuesser =
          new FileDefinitionBuilder("Guesser", fileDefinitions);

        layoutGuesser.setFileContents(Arrays.asList(lines));
        layoutGuesser.guessFileLayout();

        // See if any of the known definitions match the guessed layout
        FileDefinition matchedDefinition =
          fileDefinitions.getMatchingFileDefinition(layoutGuesser).iterator().next();
        // TODO We're assuming we'll get one match. No matches will throw a NoSuchElementException
        //      (handled below), and multiple matches just choose the first one

        setDataFile(new DataFile(
            appConfig.getProperty("filestore"),
            matchedDefinition,
            getName(),
            Arrays.asList(lines)
        ));
        if (getDataFile().getFirstDataLine() >= getDataFile()
            .getContentLineCount()) {
          if (allowEmpty) {
            fileEmpty = true;
          } else {
            throw new DataFileException(DataFileException.NO_FILE_ID, DataFileException.NO_LINE_NUMBER, "File contains headers but no data");
          }
        }

        if (!fileEmpty) {
          if (null == getDataFile().getStartDate()
              || null == getDataFile().getEndDate()) {
            putMessage(UNPROCESSABLE_STATUS, getName()
                + " has date issues, see messages below. Please fix these problems and upload the file again.",
                FacesMessage.SEVERITY_ERROR);
          } else if (getDataFile().getMessageCount() > 0) {
            putMessage(UNPROCESSABLE_STATUS, getName()
                + " could not be processed (see messages below). Please fix these problems and upload the file again.",
                FacesMessage.SEVERITY_ERROR);
          } else {
            List<DataFile> overlappingFiles = DataFileDB.getFilesWithinDates(dataSource, matchedDefinition,
                getDataFile().getStartDate(), getDataFile().getEndDate());

            boolean fileOK = true;
            String fileMessage = null;
            int fileStatus = Status.OK.getStatusCode();

            if (overlappingFiles.size() > 0 && overlappingFiles.size() > 1) {
              fileOK = false;
              fileMessage = "This file overlaps one or more existing files";
              fileStatus = Status.CONFLICT.getStatusCode();
            } else if (overlappingFiles.size() == 1) {
              DataFile existingFile = overlappingFiles.get(0);
              DataFile newFile = getDataFile();

              if (!existingFile.getFilename().equals(newFile.getFilename())) {
                fileOK = false;
                fileMessage = "This file overlaps an existing file with a different name";
                fileStatus = Status.CONFLICT.getStatusCode();
              } else {
                String oldContents = existingFile.getContents();
                String newContents = newFile.getContents();

                if (newContents.length() < oldContents.length()) {
                  fileOK = false;
                  fileMessage = "This file would replace an existing file with fewer records";
                  fileStatus = Status.CONFLICT.getStatusCode();
                } else if (!allowExactDuplicate && newContents.length() == oldContents.length()) {
                  fileOK = false;
                  fileMessage = "This is an exact copy of an existing file";
                  fileStatus = Status.CONFLICT.getStatusCode();
                } else {
                  String oldPartOfNewContents = newContents.substring(0, oldContents.length());
                  if (!oldPartOfNewContents.equals(oldContents)) {
                    fileOK = false;
                    fileMessage = "This file would update an existing file but change existing data";
                    fileStatus = Status.CONFLICT.getStatusCode();
                  } else {
                    setReplacementFile(existingFile.getDatabaseId());
                  }
                }
              }
            } else if (DataFileDB.hasFileWithName(dataSource, instrument.getDatabaseId(), getName())) {

              // We don't allow duplicate filenames
              fileOK = false;
              fileMessage = "A file with that name already exists";
              fileStatus = Status.CONFLICT.getStatusCode();
            }

            if (!fileOK) {
              matchedDefinition = null;
              setDataFile(null);
              putMessage(fileStatus, fileMessage, FacesMessage.SEVERITY_ERROR);
            }
          }
        }
      }
    } catch (NoSuchElementException nose) {
      setDataFile(null);
      putMessage(Status.BAD_REQUEST.getStatusCode(), "The format of " + getName() + " was not recognised. Please upload a different file.", FacesMessage.SEVERITY_ERROR);
    } catch (Exception e) {
      e.printStackTrace();
      setDataFile(null);
      putMessage(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "The file could not be processed: " + e.getMessage(), FacesMessage.SEVERITY_ERROR);
    }

    setProcessed(true);
  }

  /**
   * Get the HTTP response status code
   * @return The status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Get the contents of the file
   * @return The file contents
   */
  protected abstract String getFileContents();
}
