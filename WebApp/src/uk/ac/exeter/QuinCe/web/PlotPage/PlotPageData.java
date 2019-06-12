package uk.ac.exeter.QuinCe.web.PlotPage;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.primefaces.json.JSONArray;

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
    // TODO Convert to Gson
    JSONArray json = new JSONArray();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> entry :
      entrySet()) {

      if (entry.getValue().containsKey(yAxis)) {
        FieldValue value = entry.getValue().get(yAxis);

        if (null != value && !value.isNaN()) {
          JSONArray record = new JSONArray();
          record.put(DateTimeUtils.dateToLong(entry.getKey()));
          record.put(DateTimeUtils.dateToLong(entry.getKey()));
          record.put(value.getQcFlag().getFlagValue());
          record.put(value.getValue());

          json.put(record);
        }

      }
    }

    return json.toString();
  }

  private String getPlotDataWithNonIdAxis(Field xAxis, Field yAxis) {

    TreeSet<SortablePlotRecord> records = new TreeSet<SortablePlotRecord>();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> yEntry :
      entrySet()) {

      if (yEntry.getValue().containsKey(yAxis)) {

        long dateLong = DateTimeUtils.dateToLong(yEntry.getKey());
        FieldValue yValue = yEntry.getValue().get(yAxis);

        if (null != yValue && !yValue.isNaN()) {
          // If the current key also contains the x axis, use that
          FieldValue xValue = null;

          if (yEntry.getValue().containsKey(xAxis) && !yEntry.getValue().get(xAxis).isNaN()) {
            xValue = yEntry.getValue().get(xAxis);
          } else {

            long prevDiff = Long.MAX_VALUE;
            long nextDiff = Long.MAX_VALUE;

            LocalDateTime prevKey = lowerKeyWithField(yEntry.getKey(), xAxis);
            if (null != prevKey) {
              prevDiff = Math.abs(ChronoUnit.SECONDS.between(prevKey, yEntry.getKey()));
            }

            LocalDateTime nextKey = higherKeyWithField(yEntry.getKey(), xAxis);
            if (null != nextKey) {
              nextDiff = Math.abs(ChronoUnit.SECONDS.between(nextKey, yEntry.getKey()));
            }

            if (prevDiff <= nextDiff) {
              xValue = get(prevKey).get(xAxis);
            } else {
              xValue = get(nextKey).get(xAxis);
            }
          }

          records.add(new SortablePlotRecord(
            xValue.getValue(), dateLong,
            yValue.getQcFlag().getFlagValue(), yValue.getValue()));
        }
      }
    }

    // TODO Convert to GSON
    JSONArray json = new JSONArray();

    for (SortablePlotRecord record : records) {
      json.put(record.toJsonArray());
    }

    return json.toString();
  }

  private LocalDateTime lowerKeyWithField(LocalDateTime start, Field field) {

    LocalDateTime result = null;

    LocalDateTime searchKey = lowerKey(start);

    while (null == result && null != searchKey) {
      if (get(searchKey).containsKey(field) && !get(searchKey).get(field).isNaN()) {
        result = searchKey;
      } else {
        searchKey = lowerKey(searchKey);
      }
    }

    return result;
  }

  private LocalDateTime higherKeyWithField(LocalDateTime start, Field field) {

    LocalDateTime result = null;

    LocalDateTime searchKey = higherKey(start);

    while (null == result && null != searchKey) {
      if (get(searchKey).containsKey(field) && !get(searchKey).get(field).isNaN()) {
        result = searchKey;
      } else {
        searchKey = higherKey(searchKey);
      }
    }

    return result;
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


  private class SortablePlotRecord implements Comparable<SortablePlotRecord> {

    double xValue;
    long id;
    int flag;
    double yValue;

    private SortablePlotRecord(double xValue, long id, int flag, double yValue) {
      this.xValue = xValue;
      this.id = id;
      this.flag = flag;
      this.yValue = yValue;
    }

    private JSONArray toJsonArray() {
      // TODO Convert to GSON
      JSONArray json = new JSONArray();
      json.put(xValue);
      json.put(id);
      json.put(flag);
      json.put(yValue);
      return json;
    }

    @Override
    public int compareTo(SortablePlotRecord o) {

      int result = Double.compare(xValue, o.xValue);

      if (result == 0) {
        result = Double.compare(yValue, o.yValue);
      }

      return result;
    }
  }
}
