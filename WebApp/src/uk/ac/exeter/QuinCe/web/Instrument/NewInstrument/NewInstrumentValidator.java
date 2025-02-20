package uk.ac.exeter.QuinCe.web.Instrument.NewInstrument;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * Base validator for validation of entries in a new instrument. Extracts the
 * bean and passes it to the concrete validation method
 */
public abstract class NewInstrumentValidator implements Validator {

  @Override
  public final void validate(FacesContext context, UIComponent component,
    Object value) throws ValidatorException {

    ValueExpression expression = component.getValueExpression("bean");
    if (null == expression) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_FATAL,
        "BEAN ATTRIBUTE MISSING", "BEAN ATTRIBUTE MISSING"));
    }

    Object beanAttributeValue = expression.getValue(context.getELContext());
    if (!(beanAttributeValue instanceof NewInstrumentBean)) {
      throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_FATAL,
        "BEAN ATTRIBUTE NOT OF THE CORRECT TYPE",
        "BEAN ATTRIBUTE NOT OF THE CORRECT TYPE"));
    }

    NewInstrumentBean bean = (NewInstrumentBean) beanAttributeValue;
    doValidation(bean, value);
  }

  /**
   * Perform the validation
   *
   * @param bean
   *          The new instrument bean
   * @param value
   *          The value being validated
   * @throws ValidatorException
   *           If the validation fails
   */
  protected abstract void doValidation(NewInstrumentBean bean, Object value)
    throws ValidatorException;
}
