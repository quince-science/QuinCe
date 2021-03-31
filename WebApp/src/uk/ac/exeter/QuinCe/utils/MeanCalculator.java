package uk.ac.exeter.QuinCe.utils;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

public class MeanCalculator {

  private MutableDouble sum = new MutableDouble(0.0D);

  private MutableInt count = new MutableInt(0);

  public MeanCalculator() {

  }

  public void add(Double value) {
    if (null != value && !value.isNaN()) {
      sum.add(value);
      count.increment();
    }
  }

  public int getCount() {
    return count.intValue();
  }

  public Double mean() {

    Double mean = Double.NaN;
    if (count.intValue() > 0) {
      mean = sum.doubleValue() / count.intValue();
    }

    return mean;
  }

  @Override
  public String toString() {
    return sum + " / " + count + " = " + mean();
  }
}
