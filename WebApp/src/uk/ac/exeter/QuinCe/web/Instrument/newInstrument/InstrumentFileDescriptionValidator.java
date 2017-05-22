package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;

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
				
		for (FileDefinition file : bean.getInstrumentFiles()) {
			if (file != bean.getCurrentInstrumentFile()) {
				if (description.equalsIgnoreCase(file.getFileDescription())) {
					throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "This description is already being used by another file", "This description is already being used by another file"));
				}
			}
		}
	}
}
