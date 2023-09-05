package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class DExcessReducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public DExcessReducer(Variable variable, Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      Double dH218O = measurement.getMeasurementValue("δH₂¹⁸O")
        .getCalculatedValue();
      Double dHD16O = measurement.getMeasurementValue("δHD¹⁶O")
        .getCalculatedValue();

      record.put("D-Excess", dHD16O - 8 * dH218O);
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(1);

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "D-Excess", "D-Excess", "DEXCESS", "permil", true));
    }

    return calculationParameters;
  }
}
