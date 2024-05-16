package uk.ac.exeter.QuinCe.web.files;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.file.UploadedFile;

import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Files.FileExistsException;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.RunTypes.RunTypeAssignment;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.ExceptionUtils;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;

@ManagedBean(name = "fileUpload")
@ViewScoped
public class MultipleFileUploadBean extends FileUploadBean {
  /**
   * The data file object
   */
  private TreeSet<UploadedDataFile> dataFiles = new TreeSet<UploadedDataFile>();

  private List<MissingRunType> unrecognisedRunTypes;

  @Override
  public void processUploadedFile() {
    processUploadedFile(getFile());
  }

  public synchronized void processUploadedFile(UploadedFile uploadedFile) {
    UploadedDataFile uploadedDataFile = new PrimeFacesUploadedDataFile(
      uploadedFile);
    dataFiles.add(uploadedDataFile);
  }

  public TreeSet<UploadedDataFile> getUploadedFiles() {
    return dataFiles;
  }

  public List<RunTypeAssignment> getAllRunTypes() {
    return getCurrentInstrument().getFileDefinitions().stream()
      .filter(fd -> fd.getRunTypes() != null)
      .map(fd -> fd.getRunTypes().values()).flatMap(Collection::stream)
      .distinct().collect(Collectors.toList());
  }

  public List<RunTypeAssignment> getAllRunTypesWithExclusion(String exclusion) {
    return getCurrentInstrument().getFileDefinitions().stream()
      .map(df -> df.getRunTypes().values()).flatMap(Collection::stream)
      .distinct().collect(Collectors.toList());
  }

  /**
   * (Re-)extract all files in the file list.
   */
  public void processAllFiles() {
    progress.setMax(dataFiles.size());
    progress.setValue(0);
    dataFiles.forEach(f -> f.setProcessed(false));
    for (UploadedDataFile file : dataFiles) {
      file.extractFile(getCurrentInstrument(), getAppConfig(), false, false);
      progress.increment();
    }

    buildUnrecognisedRunTypes();
  }

  /**
   * Store selected files. This moves the file(s) to the file store, and updates
   * the database with file info.
   *
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws FileExistsException
   *           If a new file attempts to replace an existing one
   * @throws DatabaseException
   *           If a database error occurs
   * @throws RecordNotFoundException
   *           If a replacement file does not already exist in the database
   */
  public void store() throws MissingParamException, FileExistsException,
    DatabaseException, RecordNotFoundException {
    for (UploadedDataFile file : dataFiles) {
      if (file.isStore() && null != file.getDataFile()) {
        DataFileDB.storeFile(getDataSource(), getAppConfig(),
          file.getDataFile(), file.getReplacementFile());
      }
    }
  }

  /**
   * @return the class "hidden" if there are no datafiles yet. Otherwise returns
   *         an empty string.
   */
  public String getStoreFileButtonClass() {
    return dataFiles.size() > 0 ? "" : "hidden";
  }

  /**
   * Called when run types have been updated. This will initiate re-processing
   * of the uploaded files.
   */
  public void updateRunTypes() {
    try {
      InstrumentDB.storeFileRunTypes(getDataSource(), unrecognisedRunTypes);
    } catch (Exception e) {
      ExceptionUtils.printStackTrace(e);
    }
    unsetDataFiles();
  }

  private void unsetDataFiles() {
    // Initialize instruments with new run types
    setForceInstrumentReload(true);
    initialiseInstruments();
    TreeSet<UploadedDataFile> tmplist = dataFiles;
    dataFiles = new TreeSet<UploadedDataFile>();
    for (UploadedDataFile file : tmplist) {
      processUploadedFile(
        ((PrimeFacesUploadedDataFile) file).getUploadedFile());
    }
  }

  private void buildUnrecognisedRunTypes() {
    unrecognisedRunTypes = new ArrayList<MissingRunType>();

    for (UploadedDataFile file : dataFiles) {
      for (RunTypeAssignment runType : file.getMissingRunTypes()) {
        if (!MissingRunType.contains(unrecognisedRunTypes,
          file.getDataFile().getFileDefinition(), runType.getRunName())) {
          unrecognisedRunTypes.add(new MissingRunType(
            file.getDataFile().getFileDefinition(), runType));
        }
      }
    }
  }

  public List<MissingRunType> getUnrecognisedRunTypes() {

    if (null == unrecognisedRunTypes) {
      buildUnrecognisedRunTypes();
    }

    return unrecognisedRunTypes;
  }

  public int getUnrecognisedRunTypeCount() {
    return null == unrecognisedRunTypes ? 0 : unrecognisedRunTypes.size();
  }

  public List<String> getRunTypeValuesWithExclusion(String exclusion) {
    return getCurrentInstrument().getFileDefinitions().stream()
      .map(fd -> fd.getRunTypeValues()).flatMap(Collection::stream).distinct()
      .filter(r -> !r.equals(exclusion)).toList();
  }
}
