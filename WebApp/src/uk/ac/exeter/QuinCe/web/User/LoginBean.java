package uk.ac.exeter.QuinCe.web.User;

/**
 * JSF Managed Bean for handling user logins
 * @author Steve Jones
 *
 */
public class LoginBean {
	
	public static final String SIGNUP_RESULT = "SignUp";
	
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
		return "OK";
	}
	
	public String signUp() {
		return SIGNUP_RESULT;
	}
}
