package uk.ac.exeter.QuinCe.User;

/**
 * An exception to indicate that an attempt to
 * locate a user's database record has failed.
 *
 * @author Steve Jones
 *
 */
public class NoSuchUserException extends Exception {

  /**
   * The serial version UID
   */
  private static final long serialVersionUID = 5900018443079901207L;

  /**
   * Constructor for an error raised from a {@link User} object
   * @param user The user
   */
  public NoSuchUserException(User user) {
    super("The user '" + user.getEmailAddress() + "' does not exist in the database");
  }

  /**
   * Exception for an error raised from an email address
   * @param email The email address
   */
  public NoSuchUserException(String email) {
    super("The user '" + email + "' does not exist in the database");
  }
}
