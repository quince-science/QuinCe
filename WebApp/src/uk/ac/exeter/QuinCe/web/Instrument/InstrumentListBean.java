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
	 * The name of the session attribute that holds the ID of the currently selected instrument
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
	 * The navigation to the instrument list
	 */
	protected static final String PAGE_INSTRUMENT_LIST = "instrument_list";
	
	/**
	 * The ID of the instrument chosen from the instrument list
	 */
	private long chosenInstrument;
	
	/**
	 * The name of the chosen instrument
	 */
	private String chosenInstrumentName;
	
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
	
	/**
	 * Store the chosen instrument name and ID in the session
	 */
	private void storeChosenInstrument() {
		getSession().setAttribute(ATTR_CURRENT_INSTRUMENT, new InstrumentStub(chosenInstrument, chosenInstrumentName));
	}

	/**
	 * View the calibrations list page for the chosen instrument
	 * @return The calibrations list page navigation
	 */
	public String viewCalibrations() {
		storeChosenInstrument();
		return PAGE_CALIBRATIONS;
	}
	
	/**
	 * View the gas standards list page for the chosen instrument
	 * @return The gas standards list page navigation
	 */
	public String viewStandards() {
		storeChosenInstrument();
		return PAGE_STANDARDS;
	}
	
	/**
	 * Returns to the instrument list
	 * @return The navigation string for the instrument list
	 */
	public String viewInstrumentList() {
		return InstrumentListBean.PAGE_INSTRUMENT_LIST;
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

	/**
	 * Returns the name of the instrument chosen from the instrument list
	 * @return The instrument name
	 */
	public String getChosenInstrumentName() {
		return chosenInstrumentName;
	}
	
	/**
	 * Sets the name of the instrument chosen from the instrument list
	 * @param chosenInstrument The instrument name
	 */
	public void setChosenInstrumentName(String chosenInstrumentName) {
		this.chosenInstrumentName = chosenInstrumentName;
	}
}
