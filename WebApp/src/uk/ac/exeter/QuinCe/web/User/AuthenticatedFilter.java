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
 * <p>Based on code from stackoverflow.</p>
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
	
	/**
	 * The list of paths that will be identified as resources.
	 * 
	 * <p>
	 *   Resources are things like Javascript files, images, stylesheets etc.
	 *   They do not need checking as they are not security-restricted in terms
	 *   of the application.
	 * </p>
	 */
	private List<String> resourcePaths = new ArrayList<String>();
	
	/**
	 * Checks all requests for application pages to ensure that the user is
	 * logged in. If the user is not logged in, they will be redirected
	 * to the login page with a 'session expired' message.
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);

    	String requestURL = request.getRequestURI();
    	if (requestURL.endsWith("/")) {
    		requestURL = requestURL.substring(0, requestURL.length() - 1);
    	}
    	
        if (null == session) {
        	if (requestURL.equals(request.getContextPath())) {
            	filterChain.doFilter(request, response);
        	} else {
        		response.sendRedirect(request.getContextPath());
        	}
        } else {
            // Get the user's email address from the session (if possible)
            User user = (User) session.getAttribute(LoginBean.USER_SESSION_ATTR);

            if (user != null || isResourceRequest(request) || isAllowedPath(request)) {
            	filterChain.doFilter(request, response);
            } else {
            	if (requestURL.equals(request.getContextPath())) {
                	session.removeAttribute("SESSION_EXPIRED");            		
            	} else {
                	session.setAttribute("SESSION_EXPIRED", "true");
            	}
                response.sendRedirect(request.getContextPath());
            }
        }
	}
	
	/**
	 * Determines whether or not this is a request for a resource rather than
	 * a page of the application.
	 * @param request The request
	 * @return {@code true} if the request is a resource request; {@code false} otherwise.
	 * @see #resourcePaths
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
	 * Determines whether or not a page can be accessed without the user having logged in.
	 * @param request The request
	 * @return {@code true} if the request is allowed without being logged in; {@code false} otherwise.
	 * @see #allowedPaths
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
	 * Sets up the list of resource paths, and pages that can be accessed without being logged in.
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
