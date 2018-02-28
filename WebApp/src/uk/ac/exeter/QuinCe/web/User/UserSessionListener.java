package uk.ac.exeter.QuinCe.web.User;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.User.UserPreferences;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Class to perform user-related actions when a session
 * is created or destroyed
 * @author Steve Jones
 *
 */
public class UserSessionListener implements HttpSessionListener {

  @Override
  public void sessionCreated(HttpSessionEvent event) {
  }

  @Override
  public void sessionDestroyed(HttpSessionEvent event) {
    HttpSession session = event.getSession();

    // Save the user preferences
    try {
      UserPreferences prefs = (UserPreferences) session.getAttribute(LoginBean.USER_PREFS_ATTR);
      if (null != prefs) {
        UserDB.savePreferences(ServletUtils.getDBDataSource(), prefs);
      }
    } catch (MissingParamException | DatabaseException | ResourceException e) {
      /*
       * Just log the error
       */
      e.printStackTrace();
    }

    session.removeAttribute(LoginBean.USER_SESSION_ATTR);
      session.setAttribute("SESSION_EXPIRED", "true");
  }

}
