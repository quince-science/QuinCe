package uk.ac.exeter.QuinCe.utils;

/**
 * Exception raised when database records cannot be found
 * 
 * @author Steve Jones
 *
 */
public class RecordNotFoundException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -6955543918647293212L;

  /**
   * Basic constructor
   * 
   * @param message
   *          The error messages
   */
  public RecordNotFoundException(String message) {
    super(message);
  }

  /**
   * Constructor for a named table and record ID
   * 
   * @param message
   *          The error message
   * @param table
   *          The database table that was being searched
   * @param id
   *          The record ID that could not be found
   */
  public RecordNotFoundException(String message, String table, long id) {
    super(message + "(Table " + table + ", record " + id);
  }

}
