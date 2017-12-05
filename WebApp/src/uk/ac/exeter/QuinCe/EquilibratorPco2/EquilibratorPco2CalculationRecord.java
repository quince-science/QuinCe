package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.sql.Connection;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationRecord;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

/**
 * Calculation record for equilibrator-based pCO2 systems 
 * @author Steve Jones
 *
 */
public class EquilibratorPco2CalculationRecord extends CalculationRecord {

	/**
	 * Creates an empty calculation record for the given dataset and measurement
	 * @param datasetId The dataset ID
	 * @param measurementId The measurement ID
	 * @param resourceManager A Resource Manager containing the QC Column Config for Equilibrator pCO2
	 */
	public EquilibratorPco2CalculationRecord(long datasetId, int measurementId, ResourceManager resourceManager) {
		super(datasetId, measurementId, resourceManager.getColumnConfig());
	}

	@Override
	public void loadCalculationData(Connection conn) {
	}
}
