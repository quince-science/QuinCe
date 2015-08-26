package uk.ac.exeter.QuinCe.data;

/**
 * Object to represent a user and perform various
 * user-related tasks.
 * 
 * @author Steve Jones
 *
 */
public class User {

	/**
	 * The user's email address
	 */
	private String itsEmailAddress = null;
	
	/**
	 * The user's first/given name
	 */
	private String itsGivenName = null;
	
	/**
	 * The user's surname/family name
	 */
	private String itsSurname = null;
	
	public User (String email, String givenName, String surname) {
		itsEmailAddress = email;
		itsGivenName = givenName;
		itsSurname = surname;
	}
	
	/**
	 * Return the user's email address
	 * @return The user's email address
	 */
	public String getEmailAddress() {
		return itsEmailAddress;
	}
	
	/**
	 * Return the user's full name, constructed by
	 * combining the first name and surname.
	 * @return The user's full name
	 */
	public String getFullName() {
		return itsGivenName + " " + itsSurname;
	}
	
	/**
	 * Return the user's given name
	 * @return The user's given name
	 */
	public String getGivenName() {
		return itsGivenName;
	}
	
	/**
	 * Return the user's surname
	 * @return The user's surname
	 */
	public String getSurname() {
		return itsSurname;
	}
}
