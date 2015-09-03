package uk.ac.exeter.QuinCe.web.User;

/**
 * JSF Managed Bean for handling new user sign-up
 * @author Steve Jones
 *
 */
public class SignupBean {

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
		this.emailAddress = emailAddress;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getPassword1() {
		return password1;
	}

	public void setPassword1(String password1) {
		this.password1 = password1;
	}

	public String getPassword2() {
		return password2;
	}

	public void setPassword2(String password2) {
		this.password2 = password2;
	}
	
	public String signUp() {
		return "OK";
	}

}
