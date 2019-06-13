package uk.ac.exeter.QuinCe.web.PlotPage.Data;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.primefaces.json.JSONArray;

import com.google.gson.Gson;

import uk.ac.exeter.QuinCe.data.Dataset.GeoBounds;
import uk.ac.exeter.QuinCe.data.Dataset.Position;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.web.PlotPage.Field;
import uk.ac.exeter.QuinCe.web.PlotPage.FieldSets;
import uk.ac.exeter.QuinCe.web.PlotPage.FieldValue;

public class PlotPageData extends TreeMap<LocalDateTime, LinkedHashMap<Field, FieldValue>> {

  private FieldSets fieldSets;

  private boolean dirty = true;

  private List<LocalDateTime> rowIds;

  private String rowIdsJson;

  private TreeMap<LocalDateTime, Position> positions;

  private Map<Field, MapRecords> mapCache;

  public PlotPageData(FieldSets fieldSets) {
    super();
    this.fieldSets = fieldSets;
    mapCache = new HashMap<Field, MapRecords>();
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
    // dirty flag is set in called method
  }

  /**
   * Get data for a plot
   * @param xAxis X Axis
   * @param yAxis Y Axis
   * @return The plot data
   */
  public String getPlotData(Field xAxis, Field yAxis) {
    String result;

    if (xAxis.getId() == Field.ROWID_FIELD_ID) {
      result = getPlotDataWithIdAxis(yAxis);
    } else {
      result = getPlotDataWithNonIdAxis(xAxis, yAxis);
    }

    return result;
  }

  public List<Double> getValueRange(Field field) {

    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for (LinkedHashMap<Field, FieldValue> record : values()) {
      FieldValue value = record.get(field);
      if (null != value && !value.isNaN()) {
        if (value.getValue() < min) {
          min = value.getValue();
        }

        if (value.getValue() > max) {
          max = value.getValue();
        }
      }
    }

    Double[] result = {min, max};
    return Arrays.asList(result);
  }

  public String getMapData(Field field, GeoBounds bounds) {

    if (!mapCache.containsKey(field)) {
      buildMapCache(field);
    }

    return mapCache.get(field).getDisplayJson(bounds);
  }

  private void buildMapCache(Field field) {
    MapRecords records = new MapRecords(size());

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> entry :
      entrySet()) {

      if (entry.getValue().containsKey(field)) {
        FieldValue value = entry.getValue().get(field);

        if (null != value && !value.isNaN()) {
          Position position = getClosestPosition(entry.getKey());
          MapRecord record = new MapRecord(position, DateTimeUtils.dateToLong(entry.getKey()), value);
          records.add(record);
        }
      }
    }

    mapCache.put(field, records);
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
          record.put(DateTimeUtils.dateToLong(entry.getKey())); // Date (x axis)
          record.put(DateTimeUtils.dateToLong(entry.getKey())); // ID
          record.put(value.getQcFlag().getFlagValue()); // QC Flag
          record.put(value.getValue()); // Value

          json.put(record);
        }

      }
    }

    return json.toString();
  }

  private String getPlotDataWithNonIdAxis(Field xAxis, Field yAxis) {

    TreeSet<PlotRecord> records = new TreeSet<PlotRecord>();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> yEntry :
      entrySet()) {

      if (yEntry.getValue().containsKey(yAxis)) {

        long dateLong = DateTimeUtils.dateToLong(yEntry.getKey());
        FieldValue yValue = yEntry.getValue().get(yAxis);

        if (null != yValue && !yValue.isNaN()) {
          // If the current key also contains the x axis, use that
          FieldValue xValue = get(getClosestWithField(yEntry.getKey(), xAxis)).get(xAxis);

          records.add(new PlotRecord(
            xValue.getValue(), dateLong,
            yValue.getQcFlag().getFlagValue(), yValue.getValue()));
        }
      }
    }

    // TODO Convert to GSON
    JSONArray json = new JSONArray();

    for (PlotRecord record : records) {
      json.put(record.toJsonArray());
    }

    return json.toString();
  }

  private LocalDateTime getClosestWithField(LocalDateTime start, Field field) {
    LocalDateTime result = null;

    if (get(start).containsKey(field) && !get(start).get(field).isNaN()) {
      result = start;
    } else {

      long prevDiff = Long.MAX_VALUE;
      long nextDiff = Long.MAX_VALUE;

      LocalDateTime prevKey = lowerKeyWithField(start, field);
      if (null != prevKey) {
        prevDiff = Math.abs(ChronoUnit.SECONDS.between(prevKey, start));
      }

      LocalDateTime nextKey = higherKeyWithField(start, field);
      if (null != nextKey) {
        nextDiff = Math.abs(ChronoUnit.SECONDS.between(nextKey, start));
      }

      if (prevDiff <= nextDiff) {
        result = prevKey;
      } else {
        result = nextKey;
      }
    }

    return result;
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
      buildCaches();
    }

    return rowIds;
  }

  public String getRowIdsJson() {
    if (dirty) {
      buildCaches();
    }

    return rowIdsJson;
  }

  private void buildCaches() {
    makeRowIds();
    makePositionLookup();
    dirty = false;
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

  private void makePositionLookup() {
    positions = new TreeMap<LocalDateTime, Position>();

    Field lonField = fieldSets.getField(FileDefinition.LONGITUDE_COLUMN_ID);
    Field latField = fieldSets.getField(FileDefinition.LATITUDE_COLUMN_ID);

    for (LocalDateTime time : keySet()) {
      LinkedHashMap<Field, FieldValue> data = get(time);
      if (data.containsKey(lonField)) {
        positions.put(time,
          new Position(
            data.get(lonField).getValue(),
            data.get(latField).getValue()));
      }
    }
  }

  private Position getClosestPosition(LocalDateTime start) {

    Position result = null;

    if (positions.containsKey(start)) {
      result = positions.get(start);
    } else {
      long prevDiff = Long.MAX_VALUE;
      long nextDiff = Long.MAX_VALUE;

      LocalDateTime prevKey = lowerKey(start);
      if (null != prevKey) {
        prevDiff = Math.abs(ChronoUnit.SECONDS.between(prevKey, start));
      }

      LocalDateTime nextKey = higherKey(start);
      if (null != nextKey) {
        nextDiff = Math.abs(ChronoUnit.SECONDS.between(nextKey, start));
      }

      if (prevDiff <= nextDiff) {
        result = positions.get(prevKey);
      } else {
        result = positions.get(nextKey);
      }
    }

    return result;
  }
}
