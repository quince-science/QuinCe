package uk.ac.exeter.QuinCe.web.PlotPage;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public class TableData extends TreeMap<LocalDateTime, LinkedHashMap<Field, FieldValue>> {

  private FieldSets fieldSets;

  public TableData(FieldSets fieldSets) {
    super();
    this.fieldSets = fieldSets;
  }

  /**
   * Add a value to the table
   * @param rowId
   * @param field
   * @param value
   */
  public void addValue(LocalDateTime rowId, Field field, FieldValue value) {
    if (!containsKey(rowId)) {
      put(rowId, fieldSets.generateFieldValuesMap());
    }

    get(rowId).put(field, value);
  }

  /**
   * Add a value to the table
   * @param rowId
   * @param fieldId
   * @param value
   */
  public void addValue(LocalDateTime rowId, long fieldId, FieldValue value) {
    addValue(rowId, fieldSets.getField(fieldId), value);
  }
}
