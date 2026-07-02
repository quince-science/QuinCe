package uk.ac.exeter.QuinCe.web.files;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.sql.DataSource;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileMessage;
import uk.ac.exeter.QuinCe.data.Files.TimeDataFile;
import uk.ac.exeter.QuinCe.data.Files.UploadedFileContents;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFileSet;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Holds the contents and processing state of a data file uploaded to QuinCe.
 *
 * <p>
 * This is a wrapper around a {@link DataFile} object that can match an uploaded
 * file to its {@link FileDefinition}, extract its contents, and track whether
 * or not the file can be stored in the database.
 * </p>
 */
public abstract class UploadedDataFile implements Comparable<UploadedDataFile> {

  /**
   * HTTP Status Code to use for files that can't be processed due to data
   * issues (not defined in the {#Status} class).
   */
  public static final int UNPROCESSABLE_STATUS = 422;

  /**
   * The contents of the file split into lines.
   */
  private String[] fileLines = null;

  /**
   * Indicates whether or not the file should be stored.
   */
  private boolean store = true;

  /**
   * The extracted file.
   */
  private DataFile dataFile = null;

  /**
   * Messages for information or errors found while processing the file.
   */
  private ArrayList<FacesMessage> messages = new ArrayList<>();

  /**
   * Return status code for the uploaded file (for use with API calls).
   */
  private int statusCode = Status.OK.getStatusCode();

  /**
   * Indicates whether or not the file has been extracted and processed.
   */
  private boolean processed = false;

  /**
   * The database ID of the existing file that this file will replace -1
   * indicates that this is a completely new file.
   */
  private long replaceFile = -1;

  /**
   * Extract the file contents as individual lines.
   *
   * @return The file lines.
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
   * Get the filename of the file.
   *
   * @return The filename.
   */
  public abstract String getName();

  /**
   * Determine whether or not this file should be stored in the database.
   *
   * @return {@code true} if the file should be stored; {@code false} if not.
   */
  public boolean isStore() {
    return store;
  }

  /**
   * Specify whether or not this file should be stored in the database.
   *
   * @param store
   *          The store status.
   */
  public void setStore(boolean store) {
    this.store = store;
  }

  /**
   * Return the underlying {@link DataFile} object.
   *
   * @return The {@link DataFile} object.
   */
  public DataFile getDataFile() {
    return dataFile;
  }

  /**
   * Get the file's start point.
   *
   * <p>
   * The type of start point returned will depend on the type of
   * {@link DataFile} that has been uploaded, which in turn depends on the
   * {@link Instrument}'s measurement basis.
   * </p>
   *
   * <p>
   * Required for compatibility with PrimeFaces.
   * </p>
   *
   * @return The start point.
   * @throws DataFileException
   *           If the start point of the file cannot be determined.
   * @see DataFile#getStartDisplayString()
   * @see Instrument#basis
   */
  public String getStart() throws DataFileException {
    return null == dataFile ? null : dataFile.getStartDisplayString();
  }

  /**
   * Get the file's end point.
   *
   * <p>
   * The type of end point returned will depend on the type of {@link DataFile}
   * that has been uploaded, which in turn depends on the {@link Instrument}'s
   * measurement basis.
   * </p>
   *
   * <p>
   * Required for compatibility with PrimeFaces.
   * </p>
   *
   * @return The end point.
   * @throws DataFileException
   *           If the start point of the file cannot be determined.
   * @see DataFile#getEndDisplayString()
   * @see Instrument#basis
   */
  public String getEnd() throws DataFileException {
    return null == dataFile ? null : dataFile.getEndDisplayString();
  }

  /**
   * Add a processing message to the file.
   *
   * @param statusCode
   *          The HTTP status code for the message.
   * @param summary
   *          The message.
   * @param severityError
   *          The JavaFaces severity.
   */
  public void putMessage(int statusCode, String summary,
    Severity severityError) {
    messages.add(new FacesMessage(severityError, summary, ""));
    this.statusCode = statusCode;
    setStore(false);
  }

