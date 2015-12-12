package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;

import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.database.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for transient instrument operations, e.g.
 * listing instruments.
 * 
 * @author Steve Jones
 *
 */
public class InstrumentListBean extends BaseManagedBean {

	/**
	 * The name of the session attribute that holds the currently selected instrument
	 */
	public static final String ATTR_CURRENT_INSTRUMENT = "currentInstrument";
	
	/**
	 * The navigation for the calibrations page
	 */
	public static final String PAGE_CALIBRATIONS = "calibrations";
	
	/**
	 * The navigation for the standards page
	 */
	public static final String PAGE_STANDARDS = "standards";
	
	/**
	 * The ID of the instrument chosen from the instrument list
	 */
	private long chosenInstrument;
	
	/**
	 * Returns a list of the instruments owned by the current user
	 * @return The instruments owned by the current user
	 */
	public List<InstrumentStub> getInstrumentList() {
		List<InstrumentStub> instruments = null;
		
		try {
			instruments = InstrumentDB.getInstrumentList(ServletUtils.getDBDataSource(), getUser());
		} catch (Exception e) {
			internalError(e);
		}
		return instruments;
	}
	
	private void storeChosenInstrument() {
		getSession().setAttribute(ATTR_CURRENT_INSTRUMENT, chosenInstrument);
	}
	
	public String viewCalibrations() {
		storeChosenInstrument();
		return PAGE_CALIBRATIONS;
	}
	
	public String viewStandards() {
		storeChosenInstrument();
		return PAGE_STANDARDS;
	}
	
	///////////////// *** GETTERS AND SETTERS *** ///////////////
	
	/**
	 * Returns the ID of the instrument chosen from the instrument list
	 * @return The instrument ID
	 */
	public long getChosenInstrument() {
		return chosenInstrument;
	}
	
	/**
	 * Sets the ID of the instrument chosen from the instrument list
	 * @param chosenInstrument The instrument ID
	 */
	public void setChosenInstrument(long chosenInstrument) {
		this.chosenInstrument = chosenInstrument;
	}
}
