package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

public class FileDescriptionValidator extends NewInstrumentValidator {

  @Override
  protected void doValidation(NewInstrumentBean bean, Object value)
    throws ValidatorException {

    String description = ((String) value).trim();

    if (bean.getInstrumentFiles().containsFileDescription(description)) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
        "This description is already being used by another file",
        "This description is already being used by another file"));
    }
  }

}
