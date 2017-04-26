package uk.ac.exeter.QuinCe.web.Instrument;

import java.io.Serializable;

import uk.ac.exeter.QuinCe.web.FileUploadBean;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 *
 */
public class NewInstrumentBean extends FileUploadBean implements Serializable {

	/**
	 * Navigation to start definition of a new instrument
	 */
	public static final String NAV_START = "start";
	
	/**
	 * Navigation when cancelling definition of a new instrument
	 */
	public static final String NAV_CANCEL = "cancel";
	
	/**
	 * Begin a new instrument definition
	 * @return The navigation to the start page
	 */
	public String start() {
		clearAllData();
		return NAV_START;
	}
	
	/**
	 * Cancel the current instrument definition
	 * @return Navigation to the instrument list
	 */
	public String cancel() {
		clearAllData();
		return NAV_CANCEL;
	}
	
	@Override
	protected String getFormName() {
		return "newInstrumentForm";
	}

	@Override
	public void processUploadedFile() {
		// Do Nothing for a moment
	}
	
	/**
	 * Clear all data from the bean ready for a new
	 * instrument to be defined
	 */
	private void clearAllData() {
		
	}
}
