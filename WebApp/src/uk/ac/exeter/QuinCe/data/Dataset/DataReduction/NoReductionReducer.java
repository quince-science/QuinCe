package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * "Dummy" reducer that does no calculations.
 *
 * <p>
 * The core sensor type values for the measurements are copied as the reducer's
 * output.
 * </p>
 *
 */
public class NoReductionReducer extends DataReducer {

  public NoReductionReducer(Variable variable,
    Map<String, Properties> properties,
    CalibrationSet calculationCoefficients) {

    super(variable, properties, calculationCoefficients);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    // No actions taken
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    return new ArrayList<CalculationParameter>();
  }

}
