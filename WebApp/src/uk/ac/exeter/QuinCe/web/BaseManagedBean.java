package uk.ac.exeter.QuinCe.web;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Base class for managed beans that provides a few useful methods
 * @author Steve Jones
 *
 */
public abstract class BaseManagedBean {

	public static final String SUCCESS_RESULT = "Success";
	
	public static final String INTERNAL_ERROR_RESULT = "InternalError";
	
	public static final String VALIDATION_FAILED_RESULT = "ValidationFailed";
	
	protected static String FORM_NAME = "DEFAULT_FORM";
		
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
		return FORM_NAME + ":" + componentName;
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
		return INTERNAL_ERROR_RESULT;
	}
	
	/**
	 * Retrieve a parameter from the request
	 * @param paramName The name of the parameter to retrieve
	 * @return The parameter value
	 */
	public String getRequestParameter(String paramName) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		return (String) facesContext.getExternalContext().getRequestParameterMap().get(paramName);
	}
}
