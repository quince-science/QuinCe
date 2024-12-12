package uk.ac.exeter.QuinCe.User;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Database access class for users.
 *
 * Handles reading/writing of user details and all the functions that shouldn't
 * be performed at the session level.
 */
public class UserDB {

  /**
   * SQL query to get a user record using the email address.
   *
   * @see getUser(Connection, String)
   */
  private static final String USER_SEARCH_BY_EMAIL_STATEMENT = "SELECT "
    + "id,email,firstname,surname,email_code,email_code_time,password_code,"
    + "password_code_time,permissions,preferences FROM user WHERE email = ?";

  /**
   * SQL query to get a user record using the email address.
   *
   * @see getUser(Connection, long)
   */
  private static final String USER_SEARCH_BY_ID_STATEMENT = "SELECT "
    + "id,email,firstname,surname,email_code,email_code_time,password_code,"
    + "password_code_time,permissions,preferences FROM user WHERE id = ?";

  /**
   * SQL statement to create a new user record.
   *
   * @see createUser(DataSource, String, char[], String, String, boolean)
   */
  private static final String CREATE_USER_STATEMENT = "INSERT INTO user "
    + "(email, salt, password, firstname, surname, email_code, "
    + "email_code_time) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";

  /**
   * SQL statement to store an the email verification code for a user.
   *
   * @see generateEmailVerificationCode(DataSource, User)
   */
  private static final String CREATE_EMAIL_VERIFICATION_CODE_STATEMENT = "UPDATE "
    + "user SET email_code = ?, email_code_time = ? WHERE id = ?";

  /**
   * SQL statement to store a password reset code for a user.
   *
   * @see generatePasswordResetCode(DataSource, User)
   */
  private static final String CREATE_PASSWORD_RESET_CODE_STATEMENT = "UPDATE "
    + "user SET password_code = ?, password_code_time = ? WHERE id = ?";

  /**
   * SQL query to retrieve the details required to authenticate a user.
   *
   * @see authenticate(DataSource, String, char[])
   */
  private static final String GET_AUTHENTICATION_DETAILS_STATEMENT = "SELECT "
    + "salt,password,email_code FROM user WHERE email = ?";

  /**
   * SQL statement to set a user's password.
   *
   * @see changePassword(Connection, User, char[])
   */
  private static final String CHANGE_PASSWORD_STATEMENT = "UPDATE "
    + "user SET salt = ?, password = ? WHERE id = ?";

  /**
   * SQL statement to remove an email verification code.
   *
   * @see clearEmailVerificationCode(DataSource, String)
   */
  private static final String CLEAR_EMAIL_CODE_STATEMENT = "UPDATE "
    + "user SET email_code = NULL, email_code_time = NULL WHERE email = ?";

  /**
   * SQL statement to remove a password reset code.
   *
   * @see clearPasswordResetCode(Connection, String)
   */
  private static final String CLEAR_PASSWORD_RESET_CODE_STATEMENT = "UPDATE "
    + "user SET password_code = NULL, password_code_time = NULL "
    + "WHERE email = ?";

  /**
   * SQL statement to store a user's preferences.
   *
   * @see savePreferences(DataSource, UserPreferences)
   */
  private static final String STORE_PREFERENCES_STATEMENT = "UPDATE "
    + "user SET preferences = ? WHERE id = ?";

  /**
   * SQL query to retrieve a user's preferences.
   *
   * @see getPreferences(DataSource, long)
   */
  private static final String GET_PREFERENCES_QUERY = "SELECT "
    + "preferences FROM user WHERE id = ?";

  /**
   * SQL statement to update a user's last login time.
   *
   * @see #recordLogin(Connection, String)
   */
  private static final String RECORD_LOGIN_STATEMENT = "UPDATE "
    + "user SET last_login = ? WHERE email = ?";

  private static final String USER_NAMES_STATEMENT = "SELECT "
    + "id, firstname, surname, permissions FROM user "
    + "ORDER BY surname, firstname";

