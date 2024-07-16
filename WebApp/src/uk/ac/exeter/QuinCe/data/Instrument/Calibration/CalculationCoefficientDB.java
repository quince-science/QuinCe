package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.InstrumentException;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.Variable;
import uk.ac.exeter.QuinCe.utils.DatabaseException;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public class CalculationCoefficientDB extends CalibrationDB {

  public static final String CALCULATION_COEFFICIENT_CALIBRATION_TYPE = "CALC_COEFFICIENT";

  /**
   * The singleton instance of the class
   */
  private static CalculationCoefficientDB instance = null;

  @Override
  public Map<String, String> getTargets(Connection conn, Instrument instrument)
    throws MissingParamException, DatabaseException, RecordNotFoundException,
    InstrumentException {

    // Return a map of '<v_id>.<coefficient>' -> '<varname>: <coefficient>'
    // If there's only one variable, leave the variable name off the
    // human-readable side

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
  }

  @Override
  public String getCalibrationType() {
    return CALCULATION_COEFFICIENT_CALIBRATION_TYPE;
  }

  /**
   * Retrieve the singleton instance of the class
   *
   * @return The singleton
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
