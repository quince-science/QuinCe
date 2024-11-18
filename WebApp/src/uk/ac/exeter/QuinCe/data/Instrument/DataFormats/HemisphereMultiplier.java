package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.Arrays;
import java.util.List;

public class HemisphereMultiplier {

  private static List<String> ONE_VALUES;

  private static List<String> MINUS_ONE_VALUES;

  static {
    ONE_VALUES = Arrays.asList("N", "n", "E", "e");
    MINUS_ONE_VALUES = Arrays.asList("S", "s", "W", "w");
  }

  public static double apply(double value, String hemisphere)
    throws InvalidHemisphereException {

    double multiplier;

    if (ONE_VALUES.contains(hemisphere)) {
      multiplier = 1D;
    } else if (MINUS_ONE_VALUES.contains(hemisphere)) {
      multiplier = -1D;
    } else {
      throw new InvalidHemisphereException(hemisphere);
    }

    return value * multiplier;
  }
}
