package uk.ac.exeter.QuinCe.database.User;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Database access class for users.
 * 
 * Handles reading/writing of user details
 * and all the functions that shouldn't be performed at the session level. 
 * 
 * @author Steve Jones
 *
 */
public class UserDB {

	/**
	 * SQL statement to search for a user by email address
	 */
	private static final String USER_SEARCH_BY_EMAIL_STATEMENT = "SELECT id,email,firstname,surname,email_code,email_code_time,password_code,password_code_time FROM user WHERE email = ?";
	
	/**
	 * SQL statement to create a new user record
	 */
	private static final String CREATE_USER_STATEMENT = "INSERT INTO user (email, salt, password, firstname, surname, email_code, email_code_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	/**
	 * SQL statement to store an email verification code with a timestamp
	 */
	private static final String CREATE_EMAIL_VERIFICATION_CODE_STATEMENT = "UPDATE user SET email_code = ?, email_code_time = ? WHERE id = ?";

	/**
	 * SQL statement to store an password reset code with a timestamp
	 */
	private static final String CREATE_PASSWORD_RESET_CODE_STATEMENT = "UPDATE user SET password_code = ?, password_code_time = ? WHERE id = ?";
	
	/**
	 * SQL statement to retrieve the details required to authenticate a user
	 */
	private static final String GET_AUTHENTICATION_DETAILS_STATEMENT = "SELECT salt,password,email_code FROM user WHERE email = ?";
	
	/**
	 * SQL statement to change a user's password
	 */
	private static final String CHANGE_PASSWORD_STATEMENT = "UPDATE user SET salt = ?, password = ? WHERE id = ?";
	
	/**
	 * The length of the string to be used for email verification and password reset codes
	 */
	private static final int CODE_LENGTH = 50;
	
	/**
	 * Indicates a successful authentication
	 */
	public static final int AUTHENTICATE_OK = 0;
	
	/**
	 * Indicates a failed authentication
	 */
	public static final int AUTHENTICATE_FAILED = 1;
	
	/**
	 * Indicates that authentication could not be completed because the 
	 * email verification flag is set.
	 */
	public static final int AUTHENTICATE_EMAIL_CODE_SET = 2;
	
	/**
	 * Indicates that a code check succeeded
	 */
	public static final int CODE_OK = 0;
	
	/**
	 * Indicates that a code check failed because the supplied code was
	 * different from the one stored
	 */
	public static final int CODE_FAILED = 1;
	
	/**
	 * Indicates that a code check failed because the code has expired
	 */
	public static final int CODE_EXPIRED = 2;
	
	/**
	 * The number of hours for which generated codes are valid.
	 */
	public static final int CODE_EXPIRY_HOURS = 24;
	
	/**
	 * Locate a user in the database using their email address.
	 * 
	 * If the user can't be found, this method returns {@code null}.
	 * 
	 * @param conn The database connection to be used
	 * @param email The user's email address.
	 * @return A User object representing the user, or {@code null} if the user's record could not be found.
	 * @throws MissingParamException If the supplied email is null
	 * @throws SQLException
	 * @see uk.ac.exeter.QuinCe.data.User
	 */
	public static User getUser(DataSource dataSource, String email) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(email, "email");
		
		Connection connection = null;
		PreparedStatement stmt = null;
		User foundUser = null;
		
		try {
			connection = dataSource.getConnection();
			stmt = connection.prepareStatement(USER_SEARCH_BY_EMAIL_STATEMENT);
			stmt.setString(1, email);
			ResultSet result = stmt.executeQuery();
			
			if (result.first()) {
				foundUser = new User(result.getInt(1), result.getString(2), result.getString(3), result.getString(4));
				foundUser.setEmailVerificationCode(result.getString(5), result.getTimestamp(6));
				foundUser.setPasswordResetCode(result.getString(7), result.getTimestamp(8));
			}
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while searching for the user", e);
		} finally {
			try {
				if (null != stmt) {
					stmt.close();
				}
				
				if (null != connection) {
					connection.close();
				}
			} catch (SQLException e) {
				// There's not much we can do here...
			}
		}
		
