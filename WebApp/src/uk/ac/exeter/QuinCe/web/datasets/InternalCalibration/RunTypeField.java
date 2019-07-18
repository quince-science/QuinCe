package uk.ac.exeter.QuinCe.web.datasets.InternalCalibration;

import uk.ac.exeter.QuinCe.data.Instrument.FileColumn;
import uk.ac.exeter.QuinCe.web.PlotPage.Field;

public class RunTypeField extends Field {

  private String runType;

  private long columnId;

  public RunTypeField(String runType, FileColumn column) {
    super(makeId(runType, column), makeName(runType, column));
    this.runType = runType;
    this.columnId = column.getColumnId();
  }

  private static long makeId(String runType, FileColumn column) {
    return makeName(runType, column).hashCode();
  }

  private static String makeName(String runType, FileColumn column) {
    return runType + ":" + column.getColumnName();
  }

  protected long getColumnId() {
    return columnId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (columnId ^ (columnId >>> 32));
    result = prime * result + ((runType == null) ? 0 : runType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (!(obj instanceof RunTypeField))
      return false;
    RunTypeField other = (RunTypeField) obj;
    if (columnId != other.columnId)
      return false;
    if (runType == null) {
      if (other.runType != null)
        return false;
    } else if (!runType.equals(other.runType))
      return false;
    return true;
  }

}
