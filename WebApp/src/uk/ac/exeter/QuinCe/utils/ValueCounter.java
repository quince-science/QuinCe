package uk.ac.exeter.QuinCe.utils;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

public class ValueCounter {

  private TreeMap<Object, MutableInt> values;

  public ValueCounter() {
    values = new TreeMap<Object, MutableInt>();
  }

  public void add(Object value) {

    boolean proceed = value != null;

    if (value instanceof String && StringUtils.isBlank((String) value)) {
      proceed = false;
    }

    if (proceed) {
      if (!values.containsKey(value)) {
        values.put(value, new MutableInt(0));
      }

      values.get(value).increment();
    }
  }

  public void addAll(Collection<? extends Object> values) {
    values.forEach(this::add);
  }

  public String toString(boolean countOnes) {

    StringBuilder string = new StringBuilder();

    for (Map.Entry<Object, MutableInt> entry : values.entrySet()) {
      string.append(entry.getKey());
      if (countOnes || entry.getValue().toInteger() > 1) {
        string.append(" (");
        string.append(entry.getValue());
        string.append(')');
      }
      string.append('\n');
    }

    return string.toString().trim();
  }

  @Override
  public String toString() {
    return toString(false);
  }
}
