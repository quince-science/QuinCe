package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import uk.ac.exeter.QuinCe.data.Instrument.InstrumentFile;
import uk.ac.exeter.QuinCe.web.FileUploadBean;

/**
 * Bean for collecting data about a new instrument
 * @author Steve Jones
 */
public class NewInstrumentBean extends FileUploadBean {

	/**
	 * Navigation to start definition of a new instrument
	 */
	private static final String NAV_NAME = "name";
	
	/**
	 * Navigation when cancelling definition of a new instrument
	 */
	private static final String NAV_CANCEL = "cancel";
	
	/**
	 * Navigation to the Upload File page
	 */
	private static final String NAV_UPLOAD_FILE = "upload_file";
	
	/**
	 * Navigation to the Assign Variables page
	 */
	private static final String NAV_ASSIGN_VARIABLES = "assign_variables";
	
	/**
	 * The name of the new instrument
	 */
	private String instrumentName;
	
	/**
	 * The set of sample files for the instrument definition
	 */
	private InstrumentFileSet instrumentFiles;
	
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
	 * @throws InstrumentFileExistsException If the default instrument file has already been added.
	 */
	public String goToFiles() throws InstrumentFileExistsException {
		String result;
		
		if (instrumentFiles.size() == 0) {
			InstrumentFile newFile = new InstrumentFile();
			addInstrumentFile(new InstrumentFile());
			currentInstrumentFile = newFile;
			
			result = NAV_UPLOAD_FILE;
		} else {
			if (null == currentInstrumentFile) {
				currentInstrumentFile = instrumentFiles.getFirstUndefinedFile();
			}
			
			if (null == currentInstrumentFile) {
				currentInstrumentFile = instrumentFiles.first();
			}
			
			result = currentInstrumentFile.fileDefined() ? NAV_ASSIGN_VARIABLES : NAV_UPLOAD_FILE; 
		}
		
		return result;
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
		instrumentFiles = new InstrumentFileSet();
		currentInstrumentFile = null;
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

	/** 
	 * Add an instrument file to the collection
	 * @param file The file to be added
	 * @throws InstrumentFileExistsException If a file with the same description already exists
	 */
	private void addInstrumentFile(InstrumentFile file) throws InstrumentFileExistsException {
		boolean added = instrumentFiles.add(file);
		if (!added) {
			throw new InstrumentFileExistsException(file);
		}
	}
	
	/**
	 * Get the instrument file that is currently being worked on
	 * @return The current instrument file
	 */
	public InstrumentFile getCurrentInstrumentFile() {
		return currentInstrumentFile;
	}
	
	/**
	 * Retrieve the full set of instrument files
	 * @return The instrument files
	 */
	public InstrumentFileSet getInstrumentFiles() {
		return instrumentFiles;
	}
	
	/**
	 * Determines whether or not the file set contains more than one file
	 * @return {@code true} if more than one file is in the set; {@code false} if there are zero or one files
	 */
	public boolean getHasMultipleFiles() {
		return (instrumentFiles.size() > 1);
	}
}
