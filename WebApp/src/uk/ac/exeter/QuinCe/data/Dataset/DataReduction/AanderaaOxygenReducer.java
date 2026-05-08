package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data Reducer for Aanderaa Oxygen optodes.
 *
 * <p>
 * <b>NOTE:</b> This is still in development, and should not be used for real
 * data. At the time of writing the algorithm is completely made up.
 * </p>
 */
public class AanderaaOxygenReducer extends DataReducer {

  private double offset = 0D;

  /**
   * The reducer's calculation parameters.
   */
  private static List<CalculationParameter> calculationParameters = null;

  public AanderaaOxygenReducer(Variable variable,
    Map<String, Properties> properties,
    CalibrationSet calculationCoefficients) {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements)
    throws DataReductionException {

    offset = CalculationCoefficient.getCoefficient(calculationCoefficients,
      variable, "Coef1", dataset.getStartTime()).getValue();
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {

      Double oxygen = measurement.getMeasurementValue("DOXY")
        .getCalculatedValue();

      record.put("Adjusted Oxygen Fake", oxygen + offset);

    } catch (Exception e) {
      throw new DataReductionException(e);
    }

  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(4);

      calculationParameters.add(
        new CalculationParameter(makeParameterId(0), "Adjusted Oxygen Fake",
          "Adjusted Oxygen Fake", "O2FAKE", "fake", false));
    }

    return calculationParameters;
  }

}
