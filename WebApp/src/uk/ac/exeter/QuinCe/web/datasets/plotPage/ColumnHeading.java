package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.Collection;
import java.util.List;
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
   * The column heading.
   */
  private final String heading;

  /**
   * Indicates whether or not the column will hold numeric values.
   */
  private final boolean numeric;

  /**
   * Simple constructor.
   *
   * @param heading
   *          The heading.
   * @param numeric
   *          Whether the column is numeric.
   */
  public ColumnHeading(String heading, boolean numeric) {
    this.heading = heading;
    this.numeric = numeric;
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

  /**
   * Build a list of {@link ColumnHeader}s from a list of column names with the
   * specified {@link #numeric} flag.
   *
   * @param names
   *          The column names.
   * @param numeric
   *          The {@link #numeric} flag to use for the columns.
   * @return The list of {@link ColumnHeading} objects.
   */
  public static List<ColumnHeading> headingList(Collection<String> names,
    boolean numeric) {

    return names.stream().map(x -> new ColumnHeading(x, numeric))
      .collect(Collectors.toList());
  }
}
