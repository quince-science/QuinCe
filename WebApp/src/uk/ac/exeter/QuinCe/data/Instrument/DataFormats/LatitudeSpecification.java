package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.List;

/**
 * Specifies the latitude format for a data file
 *
 * @author Steve Jones
 *
 */
public class LatitudeSpecification extends PositionSpecification {

  /**
   * Indicates that latitudes are between -90 and 90
   */
  public static final int FORMAT_MINUS90_90 = 0;

  /**
   * Indicates that longitudes are between 0 and 90, with a separate column
   * specifying the hemisphere
   */
  public static final int FORMAT_0_90 = 1;

  /**
   * Indicates a format of degress and decimal minutes
   */
  public static final int FORMAT_HEM_DEG_DEC_MIN = 2;

  /**
   * Basic constructor
   */
  public LatitudeSpecification() {
    super();
  }

  /**
   * Constructor for a complete specification
   *
   * @param format
   *          The format
   * @param valueColumn
   *          The value column
   * @param hemisphereColumn
   *          The hemisphere column
   * @throws PositionException
   *           If the specification is incomplete or invalid
   */
  public LatitudeSpecification(int format, int valueColumn,
    int hemisphereColumn) throws PositionException {
    super(format, valueColumn, hemisphereColumn);
  }

  @Override
  public boolean formatValid(int format) {
    return (format >= 0 && format <= 2);
  }

  @Override
  public boolean hemisphereRequired() {
    return (getFormat() == FORMAT_0_90);
  }

  @Override
  public String getValue(List<String> line) throws PositionException {

    String result = line.get(getValueColumn()).trim();

    if (result.length() == 0) {
      result = null;
    } else {
      try {
        double doubleValue = 0D;

        switch (format) {
        case FORMAT_MINUS90_90: {
          // No need to do anything!
          break;
        }
        case FORMAT_0_90: {
          doubleValue = Double.parseDouble(result);
          String hemisphere = line.get(getHemisphereColumn());
          doubleValue = doubleValue * hemisphereMultiplier(hemisphere);
          break;
        }
        case FORMAT_HEM_DEG_DEC_MIN: {
          // Split on whitespace
          String[] split = result.split("\\s+");

          if (split.length != 3) {
            throw new NumberFormatException();
          }

          String hemisphere = split[0];
          int degrees = Integer.parseInt(split[1]);
          double minutes = Double.parseDouble(split[2]);

          doubleValue = degrees + (minutes / 60);

          if (hemisphere.equals("S")) {
            doubleValue = doubleValue * -1;
          } else if (!hemisphere.equals("N")) {
            throw new PositionException(
              "Invalid hemisphere value '" + hemisphere + "'");
          }

          break;
        }
        default: {
          throw new InvalidPositionFormatException(format);
        }
        }

        result = String.valueOf(doubleValue);

      } catch (NumberFormatException e) {
        System.out.println(
          "NumberFormatException: Invalid latitude value '" + result + "'");
      }
    }

    return result;
  }

  /**
   * Calculate the longitude multiplier for a longitude value. East = 1, West =
   * -1
   *
   * @param hemisphere
   *          The hemisphere
   * @return The multiplier
   * @throws PositionException
   *           If the hemisphere value is invalid
   */
  private double hemisphereMultiplier(String hemisphere)
    throws PositionException {
    double multiplier = 1.0;

    if (null == hemisphere) {
      throw new PositionException("Missing hemisphere value");
    }

    switch (hemisphere.toLowerCase()) {
    case "n":
    case "north": {
      multiplier = 1.0;
      break;
    }
    case "s":
    case "south": {
      multiplier = -1.0;
      break;
    }
    default: {
      throw new PositionException(
        "Invalid hemisphere value '" + hemisphere + "'");
    }
    }

    return multiplier;
  }
}
