package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.List;

/**
 * Handles all formats of longitudes, and the corresponding column assignments
 * within a data file
 *
 * @author Steve Jones
 *
 */
public class LongitudeSpecification extends PositionSpecification {

  /**
   * Indicates that longitudes are between 0 and 360
   */
  public static final int FORMAT_0_360 = 0;

  /**
   * Indicates that longitudes are between -180 and 180
   */
  public static final int FORMAT_MINUS180_180 = 1;

  /**
   * Indicates that longitudes are between 0 and 180, with an extra field
   * denoting East or West
   */
  public static final int FORMAT_0_180 = 2;

  /**
   * Indicates a format of degress and decimal minutes (-180:180)
   */
  public static final int FORMAT_HEM_DEG_DEC_MIN = 3;

  /**
   * Basic constructor
   */
  public LongitudeSpecification() {
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
  public LongitudeSpecification(int format, int valueColumn,
    int hemisphereColumn) throws PositionException {
    super(format, valueColumn, hemisphereColumn);
  }

  @Override
  public boolean formatValid(int format) {
    return (format >= 0 && format <= 3);
  }

  @Override
  public boolean hemisphereRequired() {
    return (format == FORMAT_0_180);
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
        case FORMAT_0_360: {
          doubleValue = Double.parseDouble(result);
          if (doubleValue > 180) {
            doubleValue = (360 - doubleValue) * -1;
          }
          break;
        }
        case FORMAT_MINUS180_180: {
          // No need to do anything!
          break;
        }
        case FORMAT_0_180: {
          String hemisphere = line.get(getHemisphereColumn());
          doubleValue = Double.parseDouble(result);
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

          if (hemisphere.equals("W")) {
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
          "NumberFormatException: Invalid longitude value '" + result + "'");
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
    case "e":
    case "east": {
      multiplier = 1.0;
      break;
    }
    case "w":
    case "west": {
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
