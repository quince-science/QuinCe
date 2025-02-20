package uk.ac.exeter.QuinCe.User;

import java.sql.Timestamp;
import java.util.Objects;

import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Object to represent a user and perform various user-related tasks.
 */
public class User {

  /**
   * Permissions bit for administrators
   */
  public static final int BIT_ADMIN_USER = 1 << 0;

  /**
   * Permissions bit for API users. Users with this bit set cannot log in to the
   * main site.
   *
   * Users without this bit set cannot make API calls
   */
  public static final int BIT_API_USER = 1 << 1;

  /**
   * Permissions bit for Approval users.
   *
   * Users with this bit can approve datasets for export
   */
  public static final int BIT_APPROVAL_USER = 1 << 2;

  /**
   * The user's database record ID
   */
  private long databaseId;

  /**
   * The user's email address
   */
  private String emailAddress;

  /**
   * The user's first/given name
   */
  private String givenName;

  /**
   * The user's surname/family name
   */
  private String surname;

  /**
   * The email verification code. If this is set, the user should not be able to
   * log in until the verification is complete.
   */
  private String emailVerificationCode = null;

  /**
   * The time at which the email verification code was set
   */
  private Timestamp emailVerificationCodeTime = null;

  /**
   * The password reset code. If this is set, the user should not be able to log
   * in until the password has been changed.
   */
  private String passwordResetCode = null;

  /**
   * The time at which the password reset code was set
   */
  private Timestamp passwordResetCodeTime = null;

  /**
   * The user's permissions
   */
  private int permissions = 0;

  /**
   * The user's preferences
   */
  private UserPreferences preferences;

  /**
   * Construct a User object
   *
   * @param databaseId
   *          The database record ID for the user
   * @param emailAddress
   *          The user's email address
   * @param givenName
   *          The user's given name
   * @param surname
   *          The user's surname
   * @param permissions
   *          The user's permissions bit mask
   * @param preferences
   *          The user's preferences
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  public User(long databaseId, String emailAddress, String givenName,
    String surname, int permissions, String preferences)
    throws MissingParamException {

    MissingParam.checkMissing(emailAddress, "email");
    MissingParam.checkMissing(givenName, "givenName");
    MissingParam.checkMissing(surname, "surname");
    MissingParam.checkZeroPositive(permissions, "permissions");

    this.databaseId = databaseId;
    this.emailAddress = emailAddress;
    this.givenName = givenName;
    this.surname = surname;
    this.permissions = permissions;
    this.preferences = new UserPreferences(databaseId, preferences);
  }

  /**
   * Return the user's email address
   *
   * @return The user's email address
   */
  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * Return the user's full name, constructed by combining the first name and
   * surname.
   *
   * @return The user's full name
   */
  public String getFullName() {
    return givenName + " " + surname;
  }

  /**
   * Return the user's given name
   *
   * @return The user's given name
   */
  public String getGivenName() {
    return givenName;
  }

  /**
   * Return the user's surname
   *
   * @return The user's surname
   */
  public String getSurname() {
    return surname;
  }

  /**
   * Set the email verification code
   *
   * @param code
   *          The email verification code
   * @param time
   *          The time that the code was created
   */
  public void setEmailVerificationCode(String code, Timestamp time) {
    emailVerificationCode = code;
    emailVerificationCodeTime = time;
  }

  /**
   * Set the password reset code
   *
   * @param code
   *          The password reset code
   * @param time
   *          The time that the code was created
   */
  public void setPasswordResetCode(String code, Timestamp time) {
    passwordResetCode = code;
    passwordResetCodeTime = time;
  }

  /**
   * Retrieve the email verification code
   *
   * @return The email verification code
   */
  public String getEmailVerificationCode() {
    return emailVerificationCode;
  }

  /**
   * Return the time at which the email verification code was set
   *
   * @return The time at which the email verification code was set
   */
  public Timestamp getEmailVerificationCodeTime() {
    return emailVerificationCodeTime;
  }

  /**
   * Retrieve the password reset code
   *
   * @return code The password reset code
   */
  public String getPasswordResetCode() {
    return passwordResetCode;
  }

  /**
   * Return the time at which the password reset code was set
   *
   * @return The time at which the password reset code was set
   */
  public Timestamp getPasswordResetCodeTime() {
    return passwordResetCodeTime;
  }

  /**
   * Return the database ID for this user. Note that this should not be used for
   * any user-facing activities.
   *
   * @return The user's database ID
   */
  public long getDatabaseID() {
    return databaseId;
  }

  /**
   * Sets the database ID for this user object
   *
   * @param id
   *          The user's ID
   */
  public void setDatabaseID(int id) {
    databaseId = id;
  }

  /**
   * Determines whether or not this user is a job manager
   *
   * @return {@code true} if this user is a job manager; {@code false} if not
   */
  public boolean getJobManager() {
    return isAdminUser();
  }

  /**
   * Get the user's preferences
   *
   * @return The user's preferences
   */
  public UserPreferences getPreferences() {
    return preferences;
  }

  /**
   * Determine whether or not this is an administrator user
   *
   * @return {@code true} if this user is an administrator; {@code false} if not
   */
  public boolean isAdminUser() {
    return (permissions & BIT_ADMIN_USER) > 0;
  }

  /**
   * Determine whether or not this is an API user
   *
   * @return {@code true} if this user can access the API; {@code false} if not
   */
  public boolean isApiUser() {
    return hasApiPermission(this.permissions);
  }

  /**
   * Determine whether or not the specified permissions entry represents an API
   * user.
   *
   * @param permissionsEntry
   *          The permissions for the user.
   * @return {@code true} if this permissions give access to the API;
   *         {@code false} if not
   */
  public static boolean hasApiPermission(int permissionsEntry) {
    return (permissionsEntry & BIT_API_USER) > 0;
  }

  /**
   * Determine whether or not this is an approval user
   *
   * @return {@code true} if this user can approve datasets for export;
   *         {@code false} if not
   */
  public boolean isApprovalUser() {
    return (permissions & BIT_APPROVAL_USER) > 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(databaseId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    User other = (User) obj;
    return databaseId == other.databaseId;
  }
}
