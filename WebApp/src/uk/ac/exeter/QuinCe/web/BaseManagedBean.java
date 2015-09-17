package uk.ac.exeter.QuinCe.web;

import java.sql.Connection;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import uk.ac.exeter.QuinCe.database.DatabaseException;

/**
 * Base class for managed beans that provides a few useful methods
 * @author Steve Jones
 *
 */
public abstract class BaseManagedBean {

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
	 * Retrieve a database connection from the pool
	 * @return A database connection
	 * @throws DatabaseException If there is an error connecting to the database
	 */
	protected Connection getDBConnection() throws DatabaseException {
		try {
			InitialContext context = new InitialContext();
	        DataSource ds = (DataSource) context.lookup("java:/comp/env/jdbc/QuinCeDB");
	        return ds.getConnection();
		} catch (Exception e) {
			throw new DatabaseException("Error while retrieving database connection from pool", e);
		}
	}

}
