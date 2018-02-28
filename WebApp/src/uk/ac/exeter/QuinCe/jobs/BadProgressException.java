package uk.ac.exeter.QuinCe.jobs;

/**
 * The progress of a job must be expressed as a percentage, between 0 and 100.
 * If the progress for a job is set to anything outside this range, this
 * exception will be thrown.
 *
 * @author Steve Jones
 *
 */
public class BadProgressException extends Exception {

  /**
   * The Serial Version UID
   */
  private static final long serialVersionUID = 8691544843626836324L;

  /**
   * The exception constructor.
   * Creates a basic exception with a simple message.
   */
  public BadProgressException() {
    super("The progress must be between 0 and 100");
  }
}
