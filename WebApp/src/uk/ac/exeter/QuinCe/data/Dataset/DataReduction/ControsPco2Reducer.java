package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ControsPco2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public ControsPco2Reducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {
    record.put("Sparta", 300D);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Contros pCO₂ Raw Detector Signal", "Contros pCO₂ Reference Signal",
      "Contros pCO₂ Zero Mode", "Contros pCO₂ Flush Mode",
      "Contros pCO₂ Runtime", "Contros pCO₂ Gas Stream Temperature",
      "Contros pCO₂ Gas Stream Pressure", "Contros pCO₂ Membrane Pressure",
      "Diagnostic Relative Humidity" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(1);

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "Sparta", "Sparta", "SPARTA", "sl", false));
    }

    return calculationParameters;
  }
}
