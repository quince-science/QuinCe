package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.web.system.ResourceManager;

public class ControsPco2Reducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  private Map<Double, Double> zeroS2Beams;

  private final SensorType runtimeSensorType;

  private final SensorType rawSensorType;

  private final SensorType refSensorType;

  private CalibrationSet priorCoefficients;

  private CalibrationSet postCoefficients;

  public ControsPco2Reducer(Variable variable,
    Map<String, Properties> properties) throws SensorTypeNotFoundException {
    super(variable, properties);

    runtimeSensorType = ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType("Contros pCO₂ Runtime");
    rawSensorType = ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType("Contros pCO₂ Raw Detector Signal");
    refSensorType = ResourceManager.getInstance().getSensorsConfiguration()
      .getSensorType("Contros pCO₂ Reference Signal");
  }

  @Override
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements) throws Exception {

    priorCoefficients = CalculationCoefficientDB.getInstance()
      .getMostRecentCalibrations(conn, instrument,
        allMeasurements.get(0).getTime());

    postCoefficients = CalculationCoefficientDB.getInstance()
      .getCalibrationsAfter(conn, instrument,
        allMeasurements.get(allMeasurements.size() - 1).getTime());

    zeroS2Beams = new HashMap<Double, Double>();

    allMeasurements.forEach(measurement -> {
      if (measurement.getRunType(variable)
        .equals(Measurement.INTERNAL_CALIBRATION_RUN_TYPE)) {

        Double runTime = measurement.getMeasurementValue(runtimeSensorType)
          .getCalculatedValue();

        Double s2Beam = calcS2Beam(measurement);
        zeroS2Beams.put(runTime, s2Beam);
      }
    });

    System.out.println(zeroS2Beams);
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {
    record.put("Sparta", 300D);
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Contros pCO₂ Raw Detector Signal", "Contros pCO₂ Reference Signal",
      "Contros pCO₂ Zero Mode", "Contros pCO₂ Flush Mode",
      "Contros pCO₂ Runtime", "Contros pCO₂ Gas Stream Temperature",
      "Contros pCO₂ Gas Stream Pressure", "Contros pCO₂ Membrane Pressure",
      "Diagnostic Relative Humidity" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>(1);

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "Sparta", "Sparta", "SPARTA", "sl", false));
    }

    return calculationParameters;
  }

  private Double calcS2Beam(Measurement measurement) {
    Double s2Beam = measurement.getMeasurementValue(rawSensorType)
      .getCalculatedValue()
      / measurement.getMeasurementValue(refSensorType).getCalculatedValue();

    Double fTSensor = CalculationCoefficient.getCoefficient(priorCoefficients,
      variable, "f(Tsensor)");

    return s2Beam * fTSensor;
  }
}
