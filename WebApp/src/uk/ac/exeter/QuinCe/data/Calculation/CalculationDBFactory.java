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
	 * The single instance of the only calculation DB we know about so far
	 */
	private static CalculationDB calculationDB = null;
	
	/**
	 * Get a CalculationDB instance
	 * @return The instance
	 */
	public static CalculationDB getCalculationDB() {
		if (null == calculationDB)  {
			calculationDB = new EquilibratorPco2DB();
		}
		
		return calculationDB;
	}
	
}
