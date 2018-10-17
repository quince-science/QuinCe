package uk.ac.exeter.QuinCe.web.User;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

@ManagedBean
@ViewScoped
public class ResetPasswordBean extends BaseManagedBean {

  /**
   * The URL for the password reset code verification bean
   */
  public static final String PATH = "/user/reset_password.jsf";

  /**
   * The user parameter
   */
  public static final String USER_PARAM = "email";

  /**
   * The code parameter
   */
  public static final String CODE_PARAM = "code";

  /**
   * Navigation string for a successful password change
   */
  public static final String PASSWORD_CHANGED = "PasswordChanged";

  /**
   * The result of the verification
   */
  private boolean verified = false;

  /**
   * The user's email address
   */
  private String email = null;

  /**
   * The first entered password
   */
  private String password1 = null;

  /**
   * The repeated password
   */
  private String password2 = null;

  /**
   * Verify the password reset code.
   */
  public void verifyCode() {

    if (!verified) {
      try {
        int codeOK = UserDB.checkPasswordResetCode(ServletUtils.getDBDataSource(), getRequestParameter(USER_PARAM), getRequestParameter(CODE_PARAM));
        verified = (codeOK == UserDB.CODE_OK);

        switch (codeOK) {
        case UserDB.CODE_OK: {
          email = getRequestParameter(USER_PARAM);
          verified = true;
          break;
        }
        default:
          // All other outcomes
          verified = false;
        }

      } catch (MissingParamException e) {
        verified = false;
      } catch (Exception e) {
        directNavigate(internalError(e));
      }
    }
  }

  public boolean getVerified() {
    return verified;
  }

  public String getEmail() {
    return email;
  }

  /**
   * Get the first password
   * @return The first password
   */
  public String getPassword1() {
    return password1;
  }

  /**
   * Set the first password
   * @param password1 The first password
   */
  public void setPassword1(String password1) {
    // Note that passwords aren't trimmed
    this.password1 = password1;
  }

  /**
   * Get the second password
   * @return The second password
   */
  public String getPassword2() {
    return password2;
  }

  /**
   * Set the second password
   * @param password2 The second password
   */
  public void setPassword2(String password2) {
    // Note that passwords aren't trimmed
    this.password2 = password2;
  }

  public String changePassword() throws Exception {
    String result = PASSWORD_CHANGED;

    if (!validate()) {
      result = null;
    } else {
      UserDB.changePassword(getDataSource(), UserDB.getUser(getDataSource(), email), password1.toCharArray());
      UserDB.clearPasswordResetCode(getDataSource(), email);
    }

    return result;
  }

  /**
   * Validate the bean's contents
   * @return {@code true} if the contents are valid; {@code false} otherwise.
   */
  private boolean validate() {
    boolean ok = true;

    // Passwords must match
    if (!password1.equals(password2)) {
      ok = false;
      setMessage(getComponentID("password2"), "Passwords must match");
    }

    return ok;
  }
}
