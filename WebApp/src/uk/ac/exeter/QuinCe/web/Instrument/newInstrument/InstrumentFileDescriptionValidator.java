package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

/**
 * Validator for instrument names. Ensures that the name contains
 * at least one character, and is unique for the current user
 *
 * @author Steve Jones
 *
 */
public class InstrumentFileDescriptionValidator extends NewInstrumentValidator {

	@Override
	public void doValidation(NewInstrumentBean bean, Object value) throws ValidatorException {

		String description = ((String) value).trim();

		if (bean.getInstrumentFiles().containsFileDescription(description)) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "This description is already being used by another file", "This description is already being used by another file"));
		}
	}
}
