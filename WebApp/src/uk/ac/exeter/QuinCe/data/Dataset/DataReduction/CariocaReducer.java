package uk.ac.exeter.QuinCe.data.Dataset.DataReduction;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Dataset.Measurement;
import uk.ac.exeter.QuinCe.data.Dataset.TimeDataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalculationCoefficient;
import uk.ac.exeter.QuinCe.data.Instrument.Calibration.CalibrationSet;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorTypeNotFoundException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Data Reducer for Carioca CO2 sensor.
 *
 * <p>
 * The calculations are taken from a calibration sheet I was sent. We really
 * need a proper reference for it. In the meantime, the calibration sheet is in
 * the Github repository at {@code Documentation/Calculations/Carioca}.
 * </p>
 */
public class CariocaReducer extends DataReducer {

  private static List<CalculationParameter> calculationParameters = null;

  private static final double S = 35;

  private BigDecimal A = null;

  private BigDecimal B = null;

  private BigDecimal C = null;

  private Double RL = null;

  private Double RH = null;

  private Double R1 = null;

  private Double a = null;

  private Double b = null;

  private Double c = null;

  private Double k = null;

  private Double kPrime = null;

  private Double A_T = null;

  private Double e1 = null;

  public CariocaReducer(Variable variable, Map<String, Properties> properties,
    CalibrationSet calculationCoefficients) throws SensorTypeNotFoundException {
    super(variable, properties, calculationCoefficients);
  }

  @Override
  public void preprocess(Connection conn, Instrument instrument,
    DataSet dataset, List<Measurement> allMeasurements)
    throws DataReductionException {

    try {
      TimeDataSet castDataset = (TimeDataSet) dataset;

      A = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "tempA", castDataset.getStartTime()).getBigDecimalValue();

      B = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "tempB", castDataset.getStartTime()).getBigDecimalValue();

