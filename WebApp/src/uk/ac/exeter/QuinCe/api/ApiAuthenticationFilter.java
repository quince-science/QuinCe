package uk.ac.exeter.QuinCe.api;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.User.User;
import uk.ac.exeter.QuinCe.User.UserDB;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * This class provides authentication checking for the QuinCe REST API services.
 * API calls are restricted by user authentication, and only users with the
 * {@link User#BIT_API_USER} bit set are allowed to make API calls.
 *
 *
 * @author Steve Jones
 *
 */
@WebFilter(servletNames = { "API Servlet" })
public class ApiAuthenticationFilter implements javax.servlet.Filter {

  /**
   * Authenticates the user credentials passed in with an API call, and ensures
   * that the user has permissions to make API calls.
   *
   * <p>
   * If authentication fails, an {@link HttpServletResponse#SC_UNAUTHORIZED}
   * response is sent. If the authentication details are valid, but the user
   * does not have the {@link User#BIT_API_USER} permissions bit set, an
   * {@link HttpServletResponse#SC_FORBIDDEN} is sent.
   * </p>
   *
   * <p>
   * If authentication succeeds and the user has the correct permissions, the
   * request is forwarded for processing.
   * </p>
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
    FilterChain filter) throws IOException, ServletException {

    int result = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    try {
      // Get the authentication passed in HTTP headers parameters
      String authString = ((HttpServletRequest) request)
        .getHeader("Authorization");

      // If the user does not have the right (does not provide any HTTP Basic
      // Auth)
      if (null == authString) {
        result = HttpServletResponse.SC_UNAUTHORIZED;
      } else {
        String[] credentials = extractUserPassword(authString);
        DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
        int authenticationResult = UserDB.authenticate(dataSource,
          credentials[0], credentials[1].toCharArray());
        if (authenticationResult == UserDB.AUTHENTICATE_FAILED) {
          result = HttpServletResponse.SC_UNAUTHORIZED;
        } else {
          User user = UserDB.getUser(dataSource, credentials[0]);
          if (!user.isApiUser()) {
            result = HttpServletResponse.SC_FORBIDDEN;
          } else {
            result = HttpServletResponse.SC_OK;
          }
        }
      }
    } catch (Exception e) {
      result = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      e.printStackTrace();
    }

    if (result == HttpServletResponse.SC_OK) {
      filter.doFilter(request, response);
    } else {
      ((HttpServletResponse) response).setStatus(result);
    }
  }

  /**
   * Performs no action.
   */
  @Override
  public void destroy() {
  }

  /**
   * Performs no action.
   */
  @Override
  public void init(FilterConfig arg0) throws ServletException {
  }

  /**
   * Extract the username and password from a Basic HTTP authorization string.
   *
   * @see <a href=
   *      "https://en.wikipedia.org/wiki/Basic_access_authentication">Basic
   *      access authentication (Wikipedia)</a>
   *
   * @param authString
   *          The HTTP Basic Auth string.
   * @return A two-element {@link java.lang.String} array containing the
   *         username and password.
   */
  private String[] extractUserPassword(String authString) {
    // Remove preamble
    authString = authString.replaceFirst("[B|b]asic ", "");
    String decoded = new String(Base64.getDecoder().decode(authString));
    return decoded.split(":", 2);
  }

}
