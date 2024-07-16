package uk.ac.exeter.QuinCe.data.Instrument.Calibration;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.ac.exeter.QuinCe.data.Dataset.DataSet;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.MissingParam;
import uk.ac.exeter.QuinCe.utils.MissingParamException;

/**
 * Represents the set of {@link Calibration}s to be used for a {@link DataSet}.
 *
 * <p>
 * Since it is sometimes possible for the {@link Calibration}s to change in the
 * middle of a {@link DataSet}, or pre- and post-{@link Calibration}s to be
 * required, the object contains complete sets of {@link Calibration}s for one
 * or more times.
 * </p>
 * <p>
 * The {@code CalibrationSet} will always contain a set of {@link Calibration}s
 * immediately after the {@link #end} time, if it is available. Whether or not
 * this is required should be determined by the code using this class.
 * </p>
 * <p>
 * <b>NB:</b>This class assumes that any {@link Calibration}s provided to it are
 * all for the desired instrument and type; they are not checked at any stage,
 * and it is the caller's responsibility to make sure of this.
 * </p>
 */
public class CalibrationSet {

  /**
   * The set of targets that can be contained in this set in sorted order.
   */
  private final TreeSet<String> targets;

  /**
   * The start time of the period to be covered by this CalibrationSet.
   */
  private final LocalDateTime start;

  /**
   * The end time of the period to be covered by this CalibrationSet.
   */
  private final LocalDateTime end;

  /**
   * Indicates whether or not the time of a {@link Calibration} influences the
   * effect it has.
   */
  private final boolean timeAffectsCalibration;

  /**
   * Indicates whether [@link Calibration}s are allowed between the
   * {@link #start} and {@link #end}.
   */
  private final boolean allowInterim;

  /**
   * Indicates whether this {@code CalibrationSet} should include
   * post-calibrations after the end of the specified period.
   */
  private final boolean includePost;

