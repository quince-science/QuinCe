package uk.ac.exeter.QuinCe.EquilibratorPco2;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Calculation.CalculationDB;
import uk.ac.exeter.QuinCe.data.Calculation.CalculatorException;
import uk.ac.exeter.QuinCe.data.Calculation.DataReductionCalculator;
import uk.ac.exeter.QuinCe.data.Dataset.CalibrationDataSet;
import uk.ac.exeter.QuinCe.data.Dataset.DataSetRawDataRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;

/**
 * This class performs data reduction calculations for an
 * equilibrator-based pCO2 system.
 *
 * The calculations follow the procedure in Pierrot et al. 2009
 * (doi:10.1016/j.dsr2.2008.12.005), with direct input from Denis Pierrot.
 *
 * @author Steve Jones
 *
 */
@Deprecated
public class EquilibratorPco2Calculator extends DataReductionCalculator {

  /**
   * The conversion factor from Pascals to Atmospheres
   */
  private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

  /**
   * Base constructor
   * @param externalStandards The external standards for the data set
   * @param calibrations The calibration data for the data set
   * @throws CalculatorException If any calibration data is missing
   */
  public EquilibratorPco2Calculator(CalibrationSet externalStandards, CalibrationDataSet calibrations) throws CalculatorException {
    super(externalStandards, calibrations);
  }

  @Override
  protected CalculationDB getDbInstance() {
    return new EquilibratorPco2DB();
  }

  @Override
  public Map<String, Double> performDataReduction(DataSetRawDataRecord measurement) throws CalculatorException {

    LocalDateTime date = measurement.getDate();
    Double intakeTemperature = measurement.getSensorValue("Intake Temperature");
    if (null == intakeTemperature) {
      throw new CalculatorException("Intake Temperature missing");
    }

    Double salinity = measurement.getSensorValue("Salinity");
    if (null == salinity) {
      throw new CalculatorException("Salinity missing");
    }

    Double equilibratorTemperature = measurement.getSensorValue("Equilibrator Temperature");
    if (null == equilibratorTemperature) {
      throw new CalculatorException("Equilibrator Temperature missing");
    }

    // TODO We need some kind of flag we can run to check which equilibrator pressure to use. #577
    Double equilibratorPressure = null;

    Double absoluteEquilibratorPressure = measurement.getSensorValue("Equilibrator Pressure (absolute)");
    if (null != absoluteEquilibratorPressure) {
      equilibratorPressure = absoluteEquilibratorPressure;
    } else {
      Double differential = measurement.getSensorValue("Equilibrator Pressure (differential)");
      Double atmospheric = measurement.getSensorValue("Ambient Pressure");
      if (null != differential && null != atmospheric) {
        equilibratorPressure = atmospheric + differential;
      }
    }

    if (null == equilibratorPressure) {
      throw new CalculatorException("Equilibrator Pressure missing");
    }

    Double xH2O = measurement.getSensorValue("xH₂O in gas");

    Double co2Measured = measurement.getSensorValue("CO₂ in gas");
    if (null == co2Measured) {
      throw new CalculatorException("CO2 value missing");
    }

    Double truexH2O = null;
    double co2Dried;

    if (null == xH2O) {
      co2Dried = co2Measured;
    } else {
      /*
       * This uses the same multiple standards as the CO2 adjustment.
       * While it's not strictly necessary, since all standards will have
       * zero xH2O, using the regression technique across all standards
       * helps to smooth out any variability within the sensor.
       */
      truexH2O = applyExternalStandards(date, "xH₂O in gas", xH2O, false);
      co2Dried = calcDriedCo2(co2Measured, truexH2O);
    }

    double co2Calibrated = applyExternalStandards(date, "CO₂ in gas", co2Dried, true);

    double pH2O = calcPH2O(salinity, equilibratorTemperature);
    double pCo2TEWet = calcPco2TEWet(co2Calibrated, equilibratorPressure, pH2O);
    double pCO2SST = calcPco2SST(pCo2TEWet, equilibratorTemperature, intakeTemperature);
    double fCO2 = calcFco2SST(pCO2SST, co2Calibrated, equilibratorPressure, equilibratorTemperature);

    Map<String, Double> calculatedValues = new HashMap<String, Double>();
    calculatedValues.put("delta_temperature", Math.abs(intakeTemperature - equilibratorTemperature));
    if (null != xH2O) {
      calculatedValues.put("true_moisture", truexH2O);
    }
    calculatedValues.put("ph2o", pH2O);
    calculatedValues.put("dried_co2", co2Dried);
    calculatedValues.put("calibrated_co2", co2Calibrated);
    calculatedValues.put("pco2_te_wet", pCo2TEWet);
    calculatedValues.put("pco2_sst", pCO2SST);
    calculatedValues.put("fco2", fCO2);

    return calculatedValues;
  }

