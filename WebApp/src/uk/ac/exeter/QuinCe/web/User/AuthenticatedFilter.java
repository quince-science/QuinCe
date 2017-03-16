package uk.ac.exeter.QuinCe.web.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.ResourceHandler;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import uk.ac.exeter.QuinCe.data.User;

/**
 * Filter for all URLs, to determine whether or not the user is logged in.
 * Exceptions are:
 * <ul>
 *   <li>Login page</li>
 *   <li>Signup page</li>
 *   <li>Email Verification Page</li>
 *   <li>Password Reset page</li>
 * </ul>
 * 
 * <p>Based on code from stackoverflow (see the See Also below).</p>
 * 
 * @see <a href="http://stackoverflow.com/questions/8480100/how-implement-a-login-filter-in-jsf">How implement a login filter in JSF?</a> 
 * 
 * @author Steve Jones
 *
 */
@WebFilter("*")
public class AuthenticatedFilter implements Filter {

	/**
	 * The list of paths that can be accessed without being logged in. 
	 */
	private List<String> allowedPaths = new ArrayList<String>();
	private List<String> resourcePaths = new ArrayList<String>();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);

        // Get the user's email address from the session (if possible)
        User user = (session != null) ? (User) session.getAttribute(LoginBean.USER_SESSION_ATTR) : null;

        if (user != null || isResourceRequest(request) || isAllowedPath(request)) {
        	filterChain.doFilter(request, response);
        } else {
        	session.setAttribute("SESSION_EXPIRED", "true");
            response.sendRedirect(request.getContextPath());
        }
	}
	
	/**
	 * Loop through the list of resource paths to determine whether or not this is a resource request
	 * @param request The request
	 * @return {@code true} if the request is a resource request; {@code false} otherwise.
	 */
	private boolean isResourceRequest(HttpServletRequest request) {
		boolean result = false;
		
		for (String resourcePath : resourcePaths) {
			if (request.getRequestURI().startsWith(request.getContextPath() + resourcePath)) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Loop through the list of allowed paths, seeing if any match the current request
	 * @param request The request
	 * @return {@code true} if the request is allowed without being logged in; {@code false} otherwise.
	 */
	private boolean isAllowedPath(HttpServletRequest request) {
		boolean allowed = false;

		if (request.getRequestURI().equals(request.getContextPath() + "/")) {
			allowed = true;
		} else {
			for (String allowedPath: allowedPaths) {
				String pathURIBase = request.getContextPath() + allowedPath;
				
				boolean pathMatched = false;
				
				if (request.getRequestURI().equals(pathURIBase + ".jsf")) {
					pathMatched = true;
				} else if (request.getRequestURI().equals(pathURIBase + ".xhtml")) {
					pathMatched = true;
				}
				
				if (pathMatched) {
					allowed = true;
					break;
				}
			}
		}
		
		return allowed;
	}

	/**
	 * Initialisation - set up the list of paths that can be accessed without being logged in.
	 */
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		allowedPaths.add("/index");
		allowedPaths.add("/user/signup");
		allowedPaths.add("/user/signup_complete");
		allowedPaths.add("/user/verify_email");
		
		resourcePaths.add(ResourceHandler.RESOURCE_IDENTIFIER);
		resourcePaths.add("/resources");
	}

	/**
	 * Destruction - nothing needs doing
	 */
	@Override
	public void destroy() {
		// Do nothing
	}

}
