package uk.ac.exeter.QuinCe.web.datasets;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

public class QCTableData extends TreeMap<LocalDateTime, LinkedHashMap<Long, QCColumnValue>> {

  private List<QCColumn> dataColumns;

  public QCTableData(List<QCColumn> dataColumns) {
    super();
    this.dataColumns = dataColumns;
  }

  public void addValue(LocalDateTime time, Long fieldId, QCColumnValue value) {

    if (!containsKey(time)) {
      LinkedHashMap<Long, QCColumnValue> newValues = new LinkedHashMap<Long, QCColumnValue>();
      for (QCColumn column : dataColumns) {
        newValues.put(column.getId(), null);
      }

      put(time, newValues);
    }

    get(time).put(fieldId, value);
  }
}
