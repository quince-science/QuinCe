package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data Reduction class for underway marine pCO₂
 *
 * @author Steve Jones
 *
 */
public class UnderwayMarinePco2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public UnderwayMarinePco2Reducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument,
    MeasurementValues sensorValues, DataReductionRecord record,
    Map<String, ArrayList<Measurement>> allMeasurements,
    DatasetSensorValues allSensorValues, Connection conn) throws Exception {

    Double intakeTemperature = sensorValues.getValue("Intake Temperature",
      allMeasurements, allSensorValues, this, valueCalculators, conn);
    Double salinity = sensorValues.getValue("Salinity", allMeasurements,
      allSensorValues, this, valueCalculators, conn);
    Double equilibratorTemperature = sensorValues.getValue(
      "Equilibrator Temperature", allMeasurements, allSensorValues, this,
      valueCalculators, conn);
    Double equilibratorPressure = sensorValues.getValue("Equilibrator Pressure",
      allMeasurements, allSensorValues, this, valueCalculators, conn);
    Double co2InGas = sensorValues.getValue("xCO₂ (with standards)",
      allMeasurements, allSensorValues, this, valueCalculators, conn);

    Double pH2O = Calculators.calcPH2O(salinity, equilibratorTemperature);
    Double pCo2TEWet = Calculators.calcpCO2TEWet(co2InGas, equilibratorPressure,
      pH2O);
    Double fCo2TEWet = Calculators.calcfCO2(pCo2TEWet, co2InGas,
      equilibratorPressure, equilibratorTemperature);

    Double pCO2SST = calcCO2AtSST(pCo2TEWet, equilibratorTemperature,
      intakeTemperature);
    Double fCO2 = calcCO2AtSST(fCo2TEWet, equilibratorTemperature,
      intakeTemperature);

    // Store the calculated values
    record.put("Equilibrator Pressure", equilibratorPressure);
    record.put("ΔT", Math.abs(intakeTemperature - equilibratorTemperature));
    record.put("pH₂O", pH2O);
    record.put("Calibrated CO₂", co2InGas);
    record.put("pCO₂ TE Wet", pCo2TEWet);
    record.put("fCO₂ TE Wet", fCo2TEWet);
    record.put("pCO₂ SST", pCO2SST);
    record.put("fCO₂", fCO2);
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
  private Double calcCO2AtSST(Double co2AtEquilibrator, Double eqt,
    Double sst) {
    return co2AtEquilibrator
      * Math.exp(0.0423 * (Calculators.kelvin(sst) - Calculators.kelvin(eqt)));
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Equilibrator Temperature", "Equilibrator Pressure",
      "xCO₂ (with standards)" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(7);
      calculationParameters.add(
        new CalculationParameter(makeParameterId(0), "Equilibrator Pressure",
          "Equilibrator Pressure", "PRESEQ", "hPa", false));
      calculationParameters
        .add(new CalculationParameter(makeParameterId(1), "ΔT",
          "Water-Equilibrator Temperature Difference", "DELTAT", "°C", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "pH₂O", "Marine Water Vapour Pressure", "RH2OX0EQ", "hPa", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "Calibrated CO₂", "xCO₂ In Water - Calibrated In Dry Air", "XCO2DECQ",
        "μmol mol-1", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "pCO₂ TE Wet", "pCO₂ In Water - Equilibrator Temperature", "PCO2IG02",
        "μatm", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "fCO₂ TE Wet", "fCO₂ In Water - Equilibrator Temperature", "FCO2IG02",
        "μatm", false));
      calculationParameters.add(new CalculationParameter(makeParameterId(6),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));
      calculationParameters.add(new CalculationParameter(makeParameterId(7),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }
}
