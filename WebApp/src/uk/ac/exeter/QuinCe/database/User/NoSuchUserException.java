package uk.ac.exeter.QuinCe.database.User;

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
		super("The specified user database ID does not exist");
	}
}
