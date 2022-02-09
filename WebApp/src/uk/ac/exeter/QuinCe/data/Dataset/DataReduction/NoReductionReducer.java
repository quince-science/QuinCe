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
    DataReductionRecord record, Connection conn) throws Exception {

    for (SensorType sensorType : variable.getCoreSensorTypes()) {
      record.put(sensorType.getShortName(),
        measurement.getMeasurementValue(sensorType).getCalculatedValue());
    }
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] {};
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {

    // The output parameters are the core sensor types defined for the variable

    List<CalculationParameter> result = new ArrayList<CalculationParameter>(0);

    int param = 0;

    for (SensorType sensorType : variable.getCoreSensorTypes()) {
      result.add(new CalculationParameter(makeParameterId(param),
        sensorType.getShortName(), sensorType.getLongName(),
        sensorType.getCodeName(), sensorType.getUnits(), true));

      param++;
    }

    return result;
  }

}
