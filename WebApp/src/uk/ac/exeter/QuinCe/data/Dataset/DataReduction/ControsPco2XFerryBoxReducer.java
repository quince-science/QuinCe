package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ControsPco2XFerryBoxReducer extends ControsPco2Reducer {

  public ControsPco2XFerryBoxReducer(Variable variable,
    Map<String, Properties> properties, CalibrationSet calculationCoefficients)
    throws SensorTypeNotFoundException {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  protected void calcZeroS2Beams(DataSet dataset,
    List<Measurement> allMeasurements) throws SensorTypeNotFoundException {

    zeroS2Beams = new TreeMap<Double, Double>();

    CalculationCoefficient prior_beam = CalculationCoefficient.getCoefficient(
      calculationCoefficients, variable, "S'2beam,Z", dataset.getStart());
    CalculationCoefficient prior_runtime = CalculationCoefficient
      .getCoefficient(calculationCoefficients, variable, "Runtime",
        dataset.getStart());

    zeroS2Beams.put(prior_runtime.getValue(), prior_beam.getValue());

    if (!calculationCoefficients.hasCompletePost()) {
      CalculationCoefficient post_beam = CalculationCoefficient.getCoefficient(
        calculationCoefficients, variable, "S'2beam,Z", dataset.getStart());
      CalculationCoefficient post_runtime = CalculationCoefficient
        .getCoefficient(calculationCoefficients, variable, "Runtime",
          dataset.getStart());

      zeroS2Beams.put(post_runtime.getValue(), post_beam.getValue());
    }

    dataset.setProperty(variable, ZEROS_PROP, new Gson().toJson(zeroS2Beams));
  }
}
