package uk.ac.exeter.QuinCe.web.files;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Files.DataFile;
import uk.ac.exeter.QuinCe.data.Files.DataFileException;
import uk.ac.exeter.QuinCe.data.Files.DataFileMessage;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinitionException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.Instrument.newInstrument.FileDefinitionBuilder;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for handling raw data files
 * @author Steve Jones
 */
@ManagedBean
@SessionScoped
public class DataFilesBean extends FileUploadBean {

	/**
	 * Navigation to the file upload page
	 */
	public static final String NAV_UPLOAD = "upload";
	
	/**
	 * Navigation to the file upload page
	 */
	public static final String NAV_FILE_LIST = "file_list";
	
	/**
	 * The instruments owned by the user
	 */
	private List<InstrumentStub> instruments;
	
	/**
	 * The complete record of the current full instrument
	 */
	private Instrument currentFullInstrument = null;
	
	/**
	 * The data file object
	 */
	private DataFile dataFile = null;
	
	/**
	 * The file definitions that match the uploaded file
	 */
	private List<FileDefinition> matchedFileDefinitions = null;
	
	@Override
	public void processUploadedFile() {
		extractFileLines();
	}

	/**
	 * Initialise/reset the bean
	 */
	@PostConstruct
	public void initialise() {
		// Load the instruments list. Set the current instrument if it isn't already set.
		try {
			instruments = InstrumentDB.getInstrumentList(getDataSource(), getUser());
			if (getCurrentInstrument() == -1 && instruments.size() > 0) {
				setCurrentInstrument(instruments.get(0).getId());
			}
			
			// TODO We don't need to reset the instrument every time -
			// TODO only if the user switches instrument in the menu
			currentFullInstrument = null;
			
			matchedFileDefinitions = null;
			dataFile = null;
		} catch (Exception e) {
			// Fail quietly, but print the log
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the list of instruments owned by the user
	 * @return The list of instruments
	 */
	public List<InstrumentStub> getInstruments() {
		if (null == instruments) {
			initialise();
		}
		
		return instruments;
	}
	
	/**
	 * Get the instrument that the user is currently viewing
	 * @return The current instrument
	 */
	public long getCurrentInstrument() {
		return getUserPrefs().getLastInstrument();
	}
	
	/**
	 * Get the name of the current instrument
	 * @return The instrument name
	 */
	public String getCurrentInstrumentName() {
		String result = null;
		for (InstrumentStub instrument : instruments) {
			if (instrument.getId() == getCurrentInstrument()) {
				result = instrument.getName();
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Set the current instrument
	 * @param currentInstrument The current instrument
	 */
	public void setCurrentInstrument(long currentInstrument) {
		if (getUserPrefs().getLastInstrument() != currentInstrument) {
			getUserPrefs().setLastInstrument(currentInstrument);
			currentFullInstrument = null;
		}
		
	}
	
	/**
	 * Start the file upload procedure
	 * @return Navigation to the upload page
	 */
	public String beginUpload() {
		initialise();
		return NAV_UPLOAD;
	}
	
	/**
	 * Extract and process the uploaded file's contents
	 */
	public void extractFile() {
		matchedFileDefinitions = null;
		dataFile = null;
		
		try {
			if (null == currentFullInstrument) {
				currentFullInstrument = InstrumentDB.getInstrument(getDataSource(), getCurrentInstrument(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
			}
			
			FileDefinitionBuilder guessedFileLayout = new FileDefinitionBuilder(currentFullInstrument.getFileDefinitions());
			guessedFileLayout.setFileContents(fileLines);
			guessedFileLayout.guessFileLayout();
			
			matchedFileDefinitions = currentFullInstrument.getFileDefinitions().getMatchingFileDefinition(guessedFileLayout);
			FileDefinition fileDefinition = null;
			
			if (matchedFileDefinitions.size() == 0) {
				fileDefinition = null;
				setMessage(null, "The format of " + getFilename() + " was not recognised. Please upload a different file.");
			} else {
				fileDefinition = matchedFileDefinitions.get(0);
			}
			// TODO Handle multiple matched definitions

			if (null != fileDefinition) {
				dataFile = new DataFile(fileDefinition, getFilename(), fileLines);
				
				if (dataFile.getMessageCount() > 0) {
					setMessage(null, getFilename() + " could not be processed (see messages below). Please fix these problems and upload the file again.");
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			dataFile = null;
			setMessage(null, "The file could not be processed: " + e.getMessage());
		}
	}
	
	/**
	 * Set the file definition for the uploaded file
	 * @param fileDescription The file description
	 * @throws FileDefinitionException If the file definition does not match the file contents
	 */
	public void setFileDefinition(String fileDescription) throws FileDefinitionException {
		dataFile.setFileDefinition(currentFullInstrument.getFileDefinitions().get(fileDescription));
	}
	
	/**
	 * Get the list of file definitions that match the uploaded file
	 * @return The matched file definitions
	 */
	public List<FileDefinition> getMatchedFileDefinitions() {
		return matchedFileDefinitions;
	}
	
	/**
	 * Return to the file list
	 * @return Navigation to the file list
	 */
	public String goToFileList() {
		return NAV_FILE_LIST;
	}
	
	/**
	 * Get the messages generated for this file as a JSON string
	 * @return The messages in JSON format
	 */
	public String getFileMessages() {
		StringBuilder json = new StringBuilder();
		
		json.append('[');

		if (null != dataFile) {
			
			TreeSet<DataFileMessage> messages = dataFile.getMessages();
		
			int count = 0;
			for (DataFileMessage message : messages) {
				json.append('"');
				json.append(message.toString());
				json.append('"');
				
				if (count < messages.size() - 1) {
					json.append(',');
				}
	
				count++;
			}
		}
		
		json.append(']');
		
		return json.toString();
	}
	
	/**
	 * Get the file format description
	 * @return The file format description
	 */
	public String getFileType() {
		String result = null;
		
		if (null != dataFile) {
			result = dataFile.getFileDescription();
		}
		
		return result;
	}
	
	/**
	 * Get the date of the first record in the file
	 * @return The start date
	 */
	public LocalDateTime getFileStartDate() {
		LocalDateTime result = null;
		
		if (dataFile != null) {
			result = dataFile.getStartDate();
		}
		
		return result;
	}
	
	/**
	 * Get the date of the last record in the file
	 * @return The end date
	 */
	public LocalDateTime getFileEndDate() {
		LocalDateTime result = null;
		
		if (dataFile != null) {
			result = dataFile.getEndDate();
		}
		
		return result;
	}
	
	/**
	 * Get the number of records in the file
	 * @return The record count
	 * @throws DataFileException If the count cannot be calculated
	 */
	public int getFileRecordCount() throws DataFileException {
		int result = -1;
		
		if (dataFile != null) {
			result = dataFile.getRecordCount();
		}
		
		return result;
	}
	
	/**
	 * Dummy method for (not) setting file messages
	 * @param dummy Parameter
	 */
	public void setFileMessages(String dummy) {
		// Do nothing
	}
	
	/**
	 * Dummy method for (not) setting file messages
	 * @param dummy Parameter
	 */
	public void setFileType(String dummy) {
		// Do nothing
	}
	
	/**
	 * Dummy method for (not) setting file messages
	 * @param dummy Parameter
	 */
	public void setFileStartDate(LocalDateTime dummy) {
		// Do nothing
	}
	
	/**
	 * Dummy method for (not) setting file messages
	 * @param dummy Parameter
	 */
	public void setFileEndDate(LocalDateTime dummy) {
		// Do nothing
	}
	
	/**
	 * Dummy method for (not) setting file messages
	 * @param dummy Parameter
	 */
	public void setFileRecordCount(String dummy) {
		// Do nothing
	}
}
