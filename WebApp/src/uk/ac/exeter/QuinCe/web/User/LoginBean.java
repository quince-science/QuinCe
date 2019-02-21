package uk.ac.exeter.QuinCe.web.User;

import java.util.HashMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.User.UserPreferences;
import uk.ac.exeter.QuinCe.jobs.JobManager;
import uk.ac.exeter.QuinCe.jobs.user.SendPasswordResetMailJob;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for handling user logins
 * @author Steve Jones
 *
 */
@ManagedBean
@RequestScoped
public class LoginBean extends BaseManagedBean {

  /**
   * The navigation to the sign-up page to create a new account
   */
  public static final String SIGNUP_RESULT = "SignUp";

  /**
   * The result indicating that authentication failed
   */
  public static final String AUTHENTICATION_FAILED_RESULT = "AuthenticationFailed";

  /**
   * The result indicating that authentication succeeded
   */
  public static final String AUTHENTICATION_OK_RESULT = "AuthenticationSuccess";

  /**
   * Navigation string for the lost password page
   */
  public static final String LOST_PASSWORD_PAGE = "LostPassword";

  /**
   * Navigation string for the lost password confirmation page
   */
  public static final String LOST_PASSWORD_LINK_SENT = "LostPasswordLinkSent";

  /**
   * The session attribute in which the user's details are stored
   */
  public static final String USER_SESSION_ATTR = "User";

  /**
   * The session attribute in which the user's preferences are stored
   */
  public static final String USER_PREFS_ATTR = "UserPrefs";

  /**
   * The entered email address
   */
  private String emailAddress = null;

  /**
   * The entered password
   */
  private String password = null;

  /**
   * Constructor - does nothing
   */
  public LoginBean() {
    // Do nothing
  }

  /**
   * Get the entered email address
   * @return The email address
   */
  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * Set the entered email address
   * @param emailAddress The email address
   */
  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  /**
   * Get the entered password
   * @return The password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the entered password
   * @param password The password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Authenticate the user.
   * @return The authentication result. One of {@link #AUTHENTICATION_OK_RESULT} or
   *         {@link #AUTHENTICATION_FAILED_RESULT}.
   * @see UserDB#authenticate(javax.sql.DataSource, String, char[])
   */
  public String authenticate() {

    String result = AUTHENTICATION_FAILED_RESULT;

    try {
      User user = UserDB.getUser(getDataSource(), emailAddress);
      int authenticateResult;

      if (null != user && user.isApiUser()) {
        // API users are not allowed to log in
        authenticateResult = UserDB.AUTHENTICATE_FAILED;
      } else {
        // Authenticate the user
        authenticateResult = UserDB.authenticate(ServletUtils.getDBDataSource(),
          emailAddress, password.toCharArray());
      }

      switch (authenticateResult) {
      case UserDB.AUTHENTICATE_OK: {
        getSession().setAttribute(USER_SESSION_ATTR, user);
        UserPreferences prefs = UserDB.getPreferences(getDataSource(), user.getDatabaseID());
        getSession().setAttribute(USER_PREFS_ATTR, prefs);
        result = AUTHENTICATION_OK_RESULT;
        break;
      }
      case UserDB.AUTHENTICATE_FAILED: {
        setMessage(null, "The email address and/or password were not recognised");
        break;
      }
      case UserDB.AUTHENTICATE_EMAIL_CODE_SET: {
        setMessage(null, "Your account has not yet been activated. Please use the "
            + "link in your activation email.");
        break;
      }
      }

    } catch (Exception e) {
      internalError(e);
    }

    getSession().removeAttribute("SESSION_EXPIRED");
    return result;
  }

  /**
   * Navigate to the sign-up page
   * @return The navigation to the sign-up page
   */
  public String signUp() {
    return SIGNUP_RESULT;
  }

  /**
   * Navigate to the Lost Password page
   * @return The navigation to the lost password page
   */
  public String startLostPassword() {
    return LOST_PASSWORD_PAGE;
  }

  /**
   * Generate and send a password reset link
   * @return Navigation to the confirmation page
   */
  public String resetPassword() throws Exception {
    String result = LOST_PASSWORD_LINK_SENT;

    User user = UserDB.getUser(getDataSource(), emailAddress);
    if (null == user) {
      setMessage(null, "The entered email address is not recognised");
      result = null;
    } else {
      UserDB.generatePasswordResetCode(getDataSource(), user);
      Map<String, String> emailJobParams = new HashMap<String, String>(1);
      emailJobParams.put(SendPasswordResetMailJob.EMAIL_KEY, emailAddress);
      JobManager.addInstantJob(ServletUtils.getResourceManager(), ServletUtils.getAppConfig(), user,
          "uk.ac.exeter.QuinCe.jobs.user.SendPasswordResetMailJob", emailJobParams);
    }

    return result;
  }

  @Override
  protected String getFormName() {
    return "loginform";
  }
}
