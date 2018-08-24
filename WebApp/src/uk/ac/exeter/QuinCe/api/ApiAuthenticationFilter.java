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
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

@WebFilter(servletNames = {"API Servlet"})
public class ApiAuthenticationFilter implements javax.servlet.Filter {
  public static final String AUTHENTICATION_HEADER = "Authorization";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filter) throws IOException, ServletException {

    int result = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

    try {
      //Get the authentication passed in HTTP headers parameters
      String authString = ((HttpServletRequest) request).getHeader("Authorization");

      //If the user does not have the right (does not provide any HTTP Basic Auth)
      if (null == authString) {
          result = HttpServletResponse.SC_UNAUTHORIZED;
      } else {
        String[] credentials = extractUserPassword(authString);
        DataSource dataSource = ResourceManager.getInstance().getDBDataSource();
        int authenticationResult = UserDB.authenticate(dataSource, credentials[0], credentials[1].toCharArray());
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

  @Override
  public void destroy() {
  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {
  }

  /**
   * Extract the username and password from a Basic HTTP authorization string.
   * @param dataSource A data source
   * @param authString The HTTP Basic Auth string
   * @return One of AUTHENTICATE_OK or AUTHENTICATE_FAILED
   * @throws MissingParamException If any of the parameters are null
   * @throws DatabaseException If an error occurs while retrieving the user's details
   */
  private String[] extractUserPassword(String authString) {
    // Remove preamble
    authString = authString.replaceFirst("[B|b]asic ", "");
    String decoded = new String(Base64.getDecoder().decode(authString));
    return decoded.split(":", 2);
  }

}
