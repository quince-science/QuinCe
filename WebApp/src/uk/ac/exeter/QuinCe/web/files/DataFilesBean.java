package uk.ac.exeter.QuinCe.web.files;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileMessage;
import uk.ac.exeter.QuinCe.data.Files.FileExistsException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.Instrument.newInstrument.FileDefinitionBuilder;
import uk.ac.exeter.QuinCe.web.system.ResourceException;

/**
 * Bean for handling raw data files
 * @author Steve Jones
 */
@ManagedBean
@SessionScoped
public class DataFilesBean extends FileUploadBean {

  /**
   * Navigation to the file upload page
   */
  @Deprecated
  public static final String NAV_UPLOAD = "upload";

  /**
   * Navigation to the file upload page
   */
  public static final String NAV_FILE_LIST = "file_list";

  /**
   * The data file object
   */
  @Deprecated
  private DataFile dataFile = null;

  /**
   * The file definitions that match the uploaded file
   */
  @Deprecated
  private List<FileDefinition> matchedFileDefinitions = null;

  @Override
  public void processUploadedFile() {
  }

  /**
   * Initialise/reset the bean
   */
  @PostConstruct
  public void initialise() {
    initialiseInstruments();
    matchedFileDefinitions = null;
    dataFile = null;
  }

  /**
   * Start the file upload procedure
   * @return Navigation to the upload page
   */
  @Deprecated
  public String beginUpload() {
    initialise();
    return NAV_UPLOAD;
  }

