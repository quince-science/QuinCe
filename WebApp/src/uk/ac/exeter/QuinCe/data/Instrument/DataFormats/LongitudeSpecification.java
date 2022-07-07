package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.Arrays;
import java.util.TreeMap;

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

  public static final String NAME_0_360 = "0° to 360°";

  /**
   * Indicates that longitudes are between -180 and 180
   */
  public static final int FORMAT_MINUS180_180 = 1;

  public static final String NAME_MINUS180_180 = "-180° to 180°";

  /**
   * Indicates that longitudes are between 0 and 180, with an extra field denoting
   * East or West
   */
  public static final int FORMAT_0_180 = 2;

  public static final String NAME_0_180 = "0° to 180°; separate hemisphere";

  /**
   * Indicates a format of hemisphere, degrees and decimal minutes in one column
   */
  public static final int FORMAT_HDM = 3;

  public static final String NAME_HDM = "H DD MM.mmm";

  private static TreeMap<Integer, String> formats = null;

  static {
    formats = new TreeMap<Integer, String>();

    formats.put(FORMAT_0_360, NAME_0_360);
    formats.put(FORMAT_MINUS180_180, NAME_MINUS180_180);
    formats.put(FORMAT_0_180, NAME_0_180);
    formats.put(FORMAT_HDM, NAME_HDM);
  }

  /**
   *
   * Basic constructor
   */
  public LongitudeSpecification() {
    super();
  }

  /**
   * Constructor for a complete specification
   *
   * @param format           The format
   * @param valueColumn      The value column
   * @param hemisphereColumn The hemisphere column
   * @throws PositionException If the specification is incomplete or invalid
   */
  public LongitudeSpecification(int format, int valueColumn, int hemisphereColumn) throws PositionException {
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
  protected double getMin() {
    return -180D;
  }

  @Override
  protected double getMax() {
    return 180D;
  }

  @Override
  protected PositionParser getParser() throws PositionException {

    PositionParser result;

    switch (format) {
    case FORMAT_MINUS180_180: {
      result = new DecimalDegreesParser(true);
      break;
    }
    case FORMAT_0_360: {
      result = new Zero360Parser();
      break;
    }
    case FORMAT_0_180: {
      result = new DecimalDegreesParser(makeHemisphereMultiplier());
      break;
    }
    case FORMAT_HDM: {
      result = new HDMParser(makeHemisphereMultiplier());
      break;
    }
    default: {
      throw new PositionException("Unknown format " + format);
    }
    }

    return result;
  }

  private HemisphereMultiplier makeHemisphereMultiplier() {
    return new HemisphereMultiplier(Arrays.asList("E", "East"), Arrays.asList("W", "West"));
  }

  public static TreeMap<Integer, String> getFormats() {
    return formats;
  }
}
