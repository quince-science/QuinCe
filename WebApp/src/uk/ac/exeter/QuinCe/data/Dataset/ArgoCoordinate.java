package uk.ac.exeter.QuinCe.data.Dataset;

import java.time.LocalDateTime;

import uk.ac.exeter.QuinCe.data.Dataset.QC.ArgoFlagScheme;
import uk.ac.exeter.QuinCe.data.Dataset.QC.FlagException;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DatabaseUtils;
import uk.ac.exeter.QuinCe.utils.StringUtils;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.PlotPageTableValue;
import uk.ac.exeter.QuinCe.web.datasets.plotPage.SimplePlotPageTableValue;

/**
 * {@link Coordinate} for Argo measurements.
 *
 * @see uk.ac.exeter.QuinCe.data.Instrument.Instrument#BASIS_ARGO
 */
public class ArgoCoordinate extends Coordinate {

  /**
   * Indicates that a float is ascending.
   */
  public static final char DIRECTION_ASCENDING = 'A';

  /**
   * Indicates that a float is descending.
   */
  public static final char DIRECTION_DESCENDING = 'D';

  /**
   * The current float cycle number.
   */
  private int cycleNumber;

  /**
   * The profile number. Each cycle may have multiple profiles, since different
   * sensors can measure at different sets of depths.
   *
   * @see #nLevel
   */
  private int nProf;

  /**
   * Indicates whether the float is descending or ascending.
   */
  private char direction;

  /**
   * The level number in the current profile.
   *
   * @see #nProf
   */
  private int nLevel;

  /**
   * The pressure (i.e. depth) of this coordinate.
   *
   * <p>
   * This is specific to a given nProf/nLevel combination.
   * </p>
   *
   * <p>
   * This value is referred to as 'pres' to be consistent with its label in the
   * Argo data.
   * </p>
   */
  private double pres;

  /**
   * The filename of the Argo profile netCDF file from which this Coordinate was
   * retrieved.
   */
  private String sourceFile;

  /**
   * Construct a new ArgoCoordinate with all values.
   *
   * <p>
   * Pass {@link DatabaseUtils#NO_DATABASE_RECORD} for a new Coordinate that is
   * not yet in the database.
   * </p>
   *
   * @param id
   *          The Coordinate's database ID.
   * @param datasetId
   *          The ID of the {@link DataSet} to which this Coordinate belongs.
   * @param cycleNumber
   *          The current float cycle number.
   * @param nProf
   *          The profile number.
   * @param direction
   *          The float's travel direction.
   * @param nLevel
   *          The level number in the specified profile.
   * @param pres
   *          The pressure.
   * @param sourceFile
   *          The netCDF file from which the coordinate was extracted.
   * @param timestamp
   *          The timestamp of the Coordinate.
   * @throws CoordinateException
   *           If the provided Coordinate details are invalid.
   */
  public ArgoCoordinate(long id, long datasetId, int cycleNumber, int nProf,
    char direction, int nLevel, double pres, String sourceFile,
    LocalDateTime timestamp) throws CoordinateException {

    super(id, datasetId, timestamp);
    this.cycleNumber = cycleNumber;
    this.nProf = nProf;
    validateDirection(direction);
    this.direction = direction;
    this.nLevel = nLevel;
    this.pres = pres;
    this.sourceFile = sourceFile;
  }

  /**
   * Construct a new ArgoCoordinate that has not yet been stored in the
   * database.
   *
   * @param datasetId
   *          The ID of the {@link DataSet} to which this Coordinate belongs.
   * @param cycleNumber
   *          The current float cycle number.
   * @param nProf
   *          The profile number.
   * @param direction
   *          The float's travel direction.
   * @param nLevel
   *          The level number in the specified profile.
   * @param pres
   *          The pressure.
   * @param sourceFile
   *          The netCDF file from which the coordinate was extracted.
   * @param timestamp
   *          The timestamp of the Coordinate.
   * @throws CoordinateException
   *           If the provided Coordinate details are invalid.
   */
  public ArgoCoordinate(long datasetId, int cycleNumber, int nProf,
    char direction, int nLevel, double pres, String sourceFile,
    LocalDateTime timestamp) throws CoordinateException {

    super(DatabaseUtils.NO_DATABASE_RECORD, datasetId, timestamp);
    this.cycleNumber = cycleNumber;
    this.nProf = nProf;
    validateDirection(direction);
    this.direction = direction;
    this.nLevel = nLevel;
    this.pres = pres;
    this.sourceFile = sourceFile;
  }

