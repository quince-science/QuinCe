package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import uk.ac.exeter.QuinCe.data.Files.DataFile;

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
   * Add a message pertaining to an error raised on specific line in a data
   * file.
   *
   * @param module
   *          The module.
   * @param file
   *          The file where the message was triggered.
   * @param line
   *          The line for which the message was triggered.
   * @param e
   *          The error.
   */
  public void addMessage(String module, DataFile file, int line, Throwable e) {
    addMessage(module, makeFileMessage(file, line, e.getMessage()));
  }

  /**
   * Add a message pertaining to an error raised on specific line in a data
   * file.
   *
   * @param module
   *          The module.
   * @param file
   *          The file where the message was triggered.
   * @param line
   *          The line for which the message was triggered.
   * @param e
   *          The error.
   */
  public void addMessage(String module, DataFile file, int line,
    String message) {
    addMessage(module, makeFileMessage(file, line, message));
  }

  private String makeFileMessage(DataFile file, int line, String messageText) {
    StringBuilder message = new StringBuilder();
    message.append(file.getFilename());
    message.append(':');
    message.append(line);
    message.append(' ');
    message.append(messageText);
    return message.toString();
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
      gson = new GsonBuilder()
        .registerTypeAdapter(DatasetProcessingMessages.class,
          new DatasetProcessingMessagesSerializer())
        .create();
    }

    return gson;
  }

  protected String toJson() {
    return getGson().toJson(this);
  }

  protected static DatasetProcessingMessages fromJson(String json) {
    DatasetProcessingMessages result;

    if (null == json) {
      result = new DatasetProcessingMessages();
    } else {
      result = getGson().fromJson(json, DatasetProcessingMessages.class);
    }

    return result;
  }
}
