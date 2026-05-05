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
import uk.ac.exeter.QuinCe.data.Dataset.TimeDataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.MeanCalculator;

public class ControsPco2Reducer extends DataReducer {

  private static final String MODE_PROPERTY = "zero_mode";

  private static final String MODE_CONTINUOUS = "Continuous";

  private static final String MODE_ZERO_BEFORE_SLEEP = "Zero before sleep";

  private static final String MODE_ZERO_AFTER_SLEEP = "Zero after sleep";

  protected static final String ZEROS_PROP = "contros.zeros";

  private static final BigDecimal T0 = new BigDecimal("273.15");

  private static final BigDecimal P0 = new BigDecimal("1013.25");

  private static List<CalculationParameter> calculationParameters = null;

  protected TreeMap<Double, Double> zeroS2Beams;

  private BigDecimal F = null;

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
    Map<String, Properties> properties, CalibrationSet calculationCoefficients)
    throws SensorTypeNotFoundException {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements)
    throws DataReductionException {

    try {
      TimeDataSet castDataset = (TimeDataSet) dataset;

      F = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "F", castDataset.getStartTime()).getBigDecimalValue();

      calcKSteps(castDataset);
      calcZeroS2Beams(castDataset, allMeasurements);
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  private void calcKSteps(TimeDataSet dataset) {
    k1Prior = CalculationCoefficient.getCoefficient(calculationCoefficients,
      variable, "k1", dataset.getStartTime());
    k2Prior = CalculationCoefficient.getCoefficient(calculationCoefficients,
      variable, "k2", dataset.getStartTime());
    k3Prior = CalculationCoefficient.getCoefficient(calculationCoefficients,
      variable, "k3", dataset.getStartTime());
    runTimePrior = CalculationCoefficient.getCoefficient(
      calculationCoefficients, variable, "Runtime", dataset.getStartTime());

    k1Post = CalculationCoefficient.getPostCoefficient(calculationCoefficients,
      variable, "k1", dataset.getEndTime());
    k2Post = CalculationCoefficient.getPostCoefficient(calculationCoefficients,
      variable, "k2", dataset.getEndTime());
    k3Post = CalculationCoefficient.getPostCoefficient(calculationCoefficients,
      variable, "k3", dataset.getEndTime());
    runTimePost = CalculationCoefficient.getPostCoefficient(
      calculationCoefficients, variable, "Runtime", dataset.getEndTime());

    if (null != k1Post && null != k2Post && null != k3Post
      && null != runTimePost) {

      BigDecimal runtimePeriod = runTimePost.getBigDecimalValue()
        .subtract(runTimePrior.getBigDecimalValue());

      if (k1Post.getBigDecimalValue().equals(k1Prior.getBigDecimalValue())) {
        k1Step = BigDecimal.ZERO;
      } else {
        BigDecimal k1Diff = k1Post.getBigDecimalValue()
          .subtract(k1Prior.getBigDecimalValue())
          .setScale(50, RoundingMode.HALF_UP);
        k1Step = k1Diff.divide(runtimePeriod, 50, RoundingMode.HALF_UP);
      }

      if (k2Post.getBigDecimalValue().equals(k2Prior.getBigDecimalValue())) {
        k2Step = BigDecimal.ZERO;
      } else {
        BigDecimal k2Diff = k2Post.getBigDecimalValue()
          .subtract(k2Prior.getBigDecimalValue());
        k2Step = k2Diff.divide(runtimePeriod, 50, RoundingMode.HALF_UP);
      }

      if (k3Post.getBigDecimalValue().equals(k3Prior.getBigDecimalValue())) {
        k3Step = BigDecimal.ZERO;
      } else {
        BigDecimal k3Diff = k3Post.getBigDecimalValue()
          .subtract(k3Prior.getBigDecimalValue());
        k3Step = k3Diff.divide(runtimePeriod, 50, RoundingMode.HALF_UP);
      }
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
  protected void calcZeroS2Beams(DataSet dataset,
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
      Double doubleRuntime = measurement.getMeasurementValue("Runtime")
        .getCalculatedValue();

      // A NaN Runtime is an invalid measurement. Skip it.
      if (!doubleRuntime.isNaN()) {

        // We use BigDecimals to maintain the precision on the k parameters,
        // which are on the order of 1e-10
        BigDecimal measurementRuntime = new BigDecimal(doubleRuntime);

        Double measurementS2Beam = calcS2Beam(measurement);

        Double zeroS2Beam;
        Double sProc;
        Double xco2;
        Double pCO2SST;
        Double fCO2;

        if (!measurementS2Beam.isNaN()) {
          BigDecimal bdMeasurementS2Beam = new BigDecimal(measurementS2Beam);

          Double interpZeroS2Beam = getInterpZeroS2Beam(
            measurementRuntime.doubleValue());

          if (null != interpZeroS2Beam) {

            try {
              BigDecimal bdZeroS2Beam = new BigDecimal(interpZeroS2Beam);

              BigDecimal sDC = bdMeasurementS2Beam.divide(bdZeroS2Beam,
                RoundingMode.HALF_UP);

              BigDecimal bdSProc = F.multiply(new BigDecimal(1D).subtract(sDC));

              BigDecimal runtimeSincePre = measurementRuntime
                .subtract(runTimePrior.getBigDecimalValue());

              BigDecimal k1Interp = k1Prior.getBigDecimalValue()
                .add(k1Step.multiply(runtimeSincePre));
              BigDecimal k2Interp = k2Prior.getBigDecimalValue()
                .add(k2Step.multiply(runtimeSincePre));
              BigDecimal k3Interp = k3Prior.getBigDecimalValue()
                .add(k3Step.multiply(runtimeSincePre));

              BigDecimal sProcCubed = bdSProc.pow(3);
              BigDecimal sProcSquared = bdSProc.pow(2);

              BigDecimal k3Part = k3Interp.multiply(sProcCubed);
              BigDecimal k2Part = k2Interp.multiply(sProcSquared);
              BigDecimal k1Part = k1Interp.multiply(bdSProc);

              BigDecimal xco2ProcPart = k3Part.add(k2Part).add(k1Part);

              // Gas temperature in Kelvin
              BigDecimal gasTemperature = new BigDecimal(
                measurement.getMeasurementValue("Gas Stream Temperature")
                  .getCalculatedValue())
                .add(T0);

              BigDecimal gasPressure = new BigDecimal(
                measurement.getMeasurementValue("Gas Stream Pressure")
                  .getCalculatedValue());

              BigDecimal membranePressure = new BigDecimal(measurement
                .getMeasurementValue("Membrane Pressure").getCalculatedValue());

              BigDecimal pressureTimesTemp = P0.multiply(gasTemperature);

              BigDecimal tempTimesPressure = T0.multiply(gasPressure);

              BigDecimal xcoPresTempPart = pressureTimesTemp
                .divide(tempTimesPressure, 50, RoundingMode.HALF_UP);

              BigDecimal bdXCO2 = xco2ProcPart.multiply(xcoPresTempPart);

              BigDecimal pco2PressurePart = membranePressure.divide(P0, 50,
                RoundingMode.HALF_UP);

              BigDecimal bdPCO2SST = bdXCO2.multiply(pco2PressurePart);

              Double waterTemp = measurement
                .getMeasurementValue("Water Temperature").getCalculatedValue();

              fCO2 = Calculators.calcfCO2(bdPCO2SST.doubleValue(),
                bdXCO2.doubleValue(), membranePressure.doubleValue(),
                waterTemp);

              // Make Double values for data reduction record
              zeroS2Beam = bdZeroS2Beam.doubleValue();
              sProc = bdSProc.doubleValue();
              xco2 = bdXCO2.doubleValue();
              pCO2SST = bdPCO2SST.doubleValue();
            } catch (NumberFormatException e) {
              /*
               * This will happen if any of the found measurement values are
               * NaN. As long as the CONTROS file isn't messed with, this
               * shouldn't happen.
               */
              zeroS2Beam = Double.NaN;
              sProc = Double.NaN;
              xco2 = Double.NaN;
              pCO2SST = Double.NaN;
              fCO2 = Double.NaN;
            }
          } else {
            zeroS2Beam = Double.NaN;
            sProc = Double.NaN;
            xco2 = Double.NaN;
            pCO2SST = Double.NaN;
            fCO2 = Double.NaN;
          }

          record.put("Zero S₂beam", zeroS2Beam.doubleValue());
          record.put("S₂beam",
            zeroS2Beam.isNaN() ? Double.NaN : measurementS2Beam.doubleValue());
          record.put("Sproc", sProc.doubleValue());
          record.put("xCO₂", xco2.doubleValue());
          record.put("pCO₂ SST", pCO2SST.doubleValue());
          record.put("fCO₂", fCO2);
        }
      }
    } catch (DataReductionException e) {
      throw e;
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

  private Double getInterpZeroS2Beam(Double runTime)
    throws DataReductionException {
    Map.Entry<Double, Double> prior = zeroS2Beams.floorEntry(runTime);
    Map.Entry<Double, Double> post = zeroS2Beams.ceilingEntry(runTime);

    Double result;

    switch (getStringProperty(MODE_PROPERTY)) {
    case MODE_CONTINUOUS: {
      result = Calculators.interpolate(prior, post, runTime);
      break;
    }
    case MODE_ZERO_AFTER_SLEEP: {
      result = null == prior ? null : prior.getValue();
      break;
    }
    case MODE_ZERO_BEFORE_SLEEP: {
      result = null == post ? null : post.getValue();
      break;
    }
    default:
      throw new DataReductionException("Invalid zero mode");
    }

    return result;
  }
}
