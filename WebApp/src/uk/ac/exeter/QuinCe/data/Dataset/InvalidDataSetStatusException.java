package uk.ac.exeter.QuinCe.data.Dataset;

/**
 * Exception for invalid data set status
 * 
 * @author Steve Jones
 */
public class InvalidDataSetStatusException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -4323030986300023229L;

  /**
   * Simple constructor
   * 
   * @param status
   *          The invalid status value
   */
  public InvalidDataSetStatusException(int status) {
    super("Unrecognised data set status value: " + status);
  }

}
