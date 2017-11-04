package uk.ac.exeter.QuinCe.data.Calculation;

import uk.ac.exeter.QuinCe.EquilibratorPco2.EquilibratorPco2DB;

/**
 * Factory class for CalculationDB instances
 * @author Steve Jones
 *
 */
public class CalculationDBFactory {

	// TODO This definitely needs to use reflection.
	
	/**
	 * Get a CalculationDB instance
	 * @return The instance
	 */
	public static CalculationDB getCalculatioDB() {
		return new EquilibratorPco2DB();
	}
	
}
