package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.DataReductionCalculator;
import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;

/**
 * 
 * @author Steve Jones
 *
 */
public class EquilibratorPco2Calculator extends DataReductionCalculator {

	/**
	 * Base constructor
	 * @param calibrations The calibration data for the data set
	 */
	public EquilibratorPco2Calculator(CalibrationDataSet calibrations) {
		super(calibrations);
	}
	
	@Override
	protected CalculationDB getDbInstance() {
		return new EquilibratorPco2DB();
	}
	
	@Override
	public void performDataReduction(DataSetRawDataRecord measurement) {

		LocalDateTime date = measurement.getDate();
		double intakeTemperature = measurement.getSensorValue("Intake Temperature");
		double salinity = measurement.getSensorValue("Salinity");
		double equilTemperature = measurement.getSensorValue("Equilibrator Temperature");
		
		
		// TODO We need some kind of flag we can run to check which equilibrator pressure to use. #577
		double equilibratorPressure;
		
		Double absoluteEquilibratorPressure = measurement.getSensorValue("Equilibrator Pressure (absolute)");
		if (null != absoluteEquilibratorPressure) {
			equilibratorPressure = absoluteEquilibratorPressure;
		} else {
			double differential = measurement.getSensorValue("Equilibrator Pressure (differential)");
			double atmospheric = measurement.getSensorValue("Atmospheric Pressure");
			equilibratorPressure = atmospheric + differential;
		}
		
		Double xH2O = measurement.getSensorValue("xH2O");
		double co2Measured = measurement.getSensorValue("CO2");
		
		
		double co2Dried;
		
		if (null == xH2O) {
			co2Dried = co2Measured;
		} else {
			co2Dried = calcDriedCo2(co2Measured, xH2O);
		}
		
		
	}
	
	/**
	 * Calculate dried CO2 using a moisture measurement
	 * @param co2 The measured CO2 value
	 * @param xH2O The moisture value
	 * @return
	 */
	private double calcDriedCo2(double co2, double xH2O) {
		return co2 / (1.0 - (xH2O / 1000));
	}
}
