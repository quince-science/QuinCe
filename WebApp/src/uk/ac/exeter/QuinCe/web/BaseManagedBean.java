package uk.ac.exeter.QuinCe.web;

import javax.faces.application.ConfigurableNavigationHandler;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.User.LoginBean;

/**
 * Base class for managed beans that provides a few useful methods
 * @author Steve Jones
 *
 */
public abstract class BaseManagedBean {

	public static final String SUCCESS_RESULT = "Success";
	
	public static final String INTERNAL_ERROR_RESULT = "InternalError";
	
	public static final String VALIDATION_FAILED_RESULT = "ValidationFailed";
	
	/**
	 * Set a message that can be displayed to the user on a form
	 * @param componentID The component ID (e.g. {@code form:inputName})
	 * @param messageString The message string
	 */
	protected void setMessage(String componentID, String messageString) {
		FacesContext context = FacesContext.getCurrentInstance();
		FacesMessage message = new FacesMessage();
		message.setSeverity(FacesMessage.SEVERITY_ERROR);
		message.setSummary(messageString);
		message.setDetail(messageString);
		context.addMessage(componentID, message);
	}
	
	/**
	 * Generates a JSF component ID for a given form input name
	 * @param componentName The form input name
	 * @return
	 */
	protected String getComponentID(String componentName) {
		return getFormName() + ":" + componentName;
	}
	
	/**
	 * Register an internal error. This places the error
	 * stack trace in the messages, and sends back a result
	 * that will redirect the application to the internal error page.
	 * 
	 * This should only be used for errors that can't be handled properly,
	 * e.g. database failures and the like.
	 * 
	 * @param error The error
	 * @return A result string that will direct to the internal error page.
	 */
	public String internalError(Throwable error) {
		setMessage("STACK_TRACE", StringUtils.stackTraceToString(error));
		if (null != error.getCause()) {
			setMessage("CAUSE_STACK_TRACE", StringUtils.stackTraceToString(error.getCause()));
		}
		return INTERNAL_ERROR_RESULT;
	}
	
	/**
	 * Retrieve a parameter from the request
	 * @param paramName The name of the parameter to retrieve
	 * @return The parameter value
	 */
	public String getRequestParameter(String paramName) {
		return (String) FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(paramName);
	}
	
	public HttpSession getSession() {
		return (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
	}
	
	/**
	 * Directly navigate to a given result
	 * @param navigation The navigation result
	 */
	public void directNavigate(String navigation) {
		ConfigurableNavigationHandler nav = (ConfigurableNavigationHandler) FacesContext.getCurrentInstance().getApplication().getNavigationHandler();
		nav.performNavigation(navigation);
	}
	
	/**
	 * Evaluate and EL expression and return its value
	 * @param expression The EL expression
	 * @return The result of evaluating the expression
	 */
	public String getELValue(String expression) {
		FacesContext context = FacesContext.getCurrentInstance();
		return context.getApplication().evaluateExpressionGet(context, expression, String.class);
	}
	
	/**
	 * Returns the User object for the current session
	 * @return The User object
	 */
	public User getUser() {
		return (User) getSession().getAttribute(LoginBean.USER_SESSION_ATTR);
	}
	
	protected String getFormName() {
		return "DEFAULT_FORM";
	}
}
