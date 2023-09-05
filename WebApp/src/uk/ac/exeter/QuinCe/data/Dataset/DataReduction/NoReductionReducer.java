package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
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
    Map<String, Properties> properties) {

    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    SensorType coreType = variable.getCoreSensorType();
    record.put(coreType.getShortName(),
      measurement.getMeasurementValue(coreType).getCalculatedValue());
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {

    // The output parameters are the core sensor types defined for the variable

    List<CalculationParameter> result = new ArrayList<CalculationParameter>(1);

    SensorType coreType = variable.getCoreSensorType();
    result.add(new CalculationParameter(makeParameterId(0),
      coreType.getShortName(), coreType.getLongName(), coreType.getCodeName(),
      coreType.getUnits(), true));

    return result;
  }

}
