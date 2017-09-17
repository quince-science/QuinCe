package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
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
	 * The instruments owned by the user
	 */
	private List<InstrumentStub> instruments;
	
	/**
	 * The complete record of the current full instrument
	 */
	private Instrument currentFullInstrument = null;
	
	/**
	 * The file definition to use for the uploaded file
	 */
	private FileDefinition fileDefinition = null;
	
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
			
			currentFullInstrument = null;
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
		return NAV_UPLOAD;
	}
	
	/**
	 * Extract and process the uploaded file's contents
	 */
	public void extractFile() {
		try {
			if (null == currentFullInstrument) {
				currentFullInstrument = InstrumentDB.getInstrument(getDataSource(), getCurrentInstrument(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
			}
			
			FileDefinitionBuilder guessedFileLayout = new FileDefinitionBuilder(currentFullInstrument.getFileDefinitions());
			guessedFileLayout.setFileContents(fileLines);
			guessedFileLayout.guessFileLayout();
			
			matchedFileDefinitions = currentFullInstrument.getFileDefinitions().getMatchingFileDefinition(guessedFileLayout);
			if (matchedFileDefinitions.size() > 0) {
				fileDefinition = matchedFileDefinitions.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the name of the matched file definition
	 * @return The file definition name
	 */
	public String getFileDefinition() {
		String result = null;
		
		if (null != fileDefinition) {
			result = fileDefinition.getFileDescription();
		}
		
		return result;
	}
	
	/**
	 * Set the file definition for the uploaded file
	 * @param fileDescription The file description
	 */
	public void setFileDefinition(String fileDescription) {
		this.fileDefinition = currentFullInstrument.getFileDefinitions().get(fileDescription);
	}
	
	/**
	 * Get the list of file definitions that match the uploaded file
	 * @return The matched file definitions
	 */
	public List<FileDefinition> getMatchedFileDefinitions() {
		return matchedFileDefinitions;
	}
}
