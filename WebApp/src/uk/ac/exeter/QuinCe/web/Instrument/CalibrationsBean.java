package uk.ac.exeter.QuinCe.web.Instrument;

import java.util.List;

import javax.annotation.PostConstruct;

import uk.ac.exeter.QuinCe.data.CalibrationCoefficients;
import uk.ac.exeter.QuinCe.data.Instrument;
import uk.ac.exeter.QuinCe.data.InstrumentStub;
import uk.ac.exeter.QuinCe.database.DatabaseException;
import uk.ac.exeter.QuinCe.database.RecordNotFoundException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.web.BaseManagedBean;
import uk.ac.exeter.QuinCe.web.system.ResourceException;

/**
 * Bean for handling viewing and entry of sensor calibrations
 * @author Steve Jones
 *
 */
public class CalibrationsBean extends BaseManagedBean {

	/**
	 * The list of calibration coefficients to be entered.
	 * There will be one entry for each of the instrument's sensors
	 */
	private List<CalibrationCoefficients> coefficients;
	
	/**
	 * Initialise the bean with a set of empty calibration
	 * coefficients ready to be filled in
	 * @throws ResourceException 
	 * @throws RecordNotFoundException 
	 * @throws DatabaseException 
	 * @throws MissingParamException 
	 */
	@PostConstruct
	public void init() throws MissingParamException, DatabaseException, RecordNotFoundException, ResourceException {
		InstrumentStub instrStub = (InstrumentStub) getSession().getAttribute(InstrumentListBean.ATTR_CURRENT_INSTRUMENT);
		Instrument instrument = instrStub.getFullInstrument();
		coefficients = CalibrationCoefficients.initCalibrationCoefficients(instrument);
	}
	
	//////// *** GETTERS AND SETTERS *** ////////
	
	/**
	 * Returns the list of calibration coefficient objects
	 * @return The list of calibration coefficient objects
	 */
	public List<CalibrationCoefficients> getCoefficients() {
		return coefficients;
	}
}
