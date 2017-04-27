package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.io.Serializable;
import java.util.TreeSet;

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
	public static final String NAV_NAME = "name";
	
	/**
	 * Navigation when cancelling definition of a new instrument
	 */
	public static final String NAV_CANCEL = "cancel";
	
	/**
	 * Navigation to the Upload File page
	 */
	public static final String NAV_FILES = "files";
	
	/**
	 * The name of the new instrument
	 */
	private String instrumentName;
	
	/**
	 * The set of sample files for the instrument definition
	 */
	private TreeSet<InstrumentFile> instrumentFiles;
	
	/**
	 * The sample file that is currently being edited
	 */
	private InstrumentFile currentInstrumentFile;
	
	/**
	 * Begin a new instrument definition
	 * @return The navigation to the start page
	 */
	public String start() {
		clearAllData();
		return NAV_NAME;
	}
	
	/**
	 * Cancel the current instrument definition
	 * @return Navigation to the instrument list
	 */
	public String cancel() {
		clearAllData();
		return NAV_CANCEL;
	}
	
	/**
	 * Navigate to the Name page
	 * @return Navigation to the name page
	 */
	public String goToName() {
		return NAV_NAME;
	}
	
	/**
	 * Navigate to the file upload step
	 * @return Navigation to the file upload step
	 */
	public String goToUploadFiles() {
		return NAV_FILES;
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
		instrumentName = null;
	}
	
	/**
	 * Get the name of the new instrument
	 * @return The instrument name
	 */
	public String getInstrumentName() {
		return instrumentName;
	}
	
	/**
	 * Set the name of the new instrument
	 * @param instrumentName The instrument name
	 */
	public void setInstrumentName(String instrumentName) {
		this.instrumentName = instrumentName;
	}
}
