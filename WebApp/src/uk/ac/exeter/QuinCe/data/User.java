package uk.ac.exeter.QuinCe.data;

import java.sql.Timestamp;

import uk.ac.exeter.QuinCe.utils.MissingData;
import uk.ac.exeter.QuinCe.utils.MissingDataException;

/**
 * Object to represent a user and perform various
 * user-related tasks.
 * 
 * @author Steve Jones
 *
 */
public class User {
	
	/**
	 * The user's database record ID
	 */
	private int itsDatabaseID;
	
	/**
	 * The user's email address
	 */
	private String itsEmailAddress;
	
	/**
	 * The user's first/given name
	 */
	private String itsGivenName;
	
	/**
	 * The user's surname/family name
	 */
	private String itsSurname;
	
	/**
	 * The email verification code. If this is set, the
	 * user should not be able to log in until the verification is complete.
	 */
	private String itsEmailVerificationCode = null;
	
	
	/**
	 * The time at which the email verification code was set
	 */
	private Timestamp itsEmailVerificationCodeTime = null;
	
	/**
	 * The password reset code.
	 * If this is set, the user should not be able to log in
	 * until the password has been changed. 
	 */
	private String itsPasswordResetCode = null;
	
	/**
	 * The time at which the password reset code was set
	 */
	private Timestamp itsPasswordResetCodeTime = null;
	
	/**
	 * Construct a User object
	 * @param id The database record ID for the user
	 * @param email The user's email address
	 * @param givenName The user's given name
	 * @param surname The user's surname
	 */
	public User(int id, String email, String givenName, String surname) throws MissingDataException {
		
		MissingData.checkMissing(email, "email");
		MissingData.checkMissing(givenName, "givenName");
		MissingData.checkMissing(surname, "surname");
		
		itsDatabaseID = id;
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
	
	/**
	 * Set the email verification code
	 * @param code The email verification code
	 */
	public void setEmailVerificationCode(String code, Timestamp time) throws MissingDataException {

		itsEmailVerificationCode = code;
		itsEmailVerificationCodeTime = time;
	}
	
	/**
	 * Set the password reset code
	 * @param code The password reset code
	 */
	public void setPasswordResetCode(String code, Timestamp time) throws MissingDataException {
		itsPasswordResetCode = code;
		itsPasswordResetCodeTime = time;
	}
	
	/**
	 * Retrieve the email verification code
	 * @return The email verification code
	 */
	public String getEmailVerificationCode() {
		return itsEmailVerificationCode;
	}
	
	/**
	 * Return the time at which the email verification code was set
	 * @return The time at which the email verification code was set
	 */
	public Timestamp getEmailVerificationCodeTime() {
		return itsEmailVerificationCodeTime;
	}

	/**
	 * Retrieve the password reset code
	 * @return code The password reset code
	 */
	public String getPasswordResetCode() {
		return itsPasswordResetCode;
	}
	
	/**
	 * Return the time at which the password reset code was set
	 * @return The time at which the password reset code was set
	 */
	public Timestamp getPasswordResetCodeTime() {
		return itsPasswordResetCodeTime;
	}
	
	/**
	 * Return the database ID for this user. Note that this should
	 * not be used for any user-facing activities.
	 * 
	 * @return
	 */
	public int getDatabaseID() {
		return itsDatabaseID;
	}
	
	/**
	 * Sets the database ID for this user object
	 * @param id The user's ID
	 */
	public void setDatabaseID(int id) {
		itsDatabaseID = id;
	}
}
