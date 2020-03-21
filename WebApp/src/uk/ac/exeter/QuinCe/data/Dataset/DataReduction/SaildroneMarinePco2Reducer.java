package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;

/**
 * Data Reduction class for NRT Marine fCO₂ from SailDrones.
 *
 * <p>
 * Calculations from Sutton et al. 2014 (doi: 10.5194/essd-6-353-2014).
 * </p>
 *
 * @author Steve Jones
 *
 */
public class SaildroneMarinePco2Reducer extends DataReducer {

  /**
   * The conversion factor from Pascals to Atmospheres
   */
  private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

  private static List<CalculationParameter> calculationParameters;

  static {
    calculationParameters = new ArrayList<CalculationParameter>(8);
    calculationParameters.add(new CalculationParameter("pH₂O",
      "Marine Water Vapour Pressure", "RH2OX0EQ", "hPa", false));
    calculationParameters.add(new CalculationParameter("pCO₂", "pCO₂ In Water",
      "PCO2TK02", "μatm", true));
    calculationParameters.add(new CalculationParameter("fCO₂", "fCO₂ In Water",
      "FCO2XXXX", "μatm", true));
  }

  public SaildroneMarinePco2Reducer(InstrumentVariable variable, boolean nrt,
    Map<String, Float> variableAttributes) {

    super(variable, nrt, variableAttributes);
  }

  @Override
  protected void doCalculation(Instrument instrument,
    MeasurementValues sensorValues, DataReductionRecord record,
    Map<String, ArrayList<Measurement>> allMeasurements, Connection conn)
    throws Exception {
    /*
     * Double intakeTemperature = getValue(sensorValues, "Intake Temperature");
     * Double salinity = getValue(sensorValues, "Salinity"); Double
     * licorPressure = getValue(sensorValues, "LICOR Pressure (Equilibrator)");
     * Double xCo2 = getValue(sensorValues, "xCO₂ water (dry, no standards)");
     * Double pH2O = calcPH2O(salinity, intakeTemperature); Double pCo2 =
     * calcPco2(xCo2, licorPressure, pH2O); Double fCO2 = calcFco2(pCo2, xCo2,
     * licorPressure, intakeTemperature);
     *
     * // Store the calculated values record.put("pH₂O", pH2O);
     * record.put("pCO₂", pCo2); record.put("fCO₂", fCO2);
     */ }

  /**
   * Calculates the water vapour pressure (pH<sub>2</sub>O). From Weiss and
   * Price (1980)
   *
   * @param salinity
   *          Salinity
   * @param sst
   *          Sea surface temperature (in celsius)
   * @return The calculated pH2O value
   */
  private Double calcPH2O(Double salinity, Double sst) {
    double kelvin = kelvin(sst);
    return Math.exp(24.4543 - 67.4509 * (100 / kelvin)
      - 4.8489 * Math.log(kelvin / 100) - 0.000544 * salinity);
  }

  /**
   * Calculates pCO<sub>2</sub> in water
   *
   * @param co2
   *          The dry, calibrated CO<sub>2</sub> value
   * @param pressure
   *          The equilibrator pressure
   * @param pH2O
   *          The water vapour pressure
   * @return pCO<sub>2</sub> in water
   */
  private Double calcPco2(Double co2, Double pressure, Double pH2O) {
    Double pressure_atm = pressure * PASCALS_TO_ATMOSPHERES * 100;
    return co2 * (pressure_atm - pH2O);
  }

  /**
   * Converts pCO<sub>2</sub> to fCO<sub>2</sub>
   *
   * @param pCo2
   *          pCO<sub>2</sub>
   * @param co2Calibrated
   *          The calibrated, dried xCO<sub>2</sub> value
   * @param eqp
   *          The equilibrator pressure
   * @param sst
   *          The sea surface temperature
   * @return The fCO<sub>2</sub> value
   */
  private Double calcFco2(Double pCo2, Double co2Calibrated, Double eqp,
    Double sst) {
    Double kelvin = kelvin(sst);
    Double B = -1636.75 + 12.0408 * kelvin - 0.0327957 * Math.pow(kelvin, 2)
      + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
    Double delta = 57.7 - 0.118 * kelvin;
    Double eqpAtmospheres = (eqp * 100) * PASCALS_TO_ATMOSPHERES;

    return pCo2 * Math.exp(
      ((B + 2 * Math.pow(1 - co2Calibrated * 1e-6, 2) * delta) * eqpAtmospheres)
        / (82.0575 * kelvin));
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "LICOR Pressure (Equilibrator)", "xCO₂ water (dry, no standards)" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    return calculationParameters;
  }
}
