package uk.ac.exeter.QuinCe.data.Calculation;

import uk.ac.exeter.QuinCe.EquilibratorPco2.EquilibratorPco2CalculationRecord;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Factory for making calculation records
 * @author Steve Jones
 *
 */
public class CalculationRecordFactory {

	/**
	 * Create a calculation record for a given data type
	 * @param datasetId The ID of the dataset to which the record belongs
	 * @param row The measurement ID
	 * @return The new, empty calculation record
	 */
	public static CalculationRecord makeCalculationRecord(long datasetId, long row) {
		return new EquilibratorPco2CalculationRecord(datasetId, row, ResourceManager.getInstance());
	}
	
}
