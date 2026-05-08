package uk.ac.exeter.QuinCe.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Extended version of the {@link BaseManagedBean} that includes file upload
 * handling.
 *
 * @see BaseManagedBean
 */
public abstract class FileUploadBean extends BaseManagedBean {

  /**
   * The uploaded file
   */
  protected UploadedFile file = null;

  /**
   * Handle the file upload and subsequent processing.
   *
   * @param event
   *          The file upload event
   */
  public final void handleFileUpload(FileUploadEvent event) {
    setFile(event.getFile());
    processUploadedFile();
  }

  /**
   * Process the uploaded file
   */
  public abstract void processUploadedFile();

  /**
   * Retrieve the uploaded file
   *
   * @return The uploaded file
   */
  public UploadedFile getFile() {
    return file;
  }

  /**
   * Set the uploaded file
   *
   * @param file
   *          The uploaded file
   */
  public void setFile(UploadedFile file) {
    this.file = file;
  }

  /**
   * Remove any existing uploaded file
   */
  public void clearFile() throws FileUploadException {
    this.file = null;
  }

  /**
   * @return the file lines in the uploaded file as strings
   */
  public List<String> getFileLines() {
    if (null == getFile()) {
      return Collections.emptyList();
    }
    String fileContent = new String(getFile().getContent(),
      StandardCharsets.UTF_8);
    List<String> fileLines = new ArrayList<String>(
      Arrays.asList(fileContent.split("[\\r\\n]+")));

    StringUtils.removeBlankTailLines(fileLines);

    return fileLines;
  }

  /**
   * Get the name of the uploaded file
   *
   * @return The filename
   */
  public String getFilename() {
    String result = null;

    if (null != file) {
      result = file.getFileName();
    }

    return result;
  }
}
