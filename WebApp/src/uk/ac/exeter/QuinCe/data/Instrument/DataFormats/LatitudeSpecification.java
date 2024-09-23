package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.Arrays;
import java.util.TreeMap;

/**
 * Specifies the latitude format for a data file
 */
public class LatitudeSpecification extends PositionSpecification {

  /**
   * Indicates that latitudes are between -90 and 90
   */
  public static final int FORMAT_MINUS90_90 = 0;

  public static final String NAME_MINUS90_90 = "-90° to 90°";

  /**
   * Indicates that longitudes are between 0 and 90, with a separate column
   * specifying the hemisphere
   */
  public static final int FORMAT_0_90 = 1;

  public static final String NAME_0_90 = "0° to 90°; separate hemisphere";

  /**
   * Indicates a format of hemisphere, degrees and decimal minutes
   */
  public static final int FORMAT_HDM = 2;

  public static final String NAME_HDM = "H DD MM.mmm";

  private static TreeMap<Integer, String> formats = null;

  public static final int FORMAT_H_DDDMMmmm = 3;

  public static final String NAME_H_DDDMMmmm = "H | DDDMM.mmm";

  public static final int FORMAT_DDDMMmmm = 4;

  public static final String NAME_DDDMMmmm = "(-)DDDMM.mmm";

  static {
    formats = new TreeMap<Integer, String>();

    formats.put(FORMAT_MINUS90_90, NAME_MINUS90_90);
    formats.put(FORMAT_0_90, NAME_0_90);
    formats.put(FORMAT_HDM, NAME_HDM);
    formats.put(FORMAT_H_DDDMMmmm, NAME_H_DDDMMmmm);
    formats.put(FORMAT_DDDMMmmm, NAME_DDDMMmmm);
  }

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
    return (format >= 0 && format <= 4);
  }

  @Override
  public boolean hemisphereRequired() {
    return (format == FORMAT_0_90 || format == FORMAT_H_DDDMMmmm);
  }

  @Override
  protected PositionParser getParser() throws PositionException {
    if (null == parser) {
      switch (format) {
      case FORMAT_MINUS90_90: {
        parser = new DecimalDegreesParser(true);
        break;
      }
      case FORMAT_0_90: {
        parser = new DecimalDegreesParser(makeHemisphereMultiplier());
        break;
      }
      case FORMAT_HDM: {
        parser = new HDMOneFieldParser(makeHemisphereMultiplier());
        break;
      }
      case FORMAT_H_DDDMMmmm: {
        parser = new DDDMMmmmParser(makeHemisphereMultiplier());
        break;
      }
      case FORMAT_DDDMMmmm: {
        parser = new DDDMMmmmParser();
        break;
      }
      default: {
        throw new PositionException("Unknown format " + format);
      }
      }
    }
    return parser;
  }

  @Override
  protected double getMin() {
    return -90D;
  }

  @Override
  protected double getMax() {
    return 90D;
  }

  private HemisphereMultiplier makeHemisphereMultiplier() {
    return new HemisphereMultiplier(Arrays.asList("N", "North"),
      Arrays.asList("S", "South"));
  }

  public static TreeMap<Integer, String> getFormats() {
    return formats;
  }
}
