package uk.ac.exeter.QuinCe.web.User;

import uk.ac.exeter.QuinCe.database.User.UserDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * JSF Managed Bean for handling user logins
 * @author Steve Jones
 *
 */
public class LoginBean extends BaseManagedBean {
	
	public static final String SIGNUP_RESULT = "SignUp";
	
	public static final String AUTHENTICATION_FAILED_RESULT = "AuthenticationFailed";
	
	public static final String AUTHENTICATION_OK_RESULT = "AuthenticationSuccess";
	
	protected static String FORM_NAME = "loginform";
	
	private String emailAddress = null;
	
	private String password = null;
	
	public LoginBean() {
		// Do nothing
	}
	
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String authenticate() {
		
		String result = AUTHENTICATION_FAILED_RESULT;
		
		try {
			int authenticateResult = UserDB.authenticate(getDBDataSource(), emailAddress, password.toCharArray());
			
			switch (authenticateResult) {
			case UserDB.AUTHENTICATE_OK: {
				result = AUTHENTICATION_OK_RESULT;
				break;
			}
			case UserDB.AUTHENTICATE_FAILED: {
				setMessage(null, "The email address and/or password were not recognised");
				break;
			}
			case UserDB.AUTHENTICATE_EMAIL_CODE_SET: {
				setMessage(null, "Your account has not yet been activated. Please use the link in your activation email.");
				break;
			}
			}
			
		} catch (Exception e) {
			internalError(e);
		}
		
		return result;
	}
	
	public String signUp() {
		return SIGNUP_RESULT;
	}
}
