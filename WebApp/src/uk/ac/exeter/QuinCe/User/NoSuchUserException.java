package uk.ac.exeter.QuinCe.User;

/**
 * An exception to indicate that an attempt to
 * locate a user's database record has failed.
 * 
 * @author Steve Jones
 *
 */
public class NoSuchUserException extends Exception {

	private static final long serialVersionUID = 5900018443079901207L;

	public NoSuchUserException() {
		super("The specified user does not exist in the database");
	}
	
	public NoSuchUserException(User user) {
		super("The user '" + user.getEmailAddress() + "' does not exist in the database");
	}
	
	public NoSuchUserException(String email) {
		super("The user '" + email + "' does not exist in the database");
	}
}
