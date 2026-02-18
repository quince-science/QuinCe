package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ControsPco2XFerryBoxReducer extends ControsPco2Reducer {

  Double priorBeam = null;
  Double priorRuntime = null;
  Double postBeam = null;
  Double postRuntime = null;

  public ControsPco2XFerryBoxReducer(Variable variable,
    Map<String, Properties> properties, CalibrationSet calculationCoefficients)
    throws SensorTypeNotFoundException {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  protected Double getZeroS2BeamRawSignal(DataSet dataset,
    Measurement measurement) throws SensorTypeNotFoundException {

    if (null == priorBeam) {
      priorBeam = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "S'2beam,Z", dataset.getStart()).getValue();
      priorRuntime = CalculationCoefficient
        .getCoefficient(calculationCoefficients, variable, "Runtime",
          dataset.getStart())
        .getValue();

      if (calculationCoefficients.hasCompletePost()) {
        postBeam = CalculationCoefficient
          .getCoefficient(calculationCoefficients, variable, "S'2beam,Z",
            dataset.getStart())
          .getValue();
        postRuntime = CalculationCoefficient
          .getCoefficient(calculationCoefficients, variable, "Runtime",
            dataset.getStart())
          .getValue();
      }
    }

    return Calculators.interpolate(priorRuntime, priorBeam, postRuntime,
      postBeam,
      measurement.getMeasurementValue("Runtime").getCalculatedValue());
  }
}
