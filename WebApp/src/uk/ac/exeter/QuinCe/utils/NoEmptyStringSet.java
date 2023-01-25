package uk.ac.exeter.QuinCe.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Special list of strings that ignores all empty strings. Also trims all
 * supplied strings
 *
 * @author Steve Jones
 *
 */
@SuppressWarnings("serial")
public class NoEmptyStringSet extends TreeSet<String> {

  public NoEmptyStringSet() {
    super();
  }

  public NoEmptyStringSet(Collection<? extends String> c) {
    super(filterCollection(c));
  }

  public NoEmptyStringSet(String s) {
    super();
    if (!StringUtils.isBlank(s)) {
      super.add(s);
    }
  }

  @Override
  public boolean add(String e) {
    boolean result = false;

    if (isValid(e)) {
      result = super.add(e.trim());
    }

    return result;
  }

  @Override
  public boolean addAll(Collection<? extends String> c) {
    return super.addAll(filterCollection(c));
  }

  /**
   * Determine whether or not a string is valid (i.e. not empty)
   *
   * @param e
   *          The string
   * @return {@code true} if the string is valid; {@code false} if it is empty
   */
  private static boolean isValid(String e) {
    return (null != e && e.trim().length() > 0);
  }

  /**
   * Remove all empty strings from a collection, and trim the remaining values
   *
   * @param c
   *          The collection
   * @return The filtered and trimmed collection
   */
  private static List<String> filterCollection(Collection<? extends String> c) {
    if (null == c) {
      return new ArrayList<String>();
    } else {
      return c.stream().filter(NoEmptyStringSet::isValid).map(String::trim)
        .collect(Collectors.toList());
    }
  }

  @Override
  public String toString() {
    return StringUtils.collectionToDelimited(this, ";");
  }
}
