package uk.ac.exeter.QuinCe.data.Dataset;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Maintains a list of messages for a dataset generated during processing and
 * data reduction.
 * 
 * <p>
 * Messages are grouped into modules (identified by a name), typically to
 * identify different jobs in the processing chain. Modules are maintained in
 * insertion order, as are the messages added for each module.
 * </p>
 * 
 * <p>
 * Although this is based on a standard {@link LinkedHashMap}, adding and
 * removing things manually may result in unpredictable behaviour in the
 * application. Please use only the methods provided here.
 * </p>
 * 
 * @author steve
 *
 */
@SuppressWarnings("serial")
public class DatasetProcessingMessages
  extends LinkedHashMap<String, List<String>> {

  private static Gson gson;

  private static final Type MAP_TYPE = new TypeToken<Map<String, List<String>>>() {
  }.getType();

  /**
   * Add a message for a specified processing module.
   * 
   * @param module
   *          The module.
   * @param message
   *          The message.
   */
  public void addMessage(String module, String message) {
    if (!containsKey(module)) {
      put(module, new ArrayList<String>());
    }

    get(module).add(message);
  }

  /**
   * Clear all the messages for a given module.
   * 
   * <p>
   * The module is not removed from the object, so new messages can be added and
   * it maintains its position in the sequence of modules.
   * </p>
   * 
   * @param module
   */
  public void clearModule(String module) {
    put(module, new ArrayList<String>());
  }

  @Override
  public List<String> get(Object key) {
    // We always return a list, even if it's empty.
    if (!containsKey(key)) {
      return new ArrayList<String>();
    } else {
      return super.get(key);
    }
  }

  /**
   * Get the messages as a formatted string for display.
   * 
   * @return The display string.
   */
  public String getDisplayString() {
    StringBuilder result = new StringBuilder();

    boolean first = true;
    for (Map.Entry<String, List<String>> entry : entrySet()) {
      if (!first) {
        result.append('\n');
      }
      result.append(entry.getKey());
      result.append('\n');
      result.append("-".repeat(entry.getKey().length()));
      result.append('\n');

      entry.getValue().forEach(s -> {
        result.append(s);
        result.append('\n');
      });
      first = false;
    }

    return result.toString();
  }

  private static Gson getGson() {
    if (null == gson) {
      gson = new Gson();
    }

    return gson;
  }

  protected String toJson() {
    return gson.toJson(this);
  }

  protected static DatasetProcessingMessages fromJson(String json) {
    DatasetProcessingMessages result;

    if (null == json) {
      result = new DatasetProcessingMessages();
    } else {
      Map<String, List<String>> messagesMap = getGson().fromJson(json,
        MAP_TYPE);
      result = (DatasetProcessingMessages) messagesMap;
    }

    return result;
  }
}
