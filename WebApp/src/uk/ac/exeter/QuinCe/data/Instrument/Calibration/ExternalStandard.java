package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.ParameterException;

/**
 * Represents a external standard calibration
 * @author Steve Jones
 *
 */
public class ExternalStandard extends Calibration {

	/**
	 * Contains the label for the concentration value
	 * (constructed in the {@code static} block)
	 */
	private static List<String> valueNames;
	
	static {
		valueNames = new ArrayList<String>(1);
		valueNames.add("CO2");
		valueNames.add("xH2O");
	}
	
	/**
	 * Create an empty external standard placeholder that isn't
	 * bound to a particular standard
	 * @param instrumentId The instrument ID
	 */
	public ExternalStandard(long instrumentId) {
		super(instrumentId, ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE);
	}
	
	/**
	 * Creates an empty external standard for a specified standard
	 * @param instrumentid The instrument ID
	 * @param standard The standard
	 */
	protected ExternalStandard(long instrumentid, String standard) {
		super(instrumentid, ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE, standard);
	}

	/**
	 * Construct a complete external standard object with all data
	 * @param instrumentId The instrument ID
	 * @param target The target external standard
	 * @param deploymentDate The deployment date
	 * @param coefficients The standard concentration
	 * @throws ParameterException If the calibration details are invalid
	 */
	protected ExternalStandard(long instrumentId, String target, LocalDateTime deploymentDate, List<Double> coefficients) throws ParameterException {
		super(instrumentId, ExternalStandardDB.EXTERNAL_STANDARD_CALIBRATION_TYPE, target);
		
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
	 * Get the concentration of the external standard
	 * @return The concentration
	 */
	public double getConcentration() {
		if (null == coefficients) {
			initialiseCoefficients();
		}
		
		return coefficients.get(0).getValue();
	}
	
	/**
	 * Set the concentration of the external standard
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
			if (coefficients.size() != 2) {
				result = false;
			} else {
				if (getConcentration() < 0) {
					result = false;
				}
				if (getCoefficients().get(1).getValue() != 0.0) {
					// xH2O must be zero
					result = false;
				}
			}
		}

		return result;
	}
}
