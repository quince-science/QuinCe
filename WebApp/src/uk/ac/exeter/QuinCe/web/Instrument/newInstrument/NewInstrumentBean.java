package uk.ac.exeter.QuinCe.web.Instrument.newInstrument;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
	private InstrumentFileBuilder currentInstrumentFile;
	
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
	 * Navigate to the files step.
	 * 
	 * <p>
	 *   The page we navigate to depends on the current status of the instrument.
	 * <p>
	 * 
	 * <ul>
	 *   <li>
	 *     If no files have been added, we create a new empty file and go to the upload page.
	 *   </li>
	 *   <li>
	 *     If there are any files that have not yet been fully defined, we go to the
	 *     upload page to finish its definition.
	 *   </li>
	 *   <li>
	 *     If all existing files have been fully defined, we go to the variable assignment page.
	 *   </li>
	 * </ul>
	 * 
	 * @return Navigation to the files
	 * @throws InstrumentFileExistsException If the default instrument file has already been added.
	 */
	public String goToFiles() throws InstrumentFileExistsException {
		String result;
		
		if (instrumentFiles.size() == 0) {
			currentInstrumentFile = new InstrumentFileBuilder();
			addInstrumentFile(currentInstrumentFile);
			
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

	/**
	 * Store the uploaded data in the current instrument file
	 */
	@Override
	public void processUploadedFile() {
		currentInstrumentFile.setFileData(new String(getFile().getContents(), StandardCharsets.UTF_8));
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
	private void addInstrumentFile(InstrumentFileBuilder file) throws InstrumentFileExistsException {
		boolean added = instrumentFiles.add(file);
		if (!added) {
			throw new InstrumentFileExistsException(file);
		}
	}
	
	/**
	 * Get the instrument file that is currently being worked on
	 * @return The current instrument file
	 */
	public InstrumentFileBuilder getCurrentInstrumentFile() {
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
