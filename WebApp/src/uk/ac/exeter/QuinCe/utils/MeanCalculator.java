package uk.ac.exeter.QuinCe.utils;

import java.util.Collection;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Class to collect numbers and calculate their mean.
 */
public class MeanCalculator {

  /**
   * The total of the added values.
   */
  private MutableDouble sum = new MutableDouble(0.0D);

  /**
   * The number of values added.
   */
  private MutableInt count = new MutableInt(0);

  /**
   * Empty constructor for simple initialisation.
   */
  public MeanCalculator() {
    // NOOP
  }

  /**
   * Create a calculator and add the specified values.
   *
   * @param values
   *          The values to be added.
   */
  public MeanCalculator(Collection<Double> values) {
    values.forEach(this::add);
  }

  /**
   * Add a value to the calculator.
   *
   * <p>
   * {@code null} and {@code NaN} values are ignored.
   * </p>
   *
   * @param value
   *          The value to add.
   */
  public void add(Double value) {
    if (null != value && !value.isNaN()) {
      sum.add(value);
      count.increment();
    }
  }

  /**
   * Add a value to the calculator.
   *
   * <p>
   * {@code null} values are ignored.
   * </p>
   *
   * @param value
   *          The value to add.
   */
  public void add(Long value) {
    if (null != value) {
      sum.add(value);
      count.increment();
    }
  }

  /**
   * Get the number of values added to the calculator.
   *
   * @return The value count.
   */
  public int getCount() {
    return count.intValue();
  }

  /**
   * Get the mean of the added values.
   *
   * @return The mean, or {@link Double#NaN} if no values have been added.
   */
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