  /**
   * Calculate dried CO2 using a moisture measurement
   * @param co2 The measured CO2 value
   * @param xH2O The moisture value
   * @return The 'dry' CO2 value
   */
  private double calcDriedCo2(double co2, double xH2O) {
    return co2 / (1.0 - (xH2O / 1000));
  }

  /**
   * Calculates the water vapour pressure (pH<sub>2</sub>O).
   * From Weiss and Price (1980)
   * @param salinity Salinity
   * @param eqt Equilibrator temperature (in celsius)
   * @return The calculated pH2O value
   */
  private double calcPH2O(double salinity, double eqt) {
    double kelvin = eqt + 273.15;
    return Math.exp(24.4543 - 67.4509 * (100 / kelvin) - 4.8489 * Math.log(kelvin / 100) - 0.000544 * salinity);
  }

  /**
   * Calculates pCO<sub>2</sub> in water at equlibrator temperature
   * @param co2 The dry, calibrated CO<sub>2</sub> value
   * @param eqp The equilibrator pressure
   * @param pH2O The water vapour pressure
   * @return pCO<sub>2</sub> in water at equlibrator temperature
   */
  private double calcPco2TEWet(double co2, double eqp, double pH2O) {
    double eqp_atm = eqp * PASCALS_TO_ATMOSPHERES * 100;
    return co2 * (eqp_atm - pH2O);
  }

  /**
   * Calculates pCO<sub>2</sub> at the intake (sea surface) temperature.
   * From Takahashi et al. (2009)
   * @param pco2TEWet The pCO<sub>2</sub> at equilibrator temperature
   * @param eqt The equilibrator temperature
   * @param sst The intake temperature
   * @return The pCO<sub>2</sub> at intake temperature
   */
  private double calcPco2SST(double pco2TEWet, double eqt, double sst) {
    double sst_kelvin = sst + 273.15;
    double eqt_kelvin = eqt + 273.15;
    return pco2TEWet * Math.exp(0.0423 * (sst_kelvin - eqt_kelvin));
  }

  /**
   * Converts pCO<sub>2</sub> to fCO<sub>2</sub>
   * @param pco2SST pCO<sub>2</sub> at intake temperature
   * @param co2Calibrated The calibrated, dried xCO<sub>2</sub> value
   * @param eqp The equilibrator pressure
   * @param eqt The equilibrator temperature
   * @return The fCO<sub>2</sub> value
   */
  private double calcFco2SST(double pco2SST, double co2Calibrated, double eqp, double eqt) {
    double kelvin = eqt + 273.15;
    double B = -1636.75 + 12.0408 * kelvin -0.0327957 * Math.pow(kelvin, 2) + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
    double delta = 57.7 - 0.118 * kelvin;
    double eqpAtmospheres = (eqp * 100) * PASCALS_TO_ATMOSPHERES;

    return pco2SST * Math.exp(((B + 2 * Math.pow(1 - co2Calibrated * 1e-6, 2) * delta) * eqpAtmospheres) / (82.0575 * kelvin));
  }
}
