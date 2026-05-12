package uk.ac.exeter.QuinCe.api.nrt;

import java.io.IOException;
import java.io.InputStream;

import uk.ac.exeter.QuinCe.web.files.UploadedDataFile;

/**
 * {@link UploadedDataFile} instance for use with simple Strings
 */
public class APIUploadedDataFile extends UploadedDataFile {

  /**
   * The filename
   */
  private String filename;

  /**
   * The file contents
   */
  private byte[] contents;

  /**
   * Constructor to build a file from an {@link InputStream}. Assumes data is in
   * UTF-8.
   *
   * Note that the InputStream is not closed by this constructor.
   *
   * @param filename
   *          The filename.
   * @param inputStream
   *          The input stream.
   * @throws IOException
   *           If the {@link InputStream} contents cannot be read.
   */
  protected APIUploadedDataFile(String filename, InputStream inputStream)
    throws IOException {
    super();
    this.filename = filename;
    this.contents = inputStream.readAllBytes();
  }

  @Override
  public String getName() {
    return filename;
  }

  @Override
  public byte[] getFileBytes() {
    return contents;
  }
}
