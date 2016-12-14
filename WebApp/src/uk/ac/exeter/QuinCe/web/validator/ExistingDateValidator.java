package uk.ac.exeter.QuinCe.web.validator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.sql.DataSource;
import javax.servlet.http.HttpSession;


import uk.ac.exeter.QuinCe.web.system.ResourceException;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * This validator class checks to see if a selected date
 * exist in its target table. It can be used to ensure that
 * no duplicate dates are entered into that table.
 *  
 * This is an abstract class - the table and field to be searched must be implemented in
 * a concrete subclass using the fields getTable and getField
 * 
 * The search can further be restricted by optionally providing
 * a second field and value in getRestrictionField and getRestrictionValue
 * 
 * The message displayed when an existing date is found is defined in
 * getErrorMessage(), which can also be overridden as required.
 * 
 * If an existing date is being edited, that date must not be detected
 * as a duplicate. Therefore the session attribute defined in ATTR_ALLOWED_DATE
 * can be populated with the edited record's date. That date will then be ignored.
 *   
 * @author Steve Jones
 *
 */
public abstract class ExistingDateValidator implements Validator {

	/**
	 * The message displayed if there is an error retrieving dates from the database
	 */
	private static final String DB_ERROR_MESSAGE = "Database error while checking for existing dates"; 
	
	/**
	 * Session attribute to store a date that is allowed to be duplicated. This is so
	 * that when a date is being edited and remains unchanged it doesn't get flagged.
	 */
	public static final String ATTR_ALLOWED_DATE = "ExistingDateValidator.AllowedDate";
	
	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		
		Date dateValue = (Date) value;
		Date allowedDate = (Date) ((HttpSession) context.getExternalContext().getSession(false)).getAttribute(ATTR_ALLOWED_DATE);
		
		if (null == allowedDate || !allowedDate.equals(dateValue)) { 
		
			// Build the search query string
			StringBuffer existingDateQuery = new StringBuffer();
			existingDateQuery.append("SELECT COUNT(*) FROM ");
			existingDateQuery.append(getTable());
			existingDateQuery.append(" WHERE ");
			existingDateQuery.append(getField());
			existingDateQuery.append(" = ?");
			
			
			if (null != getRestrictionField()) {
				existingDateQuery.append(" AND ");
				existingDateQuery.append(getRestrictionField());
				existingDateQuery.append(" = ?");
			}
			
			DataSource dataSource;
			Connection conn = null;
			PreparedStatement stmt = null;
			
			try {
				dataSource = ServletUtils.getDBDataSource();
				conn = dataSource.getConnection();
				stmt = conn.prepareStatement(existingDateQuery.toString());
				stmt.setDate(1, new java.sql.Date(dateValue.getTime()));
				
				if (null != getRestrictionField()) {
					stmt.setLong(2, getRestrictionValue());
				}
				
				ResultSet records = stmt.executeQuery();
				if (records.next()) {
					int recordCount = records.getInt(1);
					if (recordCount > 0) {
						throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, getErrorMessage(), getErrorMessage()));
					}
				}
			} catch (SQLException|ResourceException e) {
				e.printStackTrace();
				throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, DB_ERROR_MESSAGE, DB_ERROR_MESSAGE));
			} finally {
				if (null != stmt) {
					try {
						stmt.close();
					} catch (SQLException e) {
						// Do nothing
					}
				}
				
				if (null != conn) {
					try {
						conn.close();
					} catch (SQLException e) {
						// Do nothing
					}
				}
			}
		}
	}
	
	/**
	 * Returns the name of the table to be searched.
	 * @return The name of the table to be searched.
	 */
	public abstract String getTable();
	
	/**
	 * Returns the date field in the search table
	 * @return The date field to be searched
	 */
	public abstract String getField();
	
	/**
	 * Returns the error message to be displayed if a date
	 * already exists
	 * @return The error message
	 */
	public abstract String getErrorMessage();

	/**
	 * Returns the field to be used as an additional restriction
	 * @return The field to be used as an additional restriction
	 */
	public String getRestrictionField() {
		return null;
	}
	
	/**
	 * Returns the value to be used for the additional restriction
	 * @return The value to be used for the additional restriction
	 */
	public long getRestrictionValue() {
		return 0;
	}
}
