package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.SearchableSensorValuesList;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;

/**
 * Data Reduction class for underway marine pCO₂
 *
 * @author Steve Jones
 *
 */
public class UnderwayMarinePco2Reducer extends DataReducer {

  /**
   * The conversion factor from Pascals to Atmospheres
   */
  private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

  private static List<CalculationParameter> calculationParameters;

  static {
    calculationParameters = new ArrayList<CalculationParameter>(8);
    calculationParameters.add(new CalculationParameter("Equilibrator Pressure",
      "Equilibrator Pressure", "PRESEQ", "hPa", false));
    calculationParameters.add(new CalculationParameter("ΔT",
      "Water-Equilibrator Temperature Difference", "DELTAT", "°C", false));
    calculationParameters.add(new CalculationParameter("True Moisture",
      "Marine True Moisture", "MWMXRCORR", "μmol mol-1", false));
    calculationParameters.add(new CalculationParameter("pH₂O",
      "Marine Water Vapour Pressure", "RH2OX0EQ", "hPa", false));
    calculationParameters.add(new CalculationParameter("Dried CO₂",
      "xCO₂ In Water - Dry Air", "XCO2WBDY", "μmol mol-1", false));
    calculationParameters.add(new CalculationParameter("Calibrated CO₂",
      "xCO₂ In Water - Calibrated In Dry Air", "XCO2DECQ", "μmol mol-1",
      false));
    calculationParameters.add(new CalculationParameter("pCO₂ TE Wet",
      "pCO₂ In Water - Equilibrator Temperature", "PCO2IG02", "μatm", true));
    calculationParameters.add(new CalculationParameter("pCO₂ SST",
      "pCO₂ In Water", "PCO2TK02", "μatm", true));
    calculationParameters.add(new CalculationParameter("fCO₂", "fCO₂ In Water",
      "FCO2XXXX", "μatm", true));
  }

  public UnderwayMarinePco2Reducer(InstrumentVariable variable,
    Map<String, Float> variableAttributes) {

    super(variable, variableAttributes);
  }

  @Override
  protected void doCalculation(Instrument instrument,
    MeasurementValues sensorValues, DataReductionRecord record,
    Map<String, ArrayList<Measurement>> allMeasurements,
    Map<Long, SearchableSensorValuesList> allSensorValues, Connection conn)
    throws Exception {

    Double intakeTemperature = sensorValues.getValue("Intake Temperature",
      allMeasurements, allSensorValues, conn);
    Double salinity = sensorValues.getValue("Salinity", allMeasurements,
      allSensorValues, conn);
    Double equilibratorTemperature = sensorValues.getValue(
      "Equilibrator Temperature", allMeasurements, allSensorValues, conn);
    Double equilibratorPressure = sensorValues.getValue("Equilibrator Pressure",
      allMeasurements, allSensorValues, conn);
    Double co2InGas = sensorValues.getValue("xCO₂ (with standards)",
      allMeasurements, allSensorValues, conn);

    Double pH2O = calcPH2O(salinity, equilibratorTemperature);
    Double pCo2TEWet = calcPco2TEWet(co2InGas, equilibratorPressure, pH2O);
    Double pCO2SST = calcPco2SST(pCo2TEWet, equilibratorTemperature,
      intakeTemperature);
    Double fCO2 = calcFco2SST(pCO2SST, co2InGas, equilibratorPressure,
      equilibratorTemperature);

    // Store the calculated values
    record.put("Equilibrator Pressure", equilibratorPressure);
    record.put("ΔT", Math.abs(intakeTemperature - equilibratorTemperature));
    record.put("pH₂O", pH2O);
    record.put("Calibrated CO₂", co2InGas);
    record.put("pCO₂ TE Wet", pCo2TEWet);
    record.put("pCO₂ SST", pCO2SST);
    record.put("fCO₂", fCO2);
  }

  /**
   * Calculates the water vapour pressure (pH<sub>2</sub>O). From Weiss and
   * Price (1980)
   *
   * @param salinity
   *          Salinity
   * @param eqt
   *          Equilibrator temperature (in celsius)
   * @return The calculated pH2O value
   */
  private Double calcPH2O(Double salinity, Double eqt) {
    double kelvin = kelvin(eqt);
    return Math.exp(24.4543 - 67.4509 * (100 / kelvin)
      - 4.8489 * Math.log(kelvin / 100) - 0.000544 * salinity);
  }

  /**
   * Calculates pCO<sub>2</sub> in water at equilibrator temperature
   *
   * @param co2
   *          The dry, calibrated CO<sub>2</sub> value
   * @param eqp
   *          The equilibrator pressure
   * @param pH2O
   *          The water vapour pressure
   * @return pCO<sub>2</sub> in water at equlibrator temperature
   */
  private Double calcPco2TEWet(Double co2, Double eqp, Double pH2O) {
    Double eqp_atm = eqp * PASCALS_TO_ATMOSPHERES * 100;
    return co2 * (eqp_atm - pH2O);
  }

  /**
   * Calculates pCO<sub>2</sub> at the intake (sea surface) temperature. From
   * Takahashi et al. (2009)
   *
   * @param pco2TEWet
   *          The pCO<sub>2</sub> at equilibrator temperature
   * @param eqt
   *          The equilibrator temperature
   * @param sst
   *          The intake temperature
   * @return The pCO<sub>2</sub> at intake temperature
   */
  private Double calcPco2SST(Double pco2TEWet, Double eqt, Double sst) {
    return pco2TEWet * Math.exp(0.0423 * (kelvin(sst) - kelvin(eqt)));
  }

  /**
   * Converts pCO<sub>2</sub> to fCO<sub>2</sub>
   *
   * @param pco2SST
   *          pCO<sub>2</sub> at intake temperature
   * @param co2Calibrated
   *          The calibrated, dried xCO<sub>2</sub> value
   * @param eqp
   *          The equilibrator pressure
   * @param eqt
   *          The equilibrator temperature
   * @return The fCO<sub>2</sub> value
   */
  private Double calcFco2SST(Double pco2SST, Double co2Calibrated, Double eqp,
    Double eqt) {
    Double kelvin = kelvin(eqt);
    Double B = -1636.75 + 12.0408 * kelvin - 0.0327957 * Math.pow(kelvin, 2)
      + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
    Double delta = 57.7 - 0.118 * kelvin;
    Double eqpAtmospheres = (eqp * 100) * PASCALS_TO_ATMOSPHERES;

    return pco2SST * Math.exp(
      ((B + 2 * Math.pow(1 - co2Calibrated * 1e-6, 2) * delta) * eqpAtmospheres)
        / (82.0575 * kelvin));
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Equilibrator Temperature", "Equilibrator Pressure",
      "xCO₂ (with standards)" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    return calculationParameters;
  }
}
