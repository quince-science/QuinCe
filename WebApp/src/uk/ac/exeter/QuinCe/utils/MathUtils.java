package uk.ac.exeter.QuinCe.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Some useful utilities for dealing with numbers.
 */
public class MathUtils {

  /**
   * Take a {@link Map} of {@code String -> Double} and convert any NaN or
   * Infinite values to {@code null}.
   *
   * <p>
   * This returns a copy of the Map, leaving the original untouched.
   *
   * @param map
   *          The input Map.
   * @return The converted Map.
   */
  public static Map<String, Double> nanToNull(Map<String, Double> map) {
    Map<String, Double> out = new HashMap<String, Double>();
    for (Map.Entry<String, Double> entry : map.entrySet()) {
      if (null != entry.getValue() && (Double.isNaN(entry.getValue())
        || Double.isInfinite(entry.getValue()))) {
        out.put(entry.getKey(), null);
      } else {
        out.put(entry.getKey(), entry.getValue());
      }
    }

    return out;
  }

  /**
   * An extended version of {@link Double#parseDouble(String)} that handles
   * {@code null} and empty input strings.
   *
   * <p>
   * If the input is {@code null}, or a trimmed version of the input is an empty
   * string, the method returns {@code null}. Any unparseable value will throw a
   * {@link NumberFormatException} as {@link Double#parseDouble(String)} would.
   * </p>
   *
   * @param value
   *          The string value.
   * @return The parsed Double value.
   */
  public static Double nullableParseDouble(String value) {
    Double result = null;

    if (null != value && value.trim().length() > 0) {
      result = Double.parseDouble(value);
    }

    return result;
  }

  /**
   * Determine whether a given value is within a specified range (inclusive).
   *
   * @param value
   *          The value to check.
   * @param min
   *          The range minimum.
   * @param max
   *          The range maximum.
   * @return {@code true} if the value is within the range; {@code false} if it
   *         is not.
   */
  public static boolean checkRange(double value, double min, double max) {
    return value >= min && value <= max;
  }

  /**
   * Determines whether or not a {@link Double} value is {@code null} or
   * {@link Double#NaN}.
   *
   * @param d
   *          The value to check.
   * @return {@code true} if the value is {@code null} or {@code NaN};
   *         {@code false} otherwise.
   */
  public static boolean isEmpty(Double d) {
    return null == d || d.isNaN();
  }
}
