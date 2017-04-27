package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.HttpSession;

import uk.ac.exeter.QuinCe.data.User;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.User.LoginBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Validator for instrument names. Ensures that the name contains
 * at least one character, and is unique for the current user
 * 
 * @author Steve Jones
 *
 */
public class InstrumentNameValidator implements Validator {

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		
		String name = ((String) value).trim();
		
		if (name.length() == 0) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "You must enter a name", "You must enter a name"));
		} else {
			HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
			User user = (User) session.getAttribute(LoginBean.USER_SESSION_ATTR);
			
			try {
				if (InstrumentDB.instrumentExists(ServletUtils.getDBDataSource(), user, name)) {
					throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "An instrument with that name already exists", "An instrument with that name already exists"));
				}
			} catch (Exception e) {
				throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_FATAL, "Unable to check instrument at this time. Please try again later.", "Unable to check instrument at this time. Please try again later."));
			}
		}
	}
}
