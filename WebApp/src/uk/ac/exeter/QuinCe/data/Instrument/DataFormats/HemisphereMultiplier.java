package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.Collection;

import uk.ac.exeter.QuinCe.utils.MissingParam;

public class HemisphereMultiplier {

  private Collection<String> oneValues;

  private Collection<String> minusOneValues;

  public HemisphereMultiplier(Collection<String> oneValues, Collection<String> minusOneValues) {

    MissingParam.checkMissing(oneValues, "oneValues", false);
    MissingParam.checkMissing(minusOneValues, "minusOneValues", false);

    this.oneValues = oneValues.stream().map(v -> v.toLowerCase()).toList();
    this.minusOneValues = minusOneValues.stream().map(v -> v.toLowerCase()).toList();
  }

  public double apply(double value, String hemisphere) throws InvalidHemisphereException {

    double multiplier;

    if (oneValues.contains(hemisphere.toLowerCase())) {
      multiplier = 1D;
    } else if (minusOneValues.contains(hemisphere.toLowerCase())) {
      multiplier = -1D;
    } else {
      throw new InvalidHemisphereException(hemisphere);
    }

    return value * multiplier;
  }
}
