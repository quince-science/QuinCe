package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.exeter.QuinCe.data.Dataset.DateColumnGroupedSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.InstrumentVariable;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;

/**
 * Data Reduction class for underway marine pCO₂
 * 
 * @author Steve Jones
 *
 */
public class UnderwayAtmosphericPco2Reducer extends DataReducer {

  /**
   * The conversion factor from Pascals to Atmospheres
   */
  private static final double PASCALS_TO_ATMOSPHERES = 0.00000986923266716013;

  private static final double MOLAR_MASS_AIR = 28.97e-3;

  private static List<CalculationParameter> calculationParameters;

  static {
    calculationParameters = new ArrayList<CalculationParameter>(7);
    calculationParameters.add(new CalculationParameter("True Moisture",
      "Atmosphere True Moisture", "AWMXRCORR", "μmol mol-1", false));
    calculationParameters.add(new CalculationParameter("Sea Level Pressure",
      "Atmospheric Pressure At Sea Level", "CAPAZZ01", "hPa", false));
    calculationParameters.add(new CalculationParameter("pH₂O",
      "Atmosphere Water Vapour Pressure", "RH2OX0EQ", "hPa", false));
    calculationParameters.add(new CalculationParameter("Dried CO₂",
      "xCO₂ In Atmoshpere - Dry Air", "XCO2DRAT", "μmol mol-1", false));
    calculationParameters.add(new CalculationParameter("Calibrated CO₂",
      "xCO₂ In Atmosphere - Calibrated In Dry Air", "XCO2DCMA", "μmol mol-1",
      false));
    calculationParameters.add(new CalculationParameter("pCO₂",
      "pCO₂ In Atmosphere", "ACO2XXXX", "μatm", true));
    calculationParameters.add(new CalculationParameter("fCO₂",
      "fCO₂ In Atmosphere", "FCO2WTAT", "μatm", true));
  }

  public UnderwayAtmosphericPco2Reducer(InstrumentVariable variable,
    boolean nrt, Map<String, Float> variableAttributes,
    List<Measurement> allMeasurements,
    DateColumnGroupedSensorValues groupedSensorValues,
    CalibrationSet calibrationSet) {

    super(variable, nrt, variableAttributes, allMeasurements,
      groupedSensorValues, calibrationSet);
  }

  @Override
  protected void doCalculation(Instrument instrument, Measurement measurement,
    Map<SensorType, CalculationValue> sensorValues, DataReductionRecord record)
    throws Exception {

    Set<SensorType> requiredSensorTypes = getRequiredSensorTypes(
      instrument.getSensorAssignments());

    SensorType xH2OSensorType = getSensorType("xH₂O in gas");
    SensorType co2SensorType = getSensorType("CO₂ in gas");

    Double trueXH2O = 0.0D;
    if (requiredSensorTypes.contains(xH2OSensorType)) {
      trueXH2O = applyValueCalibration(measurement, xH2OSensorType,
        sensorValues.get(xH2OSensorType), false);
    }

    Double intakeTemperature = getValue(sensorValues,
      "Equilibrator Temperature");
    Double salinity = getValue(sensorValues, "Salinity");
    Double seaLevelPressure = getSeaLevelAtmPressure(
      getValue(sensorValues, "Atmospheric Pressure"), intakeTemperature);
    Double co2InGas = getValue(sensorValues, "CO₂ in gas");

    Double co2Dried = co2InGas;
    if (requiredSensorTypes.contains(xH2OSensorType)) {
      co2Dried = calcDriedCo2(co2InGas, trueXH2O);
    }

    Double co2Calibrated = applyValueCalibration(measurement, co2SensorType,
      co2Dried, true);
    Double pH2O = calcPH2O(salinity, intakeTemperature);
    Double pCO2 = calcPco2TEWet(co2Calibrated, seaLevelPressure, pH2O);
    Double fCO2 = calcFCO2(pCO2, co2Calibrated, seaLevelPressure,
      intakeTemperature);

    // Store the calculated values
    record.put("True Moisture", trueXH2O);
    record.put("Sea Level Pressure", seaLevelPressure);
    record.put("pH₂O", pH2O);
    record.put("Dried CO₂", co2Dried);
    record.put("Calibrated CO₂", co2Calibrated);
    record.put("pCO₂", pCO2);
    record.put("fCO₂", fCO2);
  }

  private Double getSeaLevelAtmPressure(Double measuredPressure,
    Double intakeTemperature) {
    Float sensorHeight = variableAttributes.get("atm_pres_sensor_height");

    Double correction = (measuredPressure * MOLAR_MASS_AIR)
      / (kelvin(intakeTemperature) * 8.314) * 9.8 * sensorHeight;

    return measuredPressure + correction;
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
   * Calculate dried CO2 using a moisture measurement
   * 
   * @param co2
   *          The measured CO2 value
   * @param xH2O
   *          The moisture value
   * @return The 'dry' CO2 value
   */
  private Double calcDriedCo2(Double co2, Double xH2O) {
    return co2 / (1.0 - (xH2O / 1000));
  }

  /**
   * Calculates pCO<sub>2</sub> in water at equlibrator temperature
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
   * Converts pCO<sub>2</sub> to fCO<sub>2</sub>
   * 
   * @param pCO2
   *          pCO<sub>2</sub> at intake temperature
   * @param co2Calibrated
   *          The calibrated, dried xCO<sub>2</sub> value
   * @param eqp
   *          The equilibrator pressure
   * @param eqt
   *          The equilibrator temperature
   * @return The fCO<sub>2</sub> value
   */
  private Double calcFCO2(Double pCO2, Double co2Calibrated, Double eqp,
    Double eqt) {
    Double kelvin = kelvin(eqt);
    Double B = -1636.75 + 12.0408 * kelvin - 0.0327957 * Math.pow(kelvin, 2)
      + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
    Double delta = 57.7 - 0.118 * kelvin;
    Double eqpAtmospheres = (eqp * 100) * PASCALS_TO_ATMOSPHERES;

    return pCO2 * Math.exp(
      ((B + 2 * Math.pow(1 - co2Calibrated * 1e-6, 2) * delta) * eqpAtmospheres)
        / (82.0575 * kelvin));
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Equilibrator Temperature", "Salinity",
      "Atmospheric Pressure", "CO₂ in gas" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    return calculationParameters;
  }
}
