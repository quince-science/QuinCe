package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception raised for an unrecognised job status
 * 
 * @author Steve Jones
 *
 */
public class UnrecognisedStatusException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = -5124308472992974683L;

  /**
   * Basic constructor
   * 
   * @param status
   *          The unrecognised status
   */
  public UnrecognisedStatusException(String status) {
    super("The status '" + status + "' is not recognised");
  }

}
