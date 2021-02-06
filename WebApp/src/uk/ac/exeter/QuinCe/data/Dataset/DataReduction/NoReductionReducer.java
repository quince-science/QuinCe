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
 * No data reduction performed.
 *
 */
public class NoReductionReducer extends DataReducer {

  public NoReductionReducer(Variable variable,
    Map<String, Properties> properties) {

    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] {};
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    return new ArrayList<CalculationParameter>(0);
  }

}
