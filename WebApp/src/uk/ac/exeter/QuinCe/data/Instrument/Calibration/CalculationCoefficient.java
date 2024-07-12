package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.DataReduction.Calculators;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

public class CalculationCoefficient extends Calibration {

  private static List<String> valueNames;

  static {
    valueNames = new ArrayList<String>();
    valueNames.add("Value");
  }

  /**
   * Create a calibration object
   *
   * @param instrumentId
   *          The instrument to which the calibration target belongs
   * @param target
   *          The calibration target (most likely a sensor)
   */
  public CalculationCoefficient(Instrument instrument, String target) {
    super(instrument,
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE,
      target);
  }

  /**
   * Create a calibration object with no target set
   *
   * @param instrumentId
   *          The instrument to which the calibration target belongs
   */
  public CalculationCoefficient(Instrument instrument, long id,
    LocalDateTime date) {
    super(instrument,
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE, id,
      date);
  }

  /**
   * Copy constructor.
   *
   * @param source
   *          The copy source.
   * @throws CalibrationException
   */
  protected CalculationCoefficient(CalculationCoefficient source)
    throws CalibrationException {
    super(source.getInstrument(),
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE,
      source.getId(), source.getDeploymentDate());
    setCoefficients(duplicateCoefficients(source));

  }

  /**
   * Construct a complete sensor calibration object.
   *
   * @param id
   *          The calibration's database ID
   * @param instrumentId
   *          The instrument ID
   * @param target
   *          The target sensor
   * @param deploymentDate
   *          The deployment date
   * @param coefficients
   *          The calibration coefficients
   * @throws CalibrationException
   *           If the calibration details are invalid
   */
  public CalculationCoefficient(long id, Instrument instrument, String target,
    LocalDateTime deploymentDate, Map<String, String> coefficients)
    throws CalibrationException {

    super(id, instrument,
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE,
      target);
    setDeploymentDate(deploymentDate);
    setCoefficients(coefficients);
  }

  @Override
  public List<String> getCoefficientNames(boolean includeHidden) {
    return valueNames;
  }

  @Override
  public boolean coefficientsValid() {
    return coefficients.size() == 1;
  }

  @Override
  public Double calibrateValue(Double rawValue) {
    return rawValue;
  }

  public static CalculationCoefficient getCoefficient(
    CalibrationSet calibrationSet, Variable variable, String coefficient,
    LocalDateTime time) {

    Calibration calibration = calibrationSet.getCalibrations(time)
      .get(getCoeffecientName(variable, coefficient));

    return null == calibration ? null : (CalculationCoefficient) calibration;
  }

  public static CalculationCoefficient getPostCoefficient(
    CalibrationSet calibrationSet, Variable variable, String coefficient,
    LocalDateTime time) {

    Calibration calibration = calibrationSet.getPostCalibrations(time)
      .get(getCoeffecientName(variable, coefficient));

    return null == calibration ? null : (CalculationCoefficient) calibration;
  }

  public static String getCoeffecientName(Variable variable,
    String coefficient) {
    return variable.getId() + "." + coefficient;
  }

  public static List<String> getCoeffecientNames(Variable variable,
    List<String> coefficients) {

    return coefficients.stream().map(c -> getCoeffecientName(variable, c))
      .toList();
  }

  public Double getValue() {
    return getDoubleCoefficient("Value");
  }

  public BigDecimal getBigDecimalValue() {
    return getBigDecimalCoefficient("Value");
  }

  public static Double interpolateDouble(CalculationCoefficient x0,
    CalculationCoefficient y0, CalculationCoefficient x1,
    CalculationCoefficient y1, Double x) {

    Double x0Value = null == x0 ? null : x0.getValue();
    Double y0Value = null == y0 ? null : y0.getValue();
    Double x1Value = null == x1 ? null : x1.getValue();
    Double y1Value = null == y1 ? null : y1.getValue();

    return Calculators.interpolate(x0Value, y0Value, x1Value, y1Value, x);
  }

  public static BigDecimal interpolateBigDecimal(CalculationCoefficient x0,
    CalculationCoefficient y0, CalculationCoefficient x1,
    CalculationCoefficient y1, BigDecimal x) {

    BigDecimal x0Value = null == x0 ? null : x0.getBigDecimalValue();
    BigDecimal y0Value = null == y0 ? null : y0.getBigDecimalValue();
    BigDecimal x1Value = null == x1 ? null : x1.getBigDecimalValue();
    BigDecimal y1Value = null == y1 ? null : y1.getBigDecimalValue();

    return Calculators.interpolate(x0Value, y0Value, x1Value, y1Value, x);
  }

  @Override
  public Calibration makeCopy() {
    try {
      return new CalculationCoefficient(this);
    } catch (CalibrationException e) {
      // This shouldn't happen, because it implies that we successfully created
      // in invalid object
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getCoefficientsLabel() {
    return "Calculation Coefficients";
  }

  @Override
  public String getJsonCoefficientsLabel() {
    return "calculationCoefficients";
  }
}
