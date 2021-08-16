package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import uk.ac.exeter.QuinCe.utils.MeanCalculator;

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

    // We calculate zero beams as averages within their run
    String currentRunType = "";
    MeanCalculator runTimes = new MeanCalculator();
    MeanCalculator s2Beams = new MeanCalculator();

    for (Measurement measurement : allMeasurements) {

      String runType = measurement.getRunType(variable);

      if (!runType.equals(currentRunType)) {
        if (currentRunType.equals(Measurement.INTERNAL_CALIBRATION_RUN_TYPE)) {
          if (runTimes.getCount() > 0) {
            zeroS2Beams.put(runTimes.mean(), s2Beams.mean());
            runTimes = new MeanCalculator();
            s2Beams = new MeanCalculator();
          }
        }
        currentRunType = runType;
      }

      if (runType.equals(Measurement.INTERNAL_CALIBRATION_RUN_TYPE)) {

        Double rawSignal = measurement
          .getMeasurementValue("Contros pCO₂ Raw Detector Signal")
          .getCalculatedValue();

        if (!rawSignal.isNaN()) {
          runTimes.add(measurement.getMeasurementValue("Contros pCO₂ Runtime")
            .getCalculatedValue());
          s2Beams.add(calcS2Beam(measurement));
        }
      }
    }

    if (runTimes.getCount() > 0) {
      zeroS2Beams.put(runTimes.mean(), s2Beams.mean());
      runTimes = new MeanCalculator();
      s2Beams = new MeanCalculator();
    }

    dataset.setProperty(variable, ZEROS_PROP, new Gson().toJson(zeroS2Beams));
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws Exception {

    // We use BigDecimals to maintain the precision on the k parameters,
    // which are on the order of 1e-10

    BigDecimal F = new BigDecimal(CalculationCoefficient
      .getCoefficient(priorCoefficients, variable, "F").getValue());

    BigDecimal measurementRunTime = new BigDecimal(measurement
      .getMeasurementValue("Contros pCO₂ Runtime").getCalculatedValue());

    Double measurementS2Beam = calcS2Beam(measurement);

    if (!measurementS2Beam.isNaN()) {
      BigDecimal bdMeasurementS2Beam = new BigDecimal(measurementS2Beam);
      BigDecimal zeroS2Beam = new BigDecimal(
        getInterpZeroS2Beam(measurementRunTime.doubleValue()));

      BigDecimal sProc = F.multiply(new BigDecimal(1D).subtract(
        (bdMeasurementS2Beam.divide(zeroS2Beam, 10, RoundingMode.HALF_UP))));

      BigDecimal k1Interp = CalculationCoefficient.interpolateBigDecimal(
        runTimePrior, k1Prior, runTimePost, k1Post, measurementRunTime);
      BigDecimal k2Interp = CalculationCoefficient.interpolateBigDecimal(
        runTimePrior, k2Prior, runTimePost, k2Post, measurementRunTime);
      BigDecimal k3Interp = CalculationCoefficient.interpolateBigDecimal(
        runTimePrior, k3Prior, runTimePost, k3Post, measurementRunTime);

      BigDecimal sProcCubed = sProc.pow(3);
      BigDecimal sProcSquared = sProc.pow(2);

      BigDecimal k3Part = k3Interp.multiply(sProcCubed);
      BigDecimal k2Part = k2Interp.multiply(sProcSquared);
      BigDecimal k1Part = k1Interp.multiply(sProc);

      // We can drop back to double precision now
      Double xco2ProcPart = k3Part.add(k2Part).add(k1Part).doubleValue();

      Double gasTemperature = measurement
        .getMeasurementValue("Gas Stream Temperature").getCalculatedValue()
        + T0;
      Double gasPressure = measurement
        .getMeasurementValue("Gas Stream Pressure").getCalculatedValue();

      Double xco2PresTempPart = (P0 * gasTemperature) / (T0 * gasPressure);

      Double xco2 = xco2ProcPart * xco2PresTempPart;

      Double membranePressure = measurement
        .getMeasurementValue("Membrane Pressure").getCalculatedValue();

      Double pCo2TEWet = xco2 * (membranePressure / P0);
      Double fCo2TEWet = Calculators.calcfCO2(pCo2TEWet, xco2, membranePressure,
        gasTemperature);

      Double sst = measurement.getMeasurementValue("Intake Temperature")
        .getCalculatedValue() + T0;
      Double membraneTemp = measurement
        .getMeasurementValue("Membrane Temperature").getCalculatedValue() + T0;

      Double pCO2SST = Calculators.calcCO2AtSST(pCo2TEWet, membraneTemp, sst);
      Double fCO2 = Calculators.calcCO2AtSST(fCo2TEWet, membraneTemp, sst);

      record.put("Zero S₂beam", zeroS2Beam.doubleValue());
      record.put("S₂beam", measurementS2Beam.doubleValue());
      record.put("Sproc", sProc.doubleValue());
      record.put("xCO₂", xco2);
      record.put("pCO₂ TE Wet", pCo2TEWet);
      record.put("fCO₂ TE Wet", fCo2TEWet);
      record.put("pCO₂ SST", pCO2SST);
      record.put("fCO₂", fCO2);
    }
  }

  @Override
  protected String[] getRequiredTypeStrings() {
    return new String[] { "Intake Temperature", "Salinity",
      "Contros pCO₂ Raw Detector Signal", "Contros pCO₂ Reference Signal",
      "Contros pCO₂ Zero Mode", "Contros pCO₂ Flush Mode",
      "Contros pCO₂ Runtime", "Gas Stream Temperature", "Gas Stream Pressure",
      "Membrane Temperature", "Membrane Pressure",
      "Diagnostic Relative Humidity" };
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
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
        "pCO₂ TE Wet", "pCO₂ In Water - Equilibrator Temperature", "PCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "fCO₂ TE Wet", "fCO₂ In Water - Equilibrator Temperature", "FCO2IG02",
        "μatm", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(6),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(7),
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
    return CalculationCoefficient.getCoefficient(postCoefficients, variable,
      coefficient);
  }

  private Double getInterpZeroS2Beam(Double runTime) {
    Map.Entry<Double, Double> prior = zeroS2Beams.floorEntry(runTime);
    Map.Entry<Double, Double> post = zeroS2Beams.ceilingEntry(runTime);
    return Calculators.interpolate(prior, post, runTime);
  }
}
