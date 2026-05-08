package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;

/**
 * Implementation of the {@link CalibrationDB} class for handling
 * {@link CalculationCoefficient}s.
 */
public class CalculationCoefficientDB extends CalibrationDB {

  /**
   * The calibration type string stored in the database for
   * {@link CalculationCoefficient}s.
   */
  public static final String CALCULATION_COEFFICIENT_CALIBRATION_TYPE = "CALC_COEFFICIENT";

  /**
   * The singleton instance of this class.
   */
  private static CalculationCoefficientDB instance = null;

  /**
   * Get the {@link CalculationCoefficient} targets for the specified
   * {@link Instrument}.
   *
   * <p>
   * Unlike most concrete implementations of the {@link CalibrationDB} class,
   * the machine-readable and human-readable representations of each target are
   * not the same. The calculation coefficients are defined by the
   * {@link Variable} to which they apply along with the name of the
   * coefficient. The entries in the returned {@link Map} therefore take the
   * form {@code "<var_id>.<coefficient>" -> "<variable_name>: <coefficient>"}.
   */
  @Override
  public Map<String, String> getTargets(Connection conn, Instrument instrument)
    throws CalibrationException {

    // Return a map of '<v_id>.<coefficient>' -> '<varname>: <coefficient>'
    // If there's only one variable, leave the variable name off the
    // human-readable side
    try {
      Map<String, String> targets = new LinkedHashMap<String, String>();

      List<Variable> variablesWithCoefficients = instrument.getVariables()
        .stream().filter(v -> v.hasCoefficients()).collect(Collectors.toList());

      if (variablesWithCoefficients.size() > 0) {

        boolean oneVariable = variablesWithCoefficients.size() == 1;

        variablesWithCoefficients.forEach(v -> {
          v.getCoefficients().forEach(c -> {

            String key = v.getId() + "." + c;
            String value = oneVariable ? c : v.getName() + ": " + c;
            targets.put(key, value);
          });
        });
      }

      return targets;
    } catch (Exception e) {
      throw new CalibrationException(e);
    }
  }

  @Override
  public String getCalibrationType() {
    return CALCULATION_COEFFICIENT_CALIBRATION_TYPE;
  }

  /**
   * Retrieve the singleton instance of this class.
   *
   * @return The singleton.
   */
  public static CalculationCoefficientDB getInstance() {
    if (null == instance) {
      instance = new CalculationCoefficientDB();
    }

    return instance;
  }

  @Override
  public boolean allowCalibrationChangeInDataset() {
    return false;
  }

  @Override
  public boolean usePostCalibrations() {
    return true;
  }

  @Override
  public boolean timeAffectesCalibration() {
    return true;
  }

  @Override
  public boolean completeSetRequired() {
    return true;
  }
}
