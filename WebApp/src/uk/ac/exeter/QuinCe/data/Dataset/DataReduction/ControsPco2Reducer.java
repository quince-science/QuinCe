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

  private static final BigDecimal T0 = new BigDecimal("273.15");

  private static final BigDecimal P0 = new BigDecimal("1013.25");

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

  private BigDecimal k1Step = null;

  private BigDecimal k2Step = null;

  private BigDecimal k3Step = null;

  public ControsPco2Reducer(Variable variable,
    Map<String, Properties> properties) throws SensorTypeNotFoundException {
    super(variable, properties);
  }

  @Override
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements)
    throws DataReductionException {

    try {
      // Get prior and post coefficients
      priorCoefficients = CalculationCoefficientDB.getInstance()
        .getMostRecentCalibrations(conn, instrument,
          allMeasurements.get(0).getTime());

      postCoefficients = CalculationCoefficientDB.getInstance()
        .getCalibrationsAfter(conn, instrument,
          allMeasurements.get(allMeasurements.size() - 1).getTime());

      calcKSteps();

      calcZeroS2Beams(dataset, allMeasurements);

    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  private void calcKSteps() {
    k1Prior = CalculationCoefficient.getCoefficient(priorCoefficients, variable,
      "k1");
    k2Prior = CalculationCoefficient.getCoefficient(priorCoefficients, variable,
      "k2");
    k3Prior = CalculationCoefficient.getCoefficient(priorCoefficients, variable,
      "k3");
    runTimePrior = CalculationCoefficient.getCoefficient(priorCoefficients,
      variable, "Runtime");

    List<String> coefficientFullNames = CalculationCoefficient
      .getCoeffecientNames(variable, "Runtime", "k1", "k2", "k3");

    if (postCoefficients.containsTargets(coefficientFullNames)) {
      k1Post = getPost("k1");
      k2Post = getPost("k2");
      k3Post = getPost("k3");
      runTimePost = getPost("Runtime");

      BigDecimal runtimePeriod = runTimePost.getBigDecimalValue()
        .subtract(runTimePrior.getBigDecimalValue());

      BigDecimal k1Diff = k1Post.getBigDecimalValue()
        .subtract(k1Prior.getBigDecimalValue())
        .setScale(50, RoundingMode.HALF_UP);
      k1Step = k1Diff.divide(runtimePeriod, 50, RoundingMode.HALF_UP);

      BigDecimal k2Diff = k2Post.getBigDecimalValue()
        .subtract(k2Prior.getBigDecimalValue());
      k2Step = k2Diff.divide(runtimePeriod, 50, RoundingMode.HALF_UP);

      BigDecimal k3Diff = k3Post.getBigDecimalValue()
        .subtract(k3Prior.getBigDecimalValue());
      k3Step = k3Diff.divide(runtimePeriod, 50, RoundingMode.HALF_UP);
    } else {
      k1Step = BigDecimal.ZERO;
      k2Step = BigDecimal.ZERO;
      k3Step = BigDecimal.ZERO;
    }
  }

  /**
   * Calculate zero S₂beam values
   *
   * @throws SensorTypeNotFoundException
   */
  private void calcZeroS2Beams(DataSet dataset,
    List<Measurement> allMeasurements) throws SensorTypeNotFoundException {
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
          .getMeasurementValue("Raw Detector Signal").getCalculatedValue();

        if (!rawSignal.isNaN()) {
          runTimes.add(
            measurement.getMeasurementValue("Runtime").getCalculatedValue());
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
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      // We use BigDecimals to maintain the precision on the k parameters,
      // which are on the order of 1e-10

      BigDecimal F = new BigDecimal(CalculationCoefficient
        .getCoefficient(priorCoefficients, variable, "F").getValue());

      BigDecimal measurementRuntime = new BigDecimal(
        measurement.getMeasurementValue("Runtime").getCalculatedValue());

      Double measurementS2Beam = calcS2Beam(measurement);

      if (!measurementS2Beam.isNaN()) {
        BigDecimal bdMeasurementS2Beam = new BigDecimal(measurementS2Beam);
        BigDecimal zeroS2Beam = new BigDecimal(
          getInterpZeroS2Beam(measurementRuntime.doubleValue()));

        BigDecimal sDC = bdMeasurementS2Beam.divide(zeroS2Beam,
          RoundingMode.HALF_UP);

        BigDecimal sProc = F.multiply(new BigDecimal(1D).subtract(sDC));

        BigDecimal k1Interp = k1Prior.getBigDecimalValue()
          .add(k1Step.multiply(measurementRuntime));
        BigDecimal k2Interp = k2Prior.getBigDecimalValue()
          .add(k2Step.multiply(measurementRuntime));
        BigDecimal k3Interp = k3Prior.getBigDecimalValue()
          .add(k3Step.multiply(measurementRuntime));

        BigDecimal sProcCubed = sProc.pow(3);
        BigDecimal sProcSquared = sProc.pow(2);

        BigDecimal k3Part = k3Interp.multiply(sProcCubed);
        BigDecimal k2Part = k2Interp.multiply(sProcSquared);
        BigDecimal k1Part = k1Interp.multiply(sProc);

        BigDecimal xco2ProcPart = k3Part.add(k2Part).add(k1Part);

        // Gas temperature in Kelvin
        BigDecimal gasTemperature = new BigDecimal(measurement
          .getMeasurementValue("Gas Stream Temperature").getCalculatedValue())
          .add(T0);

        BigDecimal gasPressure = new BigDecimal(measurement
          .getMeasurementValue("Gas Stream Pressure").getCalculatedValue());

        BigDecimal membranePressure = new BigDecimal(measurement
          .getMeasurementValue("Membrane Pressure").getCalculatedValue());

        BigDecimal pressureTimesTemp = P0.multiply(gasTemperature);

        BigDecimal tempTimesPressure = T0.multiply(gasPressure);

        BigDecimal xcoPresTempPart = pressureTimesTemp.divide(tempTimesPressure,
          50, RoundingMode.HALF_UP);

        BigDecimal xco2 = xco2ProcPart.multiply(xcoPresTempPart);

        BigDecimal pco2PressurePart = membranePressure.divide(P0, 50,
          RoundingMode.HALF_UP);

        BigDecimal pCO2SST = xco2.multiply(pco2PressurePart);
        Double fCO2 = Calculators.calcfCO2(pCO2SST.doubleValue(),
          xco2.doubleValue(), membranePressure.doubleValue(),
          gasTemperature.doubleValue());

        record.put("Zero S₂beam", zeroS2Beam.doubleValue());
        record.put("S₂beam", measurementS2Beam.doubleValue());
        record.put("Sproc", sProc.doubleValue());
        record.put("xCO₂", xco2.doubleValue());
        record.put("pCO₂ SST", pCO2SST.doubleValue());
        record.put("fCO₂", fCO2);
      }
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
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
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }

  private Double calcS2Beam(Measurement measurement)
    throws SensorTypeNotFoundException {
    return measurement.getMeasurementValue("Raw Detector Signal")
      .getCalculatedValue()
      / measurement.getMeasurementValue("Reference Signal")
        .getCalculatedValue();
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
