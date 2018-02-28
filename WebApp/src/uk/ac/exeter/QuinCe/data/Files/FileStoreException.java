package uk.ac.exeter.QuinCe.data.Files;

/**
 * An exception class for errors in the on-disk data file store
 * @author Steve Jones
 *
 */
public class FileStoreException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -269122182568751400L;

  /**
   * Constructs a new exception with the specified detail message.
   * @param message The message
   */
  public FileStoreException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   * @param message The message
   * @param cause The underlying cause
   */
  public FileStoreException(String message, Throwable cause) {
    super(message, cause);
  }
}
