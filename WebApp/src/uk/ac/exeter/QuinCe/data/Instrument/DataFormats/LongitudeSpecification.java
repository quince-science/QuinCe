package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.List;

/**
 * Handles all formats of longitudes, and the corresponding column assignments within a data file
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
   * Indicates that longitudes are between 0 and 180, with an extra
   * field denoting East or West
   */
  public static final int FORMAT_0_180 = 2;

  /**
   * Basic constructor
   */
  public LongitudeSpecification() {
    super();
  }

  /**
   * Constructor for a complete specification
   * @param format The format
   * @param valueColumn The value column
   * @param hemisphereColumn The hemisphere column
   * @throws PositionException If the specification is incomplete or invalid
   */
  public LongitudeSpecification(int format, int valueColumn, int hemisphereColumn) throws PositionException {
    super(format, valueColumn, hemisphereColumn);
  }

  @Override
  public boolean formatValid(int format) {
    return (format >= 0 && format <= 2);
  }

  @Override
  public boolean hemisphereRequired() {
    return (format == FORMAT_0_180);
  }

  @Override
  public double getValue(List<String> line) throws PositionException {

    double value;

    try {
      value = Double.parseDouble(line.get(getValueColumn()));

      switch (format) {
      case FORMAT_0_360: {
        if (value > 180) {
          value = (360 - value) * -1;
        }
        break;
      }
      case FORMAT_MINUS180_180: {
        // No need to do anything!
        break;
      }
      case FORMAT_0_180: {
        String hemisphere = line.get(getHemisphereColumn());
        value = value * hemisphereMultiplier(hemisphere);
        break;
      }
      default: {
        throw new InvalidPositionFormatException(format);
      }
      }
    } catch (NumberFormatException e) {
      throw new PositionException("Invalid longitude value " + line.get(getValueColumn()));
    }

    if (value < -180 || value > 180) {
      throw new PositionException("Invalid longitude value " + value);
    }

    return value;
  }

  /**
   * Calculate the longitude multiplier for a longitude value. East = 1, West = -1
   * @param hemisphere The hemisphere
   * @return The multiplier
   * @throws PositionException If the hemisphere value is invalid
   */
  private double hemisphereMultiplier(String hemisphere) throws PositionException {
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
      throw new PositionException("Invalid hemisphere value " + hemisphere);
    }
    }

    return multiplier;
  }
}
