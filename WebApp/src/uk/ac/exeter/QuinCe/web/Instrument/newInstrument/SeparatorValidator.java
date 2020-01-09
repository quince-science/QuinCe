package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;

/**
 * Check that the chosen separator results in more than 0 columns
 *
 * @author Steve Jones
 *
 */
public class SeparatorValidator extends NewInstrumentValidator {

  @Override
  public void doValidation(NewInstrumentBean bean, Object value)
    throws ValidatorException {

    String separator = ((String) value).trim();

    if (!FileDefinition.validateSeparator(separator)) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
        "Unsupported separator", "Unsupported separator"));

    }

    if (bean.getCurrentInstrumentFile().calculateColumnCount(separator) <= 1) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
        "Cannot extract any columns using the specified separator",
        "Cannot extract any columns using the specified separator"));
    }
  }

}
