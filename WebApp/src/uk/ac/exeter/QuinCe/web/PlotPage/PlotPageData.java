package uk.ac.exeter.QuinCe.web.PlotPage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.utils.DateTimeUtils;

public class PlotPageData extends TreeMap<LocalDateTime, LinkedHashMap<Field, FieldValue>> {

  private FieldSets fieldSets;

  private boolean dirty = true;

  private List<LocalDateTime> rowIds;

  private String rowIdsJson;

  public PlotPageData(FieldSets fieldSets) {
    super();
    this.fieldSets = fieldSets;
    dirty = true;
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
    dirty = true;
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

  /**
   * Get data for a plot
   * @param xAxis X Axis
   * @param yAxis Y Axis
   * @return The plot data
   */
  protected String getPlotData(Field xAxis, Field yAxis) {
    String result;

    if (xAxis.getId() == Field.ROWID_FIELD_ID) {
      result = getPlotDataWithIdAxis(yAxis);
    } else {
      result = getPlotDataWithNonIdAxis(xAxis, yAxis);
    }

    return result;
  }


  private String getPlotDataWithIdAxis(Field yAxis) {

    JSONArray json = new JSONArray();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> entry :
      entrySet()) {

      if (entry.getValue().containsKey(yAxis)) {
        FieldValue value = entry.getValue().get(yAxis);

        if (null != value) {
          JSONArray record = new JSONArray();
          record.put(DateTimeUtils.dateToLong(entry.getKey()));
          record.put(DateTimeUtils.dateToLong(entry.getKey()));
          record.put(value.getQcFlag().getFlagValue());

          if (value.isNaN()) {
            record.put(JSONObject.NULL);
          } else {
            record.put(value.getValue());
          }

          json.put(record);
        }

      }
    }

    return json.toString();
  }

  private String getPlotDataWithNonIdAxis(Field xAxis, Field yAxis) {
    return null;
  }

  public List<LocalDateTime> getRowIds() {
    if (dirty) {
      makeRowIds();
      dirty = false;
    }

    return rowIds;
  }

  public String getRowIdsJson() {
    if (dirty) {
      makeRowIds();
      dirty = false;
    }

    return rowIdsJson;
  }

  private void makeRowIds() {
    rowIds = new ArrayList<LocalDateTime>(keySet());

    List<Long> jsonInput = new ArrayList<Long>(rowIds.size());
    for (LocalDateTime id : rowIds) {
      jsonInput.add(DateTimeUtils.dateToLong(id));
    }

    Gson gson = new Gson();
    rowIdsJson = gson.toJson(jsonInput).toString();
  }
}