  /**
   * The length of the string to be used for email verification and password
   * reset codes.
   *
   * @see createUser(DataSource, String, char[], String, String, boolean)
   * @see generateEmailVerificationCode(DataSource, User)
   * @see generatePasswordResetCode(DataSource, User)
   */
  private static final int CODE_LENGTH = 50;

  /**
   * Value indicating that authentication succeeded.
   *
   * @see authenticate(DataSource, String, char[])
   */
  public static final int AUTHENTICATE_OK = 0;

  /**
   * Value indicating authentication failed.
   *
   * @see authenticate(DataSource, String, char[])
   */
  public static final int AUTHENTICATE_FAILED = 1;

  /**
   * Value indicating that authentication could not be completed because the
   * user's email verification flag is set.
   *
   * @see authenticate(DataSource, String, char[])
   */
  public static final int AUTHENTICATE_EMAIL_CODE_SET = 2;

  /**
   * Value indicating that a check on an email verification or password reset
   * code succeeded.
   *
   * @see #checkCode(String, Timestamp, String)
   */
  public static final int CODE_OK = 0;

  /**
   * Value indicating that a check on an email verification or password reset
   * code failed failed because the supplied code was different from the one
   * stored.
   *
   * @see #checkCode(String, Timestamp, String)
   */
  public static final int CODE_FAILED = 1;

  /**
   * Value indicating that a check on an email verification or password reset
   * code failed failed because the code has expired.
   *
   * @see #checkCode(String, Timestamp, String)
   */
  public static final int CODE_EXPIRED = 2;

  /**
   * The number of hours for which email verification and password reset codes
   * are valid.
   *
   * @see #checkCode(String, Timestamp, String)
   */
  public static final int CODE_EXPIRY_HOURS = 24;

  /**
   * Get a user's details using their email address.
   *
   * <p>
   * Returns {@code null} if the email address is not found in the database.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param email
   *          The email address.
   * @return The {@link User} object.
   * @throws DatabaseException
   *           If a database error occurs
   *
   * @see getUser(Connection, String)
   */
  public static User getUser(DataSource dataSource, String email)
    throws DatabaseException {

    Connection conn = null;
    User user = null;

    try {
      conn = dataSource.getConnection();
      user = getUser(conn, email);
    } catch (SQLException e) {
      throw new DatabaseException("An error occurred while retrieving the user",
        e);
    } catch (DatabaseException | MissingParamException e) {
      throw e;
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return user;
  }

  /**
   * Get a user's details using their email address.
   *
   * <p>
   * Returns {@code null} if the email address is not found in the database.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param email
   *          The email address.
   * @return The {@link User} object.
   * @throws DatabaseException
   *           If a database error occurs
   *
   * @see #USER_SEARCH_BY_EMAIL_STATEMENT
   */
  public static User getUser(Connection conn, String email)
    throws DatabaseException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(email, "email");

    PreparedStatement stmt = null;
    ResultSet record = null;
    User foundUser = null;

    try {
      stmt = conn.prepareStatement(USER_SEARCH_BY_EMAIL_STATEMENT);
      stmt.setString(1, email);
      record = stmt.executeQuery();

      if (record.next()) {
        foundUser = new User(record.getInt(1), record.getString(2),
          record.getString(3), record.getString(4), record.getInt(9),
          record.getString(10));
        foundUser.setEmailVerificationCode(record.getString(5),
          record.getTimestamp(6));
        foundUser.setPasswordResetCode(record.getString(7),
          record.getTimestamp(8));
      }
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while searching for the user", e);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
    }

    return foundUser;
  }

  /**
   * Determine whether or not a {@link User} exists with the specified email
   * address.
   *
   * @param dataSource
   *          A data source.
   * @param email
   *          The email address.
   * @return {@code true} if a {@link User} with the specified email exists;
   *         {@code false} if it does not.
   * @throws DatabaseException
   *           If an error occurs while searching the database.
   */
  public static boolean userExists(DataSource dataSource, String email)
    throws DatabaseException {
    return null != getUser(dataSource, email);
  }

