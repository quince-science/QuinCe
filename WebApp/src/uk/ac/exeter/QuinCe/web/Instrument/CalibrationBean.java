package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationException;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;

/**
 * Bean for handling calibrations
 * @author Steve Jones
 *
 */
public abstract class CalibrationBean extends BaseManagedBean {

	/**
	 * The database ID of the current instrument
	 */
	protected long instrumentId;
	
	/**
	 * The name of the current instrument
	 */
	private String instrumentName;
	
	/**
	 * List of the most recent calibrations for each target
	 */
	private List<Calibration> currentCalibrations;
	
	/**
	 * Empty constructor
	 */
	public CalibrationBean() {
		
	}
	
	/**
	 * Initialise the bean
	 * @return The navigation string
	 */
	public String start() {
		String nav = getListNavigation();

		boolean ok = true;
		
		// Get the instrument ID
		if (instrumentId == 0) {
			nav = internalError(new MissingParamException("instrumentId"));
			ok = false;
		}

		if (ok) {
			if (null == instrumentName || instrumentName.trim().length() == 0) {
				nav = internalError(new MissingParamException("instrumentName"));
				ok = false;
			}
		}
		
		if (ok) {
			try {
				createEnteredCalibration();
				loadCurrentCalibrations();
			} catch (Exception e) {
				nav = internalError(e);
			}
		}

		return nav;
	}
	
	/**
	 * Get the instrument's database ID
	 * @return The instrument ID
	 */
	public long getInstrumentId() {
		return instrumentId;
	}
	
	/**
	 * Set the database ID of the instrument
	 * @param instrumentId The instrument ID
	 */
	public void setInstrumentId(long instrumentId) {
		this.instrumentId = instrumentId;
	}
	
	/**
	 * Get the instrument name
	 * @return The instrument name
	 */
	public String getInstrumentName() {
		return instrumentName;
	}
	
	/**
	 * Set the instrument name
	 * @param instrumentName The instrument name
	 */
	public void setInstrumentName(String instrumentName) {
		this.instrumentName = instrumentName;
	}
	
	/**
	 * Get the calibration object for new calibrations entered on the page
	 * @return The entered calibration
	 */
	public abstract Calibration getEnteredCalibration();
	
	/**
	 * Create a new, empty calibration object ready to be populated
	 */
	protected abstract void createEnteredCalibration();
	
	/**
	 * Get the navigation string that will navigate to
	 * the list of calibrations
	 * @return The list navigation string
	 */
	protected abstract String getListNavigation();
	
	/**
	 * Get a list of all possible targets for the calibration type
	 * @return The targets
	 * @throws Exception If the list of targets cannot be retrieved
	 */
	public List<String> getTargets() throws Exception {
		return getDbInstance().getTargets(getDataSource(), instrumentId);
	};
	
	/**
	 * Store the entered calibration in the database
	 * @return The navigation
	 */
	public String addCalibration() {
		String nav = getListNavigation();
		
		try {
			storeEnteredCalibration();
			loadCurrentCalibrations();
			createEnteredCalibration();
		} catch (Exception e) {
			nav = internalError(e);
		}

		return nav;
	}
	
	/**
	 * Store the entered calibration in the database
	 * @throws Exception If the calibration cannot be stored
	 */
	protected void storeEnteredCalibration() throws Exception {
		getDbInstance().addCalibration(getDataSource(), getEnteredCalibration());
	}
	
	/**
	 * Get an instance of the database interaction class for
	 * the calibrations
	 * @return The database interaction instance
	 */
	protected abstract CalibrationDB getDbInstance();
	
	/**
	 * Load the most recent calibrations from the database
	 * @throws RecordNotFoundException If any required database records are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws CalibrationException If the calibrations are internally inconsistent
	 * @throws MissingParamException If any internal calls are missing required parameters
	 */
	private void loadCurrentCalibrations() throws MissingParamException, CalibrationException, DatabaseException, RecordNotFoundException {
		currentCalibrations = getDbInstance().getCurrentCalibrations(getDataSource(), instrumentId);
	}
	
	/**
	 * Get the current calibrations
	 * @return The current calibrations
	 * @throws RecordNotFoundException If any required database records are missing
	 * @throws DatabaseException If a database error occurs
	 * @throws CalibrationException If the calibrations are internally inconsistent
	 * @throws MissingParamException If any internal calls are missing required parameters
	 */
	public List<Calibration> getCurrentCalibrations() throws MissingParamException, CalibrationException, DatabaseException, RecordNotFoundException {
		if (null == currentCalibrations) {
			loadCurrentCalibrations();
		}
		
		return currentCalibrations;
	}
	
	/**
	 * Get the calibration type for the calibrations being edited
	 * @return The calibration type
	 */
	protected abstract String getCalibrationType();
}
