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
	 * Contains the label for the concentration value
	 * (constructed in the {@code static} block)
	 */
	private static List<String> valueNames;
	
	static {
		valueNames = new ArrayList<String>(1);
		valueNames.add("Concentration");
	}
	
	/**
	 * Create an empty gas standard
	 * @param instrumentId The instrument ID
	 */
	public GasStandard(long instrumentId) {
		super(instrumentId);
	}

	@Override
	public List<String> getValueNames() {
		return valueNames;
	}

	@Override
	protected String getType() {
		return "GAS_STANDARD";
	}
	
	@Override
	public String getHumanReadableValues() {
		return String.valueOf(values.get(0));
	}
	
	/**
	 * Get the concentration of the gas standard
	 * @return The concentration
	 */
	public double getConcentration() {
		return values.get(0);
	}
	
	/**
	 * Set the concentration of the gas standard
	 * @param concentration The concentration
	 */
	public void setConcentration(double concentration) {
		values.set(0, concentration);
	}
}
