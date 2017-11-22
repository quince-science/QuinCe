package uk.ac.exeter.QuinCe.data.Calculation;

import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;

/**
 * Abstract class for data reduction calculators
 * @author Steve Jones
 *
 */
public abstract class DataReductionCalculator {

	/**
	 * The CalculationDB instance for the calculator
	 */
	protected CalculationDB db;
	
	/**
	 * The set of calibration data to be used for the calculations
	 */
	protected CalibrationDataSet calibrations;
	
	/**
	 * The base constructor - sets up things that
	 * are universal to all calculators
	 */
	protected DataReductionCalculator(CalibrationDataSet calibrations) {
		db = getDbInstance();
		this.calibrations = calibrations;
	}
	
	/**
	 * Get an instance of the {@link CalculationDB} for the
	 * calculator
	 */
	protected abstract CalculationDB getDbInstance();
	
	/**
	 * Perform the data reduction calculation for a given measurement,
	 * and store the results in the database.
	 * @param measurement The measurement
	 */
	public abstract void performDataReduction(DataSetRawDataRecord measurement);
}
