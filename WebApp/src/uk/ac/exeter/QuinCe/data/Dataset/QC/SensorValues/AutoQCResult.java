package uk.ac.exeter.QuinCe.data.Dataset.QC.SensorValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineException;
import uk.ac.exeter.QuinCe.data.Dataset.QC.RoutineFlag;
import uk.ac.exeter.QuinCe.utils.StringUtils;

public class AutoQCResult extends ArrayList<RoutineFlag> {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 5112798751950377386L;

  private static Gson GSON = null;

  /**
   * Create an empty AutoQCResult
   */
  public AutoQCResult() {
    super();
  }

  /**
   * Build an AutoQCResult from a JSON string
   *
   * @param json
   *          The JSON string
   * @return The AutoQCResult
   */
  public static AutoQCResult buildFromJson(String json) {
    AutoQCResult result = null;
    if (null == json || json.trim().length() == 0) {
      result = new AutoQCResult();
    } else {
      result = getGson().fromJson(json, AutoQCResult.class);
    }

    return result;
  }

  private static Gson getGson() {
    if (null == GSON) {
      GSON = new Gson();
    }

    return GSON;
  }

  /**
   * Return the overall flag that results from a set of flags from QC routines.
   * This is the most significant flag of the set
   *
   * @param flags
   *          The flags
   * @return The most significant flag
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
   * Generate a JSON representation of this result
   *
   * @return The JSON representation
   */
  public String toJson() {
    String json = null;

    if (size() > 0) {
      json = getGson().toJson(this);
    }

    return json;
  }

  /**
   * Get the short messages for each QC flag in this result
   *
   * @return The messages
   * @throws RoutineException
   *           If a message cannot be retrieved
   */
  public String getAllMessages() throws RoutineException {
    return StringUtils.collectionToDelimited(getAllMessagesSet(), ";");
  }

  public Set<String> getAllMessagesSet() throws RoutineException {
    Set<String> messages = new HashSet<String>();
    for (int i = 0; i < size(); i++) {
      messages.add(get(i).getShortMessage());
    }
    return messages;
  }
}
