package uk.ac.exeter.QuinCe.utils;

import java.util.Map;
import java.util.TreeMap;

public class ModeCalculator {

  private TreeMap<Long, Long> values;

  public ModeCalculator() {
    values = new TreeMap<Long, Long>();
  }

  public void add(Long value) {
    if (!values.containsKey(value)) {
      values.put(value, 1L);
    } else {
      values.put(value, values.get(value) + 1);
    }
  }

  public Long getMode() {
    Long mode = null;
    Long maxCount = Long.MIN_VALUE;

    for (Map.Entry<Long, Long> entry : values.entrySet()) {
      if (entry.getValue() > maxCount) {
        mode = entry.getKey();
        maxCount = entry.getValue();
      }
    }

    return null == mode ? 0L : mode;
  }
}
