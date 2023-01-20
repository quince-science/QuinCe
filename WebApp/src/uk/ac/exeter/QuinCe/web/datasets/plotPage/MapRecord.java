package uk.ac.exeter.QuinCe.web.datasets.plotPage;

import com.javadocmd.simplelatlng.LatLng;

import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;

public abstract class MapRecord implements Comparable<MapRecord> {

  protected final LatLng position;
  protected final long id;

  protected MapRecord(LatLng position, long id) {
    this.position = position;
    this.id = id;
  }

  @Override
  public int compareTo(MapRecord o) {
    return Long.compare(id, o.id);
  }

  /**
   * Get the QC Page Row ID for this value.
   *
   * @return The row ID
   */
  public long getRowId() {
    return id;
  }

  public abstract boolean isGood();

  public abstract boolean flagNeeded();

  public abstract Double getValue();

  public abstract Flag getFlag(boolean ignoreNeeded);
}
