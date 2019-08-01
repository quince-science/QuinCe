package uk.ac.exeter.QuinCe.web.files;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
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
  public static final String NAV_FILE_LIST = "file_list";

  @Override
  public void processUploadedFile() {
  }

  /**
   * Initialise/reset the bean
   */
  @PostConstruct
  public void initialise() {
    initialiseInstruments();
  }

  /**
   * Return to the file list
   * @return Navigation to the file list
   */
  public String goToFileList() {
    return NAV_FILE_LIST;
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
