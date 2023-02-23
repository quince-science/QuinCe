package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class UnderwayMarine12_13Pco2Reducer extends UnderwayMarinePco2Reducer {

  public UnderwayMarine12_13Pco2Reducer(Variable variable,
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

    // xCO2 values are dried as part of sorting out their Calculated Value
    Double x12CO2 = measurement.getMeasurementValue("x¹²CO₂ (with standards)")
      .getCalculatedValue();
    Double x13CO2 = measurement.getMeasurementValue("x¹³CO₂ (with standards)")
      .getCalculatedValue();

    Double pH2O = Calculators.calcPH2O(salinity, equilibratorTemperature);

    Calculator x12CO2Calculator = new Calculator(intakeTemperature,
      equilibratorTemperature, equilibratorPressure, pH2O, x12CO2);
    Calculator x13CO2Calculator = new Calculator(intakeTemperature,
      equilibratorTemperature, equilibratorPressure, pH2O, x13CO2);

    // Store the calculated values
    record.put("ΔT", Math.abs(intakeTemperature - equilibratorTemperature));
    record.put("pH₂O", pH2O);
    record.put("pCO₂ TE Wet",
      x12CO2Calculator.pCo2TEWet + x13CO2Calculator.pCo2TEWet);
    record.put("fCO₂ TE Wet",
      x12CO2Calculator.fCo2TEWet + x13CO2Calculator.fCo2TEWet);
    record.put("pCO₂ SST", x12CO2Calculator.pCO2SST + x13CO2Calculator.pCO2SST);
    record.put("fCO₂", x12CO2Calculator.fCO2 + x13CO2Calculator.fCO2);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Equilibrator Temperature", "Equilibrator Pressure",
      "xH₂O (with standards)", "x¹²CO₂ (with standards)",
      "x¹³CO₂ (with standards)" };
  }
}
