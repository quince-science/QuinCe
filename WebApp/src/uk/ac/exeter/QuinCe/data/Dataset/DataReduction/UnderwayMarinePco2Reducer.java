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
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;

/**
 * Data Reduction class for underway marine pCO₂
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
    calculationParameters.add(new CalculationParameter("Equilibrator Pressure", "Equilibrator Pressure", "PRESEQ", "hPa", false));
    calculationParameters.add(new CalculationParameter("ΔT", "Water-Equilibrator Temperature Difference", "DELTAT", "°C", false));
    calculationParameters.add(new CalculationParameter("True Moisture", "Marine True Moisture", "MWMXRCORR", "μmol mol-1", false));
    calculationParameters.add(new CalculationParameter("pH₂O", "Marine Water Vapour Pressure", "RH2OX0EQ", "hPa", false));
    calculationParameters.add(new CalculationParameter("Dried CO₂", "xCO₂ In Water - Dry Air", "XCO2WBDY", "μmol mol-1", false));
    calculationParameters.add(new CalculationParameter("Calibrated CO₂", "xCO₂ In Water - Calibrated In Dry Air", "XCO2DECQ", "μmol mol-1", false));
    calculationParameters.add(new CalculationParameter("pCO₂ TE Wet", "pCO₂ In Water - Equilibrator Temperature", "PCO2IG02", "μatm", true));
    calculationParameters.add(new CalculationParameter("pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));
    calculationParameters.add(new CalculationParameter("fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
}

  public UnderwayMarinePco2Reducer(InstrumentVariable variable,
    Map<String, Float> variableAttributes, List<Measurement> allMeasurements,
    DateColumnGroupedSensorValues groupedSensorValues, CalibrationSet calibrationSet) {

    super(variable, variableAttributes, allMeasurements, groupedSensorValues, calibrationSet);
  }

  @Override
  protected void doCalculation(Instrument instrument, Measurement measurement,
      Map<SensorType, CalculationValue> sensorValues,
      DataReductionRecord record) throws Exception {

    Set<SensorType> requiredSensorTypes = getRequiredSensorTypes(instrument.getSensorAssignments());

    SensorType xH2OSensorType = getSensorType("xH₂O in gas");
    SensorType co2SensorType = getSensorType("CO₂ in gas");

    Double trueXH2O = 0.0D;
    if (requiredSensorTypes.contains(xH2OSensorType)) {
      trueXH2O = applyValueCalibration(measurement,
        xH2OSensorType, sensorValues.get(xH2OSensorType), false);
    }

    Double intakeTemperature = getValue(sensorValues, "Intake Temperature");
    Double salinity = getValue(sensorValues, "Salinity");
    Double equilibratorTemperature = getValue(sensorValues, "Equilibrator Temperature");
    Double equilibratorPressure = getEquilibratorPressure(requiredSensorTypes, sensorValues).getValue();
    Double co2InGas = getValue(sensorValues, "CO₂ in gas");

    Double co2Dried = co2InGas;
    if (requiredSensorTypes.contains(xH2OSensorType)) {
      co2Dried = calcDriedCo2(co2InGas, trueXH2O);
    }

    Double co2Calibrated = applyValueCalibration(measurement, co2SensorType, co2Dried, true);
    Double pH2O = calcPH2O(salinity, equilibratorTemperature);
    Double pCo2TEWet = calcPco2TEWet(co2Calibrated, equilibratorPressure, pH2O);
    Double pCO2SST = calcPco2SST(pCo2TEWet, equilibratorTemperature, intakeTemperature);
    Double fCO2 = calcFco2SST(pCO2SST, co2Calibrated, equilibratorPressure, equilibratorTemperature);

    // Store the calculated values
    record.put("Equilibrator Pressure", equilibratorPressure);
    record.put("ΔT", Math.abs(intakeTemperature - equilibratorTemperature));
    record.put("True Moisture", trueXH2O);
    record.put("pH₂O", pH2O);
    record.put("Dried CO₂", co2Dried);
    record.put("Calibrated CO₂", co2Calibrated);
    record.put("pCO₂ TE Wet", pCo2TEWet);
    record.put("pCO₂ SST", pCO2SST);
    record.put("fCO₂", fCO2);
  }

  /**
   * Calculates the water vapour pressure (pH<sub>2</sub>O).
   * From Weiss and Price (1980)
   * @param salinity Salinity
   * @param eqt Equilibrator temperature (in celsius)
   * @return The calculated pH2O value
   */
  private Double calcPH2O(Double salinity, Double eqt) {
    double kelvin = kelvin(eqt);
    return Math.exp(24.4543 - 67.4509 * (100 / kelvin) - 4.8489 * Math.log(kelvin / 100) - 0.000544 * salinity);
  }

  /**
   * Calculate dried CO2 using a moisture measurement
   * @param co2 The measured CO2 value
   * @param xH2O The moisture value
   * @return The 'dry' CO2 value
   */
  private Double calcDriedCo2(Double co2, Double xH2O) {
    return co2 / (1.0 - (xH2O / 1000));
  }

  /**
   * Calculates pCO<sub>2</sub> in water at equlibrator temperature
   * @param co2 The dry, calibrated CO<sub>2</sub> value
   * @param eqp The equilibrator pressure
   * @param pH2O The water vapour pressure
   * @return pCO<sub>2</sub> in water at equlibrator temperature
   */
  private Double calcPco2TEWet(Double co2, Double eqp, Double pH2O) {
    Double eqp_atm = eqp * PASCALS_TO_ATMOSPHERES * 100;
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
  private Double calcPco2SST(Double pco2TEWet, Double eqt, Double sst) {
    return pco2TEWet * Math.exp(0.0423 * (kelvin(sst) - kelvin(eqt)));
  }

  /**
   * Converts pCO<sub>2</sub> to fCO<sub>2</sub>
   * @param pco2SST pCO<sub>2</sub> at intake temperature
   * @param co2Calibrated The calibrated, dried xCO<sub>2</sub> value
   * @param eqp The equilibrator pressure
   * @param eqt The equilibrator temperature
   * @return The fCO<sub>2</sub> value
   */
  private Double calcFco2SST(Double pco2SST, Double co2Calibrated, Double eqp, Double eqt) {
    Double kelvin = kelvin(eqt);
    Double B = -1636.75 + 12.0408 * kelvin -0.0327957 * Math.pow(kelvin, 2) + (3.16528 * 1e-5) * Math.pow(kelvin, 3);
    Double delta = 57.7 - 0.118 * kelvin;
    Double eqpAtmospheres = (eqp * 100) * PASCALS_TO_ATMOSPHERES;

    return pco2SST * Math.exp(((B + 2 * Math.pow(1 - co2Calibrated * 1e-6, 2) * delta) * eqpAtmospheres) / (82.0575 * kelvin));
  }

  private CalculationValue getEquilibratorPressure(Set<SensorType> sensorTypes,
    Map<SensorType, CalculationValue> sensorValues) throws SensorTypeNotFoundException {

    CalculationValue absolute = null;
    CalculationValue differential = null;

    // Get the Absolute equilibrator pressure
    SensorType absoluteSensorType = getSensorType("Equilibrator Pressure (absolute)");
    if (sensorTypes.contains(absoluteSensorType)) {
      absolute = sensorValues.get(absoluteSensorType);
    }

    // Now the differential, with calculation to ambient pressure
    SensorType differentialSensorType = getSensorType("Equilibrator Pressure (differential)");
    SensorType ambientSensorType = getSensorType("Ambient Pressure");

    if (sensorTypes.contains(differentialSensorType)) {
      CalculationValue differentialPressure = sensorValues.get(differentialSensorType);
      CalculationValue ambientPressure = sensorValues.get(ambientSensorType);

      differential = CalculationValue.sum(ambientPressure, differentialPressure);
    }

    return CalculationValue.mean(absolute, differential);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] {"Intake Temperature", "Salinity",
      "Equilibrator Temperature", "Equilibrator Pressure", "CO₂ in gas"};
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    return calculationParameters;
  }
}
