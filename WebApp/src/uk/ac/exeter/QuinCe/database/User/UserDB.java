package uk.ac.exeter.QuinCe.database.User;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.DatabaseException;

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

	private static final String USER_SEARCH_BY_EMAIL_STATEMENT = "SELECT id,email,firstname,surname,email_code,password_code FROM user WHERE email = ?";
	private static final String CREATE_USER_STATEMENT = "INSERT INTO user (email, salt, password, firstname, surname) VALUES (?, ?, ?, ?, ?)";
	private static final String CREATE_EMAIL_VERIFICATION_CODE_STATEMENT = "UPDATE user SET email_code = ? where ID = ?";
	private static final String CREATE_PASSWORD_RESET_CODE_STATEMENT = "UPDATE user SET password_code = ? where ID = ?";
	
	private static final int VERIFICATION_CODE_LENGTH = 50;
	
	
	/**
	 * Locate a user in the database using their email address.
	 * 
	 * If the user can't be found, this method returns {@code null}.
	 * 
	 * @param conn The database connection to be used
	 * @param email The user's email address.
	 * @return A User object representing the user, or {@code null} if the user's record could not be found.
	 * @throws SQLException
	 * @see uk.ac.exeter.QuinCe.data.User
	 */
	public static User getUser(Connection conn, String email) throws DatabaseException {
		
		PreparedStatement query = null;
		User foundUser = null;
		
		try {
			query = conn.prepareStatement(USER_SEARCH_BY_EMAIL_STATEMENT);
			query.setString(1, email);
			ResultSet result = query.executeQuery();
			
			if (result.first()) {
				foundUser = new User(result.getInt(1), result.getString(2), result.getString(3), result.getString(4));
				foundUser.setEmailVerificationCode(result.getString(5));
				foundUser.setPasswordResetCode(result.getString(6));
			}
		} catch (SQLException e) {
			throw new DatabaseException("An error occurred while searching for the user", e);
		} finally {
			try {
				query.close();
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
	 * @return A new User object representing the user
	 * @throws UserExistsException If a user with the specified email address already exists in the database
	 * @throws SQLException If there's an error storing the details in the database
	 * @throws HashException If an error occurs while hashing the user's password 
	 * @see uk.ac.exeter.QuinCe.data.User
	 */
	public static User createUser(Connection conn, String email, char[] password, String givenName, String surname) throws UserExistsException, DatabaseException {
		
		User newUser = null;
		PreparedStatement stmt = null;
		
		try {
			if (null != getUser(conn, email)) {
				throw new UserExistsException();
			} else {
				// Create the salted, hashed password
				byte[] salt = PasswordHash.generateSalt();
				byte[] hashedPassword = PasswordHash.pbkdf2(password, salt, PasswordHash.PBKDF2_ITERATIONS, PasswordHash.HASH_BYTE_SIZE);
				
				stmt = conn.prepareStatement(CREATE_USER_STATEMENT);
				stmt.setString(1,  email);
				stmt.setString(2, new String(salt));
				stmt.setString(3, new String(hashedPassword));
				stmt.setString(4, givenName);
				stmt.setString(5, surname);
				
				stmt.execute();
				
				newUser = getUser(conn, email);
			}
			
		} catch (SQLException|InvalidKeySpecException|NoSuchAlgorithmException e) {
			throw new DatabaseException("An error occurred while creating the user's database record", e);
		} finally {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
		
		return newUser;
	}
	
	/**
	 * Generate an email verification code and store it in the user's details
	 * @param conn A database connection
	 * @param user The user who requires the code
	 * @throws NoSuchUserException If the user doesn't exist
	 * @throws DatabaseException If an error occurs while updating the database
	 */
	public static void generateEmailVerificationCode(Connection conn, User user) throws NoSuchUserException, DatabaseException {

		PreparedStatement stmt = null;

		String verificationCode = new String(PasswordHash.generateRandomString(VERIFICATION_CODE_LENGTH));
		
		try {
			stmt = conn.prepareStatement(CREATE_EMAIL_VERIFICATION_CODE_STATEMENT);
			stmt.setString(1, verificationCode);
			stmt.setInt(2, user.getDatabaseID());
			stmt.execute();
			
			user.setEmailVerificationCode(verificationCode);
		} catch(SQLException e) {
			throw new DatabaseException("An error occurred while storing the verification code", e);
		} finally {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
	}

	/**
	 * Generate a password reset code code and 
	 * @param conn
	 * @param user
	 * @throws NoSuchUserException
	 * @throws DatabaseException
	 */
	public static void generatePasswordResetCode(Connection conn, User user) throws NoSuchUserException, DatabaseException {

		PreparedStatement stmt = null;

		String resetCode = new String(PasswordHash.generateRandomString(VERIFICATION_CODE_LENGTH));
		
		try {
			stmt = conn.prepareStatement(CREATE_PASSWORD_RESET_CODE_STATEMENT);
			stmt.setString(1, resetCode);
			stmt.setInt(2, user.getDatabaseID());
			stmt.execute();
			
			user.setPasswordResetCode(resetCode);
		} catch(SQLException e) {
			throw new DatabaseException("An error occurred while storing the password reset code", e);
		} finally {
			if (null != stmt) {
				try {
					stmt.close();
				} catch (SQLException e) {
					// Do nothing
				}
			}
		}
	}
}
