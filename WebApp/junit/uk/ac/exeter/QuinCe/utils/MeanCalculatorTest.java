package uk.ac.exeter.QuinCe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MeanCalculatorTest {

  @Test
  public void countTest() {
    MeanCalculator mean = new MeanCalculator();
    mean.add(Double.valueOf(5));
    mean.add(Double.valueOf(4));
    assertEquals(2, mean.getCount());
  }

  @Test
  public void meanTest() {
    MeanCalculator mean = new MeanCalculator();
    mean.add(Double.valueOf(5));
    mean.add(Double.valueOf(4));
    assertEquals(4.5, mean.mean(), 0.001D);
  }

  @Test
  public void valueConstructorTest() {
    List<Double> values = Arrays.asList(new Double[] { 3D, 5D, 7D });
    MeanCalculator mean = new MeanCalculator(values);
    assertEquals(3, mean.getCount());
    assertEquals(5D, mean.mean(), 0.001D);
  }

  @Test
  public void emptyTest() {
    MeanCalculator mean = new MeanCalculator();
    assertEquals(Double.NaN, mean.mean());
  }

  @Test
  public void addNaNTest() {
    MeanCalculator mean = new MeanCalculator();
    mean.add(5D);
    mean.add(Double.NaN);
    assertEquals(1, mean.getCount());
  }

  @Test
  public void addLongTest() {
    MeanCalculator mean = new MeanCalculator();
    mean.add(5D);
    mean.add(10L);
    assertEquals(2, mean.getCount());
    assertEquals(7.5D, mean.mean(), 0.001D);

  }
}
