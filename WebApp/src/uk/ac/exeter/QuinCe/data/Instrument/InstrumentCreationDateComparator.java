package uk.ac.exeter.QuinCe.data.Instrument;

import java.util.Comparator;

/**
 * Comparator to compare {@link Instrument}s by their creation date.
 */
public class InstrumentCreationDateComparator implements Comparator<Instrument> {

  private boolean reverse;

  public InstrumentCreationDateComparator(boolean reverse) {
    super();
    this.reverse = reverse;
  }

  @Override
  public int compare(Instrument o1, Instrument o2) {
    return reverse ? o2.getCreated().compareTo(o1.getCreated())
      : o1.getCreated().compareTo(o2.getCreated());
  }
}
