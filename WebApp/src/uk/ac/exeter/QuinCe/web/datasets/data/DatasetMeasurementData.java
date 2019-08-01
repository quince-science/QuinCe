package uk.ac.exeter.QuinCe.web.datasets.data;

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
import uk.ac.exeter.QuinCe.data.Dataset.QC.Flag;
import uk.ac.exeter.QuinCe.data.Instrument.FileDefinition;
import uk.ac.exeter.QuinCe.data.Instrument.Instrument;
import uk.ac.exeter.QuinCe.utils.DateTimeUtils;
import uk.ac.exeter.QuinCe.utils.RecordNotFoundException;

public abstract class DatasetMeasurementData extends TreeMap<LocalDateTime, LinkedHashMap<Field, FieldValue>> {

  protected Instrument instrument;

  protected FieldSets fieldSets;

  private boolean dirty = true;

  private List<LocalDateTime> rowIds;

  private String rowIdsJson;

  private TreeMap<LocalDateTime, Position> positions;

  private Map<Field, MapRecords> mapCache;

  private boolean filterInitialised = false;

  public DatasetMeasurementData(Instrument instrument, FieldSets fieldSets) throws Exception {
    super();
    this.instrument = instrument;
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
   * Add a set of values to the table
   * @param rowId The table row
   * @param values The field values
   */
  public void addValues(LocalDateTime rowId, Map<Field, ? extends FieldValue> values) {
    if (!containsKey(rowId)) {
      put(rowId, fieldSets.generateFieldValuesMap());
    }

    get(rowId).putAll(values);
  }

  /**
   * Get data for a plot
   * @param xAxis X Axis
   * @param yAxis Y Axis
   * @return The plot data
   */
  public String getPlotData(Field xAxis, Field yAxis) {

    TreeSet<PlotRecord> records;

    if (xAxis.getId() == Field.ROWID_FIELD_ID) {
      records = getPlotDataWithIdAxis(yAxis);
    } else {
      records = getPlotDataWithNonIdAxis(xAxis, yAxis);
    }

    // TODO Convert to GSON
    JSONArray json = new JSONArray();

    for (PlotRecord record : records) {
      json.put(record.toJsonArray());
    }

    return json.toString();
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


  private TreeSet<PlotRecord> getPlotDataWithIdAxis(Field yAxis) {
    TreeSet<PlotRecord> records = new TreeSet<PlotRecord>();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> entry :
      entrySet()) {

      if (entry.getValue().containsKey(yAxis)) {
        FieldValue value = entry.getValue().get(yAxis);

        if (null != value && !value.isNaN()) {
          records.add(new PlotRecord(DateTimeUtils.dateToLong(entry.getKey()),
            DateTimeUtils.dateToLong(entry.getKey()), value));
        }
      }
    }

    return records;
  }

  private TreeSet<PlotRecord> getPlotDataWithNonIdAxis(Field xAxis, Field yAxis) {

    TreeSet<PlotRecord> records = new TreeSet<PlotRecord>();

    for (Map.Entry<LocalDateTime, LinkedHashMap<Field, FieldValue>> yEntry :
      entrySet()) {

      if (yEntry.getValue().containsKey(yAxis)) {

        long dateLong = DateTimeUtils.dateToLong(yEntry.getKey());
        FieldValue yValue = yEntry.getValue().get(yAxis);

        if (null != yValue && !yValue.isNaN()) {
          // If the current key also contains the x axis, use that
          FieldValue xValue = get(getClosestWithField(yEntry.getKey(), xAxis)).get(xAxis);

          records.add(new PlotRecord( xValue.getValue(), dateLong, yValue));
        }
      }
    }

    return records;
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

    if (fieldSets.containsField(FileDefinition.LONGITUDE_COLUMN_ID)) {
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

  /**
   * Get the field sets
   * @return
   */
  public FieldSets getFieldSets() {
    return fieldSets;
  }

  public FieldValue getValue(LocalDateTime time, int fieldIndex) {
    return get(time).get(fieldSets.getField(fieldIndex));
  }

  public CommentSet getCommentSet(int fieldIndex, List<LocalDateTime> rows) {

    CommentSet result = new CommentSet();
    Field field = fieldSets.getField(fieldIndex);

    for (LocalDateTime id : rows) {
      FieldValue value = get(id).get(field);
      if (null != value) {
        if (!value.getQcFlag().isGood()) {
          result.addComment(value.getQcComment(), value.getQcFlag());
        }
      }
    }


    return result;

  }

  public List<FieldValue> setQC(List<LocalDateTime> rows,
    int fieldIndex, Flag flag, String comment) {

    List<FieldValue> updatedValues = new ArrayList<FieldValue>(rows.size());
    Field field = fieldSets.getField(fieldIndex);

    for (LocalDateTime id : rows) {
      FieldValue value = get(id).get(field);
      if (null != value) {
        value.setQcFlag(flag);
        value.setQcComment(comment);
        value.setNeedsFlag(false);
        updatedValues.add(value);
      }
    }

    return updatedValues;
  }

  public int getFlagsRequired() {

    int result = 0;

    for (LinkedHashMap<Field, FieldValue> rowFields : values()) {
      for (FieldValue value : rowFields.values()) {
        if (null != value && value.needsFlag()) {
            result++;
        }
      }
    }

    return result;
  }

  public final void filterAndAddValues(String runType, LocalDateTime time, Map<Long, FieldValue> values)
    throws Exception {

    if (!filterInitialised) {
      initFilter();
    }

    filterAndAddValuesAction(runType, time, values);
  }


  /**
   * Add a set of values, filtering out unwanted values. The default
   * filter removes values for columns that are internally calibrated
   * where the run type is not a measurement. This has the effect
   * of removing all values taken during internal calibration.
   *
   * Override this method to filter the supplied values according to need.
   *
   * @param currentRunType
   * @param time
   * @param values
   * @throws RecordNotFoundException
   */
  protected abstract void filterAndAddValuesAction(String runType, LocalDateTime time, Map<Long, FieldValue> values)
      throws RecordNotFoundException;

  /**
   * Initialise information required for filterAndAddValues
   */
  protected abstract void initFilter() throws Exception;
}