  /**
   * Determine whether or not a {@link User} exists with the specified database
   * ID.
   *
   * @param dataSource
   *          A data source.
   * @param id
   *          The ID.
   * @return {@code true} if a {@link User} with the specified email exists;
   *         {@code false} if it does not.
   * @throws DatabaseException
   *           If an error occurs while searching the database.
   */
  public static boolean userExists(DataSource dataSource, long id)
    throws DatabaseException {
    return null != getUser(dataSource, id);
  }

  /**
   * Determine whether or not a {@link User} exists with the specified database
   * ID.
   *
   * @param conn
   *          A database connection.
   * @param id
   *          The ID.
   * @return {@code true} if a {@link User} with the specified email exists;
   *         {@code false} if it does not.
   * @throws DatabaseException
   *           If an error occurs while searching the database.
   */
  public static boolean userExists(Connection conn, long id)
    throws MissingParamException, DatabaseException {
    return null != getUser(conn, id);
  }

  /**
   * Get a user's details using their database ID.
   *
   * <p>
   * Returns {@code null} if the ID is not found in the database.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param id
   *          The user's database ID.
   * @return The {@link User} object.
   * @throws DatabaseException
   *           If a database error occurs.
   * @throws MissingParamException
   *           If any required parameters are missing.
   *
   * @see getUser(Connection, long)
   */
  public static User getUser(DataSource dataSource, long id)
    throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(id, "id");

    User user = null;
    Connection conn = null;

    try {
      conn = dataSource.getConnection();
      user = getUser(conn, id);
    } catch (SQLException e) {
      throw new DatabaseException("An error occured while finding the user", e);
    } finally {
      DatabaseUtils.closeConnection(conn);
    }

