package uk.ac.exeter.QuinCe.web;

/**
 * A small bean that lives in the session, which receives
 * empty requests to keep user sessions alive
 * @author Steve Jones
 *
 */
public class KeepAliveBean {

	/**
	 * An action method that pages can call to keep the user session alive.
	 * The method does nothing.
	 */
	public void sessionTouch() {
		// Do nothing
	}
}