  @Override
  public int getType() {
    return Instrument.BASIS_ARGO;
  }

  @Override
  protected boolean equalsWorker(Coordinate other) {
    // The parent has already checked the class.
    ArgoCoordinate o = (ArgoCoordinate) other;

    /*
     * The pressure is inherently linked to the nProf/nLevel/direction
     * combination, so we don't need to check it.
     *
     * Not all records contain timestamp or position, so we don't check those.
     */
    return cycleNumber == o.cycleNumber && nProf == o.nProf
      && direction == o.direction && nLevel == o.nLevel;
  }

  @Override
  protected int compareToWorker(Coordinate other) {
    // The parent has already checked the class.
    ArgoCoordinate o = (ArgoCoordinate) other;

    int result = cycleNumber - o.cycleNumber;

    // Descending happens before ascending!
    if (result == 0) {
      if (direction == o.direction) {
        result = 0;
      } else if (direction == 'D') {
        result = -1;
      } else {
        result = 1;
      }
    }

    if (result == 0) {
      result = nProf - o.nProf;
    }

    if (result == 0) {
      result = nLevel - o.nLevel;
    }

    return result;
  }

  /**
   * Validate a direction value.
   *
   * <p>
   * If the direction is valid, nothing happens. If it is not, a
   * {@link CoordinateException} is thrown.
   * </p>
   *
   * @param direction
   *          The direction value to check.
   * @throws CoordinateException
   *           If the value is invalid.
   */
  private void validateDirection(char direction) throws CoordinateException {
    if (direction != DIRECTION_DESCENDING && direction != DIRECTION_ASCENDING) {
      throw new CoordinateException(
        "Invalid float direction '" + direction + "'");
    }
  }

  /**
   * Get the cycle number.
   *
   * @return The cycle number.
   */
  public int getCycleNumber() {
    return cycleNumber;
  }

  /**
   * Get the nProf.
   *
   * @return The nProf.
   */
  public int getNProf() {
    return nProf;
  }

  /**
   * Get the float direction.
   *
   * @return The float direction.
   */
  public char getDirection() {
    return direction;
  }

  /**
   * Get the nLevel.
   *
   * @return The nLevel.
   */
  public int getNLevel() {
    return nLevel;
  }

  /**
   * Get the pressure.
   *
   * @return The pressure.
   */
  public double getPres() {
    return pres;
  }

  /**
   * Get the filename of the Argo profile file from which this coordinate was
   * retrieved.
   *
   * @return The source filename.
   */
  public String getSourceFile() {
    return sourceFile;
  }

  @Override
  public String toString() {
    return "" + cycleNumber + "-" + nProf + "-" + direction + "-"
      + StringUtils.formatNumber(pres);
  }

  /**
   * Create an {@link ArgoProfile} object based on this Coordinate.
   *
   * @return The new {@link ArgoProfile} object.
   */
  public ArgoProfile toProfile() {
    return new ArgoProfile(this);
  }

  @Override
  public String getValue(ColumnHeading columnHeading)
    throws ColumnHeadingNotFoundException {

    String result = null;

    switch (columnHeading.getCodeName()) {
    case "CYCLE_NUMBER": {
      result = String.valueOf(cycleNumber);
      break;
    }
    case "NPROF": {
      result = String.valueOf(nProf);
      break;
    }
    case "DIRECTION": {
      result = String.valueOf(direction);
      break;
    }
    case "NLEVEL": {
      result = String.valueOf(nLevel);
      break;
    }
    case "PRES": {
      result = String.valueOf(pres);
      break;
    }
    case "SOURCE_FILE": {
      result = sourceFile;
      break;
    }
    default: {
      throw new ColumnHeadingNotFoundException(columnHeading);
    }
    }

    return result;
  }

  public PlotPageTableValue getPlotPageTableValue(ColumnHeading columnHeading)
    throws FlagException, ColumnHeadingNotFoundException {
    return new SimplePlotPageTableValue(getValue(columnHeading),
      ArgoFlagScheme.getInstance());
  }
}
