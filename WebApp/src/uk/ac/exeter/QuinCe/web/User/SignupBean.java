package uk.ac.exeter.QuinCe.web.User;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.User.UserExistsException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.user.SendEmailVerificationMailJob;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * JSF Managed Bean for handling new user sign-up
 * @author Steve Jones
 *
 */
public class SignupBean extends BaseManagedBean {
	
	/**
	 * Navigation result for when a user already exists
	 */
	public static final String USER_EXISTS_RESULT = "UserExists";
	
	/**
	 * The user's email address
	 */
	private String emailAddress = null;
	
	/**
	 * The user's given name
	 */
	private String givenName = null;
	
	/**
	 * The user's surname
	 */
	private String surname = null;
	
	/**
	 * The first entered password
	 */
	private String password1 = null;
	
	/**
	 * The repeated password
	 */
	private String password2 = null;
	
	/**
	 * Empty constructor
	 */
	public SignupBean() {
		// Do nothing
	}

	/**
	 * Get the email address
	 * @return The email address
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * Set the email address
	 * @param emailAddress The email address
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress.trim();
	}

	/**
	 * Get the user's given name
	 * @return The given name
	 */
	public String getGivenName() {
		return givenName;
	}

	/**
	 * Set the user's given name
	 * @param givenName The given name
	 */
	public void setGivenName(String givenName) {
		this.givenName = givenName.trim();
	}

	/**
	 * Get the user's surname
	 * @return The surname
	 */
	public String getSurname() {
		return surname;
	}

	/**
	 * Set the user's surname
	 * @param surname The surname
	 */
	public void setSurname(String surname) {
		this.surname = surname.trim();
	}

	/**
	 * Get the first password
	 * @return The first password
	 */
	public String getPassword1() {
		return password1;
	}

	/**
	 * Set the first password
	 * @param password1 The first password
	 */
	public void setPassword1(String password1) {
		// Note that passwords aren't trimmed
		this.password1 = password1;
	}

	/**
	 * Get the second password
	 * @return The second password
	 */
	public String getPassword2() {
		return password2;
	}

	/**
	 * Set the second password
	 * @param password2 The second password
	 */
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
				// Add the user to the database
				User newUser = UserDB.createUser(ServletUtils.getDBDataSource(), emailAddress, password1.toCharArray(), givenName, surname, true);

				// Build and start the job to send out the verification email
				Map<String, String> emailJobParams = new HashMap<String, String>(1);
				emailJobParams.put(SendEmailVerificationMailJob.EMAIL_KEY, emailAddress);
				
				JobManager.addInstantJob(ServletUtils.getResourceManager(), ServletUtils.getAppConfig(), newUser, "uk.ac.exeter.QuinCe.jobs.user.SendEmailVerificationMailJob", emailJobParams);
			} catch (UserExistsException e) {
				setMessage(getComponentID("emailAddress"), "A user already exists with that email address");
				result = USER_EXISTS_RESULT;
			} catch (Exception e) {
				result = internalError(e);
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
			setMessage(getComponentID("emailAddress"), "Email is not valid");
		}
		
		// Passwords must match
		if (!password1.equals(password2)) {
			ok = false;
			setMessage(getComponentID("password1"), "Passwords must match");
			setMessage(getComponentID("password2"), "Passwords must match");
		}
		
		return ok;
	}

	@Override
	public String getFormName() {
		return "signupForm";
	}

}
