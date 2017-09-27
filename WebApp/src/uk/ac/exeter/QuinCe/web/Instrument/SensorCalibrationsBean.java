package uk.ac.exeter.QuinCe.web.Instrument;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import uk.ac.exeter.QuinCe.data.Instrument.Calibration.Calibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.PolynomialSensorCalibration;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.SensorCalibrationDB;

/**
 * Bean for sensor calibrations.
 * 
 * <p>
 *   This currently uses the PolynomialSensorCalibration exclusively,
 *   because that is the only supported version right now. This will
 *   need to be genericised when the full framework of different calibration
 *   formulae is implemented.
 * </p>
 * 
 * @author Steve Jones
 *
 */
@ManagedBean
@SessionScoped
public class SensorCalibrationsBean extends CalibrationBean {

	/**
	 * The navigation string for the gas standards list
	 */
	private static final String NAV_LIST = "sensor_calibrations";
	
	/**
	 * The gas standard being edited by the user
	 */
	private PolynomialSensorCalibration enteredCalibration = null;
	
	/**
	 * The gas standards database utility class
	 */
	private SensorCalibrationDB db = null;
	
	/**
	 * Constructor
	 */
	public SensorCalibrationsBean() {
		super();
		db = SensorCalibrationDB.getInstance();
	}

	@Override
	public Calibration getEnteredCalibration() {
		return enteredCalibration;
	}

	@Override
	protected void createEnteredCalibration() {
		enteredCalibration = new PolynomialSensorCalibration(instrumentId);
	}

	@Override
	protected String getListNavigation() {
		return NAV_LIST;
	}

	@Override
	protected CalibrationDB getDbInstance() {
		return db;
	}

	@Override
	protected String getCalibrationType() {
		return SensorCalibrationDB.SENSOR_CALIBRATION_TYPE;
	}
}
