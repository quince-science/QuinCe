package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.exeter.QuinCe.data.Export.ColumnHeader;

/**
 * Holds the properties of a column heading on the plot page.
 *
 * @author Steve Jones
 *
 */
public class ColumnHeading {

  /**
   * The column's ID
   */
  private final long id;

  /**
   * The column heading.
   */
  private final String heading;

  /**
   * Indicates whether or not the column will hold numeric values.
   */
  private final boolean numeric;

  /**
   * Indicates whether or not values in this column can be edited.
   */
  private final boolean editable;

  /**
   * Indicates the true selection column for this column.
   *
   * <p>
   * Some columns cannot be selected, because they are combined in the data
   * table. Most commonly, latitude is combined with longitude. Therefore
   * selections on the latitude column should instead become selections on the
   * longitude column.
   * </p>
   *
   * <p>
   * For normal columns, this field will be the same as the {@link #id} field.
   * </p>
   */
  private final long selectionColumn;

  /**
   * Simple constructor.
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public ColumnHeading(long id, String heading, boolean numeric,
    boolean editable) {
    this.id = id;
    this.heading = heading;
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = id;
  }

  /**
   * Constructor with a different selection column.
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public ColumnHeading(long id, String heading, boolean numeric,
    boolean editable, long selectionColumn) {
    this.id = id;
    this.heading = heading;
    this.numeric = numeric;
    this.editable = editable;
    this.selectionColumn = selectionColumn;
  }

  /**
   * Get the column's ID
   *
   * @return The column ID
   */
  public long getId() {
    return id;
  }

  /**
   * Get the heading.
   *
   * @return The heading.
   */
  public String getHeading() {
    return heading;
  }

  /**
   * Determine whether or not the column will hold numeric values.
   *
   * @return {@code true} if the column will hold numeric values; {@code false}
   *         otherwise.
   */
  public boolean isNumeric() {
    return numeric;
  }

  public boolean canEdit() {
    return editable;
  }

  public long getSelectionColumn() {
    return selectionColumn;
  }

  /**
   * Build a list of {@link ColumnHeader}s from a list of column names with the
   * specified {@link #numeric} flag.
   *
   * @param names
   *          The column names.
   * @param numeric
   *          The {@link #numeric} flag to use for the columns.
   * @param editable
   *          The {@link #editable} flag to use for the columns.
   * @return The list of {@link ColumnHeading} objects.
   */
  public static List<ColumnHeading> headingList(Map<String, Long> headings,
    boolean numeric, boolean editable) {

    return headings.entrySet().stream()
      .map(x -> new ColumnHeading(x.getValue(), x.getKey(), numeric, editable))
      .collect(Collectors.toList());
  }
}
