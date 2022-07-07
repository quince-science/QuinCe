package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

public abstract class PositionParser {

  protected HemisphereMultiplier hemisphereMultiplier = null;

  protected PositionParser() {
  }

  protected PositionParser(HemisphereMultiplier hemisphereMultiplier) {
    this.hemisphereMultiplier = hemisphereMultiplier;
  }

  public double parsePosition(String value, String hemisphere) throws PositionParseException {

    double numericValue = getNumericValue(value);

    if (null != hemisphereMultiplier) {
      numericValue = hemisphereMultiplier.apply(numericValue, getHemisphere(value, hemisphere));
    }

    return numericValue;
  }

  protected String getHemisphere(String value, String hemisphere) throws PositionParseException {
    return hemisphere;
  }

  protected abstract double getNumericValue(String value) throws PositionParseException;

  protected double calculateDecimalDegrees(int degrees, double minutes) throws PositionParseException {

    if (minutes >= 60D) {
      throw new PositionParseException("Invalid minutes value " + minutes);
    }

    return degrees + (minutes / 60D);
  }
}