  /**
   * The {@link Calibration}s that make up this CalibrationSet.
   */
  private TreeMap<LocalDateTime, TreeMap<String, Calibration>> members = new TreeMap<LocalDateTime, TreeMap<String, Calibration>>();

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
   * @throws InvalidCalibrationDateException
   *           If {@link #allowInterim} is {@code false} and any
   *           {@link Calibration} is between the {@link #start} and
   *           {@link #end} times.
   */
  public CalibrationSet(Map<String, String> targets, LocalDateTime start,
    LocalDateTime end, CalibrationDB dbInstance,
    TreeMap<String, TreeSet<Calibration>> calibrations)
    throws MissingParamException, InvalidCalibrationDateException {
    super();
    MissingParam.checkMissing(targets, "targets", true);
    MissingParam.checkMissing(start, "start");
    MissingParam.checkMissing(end, "end");

    this.targets = new TreeSet<String>(targets.keySet());
    this.start = start;
    this.end = end;
    this.timeAffectsCalibration = dbInstance.timeAffectesCalibration();
    this.allowInterim = dbInstance.allowCalibrationChangeInDataset();
    this.includePost = dbInstance.usePostCalibrations();
    populate(calibrations);
  }

  /**
   * Build the {@code CalibrationSet} from the supplied collection of
   * {@link Calibration}s.
   *
   * @param calibrations
   *          The {@link Calibration}s
   * @throws InvalidCalibrationDateException
   *           If {@link #allowInterim} is {@code false} and any
   *           {@link Calibration} is between the {@link #start} and
   *           {@link #end} times.
   * @see CalibrationDB#getCalibrations(java.sql.Connection, Instrument)
   */
  private void populate(TreeMap<String, TreeSet<Calibration>> calibrations)
    throws InvalidCalibrationDateException {

    // The last calibration before the start time for each target
    TreeMap<String, Calibration> priorCalibrations = new TreeMap<String, Calibration>();

    // The first calibration after the start time for each target
    TreeMap<String, Calibration> postCalibrations = new TreeMap<String, Calibration>();

    // The dates of any calibrations that appear between the start and end time
    // (inclusive)
    Set<LocalDateTime> interimTimes = new TreeSet<LocalDateTime>();

    for (String target : targets) {
      TreeSet<Calibration> targetCalibrations = calibrations.get(target);
      if (null != targetCalibrations) {
        for (Calibration calibration : calibrations.get(target)) {
          if (calibration.getDeploymentDate().isBefore(start)) {
            priorCalibrations.put(target, calibration);
          } else if (calibration.getDeploymentDate().isAfter(end)) {
            postCalibrations.put(target, calibration);
            break;
            // We don't need to check any more calibrations for this target
          } else {
            if (!allowInterim) {
              throw new InvalidCalibrationDateException(
                "Cannot set a calibration between start and end times");
            }
            interimTimes.add(calibration.getDeploymentDate());
          }
        }
      }
    }

    // Calculate the prior date (the latest prior calibration date)
    LocalDateTime priorDate = priorCalibrations.values().stream()
      .map(c -> c.getDeploymentDate()).sorted()
      .reduce((first, second) -> second).orElse(null);

    if (null != priorDate) {
      members.put(priorDate, priorCalibrations);
    }

    // Calculate the post date (the earliest post calibration date)
    LocalDateTime postDate = postCalibrations.values().stream()
      .map(c -> c.getDeploymentDate()).sorted().findFirst().orElse(null);

    if (includePost && null != postDate) {
      members.put(postDate, postCalibrations);
    }

    // For each of the interim dates, find the latest Calibration before or at
    // that time for each target
    for (LocalDateTime time : interimTimes) {
      TreeMap<String, Calibration> interimCalibrations = new TreeMap<String, Calibration>();
      for (String target : targets) {
        TreeSet<Calibration> targetCalibrations = calibrations.get(target);

        Calibration interimCalibration = targetCalibrations.stream()
          .filter(
            c -> DateTimeUtils.isEqualOrBefore(c.getDeploymentDate(), time))
          .findFirst().orElse(null);

        interimCalibrations.put(target, interimCalibration);
      }

      members.put(time, interimCalibrations);
    }
  }

  /**
   * Determines whether or not there is a complete set of {@link Calibration}s
   * before the {@link #start} time.
   *
   * @return {@code true} if there is a complete set of {@link Calibration}s
   *         before the {@link #start} time; {@code false} otherwise.
   */
  public boolean hasCompletePrior() {
    boolean result = false;

    if (members.size() > 0) {
      LocalDateTime firstTime = members.firstKey();
      if (firstTime.isBefore(start)) {
        TreeMap<String, Calibration> priors = members.get(firstTime);
        if (isComplete(priors)) {
          result = true;
        }
      }
    }

    return result;
  }

  /**
   * Determine whether or not there is a complete set of {@link Calibration}s
   * after the {@link #end} time.
   *
   * @return {@code true} if there is a complete set of {@link Calibration}s
   *         after the {@link #end} time; {@code false} otherwise.
   */
  public boolean hasCompletePost() {
    boolean result = false;

    if (members.size() > 0) {
      LocalDateTime lastTime = members.lastKey();
      if (lastTime.isAfter(end)) {
        TreeMap<String, Calibration> posts = members.get(lastTime);
        if (isComplete(posts)) {
          result = true;
        }
      }
    }

    return result;
  }

  /**
   * Get the set of {@link Calibration}s for a given time.
   *
   * <p>
   * This is the complete set of {@link Calibration}s whose
   * {@link Calibration#getDeploymentDate()} is on or immediately before the
   * specified time.
   * </p>
   * <p>
   * If there are no {@link Calibration}s at the specified time, an empty map is
   * returned.
   * </p>
   *
   * @param time
   *          The time.
   * @return The matching {@link Calibration}s.
   */
  public TreeMap<String, Calibration> getCalibrations(
    LocalDateTime targetTime) {

    LocalDateTime actualTime = members.lowerKey(targetTime);
    return null != actualTime ? members.get(actualTime)
      : new TreeMap<String, Calibration>();
  }

  /**
   * Get the first set of {@link Calibration}s after a given time.
   *
   * <p>
   * This is the complete set of {@link Calibration}s whose
   * {@link Calibration#getDeploymentDate()} is immediately after the specified
   * time.
   * </p>
   * <p>
   * If there are no calibrations after the specified time, an empty map is
   * returned.
   * </p>
   *
   * @param time
   *          The time.
   * @return The matching {@link Calibration}s.
   */
  public TreeMap<String, Calibration> getPostCalibrations(
    LocalDateTime targetTime) {

    LocalDateTime actualTime = members.higherKey(targetTime);
    return null != actualTime ? members.get(actualTime)
      : new TreeMap<String, Calibration>();

  }

  /**
   * Determines whether or not a {@link Map} of {@link Calibration}s contains an
   * entry for each target.
   *
   * @param calibrations
   *          The {@link Map} to check
   * @return {@code true} if there is an entry in the {@link Map} for each
   *         target; {@code false} otherwise.
   * @see #targets
   */
  public boolean isComplete(Map<String, Calibration> calibrations) {
    return CollectionUtils.isEqualCollection(calibrations.keySet(), targets)
      && !calibrations.values().contains(null);
  }

  /**
   * Render this {@code CalibrationSet} as a GSON JSON object.
   *
   * <p>
   * This is for export purposes only; there is no mechanism for deserialising
   * JSON back to a {@code CalibrationSet} object.
   * </p>
   *
   * @return The JSON String
   */
  public JsonObject toJson(CalibrationTargetNameMapper targetMapper) {

    JsonObject result = new JsonObject();

    for (String target : targets) {

      JsonArray targetEntries = new JsonArray();

      for (Map.Entry<LocalDateTime, TreeMap<String, Calibration>> entry : members
        .entrySet()) {

        Calibration entryCalibration = entry.getValue().get(target);
        if (null != entryCalibration) {
          JsonObject calibrationJson = new JsonObject();

          calibrationJson.addProperty("date",
            DateTimeUtils.toIsoDate(entry.getKey()));

          calibrationJson.addProperty(
            entryCalibration.getCoefficientsLabel().toLowerCase(),
            entryCalibration.getHumanReadableCoefficients());

          targetEntries.add(calibrationJson);
        }
      }

      result.add(targetMapper.map(target), targetEntries);
    }

    return result;
  }

  public boolean isEmpty() {
    return members.size() == 0;
  }

  public TreeSet<String> getTargets() {
    return targets;
  }

  @Override
  public String toString() {

    StringBuilder string = new StringBuilder();

    for (Map.Entry<LocalDateTime, TreeMap<String, Calibration>> entry : members
      .entrySet()) {

      string.append(DateTimeUtils.formatDateTime(entry.getKey()));
      string.append("->");

      for (Map.Entry<String, Calibration> timeEntry : entry.getValue()
        .entrySet()) {

        string.append(timeEntry.getKey());
        string.append('=');
        string.append(timeEntry.getValue().getId());
        string.append(':');
        string.append(timeEntry.getValue().getCoefficientsAsDelimitedList());
        string.append(';');
      }

      string.append(' ');
    }

    return string.toString();
  }

  @Override
  public int hashCode() {
    return members.hashCode();
  }

  @Override
  public boolean equals(Object o) {

    boolean result = true;

    if (!(o instanceof CalibrationSet)) {
      result = false;
    } else {
      CalibrationSet other = (CalibrationSet) o;
      TreeMap<LocalDateTime, TreeMap<String, Calibration>> otherMembers = other.members;

      // Check periods
      if (!start.equals(other.start) || !end.equals(other.end)) {
        result = false;
      } else {
        // Check dates
        if (!CollectionUtils.isEqualCollection(members.keySet(),
          otherMembers.keySet())) {

          result = false;
        } else {

          for (LocalDateTime key : members.keySet()) {
            if (!membersEqual(members.get(key), other.members.get(key))) {
              result = false;
              break;
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Determines whether or not this {@code CalibrationSet} will have the same
   * effect on the specified time period
   *
   * @param other
   * @return
   */
  public boolean hasSameEffect(CalibrationSet other) {

    boolean result = true;

    // Make sure the sets cover the same time period
    if (!start.equals(other.start) || !end.equals(other.end)) {
      throw new IllegalArgumentException(
        "CalibrationSets do not cover the same time period");
    }

    // Check the first time - this is the prior
    LocalDateTime thisPriorTime = members.firstKey();
    LocalDateTime otherPriorTime = other.members.firstKey();
    if (timeAffectsCalibration && !thisPriorTime.equals(otherPriorTime)) {
      result = false;
    } else {
      if (!membersEqual(members.get(thisPriorTime),
        other.members.get(otherPriorTime))) {
        result = false;
      }
    }

    // If the prior was OK, check the interim times
    if (allowInterim && result) {
      TreeSet<LocalDateTime> thisInterimTimes = getInterimTimes();
      TreeSet<LocalDateTime> otherInterimTimes = other.getInterimTimes();

      if (!CollectionUtils.isEqualCollection(thisInterimTimes,
        otherInterimTimes)) {
        result = false;
      } else {
        for (LocalDateTime time : thisInterimTimes) {
          if (!membersEqual(members.get(time), other.members.get(time))) {
            result = false;
            break;
          }
        }
      }
    }

    // Finally check the post calibration if applicable
    if (includePost && result) {

      LocalDateTime thisPostTime = members.lastKey().isAfter(end)
        ? members.lastKey()
        : null;
      LocalDateTime otherPostTime = other.members.lastKey().isAfter(end)
        ? other.members.lastKey()
        : null;

      if (null != thisPostTime && null != otherPostTime) {
        if (timeAffectsCalibration && !thisPostTime.equals(otherPostTime)) {
          result = false;
        } else if (!membersEqual(members.get(thisPostTime),
          other.members.get(otherPostTime))) {
          result = false;
        }
      } else if (null != thisPostTime || null != otherPostTime) {
        result = false;
      }
    }

    return result;
  }

  private boolean membersEqual(TreeMap<String, Calibration> m1,
    TreeMap<String, Calibration> m2) {

    boolean result = true;

    // Check the targets
    if (!CollectionUtils.isEqualCollection(m1.keySet(), m2.keySet())) {
      result = false;
    } else {

      // Check the calibration for each target
      for (String target : m1.keySet()) {
        Calibration calibration1 = m1.get(target);
        Calibration calibration2 = m2.get(target);

        if (!calibration1.hasSameEffect(calibration2)) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  private TreeSet<LocalDateTime> getInterimTimes() {
    return members.keySet().stream()
      .filter(t -> !t.isBefore(start) && !t.isAfter(end))
      .collect(Collectors.toCollection(TreeSet::new));
  }
}
