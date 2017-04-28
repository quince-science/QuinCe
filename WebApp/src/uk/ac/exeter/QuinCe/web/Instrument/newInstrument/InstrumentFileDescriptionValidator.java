package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFile;

/**
 * Validator for instrument names. Ensures that the name contains
 * at least one character, and is unique for the current user
 * 
 * @author Steve Jones
 *
 */
public class InstrumentFileDescriptionValidator implements Validator {

	@Override
	public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
		
		String description = ((String) value).trim();
		
		ValueExpression expression = component.getValueExpression("bean");
		if (null == expression) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_FATAL, "BEAN ATTRIBUTE MISSING", "BEAN ATTRIBUTE MISSING"));
		}
		
		Object beanAttributeValue = expression.getValue(context.getELContext());
		if (!(beanAttributeValue instanceof NewInstrumentBean)) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_FATAL, "BEAN ATTRIBUTE NOT OF THE CORRECT TYPE", "BEAN ATTRIBUTE NOT OF THE CORRECT TYPE"));
		}
		
		NewInstrumentBean bean = (NewInstrumentBean) beanAttributeValue;
		
		for (InstrumentFile file : bean.getInstrumentFiles()) {
			if (file != bean.getCurrentInstrumentFile()) {
				if (description.equalsIgnoreCase(file.getFileDescription())) {
					throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "This description is already being used by another file", "This description is already being used by another file"));
				}
			}
		}
	}
}