      C = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "tempC", castDataset.getStartTime()).getBigDecimalValue();

      RL = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "tempRL", castDataset.getStartTime()).getValue();

      RH = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "tempRH", castDataset.getStartTime()).getValue();

      R1 = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "tempR1", castDataset.getStartTime()).getValue();

      a = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "co2a", castDataset.getStartTime()).getValue();

      b = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "co2b", castDataset.getStartTime()).getValue();

      c = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "co2c", castDataset.getStartTime()).getValue();

      k = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "co2k", castDataset.getStartTime()).getValue();

      kPrime = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "co2k'", castDataset.getStartTime()).getValue();

      A_T = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "A_T", castDataset.getStartTime()).getValue();

      e1 = CalculationCoefficient.getCoefficient(calculationCoefficients,
        variable, "e1", castDataset.getStartTime()).getValue();
    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  @Override
  public void doCalculation(Instrument instrument, Measurement measurement,
    DataReductionRecord record, Connection conn) throws DataReductionException {

    try {
      // Get data values
      // Note that the nm values have reversed names because variables can't
      // begin
      // with numbers
      Double Th = getAdjustedValue(measurement, "Th");
      Double Refb = getAdjustedValue(measurement, "Refb");
      Double Refh = getAdjustedValue(measurement, "Refh");
      Double nm810 = measurement.getMeasurementValue("810nm")
        .getCalculatedValue();
      Double nm596 = measurement.getMeasurementValue("596nm")
        .getCalculatedValue();
      Double nm434 = measurement.getMeasurementValue("434nm")
        .getCalculatedValue();

      // Temperature calculation
      Double K = (Th - Refh) / (Refb - Refh);

      Double R_temp = (R1 * (RH / (RH + R1)) + R1 * (K * RL) / (RL + R1)
        - (R1 * (K * RH) / (RH + R1)))
        / (1 - (RH / (RH + R1)) - ((K * RL) / (RL + R1))
          + (K * RH) / (RH + R1));

      // T = (1 / ( A + Bln(R) + C(ln(R))^3) ) - 273.15
      BigDecimal lnR = new BigDecimal(Math.log(R_temp));
      BigDecimal Bpart = B.multiply(lnR);
      BigDecimal Cpart = C.multiply(lnR.pow(3));

      Double kelvin = new BigDecimal(1)
        .divide(A.add(Bpart).add(Cpart), MathContext.DECIMAL128).doubleValue();
      Double celsius = Calculators.celsius(kelvin);

      // CO2 calculation
      Double e2 = 8.76277 - 0.04344 * kelvin + 0.00007256 * Math.pow(kelvin, 2);
      Double e3 = -0.005765 + 0.00058 * kelvin;

      // Solubility coefficient (Weiss 1974)
      Double alphaSalinityPart = S * (0.023517 - 0.023656 * (kelvin / 100)
        + 0.0047036 * Math.pow(kelvin / 100, 2));

      Double alpha = Math.exp(-60.2409 + 93.4517 * (100 / kelvin)
        + 23.3585 * Math.log(kelvin / 100) + alphaSalinityPart);

      // Dissociation constants of carbonic acid in seawater (Lueker et al.,
      // 2000)
      Double pK_1 = 3633.86 / kelvin - 61.2172 + 9.6777 * Math.log(kelvin)
        - 0.011555 * S + 0.0001152 * Math.pow(S, 2);

      Double K_1 = Math.pow(10, pK_1 * -1);

      Double pK_2 = 471.78 / kelvin + 25.929 - 3.16967 * Math.log(kelvin)
        - 0.01781 * S + 0.0001122 * Math.pow(S, 2);

      Double K_2 = Math.pow(10, pK_2 * -1);

      // Thymol blue dissociation constant (Zhang and Byrne, 1996)
      Double pK_i = 4.706 * (S / kelvin) + 26.33 - 7.17218 * Math.log10(kelvin)
        - 0.017316 * S;

      Double K_i = Math.pow(10, pK_i * -1);

      // Other parameters
      Double A_434 = kPrime + Math.log10(nm810 / nm434);
      Double A_596 = k + Math.log10(nm810 / nm596);

      Double R = A_434 / A_596;

      Double Amax = (e2 / (e2 - e1 * e3))
        * ((e2 - e1) * A_434 + (1 - e3) * A_596);

      Double X = (R * e2 - e3) / (1 - R * e1);

      // pCO2 calculation
      Double xTop = 1 - (c / A_T) * (1 / (1 + X));
      Double xBottom = 1 + ((2 * K_2) / K_i) * (1 / X);

      Double pCO2 = (((K_i * A_T) / (alpha * K_1)) * X * (xTop / xBottom))
        * Math.pow(10, 6);

      // pCO2 is slightly off which makes fCO2 quite a long way off
      // Double fCO2 = a * pCO2 + b;

      record.put("Water Temperature", celsius);
      record.put("K", K);
      record.put("R_temp", R_temp);
      record.put("lnR", lnR.doubleValue());
      record.put("Bpart", Bpart.doubleValue());
      record.put("Cpart", Cpart.doubleValue());
      record.put("kelvin", kelvin);
      record.put("e2", e2);
      record.put("e3", e3);
      record.put("alphaSalinityPart", alphaSalinityPart);
      record.put("alpha", alpha);
      record.put("pK_1", pK_1);
      record.put("K_1", K_1);
      record.put("pK_2", pK_2);
      record.put("K_2", K_2);
      record.put("pK_i", pK_i);
      record.put("K_i", K_i);
      record.put("A_434", A_434);
      record.put("A_596", A_596);
      record.put("R", R);
      record.put("X", X);
      record.put("xTop", xTop);
      record.put("xBottom", xBottom);
      record.put("Amax", Amax);
      record.put("pCO₂ SST", pCO2);
      // record.put("fCO₂", fCO2);

    } catch (Exception e) {
      throw new DataReductionException(e);
    }
  }

  private Double getAdjustedValue(Measurement measurement, String sensor)
    throws SensorTypeNotFoundException {
    Double measuredValue = measurement.getMeasurementValue(sensor)
      .getCalculatedValue();

    if (measuredValue > 4095 && measuredValue < 8191) {
      measuredValue -= 8192;
    }

    return measuredValue;
  }

  @Override
  public List<CalculationParameter> getCalculationParameters() {
    if (null == calculationParameters) {
      calculationParameters = new ArrayList<CalculationParameter>();

      calculationParameters.add(new CalculationParameter(makeParameterId(0),
        "Water Temperature", "Water Temperature", "TEMPPR01", "°C", false));

      calculationParameters.add(
        new CalculationParameter(makeParameterId(1), "K", "K", "K", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(2),
        "R_temp", "R_temp", "R_temp", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(3),
        "lnR", "lnR", "lnR", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(4),
        "Bpart", "Bpart", "Bpart", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(5),
        "Cpart", "Cpart", "Cpart", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(6),
        "kelvin", "kelvin", "kelvin", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(7),
        "e2", "e2", "e2", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(8),
        "e3", "e3", "e3", "", false));

      calculationParameters
        .add(new CalculationParameter(makeParameterId(9), "alphaSalinityPart",
          "alphaSalinityPart", "alphaSalinityPart", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(10),
        "alpha", "alpha", "alpha", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(11),
        "pK_1", "pK_1", "pK_1", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(12),
        "K_1", "K_1", "K_1", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(13),
        "pK_2", "pK_2", "pK_2", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(14),
        "K_2", "K_2", "K_2", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(15),
        "pK_i", "pK_i", "pK_i", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(16),
        "K_i", "K_i", "K_i", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(17),
        "A_434", "A_434", "A_434", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(18),
        "A_596", "A_596", "A_596", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(19),
        "R", "R", "R", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(20),
        "X", "X", "X", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(21),
        "xTop", "xTop", "xTop", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(22),
        "xBottom", "xBottom", "xBottom", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(23),
        "Amax", "Amax", "CARAMAX", "", false));

      calculationParameters.add(new CalculationParameter(makeParameterId(24),
        "pCO₂ SST", "pCO₂ In Water", "PCO2TK02", "μatm", true));

      calculationParameters.add(new CalculationParameter(makeParameterId(25),
        "fCO₂", "fCO₂ In Water", "FCO2XXXX", "μatm", true));
    }

    return calculationParameters;
  }
}
