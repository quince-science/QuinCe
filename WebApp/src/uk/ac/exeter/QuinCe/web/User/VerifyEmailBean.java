package uk.ac.exeter.QuinCe.web.User;

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class VerifyEmailBean extends BaseManagedBean {

  /**
   * The URL for the email code verification bean
   */
  public static final String PATH = "/user/verify_email.xhtml";

  /**
   * The user parameter
   */
  public static final String USER_PARAM = "email";

  /**
   * The code parameter
   */
  public static final String CODE_PARAM = "code";

  /**
   * The result of the verification
   */
  private boolean verified = false;

  /**
   * Verify the email code. If it succeeds, remove the code from the database.
   */
  public void verifyCode() {

    try {
      int codeOK = UserDB.checkEmailVerificationCode(
        ServletUtils.getDBDataSource(), getRequestParameter(USER_PARAM),
        getRequestParameter(CODE_PARAM));

      switch (codeOK) {
      case UserDB.CODE_OK: {
        UserDB.clearEmailVerificationCode(ServletUtils.getDBDataSource(),
          getRequestParameter(USER_PARAM));
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

  public boolean getVerified() {
    return verified;
  }
}
