package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.Date;
import java.util.List;

import uk.ac.exeter.QuinCe.data.CalibrationCoefficients;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.database.DatabaseUtils;
import uk.ac.exeter.QuinCe.database.Instrument.CalibrationDB;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ServletUtils;

/**
 * Bean for handling viewing and entry of sensor calibrations
 * @author Steve Jones
 *
 */
public class CalibrationsBean extends BaseManagedBean {

	/**
	 * Navigation to the calibration editor
	 */
	private static final String PAGE_CALIBRATION_EDITOR = "calibrationEditor";
	
	/**
	 * The list of calibration coefficients to be entered.
	 * There will be one entry for each of the instrument's sensors
	 */
	private List<CalibrationCoefficients> coefficients;
	
	/**
	 * The date of the calibration
	 */
	private Date calibrationDate;
	
	/**
	 * The ID of the calibration record being edited. If this is a new
	 * calibration, this will be NO_DATABASE_RECORD.
	 */
	private long calibrationId = DatabaseUtils.NO_DATABASE_RECORD;
	
	/**
	 * Empty constructor
	 */
	public CalibrationsBean() {
		// Do nothing
	}
	
	/**
	 * Clear the bean's data
	 */
	private void clearData() {
		calibrationDate = null;
		calibrationId = DatabaseUtils.NO_DATABASE_RECORD;
		coefficients = null;
	}
	
	/**
	 * Set up a new, empty calibration and navigate to the editor
	 * and set the date to today.
	 * @return The navigation result
	 */
	public String newCalibration() {
		try {
			calibrationId = DatabaseUtils.NO_DATABASE_RECORD;
			calibrationDate = new Date();
			InstrumentStub instrStub = (InstrumentStub) getSession().getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
			Instrument instrument = instrStub.getFullInstrument();
			coefficients = CalibrationCoefficients.initCalibrationCoefficients(instrument);
		} catch (Exception e) {
			internalError(e);
		}

		return PAGE_CALIBRATION_EDITOR;
	}
	
	/**
	 * Store a calibration in the database
	 * @return The navigation back to the calibrations list
	 */
	public String saveCalibration() {
		String result = InstrumentListBean.PAGE_CALIBRATIONS;
		
		try {
			InstrumentStub instrStub = (InstrumentStub) getSession().getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
			CalibrationDB.addCalibration(ServletUtils.getDBDataSource(), instrStub.getId(), calibrationDate, coefficients);
		} catch (Exception e) {
			result = internalError(e);
		} finally {
			clearData();
		}

		return result;
}
	
	/**
	 * Cancels an edit action and returns to the calibrations list
	 * @return The navigation result
	 */
	public String cancelEdit() {
		return InstrumentListBean.PAGE_CALIBRATIONS;
	}
	
	//////// *** GETTERS AND SETTERS *** ////////
	
	/**
	 * Returns the list of calibration coefficient objects
	 * @return The list of calibration coefficient objects
	 */
	public List<CalibrationCoefficients> getCoefficients() {
		return coefficients;
	}
	
	/**
	 * Returns the calibration date
	 * @return The calibration date
	 */
	public Date getCalibrationDate() {
		return calibrationDate;
	}
	
	/**
	 * Sets the calibration date
	 * @param calibrationDate The calibration date
	 */
	public void setCalibrationDate(Date calibrationDate) {
		this.calibrationDate = calibrationDate;
	}
	
	/**
	 * Returns a Date object representing today. Used
	 * to limit the calibration date picker.
	 * @return A Date object representing today
	 */
	public Date getToday() {
		return new Date();
	}
}
