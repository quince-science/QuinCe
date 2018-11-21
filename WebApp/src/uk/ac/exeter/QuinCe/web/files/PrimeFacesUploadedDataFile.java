package uk.ac.exeter.QuinCe.web.files;

import java.nio.charset.StandardCharsets;

import org.primefaces.model.UploadedFile;

public class PrimeFacesUploadedDataFile extends UploadedDataFile {

  /**
   * The uploaded file
   */
  private UploadedFile uploadedFile;

  /**
   * Set up the object and load the file contents
   * @param uploadedFile The uploaded file
   */
  public PrimeFacesUploadedDataFile(UploadedFile uploadedFile) {
    super();
    this.uploadedFile = uploadedFile;

    // PrimeFaces uploaded files tend to disappear from the
    // file system quite quickly, so extract the contents immediately
    getLines();
  }

  protected UploadedFile getUploadedFile() {
    return uploadedFile;
  }

  @Override
  protected String getFileContents() {
    return new String(uploadedFile.getContents(), StandardCharsets.UTF_8);
  }

  @Override
  public String getName() {
    return uploadedFile.getFileName();
  }
}
