package uk.ac.exeter.QuinCe.utils;

import java.util.Comparator;

/**
 * Compares {@link String}s by ascending order of length.
 *
 * <p>
 * {@code null} strings are considered to be shorter than zero-length strings.
 * Strings of equal length are not sorted further.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class AscendingLengthComparator implements Comparator<String> {

  @Override
  public int compare(String o1, String o2) {
    int result;

    if (null == o1 && null == o2) {
      result = 0;
    } else if (null == o1) {
      result = -1;
    } else if (null == o2) {
      result = 1;
    } else {
      result = o1.length() - o2.length();
    }

    return result;
  }
}
