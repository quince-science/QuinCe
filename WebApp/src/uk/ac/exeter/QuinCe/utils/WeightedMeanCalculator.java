package uk.ac.exeter.QuinCe.utils;

import org.apache.commons.lang3.mutable.MutableDouble;

public class WeightedMeanCalculator {

  private MutableDouble weightedSum;

  private MutableDouble sumOfWeights;

  public WeightedMeanCalculator() {
    weightedSum = new MutableDouble(0D);
    sumOfWeights = new MutableDouble(0D);
  }

  public void add(Double value, Double weight) {
    weightedSum.add(value * weight);
    sumOfWeights.add(weight);
  }

  public Double getWeightedMean() {

    Double result = Double.NaN;
    if (sumOfWeights.doubleValue() > 0D) {
      result = weightedSum.doubleValue() / sumOfWeights.doubleValue();
    }

    return result;
  }

  public Double getSumOfWeights() {
    return sumOfWeights.getValue();
  }
}
