package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.util.Arrays;

import uk.ac.exeter.QuinCe.web.files.UploadedDataFile;

/**
 * Contents for an uploaded file.
 */
public class UploadedFileContents extends FileContents {

  private UploadedDataFile source;

  public UploadedFileContents(UploadedDataFile source) {
    this.source = source;
  }

  @Override
  public byte[] getBytes() throws IOException {
    return source.getFileBytes();
  }

  @Override
  protected void loadAction() throws DataFileException {
    contents = Arrays.asList(source.getLines());
  }
}
