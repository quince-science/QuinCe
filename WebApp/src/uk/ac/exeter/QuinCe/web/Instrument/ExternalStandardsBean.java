package uk.ac.exeter.QuinCe.web.Instrument;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.GasStandard;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.ExternalStandardDB;

/**
 * Bean for gas standards
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class ExternalStandardsBean extends CalibrationBean {

	/**
	 * The navigation string for the gas standards list
	 */
	private static final String NAV_LIST = "external_standards";
	
	/**
	 * The gas standard being edited by the user
	 */
	private GasStandard enteredStandard = null;
	
	/**
	 * The gas standards database utility class
	 */
	private ExternalStandardDB db = null;
	
	/**
	 * Constructor
	 */
	public ExternalStandardsBean() {
		super();
		db = ExternalStandardDB.getInstance();
	}
	
	@Override
	protected String getListNavigation() {
		return NAV_LIST;
	}
	
	@Override
	protected void createEnteredCalibration() {
		enteredStandard = new GasStandard(instrumentId);
	}

	@Override
	public GasStandard getEnteredCalibration() {
		return enteredStandard;
	}

	@Override
	protected CalibrationDB getDbInstance() {
		return db;
	}
	
	@Override
	protected String getCalibrationType() {
		return ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE;
	}
}
