package uk.ac.exeter.QuinCe.data.Dataset;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import uk.ac.exeter.QuinCe.utils.StringUtils;

@SuppressWarnings("serial")
public class DatasetUserMessages extends TreeSet<String> {

  public static DatasetUserMessages fromString(String messages) {
    DatasetUserMessages result = new DatasetUserMessages();

    if (null != messages) {
      result.addAll(Arrays.asList(messages.split("[\n\r;]")));
    }

    return result;
  }

  @Override
  public boolean add(String message) {
    boolean result;

    String preparedMessage = prepareMessage(message);
    if (null == preparedMessage) {
      result = false;
    } else {
      result = super.add(preparedMessage);
    }

    return result;
  }

  @Override
  public boolean addAll(Collection<? extends String> messages) {
    List<String> preparedStrings = messages.stream().map(m -> prepareMessage(m))
      .filter(m -> null != m).toList();
    return super.addAll(preparedStrings);
  }

  public String getDisplayString() {
    return StringUtils.collectionToDelimited(this, "\n");
  }

  private String prepareMessage(String message) {
    if (null == message) {
      return null;
    } else {
      String trimmed = message.trim();

      if (trimmed.length() == 0) {
        return null;
      } else {
        return message.trim();
      }
    }
  }

  protected String getStorageString() {
    return StringUtils.collectionToDelimited(this, ";");
  }
}
