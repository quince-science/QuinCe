package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ControsPco2XFerryBoxReducer extends ControsPco2Reducer {

  private static List<CalculationParameter> calculationParameters = null;

  public ControsPco2XFerryBoxReducer(Variable variable,
    Map<String, Properties> properties, CalibrationSet calculationCoefficients)
    throws SensorTypeNotFoundException {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  protected void calcZeroS2Beams(DataSet dataset,
    List<Measurement> allMeasurements) throws SensorTypeNotFoundException {

    // Get the S2Beams from the data
    super.calcZeroS2Beams(dataset, allMeasurements);

    // Add the S2Beams from the calibrations
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

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    /*
     * This is a direct copy of the parent method. However, this is required to
     * enable use of the static calculationParameters. Otherwise the two classes
     * fight with each other.
     */

    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(8);

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "Zero S₂beam", "Interpolated Zero Signal", "CONZERO2BEAM", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "S₂beam", "Two-beam Signal", "CON2BEAM", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "Sproc", "Drift-corrected Signal", "CONSPROC", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "xCO₂", "xCO₂ In Water", "XCO2WBDY", "μmol/mol", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }
}
