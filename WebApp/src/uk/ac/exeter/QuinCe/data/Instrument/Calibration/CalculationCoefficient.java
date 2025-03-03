package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * A version of the {@link Calibration} class to hold coefficients used in data
 * reduction calculations.
 *
 * <p>
 * The {@link Calibration#target target} for a CalculationCoefficient is
 * typically the name of the coefficient in the data reduction calculation.
 * </p>
 *
 * <p>
 * CalculationCoefficients are usually defined for a specific {@link Variable}.
 * While a CalculationCoefficient is typically given a human-readable name (the
 * {@link Calibration#target target}), this class will override that name to
 * prevent clashes, with the format
 * {@code <variable.getId()>.<Coefficient target>}. This is done with calls to
 * {@link #getCoeffecientName(Variable, String)}.
 * </p>
 *
 * <p>
 * A CalculationCoefficient only ever has one member in
 * {@link Calibration#coefficients}. The class provides convenience methods to
 * retrieve the single value directly.
 * </p>
 *
 * <p>
 * I apologise to future me for the confusing naming of this class as
 * {@code CalculationCoffecients} when the parent {@link Calibration} class has
 * a field named {@link Calibration#coefficients coefficients}, which this class
 * does not really use because there's only ever one entry in that field and
 * convenience methods are supplied to access it.
 * </p>
 */
public class CalculationCoefficient extends Calibration {

  /**
   * A fixed set of coefficient names for the CalculationCoefficient.
   *
   * <p>
   * Each CalculationCoefficient can contain only one {@code coefficient}, which
   * is named "Value".
   * </p>
   */
  private static LinkedHashSet<String> valueNames;

  static {
    valueNames = new LinkedHashSet<String>();
    valueNames.add("Value");
  }

  /**
   * Create a new Calibration.
   *
   * <p>
   * <b>Note:</b> This constructor does not include the {@link #deploymentDate}.
   * This must be set separately using {@link #setDeploymentDate(LocalDateTime)}
   * because that will ensure that the coefficients are correctly initialised.
   * </p>
   *
   * @param instrument
   *          The instrument that the calibration will be applied to.
   * @param target
   *          The calibration target.
   * @see Calibration#Calibration(Instrument, String, String)
   */
  public CalculationCoefficient(Instrument instrument, String target) {
    super(instrument,
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE,
      target);
  }

  /**
   * Create a Calibration from a database record with no specified target.
   *
   * @param instrument
   *          The instrument that the calibration will be applied to.
   * @param id
   *          The calibration's database ID.
   * @param date
   *          The date of the calibration.
   * @see Calibration#Calibration(Instrument, String, long, LocalDateTime)
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
   *           If the coefficients cannot be copied. There is no reason why this
   *           should occur.
   */
  protected CalculationCoefficient(CalculationCoefficient source)
    throws CalibrationException {
    super(source.getInstrument(),
      CalculationCoefficientDB.CALCULATION_COEFFICIENT_CALIBRATION_TYPE,
      source.getId(), source.getDeploymentDate());
    setTarget(source.getTarget());
    setCoefficients(duplicateCoefficients(source));

  }

  /**
   * Construct a complete CalculationCoefficient object.
   *
   * @param id
   *          The database ID.
   * @param instrument
   *          The instrument that the Calibration belongs to.
   * @param target
   *          The target, most likely the name of the coefficient in the data
   *          reduction calculation.
   * @param deploymentDate
   *          The deployment date.
   * @param coefficients
   *          The value of the CalculationCoefficient.
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
  public LinkedHashSet<String> getCoefficientNames(boolean includeHidden) {
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

  /**
   * Retrieve the the named CalculationCoefficient defined immediately before
   * the specified {@code time} for a given {@link Variable}.
   *
   * <p>
   * Some data reduction routines requires CalculationCoefficients
   *
   * @param calibrationSet
   *          The set of calibrations for the instrument.
   * @param variable
   *          The Variable for which the CalculationCoefficient is defined.
   * @param coefficient
   *          The name of the CalculationCoefficient (the
   *          {@link Calibration#target}).
   * @param time
   *          The time when the selected CalculationCoefficient must be active.
   * @return The CalculationCoefficient that matches the passed parameters.
   * @see #getCoeffecientName(Variable, String)
   */
  public static CalculationCoefficient getCoefficient(
    CalibrationSet calibrationSet, Variable variable, String coefficient,
    LocalDateTime time) {

    Calibration calibration = calibrationSet.getCalibrations(time)
      .get(getCoeffecientName(variable, coefficient));

    return null == calibration ? null : (CalculationCoefficient) calibration;
  }

  /**
   * Retrieve the the named CalculationCoefficient defined immediately after the
   * specified {@code time} for a given {@link Variable}.
   *
   * <p>
   * Some data reduction routines requires CalculationCoefficients
   *
   * @param calibrationSet
   *          The set of calibrations for the instrument.
   * @param variable
   *          The Variable for which the CalculationCoefficient is defined.
   * @param coefficient
   *          The name of the CalculationCoefficient (the
   *          {@link Calibration#target}).
   * @param time
   *          The time when the selected CalculationCoefficient must be active.
   * @return The CalculationCoefficient that matches the passed parameters.
   * @see #getCoeffecientName(Variable, String)
   */
  public static CalculationCoefficient getPostCoefficient(
    CalibrationSet calibrationSet, Variable variable, String coefficient,
    LocalDateTime time) {

    CalculationCoefficient result;

    TreeMap<String, Calibration> postCalibrations = calibrationSet
      .getPostCalibrations(time);

    if (null == postCalibrations) {
      result = null;
    } else {
      Calibration calibration = postCalibrations
        .get(getCoeffecientName(variable, coefficient));
      result = null == calibration ? null
        : (CalculationCoefficient) calibration;
    }

    return result;
  }

  /**
   * Construct the unique name for a CalculationCoefficient based on the
   * {@link Variable} to which it applies.
   *
   * <p>
   * The unique name is constructed as
   * {@code <variable.getId()>.<Coefficient target>}
   * </p>
   *
   * @param variable
   *          The {@link Variable} that the CalculationCoefficient applies to.
   * @param coefficient
   *          The CalculationCoefficient name ({@link Calibration#target}).
   * @return The unique name.
   */
  public static String getCoeffecientName(Variable variable,
    String coefficient) {
    return variable.getId() + "." + coefficient;
  }

  /**
   * Construct the unique names for a list of CalculationCoefficinet names.
   *
   * @param variable
   *          The {@link Variable} that the coefficients apply to.
   * @param coefficients
   *          The CalculationCoefficient names (({@link Calibration#target}).
   * @return The unique names.
   * @see #getCoeffecientName(Variable, String)
   */
  public static List<String> getCoeffecientNames(Variable variable,
    List<String> coefficients) {

    return coefficients.stream().map(c -> getCoeffecientName(variable, c))
      .toList();
  }

  /**
   * Retrieve the single value from this CalculationCoefficient.
   *
   * @return The CalculationCoefficient value.
   */
  public Double getValue() {
    return getDoubleCoefficient("Value");
  }

  /**
   * Retrieve the single value from this CalculationCoefficient as a
   * {@link BigDecimal}.
   *
   * @return The CalculationCoefficient value.
   */
  public BigDecimal getBigDecimalValue() {
    return getBigDecimalCoefficient("Value");
  }

  @Override
  public Calibration makeCopy() {
    try {
      return new CalculationCoefficient(this);
    } catch (CalibrationException e) {
      // This shouldn't happen, because it implies that we successfully created
      // in invalid object previously
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

  @Override
  protected boolean timeAffectsCalibration() {
    return true;
  }
}
