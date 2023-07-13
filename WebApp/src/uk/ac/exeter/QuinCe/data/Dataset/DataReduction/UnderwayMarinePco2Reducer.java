package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {

    Double intakeTemperature = measurement
      .getMeasurementValue("Intake Temperature").getCalculatedValue();
    Double salinity = measurement.getMeasurementValue("Salinity")
      .getCalculatedValue();
    Double equilibratorTemperature = measurement
      .getMeasurementValue("Equilibrator Temperature").getCalculatedValue();
    Double equilibratorPressure = measurement
      .getMeasurementValue("Equilibrator Pressure").getCalculatedValue();
    Double xCO2 = measurement.getMeasurementValue(getXCO2Parameter())
      .getCalculatedValue();

    Calculator calculator = new Calculator(intakeTemperature, salinity,
      equilibratorTemperature, equilibratorPressure, xCO2);

    // Store the calculated values
    record.put("ΔT", Math.abs(intakeTemperature - equilibratorTemperature));
    record.put("pH₂O", calculator.pH2O);
    record.put("pCO₂ TE Wet", calculator.pCo2TEWet);
    record.put("fCO₂ TE Wet", calculator.fCo2TEWet);
    record.put("pCO₂ SST", calculator.pCO2SST);
    record.put("fCO₂", calculator.fCO2);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Equilibrator Temperature", "Equilibrator Pressure", getXCO2Parameter() };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(6);

      calculationParameters
        .add(new CalculationParameter(makeParameterId(0), "ΔT",
          "Water-Equilibrator Temperature Difference", "DELTAT", "°C", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "pH₂O", "Marine Water Vapour Pressure", "RH2OX0EQ", "hPa", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "pCO₂ TE Wet", "pCO₂ In Water - Equilibrator Temperature", "PCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "fCO₂ TE Wet", "fCO₂ In Water - Equilibrator Temperature", "FCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }

  protected String getXCO2Parameter() {
    return "xCO₂ (with standards)";
  }

  class Calculator {

    // Inputs
    private final Double intakeTemperature;
    private final Double salinity;
    private final Double equilibratorTemperature;
    private final Double equilibratorPressure;
    private final Double co2InGas;

    // Outputs
    protected Double pH2O = null;
    protected Double pCo2TEWet = null;
    protected Double fCo2TEWet = null;
    protected Double pCO2SST = null;
    protected Double fCO2 = null;

    protected Calculator(Double intakeTemperature, Double salinity,
      Double equilibratorTemperature, Double equilibratorPressure,
      Double co2InGas) {

      this.intakeTemperature = intakeTemperature;
      this.salinity = salinity;
      this.equilibratorTemperature = equilibratorTemperature;
      this.equilibratorPressure = equilibratorPressure;
      this.co2InGas = co2InGas;
      calculate();
    }

    protected void calculate() {
      pH2O = Calculators.calcPH2O(salinity, equilibratorTemperature);
      pCo2TEWet = Calculators.calcpCO2TEWet(co2InGas, equilibratorPressure,
        pH2O);
      fCo2TEWet = Calculators.calcfCO2(pCo2TEWet, co2InGas,
        equilibratorPressure, equilibratorTemperature);
      pCO2SST = Calculators.calcCO2AtSST(pCo2TEWet, equilibratorTemperature,
        intakeTemperature);
      fCO2 = Calculators.calcCO2AtSST(fCo2TEWet, equilibratorTemperature,
        intakeTemperature);
    }
  }
}
