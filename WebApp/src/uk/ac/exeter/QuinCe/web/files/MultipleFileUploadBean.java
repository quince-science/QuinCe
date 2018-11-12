package uk.ac.exeter.QuinCe.web.files;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.UploadedFile;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.FileExistsException;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.Instrument.newInstrument.FileDefinitionBuilder;

@ManagedBean(name="fileUpload")
@ViewScoped
public class MultipleFileUploadBean extends FileUploadBean {
  /**
   * The data file object
   */
  private ArrayList<UploadedDataFile> dataFiles = new ArrayList<>();
  private String displayClass = "hidden";

  /**
   * Initialize resources
   */
  @PostConstruct
  public void init() {
    initialiseInstruments();
  }

  @Override
  public void processUploadedFile() {
    processUploadedFile(getFile());
  }

  public void processUploadedFile(UploadedFile uploadedFile) {
    UploadedDataFile uploadedDataFile = new UploadedDataFile(uploadedFile);
    dataFiles.add(uploadedDataFile);
    setDisplayClass("");
  }
  public List<UploadedDataFile> getUploadedFiles() {
    return dataFiles;
  }

  /**
   * Extract files in file list that are not yet extracted
   */
  public void extractNext() {
    for (UploadedDataFile file: dataFiles) {
      if (file.getDataFile() == null && file.isStore()) {
        extractFile(file);
        return;
      }
    }
  }

  /**
   * Store selected files. This moves the file(s) to the file store, and updates the database with file
   * info.
   * @throws MissingParamException If any required parameters are missing
   * @throws FileExistsException If a new file attempts to replace an existing one
   * @throws DatabaseException If a database error occurs
   * @throws RecordNotFoundException If a replacement file does not already exist in the database
   */
  public void store() throws MissingParamException, FileExistsException, DatabaseException, RecordNotFoundException {
    for (UploadedDataFile file: dataFiles) {
      if (file.isStore() && null != file.getDataFile()) {
        DataFileDB.storeFile(getDataSource(), getAppConfig(), file.getDataFile(), file.getReplacementFile());
      }
    }
  }

  /**
   * Extract and process the uploaded file's contents
   */
  public void extractFile(UploadedDataFile file) {
    try {
      FileDefinitionBuilder guessedFileLayout = new FileDefinitionBuilder(getCurrentInstrument().getFileDefinitions());
      String[] lines = file.getLines();
      if (null == lines) {
        throw new DataFileException("File contains no data");
      }
      guessedFileLayout.setFileContents(Arrays.asList(lines));
      guessedFileLayout.guessFileLayout();
      FileDefinition fileDefinition = getCurrentInstrument().getFileDefinitions()
          .getMatchingFileDefinition(guessedFileLayout).iterator().next();
      // TODO Handle multiple matched definitions

      file.setDataFile(new DataFile(
          getAppConfig().getProperty("filestore"),
          fileDefinition,
          file.getName(),
          Arrays.asList(lines)
      ));
      if (file.getDataFile().getFirstDataLine() >= file.getDataFile()
          .getContentLineCount()) {
        throw new DataFileException("File contains headers but no data");
      }

      if (null == file.getDataFile().getStartDate()
          || null == file.getDataFile().getEndDate()) {
        file.putMessage(file.getName()
            + " has date issues, see messages below. Please fix these problems and upload the file again.",
            FacesMessage.SEVERITY_ERROR);
      } else if (file.getDataFile().getMessageCount() > 0) {
        file.putMessage(file.getName()
            + " could not be processed (see messages below). Please fix these problems and upload the file again.",
            FacesMessage.SEVERITY_ERROR);
      } else {
        List<DataFile> overlappingFiles = DataFileDB.getFilesWithinDates(getDataSource(), fileDefinition,
            file.getDataFile().getStartDate(), file.getDataFile().getEndDate());

        boolean fileOK = true;
        String fileMessage = null;

        if (overlappingFiles.size() > 0 && overlappingFiles.size() > 1) {
          fileOK = false;
          fileMessage = "This file overlaps one or more existing files";
        } else if (overlappingFiles.size() == 1) {
          DataFile existingFile = overlappingFiles.get(0);
          DataFile newFile = file.getDataFile();

          if (!existingFile.getFilename().equals(newFile.getFilename())) {
            fileOK = false;
            fileMessage = "This file overlaps an existing file with a different name";
          } else {
            String oldContents = existingFile.getContents();
            String newContents = newFile.getContents();

            if (newContents.length() <= oldContents.length()) {
              fileOK = false;
              fileMessage = "This file would replace an existing file with identical or fewer records";
            } else {
              String oldPartOfNewContents = newContents.substring(0, oldContents.length());
              if (!oldPartOfNewContents.equals(oldContents)) {
                fileOK = false;
                fileMessage = "This file would update an existing file but change existing data";
              } else {
                file.setReplacementFile(existingFile.getDatabaseId());
              }
            }
          }
        } else if (DataFileDB.hasFileWithName(getDataSource(), getCurrentInstrument().getDatabaseId(),
            file.getName())) {

          // We don't allow duplicate filenames
          fileOK = false;
          fileMessage = "A file with that name already exists";
        }

        if (!fileOK) {
          fileDefinition = null;
          file.setDataFile(null);
          file.putMessage(fileMessage, FacesMessage.SEVERITY_ERROR);
        }
      }
    } catch (NoSuchElementException nose) {
      file.setDataFile(null);
      file.putMessage("The format of " + file.getName() + " was not recognised. Please upload a different file.", FacesMessage.SEVERITY_ERROR);
    } catch (Exception e) {
      e.printStackTrace();
      file.setDataFile(null);
      file.putMessage("The file could not be processed: " + e.getMessage(), FacesMessage.SEVERITY_ERROR);
    }

    file.setProcessed(true);
  }

  /**
   * @return the displayClass
   */
  public String getDisplayClass() {
    return displayClass;
  }

  /**
   * @param displayClass the displayClass to set
   */
  public void setDisplayClass(String displayClass) {
    this.displayClass = displayClass;
  }

  /**
   * @return the class "hidden" if there are no datafiles yet. Otherwise returns an empty string.
   */
  public String getStoreFileButtonClass() {
    return dataFiles.size()>0 ? "" : "hidden";
  }

  /**
   * Called when run types have been updated. This will initiate
   * re-processing of the uploaded files.
   */
  public void updateRunTypes(int fileIndex) {
    DataFile dataFile = dataFiles.get(fileIndex).getDataFile();

    try {
      InstrumentDB.storeFileRunTypes(
          getDataSource(),
          dataFile.getFileDefinition().getDatabaseId(),
          dataFile.getMissingRunTypes());
    } catch (Exception e) {
      e.printStackTrace();
    }

    unsetDataFiles();
  }

  private void unsetDataFiles() {
    // Initialize instruments with new run types
    setForceInstrumentReload(true);
    initialiseInstruments();
    List<UploadedDataFile> tmplist = dataFiles;
    dataFiles = new ArrayList<>();
    for (UploadedDataFile file: tmplist) {
      processUploadedFile(file.getUploadedFile());
    }
  }
}
