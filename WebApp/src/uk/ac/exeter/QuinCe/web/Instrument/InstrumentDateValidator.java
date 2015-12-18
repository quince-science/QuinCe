package uk.ac.exeter.QuinCe.web.Instrument;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.web.validator.ExistingDateValidator;

/**
 * A version of the ExistingDateValidator for instruments.
 * 
 * This abstract class defines the additional restriction on the
 * validator such that the instrument_id field matches the one held
 * as the current instrument in the session.
 * 
 * Specific instance will be defined for the calibrations and standards.
 * 
 * @author Steve Jones
 *
 */
public abstract class InstrumentDateValidator extends ExistingDateValidator {

	@Override
	public String getRestrictionField() {
		return "instrument_id";
	}
	
	@Override
	public long getRestrictionValue(FacesContext context) {
		InstrumentStub instrStub = (InstrumentStub) ((HttpSession) context.getExternalContext().getSession(false)).getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
		return instrStub.getId();
	}
}
