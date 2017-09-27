package uk.ac.exeter.QuinCe.web.Instrument;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.GasStandard;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.GasStandardDB;

/**
 * Bean for gas standards
 * @author Steve Jones
 *
 */
public class GasStandardsBean extends CalibrationBean {

	/**
	 * The navigation string for the gas standards list
	 */
	private static final String NAV_LIST = "gas_standards";
	
	/**
	 * The gas standard being edited by the user
	 */
	private GasStandard enteredStandard = null;
	
	/**
	 * The gas standards database utility class
	 */
	private GasStandardDB db = null;
	
	/**
	 * Constructor
	 */
	public GasStandardsBean() {
		super();
		db = GasStandardDB.getInstance();
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
		return GasStandardDB.GAS_STANDARD_CALIBRATION_TYPE;
	}
}
