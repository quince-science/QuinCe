package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import uk.ac.exeter.QuinCe.data.Dataset.SensorValue;
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

/**
 * A representation of a record in the plot page table.
 *
 * <p>
 * Columns are added using the {@code addColumn} methods. The class assumes that
 * all columns are added in display order.
 * </p>
 *
 * @author Steve Jones
 *
 */
public class PlotPageTableRecord {

  /**
   * The key for the row ID
   */
  protected static final String ID_KEY = "DT_RowId";

  /**
   * The record ID
   */
  private final long id;

  /**
   * The record's columns
   */
  private Map<Integer, PlotPageTableValue> columns = new HashMap<Integer, PlotPageTableValue>();

  /**
   * The column index to give to the next added column.
   */
  private int nextColumnIndex = 0;

  /**
   * Simple constructor with a direct ID
   *
   * @param id
   *          The record ID
   */
  public PlotPageTableRecord(long id) {
    this.id = id;
  }

  public PlotPageTableRecord(LocalDateTime id) {
    this.id = DateTimeUtils.dateToLong(id);
  }

  /**
   * Add a timestamp column
   *
   * @param value
   *          The time value.
   * @param used
   *          Whether or not the value is used in a calculation.
   * @param qcFlag
   *          The QC flag.
   * @param qcMessage
   *          The QC message.
   * @param flagNeeded
   *          Indicates whether or not a user QC flag is needed.
   */
  public void addColumn(LocalDateTime value) {
    addColumn(new SimplePlotPageTableValue(value, false));
  }

  public void addColumn(SensorValue sensorValue) {
    addColumn(new SensorValuePlotPageTableValue(sensorValue));
  }

  /**
   * Add a column.
   *
   * @param value
   *          The time value.
   * @param qcFlag
   *          The QC flag.
   * @param qcMessage
   *          The QC message.
   * @param flagNeeded
   *          Indicates whether or not a user QC flag is needed.
   */
  public void addColumn(String value, Flag qcFlag, String qcMessage,
    boolean flagNeeded) {

    addColumn(
      new SimplePlotPageTableValue(value, qcFlag, qcMessage, flagNeeded));
  }

  public void addColumn(PlotPageTableValue column) {
    columns.put(nextColumnIndex, column);
    nextColumnIndex++;
  }

  /**
   * Add a {@link Collection} of columns to the record.
   *
   * <p>
   * Note that the columns will be inserted in the iteration order of the
   * {@link Collection}, so it is the caller's responsibility to ensure that the
   * order is correct (by using a {@link List} or similar).
   * </p>
   *
   * @param columns
   *          The columns to add.
   */
  public void addAll(Collection<PlotPageTableValue> columns) {
    columns.forEach(c -> addColumn(c));
  }

  public void addBlankColumn() {
    addColumn("", Flag.GOOD, null, false);
  }

  public void addBlankColumns(int count) {
    for (int i = 0; i < count; i++) {
      addBlankColumn();
    }
  }

  protected long getId() {
    return id;
  }

  public Map<Integer, PlotPageTableValue> getColumns() {
    return columns;
  }
}
