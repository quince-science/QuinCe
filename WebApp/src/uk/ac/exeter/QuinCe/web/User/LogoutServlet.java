package uk.ac.exeter.QuinCe.web.User;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import uk.ac.exeter.QuinCe.web.system.ServletUtils;

public class LogoutServlet extends HttpServlet {

	private static final long serialVersionUID = 8157120031497347680L;

	/**
	 * Destroy the current session and go to the login page
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		HttpSession session = request.getSession();
		if (session != null) {
			session.invalidate();
		}
		
		String postLogoutURL = "";
		try {
			postLogoutURL = ServletUtils.getAppConfig().getProperty("app.urlstub");
		} catch (Exception e) {
			// Do nothing
		}
		response.sendRedirect(postLogoutURL);
	}
	
	/**
	 * In case we get a POST request to logout - just do the get
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doGet(request, response);
	}
	
}
