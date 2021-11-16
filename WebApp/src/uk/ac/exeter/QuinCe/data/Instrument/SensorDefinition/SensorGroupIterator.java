package uk.ac.exeter.QuinCe.data.Instrument.SensorDefinition;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SensorGroupIterator implements Iterator<SensorGroup> {

  private SensorGroup nextGroup;

  protected SensorGroupIterator(SensorGroup firstGroup) {
    this.nextGroup = firstGroup;
  }

  @Override
  public boolean hasNext() {
    return null != nextGroup;
  }

  @Override
  public SensorGroup next() {
    SensorGroup result = null;

    if (null == nextGroup) {
      throw new NoSuchElementException();
    } else {
      result = nextGroup;
      nextGroup = nextGroup.getNextGroup();
    }

    return result;
  }
}
