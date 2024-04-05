package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.Collection;

import uk.ac.exeter.QuinCe.data.Dataset.DatasetSensorValues;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

/**
 * Represents a column in the plot page table.
 *
 * @see PlotPageTableRecord
 */
public interface PlotPageTableValue {

  public static final char MEASURED_TYPE = 'M';

  public static final char INTERPOLATED_TYPE = 'I';

  public static final char DATA_REDUCTION_TYPE = 'R';

  // For fixed values, e.g. position/depth that isn't measured
  public static final char NOMINAL_TYPE = 'N';

  // For NaN values
  public static final char NAN_TYPE = '0';

  /**
   * Get the unique ID for this column value.
   *
   * @return The column value's ID.
   */
  public long getId();

  /**
   * Get the value string.
   *
   * @return The value.
   */
  public String getValue();

  /**
   * Get the raw value.
   *
   * <p>
   * There is no indication here of what class the returned value will be. It is
   * also more likely that this will be {@code null} - most implementations will
   * handle this in {@link #getValue()} but not here.
   * </p>
   *
   * @return The value
   */
  public Object getRawValue();

  /**
   * Get the QC flag.
   *
   * @return The QC flag.
   */
  public Flag getQcFlag();

  /**
   * Get the QC message.
   *
   * @return The QC message.
   */
  public String getQcMessage(DatasetSensorValues allSensorValues,
    boolean replaceNewlines);

  /**
   * Get the flag indicating whether user QC is needed.
   *
   * @return The user QC flag.
   */
  public boolean getFlagNeeded();

  /**
   * See if this value is null.
   */
  public boolean isNull();

  /**
   * Get the type of value (measured, interpolated etc)
   *
   * @return
   */
  public char getType();

  /**
   * Get the ID(s) of the source value(s) for this table value
   */
  public Collection<Long> getSources();
}
