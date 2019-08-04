package uk.ac.exeter.QuinCe.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Special list of strings that ignores all empty strings. Also trims
 * all supplied strings
 * @author Steve Jones
 *
 */
public class NoEmptyStringList extends ArrayList<String> {

  public NoEmptyStringList() {
    super();
  }

  public NoEmptyStringList(Collection<? extends String> c) {
    super(filterCollection(c));
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
  public void add(int index, String e) {
    if (isValid(e)) {
      super.add(index, e.trim());
    }
  }

  @Override
  public boolean addAll(Collection<? extends String> c) {
    return super.addAll(filterCollection(c));
  }

  @Override
  public boolean addAll(int index, Collection<? extends String> c) {
    boolean result = false;

    List<String> toAdd = filterCollection(c);
    if (toAdd.size() > 0) {
      result = super.addAll(index, toAdd);
    }

    return result;
  }

  /**
   * Determine whether or not a string is valid (i.e. not empty)
   * @param e The string
   * @return {@code true} if the string is valid; {@code false} if it is empty
   */
  private static boolean isValid(String e) {
    return (null != e && e.trim().length() > 0);
  }

  /**
   * Remove all empty strings from a collection, and trim the remaining values
   * @param c The collection
   * @return The filtered and trimmed collection
   */
  private static List<String> filterCollection(Collection<? extends String> c) {
    return c
      .stream()
      .filter(NoEmptyStringList::isValid)
      .map(String::trim)
      .collect(Collectors.toList());
  }
}
