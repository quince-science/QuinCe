package uk.ac.exeter.QuinCe.data.Instrument.DataFormats;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Handles all formats of longitudes, and the corresponding column assignments
 * within a data file
 */
public class LongitudeSpecification extends PositionSpecification {

  private static List<String> HEMISPHERE_VALUES;

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
   * Indicates that longitudes are between 0 and 180, with an extra field
   * denoting East or West
   */
  public static final int FORMAT_0_180 = 2;

  public static final String NAME_0_180 = "0° to 180°; separate hemisphere";

  /**
   * Indicates a format of hemisphere, degrees and decimal minutes in one column
   */
  public static final int FORMAT_HDM = 3;

  public static final String NAME_HDM = "H DD MM.mmm";

  public static final int FORMAT_H_DDDMMmmm = 4;

  public static final String NAME_H_DDDMMmmm = "H | DDDMM.mmm";

  public static final int FORMAT_DDDMMmmm = 5;

  public static final String NAME_DDDMMmmm = "(-)DDDMM.mmm";

  private static LinkedHashMap<Integer, String> formats = null;

  static {
    formats = new LinkedHashMap<Integer, String>();

    formats.put(FORMAT_MINUS180_180, NAME_MINUS180_180);
    formats.put(FORMAT_0_360, NAME_0_360);
    formats.put(FORMAT_0_180, NAME_0_180);
    formats.put(FORMAT_HDM, NAME_HDM);
    formats.put(FORMAT_H_DDDMMmmm, NAME_H_DDDMMmmm);
    formats.put(FORMAT_DDDMMmmm, NAME_DDDMMmmm);

    HEMISPHERE_VALUES = Arrays.asList("E", "e", "W", "w");
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
    return (format >= 0 && format <= 5);
  }

  @Override
  public boolean hemisphereRequired() {
    return (format == FORMAT_0_180 || format == FORMAT_H_DDDMMmmm);
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

    if (null == parser) {
      switch (format) {
      case FORMAT_MINUS180_180: {
        parser = new DecimalDegreesParser(true);
        break;
      }
      case FORMAT_0_360: {
        parser = new Zero360Parser();
        break;
      }
      case FORMAT_0_180: {
        parser = new DecimalDegreesParser(true);
        break;
      }
      case FORMAT_HDM: {
        parser = new HDMOneFieldParser(this);
        break;
      }
      case FORMAT_H_DDDMMmmm: {
        parser = new DDDMMmmmParser(true);
        break;
      }
      case FORMAT_DDDMMmmm: {
        parser = new DDDMMmmmParser(false);
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
  protected String fixNegatives(String value) {
    return value.equals("-180.000") ? "180.000" : super.fixNegatives(value);
  }

  public static LinkedHashMap<Integer, String> getFormats() {
    return formats;
  }

  @Override
  protected List<String> getValidHemisphereValues() {
    return HEMISPHERE_VALUES;
  }
}
