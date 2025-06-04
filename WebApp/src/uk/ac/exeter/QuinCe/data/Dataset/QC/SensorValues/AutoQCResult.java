package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Routine;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.jobs.files.AutoQCJob;
import uk.ac.exeter.QuinCe.utils.StringUtils;

/**
 * Represents the result of the {@link AutoQCJob} run on a particular
 * {@link SensorValue}.
 *
 * <p>
 * An {@code AutoQCResult} consists of multiple {@link RoutineFlag} objects,
 * representing the results of all the {@link AutoQCRoutine}s run on the
 * {@link SensorValue}. The automatic QC result will be the 'worst'
 * {@link RoutineFlag} (ranked according to
 * {@link Flag#moreSignificantThan(Flag)}), and the QC message will be the
 * combination of messages from all the contributing {@link RoutineFlag}s.
 * </p>
 */
@SuppressWarnings("serial")
public class AutoQCResult extends HashSet<RoutineFlag> {

  /**
   * GSON (de)serializer.
   */
  private static Gson GSON = new Gson();

  /**
   * Create an empty AutoQCResult.
   */
  public AutoQCResult() {
    super();
  }

  /**
   * Build an AutoQCResult from a JSON string, as stored in the database.
   *
   * @param json
   *          The JSON string.
   * @return The AutoQCResult.
   */
  public static AutoQCResult buildFromJson(String json) {
    AutoQCResult result = null;
    if (null == json || json.trim().length() == 0) {
      result = new AutoQCResult();
    } else {
      result = GSON.fromJson(json, AutoQCResult.class);
    }

    return result;
  }

  /**
   * Return the overall flag that results from a set of flags from QC routines.
   *
   * <p>
   * This is the most significant flag of the set, according to the order used
   * by {@link Flag#moreSignificantThan(Flag)}.
   * </p>
   *
   * @return The most significant flag.
   */
  public Flag getOverallFlag() {
    Flag result = Flag.GOOD;

    for (RoutineFlag flag : this) {
      if (flag.moreSignificantThan(result)) {
        result = flag;
      }
    }

    return result;
  }

  /**
   * Generate a JSON representation of this result.
   *
   * @return The JSON representation.
   */
  public String toJson() {
    String json = null;

    if (size() > 0) {
      json = GSON.toJson(this);
    }

    return json;
  }

  /**
   * Get the short messages for each QC flag in this result as a delimited
   * {@link String}.
   *
   * @return The messages.
   * @throws RoutineException
   *           If a message cannot be retrieved.
   */
  public String getAllMessages() throws RoutineException {
    return StringUtils.collectionToDelimited(getAllMessagesSet(), ";");
  }

  /**
   * Get the short messages for each QC flag in this result as a {@link Set}.
   *
   * @return The messages.
   * @throws RoutineException
   *           If a message cannot be retrieved.
   */
  public Set<String> getAllMessagesSet() throws RoutineException {

    Set<String> messages = new HashSet<String>();

    Iterator<RoutineFlag> iterator = iterator();
    while (iterator.hasNext()) {
      messages.add(iterator.next().getShortMessage());
    }

    return messages;
  }

  /**
   * Add a new {@link RoutineFlag} to this result.
   *
   * <p>
   * If an entry for the flag's source {@link AutoQCRoutine} already exists, it
   * is replaced.
   * </p>
   */
  @Override
  public boolean add(RoutineFlag flag) {
    removeIf(i -> i.getRoutineName().equals(flag.getRoutineName()));
    return super.add(flag);
  }

  /**
   * Remove any {@link RoutineFlag}s that correspond to the specified routine.
   *
   * @param routine
   *          The routine whose flags are to be removed.
   * @return {@code true} if a flag was removed; {@code false} otherwise.
   */
  public boolean remove(Routine routine) {
    return removeIf(i -> i.getRoutineName().equals(routine.getName()));
  }
}
