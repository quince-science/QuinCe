package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public abstract class PositionParser {

  protected PositionParser() {
  }

  public double parsePosition(String value, String hemisphere)
    throws PositionParseException {

    double numericValue = getNumericValue(value);

    if (null != hemisphere && numericValue < 0D) {
      throw new PositionParseException(
        "Cannot have negative position with specified hemisphere");
    }

    if (null != hemisphere) {
      numericValue = HemisphereMultiplier.apply(numericValue,
        getHemisphere(value, hemisphere));
    }

    return numericValue;
  }

  protected String getHemisphere(String value, String hemisphere)
    throws PositionParseException {
    return hemisphere;
  }

  protected abstract double getNumericValue(String value)
    throws PositionParseException;

  protected double calculateDecimalDegrees(int degrees, double minutes)
    throws PositionParseException {

    if (minutes < 0D || minutes >= 60D) {
      throw new PositionParseException("minutes", minutes);
    }

    double result;

    if (degrees >= 0) {
      result = degrees + (minutes / 60D);
    } else {
      result = degrees - (minutes / 60D);
    }

    return result;
  }
}
