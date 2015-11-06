package uk.ac.exeter.QuinCe.web.User;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LogoutServlet extends HttpServlet {

	/**
	 * Destroy the current session and go to the login page
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.getSession().invalidate();
		response.sendRedirect(request.getContextPath());
	}
	
	/**
	 * In case we get a POST request to logout - just do the get
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doGet(request, response);
	}
	
}