    return user;

  }

  /**
   * Get a user's details using their database ID.
   *
   * <p>
   * Returns {@code null} if the email address is not found in the database.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param id
   *          The ID.
   * @return The {@link User} object.
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   *
   * @see #USER_SEARCH_BY_ID_STATEMENT
   */
  public static User getUser(Connection conn, long id)
    throws DatabaseException, MissingParamException {
    MissingParam.checkMissing(conn, "dataSource");
    MissingParam.checkPositive(id, "id");

    PreparedStatement stmt = null;
    ResultSet record = null;
    User foundUser = null;

    try {
      stmt = conn.prepareStatement(USER_SEARCH_BY_ID_STATEMENT);
      stmt.setLong(1, id);
      record = stmt.executeQuery();

      if (record.next()) {
        foundUser = new User(record.getInt(1), record.getString(2),
          record.getString(3), record.getString(4), record.getInt(9),
          record.getString(10));
        foundUser.setEmailVerificationCode(record.getString(5),
          record.getTimestamp(6));
        foundUser.setPasswordResetCode(record.getString(7),
          record.getTimestamp(8));
      }
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while searching for the user", e);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
    }

    return foundUser;
  }

  /**
   * Get a user's preferences.
   *
   * @param dataSource
   *          A data source.
   * @param userId
   *          The user's database ID.
   * @return The preferences.
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws NoSuchUserException
   *           If the user does not exist in the database
   *
   * @see #GET_PREFERENCES_QUERY
   */
  public static UserPreferences getPreferences(DataSource dataSource,
    long userId)
    throws MissingParamException, DatabaseException, NoSuchUserException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkPositive(userId, "userId");

    UserPreferences result = null;

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet record = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_PREFERENCES_QUERY);
      stmt.setLong(1, userId);

      record = stmt.executeQuery();
      if (record.next()) {
        result = new UserPreferences(userId, record.getString(1));
      } else {
        throw new NoSuchUserException(userId);
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error reading user preferences", e);
    } finally {
      DatabaseUtils.closeResultSets(record);
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return result;
  }

  /**
   * Store a user's preferences.
   *
   * @param dataSource
   *          A data source
   * @param preferences
   *          The preferences to be stored
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   * @throws NoSuchUserException
   *           If the user specified in the {@link UserPreferences} object does
   *           not exist
   *
   * @see #STORE_PREFERENCES_STATEMENT
   */
  public static void savePreferences(DataSource dataSource,
    UserPreferences preferences)
    throws MissingParamException, DatabaseException, NoSuchUserException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(preferences, "preferences", true);

    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = dataSource.getConnection();

      if (null == getUser(conn, preferences.getUserId())) {
        throw new NoSuchUserException(preferences.getUserId());
      }

      stmt = conn.prepareStatement(STORE_PREFERENCES_STATEMENT);
      stmt.setString(1, preferences.writeToString());
      stmt.setLong(2, preferences.getUserId());

      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error storing user preferences", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Create a new user and store it in the database.
   *
   * <p>
   * The user's password is salted and hashed.
   * </p>
   *
   * <p>
   * The method returns a {@link User} object representing the database record.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param email
   *          The user's email address.
   * @param password
   *          The password entered by the user.
   * @param givenName
   *          The user's given name.
   * @param surname
   *          The user's surname.
   * @param generateEmailVerificationCode
   *          Indicates whether or not an email verification code should be
   *          generated for the user.
   * @return A new {@link User} object representing the user.
   * @throws UserExistsException
   *           If a user with the specified email address already exists in the
   *           database.
   * @throws MissingParamException
   *           If any of the parameters are null.
   * @throws DatabaseException
   *           If a database error occurs.
   *
   * @see #CODE_LENGTH
   * @see #CREATE_USER_STATEMENT
   * @see PasswordHash
   */
  public static User createUser(DataSource dataSource, String email,
    char[] password, String givenName, String surname,
    boolean generateEmailVerificationCode)
    throws UserExistsException, DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(email, "email");
    MissingParam.checkMissing(password, "password");
    MissingParam.checkMissing(givenName, "givenName");
    MissingParam.checkMissing(surname, "surname");

    User newUser = null;
    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      if (null != getUser(dataSource, email)) {
        throw new UserExistsException();
      } else {
        SaltAndHashedPassword generatedPassword = generateHashedPassword(
          password);

        String emailVerificationCode = null;
        Timestamp time = null;

        if (generateEmailVerificationCode) {
          emailVerificationCode = new String(
            PasswordHash.generateRandomString(CODE_LENGTH));
          time = new Timestamp(System.currentTimeMillis());
        }

        conn = dataSource.getConnection();
        stmt = conn.prepareStatement(CREATE_USER_STATEMENT);
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

    } catch (SQLException | InvalidKeySpecException
      | NoSuchAlgorithmException e) {
      throw new DatabaseException(
        "An error occurred while creating " + "the user's database record", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return newUser;
  }

  /**
   * Generate an email verification code for a {@link User}.
   *
   * <p>
   * The code is stored in the database along with its expiration time and added
   * to the passed in User object.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param user
   *          The {@link User} to which the code will be added.
   * @throws NoSuchUserException
   *           If the user doesn't exist.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   * @throws MissingParamException
   *           If any required parameters are missing.
   *
   * @see #CODE_LENGTH
   * @see #CREATE_EMAIL_VERIFICATION_CODE_STATEMENT
   */
  public static void generateEmailVerificationCode(DataSource dataSource,
    User user)
    throws NoSuchUserException, DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(user, "user");

    if (null == getUser(dataSource, user.getEmailAddress())) {
      throw new NoSuchUserException(user);
    }

    Connection conn = null;
    PreparedStatement stmt = null;

    String verificationCode = new String(
      PasswordHash.generateRandomString(CODE_LENGTH));
    Timestamp time = new Timestamp(System.currentTimeMillis());

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(CREATE_EMAIL_VERIFICATION_CODE_STATEMENT);
      stmt.setString(1, verificationCode);
      stmt.setTimestamp(2, time);
      stmt.setLong(3, user.getDatabaseID());
      stmt.execute();

      user.setEmailVerificationCode(verificationCode, time);
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while storing the verification code", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Generate a password reset code for a {@link User}.
   *
   * @param dataSource
   *          A data source.
   * @param user
   *          The {@link User} to which the code will be added.
   * @throws NoSuchUserException
   *           If the user doesn't exist.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   * @throws MissingParamException
   *           If any required parameters are missing.
   *
   * @see #CODE_LENGTH
   * @see #CREATE_PASSWORD_RESET_CODE_STATEMENT
   */
  public static void generatePasswordResetCode(DataSource dataSource, User user)
    throws NoSuchUserException, DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(user, "user");

    if (null == getUser(dataSource, user.getEmailAddress())) {
      throw new NoSuchUserException(user);
    }

    Connection conn = null;
    PreparedStatement stmt = null;

    String resetCode = new String(
      PasswordHash.generateRandomString(CODE_LENGTH));
    Timestamp time = new Timestamp(System.currentTimeMillis());

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(CREATE_PASSWORD_RESET_CODE_STATEMENT);
      stmt.setString(1, resetCode);
      stmt.setTimestamp(2, time);
      stmt.setLong(3, user.getDatabaseID());
      stmt.execute();

      user.setPasswordResetCode(resetCode, time);
    } catch (SQLException e) {
      throw new DatabaseException(
        "An error occurred while " + "storing the password reset code", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Authenticate a user.
   *
   * <p>
   * If either the email address doesn't exist, or the passwords don't match,
   * the method will return {@link #AUTHENTICATE_FAILED}. No indication of which
   * test caused the failure is given. If the email verification code is set,
   * authentication will also fail and the method will return
   * {@link #AUTHENTICATE_EMAIL_CODE_SET}.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param email
   *          The user's email address.
   * @param password
   *          The password supplied by the user.
   * @return One of {@link #AUTHENTICATE_OK}, {@link #AUTHENTICATE_FAILED}, or
   *         {@link #AUTHENTICATE_EMAIL_CODE_SET}.
   * @throws MissingParamException
   *           If any required parameters are missing
   * @throws DatabaseException
   *           If a database error occurs
   *
   * @see #GET_AUTHENTICATION_DETAILS_STATEMENT
   * @see PasswordHash
   */
  public static int authenticate(DataSource dataSource, String email,
    char[] password) throws MissingParamException, DatabaseException {

    MissingParam.checkMissing(dataSource, "dataSource");

    int authenticationResult = AUTHENTICATE_FAILED;

    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(GET_AUTHENTICATION_DETAILS_STATEMENT);
      stmt.setString(1, email);
      ResultSet result = stmt.executeQuery();
      if (result.next()) {
        byte[] salt = result.getBytes(1);
        byte[] storedPassword = result.getBytes(2);
        String emailVerificationCode = result.getString(3);

        if (null != emailVerificationCode) {
          authenticationResult = AUTHENTICATE_EMAIL_CODE_SET;
        } else {
          // Recreate the salted hashed password
          byte[] hashedPassword = PasswordHash.pbkdf2(password, salt,
            PasswordHash.PBKDF2_ITERATIONS, PasswordHash.HASH_BYTE_SIZE);

          if (Arrays.equals(storedPassword, hashedPassword)) {
            authenticationResult = AUTHENTICATE_OK;

            // Clear the password reset code - since we've authenticated, it's
            // no longer needed.
            //
            // This covers the case where a password reset code is requested by
            // a nefarious person. The user can continue to log in with their
            // valid credentials, and it will nullify the reset request.
            clearPasswordResetCode(conn, email);

            // Record the login
            recordLogin(conn, email);
          }
        }
      }
    } catch (SQLException | InvalidKeySpecException
      | NoSuchAlgorithmException e) {
      // Any failure results in an authentication failure
      // Although nothing should get returned, best to be safe
      authenticationResult = AUTHENTICATE_FAILED;

      throw new DatabaseException(
        "An error occurred while authenticating the user", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }

    return authenticationResult;
  }

  /**
   * Changes a user's password.
   *
   * <p>
   * The new password is salted and hashed before being stored in the database.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param user
   *          The user whose password is to be changed.
   * @param newPassword
   *          The user's new password.
   * @throws DatabaseException
   *           If an error occurred
   * @throws MissingParamException
   *           If any of the parameters are missing
   *
   * @see #CHANGE_PASSWORD_STATEMENT
   * @see PasswordHash
   */
  public static void changePassword(Connection conn, User user,
    char[] newPassword) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(user, "user");
    MissingParam.checkMissing(newPassword, "newPassword");

    PreparedStatement stmt = null;

    try {
      SaltAndHashedPassword generatedPassword = generateHashedPassword(
        newPassword);

      stmt = conn.prepareStatement(CHANGE_PASSWORD_STATEMENT);
      stmt.setBytes(1, generatedPassword.salt);
      stmt.setBytes(2, generatedPassword.hashedPassword);
      stmt.setLong(3, user.getDatabaseID());
      stmt.execute();

    } catch (SQLException | InvalidKeySpecException
      | NoSuchAlgorithmException e) {
      throw new DatabaseException(
        "An error occurred while updating the password", e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Record a login for the user with the specified email address.
   *
   * @param conn
   *          A database connection.
   * @param email
   *          The user's email address.
   * @throws DatabaseException
   *           If the update fails.
   */
  private static void recordLogin(Connection conn, String email)
    throws DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(email, "email");

    try (
      PreparedStatement stmt = conn.prepareStatement(RECORD_LOGIN_STATEMENT)) {
      stmt.setLong(1, DateTimeUtils.dateToLong(LocalDateTime.now()));
      stmt.setString(2, email);
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("Error recording login", e);
    }

  }

  /**
   * Check a user's email verification code against the supplied code.
   *
   * <p>
   * The method checks that the codes match, and that the code has not expired.
   * The failure modes return different codes to allow the calling method to
   * differentiate them.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param email
   *          The user's email address.
   * @param code
   *          The code to be checked.
   * @return One of {@link #CODE_OK}, {@link #CODE_FAILED}, or
   *         {@link #CODE_EXPIRED}.
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any parameters are missing
   */
  public static int checkEmailVerificationCode(DataSource dataSource,
    String email, String code) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(email, "email");

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
   * Remove the email verification code from a user's record in the database.
   *
   * <p>
   * Note that no checks are made to see if the user exists or actually has a
   * code set - it simply executes the database update statement and returns.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param email
   *          The user's email address.
   * @throws MissingParamException
   *           If any parameters are missing
   * @throws DatabaseException
   *           If an error occurs while updating the database
   *
   * @see #CLEAR_EMAIL_CODE_STATEMENT
   */
  public static void clearEmailVerificationCode(DataSource dataSource,
    String email) throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(email, "email");

    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = dataSource.getConnection();
      stmt = conn.prepareStatement(CLEAR_EMAIL_CODE_STATEMENT);
      stmt.setString(1, email);
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("An error occurred while clearing the code",
        e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
      DatabaseUtils.closeConnection(conn);
    }
  }

  /**
   * Remove the password reset code from a user's record in the database.
   *
   * <p>
   * Note that no checks are made to see if the user exists or actually has a
   * code set - it simply executes the database update statement and returns.
   * </p>
   *
   * @param conn
   *          A database connection.
   * @param email
   *          The user's email address.
   * @throws MissingParamException
   *           If any parameters are missing.
   * @throws DatabaseException
   *           If an error occurs while updating the database.
   *
   * @see #CLEAR_PASSWORD_RESET_CODE_STATEMENT
   */
  public static void clearPasswordResetCode(Connection conn, String email)
    throws MissingParamException, DatabaseException {
    MissingParam.checkMissing(conn, "conn");
    MissingParam.checkMissing(email, "email");

    PreparedStatement stmt = null;

    try {
      stmt = conn.prepareStatement(CLEAR_PASSWORD_RESET_CODE_STATEMENT);
      stmt.setString(1, email);
      stmt.execute();
    } catch (SQLException e) {
      throw new DatabaseException("An error occurred while clearing the code",
        e);
    } finally {
      DatabaseUtils.closeStatements(stmt);
    }
  }

  /**
   * Check a user's password reset code against the supplied code.
   *
   * <p>
   * The method checks that the codes match, and that the code has not expired.
   * The failure modes return different codes to allow the calling method to
   * differentiate them.
   * </p>
   *
   * @param dataSource
   *          A data source.
   * @param email
   *          The user's email address.
   * @param code
   *          The code to be checked.
   * @return One of {@link #CODE_OK}, {@link #CODE_FAILED}, or
   *         {@link #CODE_EXPIRED}.
   * @throws DatabaseException
   *           If a database error occurs
   * @throws MissingParamException
   *           If any parameters are missing
   */
  public static int checkPasswordResetCode(DataSource dataSource, String email,
    String code) throws DatabaseException, MissingParamException {

    MissingParam.checkMissing(dataSource, "dataSource");
    MissingParam.checkMissing(email, "email");

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
   * Checks whether or not two codes match, and that we are within the time
   * limit (defined by {@link #CODE_EXPIRY_HOURS}) of the code's timestamp.
   *
   * @param storedCode
   *          The code that we are checking against (i.e. the one stored in the
   *          database).
   * @param codeTime
   *          The code's timestamp.
   * @param codeToCheck
   *          The code to be checked.
   * @return One of {@link #CODE_OK}, {@link #CODE_FAILED}, or
   *         {@link #CODE_EXPIRED}.
   */
  private static int checkCode(String storedCode, Timestamp codeTime,
    String codeToCheck) {
    int result = CODE_FAILED;

    // If the code is null, the check will fail
    if (null != storedCode && null != codeToCheck) {

      // If the code isn't timestamped (this shouldn't happen),
      // we assume the code has expired.
      if (null == codeTime) {
        result = CODE_EXPIRED;
        // Check the code's timestamp
      } else if (!DateTimeUtils.timeWithinLastHours(codeTime.getTime(),
        CODE_EXPIRY_HOURS)) {
        result = CODE_EXPIRED;
        // See if the code actually matches
      } else if (codeToCheck.equals(storedCode)) {
        result = CODE_OK;
      }
    }

    return result;
  }

  /**
   * Generated a salted and hashed version of a password. The salt is randomly
   * generated and appended to the password before hashing.
   *
   * @param password
   *          The password.
   * @return An object containing the salt and hashed password and salt.
   * @throws NoSuchAlgorithmException
   *           If the hashing algorithm is not supported
   * @throws InvalidKeySpecException
   *           If the key specification is invalid
   *
   * @see PasswordHash
   */
  private static SaltAndHashedPassword generateHashedPassword(char[] password)
    throws NoSuchAlgorithmException, InvalidKeySpecException {
    SaltAndHashedPassword result = new SaltAndHashedPassword();
    // Create the salted, hashed password
    result.salt = PasswordHash.generateSalt();
    result.hashedPassword = PasswordHash.pbkdf2(password, result.salt,
      PasswordHash.PBKDF2_ITERATIONS, PasswordHash.HASH_BYTE_SIZE);

    return result;
  }

  /**
   * Returns a list of all users in the system, ordered by surname/firstname.
   *
   * <p>
   * API users are excluded from the list.
   * </p>
   *
   * @return The list of users.
   * @throws DatabaseException
   *           If an error occurs while retrieving the users.
   */
  public static LinkedHashMap<Long, String> getUserNames(DataSource dataSource)
    throws DatabaseException {
    LinkedHashMap<Long, String> users = new LinkedHashMap<Long, String>();

    MissingParam.checkMissing(dataSource, "dataSource");

    try (Connection conn = dataSource.getConnection();
      PreparedStatement stmt = conn.prepareStatement(USER_NAMES_STATEMENT);
      ResultSet records = stmt.executeQuery();) {

      while (records.next()) {

        int permissions = records.getInt(4);

        if (!User.hasApiPermission(permissions)) {
          long id = records.getLong(1);
          String firstName = records.getString(2);
          String surname = records.getString(3);

          users.put(id, surname + ", " + firstName);
        }
      }
    } catch (SQLException e) {
      throw new DatabaseException("Error getting user names", e);
    }

    return users;
  }

  /**
   * Utility class for handling generated salts and hashed passwords. They are
   * always generated together, so they belong together.
   */
  private static class SaltAndHashedPassword {

    /**
     * The salt
     */
    private byte[] salt;

    /**
     * The hashed password
     */
    private byte[] hashedPassword;
  }
}
