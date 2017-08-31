package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a gas standard calibration
 * @author Steve Jones
 *
 */
public class GasStandard extends Calibration {

	/**
	 * The calibration type for gas standards
	 */
	public static final String GAS_STANDARD_CALIBRATION_TYPE = "GAS_STANDARD";
	
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
		super(instrumentId, GAS_STANDARD_CALIBRATION_TYPE);
	}
	
	/**
	 * Creates an empty gas standard for a specified standard
	 * @param instrumentid The instrument ID
	 * @param standard The standard
	 */
	protected GasStandard(long instrumentid, String standard) {
		super(instrumentid, GAS_STANDARD_CALIBRATION_TYPE, standard);
	}

	@Override
	public List<String> getCoefficientNames() {
		return valueNames;
	}

	@Override
	public String buildHumanReadableCoefficients() {
		String result = "Not set";
		
		if (null != coefficients) {
			result = String.valueOf(coefficients.get(0)); 
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
		
		return coefficients.get(0);
	}
	
	/**
	 * Set the concentration of the gas standard
	 * @param concentration The concentration
	 */
	public void setConcentration(double concentration) {
		if (null == coefficients) {
			initialiseCoefficients();
		}
		
		coefficients.set(0, concentration);
	}
}
