package uk.ac.exeter.QuinCe.jobs;

/**
 * Exception thrown when an attempt is made to access the
 * {@link JobThreadPool} before it has been initialised.
 * @author Steve Jones
 * @see JobThreadPool#initialise(int)
 */
public class JobThreadPoolNotInitialisedException extends Exception {

  /**
   * The Serial Version UID
   */
  private static final long serialVersionUID = -2087913958516852214L;

  /**
   * Constructor
   */
  public JobThreadPoolNotInitialisedException() {
    super("The job thread pool has not been initialised");
  }
}