  /**
   * Extract and process the uploaded file's contents
   */
  @Deprecated
  public void extractFile() {
    matchedFileDefinitions = null;
    dataFile = null;

    try {
      FileDefinitionBuilder guessedFileLayout = new FileDefinitionBuilder(getCurrentInstrument().getFileDefinitions());
      List<String> fileLines = getFileLines();
      guessedFileLayout.setFileContents(fileLines);
      guessedFileLayout.guessFileLayout();

      matchedFileDefinitions = getCurrentInstrument().getFileDefinitions().getMatchingFileDefinition(guessedFileLayout);
      FileDefinition fileDefinition = null;

      if (matchedFileDefinitions.size() == 0) {
        fileDefinition = null;
        setMessage(null, "The format of " + getFilename() + " was not recognised. Please upload a different file.");
      } else {
        fileDefinition = matchedFileDefinitions.get(0);
      }
      // TODO Handle multiple matched definitions

      if (null != fileDefinition) {
        dataFile = new DataFile(getAppConfig().getProperty("filestore"), fileDefinition, getFilename(), fileLines);

        if (dataFile.getMessageCount() > 0) {
          setMessage(null, getFilename() + " could not be processed (see messages below). Please fix these problems and upload the file again.");
        }

        if (DataFileDB.fileExistsWithDates(getDataSource(), fileDefinition.getDatabaseId(), dataFile.getStartDate(), dataFile.getEndDate())) {
          // TODO This is what the front end uses to detect that the file was not processed successfully.
          //This can be improved when overlapping files are implemented instead of being rejected.
          fileDefinition = null;
          dataFile = null;
          setMessage(null, "A file already exists that covers overlaps with this file. Please upload a different file.");
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      dataFile = null;
      setMessage(null, "The file could not be processed: " + e.getMessage());
    }
  }

  /**
   * Set the file definition for the uploaded file
   * @param fileDescription The file description
   * @throws FileDefinitionException If the file definition does not match the file contents
   */
  @Deprecated
  public void setFileDefinition(String fileDescription) throws FileDefinitionException {
    dataFile.setFileDefinition(getCurrentInstrument().getFileDefinitions().get(fileDescription));
  }

  /**
   * Get the list of file definitions that match the uploaded file
   * @return The matched file definitions
   */
  @Deprecated
  public List<FileDefinition> getMatchedFileDefinitions() {
    return matchedFileDefinitions;
  }

  /**
   * Return to the file list
   * @return Navigation to the file list
   */
  public String goToFileList() {
    return NAV_FILE_LIST;
  }

  /**
   * Get the messages generated for this file as a JSON string
   * @return The messages in JSON format
   */
  @Deprecated
  public String getFileMessages() {
    StringBuilder json = new StringBuilder();

    json.append('[');

    if (null != dataFile) {

      TreeSet<DataFileMessage> messages = dataFile.getMessages();

      int count = 0;
      for (DataFileMessage message : messages) {
        json.append('"');
        json.append(message.toString());
        json.append('"');

        if (count < messages.size() - 1) {
          json.append(',');
        }

        count++;
      }
    }

    json.append(']');

    return json.toString();
  }

  /**
   * Get the file format description
   * @return The file format description
   */
  @Deprecated
  public String getFileType() {
    String result = null;

    if (null != dataFile) {
      result = dataFile.getFileDescription();
    }

    return result;
  }

  /**
   * Get the date of the first record in the file
   * @return The start date
   */
  @Deprecated
  public LocalDateTime getFileStartDate() {
    LocalDateTime result = null;

    if (dataFile != null) {
      result = dataFile.getStartDate();
    }

    return result;
  }

  /**
   * Get the date of the last record in the file
   * @return The end date
   * @throws DataFileException If the end date cannot be retrieved
   */
  @Deprecated
  public LocalDateTime getFileEndDate() throws DataFileException {
    LocalDateTime result = null;

    if (dataFile != null) {
      result = dataFile.getEndDate();
    }

    return result;
  }

  /**
   * Get the number of records in the file
   * @return The record count
   * @throws DataFileException If the count cannot be calculated
   */
  @Deprecated
  public int getFileRecordCount() throws DataFileException {
    int result = -1;

    if (dataFile != null) {
      result = dataFile.getRecordCount();
    }

    return result;
  }

  /**
   * Dummy method for (not) setting file messages
   * @param dummy Parameter
   */
  @Deprecated
  public void setFileMessages(String dummy) {
    // Do nothing
  }

  /**
   * Dummy method for (not) setting file messages
   * @param dummy Parameter
   */
  @Deprecated
  public void setFileType(String dummy) {
    // Do nothing
  }

  /**
   * Dummy method for (not) setting file messages
   * @param dummy Parameter
   */
  @Deprecated
  public void setFileStartDate(LocalDateTime dummy) {
    // Do nothing
  }

  /**
   * Dummy method for (not) setting file messages
   * @param dummy Parameter
   */
  @Deprecated
  public void setFileEndDate(LocalDateTime dummy) {
    // Do nothing
  }

  /**
   * Dummy method for (not) setting file messages
   * @param dummy Parameter
   */
  @Deprecated
  public void setFileRecordCount(String dummy) {
    // Do nothing
  }

  /**
   * Store the uploaded file
   * @return Navigation to the file list
   * @throws MissingParamException If any internal calls are missing required parameters
   * @throws FileExistsException If the file already exists
   * @throws DatabaseException If a database error occurs
   */
  @Deprecated
  public String storeFile() throws MissingParamException, FileExistsException, DatabaseException {
    throw new RuntimeException("This method is deprecated");
    //DataFileDB.storeFile(getDataSource(), getAppConfig(), dataFile);
    //return NAV_FILE_LIST;
  }

  /**
   * Get the files to be displayed in the file list
   * @return The files
   * @throws DatabaseException If the file list cannot be retrieved
   * @throws ResourceException If the app resources cannot be accessed
   * @throws InstrumentException If the instrument data is invalid
   * @throws RecordNotFoundException If the instrument cannot be found
   * @throws MissingParamException If any internal calls have missing parameters
   */
  public List<DataFile> getListFiles() throws DatabaseException, MissingParamException, RecordNotFoundException, InstrumentException, ResourceException {

    List<DataFile> result;

    if (null != getCurrentInstrument()) {
      result = DataFileDB.getFiles(getDataSource(), getAppConfig(), getCurrentInstrument().getDatabaseId());
    } else {
      result = new ArrayList<DataFile>();
    }

    return result;
  }
}