  /**
   * Get the {@link #messages} generated during processing as a JSON array.
   *
   * @return The messages in JSON format.
   */
  public String getMessages() {
    JsonArray jsonArray = new JsonArray();
    for (FacesMessage message : messages) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("summary", message.getSummary());
      jsonObject.addProperty("severity",
        getSeverityLabel(message.getSeverity()));
      jsonObject.addProperty("type", "file");
      jsonArray.add(jsonObject);
    }
    if (null != dataFile) {
      for (DataFileMessage message : dataFile.getMessages()) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("summary", message.toString());
        jsonObject.addProperty("severity",
          getSeverityLabel(FacesMessage.SEVERITY_INFO));
        jsonObject.addProperty("type", "row");
        jsonArray.add(jsonObject);
      }
    }
    return jsonArray.toString();
  }

  /**
   * Get a {@link String} representation of a JavaFaces {@link Severity}.
   *
   * @param severity
   *          The {@link Severity} object.
   * @return the string representation.
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
   * Determine whether or not this file has been extracted and processed.
   *
   * @return {@code true} if the file has been processed; {@code false} if it
   *         has not.
   */
  public boolean isProcessed() {
    return processed;
  }

  /**
   * Set the flag indicating whether or not the file has been processed.
   *
   * @param processed
   *          Whether or not the file has been processed.
   */
  public void setProcessed(boolean processed) {
    this.processed = processed;
  }

  /**
   * Determine whether or not any messages have been generated for this file.
   *
   * @return {@code true} if there are messages; {@code false} if there are
   *         none.
   */
  public boolean getHasMessages() {
    return null != messages && messages.size() > 0;
  }

  /**
   * Determine whether or not unrecognised Run Types have been detected in the
   * file.
   *
   * @return {@code true} if unrecognised Run Types have been found;
   *         {@code false} otherwise.
   */
  public boolean getHasUnrecognisedRunTypes() {

    if (null != dataFile && dataFile instanceof TimeDataFile) {
      return ((TimeDataFile) dataFile).getMissingRunTypes().size() > 0;
    } else {
      return false;
    }
  }

  /**
   * Get the unrecognised Run Types encountered in the file.
   *
   * @return The unrecognised Run Types.
   */
  public List<RunTypeAssignment> getMissingRunTypes() {
    if (null != dataFile && dataFile instanceof TimeDataFile) {
      return ((TimeDataFile) dataFile).getMissingRunTypes();
    } else {
      return new ArrayList<RunTypeAssignment>();
    }
  }

  /**
   * Determine whether or not this file will replace an existing file.
   *
   * @return {@code true} if this is a replacement file; {@code false} if it is
   *         a new file.
   */
  public boolean isReplacement() {
    return (replaceFile != -1);
  }

  /**
   * Set the database ID of the data file that this file will replace.
   *
   * @param oldId
   *          The ID of the file being replaced.
   */
  public void setReplacementFile(long oldId) {
    this.replaceFile = oldId;
  }

  /**
   * Get the ID of the file that this file will replace.
   *
   * @return The ID of the file being replaced.
   */
  public long getReplacementFile() {
    return replaceFile;
  }

  /**
   * Extract the file contents and ensure that it doesn't clash with existing
   * files.
   *
   * @param instrument
   *          The instrument to which the file belongs.
   * @param allowExactDuplicate
   *          Indicates whether exact duplicate files are accepted.
   * @param allowEmpty
   *          Indicates whether or not empty files are accepted.
   */
  public void extractFile(Instrument instrument, boolean allowExactDuplicate,
    boolean allowEmpty) {
    boolean fileEmpty = false;

    try {
      DataSource dataSource = ResourceManager.getInstance().getDBDataSource();

      InstrumentFileSet fileDefinitions = instrument.getFileDefinitions();
      List<String> lines = Arrays.asList(getLines());
      if (null == lines) {
        if (allowEmpty) {
          fileEmpty = true;
        } else {
          throw new DataFileException(DataFileException.NO_FILE_ID,
            DataFileException.NO_LINE_NUMBER, "File contains no data");
        }
      }

      if (!fileEmpty) {

        FileDefinition matchedDefinition = null;

        // TODO We're assuming we'll get one match. No matches will throw a
        // NoSuchElementException
        // (handled below), and multiple matches just choose the first one
        for (FileDefinition matchTest : fileDefinitions) {
          if (matchTest.fileMatches(lines)) {
            matchedDefinition = matchTest;
            break;
          }
        }

        if (null == matchedDefinition) {
          throw new NoSuchElementException();
        }

        dataFile = matchedDefinition.makeDataFile(instrument, getName(),
          new UploadedFileContents(this));

        if (getDataFile().getFirstDataLine() >= getDataFile()
          .getContentLineCount()) {
          if (allowEmpty) {
            fileEmpty = true;
          } else {
            throw new DataFileException(DataFileException.NO_FILE_ID,
              DataFileException.NO_LINE_NUMBER,
              "File contains headers but no data");
          }
        }

        if (fileEmpty) {
          putMessage(Status.OK.getStatusCode(),
            getName() + " is empty. File accepted but not processed",
            FacesMessage.SEVERITY_INFO);
        } else {
          if (dataFile.hasFundametalProcessingIssue()) {
            putMessage(UNPROCESSABLE_STATUS, getName() + " has "
              + dataFile.getFundamentalProcessingIssueItem()
              + " issues (see individual messages below). Please fix these problems and upload the file again.",
              FacesMessage.SEVERITY_ERROR);
          } else if (getDataFile().getMessageCount() > 0) {
            putMessage(UNPROCESSABLE_STATUS, getName()
              + " could not be processed (see messages below). Please fix these problems and upload the file again.",
              FacesMessage.SEVERITY_ERROR);
          } else {
            TreeSet<DataFile> overlappingFiles = getDataFile()
              .getOverlappingFiles(DataFileDB.getFiles(dataSource, instrument,
                getDataFile().getFileDefinition()));

            boolean fileOK = true;
            String fileMessage = null;
            int fileStatus = Status.OK.getStatusCode();

            if (overlappingFiles.size() > 0 && overlappingFiles.size() > 1) {
              fileOK = false;
              fileMessage = "This file overlaps the following file(s): ";
              fileMessage += overlappingFiles.stream()
                .map(f -> f.getFilename() + " ");

              fileStatus = Status.CONFLICT.getStatusCode();
            } else if (overlappingFiles.size() == 1) {
              DataFile existingFile = overlappingFiles.stream().findAny().get();
              DataFile newFile = getDataFile();

              if (!existingFile.getFilename().equals(newFile.getFilename())) {
                fileOK = false;
                fileMessage = "This file overlaps existing file "
                  + existingFile.getFilename();
                fileStatus = Status.CONFLICT.getStatusCode();
              } else {
                String oldContents = existingFile.getContentsAsString();
                String newContents = newFile.getContentsAsString();

                if (newContents.length() < oldContents.length()) {
                  fileOK = false;
                  fileMessage = "This file would replace existing file "
                    + existingFile.getFilename() + " with fewer records";
                  fileStatus = Status.CONFLICT.getStatusCode();
                } else if (!allowExactDuplicate
                  && newContents.length() == oldContents.length()) {
                  fileOK = false;
                  fileMessage = "This is an exact copy of existing file "
                    + existingFile.getFilename();
                  fileStatus = Status.CONFLICT.getStatusCode();
                } else {
                  String oldPartOfNewContents = newContents.substring(0,
                    oldContents.length());
                  if (!oldPartOfNewContents.equals(oldContents)) {
                    fileOK = false;
                    fileMessage = "This file would update existing file "
                      + existingFile.getFilename()
                      + " but change existing data";
                    fileStatus = Status.CONFLICT.getStatusCode();
                  } else {
                    setReplacementFile(existingFile.getDatabaseId());
                  }
                }
              }
            } else if (DataFileDB.hasFileWithName(dataSource,
              instrument.getId(), getName())) {

              // We don't allow duplicate filenames
              fileOK = false;
              fileMessage = "A file with this name already exists";
              fileStatus = Status.CONFLICT.getStatusCode();
            }

            if (fileOK) {
              dataFile.validate();
            } else {
              matchedDefinition = null;
              dataFile = null;
              putMessage(fileStatus, fileMessage, FacesMessage.SEVERITY_ERROR);
            }
          }
        }
      }
    } catch (NoSuchElementException nose) {
      dataFile = null;
      putMessage(Status.BAD_REQUEST.getStatusCode(),
        "The format of " + getName()
          + " was not recognised. Please upload a different file.",
        FacesMessage.SEVERITY_ERROR);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
      dataFile = null;
      putMessage(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
        "The file could not be processed: " + e.getMessage(),
        FacesMessage.SEVERITY_ERROR);
    }

    setProcessed(true);
  }

  /**
   * Get the HTTP response status code resulting from processing the file.
   *
   * @return The status code.
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Get the contents of the file as a {@link String}.
   *
   * @return The file contents.
   */
  protected String getFileContents() {
    return new String(getFileBytes(), StandardCharsets.UTF_8);
  }

  /**
   * Get the raw bytes of the file.
   *
   * @return The file bytes.
   */
  public abstract byte[] getFileBytes();

  @Override
  public int compareTo(UploadedDataFile o) {
    return getName().compareTo(o.getName());
  }
}
