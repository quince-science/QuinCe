package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition.SensorType;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

/**
 * Class representing a set of calibrations of a given type for a given
 * instrument.
 *
 * <p>
 * Calibrations can only be added to the set if they are for the correct
 * instrument and of the correct type.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class CalibrationSet extends TreeSet<Calibration> {

  /**
   * Serial version UID
   */
  private static final long serialVersionUID = 1647218597328709319L;

  /**
   * The instrument to which this calibration set belongs
   */
  private Instrument instrument;

  /**
   * The calibration type that is allowed in this set
   */
  private String type;

  /**
   * The set of targets that can be contained in this set
   */
  private Map<String, String> targets;

  /**
   * Initialise an empty calibration set
   *
   * @param instrumentId
   *          The ID of the instrument to which the calibrations will belong
   * @param type
   *          The calibration type
   * @param targets
   *          The set of targets for the calibration set
   * @throws MissingParamException
   *           If any required parameters are missing
   */
  protected CalibrationSet(Instrument instrument, String type,
    Map<String, String> targets) throws MissingParamException {
    super();
    MissingParam.checkMissing(instrument, "instrument");
    MissingParam.checkMissing(type, "type");
    MissingParam.checkMissing(targets, "targets", true);

    this.instrument = instrument;
    this.type = type;
    this.targets = targets;

    for (String target : targets.keySet()) {
      add(new EmptyCalibration(instrument, type, target));
    }
  }

  @Override
  public boolean add(Calibration calibration) {
    if (!calibration.getInstrument().equals(instrument)) {
      throw new CalibrationException("Instrument ID does not match");
    }

    if (!type.equals(calibration.getType())) {
      throw new CalibrationException("Incorrect calibration type");
    }

    if (!targets.containsKey(calibration.getTarget())) {
      throw new CalibrationException("Calibration with target '"
        + calibration.getTarget() + "' is not allowed in this set");
    }

    if (contains(calibration)) {
      super.remove(calibration);
    }

    super.add(calibration);

    return true;
  }

  @Override
  public boolean addAll(Collection<? extends Calibration> c) {
    for (Calibration calibration : c) {
      add(calibration);
    }

    return true;
  }

  /**
   * Determines whether or not a {@code Calibration} for the specified target
   * has been added to the set. The method does not check whether or not the
   * target is in the list of allowed targets.
   *
   * Empty calibrations are not detected by this method.
   *
   * @param target
   *          The target to find
   * @return {@code true} if a calibration for the target is found;
   *         {@code false} otherwise
   */
  public boolean containsTarget(String target) {
    boolean result = false;

    for (Calibration calibration : this) {
      if (!(calibration instanceof EmptyCalibration)
        && calibration.getTarget().equals(target)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determines whether or not a {@code Calibration} for the specified target
   * (as a database ID) has been added to the set. The method does not check
   * whether or not the target is in the list of allowed targets.
   *
   * Empty calibrations are not detected by this method.
   *
   * @param target
   *          The target to find
   * @return {@code true} if a calibration for the target is found;
   *         {@code false} otherwise
   */
  public boolean containsTarget(long target) {
    return containsTarget(String.valueOf(target));
  }

  /**
   * Get the contents of the calibration set as a {@link List}.
   *
   * Required for JSF.
   *
   * @return The calibration set as a {@link List}.
   */
  public List<Calibration> asList() {
    return new ArrayList<Calibration>(this);
  }

  /**
   * Determines whether or not the calibration set contains a
   * {@link Calibration} for all the targets specified for the set.
   *
   * @return {@code true} if a Calibration has been added for each target;
   *         {@code false} otherwise
   * @see #targets
   */
  public boolean isComplete() {
    boolean result = false;

    List<String> addedTargets = new ArrayList<String>();
    for (Calibration calibration : this) {
      if (!(calibration instanceof EmptyCalibration)) {
        addedTargets.add(calibration.getTarget());
      }
    }

    /*
     * Since we can only add calibrations for targets in the original targets
     * list, then by definition the list of added targets will be the same size
     * as the original targets list if, and only if, all targets have been
     * added.
     */
    if (addedTargets.size() == targets.size()) {
      result = true;
    }

    return result;
  }

  /**
   * Get the value of the named calibration. Assumes that there is only one
   * coefficient.
   *
   * @param target
   *          The calibration target
   * @return The calibration value
   * @throws RecordNotFoundException
   *           If the target does not exist
   */
  public double getCalibrationValue(String target, String sensorName)
    throws RecordNotFoundException {
    double result = 0;
    boolean calibrationFound = false;

    for (Calibration calibration : this) {
      if (calibration.getTarget().equals(target)) {
        calibrationFound = true;
        result = calibration.getCoefficient(sensorName);
      }
    }

    if (!calibrationFound) {
      throw new RecordNotFoundException(
        "Calibration '" + target + "' not found in calibration set");
    }
    return result;
  }

  public Calibration getTargetCalibration(String target) {
    for (Calibration calibration : this) {
      if (calibration.getTarget().equals(target)) {
        return calibration;
      }
    }
    return null;
  }

  /**
   * Check that all calibrations in this set are valid
   *
   * @return
   */
  public boolean isValid() {
    boolean valid = true;
    for (Calibration calibration : this) {
      valid = valid && calibration.isValid();
    }
    return valid;
  }

  /**
   * Get the names of the targets that can be stored in this calibration set
   *
   * @return
   */
  public Map<String, String> getTargets() {
    return targets;
  }

  /**
   * Get the closest standards to a specified value for the specified
   * {@link SensorType}.
   *
   * <p>
   * Attempts to return the three closest standards. If some standards are an
   * equal distance from the value, exactly which three are returned is
   * undefined (and it shouldn't matter for calibration calculations). If the
   * CalibrationSet contains fewer than three standards then all will be
   * returned.
   * </p>
   *
   * @param value
   *          The value.
   * @return The closest standards to the value.
   */
  public Map<String, Double> getClosestStandards(SensorType sensorType,
    Double value) {

    // Build the list of standards ordered according to their offset from the
    // specified value
    TreeMap<Double, String> differenceOrderedStandards = new TreeMap<Double, String>();

    for (String target : targets.keySet()) {
      try {
        Double calibrationValue = getCalibrationValue(target,
          sensorType.getShortName());

        differenceOrderedStandards.put(Math.abs(calibrationValue - value),
          target);

      } catch (RecordNotFoundException e) {
        // Do nothing - if we can't find the standard, then it can't be used to
        // calibrate.
      }
    }

    // Now we get the three first standards and return them
    HashMap<String, Double> result = new HashMap<String, Double>();

    for (String target : differenceOrderedStandards.values()) {
      try {
        result.put(target,
          getCalibrationValue(target, sensorType.getShortName()));
        if (result.size() >= 3) {
          break;
        }
      } catch (RecordNotFoundException e) {
        // Do nothing - this exception can't be thrown at this stage.
      }
    }

    return result;
  }
}
