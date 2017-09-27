package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.ParameterException;

/**
 * Represents a gas standard calibration
 * @author Steve Jones
 *
 */
public class GasStandard extends Calibration {

	/**
	 * Contains the label for the concentration value
	 * (constructed in the {@code static} block)
	 */
	private static List<String> valueNames;
	
	static {
		valueNames = new ArrayList<String>(1);
		valueNames.add("Concentration");
	}
	
	/**
	 * Create an empty gas standard placeholder that isn't
	 * bound to a particular standard
	 * @param instrumentId The instrument ID
	 */
	public GasStandard(long instrumentId) {
		super(instrumentId, GasStandardDB.GAS_STANDARD_CALIBRATION_TYPE);
	}
	
	/**
	 * Creates an empty gas standard for a specified standard
	 * @param instrumentid The instrument ID
	 * @param standard The standard
	 */
	protected GasStandard(long instrumentid, String standard) {
		super(instrumentid, GasStandardDB.GAS_STANDARD_CALIBRATION_TYPE, standard);
	}

	/**
	 * Construct a complete gas standard object with all data
	 * @param instrumentId The instrument ID
	 * @param target The target gas standard
	 * @param deploymentDate The deployment date
	 * @param coefficients The standard concentration
	 * @throws ParameterException If the calibration details are invalid
	 */
	protected GasStandard(long instrumentId, String target, LocalDateTime deploymentDate, List<Double> coefficients) throws ParameterException {
		super(instrumentId, GasStandardDB.GAS_STANDARD_CALIBRATION_TYPE, target);
		
		if (null != target) {
			setDeploymentDate(deploymentDate);
			setCoefficients(coefficients);
			if (!validate()) {
				throw new ParameterException("Deployment date/coefficients", "Calibration deployment is invalid");
			}
		}
	}

	@Override
	public List<String> getCoefficientNames() {
		return valueNames;
	}

	@Override
	public String buildHumanReadableCoefficients() {
		String result = "Not set";
		
		if (null != coefficients) {
			result = String.valueOf(coefficients.get(0).getValue()); 
		}
		
		return result;
	}
	
	/**
	 * Get the concentration of the gas standard
	 * @return The concentration
	 */
	public double getConcentration() {
		if (null == coefficients) {
			initialiseCoefficients();
		}
		
		return coefficients.get(0).getValue();
	}
	
	/**
	 * Set the concentration of the gas standard
	 * @param concentration The concentration
	 */
	public void setConcentration(double concentration) {
		if (null == coefficients) {
			initialiseCoefficients();
		}
		
		coefficients.set(0, new CalibrationCoefficient(getCoefficientNames().get(0), concentration));
	}

	@Override
	public boolean coefficientsValid() {
		boolean result = true;
		
		if (null != coefficients) {
			if (coefficients.size() != 1) {
				result = false;
			} else if (getConcentration() < 0) {
				result = false;
			}
		}

		return result;
	}
}
