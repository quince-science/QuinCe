package uk.ac.exeter.QuinCe.api.nrt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import uk.ac.exeter.QuinCe.web.files.UploadedDataFile;

/**
 * {#UploadedDataFile} instance for use with simple Strings
 * 
 * @author Steve Jones
 *
 */
public class APIUploadedDataFile extends UploadedDataFile {

  /**
   * The filename
   */
  private String filename;

  /**
   * The file contents
   */
  private String contents;

  /**
   * Basic constructor for pre-prepared strings
   * 
   * @param filename
   *          The filename
   * @param contents
   *          The file contents
   */
  protected APIUploadedDataFile(String filename, String contents) {
    super();
    this.filename = filename;
    this.contents = contents;
  }

  /**
   * Constructor to build a file from an {#InputStream}. Assumes data is in
   * UTF-8.
   *
   * Note that the InputStream is not closed by this constructor.
   * 
   * @param filename
   *          The filename
   * @param inputStream
   *          The input stream
   */
  protected APIUploadedDataFile(String filename, InputStream inputStream)
    throws IOException {
    super();
    this.filename = filename;
    this.contents = IOUtils.toString(inputStream,
      StandardCharsets.UTF_8.displayName());
  }

  @Override
  public String getName() {
    return filename;
  }

  @Override
  protected String getFileContents() {
    return contents;
  }
}
