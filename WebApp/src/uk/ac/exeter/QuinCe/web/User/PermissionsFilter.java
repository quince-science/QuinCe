package uk.ac.exeter.QuinCe.web.User;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import uk.ac.exeter.QuinCe.User.User;

/**
 * A filter to determine whether a user has a given permission.
 * 
 * <p>
 *   This is an abstract class. Implementing classes will determine
 *   which permission they will check.
 * </p>
 * @author Steve Jones
 *
 */
public abstract class PermissionsFilter implements Filter {

	@Override
	public void destroy() {
		// Do nothing
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);

        User user = (User) session.getAttribute(LoginBean.USER_SESSION_ATTR);
        
        if (hasPermission(user)) {
        	filterChain.doFilter(request, response);
        } else {
        	response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// Do nothing
	}
	
	/**
	 * Determine whether or not the specified user has the permission
	 * being checked by this filter.
	 * @param user The user whose permissions must be checked
	 * @return {@code true} if the user has the permission; {@code false} if they do not
	 */
	public abstract boolean hasPermission(User user);
}
