package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.utils.ParameterException;

/**
 * A calibration that applies up to a fifth-order
 * polynomial adjustment
 * @author Steve Jones
 *
 */
public class PolynomialSensorCalibration extends SensorCalibration {

	/**
	 * The name of the Intercept coefficient
	 */
	private static final String INTERCEPT_NAME = "Intercept";
	
	/**
	 * Contains the labels for the polynomial curve parameters
	 * (constructed in the {@code static} block)
	 */
	private static List<String> valueNames;
	
	static {
		valueNames = new ArrayList<String>(5);
		valueNames.add("x⁵");
		valueNames.add("x⁴");
		valueNames.add("x³");
		valueNames.add("x²");
		valueNames.add("x");
		valueNames.add(INTERCEPT_NAME);
	}
	
	/**
	 * Create a calibration object
	 * @param instrumentId The instrument to which the calibration target belongs
	 * @param target The calibration target (most likely a sensor)
	 */
	public PolynomialSensorCalibration(long instrumentId, String target) {
		super(instrumentId, target);
	}

	/**
	 * Create a calibration object with no target set
	 * @param instrumentId The instrument to which the calibration target belongs
	 */
	public PolynomialSensorCalibration(long instrumentId) {
		super(instrumentId);
	}

	/**
	 * Construct a complete sensor calibration object.
	 * @param instrumentId The instrument ID
	 * @param target The target sensor
	 * @param deploymentDate The deployment date
	 * @param coefficients The calibration coefficients
	 * @throws CalibrationException If the calibration details are invalid
	 */
	public PolynomialSensorCalibration(long instrumentId, String target, LocalDateTime deploymentDate, List<Double> coefficients) throws ParameterException {
		super(instrumentId, target, deploymentDate, coefficients);
	}

	@Override
	public List<String> getCoefficientNames() {
		return valueNames;
	}

	@Override
	protected String buildHumanReadableCoefficients() {
		
		StringBuilder result = new StringBuilder();
		
		for (int i = 0; i < valueNames.size(); i++) {
			appendCoefficient(result, coefficients.get(i), valueNames.get(i));
		}
		
		return result.toString();		
	}

	/**
	 * Add a coefficient to the human readable coefficients string
	 * @param string The string being constructed
	 * @param coefficient The coefficient value
	 * @param name The coefficient name
	 */
	private void appendCoefficient(StringBuilder string, double coefficient, String name) {
		
		if (string.length() == 0 && coefficient != 0) {
			string.append(coefficient);
			
			if (!name.equals(INTERCEPT_NAME)) {
				string.append(name);
			}
		} else if (coefficient != 0) {
			if (coefficient > 0) {
				string.append(" + ");
			} else if (coefficient < 0) {
				string.append(" - ");
			}
			
			string.append(Math.abs(coefficient));
			if (!name.equals(INTERCEPT_NAME)) {
				string.append(name);
			}
		}
	}

	@Override
	public boolean coefficientsValid() {
		return (null != coefficients && coefficients.size() == 6);
	}
	
	@Override
	protected void initialiseCoefficients() {
		coefficients = new ArrayList<Double>(getCoefficientNames().size());
		for (int i = 0; i < getCoefficientNames().size(); i++) {
			if (getCoefficientNames().get(i).equals("x")) {
				coefficients.add(1.0);
			} else {
				coefficients.add(0.0);
			}
		}
	}
}