		return foundUser;
	}
	
	/**
	 * Creates a new user and stores it in the database.
	 * 
	 * The user's password is salted and hashed.
	 * 
	 * @param conn The database connection
	 * @param email The user's email address
	 * @param password The password entered by the user
	 * @param givenName The user's given name
	 * @param surname The user's surname
	 * @param generateEmailVerificationCode Indicates whether or not an email verification code should be generated for the user
	 * @return A new User object representing the user
	 * @throws UserExistsException If a user with the specified email address already exists in the database
	 * @throws MissingParamException If any of the parameters are null
	 * @throws SQLException If there's an error storing the details in the database
	 * @throws HashException If an error occurs while hashing the user's password 
	 * @see uk.ac.exeter.QuinCe.data.User
	 */
	public static User createUser(DataSource dataSource, String email, char[] password, String givenName, String surname, boolean generateEmailVerificationCode) throws UserExistsException, DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(email, "email");
		MissingParam.checkMissing(password, "password");
		MissingParam.checkMissing(givenName, "givenName");
		MissingParam.checkMissing(surname, "surname");

		User newUser = null;
		Connection connection = null;
		PreparedStatement stmt = null;
		
		try {
			if (null != getUser(dataSource, email)) {
				throw new UserExistsException();
			} else {
				SaltAndHashedPassword generatedPassword = generateHashedPassword(password);
				
				String emailVerificationCode = null;
				Timestamp time = null;
				
				if (generateEmailVerificationCode) {
					emailVerificationCode = new String(PasswordHash.generateRandomString(CODE_LENGTH));
					time = new Timestamp(System.currentTimeMillis());
				}
				
				connection = dataSource.getConnection();
				stmt = connection.prepareStatement(CREATE_USER_STATEMENT);
				stmt.setString(1, email);
				stmt.setBytes(2, generatedPassword.salt);
				stmt.setBytes(3, generatedPassword.hashedPassword);
				stmt.setString(4, givenName);
				stmt.setString(5, surname);
				stmt.setString(6, emailVerificationCode);
				stmt.setTimestamp(7, time);

				stmt.execute();
				
				newUser = getUser(dataSource, email);
			}
			
		} catch (SQLException|InvalidKeySpecException|NoSuchAlgorithmException e) {
			throw new DatabaseException("An error occurred while creating the user's database record", e);
		} finally {
			try {
				if (null != stmt) {
					stmt.close();
				}
				if (null != connection) {
					connection.close();
				}
			} catch (SQLException e) {
				// Do nothing
			}
		}
		
		return newUser;
	}
	
	/**
	 * Generate an email verification code and store it in the database.
	 * The code will be added to the passed in User object.
	 * @param conn A database connection
	 * @param user The user who requires the code
	 * @throws NoSuchUserException If the user doesn't exist
	 * @throws DatabaseException If an error occurs while updating the database
	 * @throws MissingParamException If the supplied user object is null
	 */
	public static void generateEmailVerificationCode(DataSource dataSource, User user) throws NoSuchUserException, DatabaseException, MissingParamException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(user, "user");

		if (null == getUser(dataSource, user.getEmailAddress())) {
			throw new NoSuchUserException();
		}
		
		Connection connection = null;
		PreparedStatement stmt = null;

		String verificationCode = new String(PasswordHash.generateRandomString(CODE_LENGTH));
		Timestamp time = new Timestamp(System.currentTimeMillis());
		
		try {
			connection = dataSource.getConnection();
			stmt = connection.prepareStatement(CREATE_EMAIL_VERIFICATION_CODE_STATEMENT);
			stmt.setString(1, verificationCode);
			stmt.setTimestamp(2, time);
			stmt.setInt(3, user.getDatabaseID());
			stmt.execute();
			
			user.setEmailVerificationCode(verificationCode, time);
		} catch(SQLException e) {
			throw new DatabaseException("An error occurred while storing the verification code", e);
		} finally {
			try {
				if (null != stmt) {
					stmt.close();
				}
				
				if (null != connection) {
					connection.close();
				}
			} catch (SQLException e) {
				// Do nothing
			}
		}
	}

	/**
	 * Generate a password reset code code and store it in the database.
	 * The code will be added to the passed in User object.
	 * @param conn
	 * @param user
	 * @throws NoSuchUserException If the user doesn't exist
	 * @throws DatabaseException If an error occurs while updating the database
	 * @throws MissingParamException If the supplied user object is null
	 */
	public static void generatePasswordResetCode(DataSource dataSource, User user) throws NoSuchUserException, DatabaseException, MissingParamException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(user, "user");

		if (null == getUser(dataSource, user.getEmailAddress())) {
			throw new NoSuchUserException();
		}
		
		Connection connection = null;
		PreparedStatement stmt = null;

		String resetCode = new String(PasswordHash.generateRandomString(CODE_LENGTH));
		Timestamp time = new Timestamp(System.currentTimeMillis());
		
		try {
			connection = dataSource.getConnection();
			stmt = connection.prepareStatement(CREATE_PASSWORD_RESET_CODE_STATEMENT);
			stmt.setString(1, resetCode);
			stmt.setTimestamp(2, time);
			stmt.setInt(3, user.getDatabaseID());
			stmt.execute();
			
			user.setPasswordResetCode(resetCode, time);
		} catch(SQLException e) {
			throw new DatabaseException("An error occurred while storing the password reset code", e);
		} finally {
			try {
				if (null != stmt) {
					stmt.close();
				}
				if (null != connection) {
					connection.close();
				}
			} catch (SQLException e) {
				// Do nothing
			}
		}
	}
	
	/**
	 * Authenticates a user. If either the email address doesn't exist,
	 * or the passwords don't match, authentication will fail. No indication
	 * of which test caused the failure is given. If the email verification
	 * code is set, authentication will also fail. This will be indicated.
	 * 
	 * @param conn A database connection
	 * @param email The user's email address
	 * @param password The password supplied by the user
	 * @return One of AUTHENTICATE_OK, AUTHENTICATE_FAILED, or AUTHENTICATE_EMAIL_CODE_SET
	 * @throws DatabaseException If an error occurs while retrieving the user's details
	 */
	public static int authenticate(DataSource dataSource, String email, char[] password) throws MissingParamException, DatabaseException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		
		int authenticationResult = AUTHENTICATE_FAILED;
		
		Connection connection = null;
		PreparedStatement stmt = null;
		
		try {
			connection = dataSource.getConnection();
			stmt = connection.prepareStatement(GET_AUTHENTICATION_DETAILS_STATEMENT);
			stmt.setString(1,  email);
			ResultSet result = stmt.executeQuery();
			if (result.first()) {
				byte[] salt = result.getBytes(1);
				byte[] storedPassword = result.getBytes(2);
				String emailVerificationCode = result.getString(3);
				
				if (null != emailVerificationCode) {
					authenticationResult = AUTHENTICATE_EMAIL_CODE_SET;
				} else {
					// Recreate the salted hashed password
					byte[] hashedPassword = PasswordHash.pbkdf2(password, salt, PasswordHash.PBKDF2_ITERATIONS, PasswordHash.HASH_BYTE_SIZE);
					
					if (Arrays.equals(storedPassword, hashedPassword)) {
						authenticationResult = AUTHENTICATE_OK;
					}
				}
			}
		} catch (SQLException|InvalidKeySpecException|NoSuchAlgorithmException e) {
			// Any failure results in an authentication failure
			// Although nothing should get returned, best to be safe
			authenticationResult = AUTHENTICATE_FAILED;

			throw new DatabaseException("An error occurred while authenticating the user", e);
		} finally {
			try {
				if (null != stmt) {
					stmt.close();
				}
				if (null != connection) {
					connection.close();
				}
			} catch (SQLException e) {
				// Do nothing
			}
		}
		
		return authenticationResult;
	}
	
	/**
	 * Changes a user's password. The user's old password must be
	 * authenticated before the new one is stored.
	 * @param conn A database connection
	 * @param user The user whose password is to be changed
	 * @param oldPassword The user's old password
	 * @param newPassword The user's new password
	 * @return {@code true} if the password was changed successfully; {@code false} otherwise.
	 * @throws DatabaseException If an error occurred
	 * @throws MissingParamException If any of the parameters are null
	 */
	public static boolean changePassword(DataSource dataSource, User user, char[] oldPassword, char[] newPassword) throws DatabaseException, MissingParamException {
		
		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(user, "user");
		MissingParam.checkMissing(oldPassword, "oldPassword", true);
		MissingParam.checkMissing(newPassword, "newPassword");

		// First we authenticate the user with their current password. If that works, we can set
		// the new password
		boolean result = false;
		
		int authenticationResult = authenticate(dataSource, user.getEmailAddress(), oldPassword);
		
		if (AUTHENTICATE_OK == authenticationResult) {
			Connection connection = null;
			PreparedStatement stmt = null;

			try {
				SaltAndHashedPassword generatedPassword = generateHashedPassword(newPassword);
				
				connection = dataSource.getConnection();
				stmt = connection.prepareStatement(CHANGE_PASSWORD_STATEMENT);
				stmt.setBytes(1, generatedPassword.salt);
				stmt.setBytes(2, generatedPassword.hashedPassword);
				stmt.setInt(3, user.getDatabaseID());
				stmt.execute();
				
			} catch (SQLException|InvalidKeySpecException|NoSuchAlgorithmException e) {
				result = false;
				
				throw new DatabaseException("An error occurred while updating the password", e);
			} finally {
				try {
					if (null != stmt) {
						stmt.close();
					}
					
					if (null != connection) {
						connection.close();
					}
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Check a user's email verification code against the supplied code
	 * @param conn A database connection
	 * @param email The user's email address
	 * @param code The code to be checked
	 * @return An integer value indicating whether the code matched, didn't match, or the timestamp has expired.
	 * @throws DatabaseException
	 */
	public static int checkEmailVerificationCode(DataSource dataSource, String email, String code) throws DatabaseException, MissingParamException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(email, "email");
		MissingParam.checkMissing(code, "code");

		int result = CODE_FAILED;
		
		User user = getUser(dataSource, email);
		if (null != user) {
			String storedCode = user.getEmailVerificationCode();
			Timestamp codeTime = user.getEmailVerificationCodeTime();
			
			result = checkCode(storedCode, codeTime, code);
		}
		
		return result;
	}
	
	/**
	 * Check a user's password reset code against the supplied code
	 * @param conn A database connection
	 * @param email The user's email address
	 * @param code The code to be checked
	 * @return An integer value indicating whether the code matched, didn't match, or the timestamp has expired.
	 * @throws DatabaseException
	 */
	public static int checkPasswordResetCode(DataSource dataSource, String email, String code) throws DatabaseException, MissingParamException {

		MissingParam.checkMissing(dataSource, "dataSource");
		MissingParam.checkMissing(email, "email");
		MissingParam.checkMissing(code, "code");

		int result = CODE_FAILED;
		
		User user = getUser(dataSource, email);
		if (null != user) {
			String storedCode = user.getPasswordResetCode();
			Timestamp codeTime = user.getPasswordResetCodeTime();
			
			result = checkCode(storedCode, codeTime, code);
		}
		
		return result;
	}
	
	/**
	 * Checks wheter or not two codes match, and that we are within
	 * the time limit (defined by {@code CODE_EXPIRY_HOURS} of the code's timestamp
	 * @param userCode The code that we are checking against (i.e. the one stored in the database)
	 * @param codeTime The code's timestamp
	 * @param codeToCheck The code to be checked
	 * @return An integer value indicating whether the code matched, didn't match, or the timestamp has expired.
	 */
	private static int checkCode(String userCode, Timestamp codeTime, String codeToCheck) {
		int result = CODE_FAILED;
		
		// If the code is null, the check will fail
		if (null != userCode && null != codeToCheck) {

			// If the code isn't timestamped (this shouldn't happen),
			// we assume the code has expired.
			if (null == codeTime) {
				result = CODE_EXPIRED;
			// Check the code's timestamp
			} else if (!DateTimeUtils.timeWithinLastHours(codeTime.getTime(), CODE_EXPIRY_HOURS)) {
				result = CODE_EXPIRED;
			// See if the code actually matches
			} else if (codeToCheck.equals(userCode)) {
				result = CODE_OK;
			}
		}
		
		return result;
	}
	
	/**
	 * Generated a salted+hashed version of a password.
	 * The salt is randomly generated and appended to the password before hashing.
	 * @param password The password
	 * @return An object containing the salt and hashed password+salt.
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	private static SaltAndHashedPassword generateHashedPassword(char[] password) throws NoSuchAlgorithmException, InvalidKeySpecException {
		SaltAndHashedPassword result = new SaltAndHashedPassword();
		// Create the salted, hashed password
		result.salt = PasswordHash.generateSalt();
		result.hashedPassword = PasswordHash.pbkdf2(password, result.salt, PasswordHash.PBKDF2_ITERATIONS, PasswordHash.HASH_BYTE_SIZE);
		
		return result;
	}

	/**
	 * Mini utility class for handling generated salts and hashed passwords.
	 * They are always generated together, so they belong together.
	 */
	private static class SaltAndHashedPassword {
		private byte[] salt;
		private byte[] hashedPassword;
	}
}

