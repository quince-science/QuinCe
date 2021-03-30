package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficientDB;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class ControsPco2Reducer extends DataReducer {

  public static final String ZEROS_PROP = "contros.zeros";

  private static final double P0 = 1013.25;

  private static final double T0 = 273.15;

  private static List<CalculationParameter> calculationParameters = null;

  private TreeMap<Double, Double> zeroS2Beams;

  private CalibrationSet priorCoefficients;

  private CalibrationSet postCoefficients;

  private CalculationCoefficient k1Prior = null;

  private CalculationCoefficient k2Prior = null;

  private CalculationCoefficient k3Prior = null;

  private CalculationCoefficient runTimePrior = null;

  private CalculationCoefficient k1Post = null;

  private CalculationCoefficient k2Post = null;

  private CalculationCoefficient k3Post = null;

  private CalculationCoefficient runTimePost = null;

  public ControsPco2Reducer(Variable variable,
    Map<String, Properties> properties) throws SensorTypeNotFoundException {
    super(variable, properties);
  }

  @Override
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements) throws Exception {

    // Get prior and post coefficients
    priorCoefficients = CalculationCoefficientDB.getInstance()
      .getMostRecentCalibrations(conn, instrument,
        allMeasurements.get(0).getTime());

    postCoefficients = CalculationCoefficientDB.getInstance()
      .getCalibrationsAfter(conn, instrument,
        allMeasurements.get(allMeasurements.size() - 1).getTime());

    // Extract coefficients that will be used multiple times
    k1Prior = CalculationCoefficient.getCoefficient(priorCoefficients, variable,
      "k1");
    k2Prior = CalculationCoefficient.getCoefficient(priorCoefficients, variable,
      "k2");
    k3Prior = CalculationCoefficient.getCoefficient(priorCoefficients, variable,
      "k3");
    runTimePrior = CalculationCoefficient.getCoefficient(priorCoefficients,
      variable, "Runtime");

    k1Post = getPost("k1");
    k2Post = getPost("k2");
    k3Post = getPost("k3");
    runTimePost = getPost("Runtime");

    // Calculate zero Sbeam values
    zeroS2Beams = new TreeMap<Double, Double>();

    for (Measurement measurement : allMeasurements) {
      if (measurement.getRunType(variable)
        .equals(Measurement.INTERNAL_CALIBRATION_RUN_TYPE)) {

        Double runTime = measurement.getMeasurementValue("Contros pCO₂ Runtime")
          .getCalculatedValue();

        Double s2Beam = calcS2Beam(measurement);
        zeroS2Beams.put(runTime, s2Beam);
      }
    }

    dataset.setProperty(variable, ZEROS_PROP, new Gson().toJson(zeroS2Beams));
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {

    Double F = CalculationCoefficient
      .getCoefficient(priorCoefficients, variable, "F").getValue();

    Double measurementRunTime = measurement
      .getMeasurementValue("Contros pCO₂ Runtime").getCalculatedValue();

    Double measurementS2Beam = calcS2Beam(measurement);
    Double zeroS2Beam = getInterpZeroS2Beam(measurementRunTime);

    Double sProc = F * (1 - (measurementS2Beam / zeroS2Beam));

    Double k1Interp = CalculationCoefficient.interpolate(runTimePrior, k1Prior,
      runTimePost, k1Post, measurementRunTime);
    Double k2Interp = CalculationCoefficient.interpolate(runTimePrior, k2Prior,
      runTimePost, k2Post, measurementRunTime);
    Double k3Interp = CalculationCoefficient.interpolate(runTimePrior, k3Prior,
      runTimePost, k3Post, measurementRunTime);

    Double xco2ProcPart = (k3Interp * Math.pow(sProc, 3))
      + (k2Interp * Math.pow(sProc, 2)) + (k1Interp * sProc);

    Double gasTemperature = measurement
      .getMeasurementValue("Contros pCO₂ Gas Stream Temperature")
      .getCalculatedValue() + T0;
    Double gasPressure = measurement
      .getMeasurementValue("Contros pCO₂ Gas Stream Pressure")
      .getCalculatedValue();

    Double xco2PresTempPart = (P0 * gasTemperature) / (T0 * gasPressure);

    Double xco2 = xco2ProcPart * xco2PresTempPart;

    Double membranePressure = measurement
      .getMeasurementValue("Contros pCO₂ Membrane Pressure")
      .getCalculatedValue();

    Double pCo2TEWet = xco2 * (membranePressure / P0);
    Double fCo2TEWet = Calculators.calcfCO2(pCo2TEWet, xco2, membranePressure,
      gasTemperature);

    Double sst = measurement.getMeasurementValue("Intake Temperature")
      .getCalculatedValue();

    Double pCO2SST = Calculators.calcCO2AtSST(pCo2TEWet, gasTemperature, sst);
    Double fCO2 = Calculators.calcCO2AtSST(fCo2TEWet, gasTemperature, sst);

    record.put("Zero S₂beam", zeroS2Beam);
    record.put("S₂beam", measurementS2Beam);
    record.put("Sproc", sProc);
    record.put("pCO₂ TE Wet", pCo2TEWet);
    record.put("fCO₂ TE Wet", fCo2TEWet);
    record.put("pCO₂ SST", pCO2SST);
    record.put("fCO₂", fCO2);
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
      calculationParameters = new ArrayList<CalculationParameter>(4);

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "Zero S₂beam", "Interpolated Zero Signal", "CONZERO2BEAM", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "S₂beam", "Two-beam Signal", "CON2BEAM", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(1),
        "Sproc", "Drift-corrected Signal", "CONSPROC", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "pCO₂ TE Wet", "pCO₂ In Water - Equilibrator Temperature", "PCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "fCO₂ TE Wet", "fCO₂ In Water - Equilibrator Temperature", "FCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }

  private Double calcS2Beam(Measurement measurement)
    throws SensorTypeNotFoundException {
    Double s2Beam = measurement
      .getMeasurementValue("Contros pCO₂ Raw Detector Signal")
      .getCalculatedValue()
      / measurement.getMeasurementValue("Contros pCO₂ Reference Signal")
        .getCalculatedValue();

    Double fTSensor = CalculationCoefficient
      .getCoefficient(priorCoefficients, variable, "f(Tsensor)").getValue();

    return s2Beam * fTSensor;
  }

  private CalculationCoefficient getPost(String coefficient) {
    return null == postCoefficients ? null
      : CalculationCoefficient.getCoefficient(postCoefficients, variable,
        coefficient);
  }

  private Double getInterpZeroS2Beam(Double runTime) {
    Map.Entry<Double, Double> prior = zeroS2Beams.floorEntry(runTime);
    Map.Entry<Double, Double> post = zeroS2Beams.ceilingEntry(runTime);
    return Calculators.interpolate(prior, post, runTime);
  }
}
