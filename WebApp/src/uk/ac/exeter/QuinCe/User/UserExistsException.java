package uk.ac.exeter.QuinCe.User;

/**
 * An exception that indicates an attempt to add a user
 * whose email already exists in the database.
 * @author Steve Jones
 *
 */
public class UserExistsException extends Exception {
	
	/**
	 *  The serial version UID
	 */
	private static final long serialVersionUID = -3238441043465843608L;

	/**
	 * Basic constructor. There is no real information to be added to this exception
	 */
	public UserExistsException() {
		super("A user already exists with the specified email address");
	}
}
