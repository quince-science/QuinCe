package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ProOceanusAtmosphericCO2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public ProOceanusAtmosphericCO2Reducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {

    Double airTemperature = measurement.getMeasurementValue("Air Temperature")
      .getCalculatedValue();
    Double membranePressure = measurement
      .getMeasurementValue("Membrane Pressure").getCalculatedValue();
    Double xCO2 = measurement.getMeasurementValue("xCO₂ (wet, no standards)")
      .getCalculatedValue();

    Double p = Calculators.hPaToAtmospheres(membranePressure);
    Double pCO2 = xCO2 * p;
    Double fCO2 = Calculators.calcfCO2(pCO2, xCO2, p, airTemperature);

    record.put("pCO₂", pCO2);
    record.put("fCO₂", fCO2);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Membrane Pressure",
      "xCO₂ (wet, no standards)" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    calculationParameters = new ArrayList<CalculationParameter>(2);

    calculationParameters.add(new CalculationParameter(makeParameterId(0),
      "pCO₂", "pCO₂ In Atmosphere", "ACO2XXXX", "μatm", true));

    calculationParameters.add(new CalculationParameter(makeParameterId(1),
      "fCO₂", "fCO₂ In Atmosphere", "FCO2WTAT", "μatm", true));

    return calculationParameters;
  }
}
