package uk.ac.exeter.QuinCe.web.User;

import java.sql.Connection;

import org.apache.commons.validator.routines.EmailValidator;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.User.NoSuchUserException;
import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.database.User.UserExistsException;
import uk.ac.exeter.QuinCe.utils.MissingDataException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * JSF Managed Bean for handling new user sign-up
 * @author Steve Jones
 *
 */
public class SignupBean extends BaseManagedBean {

	public static final String USER_EXISTS_RESULT = "UserExists";
	
	private String emailAddress = null;
	
	private String givenName = null;
	
	private String surname = null;
	
	private String password1 = null;
	
	private String password2 = null;
	
	public SignupBean() {
		// Do nothing
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress.trim();
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName.trim();
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname.trim();
	}

	public String getPassword1() {
		return password1;
	}

	public void setPassword1(String password1) {
		// Note that passwords aren't trimmed
		this.password1 = password1;
	}

	public String getPassword2() {
		return password2;
	}

	public void setPassword2(String password2) {
		// Note that passwords aren't trimmed
		this.password2 = password2;
	}

	/**
	 * The main signup action method
	 * @return Result string
	 */
	public String signUp() {
		
		String result = SUCCESS_RESULT;
		
		if (!validate()) {
			result = VALIDATION_FAILED_RESULT;
		} else {
			
			try {
				Connection db = getDBConnection();
				
				// Add the user to the database
				User newUser = UserDB.createUser(db, emailAddress, password1.toCharArray(), givenName, surname);
				UserDB.generateEmailVerificationCode(db, newUser);
				
			} catch (DatabaseException|MissingDataException|NoSuchUserException e) {
				result = INTERNAL_ERROR_RESULT;
			} catch (UserExistsException e) {
				setMessage("signupForm:emailAddress", "A user already exists with that email address");
				result = USER_EXISTS_RESULT;
			}
		}
		
		return result;
	}
	
	/**
	 * Validate the bean's contents
	 * @return {@code true} if the contents are valid; {@code false} otherwise.
	 */
	private boolean validate() {
		boolean ok = true;
		
		// Email
		if (!EmailValidator.getInstance().isValid(emailAddress)) {
			ok = false;
			setMessage("signupForm:emailAddress", "Email is not valid");
		}
		
		// Passwords must match
		if (!password1.equals(password2)) {
			ok = false;
			setMessage("signupForm:password1", "Passwords must match");
			setMessage("signupForm:password2", "Passwords must match");
		}
		
		return ok;
	}
}
