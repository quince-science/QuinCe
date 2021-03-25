package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

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
  public CalculationCoefficient(Instrument instrument) {
    super(instrument,
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE);
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
    LocalDateTime deploymentDate, List<Double> coefficients) {

    super(id, instrument,
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE,
      target);
    setDeploymentDate(deploymentDate);
    setCoefficients(coefficients);
  }

  @Override
  public List<String> getCoefficientNames() {
    return valueNames;
  }

  @Override
  protected String buildHumanReadableCoefficients() {
    String result = "Not set";

    if (null != coefficients) {
      result = String.valueOf(coefficients.get(0).getValue());
    }

    return result;
  }

  @Override
  public boolean coefficientsValid() {
    return true;
  }

  @Override
  public Double calibrateValue(Double rawValue) {
    return rawValue;
  }

  public static Double getCoefficient(CalibrationSet calibrationSet,
    Variable variable, String coefficient) {

    Double result;

    try {
      result = calibrationSet
        .getCalibrationValue(variable.getId() + "." + coefficient, "Value");
    } catch (RecordNotFoundException e) {
      result = null;
    }

    return result;
  }

}
