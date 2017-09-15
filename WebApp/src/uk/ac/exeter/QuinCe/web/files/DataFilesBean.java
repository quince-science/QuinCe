package uk.ac.exeter.QuinCe.web.files;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentDB;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentStub;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.FileUploadBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;
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
	
	@Override
	public void processUploadedFile() {
		// We don't immediately do anything in this bean, so we can return messages
		// to the user a quickly as possible
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
	 * @throws RecordNotFoundException If any records are missing from the database
	 * @throws DatabaseException If a database error occurs
	 * @throws MissingParamException If any required parameters are missing
	 * @throws ResourceException If the application configuration cannot be retrieved
	 * @throws InstrumentException If any instrument details are invalid
	 */
	public void extractFile() throws MissingParamException, DatabaseException, RecordNotFoundException, InstrumentException, ResourceException {
		extractFileLines();
		if (null == currentFullInstrument) {
			currentFullInstrument = InstrumentDB.getInstrument(getDataSource(), getCurrentInstrument(), ServletUtils.getResourceManager().getSensorsConfiguration(), ServletUtils.getResourceManager().getRunTypeCategoryConfiguration());
		}
		
	}
}
