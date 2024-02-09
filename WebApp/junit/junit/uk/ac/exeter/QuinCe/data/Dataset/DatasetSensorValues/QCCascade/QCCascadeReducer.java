package junit.uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues.QCCascade;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.CalculationParameter;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReducer;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionException;
import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.DataReductionRecord;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class QCCascadeReducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  public QCCascadeReducer(Variable variable,
    Map<String, Properties> properties) {
    super(variable, properties);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      Double intakeTemperature = measurement
        .getMeasurementValue("Intake Temperature").getCalculatedValue();
      Double salinity = measurement.getMeasurementValue("Salinity")
        .getCalculatedValue();
      Double co2 = measurement.getMeasurementValue("xCO₂ (wet, no standards)")
        .getCalculatedValue();

      record.put("Sum", intakeTemperature + salinity + co2);
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(1);

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "Sum", "Sum", "SUM", "NONS", true));
    }

    return calculationParameters;
  }

}
