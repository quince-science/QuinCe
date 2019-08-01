package uk.ac.exeter.QuinCe.web.datasets.export;

import uk.ac.exeter.QuinCe.web.datasets.data.FieldValue;

/**
 * Special version of FieldValue that combines multiple values
 * of the same type.
 *
 * The value ID and used flags aren't used in this instance,
 * so their values are undefined.
 *
 * @author Steve Jones
 *
 */
public class CombinedFieldValue extends FieldValue {

  /**
   * The number of values added to this value
   */
  private int count;

  public CombinedFieldValue(FieldValue initValue) {
    super(initValue);
    this.count = 1;
  }

  public void addValue(FieldValue newValue) {
    value = ((value * count) + newValue.getValue()) / (count + 1);
    count++;

    if (newValue.getQcFlag().moreSignificantThan(qcFlag)) {
      qcFlag = newValue.getQcFlag();
    }

    if (newValue.needsFlag()) {
      needsFlag = true;
    }
  }
}
