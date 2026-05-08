package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import java.util.Comparator;

/**
 * Compare the value of a {@link PlotPageTableValue} assuming that the value is
 * numeric.
 *
 * <p>
 * The comparator uses {@link Double#parseDouble(String)} and does not perform
 * any checks, so it can throw any exceptions thrown by
 * {@link Double#parseDouble(String)}.
 * </p>
 */
public class PlotPageTableValueNumericComparator
  implements Comparator<PlotPageTableValue> {

  @Override
  public int compare(PlotPageTableValue arg0, PlotPageTableValue arg1) {
    return Double.compare(Double.parseDouble(arg0.getValue()),
      Double.parseDouble(arg1.getValue()));
  }
}
