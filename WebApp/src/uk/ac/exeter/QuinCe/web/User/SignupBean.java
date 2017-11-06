package uk.ac.exeter.QuinCe.web.User;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.validator.routines.EmailValidator;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.User.UserExistsException;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.user.SendEmailVerificationMailJob;
import uk.ac.exeter.QuinCe.utils.StringFormatException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

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
	 * @throws StringFormatException 
	 * @throws SecurityException 
	 */
	public String signUp() throws SecurityException, StringFormatException {
		
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
