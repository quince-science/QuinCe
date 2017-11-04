package uk.ac.exeter.QuinCe.EquilibratorPco2;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;

/**
 * Instance of {@link CalculationDB} for underway pCO2
 * @author Steve Jones
 *
 */
public class EquilibratorPco2DB extends CalculationDB {

	@Override
	public String getCalculationTable() {
		return "equilibrator_pco2";
	}
	
}
