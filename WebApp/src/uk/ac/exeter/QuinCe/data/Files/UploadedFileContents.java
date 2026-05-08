package uk.ac.exeter.QuinCe.data.Files;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.NotImplementedException;

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
    /*
     * This is not implemented because the contents of an uploaded file can only
     * be read once. Luckily it's not needed - we only read bytes when exporting
     * a DataSet, and that doesn't happen with uploaded files.
     *
     * Note that we could fix this by reading the bytes in the UploadedDataFile
     * object and building the fileLines array from that, but that would double
     * the RAM consumption so while it's not needed there's no point.
     */
    throw new NotImplementedException();
  }

  @Override
  protected void loadAction() throws DataFileException {
    contents = Arrays.asList(source.getLines());
  }
}
