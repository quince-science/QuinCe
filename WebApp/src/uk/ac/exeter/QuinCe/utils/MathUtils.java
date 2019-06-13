package uk.ac.exeter.QuinCe.utils;

public class MathUtils {

  public static double round(double number, int decimal) {
    double toRound = number * Math.pow(10, decimal);
    double rounded = Math.round(toRound);
    return rounded / Math.pow(10, decimal);
  }

}
