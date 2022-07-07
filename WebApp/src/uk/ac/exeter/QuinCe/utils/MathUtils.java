package uk.ac.exeter.QuinCe.utils;

import java.util.HashMap;
import java.util.Map;

public class MathUtils {

  public static double round(double number, int decimal) {
    double toRound = number * Math.pow(10, decimal);
    double rounded = Math.round(toRound);
    return rounded / Math.pow(10, decimal);
  }

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

    /*
     * System.out.println(map); return map.entrySet().stream() .collect(
     * Collectors.toMap( Map.Entry::getKey, e -> Double.isNaN(e.getValue()) ?
     * null : e.getValue() ) );
     */
  }

  public static Map<String, Double> nullToNan(Map<String, Double> map) {

    Map<String, Double> out = new HashMap<String, Double>();
    for (Map.Entry<String, Double> entry : map.entrySet()) {
      if (null == entry.getValue()) {
        out.put(entry.getKey(), Double.NaN);
      } else {
        out.put(entry.getKey(), entry.getValue());
      }
    }

    return out;
  }

  public static Double nullableParseDouble(String value) {
    Double result = null;

    if (null != value && value.trim().length() > 0) {
      result = Double.parseDouble(value);
    }

    return result;
  }

  public static boolean checkRange(double value, double min, double max) {
    return value >= min && value <= max;
  }
}
